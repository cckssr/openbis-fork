package ch.ethz.sis.openbis.afsserver.server.shuffling.messages;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.messages.consumer.IMessageHandler;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.openbis.afsserver.server.common.DTOTranslator;
import ch.ethz.sis.openbis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.openbis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.messages.DataSetCreatedMessage;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.ethz.sis.shared.log.classic.impl.SimpleLogger;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.collection.CollectionUtils;
import ch.systemsx.cisd.common.filesystem.FileOperations;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.filesystem.SimpleFreeSpaceProvider;
import ch.systemsx.cisd.common.properties.PropertyParametersUtil;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import ch.systemsx.cisd.common.reflection.ClassUtils;
import ch.systemsx.cisd.common.time.DateTimeUtils;
import ch.systemsx.cisd.etlserver.plugins.DataSetMover;
import ch.systemsx.cisd.openbis.dss.generic.shared.IChecksumProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareFinder;
import ch.systemsx.cisd.openbis.dss.generic.shared.ShufflingServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.utils.SegmentedStoreUtils;
import ch.systemsx.cisd.openbis.dss.generic.shared.utils.Share;
import ch.systemsx.cisd.openbis.generic.shared.dto.SimpleDataSetInformationDTO;

public class EagerShufflingMessageHandler implements IMessageHandler
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, EagerShufflingMessageHandler.class);

    private static final Logger notificationLog = LogFactory.getLogger(LogCategory.NOTIFY, EagerShufflingMessageHandler.class);

    private static final String SHUFFLING_SECTION_NAME = "shuffling";

    private static final String SHARE_FINDER_KEY = "share-finder";

    private static final String CLASS_KEY = "class";

    private static final String FREE_SPACE_LIMIT_KEY = "free-space-limit-in-MB-triggering-notification";

    private static final String STOP_ON_NO_SHARE_FOUND_KEY = "stop-on-no-share-found";

    private static final String VERIFY_CHECKSUM_KEY = "verify-checksum";

    private File storeRoot;

    private IShareFinder finder;

    private long freeSpaceLimitTriggeringNotification;

    private boolean stopOnNoShareFound;

    private boolean verifyChecksum;

    private List<Share> shares;

    @Override public Set<String> getSupportedMessageTypes()
    {
        return Set.of(DataSetCreatedMessage.TYPE);
    }

    @Override public void beforeFirstMessage()
    {
        storeRoot = ShufflingServiceProviderFactory.getInstance().getConfigProvider().getStoreRoot();
        String dataStoreCode = ShufflingServiceProviderFactory.getInstance().getConfigProvider().getDataStoreCode();
        Set<String> incomingShares = ShufflingServiceProviderFactory.getInstance().getIncomingShareIdProvider().getIdsOfIncomingShares();
        IOpenBISService service = ShufflingServiceProviderFactory.getInstance().getOpenBISService();

        Properties shufflingProperties =
                PropertyParametersUtil.extractSingleSectionProperties(ServiceProvider.getInstance().getConfiguration().getProperties(),
                        SHUFFLING_SECTION_NAME, false).getProperties();
        Properties finderProperties =
                PropertyParametersUtil.extractSingleSectionProperties(shufflingProperties, SHARE_FINDER_KEY,
                        false).getProperties();
        finder = ClassUtils.create(IShareFinder.class, finderProperties.getProperty(CLASS_KEY), finderProperties);
        freeSpaceLimitTriggeringNotification =
                FileUtils.ONE_MB * PropertyUtils.getInt(shufflingProperties, FREE_SPACE_LIMIT_KEY, 0);
        stopOnNoShareFound =
                PropertyUtils.getBoolean(shufflingProperties, STOP_ON_NO_SHARE_FOUND_KEY, false);
        verifyChecksum = PropertyUtils.getBoolean(shufflingProperties, VERIFY_CHECKSUM_KEY, true);
        shares = SegmentedStoreUtils.getSharesWithDataSets(storeRoot, dataStoreCode,
                SegmentedStoreUtils.FilterOptions.AVAILABLE_FOR_SHUFFLING,
                incomingShares, new SimpleFreeSpaceProvider(), service, new SimpleLogger(operationLog));
    }

    @Override public void afterLastMessage()
    {
        shares = null;
    }

    @Override public void handleMessage(final Message message)
    {
        Configuration configuration = ServiceProvider.getInstance().getConfiguration();
        JsonObjectMapper jsonObjectMapper = AtomicFileSystemServerParameterUtil.getJsonObjectMapper(configuration);
        DataSetCreatedMessage createdMessage = DataSetCreatedMessage.deserialize(jsonObjectMapper, message);

        if (createdMessage.getDataSetCodes().isEmpty())
        {
            return;
        }

        List<SimpleDataSetInformationDTO> foundDataSets = findDataSets(createdMessage.getDataSetCodes());

        if (foundDataSets.isEmpty())
        {
            operationLog.info("Could not find any of the data sets to be shuffled: " + CollectionUtils.abbreviate(createdMessage.getDataSetCodes(),
                    CollectionUtils.DEFAULT_MAX_LENGTH) + ". Nothing will be archived.");
            return;
        } else
        {
            Set<String> notFoundDataSetCodes = new LinkedHashSet<>(createdMessage.getDataSetCodes());
            notFoundDataSetCodes.removeAll(codesSet(foundDataSets));

            if (!notFoundDataSetCodes.isEmpty())
            {
                operationLog.info(
                        "The following data sets to be shuffled could not be found: " + CollectionUtils.abbreviate(notFoundDataSetCodes,
                                CollectionUtils.DEFAULT_MAX_LENGTH) + ". Only those found will be shuffled: " + CollectionUtils.abbreviate(
                                codesList(foundDataSets), CollectionUtils.DEFAULT_MAX_LENGTH));
            }
        }

        for (SimpleDataSetInformationDTO foundDataSet : foundDataSets)
        {
            if (foundDataSet.getDataSetSize() == null || foundDataSet.getImmutableDataDate() == null)
            {
                updateDataSetSize(foundDataSet);
            }

            if (foundDataSet.getDataSetSize() != null)
            {
                shuffleDataSet(foundDataSet);
            }
        }
    }

    private List<SimpleDataSetInformationDTO> findDataSets(List<String> dataSetCodes)
    {
        DataSetSearchCriteria criteria = new DataSetSearchCriteria();
        criteria.withDataStore().withKind().thatIn(DataStoreKind.AFS);
        criteria.withPhysicalData();
        criteria.withCodes().thatIn(dataSetCodes);

        DataSetFetchOptions dataSetFetchOptions = new DataSetFetchOptions();
        dataSetFetchOptions.withType();
        dataSetFetchOptions.withDataStore();
        dataSetFetchOptions.withPhysicalData();

        SampleFetchOptions sampleFetchOptions = dataSetFetchOptions.withSample();
        sampleFetchOptions.withType();
        sampleFetchOptions.withSpace();
        sampleFetchOptions.withProject().withSpace();

        ExperimentFetchOptions experimentFetchOptions = dataSetFetchOptions.withExperiment();
        experimentFetchOptions.withType();
        experimentFetchOptions.withProject().withSpace();

        Configuration configuration = ServiceProvider.getInstance().getConfiguration();
        OpenBISConfiguration openBISConfiguration = OpenBISConfiguration.getInstance(configuration);

        List<DataSet> dataSets = openBISConfiguration.getOpenBISFacade().searchDataSets(criteria, dataSetFetchOptions).getObjects();
        return dataSets.stream().map(DTOTranslator::translateToSimpleDataSet).toList();
    }

    private void updateDataSetSize(SimpleDataSetInformationDTO dataSet)
    {
        final File dataSetInStore = new File(new File(storeRoot, dataSet.getDataSetShareId()), dataSet.getDataSetLocation());

        if (FileOperations.getMonitoredInstanceForCurrentThread().exists(dataSetInStore))
        {
            operationLog.info("Calculating size of " + dataSetInStore);
            long startMillis = System.currentTimeMillis();
            long size = FileUtils.sizeOfDirectory(dataSetInStore);
            long endMillis = System.currentTimeMillis();
            operationLog.info("Data set " + dataSet.getDataSetCode() + " stored in " + dataSetInStore + " contains " + size + " bytes (calculated in "
                    + DateTimeUtils.renderDuration(endMillis - startMillis) + ")");

            IOpenBISService service = ShufflingServiceProviderFactory.getInstance().getOpenBISService();
            service.updateSize(dataSet.getDataSetCode(), size);
            dataSet.setDataSetSize(size);
        } else
        {
            operationLog.warn(
                    "Couldn't calculate data set " + dataSet.getDataSetCode() + " size as it no longer exists at its location "
                            + dataSetInStore.getAbsolutePath());
        }
    }

    private void shuffleDataSet(SimpleDataSetInformationDTO dataSet)
    {
        if (!dataSet.getStatus().isAvailable())
        {
            operationLog.warn("Data set " + dataSet.getDataSetCode() + " couldn't be shuffled because "
                    + "its archiving status is " + dataSet.getStatus());
            return;
        }

        Share shareWithMostFreeOrNull = finder.tryToFindShare(dataSet, shares);

        if (shareWithMostFreeOrNull == null)
        {
            String message = "No share found for shuffling data set " + dataSet.getDataSetCode() + ".";
            if (stopOnNoShareFound)
            {
                notificationLog.error(message);
                throw new RuntimeException(message);
            } else
            {
                operationLog.warn(message);
                return;
            }
        }

        String shareId = shareWithMostFreeOrNull.getShareId();

        try
        {
            long freeSpaceBefore = shareWithMostFreeOrNull.calculateFreeSpace();
            File share = new File(storeRoot, dataSet.getDataSetShareId());
            IChecksumProvider checksumProvider = verifyChecksum ? ShufflingServiceProviderFactory.getInstance().getChecksumProvider() : null;
            DataSetMover dataSetMover = new DataSetMover(ShufflingServiceProviderFactory.getInstance().getOpenBISService(),
                    ShufflingServiceProviderFactory.getInstance().getShareIdManager());

            dataSetMover.moveDataSetToAnotherShare(
                    new File(share, dataSet.getDataSetLocation()),
                    shareWithMostFreeOrNull.getShare(), checksumProvider, new SimpleLogger(operationLog));

            operationLog.info("Data set " + dataSet.getDataSetCode()
                    + " successfully moved from share " + dataSet.getDataSetShareId()
                    + " to " + shareId + ".");

            long freeSpaceAfter = shareWithMostFreeOrNull.calculateFreeSpace();
            if (freeSpaceBefore > freeSpaceLimitTriggeringNotification
                    && freeSpaceAfter < freeSpaceLimitTriggeringNotification)
            {
                notificationLog.warn("After moving data set " + dataSet.getDataSetCode() + " to share " + shareId
                        + " that share has only "
                        + FileUtilities.byteCountToDisplaySize(freeSpaceAfter)
                        + " free space. It might be necessary to add a new share.");
            }
        } catch (Throwable t)
        {
            operationLog.error("Couldn't move data set " + dataSet.getDataSetCode()
                    + " to share " + shareId + ".", t);
            throw t;
        }
    }

    private Set<String> codesSet(List<SimpleDataSetInformationDTO> dataSets)
    {
        return dataSets.stream().map(SimpleDataSetInformationDTO::getDataSetCode).collect(Collectors.toSet());
    }

    private List<String> codesList(List<SimpleDataSetInformationDTO> dataSets)
    {
        return dataSets.stream().map(SimpleDataSetInformationDTO::getDataSetCode).collect(Collectors.toList());
    }

}
