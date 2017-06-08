package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.ProjectId;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IRI;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 *         Stanford Center for Biomedical Informatics Research
 */
public class CommitChangesTest extends BaseTest {

	private static final OWLClass MEAT_TOPPING = Class(IRI(ONTOLOGY_ID, "MeatTopping"));

	private ProjectId projectId;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void createProject() throws Exception {
		projectId = createPizzaProject();
	}

	private VersionedOWLOntology openProjectAsManager(LocalHttpClient client) throws Exception {
		ServerDocument serverDocument = client.openProject(projectId);
		return client.buildVersionedOntology(serverDocument, owlManager, projectId);
	}

	@Test
	public void shouldCommitAddition() throws Exception {
		LocalHttpClient client = client("bob");
		VersionedOWLOntology vont = openProjectAsManager(client);

		List<OWLOntologyChange> cs = getOwlOntologyChanges(vont.getOntology());
		owlManager.applyChanges(cs);
		histManager.logChanges(cs);

		CommitBundle commitBundle = commitBundle(getAdmin(), vont, "Add customer subclass of domain concept");
		ChangeHistory approvedChanges = client.commit(projectId, commitBundle);
		vont.update(approvedChanges);

		assertThat("The local change history should not be empty", !vont.getChangeHistory().isEmpty());
		Utils.assertChangeHistoryNotEmpty(vont.getChangeHistory(),
			"The local change history should not be empty", 2);
		ChangeHistory changeHistoryFromServer = client.getAllChanges(vont.getServerDocument(), projectId);
		Utils.assertChangeHistoryNotEmpty(changeHistoryFromServer,
			"The remote change history should not be empty", 2);
	}

	@Test
	public void shouldCommitDeletion() throws Exception {
		LocalHttpClient client = client("bob");
		VersionedOWLOntology vont = openProjectAsManager(client);

		Set<OWLAxiom> axiomsToRemove = new HashSet<>();
		for (OWLAxiom ax : vont.getOntology().getAxioms()) {
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
		owlManager.removeAxioms(vont.getOntology(), axiomsToRemove);

		List<OWLOntologyChange> cs = new ArrayList<OWLOntologyChange>();
		for (OWLAxiom ax : axiomsToRemove) {
			cs.add(new RemoveAxiom(vont.getOntology(), ax));

		}

		histManager.logChanges(cs);
		CommitBundle commitBundle = commitBundle(getAdmin(), vont, "Remove MeatTopping and its references");
		ChangeHistory approvedChanges = client.commit(projectId, commitBundle);
		vont.update(approvedChanges);

		Utils.assertChangeHistoryNotEmpty(vont.getChangeHistory(), "The local change history should not be empty", 16);
		ChangeHistory changeHistoryFromServer = client.getAllChanges(vont.getServerDocument(), projectId);
		Utils.assertChangeHistoryNotEmpty(changeHistoryFromServer, "The remote change history should not be empty", 16);
	}

	@Test
	public void shouldNotCommitChange() throws Exception {
		LocalHttpClient guest = client("guest");
		ServerDocument serverDocument = guest.openProject(projectId);
		VersionedOWLOntology vont = guest.buildVersionedOntology(serverDocument, owlManager, projectId);
		OWLOntology workingOntology = vont.getOntology();

		List<OWLOntologyChange> cs = getOwlOntologyChanges(workingOntology);
		owlManager.applyChanges(cs);
		histManager.logChanges(cs);

		CommitBundle commitBundle = commitBundle(guest, vont, "Add customer subclass of domain concept");

		thrown.expect(ClientRequestException.class);
		guest.commit(projectId, commitBundle);
	}

	private CommitBundle commitBundle(LocalHttpClient guest, VersionedOWLOntology vont, String comment) {
		List<OWLOntologyChange> changes = ClientUtils.getUncommittedChanges(histManager, vont.getOntology(), vont.getChangeHistory());
		Commit commit = ClientUtils.createCommit(guest, comment, changes);
		DocumentRevision commitBaseRevision = vont.getHeadRevision();
		return new CommitBundleImpl(commitBaseRevision, commit);
	}

	@After
	public void removeProject() throws Exception {
		getAdmin().deleteProject(projectId, true);
	}
}
