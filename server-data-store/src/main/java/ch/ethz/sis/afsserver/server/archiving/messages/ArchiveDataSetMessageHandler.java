package ch.ethz.sis.afsserver.server.archiving.messages;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverTaskContext;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverPlugin;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

public class ArchiveDataSetMessageHandler implements IMessageHandler
{
    @Override public Set<String> getSupportedMessageTypes()
    {
        return Set.of(ArchiveDataSetMessage.TYPE);
    }

    @Override public void handleMessage(final Message message)
    {
        Configuration configuration = ServiceProvider.getInstance().getConfiguration();
        OpenBISConfiguration openBISConfiguration = OpenBISConfiguration.getInstance(configuration);
        JsonObjectMapper jsonObjectMapper = AtomicFileSystemServerParameterUtil.getJsonObjectMapper(configuration);

        ArchiveDataSetMessage archiveMessage = ArchiveDataSetMessage.deserialize(jsonObjectMapper, message);

        DataSetSearchCriteria criteria = new DataSetSearchCriteria();
        criteria.withDataStore().withKind().thatIn(DataStoreKind.AFS);
        criteria.withPhysicalData();
        criteria.withCodes().thatIn(archiveMessage.getDataSetCodes());

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

        List<DataSet> dataSets = openBISConfiguration.getOpenBISFacade().searchDataSets(criteria, dataSetFetchOptions).getObjects();

        List<String> notAvailableDataSets =
                dataSets.stream().filter(dataSet -> !ArchivingStatus.AVAILABLE.equals(dataSet.getPhysicalData().getStatus())).map(DataSet::getCode)
                        .collect(Collectors.toList());

        if (!notAvailableDataSets.isEmpty())
        {
            throw new RuntimeException(
                    "Data sets: " + notAvailableDataSets + " cannot be archived as their archiving status is not " + ArchivingStatus.AVAILABLE);
        }

        IOpenBISService openBISService = ServiceProvider.getInstance().getOpenBISService();
        openBISService.updateDataSetStatuses(dataSets.stream().map(DataSet::getCode).collect(Collectors.toList()),
                archiveMessage.isRemoveFromDataStore() ? DataSetArchivingStatus.ARCHIVE_PENDING : DataSetArchivingStatus.BACKUP_PENDING,
                null);

        try
        {
            List<DatasetDescription> dataSetDescriptions = dataSets.stream().map(DTOTranslator::translateToDescription).collect(Collectors.toList());

            IArchiverPlugin archiverPlugin = ServiceProvider.getInstance().getArchiverPlugin();
            ArchiverTaskContext archiverTaskContext = ServiceProvider.getInstance().getArchiverContextFactory().createContext();
            archiverTaskContext.setOptions(archiveMessage.getOptions());

            archiverPlugin.archive(dataSetDescriptions, archiverTaskContext, archiveMessage.isRemoveFromDataStore());
        } catch (Exception e)
        {
            ServiceProvider.getInstance().getDataSetStatusUpdater()
                    .scheduleUpdate(dataSets.stream().map(DataSet::getCode).collect(Collectors.toList()), DataSetArchivingStatus.AVAILABLE, null);
        }
    }
}
