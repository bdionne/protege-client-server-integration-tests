package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.ProjectId;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.List;

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

		List<OWLOntologyChange> cs = getOwlOntologyChanges(vont.getOntology());
		owlManager.applyChanges(cs);
		histManager.logChanges(cs);

		List<OWLOntologyChange> changes = ClientUtils.getUncommittedChanges(histManager, vont.getOntology(), vont.getChangeHistory());
		Commit commit = ClientUtils.createCommit(manager, "Add customer subclass of domain concept", changes);
		DocumentRevision commitBaseRevision = vont.getHeadRevision();
		CommitBundle commitBundle = new CommitBundleImpl(commitBaseRevision, commit);

		vont.update(manager.commit(projectId, commitBundle));

		try {
			vont.update(manager.commit(projectId, commitBundle));
			Assert.fail("Second commit should have failed");
		} catch (SynchronizationException e) {
			assertThat(e.getMessage(), is("Commit failed, please update your local copy first"));
		}

		Utils.assertChangeHistoryNotEmpty(vont.getChangeHistory(), "The local change history should not be empty", 2);
		ChangeHistory changeHistoryFromServer = manager.getAllChanges(vont.getServerDocument(), projectId);
		Utils.assertChangeHistoryNotEmpty(changeHistoryFromServer, "The remote change history should not be empty", 2);
	}

	@After
	public void removeProject() throws Exception {
		getAdmin().deleteProject(projectId, true);
	}


}
