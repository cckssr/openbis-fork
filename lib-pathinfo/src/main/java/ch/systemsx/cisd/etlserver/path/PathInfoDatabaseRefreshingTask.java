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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.ArchivingStatus;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.pathinfo.IPathInfoNonAutoClosingDAO;
import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import ch.systemsx.cisd.etlserver.PathInfoServiceProviderFactory;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.IHierarchicalContentFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractExternalData;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DatasetV3Location;
import net.lemnik.eodsql.QueryTool;

/**
 * Maintenance task to recreate path-info database entries.
 *
 * @author Franz-Josef Elmer
 */
public class PathInfoDatabaseRefreshingTask extends AbstractPathInfoDatabaseFeedingTask
{
    static final String DATA_SET_TYPE_KEY = "data-set-type";

    static final String TIME_STAMP_OF_YOUNGEST_DATA_SET_KEY = "time-stamp-of-youngest-data-set";

    static final String CHUNK_SIZE_KEY = "chunk-size";

    private static final int DEFAULT_CHUNK_SIZE = 100;

    private static final Comparator<DataSet> REVERSE_REGISTRATION_DATE_COMPARATOR = new Comparator<DataSet>()
    {
        @Override
        public int compare(DataSet ds1, DataSet ds2)
        {
            long t1 = ds1.getRegistrationDate().getTime();
            long t2 = ds2.getRegistrationDate().getTime();
            if (t1 == t2)
            {
                return -ds1.getCode().compareTo(ds2.getCode());
            }
            return t1 < t2 ? 1 : (t1 > t2 ? -1 : 0);
        }
    };

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            PathInfoDatabaseRefreshingTask.class);

    private IApplicationServerApi service;

    private String timeStampAndCodeOfYoungestDataSet;

    private int chunkSize;

    private String dataSetType;

    public PathInfoDatabaseRefreshingTask()
    {
    }

    PathInfoDatabaseRefreshingTask(IApplicationServerApi service, IPathInfoNonAutoClosingDAO dao,
            IHierarchicalContentFactory hierarchicalContentFactory, IDataSetDirectoryProvider directoryProvider)
    {
        this.service = service;
        this.dao = dao;
        this.directoryProvider = directoryProvider;
    }

    @Override
    public void setUp(String pluginName, Properties properties)
    {
        if (service == null)
        {
            service = PathInfoServiceProviderFactory.getInstance().getV3ApplicationService();
            dao = QueryTool.getQuery(PathInfoServiceProviderFactory.getInstance().getPathInfoDataSourceProvider().getDataSource(),
                    IPathInfoNonAutoClosingDAO.class);
            directoryProvider = PathInfoServiceProviderFactory.getInstance().getDataSetDirectoryProvider();
        }
        defineStateFile(properties, directoryProvider.getStoreRoot());
        timeStampAndCodeOfYoungestDataSet = tryGetTimeStampAndCodeOfYoungestDataSet(properties);
        if (stateFile.exists() == false && timeStampAndCodeOfYoungestDataSet == null)
        {
            throw new ConfigurationFailureException("Either property '" + TIME_STAMP_OF_YOUNGEST_DATA_SET_KEY
                    + "' is defined or '" + stateFile.getAbsolutePath() + "' exists.");
        }
        chunkSize = PropertyUtils.getInt(properties, CHUNK_SIZE_KEY, DEFAULT_CHUNK_SIZE);
        computeChecksum = PropertyUtils.getBoolean(properties, COMPUTE_CHECKSUM_KEY, true);
        checksumType = getAndCheckChecksumType(properties);
        dataSetType = properties.getProperty(DATA_SET_TYPE_KEY);
    }

    private String tryGetTimeStampAndCodeOfYoungestDataSet(Properties properties)
    {
        String ts = properties.getProperty(TIME_STAMP_OF_YOUNGEST_DATA_SET_KEY);
        if (ts == null)
        {
            return null;
        }
        try
        {
            parseTimeStamp(ts);
            return ts;
        } catch (ParseException ex)
        {
            throw new ConfigurationFailureException("Invalid property '"
                    + TIME_STAMP_OF_YOUNGEST_DATA_SET_KEY + "': " + ts, ex);
        }

    }

    @Override
    public void execute()
    {
        List<DataSet> dataSets = getPhysicalDataSets();
        if (dataSets.isEmpty())
        {
            return;
        }
        operationLog.info("Refresh path info for " + dataSets.size() + " physical data sets.");
        long t0 = System.currentTimeMillis();
        for (DataSet dataSet : dataSets)
        {
            String dataSetCode = dataSet.getCode();
            dao.deleteDataSet(dataSetCode);
            feedPathInfoDatabase(new DatasetV3Location(dataSet), dataSet.getPhysicalData().isH5Folders(), dataSet.getPhysicalData().isH5arFolders());
            updateTimeStampFile(renderTimeStampAndCode(dataSet));
        }
        operationLog.info("Path info for " + dataSets.size() + " physical data sets refreshed in "
                + (System.currentTimeMillis() - t0) / 1000 + " secs.");
    }

    private List<DataSet> getPhysicalDataSets()
    {
        String timeStampAndCode = getLastTimeStampAndCode();

        DataSetSearchCriteria criteria = new DataSetSearchCriteria();
        criteria.withRegistrationDate().thatIsEarlierThanOrEqualTo(extractTimeStamp(timeStampAndCode));
        if (dataSetType != null)
        {
            criteria.withType().withCode().thatEquals(dataSetType);
        }

        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withPhysicalData();
        fetchOptions.withDataStore();
        fetchOptions.sortBy().registrationDate().asc();

        String sessionToken = PathInfoServiceProviderFactory.getInstance().getOpenBISService().getSessionToken();

        SearchResult<DataSet> searchResult = service.searchDataSets(sessionToken, criteria, fetchOptions);

        List<DataSet> dataSets = new ArrayList<>(searchResult.getObjects());
        dataSets.sort(REVERSE_REGISTRATION_DATE_COMPARATOR);

        List<DataSet> result = new ArrayList<>();
        for (DataSet dataSet : dataSets)
        {
            if (checkDataSet(dataSet, timeStampAndCode))
            {
                result.add(dataSet);
                if (result.size() >= chunkSize)
                {
                    return result;
                }
            }
        }

        return result;
    }

    private boolean checkDataSet(DataSet dataSet, String timeStampAndCode)
    {
        if (dataSet.getPhysicalData() == null)
        {
            return false;
        }
        if (!dataSet.getPhysicalData().getStatus().equals(ArchivingStatus.AVAILABLE))
        {
            return false;
        }
        return renderTimeStampAndCode(dataSet).compareTo(timeStampAndCode) < 0;
    }

    @Override
    protected Logger getOperationLog()
    {
        return operationLog;
    }

    private String getLastTimeStampAndCode()
    {
        if (stateFile.exists() == false)
        {
            return timeStampAndCodeOfYoungestDataSet;
        }
        return FileUtilities.loadToString(stateFile).trim();
    }

    private String renderTimeStampAndCode(DataSet dataSet)
    {
        return renderTimeStampAndCode(dataSet.getRegistrationDate(), dataSet.getCode());
    }

}
