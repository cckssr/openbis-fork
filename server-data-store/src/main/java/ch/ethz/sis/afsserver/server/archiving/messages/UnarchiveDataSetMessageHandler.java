package ch.ethz.sis.afsserver.server.archiving.messages;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ethz.sis.shared.log.classic.impl.Logger;

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
import ch.ethz.sis.openbis.messages.UnarchiveDataSetMessage;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.collection.CollectionUtils;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverTaskContext;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverPlugin;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

public class UnarchiveDataSetMessageHandler implements IMessageHandler
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, UnarchiveDataSetMessageHandler.class);

    @Override public Set<String> getSupportedMessageTypes()
    {
        return Set.of(UnarchiveDataSetMessage.TYPE);
    }

    @Override public void handleMessage(final Message message)
    {
        Configuration configuration = ServiceProvider.getInstance().getConfiguration();
        JsonObjectMapper jsonObjectMapper = AtomicFileSystemServerParameterUtil.getJsonObjectMapper(configuration);
        UnarchiveDataSetMessage unarchiveMessage = UnarchiveDataSetMessage.deserialize(jsonObjectMapper, message);

        if (unarchiveMessage.getDataSetCodes().isEmpty())
        {
            return;
        }

        IArchiverPlugin archiverPlugin = ServiceProvider.getInstance().getArchiverPlugin();
        List<String> enhancedCodes = archiverPlugin.getDataSetCodesForUnarchiving(unarchiveMessage.getDataSetCodes());

        List<DataSet> foundDataSets = findDataSets(enhancedCodes);

        if (foundDataSets.isEmpty())
        {
            operationLog.info(
                    "Could not find any of the data sets to be unarchived: " + CollectionUtils.abbreviate(unarchiveMessage.getDataSetCodes(),
                            CollectionUtils.DEFAULT_MAX_LENGTH) + ". Nothing will be unarchived.");
            return;
        } else
        {
            Set<String> notFoundDataSetCodes = new LinkedHashSet<>(unarchiveMessage.getDataSetCodes());
            notFoundDataSetCodes.removeAll(codesSet(foundDataSets));

            if (!notFoundDataSetCodes.isEmpty())
            {
                operationLog.info(
                        "The following data sets to be unarchived could not be found: " + CollectionUtils.abbreviate(notFoundDataSetCodes,
                                CollectionUtils.DEFAULT_MAX_LENGTH) + ". Only those found will be unarchived: " + CollectionUtils.abbreviate(
                                codesList(foundDataSets), CollectionUtils.DEFAULT_MAX_LENGTH));
            }
        }

        List<DataSet> archivedDataSets = new ArrayList<>();
        List<DataSet> notArchivedDataSets = new ArrayList<>();

        for (DataSet dataSet : foundDataSets)
        {
            if (ArchivingStatus.ARCHIVED.equals(dataSet.getPhysicalData().getStatus()))
            {
                archivedDataSets.add(dataSet);
            } else
            {
                notArchivedDataSets.add(dataSet);
            }
        }

        if (archivedDataSets.isEmpty())
        {
            operationLog.info(
                    "All data sets to be archived have archiving status != '" + ArchivingStatus.ARCHIVED + "'. Nothing will be unarchived.");
            return;
        } else if (!notArchivedDataSets.isEmpty())
        {
            operationLog.info(
                    "The following data sets have archiving status != '" + ArchivingStatus.ARCHIVED
                            + "' therefore they will not be unarchived: "
                            + CollectionUtils.abbreviate(codesList(notArchivedDataSets), CollectionUtils.DEFAULT_MAX_LENGTH));
        }

        unarchiveDataSets(archivedDataSets);
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

    private void unarchiveDataSets(List<DataSet> archivedDataSets)
    {
        IOpenBISService openBISService = ServiceProvider.getInstance().getOpenBISService();
        openBISService.updateDataSetStatuses(codesList(archivedDataSets), DataSetArchivingStatus.UNARCHIVE_PENDING, null);

        try
        {
            List<DatasetDescription> dataSetDescriptions =
                    archivedDataSets.stream().map(DTOTranslator::translateToDescription).collect(Collectors.toList());

            IArchiverPlugin archiverPlugin = ServiceProvider.getInstance().getArchiverPlugin();
            ArchiverTaskContext archiverTaskContext = ServiceProvider.getInstance().getArchiverContextFactory().createContext();

            ServiceProvider.getInstance().getShareIdManager().lock(codesList(archivedDataSets));

            archiverPlugin.unarchive(dataSetDescriptions, archiverTaskContext);
        } catch (Exception e)
        {
            operationLog.error(
                    "Unarchiving failed for data sets " + CollectionUtils.abbreviate(codesList(archivedDataSets), CollectionUtils.DEFAULT_MAX_LENGTH),
                    e);
            ServiceProvider.getInstance().getDataSetStatusUpdater()
                    .scheduleUpdate(codesList(archivedDataSets), DataSetArchivingStatus.ARCHIVED, null);
        } finally
        {
            ServiceProvider.getInstance().getShareIdManager().releaseLocks(codesList(archivedDataSets));
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
