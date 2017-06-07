package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.ProjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class OpenProjectTest extends BaseTest {

    private ProjectId projectId;

    @Before
    public void createProject() throws Exception {
        projectId = createPizzaProject();
    }

    @Test
    public void shouldDownloadRemoteChanges() throws Exception {
        /*
         * Login as Guest
         */
        LocalHttpClient guest = client("guest");
        
        ServerDocument serverDocument = guest.openProject(projectId);
        VersionedOWLOntology vont = guest.buildVersionedOntology(serverDocument, owlManager, projectId);
        ChangeHistory changeHistoryFromClient = vont.getChangeHistory();
        
			Utils.assertChangeHistoryEmpty(changeHistoryFromClient, "The local change history should be empty");
			ChangeHistory changeHistoryFromServer = guest.getAllChanges(vont.getServerDocument(), projectId);
			Utils.assertChangeHistoryEmpty(changeHistoryFromServer, "The remote change history should be empty");
    }

	@Test
    public void shouldConstructOntology() throws Exception {
        /*
         * Login as Guest
         */
        LocalHttpClient guest = client("guest");
        
        ServerDocument serverDocument = guest.openProject(projectId);
        VersionedOWLOntology vont = guest.buildVersionedOntology(serverDocument, owlManager, projectId);

		// Assert the produced ontology
        OWLOntology originalOntology = owlManager.getOntology(IRI.create(PizzaOntology.getId()));
        assertThat(vont.getOntology(), is(originalOntology));
        assertThat(vont.getOntology().getSignature(), is(originalOntology.getSignature()));
        assertThat(vont.getOntology().getAxiomCount(), is(originalOntology.getAxiomCount()));
        assertThat(vont.getOntology().getAxioms(), is(originalOntology.getAxioms()));
    }

    @After
    public void removeProject() throws Exception {
        getAdmin().deleteProject(projectId, true);
    }
}
