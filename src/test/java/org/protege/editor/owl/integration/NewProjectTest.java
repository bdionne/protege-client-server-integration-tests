package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.*;
import org.junit.After;
import org.junit.Test;
import org.protege.editor.owl.server.versioning.api.ServerDocument;

import com.google.common.base.Optional;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 *         Stanford Center for Biomedical Informatics Research
 */
public class NewProjectTest extends BaseTest {

	private ProjectId projectId;

	@Test
	public void createNewProject() throws Exception {
		projectId = f.getProjectId("pizza-" + System.currentTimeMillis()); // currentTimeMilis() for uniqueness
		Name projectName = f.getName("Pizza Project");
		Description description = f.getDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
		UserId owner = f.getUserId("root");

		Optional<ProjectOptions> options = Optional.absent();

		Project proj = f.getProject(projectId, projectName, description, owner, options);

		ServerDocument serverDocument = getAdmin().createProject(proj, PizzaOntology.getResource());

		Utils.assertServerDocument(client("bob"), serverDocument, projectId);
	}

	@After
	public void removeProject() throws Exception {
		getAdmin().deleteProject(projectId, true);
	}
}
