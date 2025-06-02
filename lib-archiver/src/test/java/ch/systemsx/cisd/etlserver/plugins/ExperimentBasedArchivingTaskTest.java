/*
 * Copyright ETH 2011 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.etlserver.plugins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsAnything;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.ISearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.PermIdSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.ArchivingStatus;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.PhysicalData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel;
import ch.systemsx.cisd.base.tests.AbstractFileSystemTestCase;
import ch.systemsx.cisd.common.filesystem.HostAwareFile;
import ch.systemsx.cisd.common.filesystem.IFreeSpaceProvider;
import ch.systemsx.cisd.common.logging.BufferedAppender;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogRecordingUtils;
import ch.systemsx.cisd.common.test.AssertionUtil;
import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;
import ch.systemsx.cisd.openbis.generic.shared.dto.SimpleDataSetInformationDTO;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.ProjectIdentifier;

/**
 * @author Franz-Josef Elmer
 */
public class ExperimentBasedArchivingTaskTest extends AbstractFileSystemTestCase
{
    public static class MockFreeSpaceProvider implements IFreeSpaceProvider
    {

        static IFreeSpaceProvider mock;

        @Override
        public long freeSpaceKb(HostAwareFile path) throws IOException
        {
            return mock.freeSpaceKb(path);
        }
    }

    private static final String LOG_ENTRY_PREFIX_TEMPLATE =
            "INFO  %s.ExperimentBasedArchivingTask - ";

    private static final String ERROR_LOG_ENTRY_PREFIX_TEMPLATE =
            "ERROR %s.ExperimentBasedArchivingTask - ";

    private static final String LOG_ENTRY_PREFIX = String.format(LOG_ENTRY_PREFIX_TEMPLATE,
            LogCategory.OPERATION);

    private static final String NOTIFY_LOG_ENTRY_PREFIX = String.format(LOG_ENTRY_PREFIX_TEMPLATE,
            LogCategory.NOTIFY);

    private static final String NOTIFY_ERROR_LOG_ENTRY_PREFIX = String.format(
            ERROR_LOG_ENTRY_PREFIX_TEMPLATE, LogCategory.NOTIFY);

    private static final String FREE_SPACE_BELOW_THRESHOLD_LOG_ENTRY = LOG_ENTRY_PREFIX
            + "Free space is below threshold, searching for datasets to archive.";

    private static final String FREE_SPACE_LOG_ENTRY = LOG_ENTRY_PREFIX
            + "Free space: 99 MB, minimal free space required: 100 MB";

    private static final String FREE_SPACE_LOG_ENTRY2 = LOG_ENTRY_PREFIX
            + "Free space: 90 MB, minimal free space required: 100 MB";

    private static final String LOCATION_PREFIX = "abc/";

    private BufferedAppender logRecorder;

    private Mockery context;

    private IConfigProvider configProvider;

    private IOpenBISService service;

    private IApplicationServerApi applicationServerApi;

    private IFreeSpaceProvider freeSpaceProvider;

    private ExperimentBasedArchivingTask task;

    private Properties properties;

    private File monitoredDir;

    private Experiment e1;

    private Experiment e2;

    private Experiment e3;

    private DataSet lockedDataSet;

    private DataSet dataSetOfIgnoredType;

    private DataSet dataSetWithNoModificationDate;

    private DataSet oldDataSet;

    private DataSet middleOldDataSet;

    private DataSet youngDataSet;

    private DataSet veryYoungDataSet;

