package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.ProjectId;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Created by rgrinberg on 6/7/17.
 */
public class Utils {
	protected static void assertChangeHistoryEmpty(ChangeHistory changeHistory, String message) {
		assertThat(message, changeHistory.isEmpty());
		MatcherAssert.assertThat(changeHistory.getBaseRevision(), Matchers.is(BaseTest.R0));
		MatcherAssert.assertThat(changeHistory.getHeadRevision(), Matchers.is(BaseTest.R0));
		assertThat(changeHistory.getMetadata().size(), is(0));
		assertThat(changeHistory.getRevisions().size(), is(0));
	}

	static void assertChangeHistoryNotEmpty(ChangeHistory changeHistory, String message, int changesForRevision) {
		assertThat(message, !changeHistory.isEmpty());
		MatcherAssert.assertThat(changeHistory.getBaseRevision(), Matchers.is(BaseTest.R0));
		MatcherAssert.assertThat(changeHistory.getHeadRevision(), Matchers.is(BaseTest.R1));
		assertThat(changeHistory.getMetadata().size(), is(1));
		assertThat(changeHistory.getRevisions().size(), is(1));
		assertThat(changeHistory.getChangesForRevision(BaseTest.R1).size(), is(changesForRevision));
	}

	static void assertServerDocument(LocalHttpClient login, ServerDocument serverDocument, ProjectId projectId)
		throws AuthorizationException, ClientRequestException {
		assertThat(serverDocument, is(notNullValue()));
		assertThat(serverDocument.getServerAddress(), is(URI.create(BaseTest.SERVER_ADDRESS)));
		assertThat(serverDocument.getHistoryFile(), is(notNullValue()));

		ChangeHistory remoteChangeHistory = login.getAllChanges(serverDocument, projectId);
		assertChangeHistoryEmpty(remoteChangeHistory, "The remote change history should be empty");
	}
}
