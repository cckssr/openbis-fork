package ch.ethz.sis.afsserver.server.archiving.messages;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsserver.server.common.DTOTranslator;
import ch.ethz.sis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.messages.consumer.IMessageHandler;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.ArchivingStatus;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.messages.ArchiveDataSetMessage;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.collection.CollectionUtils;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverTaskContext;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverPlugin;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

public class ArchiveDataSetMessageHandler implements IMessageHandler
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, ArchiveDataSetMessageHandler.class);

    @Override public Set<String> getSupportedMessageTypes()
    {
        return Set.of(ArchiveDataSetMessage.TYPE);
    }

    @Override public void handleMessage(final Message message)
    {
        Configuration configuration = ServiceProvider.getInstance().getConfiguration();
        JsonObjectMapper jsonObjectMapper = AtomicFileSystemServerParameterUtil.getJsonObjectMapper(configuration);
        ArchiveDataSetMessage archiveMessage = ArchiveDataSetMessage.deserialize(jsonObjectMapper, message);

        if (archiveMessage.getDataSetCodes().isEmpty())
        {
            return;
        }

        List<DataSet> foundDataSets = findDataSets(archiveMessage.getDataSetCodes());

        if (foundDataSets.isEmpty())
        {
            operationLog.info("Could not find any of the data sets to be archived: " + CollectionUtils.abbreviate(archiveMessage.getDataSetCodes(),
                    CollectionUtils.DEFAULT_MAX_LENGTH) + ". Nothing to archive.");
            return;
        } else
        {
            Set<String> notFoundDataSetCodes = new LinkedHashSet<>(archiveMessage.getDataSetCodes());
            notFoundDataSetCodes.removeAll(codesSet(foundDataSets));

            if (!notFoundDataSetCodes.isEmpty())
            {
                operationLog.info(
                        "The following data sets to be archived could not be found: " + CollectionUtils.abbreviate(notFoundDataSetCodes,
                                CollectionUtils.DEFAULT_MAX_LENGTH) + ". Only those found will be archived: " + CollectionUtils.abbreviate(
                                codesList(foundDataSets), CollectionUtils.DEFAULT_MAX_LENGTH));
            }
        }

        List<DataSet> availableDataSets = new ArrayList<>();
        List<DataSet> notAvailableDataSets = new ArrayList<>();

        for (DataSet dataSet : foundDataSets)
        {
            if (ArchivingStatus.AVAILABLE.equals(dataSet.getPhysicalData().getStatus()))
            {
                availableDataSets.add(dataSet);
            } else
            {
                notAvailableDataSets.add(dataSet);
            }
        }

        if (availableDataSets.isEmpty())
        {
            operationLog.info(
                    "All data sets to be archived have archiving status != '" + ArchivingStatus.AVAILABLE + "'. Nothing to archive.");
            return;
        } else if (!notAvailableDataSets.isEmpty())
        {
            operationLog.info(
                    "The following data sets have archiving status != '" + ArchivingStatus.AVAILABLE
                            + "' therefore they will not be archived: "
                            + CollectionUtils.abbreviate(codesList(notAvailableDataSets), CollectionUtils.DEFAULT_MAX_LENGTH));
        }

        archiveDataSets(availableDataSets, archiveMessage.isRemoveFromDataStore(), archiveMessage.getOptions());
    }

    private List<DataSet> findDataSets(List<String> dataSetCodes)
    {
        DataSetSearchCriteria criteria = new DataSetSearchCriteria();
        criteria.withDataStore().withKind().thatIn(DataStoreKind.AFS);
        criteria.withPhysicalData();
        criteria.withCodes().thatIn(dataSetCodes);

        DataSetFetchOptions dataSetFetchOptions = new DataSetFetchOptions();
        dataSetFetchOptions.withType();
        dataSetFetchOptions.withDataStore();
        dataSetFetchOptions.withPhysicalData().withFileFormatType();
        dataSetFetchOptions.withContainers();

        SampleFetchOptions sampleFetchOptions = dataSetFetchOptions.withSample();
        sampleFetchOptions.withType();
        sampleFetchOptions.withSpace();
        sampleFetchOptions.withProject();

        ExperimentFetchOptions experimentFetchOptions = dataSetFetchOptions.withExperiment();
        experimentFetchOptions.withType();
        experimentFetchOptions.withProject().withSpace();

        Configuration configuration = ServiceProvider.getInstance().getConfiguration();
        OpenBISConfiguration openBISConfiguration = OpenBISConfiguration.getInstance(configuration);

        return openBISConfiguration.getOpenBISFacade().searchDataSets(criteria, dataSetFetchOptions).getObjects();
    }

    private void archiveDataSets(List<DataSet> availableDataSets, boolean removeFromDataStore, Map<String, String> options)
    {
        IOpenBISService openBISService = ServiceProvider.getInstance().getOpenBISService();
        openBISService.updateDataSetStatuses(codesList(availableDataSets),
                removeFromDataStore ? DataSetArchivingStatus.ARCHIVE_PENDING : DataSetArchivingStatus.BACKUP_PENDING,
                null);

        try
        {
            List<DatasetDescription> dataSetDescriptions =
                    availableDataSets.stream().map(DTOTranslator::translateToDescription).collect(Collectors.toList());

            IArchiverPlugin archiverPlugin = ServiceProvider.getInstance().getArchiverPlugin();
            ArchiverTaskContext archiverTaskContext = ServiceProvider.getInstance().getArchiverContextFactory().createContext();
            archiverTaskContext.setOptions(options);

            archiverPlugin.archive(dataSetDescriptions, archiverTaskContext, removeFromDataStore);
        } catch (Exception e)
        {
            ServiceProvider.getInstance().getDataSetStatusUpdater()
                    .scheduleUpdate(codesList(availableDataSets), DataSetArchivingStatus.AVAILABLE, null);
        }
    }

    private Set<String> codesSet(List<DataSet> dataSets)
    {
        return dataSets.stream().map(DataSet::getCode).collect(Collectors.toSet());
    }

    private List<String> codesList(List<DataSet> dataSets)
    {
        return dataSets.stream().map(DataSet::getCode).collect(Collectors.toList());
    }

}
