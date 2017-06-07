package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.ProjectId;
import org.junit.Before;
import org.junit.Test;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.ServiceUnavailableException;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by rgrinberg on 6/1/17.
 */
public class PauseServerTest extends BaseTest {

	@Test
	public void unpauseServerSucceeds() throws Exception {
		LocalHttpClient client = client("bob");
		client.pauseServer();
		client.resumeServer();
	}

	@Test(expected = AuthorizationException.class)
	public void pauseLocksToUser() throws Exception {
		final LocalHttpClient bob = client("bob");
		bob.pauseServer();
		try {
			client("alice").resumeServer();
		} finally {
			bob.resumeServer();
		}
	}

	@Test
	public void nonWorkflowManagerFailsToPause() throws Exception {
		final ProjectId projectId = createPizzaProject();
		LocalHttpClient client = client("alice");
		assertThat(client.isWorkFlowManager(projectId), is(false));
		client.pauseServer();
		// if the pause succeeds, then we should unpause the server for the other tests.
		client.resumeServer();
	}

	@Test
	public void nonWorkflowManagerCanFetchLatestChanges() throws ClientRequestException, AuthorizationException {
		final ProjectId projectId = createPizzaProject();
		final LocalHttpClient nonManager = client("alice");
		final LocalHttpClient pauser = client("bob");
		assertThat(nonManager.isWorkFlowManager(projectId), is(false));
		pauser.pauseServer();
		try {
			ServerDocument serverDocument = nonManager.openProject(projectId);
			final VersionedOWLOntology vont = nonManager.buildVersionedOntology(serverDocument, owlManager, projectId);
			nonManager.getLatestChanges(vont, projectId);
		} finally {
			pauser.resumeServer();
		}
	}

	@Test
	public void pauseIndependentOfCreate() throws Exception {
		final LocalHttpClient client = client("bob");
		client.pauseServer();
		try {
			getAdmin().deleteProject(createPizzaProject(), true);
		} finally {
			client.resumeServer();
		}
	}

	void pauseAndCommitFails(String pauserName, String committerName) throws Exception {
		final ProjectId projectId = createPizzaProject();
		final LocalHttpClient pauser = client(pauserName);
		final LocalHttpClient committer = client(committerName);
		pauser.pauseServer();
		try {
			ServerDocument serverDocument = committer.openProject(projectId);
			VersionedOWLOntology vont = committer.buildVersionedOntology(serverDocument, owlManager, projectId);
			List<OWLOntologyChange> cs = getOwlOntologyChanges(vont.getOntology());

			owlManager.applyChanges(cs);

			histManager.logChanges(cs);

			List<OWLOntologyChange> changes = ClientUtils.getUncommittedChanges(
				histManager, vont.getOntology(), vont.getChangeHistory());
			Commit commit = ClientUtils.createCommit(committer, "Add customer subclass of domain concept", changes);
			DocumentRevision commitBaseRevision = vont.getHeadRevision();
			CommitBundle commitBundle = new CommitBundleImpl(commitBaseRevision, commit);
			committer.commit(projectId, commitBundle);
		} finally {
			getAdmin().deleteProject(projectId, true);
			pauser.resumeServer();
		}
	}

	@Test(expected = ServiceUnavailableException.class)
	public void pauseAndCommitDifferentUsersFails() throws Exception {
		pauseAndCommitFails("alice", "bob");
	}

	@Test
	public void pauseAndCommitSameUser() throws Exception {
		pauseAndCommitFails("bob", "bob");
	}

	@Before
	public void serversStartsUnpaused() throws ClientRequestException {
		assertThat("Server is already paused", client("bob").getServerStatus().paused(), is(false));
	}

	@Test
	public void statusAfterPause() throws ClientRequestException, AuthorizationException {
		LocalHttpClient client = client("bob");
		client.pauseServer();
		try {
			assertThat(client.getServerStatus().paused(), is(true));
		} finally {
			client.resumeServer();
		}
	}
}
