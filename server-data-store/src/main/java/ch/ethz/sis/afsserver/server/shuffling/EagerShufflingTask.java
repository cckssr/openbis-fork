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
package ch.ethz.sis.afsserver.server.shuffling;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import ch.ethz.sis.afsserver.server.common.IConfigProvider;
import ch.ethz.sis.afsserver.server.common.IEncapsulatedOpenBISService;
import ch.ethz.sis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.afsserver.server.common.SimpleDataSetInformationDTO;
import ch.ethz.sis.shared.log.LogManager;
import ch.ethz.sis.shared.log.Logger;
import ch.rinn.restrictions.Private;
import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;
import ch.systemsx.cisd.common.exceptions.EnvironmentFailureException;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.filesystem.IFreeSpaceProvider;
import ch.systemsx.cisd.common.logging.ISimpleLogger;
import ch.systemsx.cisd.common.logging.LogLevel;
import ch.systemsx.cisd.common.properties.PropertyParametersUtil;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import ch.systemsx.cisd.common.reflection.ClassUtils;

/**
 * Post registration task which move the data set to share which has enough space.
 *
 * @author Franz-Josef Elmer
 */
public class EagerShufflingTask extends AbstractPostRegistrationTaskForPhysicalDataSets
{
    private static final int SHARES_CACHING_TIMEOUT = 60 * 60 * 1000;

    @Private
    public static final String SHARE_FINDER_KEY = "share-finder";

    @Private
    public static final String FREE_SPACE_LIMIT_KEY =
            "free-space-limit-in-MB-triggering-notification";

    @Private
    public static final String STOP_ON_NO_SHARE_FOUND_KEY = "stop-on-no-share-found";

    @Private
    public static final String VERIFY_CHECKSUM_KEY = "verify-checksum";

    private static final Logger operationLog = LogManager.getLogger(EagerShufflingTask.class);

    private static final Logger notificationLog = LogManager.getLogger(EagerShufflingTask.class);

    private final IFreeSpaceProvider freeSpaceProvider;

    private final IDataSetMover dataSetMover;

    private final IChecksumProvider checksumProvider;

    private final ISimpleLogger logger;

    private final ISimpleLogger notifyer;

    private final File storeRoot;

    private final String dataStoreCode;

    private final Set<String> incomingShares;

    private IShareFinder finder;

    private long freeSpaceLimitTriggeringNotification;

    private boolean stopOnNoShareFound;

    private boolean verifyChecksum;

    public EagerShufflingTask(Properties properties, Set<String> incomingShares,
            IEncapsulatedOpenBISService service,
            IFreeSpaceProvider freeSpaceProvider, IDataSetMover dataSetMover,
            IConfigProvider configProvider, IChecksumProvider checksumProvider)
    {
        super(properties, service);
        this.incomingShares = incomingShares;
        this.freeSpaceProvider = freeSpaceProvider;
        this.dataSetMover = dataSetMover;
        this.checksumProvider = checksumProvider;
        this.logger = new SimpleLogger(operationLog);
        this.notifyer = new SimpleLogger(notificationLog);

        dataStoreCode = configProvider.getDataStoreCode();
        storeRoot = configProvider.getStoreRoot();
        if (storeRoot.isDirectory() == false)
        {
            throw new ConfigurationFailureException(
                    "Store root does not exists or is not a directory: "
                            + storeRoot.getAbsolutePath());
        }
        Properties props =
                PropertyParametersUtil.extractSingleSectionProperties(properties, SHARE_FINDER_KEY,
                        false).getProperties();
        finder = ClassUtils.create(IShareFinder.class, props.getProperty("class"), props);
        freeSpaceLimitTriggeringNotification =
                FileUtils.ONE_MB * PropertyUtils.getInt(properties, FREE_SPACE_LIMIT_KEY, 0);
        stopOnNoShareFound =
                PropertyUtils.getBoolean(properties, STOP_ON_NO_SHARE_FOUND_KEY, false);
        verifyChecksum = PropertyUtils.getBoolean(properties, VERIFY_CHECKSUM_KEY, true);
    }

    private IChecksumProvider getChecksumProvider()
    {
        if (verifyChecksum)
        {
            return checksumProvider;
        } else
        {
            return null;
        }
    }

    private List<Share> shares;

    private Date sharesTimestamp;

    @Override
    public void clearCache()
    {
        sharesTimestamp = null;
    }

