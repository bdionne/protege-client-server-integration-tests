package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.ConfigurationManager;
import edu.stanford.protege.metaproject.api.*;
import org.apache.log4j.BasicConfigurator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.model.history.HistoryManagerImpl;
import org.protege.editor.owl.server.http.HTTPServer;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.*;

public abstract class BaseTest {

	protected static final String ADMIN_SERVER_ADDRESS = "http://localhost:8081";
	protected static final String SERVER_ADDRESS = "http://localhost:8080";

	protected static final DocumentRevision R0 = DocumentRevision.START_REVISION;
	protected static final DocumentRevision R1 = DocumentRevision.create(1);

	public static final String ONTOLOGY_ID = PizzaOntology.getId() + "#";
	public static final OWLClass DOMAIN_CONCEPT = Class(IRI(ONTOLOGY_ID, "DomainConcept"));
	public static final OWLClass CUSTOMER = Class(IRI(ONTOLOGY_ID, "Customer"));

	protected static PolicyFactory f = ConfigurationManager.getFactory();
	protected OWLOntologyManager owlManager;
	protected HistoryManagerImpl histManager;
	private static HTTPServer httpServer = null;
	private LocalHttpClient admin;

	static class PizzaOntology {
		static final String getId() {
			return "http://www.co-ode.org/ontologies/pizza/pizza.owl";
		}

		static final File getResource() {
			try {
				return new File(NewProjectTest.class.getResource("/pizza.owl").toURI());
			} catch (URISyntaxException e) {
				throw new OWLRuntimeException("File not found", e);
			}
		}
	}

	static final File largeOntologyResource() {
		try {
			return new File(NewProjectTest.class.getResource("/thesaurus.owl.zip").toURI());
		} catch (URISyntaxException e) {
			throw new OWLRuntimeException("File not found", e);
		}
	}

	public LocalHttpClient client(String user) {
		UserId userId = f.getUserId(user);
		PlainPassword password = f.getPlainPassword("guest".equals(user) || "root".equals(user) ? user + "pwd" : user);
		try {
			return login(userId, password);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void setup() throws Exception {
		owlManager = OWLManager.createOWLOntologyManager();
		histManager = new HistoryManagerImpl(owlManager);
		admin = new LocalHttpClient(f.getUserId("bob").get(), f.getPlainPassword("bob").getPassword(),
			ADMIN_SERVER_ADDRESS);
	}


	@BeforeClass
	public static void startServer() throws Exception {
		File f = new File(BaseTest.class.getResource("/server-configuration.json").toURI());

		BasicConfigurator.configure();
		httpServer = new HTTPServer(f.getAbsolutePath());
		httpServer.start();
	}


	protected LocalHttpClient getAdmin() {
		return admin;
	}

	protected static LocalHttpClient login(UserId userId, PlainPassword password) throws Exception {
		return new LocalHttpClient(userId.get(), password.getPassword(), SERVER_ADDRESS);
	}

	@AfterClass
	public static void stopServer() throws Exception {
		httpServer.stop();
	}

	protected ProjectId createPizzaProject() {
		ProjectId projectId = f.getProjectId("pizza-" + System.currentTimeMillis()); // currentTimeMilis() for uniqueness
		Name projectName = f.getName("Pizza Project");
		Description description = f.getDescription("Pizza project! " + System.currentTimeMillis());
		UserId owner = f.getUserId("bob");
		Optional<ProjectOptions> options = Optional.ofNullable(null);

		Project proj = f.getProject(projectId, projectName, description, owner, options);
		try {
			getAdmin().createProject(proj, PizzaOntology.getResource());
		} catch (AuthorizationException | ClientRequestException e) {
			throw new RuntimeException(e);
		}
		return projectId;
	}

	public static List<OWLOntologyChange> getOwlOntologyChanges(OWLOntology workingOntology) {
		List<OWLOntologyChange> cs = new ArrayList<OWLOntologyChange>();
		cs.add(new AddAxiom(workingOntology, Declaration(CUSTOMER)));
		cs.add(new AddAxiom(workingOntology, SubClassOf(CUSTOMER, DOMAIN_CONCEPT)));
		return cs;
	}

}
