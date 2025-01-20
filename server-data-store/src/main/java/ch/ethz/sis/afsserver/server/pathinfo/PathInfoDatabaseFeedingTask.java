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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.afsserver.server.shuffling.IEncapsulatedOpenBISService;
import ch.ethz.sis.afsserver.server.shuffling.ILockManager;
import ch.ethz.sis.afsserver.server.shuffling.ServiceProvider;
import ch.ethz.sis.afsserver.server.shuffling.SimpleDataSetInformationDTO;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.pathinfo.IPathInfoNonAutoClosingDAO;
import ch.rinn.restrictions.Private;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import ch.systemsx.cisd.common.time.DateTimeUtils;
import ch.systemsx.cisd.common.utilities.ITimeProvider;
import ch.systemsx.cisd.common.utilities.SystemTimeProvider;
import net.lemnik.eodsql.QueryTool;

/**
 * Maintenance and post registration task which feeds pathinfo database with all data set paths.
 *
 * @author Franz-Josef Elmer
 */
public class PathInfoDatabaseFeedingTask extends AbstractPathInfoDatabaseFeedingTask
{
    private static interface IStopCondition
    {
        void handle(SimpleDataSetInformationDTO dataSet);

        boolean fulfilled();
    }

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            PathInfoDatabaseFeedingTask.class);

    static final String CHUNK_SIZE_KEY = "data-set-chunk-size";

    static final int DEFAULT_CHUNK_SIZE = 1000;

    static final String MAX_NUMBER_OF_CHUNKS_KEY = "max-number-of-chunks";

    static final int DEFAULT_MAX_NUMBER_OF_DATA_SETS = -1;

    static final String TIME_LIMIT_KEY = "time-limit";

    private IEncapsulatedOpenBISService service;

    private ITimeProvider timeProvider;

    private int chunkSize;

    private int maxNumberOfChunks;

    private long timeLimit;

    public PathInfoDatabaseFeedingTask()
    {
    }

    @Private PathInfoDatabaseFeedingTask(IEncapsulatedOpenBISService service,
            IPathInfoNonAutoClosingDAO dao, ILockManager lockManager, ITimeProvider timeProvider, String storageRoot, boolean computeChecksum,
            String checksumType, int chunkSize, int maxNumberOfChunks, long timeLimit)
    {
        this.service = service;
        this.dao = dao;
        this.lockManager = lockManager;
        this.timeProvider = timeProvider;
        this.storageRoot = storageRoot;
        this.computeChecksum = computeChecksum;
        this.checksumType = checksumType;
        this.chunkSize = chunkSize;
        this.maxNumberOfChunks = maxNumberOfChunks;
        this.timeLimit = timeLimit;
    }

    @Override
    public void setUp(String pluginName, Properties properties)
    {
        service = ServiceProvider.getOpenBISService();
        lockManager = ServiceProvider.getLockManager();
        timeProvider = SystemTimeProvider.SYSTEM_TIME_PROVIDER;

        DatabaseConfiguration pathInfoConfiguration = PathInfoDatabaseConfiguration.getInstance(ServiceProvider.getConfiguration());

        if (pathInfoConfiguration != null)
        {
            dao = QueryTool.getQuery(pathInfoConfiguration.getDataSource(), IPathInfoNonAutoClosingDAO.class);
        } else
        {
            throw new RuntimeException("Path info database is not configured.");
        }

        storageRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(ServiceProvider.getConfiguration());
        computeChecksum = getComputeChecksumFlag(properties);
        checksumType = getAndCheckChecksumType(properties);
        chunkSize = PropertyUtils.getInt(properties, CHUNK_SIZE_KEY, DEFAULT_CHUNK_SIZE);
        maxNumberOfChunks =
                PropertyUtils.getInt(properties, MAX_NUMBER_OF_CHUNKS_KEY,
                        DEFAULT_MAX_NUMBER_OF_DATA_SETS);
        timeLimit = DateTimeUtils.getDurationInMillis(properties, TIME_LIMIT_KEY, 0);
        StringBuilder builder = new StringBuilder(pluginName);
        builder.append(" intialized with chunk size = ").append(chunkSize).append(".");
        if (timeLimit > 0)
        {
            builder.append(" Time limit: ").append(DateTimeUtils.renderDuration(timeLimit));
        } else if (maxNumberOfChunks > 0)
        {
            builder.append(" Maximum number of chunks: ").append(maxNumberOfChunks);
        }
        operationLog.info(builder.toString());
    }

    private static boolean getComputeChecksumFlag(Properties properties)
    {
        return PropertyUtils.getBoolean(properties, COMPUTE_CHECKSUM_KEY, false);
    }

    @Override
    public void execute()
    {
        IStopCondition stopCondition = createStopCondition();
        List<SimpleDataSetInformationDTO> dataSets;
        int count = 0;
        operationLog.info("Start feeding.");
        Set<String> processedDataSets = new HashSet<>();
        do
        {
            Date lastImmutableDataTimestamp = dao.getLastSeenTimestamp(DataStoreKind.AFS.name());
            dataSets = listDataSets(lastImmutableDataTimestamp, chunkSize);
            dataSets = filteredDataSets(dataSets, processedDataSets);
            Date maxImmutableDataTimestamp = null;

            for (SimpleDataSetInformationDTO dataSet : dataSets)
            {
                final Long size = feedPathInfoDatabase(dataSet);
                if (size != null)
                {
                    service.updateShareIdAndSize(dataSet.getDataSetCode(), dataSet.getDataSetShareId(), size);
                    count++;
                }
                processedDataSets.add(dataSet.getDataSetCode());
                Date immutableDataTimestamp = dataSet.getImmutableDataTimestamp();
                if (maxImmutableDataTimestamp == null || maxImmutableDataTimestamp.getTime() < immutableDataTimestamp.getTime())
                {
                    maxImmutableDataTimestamp = immutableDataTimestamp;
                }
                stopCondition.handle(dataSet);
            }

            operationLog.info("Fed " + count + " data set(s).");

            if (maxImmutableDataTimestamp != null)
            {
                dao.deleteLastSeenTimestamp(DataStoreKind.AFS.name());
                dao.createLastSeenTimestamp(maxImmutableDataTimestamp, DataStoreKind.AFS.name());
                dao.commit();
            }
        } while (dataSets.size() >= chunkSize && stopCondition.fulfilled() == false);
        operationLog.info("Feeding finished.");
    }

    @Override
    protected Logger getOperationLog()
    {
        return operationLog;
    }

    private List<SimpleDataSetInformationDTO> filteredDataSets(List<SimpleDataSetInformationDTO> dataSets, Set<String> processedDataSets)
    {
        List<SimpleDataSetInformationDTO> result = new ArrayList<>();
        for (SimpleDataSetInformationDTO dataSet : dataSets)
        {
            if (processedDataSets.contains(dataSet.getDataSetCode()) == false)
            {
                result.add(dataSet);
            }
        }
        return result;
    }

    private List<SimpleDataSetInformationDTO> listDataSets(Date timestamp, int actualChunkSize)
    {
        // We cannot sort data sets by experiment/sample immutableDataDate field,
        // therefore we need to fetch experiments and samples sorted by the immutableDataDate first,
        // combine the results and use them to find the data sets that belong to these
        // experiments and samples.

        List<Experiment> experiments = listExperiments(timestamp, actualChunkSize);
        List<Sample> samples = listSamples(timestamp, actualChunkSize);

        List<ExperimentOrSample> experimentsAndSamples = new ArrayList<>();
        experimentsAndSamples.addAll(experiments.stream().map(ExperimentOrSample::new).collect(Collectors.toList()));
        experimentsAndSamples.addAll(samples.stream().map(ExperimentOrSample::new).collect(Collectors.toList()));
        experimentsAndSamples.sort(Comparator.comparing(ExperimentOrSample::getImmutableDataDate));

        if (experimentsAndSamples.isEmpty())
        {
            return Collections.emptyList();
        }

        List<ExperimentOrSample> experimentsAndSamplesBatch = new ArrayList<>();
        Date lastImmutableDataDate = null;

        for (int i = 0; i < experimentsAndSamples.size(); i++)
        {
            ExperimentOrSample experimentOrSample = experimentsAndSamples.get(i);

            if (i < actualChunkSize)
            {
                experimentsAndSamplesBatch.add(experimentOrSample);
                lastImmutableDataDate = experimentOrSample.getImmutableDataDate();
            } else
            {
                // make sure all experiments and samples with the same immutableDataDate are processed
                if (experimentOrSample.getImmutableDataDate().equals(lastImmutableDataDate))
                {
                    experimentsAndSamplesBatch.add(experimentOrSample);
                } else
                {
                    break;
                }
            }
        }

        DataSetSearchCriteria criteria = new DataSetSearchCriteria();
        // data sets codes are equal to sample/experiment perm ids they are connected to
        criteria.withCodes().thatIn(experimentsAndSamplesBatch.stream().map(ExperimentOrSample::getPermId).collect(Collectors.toList()));

        return service.listDataSets(criteria, new DataSetFetchOptions());
    }

    private List<Experiment> listExperiments(Date timestamp, int actualChunkSize)
    {
        ExperimentSearchCriteria criteria = new ExperimentSearchCriteria();
        if (timestamp != null)
        {
            criteria.withImmutableDataDate().thatIsLaterThan(timestamp);
        } else
        {
            criteria.withImmutableDataDate().thatIsLaterThanOrEqualTo(new Date(0));
        }

        ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
        fetchOptions.count(actualChunkSize);
        fetchOptions.sortBy().immutableDataDate().asc();

        List<Experiment> experiments = service.listExperiments(criteria, fetchOptions);

        if (experiments.size() < actualChunkSize || !allValuesTheSame(experiments, Experiment::getImmutableDataDate))
        {
            return experiments;
        }

        operationLog.warn("There are at least " + actualChunkSize
                + " experiments with same immutable data timestamp. Twice the chunk size will be tried.");

        return listExperiments(timestamp, 2 * actualChunkSize);
    }

    private List<Sample> listSamples(Date timestamp, int actualChunkSize)
    {
        SampleSearchCriteria criteria = new SampleSearchCriteria();
        if (timestamp != null)
        {
            criteria.withImmutableDataDate().thatIsLaterThan(timestamp);
        } else
        {
            criteria.withImmutableDataDate().thatIsLaterThanOrEqualTo(new Date(0));
        }

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.count(actualChunkSize);
        fetchOptions.sortBy().immutableDataDate().asc();

        List<Sample> samples = service.listSamples(criteria, fetchOptions);

        if (samples.size() < actualChunkSize || !allValuesTheSame(samples, Sample::getImmutableDataDate))
        {
            return samples;
        }

        operationLog.warn("There are at least " + actualChunkSize
                + " samples with same immutable data timestamp. Twice the chunk size will be tried.");

        return listSamples(timestamp, 2 * actualChunkSize);
    }

    private <O, V> boolean allValuesTheSame(List<O> list, Function<O, V> valueExtractor)
    {
        Set<V> values = new HashSet<>();
        for (O object : list)
        {
            values.add(valueExtractor.apply(object));
        }
        return values.size() == 1;
    }

    private IStopCondition createStopCondition()
    {
        if (timeLimit > 0)
        {
            return createStopConditionForTimeLimit();
        }
        if (maxNumberOfChunks > 0)
        {
            return createStopConditionForMaxNumber();
        }
        return new IStopCondition()
        {

            @Override
            public void handle(SimpleDataSetInformationDTO dataSet)
            {
            }

            @Override
            public boolean fulfilled()
            {
                return false;
            }
        };
    }

    private IStopCondition createStopConditionForMaxNumber()
    {
        return new IStopCondition()
        {
            private int count;

            @Override
            public void handle(SimpleDataSetInformationDTO dataSet)
            {
            }

            @Override
            public boolean fulfilled()
            {
                return ++count >= maxNumberOfChunks;
            }
        };
    }

    private IStopCondition createStopConditionForTimeLimit()
    {
        return new IStopCondition()
        {
            private long startTime = timeProvider.getTimeInMilliseconds();

            @Override
            public void handle(SimpleDataSetInformationDTO dataSet)
            {
            }

            @Override
            public boolean fulfilled()
            {
                return timeProvider.getTimeInMilliseconds() - startTime > timeLimit;
            }
        };
    }

    private static class ExperimentOrSample
    {

        private Experiment experiment;

        private Sample sample;

        public ExperimentOrSample(Experiment experiment)
        {
            this.experiment = experiment;
        }

        public ExperimentOrSample(Sample sample)
        {
            this.sample = sample;
        }

        public String getPermId()
        {
            if (experiment != null)
            {
                return experiment.getPermId().getPermId();
            } else
            {
                return sample.getPermId().getPermId();
            }
        }

        public Date getImmutableDataDate()
        {
            if (experiment != null)
            {
                return experiment.getImmutableDataDate();
            } else
            {
                return sample.getImmutableDataDate();
            }
        }

    }

}