    @BeforeMethod
    public void before()
    {
        logRecorder = LogRecordingUtils.createRecorder("%-5p %c - %m%n", Level.INFO);
        context = new Mockery();
        configProvider = context.mock(IConfigProvider.class);
        service = context.mock(IOpenBISService.class);
        applicationServerApi = context.mock(IApplicationServerApi.class);
        freeSpaceProvider = context.mock(IFreeSpaceProvider.class);
        MockFreeSpaceProvider.mock = freeSpaceProvider;

        context.checking(new Expectations()
        {
            {
                allowing(configProvider).getDataStoreKind();
                will(returnValue(DataStoreKind.DSS));

                allowing(service).getSessionToken();
                will(returnValue("test-session-token"));
            }
        });

        task = new ExperimentBasedArchivingTask(configProvider, service, applicationServerApi);
        assertEquals(true, task.requiresDataStoreLock());

        e1 = new Experiment();
        e1.setPermId(new ExperimentPermId("E1"));
        e1.setIdentifier(new ExperimentIdentifier("/S/P/E1"));
        e2 = new Experiment();
        e2.setPermId(new ExperimentPermId("E2"));
        e2.setIdentifier(new ExperimentIdentifier("/S/P/E2"));
        e3 = new Experiment();
        e3.setPermId(new ExperimentPermId("E3"));
        e3.setIdentifier(new ExperimentIdentifier("/S/P/E3"));

        lockedDataSet = dataSet("A", "lockedDataSet", new Date(100), ArchivingStatus.LOCKED);
        dataSetOfIgnoredType = dataSet("ABC", "dataSetOfIgnoredType", new Date(100), ArchivingStatus.AVAILABLE);
        dataSetWithNoModificationDate = dataSet("A", "dataSetWithNoModificationDate", null, ArchivingStatus.AVAILABLE);
        oldDataSet = dataSet("A", "oldDataSet", new Date(300), ArchivingStatus.AVAILABLE);
        middleOldDataSet = dataSet("A", "middleOldDataSet", new Date(400), ArchivingStatus.AVAILABLE);
        youngDataSet = dataSet("A", "youngDataSet", new Date(1000), ArchivingStatus.AVAILABLE);
        veryYoungDataSet = dataSet("A", "veryYoungDataSet", new Date(2000), ArchivingStatus.AVAILABLE);

        properties = new Properties();
        properties.setProperty(ExperimentBasedArchivingTask.MINIMUM_FREE_SPACE_KEY, "100");
        properties.setProperty(ExperimentBasedArchivingTask.EXCLUDED_DATA_SET_TYPES_KEY, "ABC, B");
        monitoredDir = new File(workingDirectory, "/some/dir");
        monitoredDir.mkdirs();
        properties.setProperty(ExperimentBasedArchivingTask.MONITORED_DIR,
                monitoredDir.getAbsolutePath());
        properties.setProperty(ExperimentBasedArchivingTask.FREE_SPACE_PROVIDER_PREFIX + "class",
                MockFreeSpaceProvider.class.getName());
    }

    private DataSet dataSet(String typeCode, String code, Date modificationDate, ArchivingStatus status)
    {
        DataSetType type = new DataSetType();
        type.setCode(typeCode);

        PhysicalData physicalData = new PhysicalData();
        physicalData.setStatus(status);
        physicalData.setLocation(LOCATION_PREFIX + code);

        DataSet dataSet = new DataSet();
        dataSet.setCode(code);
        dataSet.setType(type);
        dataSet.setPhysicalData(physicalData);
        dataSet.setRegistrationDate(new Date(100));
        dataSet.setModificationDate(modificationDate);

        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withType();
        fetchOptions.withPhysicalData();
        dataSet.setFetchOptions(fetchOptions);

        return dataSet;
    }

    @AfterMethod
    public void afterMethod()
    {
        logRecorder.reset();
        // The following line of code should also be called at the end of each test method.
        // Otherwise one do not known which test failed.
        context.assertIsSatisfied();
    }

    @Test
    public void testOngoingArchiving()
    {
        prepareDefaultDataSetSize(20L);
        prepareListPhysicalDataSetsInStatusArchivingPending(10);

        task.setUp("", properties);
        task.execute();

        assertEquals("INFO  OPERATION.ExperimentBasedArchivingTask - Does nothing because there are 10 "
                + "data sets in status ARCHIVE_PENDING.", logRecorder.getLogContent());
        context.assertIsSatisfied();
    }

