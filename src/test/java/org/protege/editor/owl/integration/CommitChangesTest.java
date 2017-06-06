package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.IRI;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.*;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class CommitChangesTest extends BaseTest {

    private static final String ONTOLOGY_ID = PizzaOntology.getId() + "#";

    private static final OWLClass DOMAIN_CONCEPT = Class(IRI(ONTOLOGY_ID, "DomainConcept"));
    private static final OWLClass CUSTOMER = Class(IRI(ONTOLOGY_ID, "Customer"));
    private static final OWLClass MEAT_TOPPING = Class(IRI(ONTOLOGY_ID, "MeatTopping"));

    private ProjectId projectId;

    private LocalHttpClient guest;
    private LocalHttpClient manager;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void createProject() throws Exception {
        /*
         * User inputs part
         */
        projectId = f.getProjectId("pizza-" + System.currentTimeMillis()); // currentTimeMilis() for uniqueness
        Name projectName = f.getName("Pizza Project");
        Description description = f.getDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        UserId owner = f.getUserId("bob");
        Optional<ProjectOptions> options = Optional.ofNullable(null);
        
        Project proj = f.getProject(projectId, projectName, description, owner, options);
       
        getAdmin().createProject(proj, PizzaOntology.getResource());
    }

    private VersionedOWLOntology openProjectAsManager() throws Exception {
        this.manager = client("bob");
        ServerDocument serverDocument = manager.openProject(projectId);
        return manager.buildVersionedOntology(serverDocument, owlManager, projectId);
    }

    private VersionedOWLOntology openProjectAsGuest() throws Exception {
        guest = client("guest");
        ServerDocument serverDocument = guest.openProject(projectId);
        return guest.buildVersionedOntology(serverDocument, owlManager, projectId);
    }

    @Test
    public void shouldCommitAddition() throws Exception {
        VersionedOWLOntology vont = openProjectAsManager();
        OWLOntology workingOntology = vont.getOntology();
        
        /*
         * Simulates user edits over a working ontology (add axioms)
         */
        List<OWLOntologyChange> cs = new ArrayList<>();
        cs.add(new AddAxiom(workingOntology, Declaration(CUSTOMER)));
        cs.add(new AddAxiom(workingOntology, SubClassOf(CUSTOMER, DOMAIN_CONCEPT)));
        
        owlManager.applyChanges(cs);
        
        histManager.logChanges(cs);
        
        
        
        /*
         * Prepare the commit bundle
         */
        List<OWLOntologyChange> changes = ClientUtils.getUncommittedChanges(histManager, vont.getOntology(), vont.getChangeHistory());
        Commit commit = ClientUtils.createCommit(getAdmin(), "Add customer subclass of domain concept", changes);
        DocumentRevision commitBaseRevision = vont.getHeadRevision();
        CommitBundle commitBundle = new CommitBundleImpl(commitBaseRevision, commit);
        
        /*
         * Do commit
         */
        ChangeHistory approvedChanges = manager.commit(projectId, commitBundle);
        
        /*
         * Update local history
         */
        vont.update(approvedChanges);

        ChangeHistory changeHistoryFromClient = vont.getChangeHistory();

        // Assert the local change history
        assertThat("The local change history should not be empty", !changeHistoryFromClient.isEmpty());
        assertThat(changeHistoryFromClient.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromClient.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromClient.getMetadata().size(), is(1));
        assertThat(changeHistoryFromClient.getRevisions().size(), is(1));
        assertThat(changeHistoryFromClient.getChangesForRevision(R1).size(), is(2));
        
        ChangeHistory changeHistoryFromServer = ((LocalHttpClient) manager).getAllChanges(vont.getServerDocument(), projectId);
        
        // Assert the remote change history
        assertThat("The remote change history should not be empty", !changeHistoryFromServer.isEmpty());
        assertThat(changeHistoryFromServer.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromServer.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromServer.getMetadata().size(), is(1));
        assertThat(changeHistoryFromServer.getRevisions().size(), is(1));
        assertThat(changeHistoryFromServer.getChangesForRevision(R1).size(), is(2));
    }

    @Test
    public void shouldCommitDeletion() throws Exception {
        VersionedOWLOntology vont = openProjectAsManager();
        OWLOntology workingOntology = vont.getOntology();
        
        /*
         * Simulates user edits over a working ontology (remove a class and its references)
         */
        Set<OWLAxiom> axiomsToRemove = new HashSet<>();
        for (OWLAxiom ax : workingOntology.getAxioms()) {
            if (ax.getSignature().contains(MEAT_TOPPING)) {
                axiomsToRemove.add(ax);
            }
            if (ax instanceof OWLAnnotationAssertionAxiom) {
                OWLAnnotationAssertionAxiom asa = (OWLAnnotationAssertionAxiom) ax;
                OWLAnnotationSubject subject = asa.getSubject();
                if (subject instanceof IRI && subject.equals(MEAT_TOPPING.getIRI())) {
                    axiomsToRemove.add(ax);
                }
                OWLAnnotationValue value = asa.getValue();
                if (value instanceof IRI && value.equals(MEAT_TOPPING.getIRI())) {
                    axiomsToRemove.add(ax);
                }
            }
        }
        owlManager.removeAxioms(workingOntology, axiomsToRemove);
        
        List<OWLOntologyChange> cs = new ArrayList<OWLOntologyChange>();
        for (OWLAxiom ax : axiomsToRemove) {
        	cs.add(new RemoveAxiom(workingOntology, ax));
        	
        }
        
        histManager.logChanges(cs);
        
        /*
         * Prepare the commit bundle
         */
        List<OWLOntologyChange> changes = ClientUtils.getUncommittedChanges(histManager, vont.getOntology(), vont.getChangeHistory());
        Commit commit = ClientUtils.createCommit(getAdmin(), "Remove MeatTopping and its references", changes);
        DocumentRevision commitBaseRevision = vont.getHeadRevision();
        CommitBundle commitBundle = new CommitBundleImpl(commitBaseRevision, commit);
        
        /*
         * Do commit
         */
        ChangeHistory approvedChanges = manager.commit(projectId, commitBundle);
        
        /*
         * Update local history
         */
        vont.update(approvedChanges);

        ChangeHistory changeHistoryFromClient = vont.getChangeHistory();

        // Assert the local change history
        assertThat("The local change history should not be empty", !changeHistoryFromClient.isEmpty());
        assertThat(changeHistoryFromClient.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromClient.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromClient.getMetadata().size(), is(1));
        assertThat(changeHistoryFromClient.getRevisions().size(), is(1));
        assertThat(changeHistoryFromClient.getChangesForRevision(R1).size(), is(16));
        
        ChangeHistory changeHistoryFromServer = ((LocalHttpClient) manager).getAllChanges(vont.getServerDocument(), projectId);
        
        // Assert the remote change history
        assertThat("The remote change history should not be empty", !changeHistoryFromServer.isEmpty());
        assertThat(changeHistoryFromServer.getBaseRevision(), is(R0));
        assertThat(changeHistoryFromServer.getHeadRevision(), is(R1));
        assertThat(changeHistoryFromServer.getMetadata().size(), is(1));
        assertThat(changeHistoryFromServer.getRevisions().size(), is(1));
        assertThat(changeHistoryFromServer.getChangesForRevision(R1).size(), is(16));
    }

    @Test
    public void shouldNotCommitChange() throws Exception {
        VersionedOWLOntology vont = openProjectAsGuest();
        OWLOntology workingOntology = vont.getOntology();
        
        /*
         * Simulates user edits over a working ontology (add axioms)
         */
        List<OWLOntologyChange> cs = new ArrayList<OWLOntologyChange>();
        cs.add(new AddAxiom(workingOntology, Declaration(CUSTOMER)));
        cs.add(new AddAxiom(workingOntology, SubClassOf(CUSTOMER, DOMAIN_CONCEPT)));
        
        owlManager.applyChanges(cs);
        
        histManager.logChanges(cs);
       
        
        /*
         * Prepare the commit bundle
         */
        List<OWLOntologyChange> changes = ClientUtils.getUncommittedChanges(histManager, vont.getOntology(), vont.getChangeHistory());
        Commit commit = ClientUtils.createCommit(guest, "Add customer subclass of domain concept", changes);
        DocumentRevision commitBaseRevision = vont.getHeadRevision();
        CommitBundle commitBundle = new CommitBundleImpl(commitBaseRevision, commit);
        
        thrown.expect(ClientRequestException.class);
        //thrown.expectCause(new CauseMatcher(OperationNotAllowedException.class,
               // "User has no permission for 'Add axiom' operation"));
        
        /*
         * Do commit
         */
        guest.commit(projectId, commitBundle);
    }

    @After
    public void removeProject() throws Exception {
        getAdmin().deleteProject(projectId, true);
    }
}
