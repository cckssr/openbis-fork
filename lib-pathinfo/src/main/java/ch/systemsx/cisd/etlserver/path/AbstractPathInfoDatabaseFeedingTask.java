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

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.apache.log4j.Logger;

import ch.ethz.sis.pathinfo.IPathInfoNonAutoClosingDAO;
import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;
import ch.systemsx.cisd.etlserver.plugins.AbstractMaintenanceTaskWithStateFile;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.Hdf5AwareHierarchicalContentFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IDatasetLocation;

/**
 * @author Franz-Josef Elmer
 */
abstract class AbstractPathInfoDatabaseFeedingTask extends AbstractMaintenanceTaskWithStateFile
{
    static final String COMPUTE_CHECKSUM_KEY = "compute-checksum";

    static final String CHECKSUM_TYPE_KEY = "checksum-type";

    static String getAndCheckChecksumType(Properties properties)
    {
        String checksumType = properties.getProperty(CHECKSUM_TYPE_KEY);
        if (checksumType != null)
        {
            checksumType = checksumType.trim();
            try
            {
                MessageDigest.getInstance(checksumType);
            } catch (NoSuchAlgorithmException ex)
            {
                throw new ConfigurationFailureException("Unsupported checksum type: " + checksumType);
            }
        }
        return checksumType;
    }

    protected IDataSetDirectoryProvider directoryProvider;

    protected IPathInfoNonAutoClosingDAO dao;

    protected boolean computeChecksum;

    protected String checksumType;

    protected Long feedPathInfoDatabase(IDatasetLocation dataSet, boolean h5Folders, boolean h5arFolders)
    {

        IShareIdManager shareIdManager = directoryProvider.getShareIdManager();
        String dataSetCode = dataSet.getDataSetCode();

        shareIdManager.lock(dataSetCode);
        Long size = null;

        try
        {
            DatabaseBasedDataSetPathsInfoFeeder feeder =
                    new DatabaseBasedDataSetPathsInfoFeeder(dao, new Hdf5AwareHierarchicalContentFactory(h5Folders, h5arFolders), computeChecksum,
                            checksumType);
            Long id = dao.tryGetDataSetId(dataSetCode);

            if (id == null)
            {
                File dataSetRoot = directoryProvider.getDataSetDirectory(dataSet);

                if (!dataSetRoot.exists())
                {
                    getOperationLog().error("Root directory of data set " + dataSetCode
                            + " does not exists: " + dataSetRoot);
                    return size;
                }

                size = feeder.addPaths(dataSetCode, dataSet.getDataSetLocation(), dataSetRoot);
                feeder.commit();
                getOperationLog().info("Paths inside data set " + dataSetCode
                        + " successfully added to database. Data set size: " + size);
            } else
            {
                getOperationLog().info("Data set " + dataSetCode + " already exists in the path info database. Skipping.");
            }
        } catch (Exception ex)
        {
            getOperationLog().error("Couldn't feed database with path infos of data set " + dataSetCode, ex);
            dao.rollback();
            throw ex;
        } finally
        {
            shareIdManager.releaseLock(dataSetCode);
        }
        return size;
    }

    protected abstract Logger getOperationLog();

}