    @Test
    public void testLockedDataSet()
    {
        prepareListPhysicalDataSetsInStatusArchivingPending(0);
        prepareFreeSpaceProvider(99L);
        prepareListExperiments(e1);
        prepareListDataSetsOf(e1, oldDataSet, lockedDataSet, youngDataSet);
        prepareTypeBySize(lockedDataSet, 100L);
        prepareTypeBySize(oldDataSet, 20L);
        prepareTypeBySize(youngDataSet, 10L);
        prepareArchivingDataSets(oldDataSet, youngDataSet);

        task.setUp("", properties);
        task.execute();

        checkLog(logEntry(e1, oldDataSet, youngDataSet));
        context.assertIsSatisfied();
    }

    @Test
    public void testArchiveExperiments()
    {
        prepareListPhysicalDataSetsInStatusArchivingPending(0);
        prepareFreeSpaceProvider(99L);
        prepareListExperiments(e1, e2);
        prepareListDataSetsOf(e1);
        prepareListDataSetsOf(e2);

        task.setUp("", properties);
        task.execute();

        checkLog();
        context.assertIsSatisfied();
    }

    @Test
    public void testArchiveExperimentContainingDataSetsOfAnyType()
    {
        properties.remove(ExperimentBasedArchivingTask.EXCLUDED_DATA_SET_TYPES_KEY);
        prepareListPhysicalDataSetsInStatusArchivingPending(0);
        prepareFreeSpaceProvider(99L);
        prepareListExperiments(e1);
        prepareListDataSetsOf(e1, dataSetOfIgnoredType, youngDataSet);
        prepareDefaultDataSetSize(20L);
        prepareTypeBySize(youngDataSet, 10L);
        prepareArchivingDataSets(dataSetOfIgnoredType, youngDataSet);

        task.setUp("", properties);
        task.execute();

        checkLog(false, false, logEntry(e1, dataSetOfIgnoredType, youngDataSet));
        context.assertIsSatisfied();
    }

    @Test
    public void testArchiveExperimentContainingDataSetsOfTypeToBeIgnored()
    {
        prepareListPhysicalDataSetsInStatusArchivingPending(0);
        prepareFreeSpaceProvider(99L);
        prepareListExperiments(e1);
        prepareListDataSetsOf(e1, dataSetOfIgnoredType, youngDataSet);
        prepareTypeBySize(youngDataSet, 10L);
        prepareArchivingDataSets(youngDataSet);

        task.setUp("", properties);
        task.execute();

        checkLog(logEntry(e1, youngDataSet));
        context.assertIsSatisfied();
    }

    @Test
    public void testCalculatingExperimentAgeWhereSomeDataSetsAreAlreadyArchived()
    {
        prepareListPhysicalDataSetsInStatusArchivingPending(0);
        prepareFreeSpaceProvider(99L);
        prepareListExperiments(e1, e2);
        prepareListDataSetsOf(e1, oldDataSet, youngDataSet, veryYoungDataSet);
        prepareTypeBySize(oldDataSet, 10L);
        youngDataSet.getPhysicalData().setStatus(ArchivingStatus.ARCHIVE_PENDING);
        veryYoungDataSet.getPhysicalData().setStatus(ArchivingStatus.ARCHIVED);
        prepareListDataSetsOf(e2, middleOldDataSet);
        prepareTypeBySize(middleOldDataSet, 10L);
        prepareArchivingDataSets(oldDataSet);
        prepareArchivingDataSets(middleOldDataSet);

        task.setUp("", properties);
        task.execute();

        checkLog(logEntry(e1, oldDataSet), logEntry(e2, middleOldDataSet));
        context.assertIsSatisfied();
    }

