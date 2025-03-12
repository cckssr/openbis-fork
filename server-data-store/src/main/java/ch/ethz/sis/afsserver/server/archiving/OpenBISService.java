package ch.ethz.sis.afsserver.server.archiving;

import java.util.Date;
import java.util.List;
import java.util.Map;

import ch.ethz.sis.afsserver.server.common.OpenBISFacade;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractExternalData;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ArchiverDataSetCriteria;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DeletedDataSet;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IDatasetLocationNode;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Metaproject;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Sample;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.id.metaproject.MetaprojectIdentifierId;
import ch.systemsx.cisd.openbis.generic.shared.dto.SimpleDataSetInformationDTO;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.ExperimentIdentifier;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.SampleIdentifier;

public class OpenBISService implements IOpenBISService
{
    private final OpenBISFacade openBISFacade;

    public OpenBISService(OpenBISFacade openBISFacade)
    {
        this.openBISFacade = openBISFacade;
    }

    @Override public String getSessionToken()
    {
        return openBISFacade.getSessionToken();
    }

    @Override public AbstractExternalData tryGetDataSet(final String dataSetCode)
    {
        DataSetSearchCriteria criteria = new DataSetSearchCriteria();
        criteria.withDataStore().withKind().thatIn(DataStoreKind.AFS);
        criteria.withCode().thatEquals(dataSetCode);

        SpaceFetchOptions spaceFetchOptions = new SpaceFetchOptions();
        spaceFetchOptions.withRegistrator();

        ProjectFetchOptions projectFetchOptions = new ProjectFetchOptions();
        projectFetchOptions.withSpaceUsing(spaceFetchOptions);
        projectFetchOptions.withLeader();
        projectFetchOptions.withRegistrator();
        projectFetchOptions.withModifier();

        ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
        experimentFetchOptions.withType().withPropertyAssignments().withPropertyType();
        experimentFetchOptions.withProjectUsing(projectFetchOptions);
        experimentFetchOptions.withProperties();
        experimentFetchOptions.withRegistrator();
        experimentFetchOptions.withModifier();

        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withType().withPropertyAssignments().withPropertyType();
        sampleFetchOptions.withSpaceUsing(spaceFetchOptions);
        sampleFetchOptions.withProjectUsing(projectFetchOptions);
        sampleFetchOptions.withExperimentUsing(experimentFetchOptions);
        sampleFetchOptions.withProperties();
        sampleFetchOptions.withRegistrator();
        sampleFetchOptions.withModifier();

        DataSetFetchOptions dataSetFetchOptions = new DataSetFetchOptions();
        dataSetFetchOptions.withType().withPropertyAssignments().withPropertyType();
        dataSetFetchOptions.withExperimentUsing(experimentFetchOptions);
        dataSetFetchOptions.withSampleUsing(sampleFetchOptions);
        dataSetFetchOptions.withComponentsUsing(dataSetFetchOptions);
        dataSetFetchOptions.withDataStore();
        dataSetFetchOptions.withProperties();
        dataSetFetchOptions.withPhysicalData();
        dataSetFetchOptions.withLinkedData();
        dataSetFetchOptions.withTags();
        dataSetFetchOptions.withRegistrator();
        dataSetFetchOptions.withModifier();

        DataSetFetchOptions containersFetchOptions = dataSetFetchOptions.withContainers();
        containersFetchOptions.withType().withPropertyAssignments().withPropertyType();
        containersFetchOptions.withExperimentUsing(experimentFetchOptions);
        containersFetchOptions.withSampleUsing(sampleFetchOptions);
        containersFetchOptions.withDataStore();
        containersFetchOptions.withProperties();
        containersFetchOptions.withPhysicalData();
        containersFetchOptions.withLinkedData();
        containersFetchOptions.withTags();
        containersFetchOptions.withRegistrator();
        containersFetchOptions.withModifier();

        DataSetFetchOptions parentsFetchOptions = dataSetFetchOptions.withParents();
        parentsFetchOptions.withType();

        DataSetFetchOptions childrenFetchOptions = dataSetFetchOptions.withChildren();
        childrenFetchOptions.withType();

        SearchResult<DataSet> searchResult = openBISFacade.searchDataSets(criteria, dataSetFetchOptions);

        if (!searchResult.getObjects().isEmpty())
        {
            DataSet dataSet = searchResult.getObjects().get(0);
            return DTOTranslator.translate(dataSet);
        } else
        {
            return null;
        }
    }

    @Override public IDatasetLocationNode tryGetDataSetLocation(final String dataSetCode)
    {
        return null;
    }

    @Override public Experiment tryGetExperiment(final ExperimentIdentifier experimentIdentifier) throws UserFailureException
    {
        return null;
    }

    @Override public Sample tryGetSampleWithExperiment(final SampleIdentifier sampleIdentifier) throws UserFailureException
    {
        return null;
    }

    @Override public Metaproject tryGetMetaproject(final String metaprojectName, final String metaprojectOwner)
    {
        return null;
    }

    @Override public List<AbstractExternalData> listDataSetsByCode(final List<String> dataSetCodes)
    {
        return List.of();
    }

    @Override public List<SimpleDataSetInformationDTO> listPhysicalDataSets() throws UserFailureException
    {
        return List.of();
    }

    @Override public List<AbstractExternalData> listAvailableDataSets(final ArchiverDataSetCriteria criteria)
    {
        return List.of();
    }

    @Override public List<AbstractExternalData> listNotArchivedDatasetsWithMetaproject(final MetaprojectIdentifierId metaprojectId)
    {
        return List.of();
    }

    @Override public List<DeletedDataSet> listDeletedDataSets(final Long lastSeenDeletionEventIdOrNull, final Date maxDeletionDateOrNull)
    {
        return List.of();
    }

    @Override public void updateDataSetStatuses(final List<String> dataSetCodes, final DataSetArchivingStatus newStatus,
            final boolean presentInArchive)
            throws UserFailureException
    {

    }

    @Override public void updateShareIdAndSize(final String dataSetCode, final String shareId, final long size)
    {

    }

    @Override public void archiveDataSets(final List<String> dataSetCodes, final boolean removeFromDataStore, final Map<String, String> options)
            throws UserFailureException
    {

    }

    @Override public void notifyDatasetAccess(final String dataSetCode)
    {

    }

    @Override public boolean isDataSetOnTrashCanOrDeleted(final String dataSetCode)
    {
        return false;
    }

}
