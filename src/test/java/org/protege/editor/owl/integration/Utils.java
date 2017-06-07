package org.protege.editor.owl.integration;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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

}