    @Test
    public void testArchiveExperimentsUntilThereIsEnoughFreeSpace()
    {
        prepareListPhysicalDataSetsInStatusArchivingPending(0);
        prepareFreeSpaceProvider(99L);
        prepareListExperiments(e1, e2);
        prepareListDataSetsOf(e1, veryYoungDataSet);
        prepareListDataSetsOf(e2, oldDataSet, youngDataSet);
        prepareTypeBySize(oldDataSet, 500L);
        prepareTypeBySize(youngDataSet, 700L);
        prepareArchivingDataSets(oldDataSet, youngDataSet);

        task.setUp("", properties);
        task.execute();

        checkLog(logEntry(e2, oldDataSet, youngDataSet));
        context.assertIsSatisfied();
    }

    @Test
    public void testArchiveAllExperiments()
    {
        prepareListPhysicalDataSetsInStatusArchivingPending(0);
        prepareFreeSpaceProvider(90L);
        prepareListExperiments(e3, e1, e2);
        prepareListDataSetsOf(e3, lockedDataSet, dataSetWithNoModificationDate);
        prepareListDataSetsOf(e1, veryYoungDataSet);
        prepareListDataSetsOf(e2, oldDataSet, youngDataSet);
        lockedDataSet.getPhysicalData().setSize(1000L);
        prepareTypeBySize(dataSetWithNoModificationDate, 100L);
        prepareTypeBySize(oldDataSet, 500L);
        prepareTypeBySize(youngDataSet, 700L);
        prepareArchivingDataSets(dataSetWithNoModificationDate);
        prepareArchivingDataSets(oldDataSet, youngDataSet);
        prepareArchivingDataSets(veryYoungDataSet);
        prepareDefaultDataSetSize(5000L);

        task.setUp("", properties);
        task.execute();

        checkLog(false, true, logEntry(e3, dataSetWithNoModificationDate),
                logEntry(e2, oldDataSet, youngDataSet), logEntry(e1, veryYoungDataSet));
        context.assertIsSatisfied();
    }

    @Test
    public void testArchiveDataSetsWithNoSizeEstimates()
    {
        prepareListPhysicalDataSetsInStatusArchivingPending(0);
        prepareFreeSpaceProvider(90L);
        prepareListExperiments(e1);
        prepareListDataSetsOf(e1, veryYoungDataSet, oldDataSet, youngDataSet);
        lockedDataSet.getPhysicalData().setSize(1000L);
        prepareTypeBySize(oldDataSet, 500L);
        youngDataSet.getPhysicalData().setSize(700L);
        prepareArchivingDataSets(veryYoungDataSet, oldDataSet, youngDataSet);

        task.setUp("", properties);
        task.execute();

        checkLog(true, true, Arrays.asList("A"), logEntry(e1, veryYoungDataSet, oldDataSet, youngDataSet));
        context.assertIsSatisfied();
    }

    @Test
    public void testArchiveDataSetsWithSizeNotUsingEstimates()
    {
        prepareListPhysicalDataSetsInStatusArchivingPending(0);
        prepareFreeSpaceProvider(99L);
        prepareListExperiments(e1, e2);
        prepareListDataSetsOf(e1, veryYoungDataSet);
        prepareListDataSetsOf(e2, oldDataSet, youngDataSet);
        prepareTypeBySize(oldDataSet, 500L);
        youngDataSet.setType(oldDataSet.getType());
        youngDataSet.getPhysicalData().setSize(100 * FileUtils.ONE_MB);
        prepareArchivingDataSets(oldDataSet, youngDataSet);

        task.setUp("", properties);
        task.execute();

        checkLog(logEntry(e2, oldDataSet, youngDataSet));
        context.assertIsSatisfied();
    }

