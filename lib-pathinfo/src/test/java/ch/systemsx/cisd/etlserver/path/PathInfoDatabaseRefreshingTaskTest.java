/*
 * Copyright ETH 2017 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.etlserver.path;

import static ch.systemsx.cisd.etlserver.path.PathInfoDatabaseRefreshingTask.CHUNK_SIZE_KEY;
import static ch.systemsx.cisd.etlserver.path.PathInfoDatabaseRefreshingTask.STATE_FILE_KEY;
import static ch.systemsx.cisd.etlserver.path.PathInfoDatabaseRefreshingTask.TIME_STAMP_OF_YOUNGEST_DATA_SET_KEY;

import java.io.File;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Level;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.ArchivingStatus;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.PhysicalData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.pathinfo.IPathInfoNonAutoClosingDAO;
import ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel;
import ch.systemsx.cisd.base.tests.AbstractFileSystemTestCase;
import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.logging.BufferedAppender;
import ch.systemsx.cisd.common.logging.LogInitializer;
import ch.systemsx.cisd.common.test.AssertionUtil;
import ch.systemsx.cisd.common.test.RecordingMatcher;
import ch.systemsx.cisd.etlserver.IPathInfoServiceProvider;
import ch.systemsx.cisd.etlserver.PathInfoServiceProviderAdapter;
import ch.systemsx.cisd.etlserver.PathInfoServiceProviderFactory;
import ch.systemsx.cisd.etlserver.plugins.AbstractMaintenanceTaskWithStateFile;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.DefaultFileBasedHierarchicalContentFactory;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.IHierarchicalContentFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DatasetV3Location;

/**
 * @author Franz-Josef Elmer
 */
public class PathInfoDatabaseRefreshingTaskTest extends AbstractFileSystemTestCase
{
    private static final String LOG_PREFIX = "INFO  OPERATION.PathInfoDatabaseRefreshingTask - ";

    private static final String T1 = "2017-01-21 15:42:47";

    private static final String T2 = "2017-01-21 15:42:48";

    private static final String T3 = "2017-01-21 15:42:49";

    private static final String T4 = "2017-01-23 15:42:48";

    private static final String T5 = "2017-01-24 15:42:48";

    private BufferedAppender logRecorder;

    private Mockery context;

    private IApplicationServerApi applicationServerApi;

    private IOpenBISService service;

    private IPathInfoNonAutoClosingDAO dao;

    private IHierarchicalContentFactory contentFactory;

    private IDataSetDirectoryProvider directoryProvider;

    private PathInfoDatabaseRefreshingTask task;

    private File store;

    private IShareIdManager shareIdManager;

    private IPathInfoServiceProvider originalProvider;

    @BeforeMethod
    public void beforeMethod()
    {
        LogInitializer.init();
        logRecorder = new BufferedAppender("%-5p %c - %m%n", Level.INFO);
        context = new Mockery();
        applicationServerApi = context.mock(IApplicationServerApi.class);
        service = context.mock(IOpenBISService.class);
        dao = context.mock(IPathInfoNonAutoClosingDAO.class);
        contentFactory = new DefaultFileBasedHierarchicalContentFactory();
        directoryProvider = context.mock(IDataSetDirectoryProvider.class);
        shareIdManager = context.mock(IShareIdManager.class);
        store = new File(workingDirectory, "store");
        store.mkdir();
        context.checking(new Expectations()
        {
            {
                allowing(directoryProvider).getStoreRoot();
                will(returnValue(store));

                allowing(directoryProvider).getShareIdManager();
                will(returnValue(shareIdManager));

                allowing(service).getSessionToken();
                will(returnValue("testSessionToken"));
            }
        });

        originalProvider = PathInfoServiceProviderFactory.getInstance();
        PathInfoServiceProviderFactory.setInstance(new PathInfoServiceProviderAdapter()
        {
            @Override public IOpenBISService getOpenBISService()
            {
                return service;
            }
        });

        task = new PathInfoDatabaseRefreshingTask(applicationServerApi, dao, contentFactory, directoryProvider);
    }

    @AfterMethod
    public void tearDown(Method method)
    {
        PathInfoServiceProviderFactory.setInstance(originalProvider);

        try
        {
            context.assertIsSatisfied();
        } catch (Throwable t)
        {
            // assert expectations were met, including the name of the failed method
            throw new Error(method.getName() + "() : ", t);
        }
    }

    @Test
    public void testNoProperties()
    {
        try
        {
            task.setUp(null, new Properties());
            fail("ConfigurationFailureException expected");
        } catch (ConfigurationFailureException ex)
        {
            assertEquals("Either property '" + TIME_STAMP_OF_YOUNGEST_DATA_SET_KEY + "' is defined or '"
                    + createDefaultStateFile().getAbsolutePath() + "' exists.", ex.getMessage());
        }
    }

