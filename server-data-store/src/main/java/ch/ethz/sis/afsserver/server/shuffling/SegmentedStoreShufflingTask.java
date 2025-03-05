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

import static ch.systemsx.cisd.common.logging.LogLevel.INFO;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;

import ch.ethz.sis.afsserver.server.common.IConfigProvider;
import ch.ethz.sis.afsserver.server.common.IEncapsulatedOpenBISService;
import ch.ethz.sis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.afsserver.server.common.SimpleDataSetInformationDTO;
import ch.ethz.sis.afsserver.server.observer.impl.OpenBISUtils;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.shared.log.LogManager;
import ch.ethz.sis.shared.log.Logger;
import ch.ethz.sis.shared.startup.Configuration;
import ch.rinn.restrictions.Private;
import ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel;
import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.filesystem.IFreeSpaceProvider;
import ch.systemsx.cisd.common.filesystem.SimpleFreeSpaceProvider;
import ch.systemsx.cisd.common.logging.ISimpleLogger;
import ch.systemsx.cisd.common.logging.LogInitializer;
import ch.systemsx.cisd.common.maintenance.IDataStoreLockingMaintenanceTask;
import ch.systemsx.cisd.common.properties.PropertyParametersUtil;
import ch.systemsx.cisd.common.reflection.ClassUtils;

/**
 * Maintenance task which shuffles data sets between shares of a segmented store. This task is supposed to prevent incoming shares from having not
 * enough space.
 *
 * @author Franz-Josef Elmer
 */
public class SegmentedStoreShufflingTask implements IDataStoreLockingMaintenanceTask
{
    private static final ISegmentedStoreShuffling DUMMY_SHUFFLING = new ISegmentedStoreShuffling()
    {
        private static final int N = 3;

        @Override
        public void init(ISimpleLogger logger)
        {
        }

        @Override public void shuffleDataSets(final List<Share> sourceShares, final List<Share> targetShares, final Set<String> incomingShares,
                final IEncapsulatedOpenBISService service, final IFreeSpaceProvider freeSpaceProvider, final IDataSetMover dataSetMover,
                final IConfigProvider configProvider,
                final IChecksumProvider checksumProvider, final ISimpleLogger logger)
        {
            logger.log(INFO, "Data Store Shares:");

            for (Share share : targetShares)
            {
                List<SimpleDataSetInformationDTO> dataSets = share.getDataSetsOrderedBySize();
                logger.log(
                        INFO,
                        "   "
                                + (share.isIncoming() ? "Incoming" : "External")
                                + " share "
                                + share.getShareId()
                                + " (free space: "
                                + FileUtils.byteCountToDisplaySize(share.calculateFreeSpace())
                                + ") has "
                                + dataSets.size()
                                + " data sets occupying "
                                + FileUtilities.byteCountToDisplaySize(share
                                .getTotalSizeOfDataSets()) + ".");
                for (int i = 0, n = Math.min(N, dataSets.size()); i < n; i++)
                {
                    SimpleDataSetInformationDTO dataSet = dataSets.get(i);
                    logger.log(
                            INFO,
                            "      "
                                    + dataSet.getDataSetCode()
                                    + " "
                                    + FileUtilities.byteCountToDisplaySize(dataSet
                                    .getDataSetSize()));
                }
                if (dataSets.size() > N)
                {
                    logger.log(INFO, "      ...");
                }
            }
        }
    };

    @Private
    static final String SHUFFLING_SECTION_NAME = "shuffling";

    @Private
    static final String CLASS_PROPERTY_NAME = "class";

    private static final Logger operationLog = LogManager.getLogger(SegmentedStoreShufflingTask.class);

    private static final Logger notificationLog = LogManager.getLogger(SegmentedStoreShufflingTask.class);

    private final Set<String> incomingShares;

    private final IEncapsulatedOpenBISService service;

    private final IDataSetMover dataSetMover;

    private final IConfigProvider configProvider;

    private final IFreeSpaceProvider freeSpaceProvider;

    private final IChecksumProvider checksumProvider;

    private final ISimpleLogger operationLogger;

    private File storeRoot;

    private final String dataStoreCode = OpenBISUtils.AFS_DATA_STORE_CODE;

    @Private
    ISegmentedStoreShuffling shuffling;

