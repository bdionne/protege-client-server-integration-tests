package org.protege.editor.owl.integration;

import edu.stanford.protege.metaproject.api.Project;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by rgrinberg on 6/2/17.
 */
public class FetchClassifiableProjects extends BaseTest {
	@Test
	public void classifiedEndpoint() throws Exception {
		List<Project> projects = client("bob").classifiableProjects();
		assertThat(projects, IsEmptyCollection.emptyCollectionOf(Project.class));
	}
}