    @Test
    public void testInvalidTimeStampOfYoungestDataSetProperty()
    {
        Properties properties = new Properties();
        properties.setProperty(TIME_STAMP_OF_YOUNGEST_DATA_SET_KEY, "abcde");
        try
        {
            task.setUp(null, properties);
            fail("ConfigurationFailureException expected");
        } catch (ConfigurationFailureException ex)
        {
            assertEquals("Invalid property '" + TIME_STAMP_OF_YOUNGEST_DATA_SET_KEY + "': abcde", ex.getMessage());
        }
    }

    @Test
    public void testStateFileIsADirectory()
    {
        Properties properties = new Properties();
        properties.setProperty(STATE_FILE_KEY, store.getPath());
        try
        {
            task.setUp(null, properties);
            fail("ConfigurationFailureException expected");
        } catch (ConfigurationFailureException ex)
        {
            assertEquals("File '" + store.getAbsolutePath() + "' (specified by property '"
                    + STATE_FILE_KEY + "') is a directory.", ex.getMessage());
        }
    }

    @Test
    public void testAllDataSetTypesFirstRun()
    {
        Properties properties = new Properties();
        properties.setProperty(TIME_STAMP_OF_YOUNGEST_DATA_SET_KEY, T4);
        task.setUp(null, properties);

        DataSet ds1 = newPhysicalDataSet("archived", null, ArchivingStatus.ARCHIVED, asDate(T1));
        DataSet ds2 = newContainerDataSet("container", asDate(T2));
        DataSet ds3 = newPhysicalDataSet("ds-3", "1/ds-3", ArchivingStatus.AVAILABLE, asDate(T2));
        DataSet ds4 = newPhysicalDataSet("too-young", null, ArchivingStatus.AVAILABLE, asDate(T5));
        DataSet ds5 = newPhysicalDataSet("ds-5", "1/ds-5", ArchivingStatus.AVAILABLE, asDate(T3));

        RecordingMatcher<DataSetSearchCriteria> criteriaMatcher = prepareGetPhysicalDataSets(ds1, ds2, ds3, ds4, ds5);
        prepareDeleteAndLockDataSet(ds3);
        prepareDeleteAndLockDataSet(ds5);

        task.execute();

        assertEquals("DATASET\n    with attribute 'registration_date' earlier than or equal to '2017-01-23 15:42:48'\n",
                criteriaMatcher.recordedObject().toString());
        AssertionUtil.assertContainsLines(LOG_PREFIX + "Refresh path info for 2 physical data sets.\n" +
                        LOG_PREFIX + "Paths inside data set ds-5 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Paths inside data set ds-3 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Path info for 2 physical data sets refreshed in 0 secs.",
                logRecorder.getLogContent());
        File stateFile = createDefaultStateFile();
        assertEquals(T2 + " [ds-3]", FileUtilities.loadToString(stateFile).trim());
    }

    @Test
    public void testDataSetTypeAndSecondRun()
    {
        Properties properties = new Properties();
        properties.setProperty(TIME_STAMP_OF_YOUNGEST_DATA_SET_KEY, T5);
        File stateFile = new File(store, "ts.txt");
        FileUtilities.writeToFile(stateFile, T3);
        properties.setProperty(STATE_FILE_KEY, stateFile.getPath());
        properties.setProperty(PathInfoDatabaseRefreshingTask.DATA_SET_TYPE_KEY, "A");
        task.setUp(null, properties);

        DataSet ds1 = newPhysicalDataSet("ds-1", "1/ds-1", ArchivingStatus.AVAILABLE, asDate(T1));
        DataSet ds2 = newPhysicalDataSet("ds-2", "1/ds-2", ArchivingStatus.AVAILABLE, asDate(T2));
        DataSet ds3 = newPhysicalDataSet("too-young", null, ArchivingStatus.AVAILABLE, asDate(T4));

        RecordingMatcher<DataSetSearchCriteria> criteriaMatcher = prepareGetPhysicalDataSets(ds1, ds2, ds3);
        prepareDeleteAndLockDataSet(ds1);
        prepareDeleteAndLockDataSet(ds2);

        task.execute();

        assertEquals("DATASET\n    with operator 'AND'\n"
                        + "    with attribute 'registration_date' earlier than or equal to '2017-01-21 15:42:49'\n"
                        + "    with data_set_type:\n"
                        + "        with attribute 'code' equal to 'A'\n",
                criteriaMatcher.recordedObject().toString());
        AssertionUtil.assertContainsLines(LOG_PREFIX + "Refresh path info for 2 physical data sets.\n" +
                        LOG_PREFIX + "Paths inside data set ds-2 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Paths inside data set ds-1 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Path info for 2 physical data sets refreshed in 0 secs.",
                logRecorder.getLogContent());
        assertEquals(T1 + " [ds-1]", FileUtilities.loadToString(stateFile).trim());
    }

