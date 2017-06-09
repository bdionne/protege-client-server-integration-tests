package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.*;
import org.junit.After;
import org.junit.Test;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;

import java.net.URI;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class LargeProjectTest extends BaseTest {
	private ProjectId projectId;

	@Test
	public void createLargeProject() throws Exception {
		projectId = f.getProjectId("BiomedGT-" + System.currentTimeMillis());
		Name projectName = f.getName("NCI Thesaurus");
		Description description = f.getDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
		UserId owner = f.getUserId("root");

		Optional<ProjectOptions> options = Optional.ofNullable(null);

		Project proj = f.getProject(projectId, projectName, description, owner, options);

		ServerDocument serverDocument = getAdmin().createProject(proj, LargeOntology.getResource());

		Utils.assertServerDocument(client("bob"), serverDocument, projectId);
	}

	@After
	public void shouldDownloadRemoteChanges() throws Exception {
		LocalHttpClient guest = client("guest");

		ServerDocument serverDocument = guest.openProject(projectId);
		VersionedOWLOntology vont = guest.buildVersionedOntology(serverDocument, owlManager, projectId);
		Utils.assertChangeHistoryEmpty(vont.getChangeHistory(), "The local change history should be empty");
		ChangeHistory changeHistoryFromServer = guest.getAllChanges(vont.getServerDocument(), projectId);
		Utils.assertChangeHistoryEmpty(changeHistoryFromServer, "The remote change history should be empty");

		getAdmin().deleteProject(projectId, true);
	}


}