    @Test
    public void testArchiveExperimentsInCorrectOrderWhereTheOldestDataSetHasNoModificationDate()
    {
        prepareListPhysicalDataSetsInStatusArchivingPending(0);
        prepareFreeSpaceProvider(99L);
        prepareListExperiments(e1, e2);
        prepareListDataSetsOf(e1, youngDataSet);
        prepareListDataSetsOf(e2, dataSetWithNoModificationDate);
        prepareArchivingDataSets(youngDataSet);
        prepareArchivingDataSets(dataSetWithNoModificationDate);
        prepareTypeBySize(youngDataSet, 700L);
        prepareTypeBySize(dataSetWithNoModificationDate, 500L);

        task.setUp("", properties);
        task.execute();

        checkLog(logEntry(e2, dataSetWithNoModificationDate), logEntry(e1, youngDataSet));
        context.assertIsSatisfied();
    }

    private void checkLog(String... archivingEntries)
    {
        checkLog(true, false, archivingEntries);
    }

    private void checkLog(boolean noDefault, boolean free90mb, String... archivingEntries)
    {
        checkLog(noDefault, free90mb, Arrays.<String>asList(), archivingEntries);
    }

    private void checkLog(boolean noDefault, boolean free90mb,
            List<String> dataSetsWithMissingEstimates, String... archivingEntries)
    {
        StringBuilder operationLogBuilder = new StringBuilder();
        if (noDefault)
        {
            operationLogBuilder.append("WARN  OPERATION.ExperimentBasedArchivingTask - "
                    + "No default estimated data set size specified.\n");
        }
        operationLogBuilder.append(free90mb ? FREE_SPACE_LOG_ENTRY2 : FREE_SPACE_LOG_ENTRY);
        operationLogBuilder.append('\n');
        operationLogBuilder.append(FREE_SPACE_BELOW_THRESHOLD_LOG_ENTRY);
        StringBuilder notifyMessageBuilder = new StringBuilder();
        for (String entry : archivingEntries)
        {
            operationLogBuilder.append("\n").append(LOG_ENTRY_PREFIX).append(entry);
            notifyMessageBuilder.append("\n").append(entry);
        }
        if (dataSetsWithMissingEstimates.size() > 0)
        {
            operationLogBuilder.append("\n").append(NOTIFY_ERROR_LOG_ENTRY_PREFIX);
            if (dataSetsWithMissingEstimates.size() > 0)
            {
                operationLogBuilder
                        .append("Failed to estimate the avarage size for the following data set types: ");
                operationLogBuilder.append(dataSetsWithMissingEstimates);
                operationLogBuilder
                        .append("\nPlease, configure the maintenance task with a property " +
                                "'estimated-data-set-size-in-KB.<data set type>' " +
                                "for each of these data set types. Alternatively, the property " +
                                "'estimated-data-set-size-in-KB.DEFAULT' can be specified.");
            }
        }
        if (archivingEntries.length > 0)
        {
            operationLogBuilder.append("\n").append(NOTIFY_LOG_ENTRY_PREFIX);
            operationLogBuilder.append("Archiving summary:").append(
                    notifyMessageBuilder.toString().replaceAll("Starting archiving ", "Archived "));
        }
        AssertionUtil.assertContainsLines(operationLogBuilder.toString(), logRecorder.getLogContent());
    }

    private String logEntry(Experiment experiment, DataSet... dataSets)
    {
        List<String> dataSetCodes = getDataSetCodes(dataSets);
        return "Starting archiving #" + dataSetCodes.size() + " data sets of experiment "
                + experiment.getIdentifier() + ": " + dataSetCodes;
    }

    private void prepareFreeSpaceProvider(final long freeSpace)
    {
        prepareFreeSpaceProvider(monitoredDir, freeSpace);
    }

    private void prepareFreeSpaceProvider(final File dir, final long freeSpace)
    {
        context.checking(new Expectations()
        {
            {
                try
                {
                    File absoluteFile = new File(dir.getAbsolutePath());
                    one(freeSpaceProvider).freeSpaceKb(new HostAwareFile(absoluteFile));
                    will(returnValue(FileUtils.ONE_KB * freeSpace));
                } catch (IOException ex)
                {
                    throw CheckedExceptionTunnel.wrapIfNecessary(ex);
                }

            }
        });
    }

