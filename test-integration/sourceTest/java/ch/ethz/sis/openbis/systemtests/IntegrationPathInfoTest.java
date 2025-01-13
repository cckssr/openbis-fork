package ch.ethz.sis.openbis.systemtests;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.systemtests.common.AbstractIntegrationTest;
import ch.ethz.sis.pathinfo.IPathInfoAutoClosingDAO;
import net.lemnik.eodsql.QueryTool;

public class IntegrationPathInfoTest extends AbstractIntegrationTest
{

    private static final String ENTITY_CODE_PREFIX = "PATH_INFO_TEST_";

    private static final long WAITING_TIME_FOR_PATH_INFO_FEEDING = 2000L;

    private static final long WAITING_TIME_FOR_PATH_INFO_DELETION = 3000L;

    private IPathInfoAutoClosingDAO pathInfoDAO;

    @BeforeMethod
    public void beforeMethod(Method method) throws Exception
    {
        super.beforeMethod(method);

        deleteLastSeenDeletionFile();

        DatabaseConfiguration pathInfoDatabaseConfiguration = PathInfoDatabaseConfiguration.getInstance(getAfsServerConfiguration());
        pathInfoDAO = QueryTool.getQuery(pathInfoDatabaseConfiguration.getDataSource(), IPathInfoAutoClosingDAO.class);
    }

    @Test
    public void testEntriesGetCreatedAndDeleted() throws Exception
    {
        OpenBIS openBIS = createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        SpacePermId spaceId = new SpacePermId(DEFAULT_SPACE);
        Project project = createProject(openBIS, spaceId, ENTITY_CODE_PREFIX + UUID.randomUUID());

        // create both mutable and immutable experiments and samples
        Experiment mutableExperiment = createExperiment(openBIS, project.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());
        Experiment immutableExperiment = createExperiment(openBIS, project.getPermId(), ENTITY_CODE_PREFIX + UUID.randomUUID());
        Sample mutableSample = createSample(openBIS, spaceId, ENTITY_CODE_PREFIX + UUID.randomUUID());
        Sample immutableSample = createSample(openBIS, spaceId, ENTITY_CODE_PREFIX + UUID.randomUUID());

        Long mutableExperimentEntryId = pathInfoDAO.tryToGetDataSetId(mutableExperiment.getPermId().getPermId());
        Long immutableExperimentEntryId = pathInfoDAO.tryToGetDataSetId(immutableExperiment.getPermId().getPermId());
        Long mutableSampleEntryId = pathInfoDAO.tryToGetDataSetId(mutableSample.getPermId().getPermId());
        Long immutableSampleEntryId = pathInfoDAO.tryToGetDataSetId(immutableSample.getPermId().getPermId());

        assertNull(mutableExperimentEntryId);
        assertNull(immutableExperimentEntryId);
        assertNull(mutableSampleEntryId);
        assertNull(immutableSampleEntryId);

        String testFile = "test-file-" + UUID.randomUUID();
        String testData = "test-content-" + UUID.randomUUID();

        // write data to the experiments and samples
        openBIS.getAfsServerFacade().write(mutableExperiment.getPermId().getPermId(), testFile, 0L, testData.getBytes());
        openBIS.getAfsServerFacade().write(immutableExperiment.getPermId().getPermId(), testFile, 0L, testData.getBytes());
        openBIS.getAfsServerFacade().write(mutableSample.getPermId().getPermId(), testFile, 0L, testData.getBytes());
        openBIS.getAfsServerFacade().write(immutableSample.getPermId().getPermId(), testFile, 0L, testData.getBytes());

        // make chosen data immutable
        makeExperimentImmutable(openBIS, immutableExperiment.getPermId());
        makeSampleImmutable(openBIS, immutableSample.getPermId());

        Thread.sleep(WAITING_TIME_FOR_PATH_INFO_FEEDING);

        // verify that path info entries got created for the immutable data
        mutableExperimentEntryId = pathInfoDAO.tryToGetDataSetId(mutableExperiment.getPermId().getPermId());
        immutableExperimentEntryId = pathInfoDAO.tryToGetDataSetId(immutableExperiment.getPermId().getPermId());
        mutableSampleEntryId = pathInfoDAO.tryToGetDataSetId(mutableSample.getPermId().getPermId());
        immutableSampleEntryId = pathInfoDAO.tryToGetDataSetId(immutableSample.getPermId().getPermId());

        assertNull(mutableExperimentEntryId);
        assertNotNull(immutableExperimentEntryId);
        assertNull(mutableSampleEntryId);
        assertNotNull(immutableSampleEntryId);

        // delete the experiments and samples
        deleteExperiment(openBIS, mutableExperiment.getPermId());
        deleteExperiment(openBIS, immutableExperiment.getPermId());
        deleteSample(openBIS, mutableSample.getPermId());
        deleteSample(openBIS, immutableSample.getPermId());

        Thread.sleep(WAITING_TIME_FOR_PATH_INFO_DELETION);

        // verify that path info entries got deleted
        mutableExperimentEntryId = pathInfoDAO.tryToGetDataSetId(mutableExperiment.getPermId().getPermId());
        immutableExperimentEntryId = pathInfoDAO.tryToGetDataSetId(immutableExperiment.getPermId().getPermId());
        mutableSampleEntryId = pathInfoDAO.tryToGetDataSetId(mutableSample.getPermId().getPermId());
        immutableSampleEntryId = pathInfoDAO.tryToGetDataSetId(immutableSample.getPermId().getPermId());

        assertNull(mutableExperimentEntryId);
        assertNull(immutableExperimentEntryId);
        assertNull(mutableSampleEntryId);
        assertNull(immutableSampleEntryId);
    }

    private void deleteLastSeenDeletionFile() throws Exception
    {
        String lastSeenDeletionFile = OpenBISConfiguration.getInstance(getAfsServerConfiguration()).getOpenBISLastSeenDeletionFile();
        Files.deleteIfExists(Path.of(lastSeenDeletionFile));
    }

}