    public SegmentedStoreShufflingTask()
    {
        this(IncomingShareIdProvider.getIdsOfIncomingShares(), ServiceProvider.getOpenBISService(),
                new SimpleFreeSpaceProvider(), new DataSetMover(
                        ServiceProvider.getOpenBISService(), ServiceProvider.getLockManager()), new PathInfoChecksumProvider(),
                ServiceProvider.getConfigProvider());
    }

    public SegmentedStoreShufflingTask(Set<String> incomingShares, IEncapsulatedOpenBISService service,
            IFreeSpaceProvider freeSpaceProvider, IDataSetMover dataSetMover, IChecksumProvider checksumProvider, IConfigProvider configProvider)
    {
        LogInitializer.init();
        this.incomingShares = incomingShares;
        this.freeSpaceProvider = freeSpaceProvider;
        this.checksumProvider = checksumProvider;
        this.service = service;
        this.dataSetMover = dataSetMover;
        this.configProvider = configProvider;
        operationLogger = new SimpleLogger(operationLog);
    }

    @Override
    public void setUp(String pluginName, Properties properties)
    {
        Configuration configuration = new Configuration(properties);
        storeRoot = new File(AtomicFileSystemServerParameterUtil.getStorageRoot(configuration));
        if (storeRoot.isDirectory() == false)
        {
            throw new ConfigurationFailureException(
                    "Store root does not exists or is not a directory: "
                            + storeRoot.getAbsolutePath());
        }
        shuffling = createShuffling(properties);
        shuffling.init(operationLogger);
        operationLog.info("Plugin '" + pluginName + "' initialized: shuffling strategy: "
                + shuffling.getClass().getName() + ", data store code: " + dataStoreCode
                + ", data store root: " + storeRoot.getAbsolutePath() + ", incoming shares: "
                + incomingShares);
    }

    private ISegmentedStoreShuffling createShuffling(Properties properties)
    {
        Properties shufflingProps =
                PropertyParametersUtil.extractSingleSectionProperties(properties,
                        SHUFFLING_SECTION_NAME, false).getProperties();
        String className = shufflingProps.getProperty(CLASS_PROPERTY_NAME);
        if (className == null)
        {
            return DUMMY_SHUFFLING;
        }
        try
        {
            return ClassUtils.create(ISegmentedStoreShuffling.class, className, shufflingProps);
        } catch (ConfigurationFailureException ex)
        {
            throw ex;
        } catch (Exception ex)
        {
            throw new ConfigurationFailureException("Cannot find shuffling class '" + className
                    + "'", CheckedExceptionTunnel.unwrapIfNecessary(ex));
        }
    }

    @Override
    public void execute()
    {
        operationLog.info("Starting segmented store shuffling.");
        List<Share> shares = listShares();
        List<Share> sourceShares = new ArrayList<Share>();
        Set<String> nonEmptyShares = new TreeSet<String>();
        for (Share share : shares)
        {
            if (incomingShares.contains(share.getShareId()) || share.isWithdrawShare())
            {
                sourceShares.add(share);
            }
            if (share.getDataSetsOrderedBySize().isEmpty() == false)
            {
                nonEmptyShares.add(share.getShareId());
            }
        }
        shuffling.shuffleDataSets(sourceShares, shares, incomingShares, service, freeSpaceProvider, dataSetMover, configProvider, checksumProvider,
                operationLogger);

        operationLog.info("Segmented store shuffling finished.");
        Set<String> emptyShares = new TreeSet<String>();
        for (Share share : listShares())
        {
            if (share.getDataSetsOrderedBySize().isEmpty()
                    && nonEmptyShares.contains(share.getShareId()))
            {
                emptyShares.add(share.getShareId());
            }
        }
        if (emptyShares.isEmpty() == false)
        {
            notificationLog.info("The following shares were emptied by shuffling: " + emptyShares);
        }
    }

    private List<Share> listShares()
    {
        return SegmentedStoreUtils.getSharesWithDataSets(storeRoot, dataStoreCode, SegmentedStoreUtils.FilterOptions.AVAILABLE_FOR_SHUFFLING,
                Collections.<String>emptySet(), freeSpaceProvider, service, operationLogger);
    }

    /**
     * @see IDataStoreLockingMaintenanceTask#requiresDataStoreLock()
     */
    @Override
    public boolean requiresDataStoreLock()
    {
        return true;
    }

}
