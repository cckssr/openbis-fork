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
package ch.ethz.sis.afsserver.server.pathinfo;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.sis.afs.dto.LockType;
import ch.ethz.sis.afsserver.server.common.IEncapsulatedOpenBISService;
import ch.ethz.sis.afsserver.server.common.ILockManager;
import ch.ethz.sis.afsserver.server.common.SimpleDataSetInformationDTO;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.fetchoptions.FetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.AbstractDateObjectValue;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.AbstractEntitySearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.CodesSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.ImmutableDataDateSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.pathinfo.IPathInfoNonAutoClosingDAO;
import ch.rinn.restrictions.Friend;
import ch.systemsx.cisd.base.tests.AbstractFileSystemTestCase;
import ch.systemsx.cisd.common.logging.LogInitializer;
import ch.systemsx.cisd.common.utilities.MockTimeProvider;

/**
 * @author Franz-Josef Elmer
 */
@Friend(toClasses = PathInfoDatabaseFeedingTask.class)
public class PathInfoDatabaseFeedingTaskTest extends AbstractFileSystemTestCase
{
    private static final File STORE_ROOT =
            new File("src/test/resources/"
                    + PathInfoDatabaseFeedingTaskTest.class.getName().replaceAll("\\.", "/"));

    private static final String DATA_SET_CODE = "ds1";

    private Mockery context;

    private IEncapsulatedOpenBISService service;

    private ILockManager lockManager;

    @Before
    public void beforeMethod()
    {
        LogInitializer.init();
        context = new Mockery();
        service = context.mock(IEncapsulatedOpenBISService.class);
        lockManager = context.mock(ILockManager.class);
    }

    @After
    public void afterMethod()
    {
        context.assertIsSatisfied();
    }

