package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.ProjectId;
import org.junit.Before;
import org.junit.Test;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.util.SnapShot;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;


public class SquashHistoryTest extends BaseTest {

	private ProjectId projectId;

	@Before
	public void createProject() throws Exception {
		projectId = createPizzaProject();
	}

	@Test
	public void shouldSquashHistory() throws Exception {
		LocalHttpClient client = client("bob");
		ServerDocument serverDocument = client.openProject(projectId);
		VersionedOWLOntology vont = client.buildVersionedOntology(serverDocument, owlManager, projectId);

		List<OWLOntologyChange> cs = getOwlOntologyChanges(vont.getOntology());

		owlManager.applyChanges(cs);

		List<OWLOntologyChange> changes = ClientUtils.getUncommittedChanges(histManager, vont.getOntology(), vont.getChangeHistory());
		Commit commit = ClientUtils.createCommit(client, "Add customer subclass of domain concept", changes);

		CommitBundle commitBundle = new CommitBundleImpl(vont.getHeadRevision(), commit);

		vont.update(client.commit(projectId, commitBundle));

		ChangeHistory clientHistory = vont.getChangeHistory();
		ChangeHistory serverHistory = client.getAllChanges(vont.getServerDocument(), projectId);

		assertThat(clientHistory.getHeadRevision(), is(R1));
		assertThat(serverHistory.getHeadRevision(), is(R1));

		// Squash
		SnapShot clientSnapShot = new SnapShot(vont.getOntology());
		client.squashHistory(clientSnapShot, projectId);

		serverHistory = client.getAllChanges(vont.getServerDocument(), projectId);

		assertThat(serverHistory.getHeadRevision(), is(R0));

		SnapShot serverSnapShot = client.getSnapShot(projectId);

		assertThat(clientSnapShot.getOntology().getGeneralClassAxioms(),
			equalTo(serverSnapShot.getOntology().getGeneralClassAxioms()));

		assertThat(clientSnapShot.getOntology().getSignature(),
			equalTo(serverSnapShot.getOntology().getSignature()));
	}

}

