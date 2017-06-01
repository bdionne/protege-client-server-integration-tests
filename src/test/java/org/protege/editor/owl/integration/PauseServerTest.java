package org.protege.editor.owl.integration;

import org.junit.Test;
import org.protege.editor.owl.client.LocalHttpClient;

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

	@Test(expected = Exception.class)
	public void pauseLocksToUser() throws Exception {
		client("bob").pauseServer();
		client("alice").resumeServer();
	}
}
