package ch.ethz.sis.openbis.generic.asapi.v3.exporter;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class ExportEntityCollectorTest
{

    private static final String SESSION_TOKEN = "test-token";

    private Mockery mockery;

    private IApplicationServerApi api;

    @BeforeMethod
    public void beforeMethod()
    {
        mockery = new Mockery();
        api = mockery.mock(IApplicationServerApi.class);
    }

    @AfterMethod
    public void afterMethod()
    {
        mockery.assertIsSatisfied();
    }

    /**
     * A harvester may be configured with a space that has since been deleted from the source. Resolving the
     * selected (root) space then returns nothing. The traversal must skip it instead of throwing, so the rest
     * of the resource list can still be generated.
     */
    @Test
    public void testMissingRootSpaceIsSkipped()
    {
        mockery.checking(new Expectations()
        {
            {
                allowing(api).getSpaces(with(SESSION_TOKEN),
                        with(Collections.singletonList(new SpacePermId("GONE"))),
                        with(any(SpaceFetchOptions.class)));
                will(returnValue(Collections.emptyMap()));
            }
        });

        final Set<ExportablePermId> collection = new HashSet<>();
        ExportEntityCollector.collectEntities(api, SESSION_TOKEN, collection,
                new ExportablePermId(ExportableKind.SPACE, "GONE"),
                false, true, false, true, false);

        assertEquals(collection, Collections.emptySet());
    }

    /**
     * A referenced entity reached during traversal (here a project belonging to the selected space) may be
     * deleted between selection and resolution. It must be skipped while the root entity is retained.
     */
    @Test
    public void testMissingReferencedProjectIsSkippedAndRootRetained()
    {
        final SpaceFetchOptions spaceFetchOptions = new SpaceFetchOptions();
        spaceFetchOptions.withProjects();

        final Project project = new Project();
        project.setPermId(new ProjectPermId("/S1/P1"));

        final Space space = new Space();
        space.setFetchOptions(spaceFetchOptions);
        space.setPermId(new SpacePermId("S1"));
        space.setProjects(List.of(project));

        mockery.checking(new Expectations()
        {
            {
                allowing(api).getSpaces(with(SESSION_TOKEN),
                        with(Collections.singletonList(new SpacePermId("S1"))),
                        with(any(SpaceFetchOptions.class)));
                will(returnValue(Collections.singletonMap(space.getPermId(), space)));

                // Space samples without a project: none.
                allowing(api).searchSamples(with(SESSION_TOKEN),
                        with(any(SampleSearchCriteria.class)),
                        with(any(SampleFetchOptions.class)));
                will(returnValue(new SearchResult<Sample>(Collections.emptyList(), 0)));

                // The referenced project has been deleted from the source.
                allowing(api).getProjects(with(SESSION_TOKEN),
                        with(Collections.singletonList(new ProjectPermId("/S1/P1"))),
                        with(any(ProjectFetchOptions.class)));
                will(returnValue(Collections.emptyMap()));
            }
        });

        final Set<ExportablePermId> collection = new HashSet<>();
        ExportEntityCollector.collectEntities(api, SESSION_TOKEN, collection,
                new ExportablePermId(ExportableKind.SPACE, "S1"),
                false, true, false, true, false);

        assertEquals(collection, Set.of(new ExportablePermId(ExportableKind.SPACE, "S1")));
    }

}