    @Test
    public void testChunkSize()
    {
        Properties properties = new Properties();
        properties.setProperty(TIME_STAMP_OF_YOUNGEST_DATA_SET_KEY, T4);
        properties.setProperty(CHUNK_SIZE_KEY, "2");
        task.setUp(null, properties);

        DataSet ds1 = newPhysicalDataSet("ds-1", "1/ds-1", ArchivingStatus.AVAILABLE, asDate(T1));
        DataSet ds2 = newPhysicalDataSet("ds-2", "1/ds-2", ArchivingStatus.AVAILABLE, asDate(T2));
        DataSet ds3 = newPhysicalDataSet("ds-3", "1/ds-3", ArchivingStatus.AVAILABLE, asDate(T3));

        RecordingMatcher<DataSetSearchCriteria> criteriaMatcher = prepareGetPhysicalDataSets(ds1, ds2, ds3);
        prepareDeleteAndLockDataSet(ds2);
        prepareDeleteAndLockDataSet(ds3);

        task.execute();

        assertEquals("DATASET\n    with attribute 'registration_date' earlier than or equal to '2017-01-23 15:42:48'\n",
                criteriaMatcher.recordedObject().toString());
        AssertionUtil.assertContainsLines(LOG_PREFIX + "Refresh path info for 2 physical data sets.\n" +
                        LOG_PREFIX + "Paths inside data set ds-3 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Paths inside data set ds-2 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Path info for 2 physical data sets refreshed in 0 secs.",
                logRecorder.getLogContent());
        File stateFile = createDefaultStateFile();
        assertEquals(T2 + " [ds-2]", FileUtilities.loadToString(stateFile).trim());
    }

    @Test
    public void testExecuteThreeTimes()
    {
        Properties properties = new Properties();
        properties.setProperty(TIME_STAMP_OF_YOUNGEST_DATA_SET_KEY, T4);
        properties.setProperty(CHUNK_SIZE_KEY, "2");
        task.setUp(null, properties);

        DataSet ds1 = newPhysicalDataSet("ds-1", "1/ds-1", ArchivingStatus.AVAILABLE, asDate(T1));
        DataSet ds2 = newPhysicalDataSet("ds-2", "1/ds-2", ArchivingStatus.AVAILABLE, asDate(T2));
        DataSet ds3 = newPhysicalDataSet("ds-3", "1/ds-3", ArchivingStatus.AVAILABLE, asDate(T3));

        RecordingMatcher<DataSetSearchCriteria> criteriaMatcher = prepareGetPhysicalDataSets(ds1, ds2, ds3);
        prepareDeleteAndLockDataSet(ds2);
        prepareDeleteAndLockDataSet(ds3);

        task.execute();

        assertEquals("DATASET\n    with attribute 'registration_date' earlier than or equal to '2017-01-23 15:42:48'\n",
                criteriaMatcher.recordedObject().toString());
        AssertionUtil.assertContainsLines(LOG_PREFIX + "Refresh path info for 2 physical data sets.\n" +
                        LOG_PREFIX + "Paths inside data set ds-3 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Paths inside data set ds-2 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Path info for 2 physical data sets refreshed in 0 secs.",
                logRecorder.getLogContent());
        File stateFile = createDefaultStateFile();
        assertEquals(T2 + " [ds-2]", FileUtilities.loadToString(stateFile).trim());

        criteriaMatcher = prepareGetPhysicalDataSets(ds1, ds2, ds3);
        prepareDeleteAndLockDataSet(ds1);

        task.execute();

        assertEquals("DATASET\n    with attribute 'registration_date' earlier than or equal to '" + T2 + "'\n",
                criteriaMatcher.recordedObject().toString());
        AssertionUtil.assertContainsLines(LOG_PREFIX + "Refresh path info for 2 physical data sets.\n" +
                        LOG_PREFIX + "Paths inside data set ds-3 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Paths inside data set ds-2 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Path info for 2 physical data sets refreshed in 0 secs.\n" +
                        LOG_PREFIX + "Refresh path info for 1 physical data sets.\n" +
                        LOG_PREFIX + "Paths inside data set ds-1 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Path info for 1 physical data sets refreshed in 0 secs.",
                logRecorder.getLogContent());
        assertEquals(T1 + " [ds-1]", FileUtilities.loadToString(stateFile).trim());

        criteriaMatcher = prepareGetPhysicalDataSets(ds1, ds2, ds3);

        task.execute();

        assertEquals("DATASET\n    with attribute 'registration_date' earlier than or equal to '" + T1 + "'\n",
                criteriaMatcher.recordedObject().toString());
        AssertionUtil.assertContainsLines(LOG_PREFIX + "Refresh path info for 2 physical data sets.\n" +
                        LOG_PREFIX + "Paths inside data set ds-3 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Paths inside data set ds-2 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Path info for 2 physical data sets refreshed in 0 secs.\n" +
                        LOG_PREFIX + "Refresh path info for 1 physical data sets.\n" +
                        LOG_PREFIX + "Paths inside data set ds-1 successfully added to database. Data set size: 0\n" +
                        LOG_PREFIX + "Path info for 1 physical data sets refreshed in 0 secs.",
                logRecorder.getLogContent());
        assertEquals(T1 + " [ds-1]", FileUtilities.loadToString(stateFile).trim());
    }

