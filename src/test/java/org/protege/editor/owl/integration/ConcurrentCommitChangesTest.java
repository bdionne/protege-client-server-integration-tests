package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.ProjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ConcurrentCommitChangesTest extends BaseTest {

	private ProjectId projectId;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void createProject() throws Exception {
		projectId = createPizzaProject();
	}

	@Test
	public void shouldCommitAddition() throws Exception {
		LocalHttpClient manager = client("bob");
		ServerDocument serverDocument = manager.openProject(projectId);
		VersionedOWLOntology vont = manager.buildVersionedOntology(serverDocument, owlManager, projectId);

	        
	        
	        /*
					 * Simulates user edits over a working ontology (add axioms)
	         */


		List<OWLOntologyChange> cs = getOwlOntologyChanges(vont.getOntology());

		owlManager.applyChanges(cs);

		histManager.logChanges(cs);
	        
	        /*
	         * Prepare the commit bundle
	         */
		List<OWLOntologyChange> changes = ClientUtils.getUncommittedChanges(histManager, vont.getOntology(), vont.getChangeHistory());
		Commit commit = ClientUtils.createCommit(manager, "Add customer subclass of domain concept", changes);
		DocumentRevision commitBaseRevision = vont.getHeadRevision();
		CommitBundle commitBundle = new CommitBundleImpl(commitBaseRevision, commit);

		ExecutorService ex_serv = Executors.newFixedThreadPool(2);

		Future<ChangeHistory> fch1 = ex_serv.submit(new Callable<ChangeHistory>() {

			@Override
			public ChangeHistory call() throws Exception {
				// TODO Auto-generated method stub
				return manager.commit(projectId, commitBundle);
			}

		});


		Future<ChangeHistory> fch2 = ex_serv.submit(new Callable<ChangeHistory>() {

			@Override
			public ChangeHistory call() throws Exception {
				// TODO Auto-generated method stub
				return manager.commit(projectId, commitBundle);
			}

		});

		ExecutionException expected = null;

		ChangeHistory h1 = null;
		ChangeHistory h2 = null;

		try {
			h1 = fch1.get();
			vont.update(h1);
		} catch (ExecutionException ex) {
			expected = ex;


		}

		try {
			h2 = fch2.get();
			vont.update(h2);
		} catch (ExecutionException ex) {
			expected = ex;


		}


		if (expected != null) {
			assertThat(expected.getCause().getMessage(), is("Commit failed, please update your local copy first"));
		} else {
			// we should have gotten an exception
			assertThat(1 + 1, is(3));
		}
		ChangeHistory changeHistoryFromClient = vont.getChangeHistory();

		Utils.assertChangeHistoryNotEmpty(changeHistoryFromClient, "The local change history should not be empty", 2);
		ChangeHistory changeHistoryFromServer = ((LocalHttpClient) manager).getAllChanges(vont.getServerDocument(), projectId);
		Utils.assertChangeHistoryNotEmpty(changeHistoryFromServer, "The remote change history should not be empty", 2);
	}

	@After
	public void removeProject() throws Exception {
		getAdmin().deleteProject(projectId, true);
	}


}