    private void prepareListExperiments(final Experiment... experiments)
    {
        context.checking(new Expectations()
        {
            {
                SearchResult<Experiment> searchResult = new SearchResult<>(List.of(experiments), experiments.length);
                one(applicationServerApi).searchExperiments(with(any(String.class)), with(any(ExperimentSearchCriteria.class)), with(any(
                        ExperimentFetchOptions.class)));
                will(returnValue(searchResult));
            }
        });
    }

    private void prepareListDataSetsOf(final Experiment experiment, final DataSet... dataSets)
    {
        Matcher<DataSetSearchCriteria> experimentPermIdMatcher = new BaseMatcher<DataSetSearchCriteria>()
        {
            @Override public boolean matches(final Object object)
            {
                if (object instanceof DataSetSearchCriteria)
                {
                    for (ISearchCriteria dataSetCriterion : ((DataSetSearchCriteria) object).getCriteria())
                    {
                        if (dataSetCriterion instanceof ExperimentSearchCriteria)
                        {
                            for (ISearchCriteria experimentCriterion : ((ExperimentSearchCriteria) dataSetCriterion).getCriteria())
                            {
                                if (experimentCriterion instanceof PermIdSearchCriteria)
                                {
                                    String permIdValue = ((PermIdSearchCriteria) experimentCriterion).getFieldValue().getValue();
                                    if (permIdValue.equals(experiment.getPermId().getPermId()))
                                    {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }

                return false;
            }

            @Override public void describeTo(final Description description)
            {
            }
        };

        context.checking(new Expectations()
        {
            {
                SearchResult<DataSet> searchResult = new SearchResult<>(List.of(dataSets), dataSets.length);
                one(applicationServerApi).searchDataSets(with(any(String.class)), with(experimentPermIdMatcher),
                        with(any(DataSetFetchOptions.class)));
                will(returnValue(searchResult));
            }
        });
    }

    private void prepareListPhysicalDataSetsInStatusArchivingPending(int numberOfArchivePending)
    {
        context.checking(new Expectations()
        {
            {
                SimpleDataSetInformationDTO dataSet = new SimpleDataSetInformationDTO();
                one(service).listPhysicalDataSetsByArchivingStatus(DataSetArchivingStatus.ARCHIVE_PENDING, null);
                will(returnValue(Collections.nCopies(numberOfArchivePending, dataSet)));
            }
        });
    }

    private void prepareDefaultDataSetSize(long defaultSize)
    {
        properties.put(ExperimentBasedArchivingTask.DATA_SET_SIZE_PREFIX
                + ExperimentBasedArchivingTask.DEFAULT_DATA_SET_TYPE, "" + defaultSize);
    }

    private void prepareTypeBySize(DataSet dataSet, long dataSetSize)
    {
        String dataSetType = "DS_TYPE_" + dataSetSize;
        dataSet.getType().setCode(dataSetType);
        properties.put(ExperimentBasedArchivingTask.DATA_SET_SIZE_PREFIX + dataSetType,
                String.valueOf(dataSetSize));
    }

    private void prepareArchivingDataSets(DataSet... dataSets)
    {
        final List<String> dataSetCodes = getDataSetCodes(dataSets);
        context.checking(new Expectations()
        {
            {
                one(service).archiveDataSets(with(dataSetCodes), with(true), with(new IsAnything<Map<String, String>>()));
            }
        });
    }

    private List<String> getDataSetCodes(DataSet... dataSets)
    {
        final List<String> dataSetCodes = new ArrayList<String>();
        for (DataSet dataSet : dataSets)
        {
            dataSetCodes.add(dataSet.getCode());
        }
        return dataSetCodes;
    }

    private ProjectIdentifier getProjectIdentifier(Experiment e)
    {
        return new ProjectIdentifier(e.getProject().getSpace().getCode(), e.getProject().getCode());
    }

}
