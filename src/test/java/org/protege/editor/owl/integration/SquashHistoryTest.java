package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.*;
import org.protege.editor.owl.client.*;
import org.protege.editor.owl.client.util.*;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.*;
import org.protege.editor.owl.server.versioning.*;
import org.protege.editor.owl.server.api.*;
import org.protege.editor.owl.server.policy.*;
import org.protege.editor.owl.server.util.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.IRI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.*;


public class SquashHistoryTest extends BaseTest {
	private static final String ONTOLOGY_ID = PizzaOntology.getId() + "#";

	private static final OWLClass DOMAIN_CONCEPT = Class(IRI(ONTOLOGY_ID, "DomainConcept"));
	private static final OWLClass CUSTOMER = Class(IRI(ONTOLOGY_ID, "Customer"));

	private ProjectId projectId;
	private LocalHttpClient client;

	@Before
	public void createProject() throws Exception {
		projectId = f.getProjectId("pizza-" + System.currentTimeMillis()); // currentTimeMilis() for uniqueness
		Name projectName = f.getName("Pizza Project");
		Description description = f.getDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit");
		UserId owner = f.getUserId("bob");
		Optional<ProjectOptions> options = Optional.ofNullable(null);

		Project proj = f.getProject(projectId, projectName, description, owner, options);

		getAdmin().createProject(proj, PizzaOntology.getResource());
	}

	private VersionedOWLOntology openProjectAsManager() throws Exception {
		client = client("bob");
		ServerDocument serverDocument = client.openProject(projectId);
		return client.buildVersionedOntology(serverDocument, owlManager, projectId);
	}

	@Test
	public void shouldSquashHistory() throws Exception {
		VersionedOWLOntology vont = openProjectAsManager();
		OWLOntology ontology = vont.getOntology();

		List<OWLOntologyChange> cs = new ArrayList<>();
		cs.add(new AddAxiom(ontology, Declaration(CUSTOMER)));
		cs.add(new AddAxiom(ontology, SubClassOf(CUSTOMER, DOMAIN_CONCEPT)));

		owlManager.applyChanges(cs);

		List<OWLOntologyChange> changes = ClientUtils.getUncommittedChanges(histManager, vont.getOntology(), vont.getChangeHistory());
		Commit commit = ClientUtils.createCommit(getAdmin(), "Add customer subclass of domain concept", changes);

		CommitBundle commitBundle = new CommitBundleImpl(vont.getHeadRevision(), commit);

		vont.update(client.commit(projectId, commitBundle));

		ChangeHistory clientHistory = vont.getChangeHistory();
		ChangeHistory serverHistory = client.getAllChanges(vont.getServerDocument());

		assertThat(clientHistory.getHeadRevision(), is(R1));
		assertThat(serverHistory.getHeadRevision(), is(R1));

		// Squash
		SnapShot clientSnapShot = new SnapShot(ontology);
		client.squashHistory(clientSnapShot, projectId);

		serverHistory = client.getAllChanges(vont.getServerDocument());

		assertThat(serverHistory.getHeadRevision(), is(R0));

		SnapShot serverSnapShot = client.getSnapShot(projectId);

		assertThat(     clientSnapShot.getOntology().getGeneralClassAxioms(),
				equalTo(serverSnapShot.getOntology().getGeneralClassAxioms()));

		assertThat(     clientSnapShot.getOntology().getSignature(),
				equalTo(serverSnapShot.getOntology().getSignature()));
	}

}

