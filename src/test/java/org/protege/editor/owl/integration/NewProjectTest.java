package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.*;
import org.junit.After;
import org.junit.Test;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;

import java.net.URI;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class NewProjectTest extends BaseTest {

    private ProjectId projectId;

    @Test
    public void createNewProject() throws Exception {
        /*
         * [GUI] The input project properties
         */
        projectId = f.getProjectId("pizza-" + System.currentTimeMillis()); // currentTimeMilis() for uniqueness
        Name projectName = f.getName("Pizza Project");
        Description description = f.getDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        UserId owner = f.getUserId("root");
        
        Optional<ProjectOptions> options = Optional.ofNullable(null);
        
        Project proj = f.getProject(projectId, projectName, description, owner, options);
       
        ServerDocument serverDocument = getAdmin().createProject(proj, PizzaOntology.getResource());
        
        // Assert the server document
        assertThat(serverDocument, is(notNullValue()));
        assertThat(serverDocument.getServerAddress(), is(URI.create(SERVER_ADDRESS)));
        assertThat(serverDocument.getHistoryFile(), is(notNullValue()));
        
        // Assert the remote change history
			UserId managerId = f.getUserId("bob");
			PlainPassword managerPassword = f.getPlainPassword("bob");
        ChangeHistory remoteChangeHistory = login(managerId, managerPassword).getAllChanges(serverDocument);
        assertThat("The remote change history should be empty", remoteChangeHistory.isEmpty());
        assertThat(remoteChangeHistory.getBaseRevision(), is(R0));
        assertThat(remoteChangeHistory.getHeadRevision(), is(R0));
        assertThat(remoteChangeHistory.getMetadata().size(), is(0));
        assertThat(remoteChangeHistory.getRevisions().size(), is(0));
    }

    @After
    public void removeProject() throws Exception {
        getAdmin().deleteProject(projectId, true);
    }
}
