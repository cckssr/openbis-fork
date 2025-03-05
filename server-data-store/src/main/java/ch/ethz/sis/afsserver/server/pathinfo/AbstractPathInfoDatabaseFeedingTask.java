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
package ch.ethz.sis.afsserver.server.pathinfo;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;

import ch.ethz.sis.afs.dto.LockType;
import ch.ethz.sis.afsserver.server.common.ILockManager;
import ch.ethz.sis.afsserver.server.common.SimpleDataSetInformationDTO;
import ch.ethz.sis.pathinfo.IPathInfoNonAutoClosingDAO;
import ch.ethz.sis.shared.io.IOUtils;
import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;
import ch.systemsx.cisd.common.maintenance.IMaintenanceTask;

/**
 * @author Franz-Josef Elmer
 */
abstract class AbstractPathInfoDatabaseFeedingTask implements IMaintenanceTask
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

    protected IPathInfoNonAutoClosingDAO dao;

    protected ILockManager lockManager;

    protected String storageRoot;

    protected boolean computeChecksum;

    protected String checksumType;

    protected Long feedPathInfoDatabase(SimpleDataSetInformationDTO dataSet)
    {
        final UUID transactionId = UUID.randomUUID();

        lockManager.lock(transactionId, List.of(dataSet), LockType.HierarchicallyExclusive);

        Long size = null;

        try
        {
            File dataSetRoot = getDataSetRoot(dataSet);

            if (dataSetRoot.exists() == false)
            {
                getOperationLog().error("Root directory of data set " + dataSet.getDataSetCode()
                        + " does not exists: " + dataSetRoot);
                return size;
            }

            DatabaseBasedDataSetPathsInfoFeeder feeder =
                    new DatabaseBasedDataSetPathsInfoFeeder(dao, computeChecksum, checksumType);
            Long id = dao.tryGetDataSetId(dataSet.getDataSetCode());
            if (id == null)
            {
                size = feeder.addPaths(dataSet.getDataSetCode(), dataSet.getDataSetLocation(), dataSetRoot);
                feeder.commit();
                getOperationLog().info("Paths inside data set " + dataSet.getDataSetCode()
                        + " successfully added to database. Data set size: " + size);
            }
        } catch (Exception ex)
        {
            getOperationLog().error("Couldn't feed database with path infos of data set " + dataSet.getDataSetCode(), ex);
            dao.rollback();
            throw ex;
        } finally
        {
            lockManager.unlock(transactionId, List.of(dataSet), LockType.HierarchicallyExclusive);
        }
        return size;
    }

    private File getDataSetRoot(SimpleDataSetInformationDTO dataSet)
    {
        String path = IOUtils.getPath(storageRoot, dataSet.getDataSetShareId(), dataSet.getDataSetLocation());
        return new File(path);
    }

    protected abstract Logger getOperationLog();

}