    @Override
    public IPostRegistrationTaskExecutor createExecutor(String dataSetCode)
    {
        return new Executor(dataSetCode);
    }

    private List<Share> getShares()
    {
        if (shares == null || sharesTimestamp == null
                || sharesTimestamp.getTime() + SHARES_CACHING_TIMEOUT < System.currentTimeMillis())
        {
            shares = SegmentedStoreUtils.getSharesWithDataSets(storeRoot, dataStoreCode,
                    SegmentedStoreUtils.FilterOptions.AVAILABLE_FOR_SHUFFLING,
                    incomingShares, freeSpaceProvider, service, logger);
            sharesTimestamp = new Date();
        }
        return shares;
    }

    private final class Executor implements IPostRegistrationTaskExecutor
    {
        private final String dataSetCode;

        private SimpleDataSetInformationDTO dataSet;

        private Share shareWithMostFreeOrNull;

        Executor(String dataSetCode)
        {
            this.dataSetCode = dataSetCode;
        }

        @Override
        public ICleanupTask createCleanupTask()
        {
            dataSet = service.tryGetDataSet(dataSetCode);

            if (dataSet == null)
            {
                logger.log(LogLevel.WARN, "Data set " + dataSetCode + " will not be shuffled because it is in the trash can or has been deleted.");
                return new NoCleanupTask();
            }

            if (dataSet.getStatus().isAvailable() == false)
            {
                logger.log(LogLevel.WARN, "Data set " + dataSetCode + " couldn't been shuffled because "
                        + "its archiving status is " + dataSet.getStatus());
                return new NoCleanupTask();
            }

            List<Share> currentShares = getShares();
            shareWithMostFreeOrNull = finder.tryToFindShare(dataSet, currentShares);

            if (shareWithMostFreeOrNull == null)
            {
                String message = "No share found for shuffling data set " + dataSetCode + ".";
                if (stopOnNoShareFound)
                {
                    notifyer.log(LogLevel.ERROR, message);
                    throw new EnvironmentFailureException(message);
                }
                logger.log(LogLevel.WARN, message);
                return new NoCleanupTask();
            }

            return new CleanupTask(dataSet, storeRoot, shareWithMostFreeOrNull.getShareId());
        }

        @Override
        public void execute()
        {
            if (shareWithMostFreeOrNull != null)
            {
                String shareId = shareWithMostFreeOrNull.getShareId();
                try
                {
                    long freeSpaceBefore = shareWithMostFreeOrNull.calculateFreeSpace();
                    File share = new File(storeRoot, dataSet.getDataSetShareId());

                    dataSetMover.moveDataSetToAnotherShare(
                            new File(share, dataSet.getDataSetLocation()),
                            shareWithMostFreeOrNull.getShare(), getChecksumProvider(), logger);

                    logger.log(LogLevel.INFO, "Data set " + dataSetCode
                            + " successfully moved from share " + dataSet.getDataSetShareId()
                            + " to " + shareId + ".");
                    long freeSpaceAfter = shareWithMostFreeOrNull.calculateFreeSpace();
                    if (freeSpaceBefore > freeSpaceLimitTriggeringNotification
                            && freeSpaceAfter < freeSpaceLimitTriggeringNotification)
                    {
                        notifyer.log(
                                LogLevel.WARN,
                                "After moving data set " + dataSetCode + " to share " + shareId
                                        + " that share has only "
                                        + FileUtilities.byteCountToDisplaySize(freeSpaceAfter)
                                        + " free space. It might be necessary to add a new share.");
                    }
                } catch (Throwable t)
                {
                    logger.log(LogLevel.ERROR, "Couldn't move data set " + dataSetCode
                            + " to share " + shareId + ".", t);
                    throw t;
                }
            }
        }
    }

    private static final class CleanupTask implements ICleanupTask
    {
        private static final long serialVersionUID = 1L;

        private final SimpleDataSetInformationDTO dataSet;

        private final File storeRoot;

        private final String newShareId;

        CleanupTask(SimpleDataSetInformationDTO dataSet, File storeRoot, String newShareId)
        {
            this.dataSet = dataSet;
            this.storeRoot = storeRoot;
            this.newShareId = newShareId;
        }

        @Override
        public void cleanup(ISimpleLogger logger)
        {
            SegmentedStoreUtils.cleanUp(dataSet, storeRoot, newShareId, ServiceProvider.getOpenBISService(), ServiceProvider.getLockManager(),
                    logger);
        }
    }

}
