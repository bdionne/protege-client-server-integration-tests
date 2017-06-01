package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.PlainPassword;
import edu.stanford.protege.metaproject.api.UserId;
import org.junit.Test;
import org.protege.editor.owl.client.LocalHttpClient;

/**
 * Created by rgrinberg on 6/1/17.
 */
public class PauseServerTest extends BaseTest {

	@Test
	public void unpauseServerSucceeds() throws Exception {
		LocalHttpClient client = login(f.getUserId("bob"), f.getPlainPassword("bob"));
		client.pauseServer();
		client.resumeServer();
	}
}
