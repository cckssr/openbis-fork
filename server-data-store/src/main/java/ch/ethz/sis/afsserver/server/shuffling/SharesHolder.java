/*
 * Copyright ETH 2012 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.afsserver.server.shuffling;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import ch.ethz.sis.afs.dto.LockType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.systemsx.cisd.common.filesystem.FileOperations;
import ch.systemsx.cisd.common.logging.ISimpleLogger;
import ch.systemsx.cisd.common.logging.LogLevel;
import ch.systemsx.cisd.common.utilities.ITimeProvider;

/**
 * A class that holds all shares and adds the data sets to them on demand.
 *
 * @author Bernd Rinn
 */
final class SharesHolder
{
    private final String dataStoreCode;

    private final Map<String, Share> shares;

    private final IEncapsulatedOpenBISService service;

    private final ISimpleLogger log;

    private final ITimeProvider timeProvider;

    private boolean areDataSetsAdded;

    SharesHolder(String dataStoreCode, Map<String, Share> shares,
            IEncapsulatedOpenBISService service, ISimpleLogger log, ITimeProvider timeProvider)
    {
        this.dataStoreCode = dataStoreCode;
        this.shares = shares;
        this.service = service;
        this.log = log;
        this.timeProvider = timeProvider;
    }

    /**
     * Adds the datasetts to the stores, if they have not yet been added.
     */
    void addDataSetsToStores()
    {
        if (areDataSetsAdded)
        {
            return;
        }

        final ILockManager lockManager = ServiceProvider.getLockManager();

        for (SimpleDataSetInformationDTO dataSet : service.listDataSets(new DataSetSearchCriteria(), new DataSetFetchOptions()))
        {
            String shareId = dataSet.getDataSetShareId();
            if (dataStoreCode.equals(dataSet.getDataStoreCode()))
            {
                Share share = shares.get(shareId);
                String dataSetCode = dataSet.getDataSetCode();
                if (share != null)
                {
                    if (dataSet.getDataSetSize() == null)
                    {
                        final UUID transactionId = UUID.randomUUID();

                        final boolean locked = lockManager.lock(transactionId, List.of(dataSet), LockType.HierarchicallyExclusive);

                        if (locked)
                        {
                            try
                            {
                                final File dataSetInStore =
                                        new File(share.getShare(), dataSet.getDataSetLocation());

                                if (FileOperations.getMonitoredInstanceForCurrentThread()
                                        .exists(dataSetInStore))
                                {
                                    log.log(LogLevel.INFO, "Calculating size of " + dataSetInStore);
                                    long t0 = timeProvider.getTimeInMilliseconds();
                                    long size = FileUtils.sizeOfDirectory(dataSetInStore);
                                    log.log(LogLevel.INFO,
                                            dataSetInStore + " contains " + size + " bytes (calculated in "
                                                    + (timeProvider.getTimeInMilliseconds() - t0)
                                                    + " msec)");
                                    service.updateShareIdAndSize(dataSetCode, shareId, size);
                                    dataSet.setDataSetSize(size);
                                    share.addDataSet(dataSet);
                                } else
                                {
                                    log.log(LogLevel.WARN, "Data set " + dataSetCode
                                            + " no longer exists in share " + shareId + ".");
                                }
                            } catch (Exception e)
                            {
                                log.log(LogLevel.ERROR, "Data set " + dataSetCode + " size could not be calculated.", e);
                            } finally
                            {
                                lockManager.unlock(transactionId, List.of(dataSet), LockType.HierarchicallyExclusive);
                            }
                        } else
                        {
                            log.log(LogLevel.INFO,
                                    "Data set " + dataSetCode
                                            + " size could not be calculated because the data set could not be locked (i.e. it is being used by another operation).");
                            share.addDataSet(dataSet);
                        }
                    } else
                    {
                        share.addDataSet(dataSet);
                    }
                }
            }
        }
        areDataSetsAdded = true;
    }

}