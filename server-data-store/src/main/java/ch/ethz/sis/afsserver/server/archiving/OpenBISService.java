package ch.ethz.sis.afsserver.server.archiving;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.ethz.sis.afsserver.server.common.OpenBISFacade;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.ArchivingStatus;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.LinkedDataFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.PhysicalDataFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.fetchoptions.TagFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.id.TagPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.search.TagSearchCriteria;
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

        PhysicalDataFetchOptions physicalDataFetchOptions = dataSetFetchOptions.withPhysicalData();
        physicalDataFetchOptions.withLocatorType();
        physicalDataFetchOptions.withStorageFormat();

        LinkedDataFetchOptions linkedDataFetchOptions = dataSetFetchOptions.withLinkedData();
        linkedDataFetchOptions.withExternalDms();

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

        SearchResult<ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet> searchResult =
                openBISFacade.searchDataSets(criteria, dataSetFetchOptions);

        if (!searchResult.getObjects().isEmpty())
        {
            ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet dataSet = searchResult.getObjects().get(0);
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
        ExperimentSearchCriteria criteria = new ExperimentSearchCriteria();
        criteria.withIdentifier().thatEquals(experimentIdentifier.toString());

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

        SearchResult<ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment> searchResult =
                openBISFacade.searchExperiments(criteria, experimentFetchOptions);

        if (!searchResult.getObjects().isEmpty())
        {
            ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment experiment = searchResult.getObjects().get(0);
            return DTOTranslator.translate(experiment);
        } else
        {
            return null;
        }
    }

    @Override public Sample tryGetSampleWithExperiment(final SampleIdentifier sampleIdentifier) throws UserFailureException
    {
        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withIdentifier().thatEquals(sampleIdentifier.toString());

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();

        SearchResult<ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample> searchResult =
                openBISFacade.searchSamples(criteria, fetchOptions);

        if (!searchResult.getObjects().isEmpty())
        {
            ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample sample = searchResult.getObjects().get(0);
            return DTOTranslator.translate(sample);
        } else
        {
            return null;
        }
    }

    @Override public Metaproject tryGetMetaproject(final String metaprojectName, final String metaprojectOwner)
    {
        TagSearchCriteria criteria = new TagSearchCriteria();
        criteria.withId().thatEquals(new TagPermId(metaprojectOwner, metaprojectName));

        TagFetchOptions fetchOptions = new TagFetchOptions();

        SearchResult<ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.Tag> searchResult =
                openBISFacade.searchTags(criteria, fetchOptions);

        if (!searchResult.getObjects().isEmpty())
        {
            ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.Tag tag = searchResult.getObjects().get(0);
            return DTOTranslator.translate(tag);
        } else
        {
            return null;
        }
    }

    @Override public List<AbstractExternalData> listDataSetsByCode(final List<String> dataSetCodes)
    {
        DataSetSearchCriteria criteria = new DataSetSearchCriteria();
        criteria.withDataStore().withKind().thatIn(DataStoreKind.AFS);
        criteria.withCodes().thatIn(dataSetCodes);
        return listDataSets(criteria);
    }

    @Override public List<SimpleDataSetInformationDTO> listPhysicalDataSets() throws UserFailureException
    {
        return List.of();
    }

    @Override public List<AbstractExternalData> listAvailableDataSets(final ArchiverDataSetCriteria archiverCriteria)
    {
        DataSetSearchCriteria criteria = new DataSetSearchCriteria();
        criteria.withDataStore().withKind().thatIn(DataStoreKind.AFS);
        criteria.withPhysicalData().withStatus().thatEquals(ArchivingStatus.AVAILABLE);
        criteria.withPhysicalData().withPresentInArchive().thatEquals(archiverCriteria.isPresentInArchive());

        Calendar accessDate = Calendar.getInstance();
        accessDate.setTime(new Date());
        accessDate.add(Calendar.DAY_OF_MONTH, -archiverCriteria.getOlderThan());
        criteria.withAccessDate().thatIsEarlierThan(accessDate.getTime());

        if (archiverCriteria.tryGetDataSetTypeCode() != null)
        {
            criteria.withType().withCode().thatEquals(archiverCriteria.tryGetDataSetTypeCode());
        }

        return listDataSets(criteria);
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

    private List<AbstractExternalData> listDataSets(DataSetSearchCriteria criteria)
    {
        SpaceFetchOptions spaceFetchOptions = new SpaceFetchOptions();

        ProjectFetchOptions projectFetchOptions = new ProjectFetchOptions();
        projectFetchOptions.withSpaceUsing(spaceFetchOptions);

        ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
        experimentFetchOptions.withType();
        experimentFetchOptions.withProjectUsing(projectFetchOptions);

        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withType();
        sampleFetchOptions.withSpaceUsing(spaceFetchOptions);
        sampleFetchOptions.withProjectUsing(projectFetchOptions);
        sampleFetchOptions.withExperimentUsing(experimentFetchOptions);

        PhysicalDataFetchOptions physicalDataFetchOptions = new PhysicalDataFetchOptions();
        physicalDataFetchOptions.withStorageFormat();
        physicalDataFetchOptions.withLocatorType();

        LinkedDataFetchOptions linkedDataFetchOptions = new LinkedDataFetchOptions();
        linkedDataFetchOptions.withExternalDms();

        DataSetFetchOptions simpleDataSetsFetchOptions = new DataSetFetchOptions();
        simpleDataSetsFetchOptions.withType();
        simpleDataSetsFetchOptions.withDataStore();
        simpleDataSetsFetchOptions.withRegistrator();
        simpleDataSetsFetchOptions.withModifier();
        simpleDataSetsFetchOptions.withSample();
        simpleDataSetsFetchOptions.withExperiment();
        simpleDataSetsFetchOptions.withPhysicalDataUsing(physicalDataFetchOptions);
        simpleDataSetsFetchOptions.withLinkedDataUsing(linkedDataFetchOptions);

        DataSetFetchOptions dataSetFetchOptions = new DataSetFetchOptions();
        dataSetFetchOptions.withType().withPropertyAssignments().withPropertyType();
        dataSetFetchOptions.withProperties();
        dataSetFetchOptions.withTags();
        dataSetFetchOptions.withExperimentUsing(experimentFetchOptions);
        dataSetFetchOptions.withSampleUsing(sampleFetchOptions);
        dataSetFetchOptions.withContainersUsing(dataSetFetchOptions);
        dataSetFetchOptions.withComponentsUsing(simpleDataSetsFetchOptions);
        dataSetFetchOptions.withParentsUsing(simpleDataSetsFetchOptions);
        dataSetFetchOptions.withChildrenUsing(simpleDataSetsFetchOptions);

        SearchResult<ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet> searchResult =
                openBISFacade.searchDataSets(criteria, dataSetFetchOptions);

        return searchResult.getObjects().stream().map(DTOTranslator::translate).collect(Collectors.toList());
    }

}