    private void prepareDeleteAndLockDataSet(final DataSet dataSet)
    {
        final String dataSetCode = dataSet.getCode();
        final File file = new File(store, dataSet.getPhysicalData().getLocation());
        file.mkdirs();
        context.checking(new Expectations()
        {
            {
                one(dao).deleteDataSet(dataSetCode);

                one(dao).tryGetDataSetId(dataSetCode);
                will(returnValue(null));

                one(dao).createDataSet(dataSetCode, dataSet.getPhysicalData().getLocation());
                will(returnValue((long) dataSet.hashCode()));

                one(dao).createDataSetFile(dataSet.hashCode(), null, "", dataSetCode, 0L, true, null, null, new Date(file.lastModified()));

                one(dao).commit();

                one(shareIdManager).lock(with(any(UUID.class)), with(dataSetCode));

                one(directoryProvider).getDataSetDirectory(with(new BaseMatcher<DatasetV3Location>()
                                                                {

                                                                    @Override
                                                                    public boolean matches(Object arg0)
                                                                    {
                                                                        if (arg0 instanceof DatasetV3Location)
                                                                        {
                                                                            DatasetV3Location location = (DatasetV3Location) arg0;
                                                                            return Objects.equals(dataSet.getCode(), location.getDataSetCode());
                                                                        }
                                                                        return false;
                                                                    }

                                                                    @Override
                                                                    public void describeTo(Description arg0)
                                                                    {
                                                                        arg0.appendText(dataSet.getCode());
                                                                    }
                                                                }
                ));
                will(returnValue(file));

                one(shareIdManager).releaseLocks(with(any(UUID.class)));
            }
        });
    }

    private RecordingMatcher<DataSetSearchCriteria> prepareGetPhysicalDataSets(final DataSet... dataSets)
    {
        final RecordingMatcher<DataSetSearchCriteria> recordingMatcher = new RecordingMatcher<DataSetSearchCriteria>();

        context.checking(new Expectations()
        {
            {
                one(applicationServerApi).searchDataSets(with(any(String.class)), with(recordingMatcher), with(any(DataSetFetchOptions.class)));
                will(returnValue(new SearchResult<>(List.of(dataSets), dataSets.length)));
            }
        });
        return recordingMatcher;
    }

    private Date asDate(String dateString)
    {
        try
        {
            return new SimpleDateFormat(AbstractMaintenanceTaskWithStateFile.TIME_STAMP_FORMAT).parse(dateString);
        } catch (ParseException ex)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(ex);
        }
    }

    private File createDefaultStateFile()
    {
        return new File(store, PathInfoDatabaseRefreshingTask.class.getSimpleName() + "-state.txt");
    }

    private DataSet newPhysicalDataSet(String code, String location, ArchivingStatus status, Date registrationDate)
    {
        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withPhysicalData();

        PhysicalData physicalData = new PhysicalData();
        physicalData.setStatus(status);
        physicalData.setLocation(location);
        physicalData.setH5Folders(false);
        physicalData.setH5arFolders(false);

        DataSet dataSet = new DataSet();
        dataSet.setCode(code);
        dataSet.setRegistrationDate(registrationDate);
        dataSet.setPhysicalData(physicalData);
        dataSet.setFetchOptions(fetchOptions);

        return dataSet;
    }

    private DataSet newContainerDataSet(String code, Date registrationDate)
    {
        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withPhysicalData();

        DataSet dataSet = new DataSet();
        dataSet.setCode(code);
        dataSet.setRegistrationDate(registrationDate);
        dataSet.setFetchOptions(fetchOptions);

        return dataSet;
    }

}