    @Test
    public void testOneChunk()
    {
        final Experiment experiment = new Experiment();
        experiment.setPermId(new ExperimentPermId("ds1"));
        experiment.setImmutableDataDate(new Date(78000));

        final SimpleDataSetInformationDTO dataset = dataSet(experiment.getPermId().getPermId(), experiment.getImmutableDataDate().getTime(), "2");

        context.checking(new Expectations()
        {
            {
                one(service).listExperiments(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(12)));
                will(returnValue(List.of(experiment)));

                one(service).listSamples(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(12)));
                will(returnValue(List.of()));

                one(service).listDataSets(with(criteriaCodes(List.of(experiment.getPermId().getPermId()))), with(fetchOptionsCount(null)));
                will(returnValue(List.of(dataset)));

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset)), with(LockType.HierarchicallyExclusive));

                one(service).updateShareIdAndSize(dataset.getDataSetCode(), dataset.getDataSetShareId(), 3);
            }
        });

        MockPathsInfoDAO pathsInfoDAO = new MockPathsInfoDAO();
        createTask(pathsInfoDAO, 12, 3, 0).execute();

        assertEquals("createDataSet(code=DS1, location=2)\n"
                + "createDataSetFile(0, parent=null, 2 (, 3, d))\n"
                + "createDataSetFiles:\n"
                + "  0, parent=1, hi.txt (hi.txt, 3, f, checksumCRC32=ed6f7a7a)\n"
                + "commit()\n"
                + "deleteLastFeedingEvent()\n"
                + "createLastFeedingEvent(Thu Jan 01 01:01:18 CET 1970)\n"
                + "commit()\n", pathsInfoDAO.getLog());
    }

    @Test
    public void testMultipleChunks()
    {
        final Sample sample1 = new Sample();
        sample1.setPermId(new SamplePermId("DS-1000"));
        sample1.setImmutableDataDate(new Date(1000));

        final Experiment experiment2 = new Experiment();
        experiment2.setPermId(new ExperimentPermId("DS-2000"));
        experiment2.setImmutableDataDate(new Date(2000));

        final Sample sample3 = new Sample();
        sample3.setPermId(new SamplePermId("DS-3000"));
        sample3.setImmutableDataDate(new Date(3000));

        final Experiment experiment4 = new Experiment();
        experiment4.setPermId(new ExperimentPermId("DS-4000"));
        experiment4.setImmutableDataDate(new Date(4000));

        final Sample sample5 = new Sample();
        sample5.setPermId(new SamplePermId("DS-5000"));
        sample5.setImmutableDataDate(new Date(5000));

        final Sample sample6 = new Sample();
        sample6.setPermId(new SamplePermId("DS-6000"));
        sample6.setImmutableDataDate(new Date(6000));

        final SimpleDataSetInformationDTO dataset1 = dataSet(sample1.getPermId().getPermId(), sample1.getImmutableDataDate().getTime());
        final SimpleDataSetInformationDTO dataset2 = dataSet(experiment2.getPermId().getPermId(), experiment2.getImmutableDataDate().getTime());
        final SimpleDataSetInformationDTO dataset3 = dataSet(sample3.getPermId().getPermId(), sample3.getImmutableDataDate().getTime());
        final SimpleDataSetInformationDTO dataset4 = dataSet(experiment4.getPermId().getPermId(), experiment4.getImmutableDataDate().getTime());
        final SimpleDataSetInformationDTO dataset5 = dataSet(sample5.getPermId().getPermId(), sample5.getImmutableDataDate().getTime());
        final SimpleDataSetInformationDTO dataset6 = dataSet(sample6.getPermId().getPermId(), sample6.getImmutableDataDate().getTime());
        final Sequence chunkReadingSequence = context.sequence("chunkReadingSequence");

        context.checking(new Expectations()
        {
            {
                // chunk 1
                one(service).listExperiments(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(2)));
                will(returnValue(List.of(experiment2, experiment4)));
                inSequence(chunkReadingSequence);

                one(service).listSamples(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(2)));
                will(returnValue(List.of(sample1, sample3)));
                inSequence(chunkReadingSequence);

                one(service).listDataSets(with(criteriaCodes(List.of(sample1.getPermId().getPermId(), experiment2.getPermId().getPermId()))),
                        with(fetchOptionsCount(null)));
                will(returnValue(List.of(dataset1, dataset2)));
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset1)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset1)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset1.getDataSetCode(), dataset1.getDataSetShareId(), 16);
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset2)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset2)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset2.getDataSetCode(), dataset2.getDataSetShareId(), 3);
                inSequence(chunkReadingSequence);

                // chunk 2
                one(service).listExperiments(with(criteriaImmutableDate(experiment2.getImmutableDataDate())),
                        with(fetchOptionsCount(2)));
                will(returnValue(List.of(experiment4)));
                inSequence(chunkReadingSequence);

                one(service).listSamples(with(criteriaImmutableDate(experiment2.getImmutableDataDate())), with(fetchOptionsCount(2)));
                will(returnValue(List.of(sample3, sample5)));
                inSequence(chunkReadingSequence);

                one(service).listDataSets(with(criteriaCodes(List.of(sample3.getPermId().getPermId(), experiment4.getPermId().getPermId()))),
                        with(fetchOptionsCount(null)));
                will(returnValue(List.of(dataset3, dataset4)));
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset3)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset3)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset3.getDataSetCode(), dataset3.getDataSetShareId(), 16);
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset4)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset4)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset4.getDataSetCode(), dataset4.getDataSetShareId(), 3);
                inSequence(chunkReadingSequence);

                // chunk 3
                one(service).listExperiments(with(criteriaImmutableDate(experiment4.getImmutableDataDate())),
                        with(fetchOptionsCount(2)));
                will(returnValue(List.of()));
                inSequence(chunkReadingSequence);

                one(service).listSamples(with(criteriaImmutableDate(experiment4.getImmutableDataDate())), with(fetchOptionsCount(2)));
                will(returnValue(List.of(sample5, sample6)));
                inSequence(chunkReadingSequence);

                one(service).listDataSets(with(criteriaCodes(List.of(sample5.getPermId().getPermId(), sample6.getPermId().getPermId()))),
                        with(fetchOptionsCount(null)));
                will(returnValue(List.of(dataset5, dataset6)));
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset5)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset5)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset5.getDataSetCode(), dataset5.getDataSetShareId(), 16);
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset6)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset6)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset6.getDataSetCode(), dataset6.getDataSetShareId(), 3);
                inSequence(chunkReadingSequence);
            }
        });

        MockPathsInfoDAO pathsInfoDAO = new MockPathsInfoDAO();
        createTask(pathsInfoDAO, 2, 3, 0).execute();

        assertEquals("createDataSet(code=DS-1000, location=3)\n"
                + "createDataSetFile(0, parent=null, 3 (, 16, d))\n"
                + "createDataSetFiles:\n"
                + "  0, parent=1, readme.txt (readme.txt, 16, f, checksumCRC32=379d0103)\n"
                + "commit()\n"
                + "createDataSet(code=DS-2000, location=2)\n"
                + "createDataSetFile(2, parent=null, 2 (, 3, d))\n"
                + "createDataSetFiles:\n"
                + "  2, parent=3, hi.txt (hi.txt, 3, f, checksumCRC32=ed6f7a7a)\n"
                + "commit()\n"
                + "deleteLastFeedingEvent()\n"
                + "createLastFeedingEvent(Thu Jan 01 01:00:02 CET 1970)\n"
                + "commit()\n"
                + "createDataSet(code=DS-3000, location=3)\n"
                + "createDataSetFile(4, parent=null, 3 (, 16, d))\n"
                + "createDataSetFiles:\n"
                + "  4, parent=5, readme.txt (readme.txt, 16, f, checksumCRC32=379d0103)\n"
                + "commit()\n"
                + "createDataSet(code=DS-4000, location=2)\n"
                + "createDataSetFile(6, parent=null, 2 (, 3, d))\n"
                + "createDataSetFiles:\n"
                + "  6, parent=7, hi.txt (hi.txt, 3, f, checksumCRC32=ed6f7a7a)\n"
                + "commit()\n"
                + "deleteLastFeedingEvent()\n"
                + "createLastFeedingEvent(Thu Jan 01 01:00:04 CET 1970)\n"
                + "commit()\n"
                + "createDataSet(code=DS-5000, location=3)\n"
                + "createDataSetFile(8, parent=null, 3 (, 16, d))\n"
                + "createDataSetFiles:\n"
                + "  8, parent=9, readme.txt (readme.txt, 16, f, checksumCRC32=379d0103)\n"
                + "commit()\n"
                + "createDataSet(code=DS-6000, location=2)\n"
                + "createDataSetFile(10, parent=null, 2 (, 3, d))\n"
                + "createDataSetFiles:\n"
                + "  10, parent=11, hi.txt (hi.txt, 3, f, checksumCRC32=ed6f7a7a)\n"
                + "commit()\n"
                + "deleteLastFeedingEvent()\n"
                + "createLastFeedingEvent(Thu Jan 01 01:00:06 CET 1970)\n"
                + "commit()\n", pathsInfoDAO.getLog());
    }

    @Test
    public void testChunkGetsExtendedOneChunk()
    {
        final Sample sample1 = new Sample();
        sample1.setPermId(new SamplePermId("ds1"));
        sample1.setImmutableDataDate(new Date(1000));

        final Sample sample2 = new Sample();
        sample2.setPermId(new SamplePermId("ds2"));
        sample2.setImmutableDataDate(new Date(1000));

        final Sample sample3 = new Sample();
        sample3.setPermId(new SamplePermId("ds3"));
        sample3.setImmutableDataDate(new Date(1000));

        final SimpleDataSetInformationDTO dataset1 = dataSet(sample1.getPermId().getPermId(), sample1.getImmutableDataDate().getTime());
        final SimpleDataSetInformationDTO dataset2 = dataSet(sample2.getPermId().getPermId(), sample2.getImmutableDataDate().getTime());
        final SimpleDataSetInformationDTO dataset3 = dataSet(sample3.getPermId().getPermId(), sample3.getImmutableDataDate().getTime());

        final Sequence chunkReadingSequence = context.sequence("chunkReadingSequence");

        context.checking(new Expectations()
        {
            {
                // batch 1
                one(service).listExperiments(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(2)));
                will(returnValue(List.of()));
                inSequence(chunkReadingSequence);

                one(service).listSamples(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(2)));
                will(returnValue(List.of(sample1, sample2)));
                inSequence(chunkReadingSequence);

                one(service).listSamples(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(4)));
                will(returnValue(List.of(sample1, sample2, sample3)));
                inSequence(chunkReadingSequence);

                one(service).listDataSets(with(criteriaCodes(
                                List.of(sample1.getPermId().getPermId(), sample2.getPermId().getPermId(), sample3.getPermId().getPermId()))),
                        with(fetchOptionsCount(null)));
                will(returnValue(List.of(dataset1, dataset2, dataset3)));
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset1)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset1)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset1.getDataSetCode(), dataset1.getDataSetShareId(), 16);
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset2)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset2)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset2.getDataSetCode(), dataset2.getDataSetShareId(), 16);
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset3)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset3)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset3.getDataSetCode(), dataset3.getDataSetShareId(), 16);
                inSequence(chunkReadingSequence);

                // batch 2
                one(service).listExperiments(with(criteriaImmutableDate(sample3.getImmutableDataDate())), with(fetchOptionsCount(2)));
                will(returnValue(List.of()));
                inSequence(chunkReadingSequence);

                one(service).listSamples(with(criteriaImmutableDate(sample3.getImmutableDataDate())), with(fetchOptionsCount(2)));
                will(returnValue(List.of()));
                inSequence(chunkReadingSequence);
            }
        });

        MockPathsInfoDAO pathsInfoDAO = new MockPathsInfoDAO();
        createTask(pathsInfoDAO, 2, 0, 0).execute();

        assertEquals("createDataSet(code=DS1, location=3)\n"
                + "createDataSetFile(0, parent=null, 3 (, 16, d))\n"
                + "createDataSetFiles:\n"
                + "  0, parent=1, readme.txt (readme.txt, 16, f, checksumCRC32=379d0103)\n"
                + "commit()\n"
                + "createDataSet(code=DS2, location=3)\n"
                + "createDataSetFile(2, parent=null, 3 (, 16, d))\n"
                + "createDataSetFiles:\n"
                + "  2, parent=3, readme.txt (readme.txt, 16, f, checksumCRC32=379d0103)\n"
                + "commit()\n"
                + "createDataSet(code=DS3, location=3)\n"
                + "createDataSetFile(4, parent=null, 3 (, 16, d))\n"
                + "createDataSetFiles:\n"
                + "  4, parent=5, readme.txt (readme.txt, 16, f, checksumCRC32=379d0103)\n"
                + "commit()\n"
                + "deleteLastFeedingEvent()\n"
                + "createLastFeedingEvent(Thu Jan 01 01:00:01 CET 1970)\n"
                + "commit()\n", pathsInfoDAO.getLog());
    }

    @Test
    public void testChunkGetsExtendedMultipleChunks()
    {
        final Sample sample1 = new Sample();
        sample1.setPermId(new SamplePermId("ds1"));
        sample1.setImmutableDataDate(new Date(1000));

        final Sample sample2 = new Sample();
        sample2.setPermId(new SamplePermId("ds2"));
        sample2.setImmutableDataDate(new Date(1000));

        final Sample sample3 = new Sample();
        sample3.setPermId(new SamplePermId("ds3"));
        sample3.setImmutableDataDate(new Date(1000));

        final Sample sample4 = new Sample();
        sample4.setPermId(new SamplePermId("ds4"));
        sample4.setImmutableDataDate(new Date(2000));

        final SimpleDataSetInformationDTO dataset1 = dataSet(sample1.getPermId().getPermId(), sample1.getImmutableDataDate().getTime());
        final SimpleDataSetInformationDTO dataset2 = dataSet(sample2.getPermId().getPermId(), sample2.getImmutableDataDate().getTime());
        final SimpleDataSetInformationDTO dataset3 = dataSet(sample3.getPermId().getPermId(), sample3.getImmutableDataDate().getTime());
        final SimpleDataSetInformationDTO dataset4 = dataSet(sample4.getPermId().getPermId(), sample4.getImmutableDataDate().getTime());

        final Sequence chunkReadingSequence = context.sequence("chunkReadingSequence");
        context.checking(new Expectations()
        {
            {
                // batch 1
                one(service).listExperiments(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(2)));
                will(returnValue(List.of()));
                inSequence(chunkReadingSequence);

                one(service).listSamples(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(2)));
                will(returnValue(List.of(sample1, sample2)));
                inSequence(chunkReadingSequence);

                one(service).listSamples(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(4)));
                will(returnValue(List.of(sample1, sample2, sample3, sample4)));
                inSequence(chunkReadingSequence);

                one(service).listDataSets(with(criteriaCodes(
                                List.of(sample1.getPermId().getPermId(), sample2.getPermId().getPermId(), sample3.getPermId().getPermId()))),
                        with(fetchOptionsCount(null)));
                will(returnValue(List.of(dataset1, dataset2, dataset3)));
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset1)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset1)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset1.getDataSetCode(), dataset1.getDataSetShareId(), 16);
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset2)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset2)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset2.getDataSetCode(), dataset2.getDataSetShareId(), 16);
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset3)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset3)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset3.getDataSetCode(), dataset3.getDataSetShareId(), 16);
                inSequence(chunkReadingSequence);

                // batch 2
                one(service).listExperiments(with(criteriaImmutableDate(sample3.getImmutableDataDate())), with(fetchOptionsCount(2)));
                will(returnValue(List.of()));
                inSequence(chunkReadingSequence);

                one(service).listSamples(with(criteriaImmutableDate(sample3.getImmutableDataDate())), with(fetchOptionsCount(2)));
                will(returnValue(List.of(sample4)));
                inSequence(chunkReadingSequence);

                one(service).listDataSets(with(criteriaCodes(List.of(sample4.getPermId().getPermId()))), with(fetchOptionsCount(null)));
                will(returnValue(List.of(dataset4)));
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset4)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset4)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset4.getDataSetCode(), dataset4.getDataSetShareId(), 3);
                inSequence(chunkReadingSequence);
            }
        });

        MockPathsInfoDAO pathsInfoDAO = new MockPathsInfoDAO();
        createTask(pathsInfoDAO, 2, 0, 0).execute();

        assertEquals("createDataSet(code=DS1, location=3)\n"
                + "createDataSetFile(0, parent=null, 3 (, 16, d))\n"
                + "createDataSetFiles:\n"
                + "  0, parent=1, readme.txt (readme.txt, 16, f, checksumCRC32=379d0103)\n"
                + "commit()\n"
                + "createDataSet(code=DS2, location=3)\n"
                + "createDataSetFile(2, parent=null, 3 (, 16, d))\n"
                + "createDataSetFiles:\n"
                + "  2, parent=3, readme.txt (readme.txt, 16, f, checksumCRC32=379d0103)\n"
                + "commit()\n"
                + "createDataSet(code=DS3, location=3)\n"
                + "createDataSetFile(4, parent=null, 3 (, 16, d))\n"
                + "createDataSetFiles:\n"
                + "  4, parent=5, readme.txt (readme.txt, 16, f, checksumCRC32=379d0103)\n"
                + "commit()\n"
                + "deleteLastFeedingEvent()\n"
                + "createLastFeedingEvent(Thu Jan 01 01:00:01 CET 1970)\n"
                + "commit()\n"
                + "createDataSet(code=DS4, location=2)\n"
                + "createDataSetFile(6, parent=null, 2 (, 3, d))\n"
                + "createDataSetFiles:\n"
                + "  6, parent=7, hi.txt (hi.txt, 3, f, checksumCRC32=ed6f7a7a)\n"
                + "commit()\n"
                + "deleteLastFeedingEvent()\n"
                + "createLastFeedingEvent(Thu Jan 01 01:00:02 CET 1970)\n"
                + "commit()\n", pathsInfoDAO.getLog());
    }

    @Test
    public void testChecksumMD5()
    {
        final Experiment experiment = new Experiment();
        experiment.setPermId(new ExperimentPermId("ds1"));
        experiment.setImmutableDataDate(new Date(78000));

        final SimpleDataSetInformationDTO dataset = dataSet(experiment.getPermId().getPermId(), experiment.getImmutableDataDate().getTime(), "2");

        context.checking(new Expectations()
        {
            {
                one(service).listExperiments(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(12)));
                will(returnValue(List.of(experiment)));

                one(service).listSamples(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(12)));
                will(returnValue(List.of()));

                one(service).listDataSets(with(criteriaCodes(List.of(experiment.getPermId().getPermId()))), with(fetchOptionsCount(null)));
                will(returnValue(List.of(dataset)));

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset)), with(LockType.HierarchicallyExclusive));

                one(service).updateShareIdAndSize(dataset.getDataSetCode(), dataset.getDataSetShareId(), 3);
            }
        });

        MockPathsInfoDAO pathsInfoDAO = new MockPathsInfoDAO();
        createTask(pathsInfoDAO, "MD5", 12, 3, 0).execute();

        assertEquals("createDataSet(code=DS1, location=2)\n"
                + "createDataSetFile(0, parent=null, 2 (, 3, d))\n"
                + "createDataSetFiles:\n"
                + "  0, parent=1, hi.txt (hi.txt, 3, f, checksumCRC32=ed6f7a7a, checksum=MD5:764efa883dda1e11db47671c4a3bbd9e)\n"
                + "commit()\n"
                + "deleteLastFeedingEvent()\n"
                + "createLastFeedingEvent(Thu Jan 01 01:01:18 CET 1970)\n"
                + "commit()\n", pathsInfoDAO.getLog());
    }

    @Test
    public void testChecksumSHA1()
    {
        final Experiment experiment = new Experiment();
        experiment.setPermId(new ExperimentPermId("ds1"));
        experiment.setImmutableDataDate(new Date(78000));

        final SimpleDataSetInformationDTO dataset = dataSet(experiment.getPermId().getPermId(), experiment.getImmutableDataDate().getTime(), "2");

        context.checking(new Expectations()
        {
            {
                one(service).listExperiments(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(12)));
                will(returnValue(List.of(experiment)));

                one(service).listSamples(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(12)));
                will(returnValue(List.of()));

                one(service).listDataSets(with(criteriaCodes(List.of(experiment.getPermId().getPermId()))), with(fetchOptionsCount(null)));
                will(returnValue(List.of(dataset)));

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset)), with(LockType.HierarchicallyExclusive));

                one(service).updateShareIdAndSize(dataset.getDataSetCode(), dataset.getDataSetShareId(), 3);
            }
        });

        MockPathsInfoDAO pathsInfoDAO = new MockPathsInfoDAO();
        createTask(pathsInfoDAO, "SHA1", 12, 3, 0).execute();

        assertEquals("createDataSet(code=DS1, location=2)\n"
                + "createDataSetFile(0, parent=null, 2 (, 3, d))\n"
                + "createDataSetFiles:\n"
                + "  0, parent=1, hi.txt (hi.txt, 3, f, checksumCRC32=ed6f7a7a, checksum=SHA1:55ca6286e3e4f4fba5d0448333fa99fc5a404a73)\n"
                + "commit()\n"
                + "deleteLastFeedingEvent()\n"
                + "createLastFeedingEvent(Thu Jan 01 01:01:18 CET 1970)\n"
                + "commit()\n", pathsInfoDAO.getLog());
    }

    @Test
    public void testFailure()
    {
        final Experiment experiment = new Experiment();
        experiment.setPermId(new ExperimentPermId("ds1"));
        experiment.setImmutableDataDate(new Date(78000));

        final SimpleDataSetInformationDTO dataset = dataSet(experiment.getPermId().getPermId(), experiment.getImmutableDataDate().getTime(), "2");

        context.checking(new Expectations()
        {
            {
                one(service).listExperiments(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(12)));
                will(returnValue(List.of(experiment)));

                one(service).listSamples(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(12)));
                will(returnValue(List.of()));

                one(service).listDataSets(with(criteriaCodes(List.of(experiment.getPermId().getPermId()))), with(fetchOptionsCount(null)));
                will(returnValue(List.of(dataset)));

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset)), with(LockType.HierarchicallyExclusive));
            }
        });

        MockPathsInfoDAO pathsInfoDAO = new MockPathsInfoDAO();
        pathsInfoDAO.addException(1L, new RuntimeException("Oops!"));

        try
        {
            createTask(pathsInfoDAO, 12, 3, 0).execute();
        } catch (Throwable ex)
        {
            assertEquals("Oops!", ex.getMessage());
        }

        assertEquals("createDataSet(code=DS1, location=2)\n"
                + "createDataSetFile(0, parent=null, 2 (, 3, d))\n"
                + "createDataSetFiles:\n"
                + "ERROR:java.lang.RuntimeException: Oops!\n"
                + "rollback()\n", pathsInfoDAO.getLog());
    }

    @Test
    public void testNonExistingDataSetFolder()
    {
        final Experiment experiment = new Experiment();
        experiment.setPermId(new ExperimentPermId("ds1"));
        experiment.setImmutableDataDate(new Date(78000));

        final SimpleDataSetInformationDTO dataset =
                dataSet(experiment.getPermId().getPermId(), experiment.getImmutableDataDate().getTime(), "idontexist");

        context.checking(new Expectations()
        {
            {
                one(service).listExperiments(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(12)));
                will(returnValue(List.of(experiment)));

                one(service).listSamples(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(12)));
                will(returnValue(List.of()));

                one(service).listDataSets(with(criteriaCodes(List.of(experiment.getPermId().getPermId()))), with(fetchOptionsCount(null)));
                will(returnValue(List.of(dataset)));

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset)), with(LockType.HierarchicallyExclusive));
            }
        });

        MockPathsInfoDAO pathsInfoDAO = new MockPathsInfoDAO();
        createTask(pathsInfoDAO, 12, 3, 0).execute();

        assertEquals("deleteLastFeedingEvent()\n"
                + "createLastFeedingEvent(Thu Jan 01 01:01:18 CET 1970)\n"
                + "commit()\n", pathsInfoDAO.getLog());
    }

    @Test
    public void testAlreadyExistingDataSetInDatabase()
    {
        final Experiment experiment = new Experiment();
        experiment.setPermId(new ExperimentPermId("ds1"));
        experiment.setImmutableDataDate(new Date(78000));

        final SimpleDataSetInformationDTO dataset =
                dataSet(experiment.getPermId().getPermId(), experiment.getImmutableDataDate().getTime(), "ialreadyexist");

        context.checking(new Expectations()
        {
            {
                one(service).listExperiments(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(12)));
                will(returnValue(List.of(experiment)));

                one(service).listSamples(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(12)));
                will(returnValue(List.of()));

                one(service).listDataSets(with(criteriaCodes(List.of(experiment.getPermId().getPermId()))), with(fetchOptionsCount(null)));
                will(returnValue(List.of(dataset)));

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset)), with(LockType.HierarchicallyExclusive));
            }
        });

        MockPathsInfoDAO pathsInfoDAO = new MockPathsInfoDAO();
        pathsInfoDAO.setDataSetId(42);

        createTask(pathsInfoDAO, 12, 3, 0).execute();

        assertEquals("deleteLastFeedingEvent()\n"
                + "createLastFeedingEvent(Thu Jan 01 01:01:18 CET 1970)\n"
                + "commit()\n", pathsInfoDAO.getLog());
    }

    @Test
    public void testTimeLimit()
    {
        final int TIME_LIMIT = 100;

        final Sample sample1 = new Sample();
        sample1.setPermId(new SamplePermId("DS-1000"));
        sample1.setImmutableDataDate(new Date(1000));

        final Sample sample2 = new Sample();
        sample2.setPermId(new SamplePermId("DS-2000"));
        sample2.setImmutableDataDate(new Date(2000));

        final SimpleDataSetInformationDTO dataset1 = dataSet(sample1.getPermId().getPermId(), sample1.getImmutableDataDate().getTime());

        final Sequence chunkReadingSequence = context.sequence("chunkReadingSequence");

        context.checking(new Expectations()
        {
            {
                // chunk 1
                one(service).listExperiments(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(1)));
                will(returnValue(List.of()));
                inSequence(chunkReadingSequence);

                one(service).listSamples(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(1)));
                will(returnValue(List.of(sample1)));
                inSequence(chunkReadingSequence);

                one(service).listSamples(with(criteriaImmutableDate(new Date(0))), with(fetchOptionsCount(2)));
                will(returnValue(List.of(sample1, sample2)));
                inSequence(chunkReadingSequence);

                one(service).listDataSets(with(criteriaCodes(List.of(sample1.getPermId().getPermId()))),
                        with(fetchOptionsCount(null)));
                will(new CustomAction("return value with delay")
                {
                    @Override public Object invoke(final Invocation invocation) throws Throwable
                    {
                        Thread.sleep(TIME_LIMIT + 1);
                        return List.of(dataset1);
                    }
                });
                inSequence(chunkReadingSequence);

                one(lockManager).lock(with(any(UUID.class)), with(List.of(dataset1)), with(LockType.HierarchicallyExclusive));
                one(lockManager).unlock(with(any(UUID.class)), with(List.of(dataset1)), with(LockType.HierarchicallyExclusive));
                one(service).updateShareIdAndSize(dataset1.getDataSetCode(), dataset1.getDataSetShareId(), 16);
                inSequence(chunkReadingSequence);
            }
        });

        MockPathsInfoDAO pathsInfoDAO = new MockPathsInfoDAO();
        createTask(pathsInfoDAO, 1, 0, TIME_LIMIT).execute();

        assertEquals("createDataSet(code=DS-1000, location=3)\n"
                + "createDataSetFile(0, parent=null, 3 (, 16, d))\n"
                + "createDataSetFiles:\n"
                + "  0, parent=1, readme.txt (readme.txt, 16, f, checksumCRC32=379d0103)\n"
                + "commit()\n"
                + "deleteLastFeedingEvent()\n"
                + "createLastFeedingEvent(Thu Jan 01 01:00:01 CET 1970)\n"
                + "commit()\n", pathsInfoDAO.getLog());
    }

    private SimpleDataSetInformationDTO dataSet(String dataSetCode, long timeStamp)
    {
        return dataSet(dataSetCode, timeStamp, (timeStamp / 1000) % 2 == 0 ? "2" : "3");
    }

    private SimpleDataSetInformationDTO dataSet(String dataSetCode, long timeStamp, String location)
    {
        SimpleDataSetInformationDTO dataSet = new SimpleDataSetInformationDTO();
        dataSet.setDataSetCode(dataSetCode);
        dataSet.setImmutableDataTimestamp(new Date(timeStamp));
        dataSet.setDataSetLocation(location);
        dataSet.setDataSetShareId("1");
        return dataSet;
    }

    private PathInfoDatabaseFeedingTask createTask(IPathInfoNonAutoClosingDAO pathsInfoDAO,
            int chunkSize, int maxNumberOfChunks, long timeLimite)
    {
        return createTask(pathsInfoDAO, null, chunkSize, maxNumberOfChunks, timeLimite);
    }

    private PathInfoDatabaseFeedingTask createTask(IPathInfoNonAutoClosingDAO pathsInfoDAO, String checksumType,
            int chunkSize, int maxNumberOfChunks, long timeLimite)
    {
        return new PathInfoDatabaseFeedingTask(service, pathsInfoDAO, lockManager,
                new MockTimeProvider(0, 1000), STORE_ROOT.getPath(), true, checksumType, chunkSize, maxNumberOfChunks,
                timeLimite);
    }

    private <T extends AbstractEntitySearchCriteria<?>> Matcher<T> criteriaImmutableDate(Date immutableDataDate)
    {
        return new BaseMatcher<T>()
        {
            @Override public boolean matches(final Object o)
            {
                if (o instanceof AbstractEntitySearchCriteria)
                {
                    AbstractEntitySearchCriteria<?> criteria = (AbstractEntitySearchCriteria<?>) o;
                    ImmutableDataDateSearchCriteria criterion =
                            (ImmutableDataDateSearchCriteria) criteria.getCriteria().stream()
                                    .filter(c -> c instanceof ImmutableDataDateSearchCriteria)
                                    .findFirst().orElse(null);
                    return Objects.equals(immutableDataDate,
                            criterion != null ? ((AbstractDateObjectValue) criterion.getFieldValue()).getValue() : null);
                }
                return false;
            }

            @Override public void describeTo(final Description description)
            {
                description.appendText(String.valueOf(immutableDataDate));
            }
        };
    }

    private <T extends AbstractEntitySearchCriteria<?>> Matcher<T> criteriaCodes(List<String> codes)
    {
        return new BaseMatcher<T>()
        {
            @Override public boolean matches(final Object o)
            {
                if (o instanceof AbstractEntitySearchCriteria<?>)
                {
                    AbstractEntitySearchCriteria<?> criteria = (AbstractEntitySearchCriteria<?>) o;
                    CodesSearchCriteria criterion =
                            (CodesSearchCriteria) criteria.getCriteria().stream()
                                    .filter(c -> c instanceof CodesSearchCriteria)
                                    .findFirst().orElse(null);
                    return Objects.equals(codes, criterion != null ? criterion.getFieldValue() : null);
                }
                return false;
            }

            @Override public void describeTo(final Description description)
            {
                description.appendText(String.valueOf(codes));
            }
        };
    }

    private <T extends FetchOptions<?>> Matcher<T> fetchOptionsCount(Integer count)
    {
        return new BaseMatcher<T>()
        {
            @Override public boolean matches(final Object o)
            {
                if (o instanceof FetchOptions<?>)
                {
                    FetchOptions<?> fo = (FetchOptions<?>) o;
                    return Objects.equals(count, fo.getCount());
                }
                return false;
            }

            @Override public void describeTo(final Description description)
            {
                description.appendText(String.valueOf(count));
            }
        };
    }

}
