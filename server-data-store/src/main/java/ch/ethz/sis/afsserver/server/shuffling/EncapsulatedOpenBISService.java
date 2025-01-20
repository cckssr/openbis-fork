/*
 * Copyright ETH 2007 - 2023 Zürich, Scientific IT Services
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

import java.util.List;
import java.util.stream.Collectors;

import ch.ethz.sis.afsserver.server.common.OpenBISFacade;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.PhysicalDataUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;

public class EncapsulatedOpenBISService implements IEncapsulatedOpenBISService
{

    private final OpenBISFacade openBISFacade;

    public EncapsulatedOpenBISService(OpenBISFacade openBISFacade)
    {
        this.openBISFacade = openBISFacade;
    }

    @Override public List<Experiment> listExperiments(final ExperimentSearchCriteria criteria, final ExperimentFetchOptions fetchOptions)
    {
        return openBISFacade.searchExperiments(criteria, fetchOptions).getObjects();
    }

    @Override public List<Sample> listSamples(final SampleSearchCriteria criteria, final SampleFetchOptions fetchOptions)
    {
        return openBISFacade.searchSamples(criteria, fetchOptions).getObjects();
    }

    @Override public List<SimpleDataSetInformationDTO> listDataSets(final DataSetSearchCriteria criteria, final DataSetFetchOptions fetchOptions)
    {
        criteria.withDataStore().withKind().thatIn(DataStoreKind.AFS);

        fetchOptions.withType();
        fetchOptions.withPhysicalData();
        fetchOptions.withDataStore();
        fetchOptions.withExperiment().withProject().withSpace();
        fetchOptions.withSample();

        SearchResult<DataSet> searchResult = openBISFacade.searchDataSets(criteria, fetchOptions);

        return searchResult.getObjects().stream().map(EncapsulatedOpenBISService::convert).collect(Collectors.toList());
    }

    @Override public SimpleDataSetInformationDTO tryGetDataSet(String dataSetCode)
    {
        DataSetSearchCriteria criteria = new DataSetSearchCriteria();
        criteria.withDataStore().withKind().thatIn(DataStoreKind.AFS);
        criteria.withCode().thatEquals(dataSetCode);

        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withType();
        fetchOptions.withPhysicalData();
        fetchOptions.withDataStore();
        fetchOptions.withExperiment().withProject().withSpace();
        fetchOptions.withSample();

        SearchResult<DataSet> searchResult = openBISFacade.searchDataSets(criteria, fetchOptions);

        if (!searchResult.getObjects().isEmpty())
        {
            DataSet dataSet = searchResult.getObjects().get(0);
            return convert(dataSet);
        } else
        {
            return null;
        }
    }

    @Override public void updateShareIdAndSize(final String dataSetCode, final String shareId, final long size)
    {
        PhysicalDataUpdate physicalDataUpdate = new PhysicalDataUpdate();
        physicalDataUpdate.setShareId(shareId);
        physicalDataUpdate.setSize(size);

        DataSetUpdate update = new DataSetUpdate();
        update.setDataSetId(new DataSetPermId(dataSetCode));
        update.setPhysicalData(physicalDataUpdate);

        openBISFacade.updateDataSets(List.of(update));
    }

    private static SimpleDataSetInformationDTO convert(DataSet dataSet)
    {
        SimpleDataSetInformationDTO simpleDTO = new SimpleDataSetInformationDTO();
        simpleDTO.setDataSetCode(dataSet.getCode());
        simpleDTO.setDataSetType(dataSet.getType().getCode());
        simpleDTO.setDataStoreCode(dataSet.getDataStore().getCode());
        simpleDTO.setDataSetShareId(dataSet.getPhysicalData().getShareId());
        simpleDTO.setDataSetLocation(dataSet.getPhysicalData().getLocation());
        simpleDTO.setDataSetSize(dataSet.getPhysicalData().getSize());
        simpleDTO.setStatus(DataSetArchivingStatus.valueOf(dataSet.getPhysicalData().getStatus().name()));
        simpleDTO.setPresentInArchive(dataSet.getPhysicalData().isPresentInArchive());
        simpleDTO.setSpeedHint(dataSet.getPhysicalData().getSpeedHint());
        simpleDTO.setStorageConfirmed(dataSet.getPhysicalData().isStorageConfirmation());
        simpleDTO.setRegistrationTimestamp(dataSet.getRegistrationDate());
        simpleDTO.setModificationTimestamp(dataSet.getModificationDate());
        simpleDTO.setAccessTimestamp(dataSet.getAccessDate());

        if (dataSet.getExperiment() != null)
        {
            simpleDTO.setExperimentCode(dataSet.getExperiment().getCode());
            simpleDTO.setProjectCode(dataSet.getExperiment().getProject().getCode());
            simpleDTO.setSpaceCode(dataSet.getExperiment().getProject().getSpace().getCode());
            if (dataSet.getExperiment().getImmutableDataDate() != null)
            {
                simpleDTO.setImmutableDataTimestamp(dataSet.getExperiment().getImmutableDataDate());
            }
        }

        if (dataSet.getSample() != null)
        {
            simpleDTO.setSampleCode(dataSet.getSample().getCode());
            if (dataSet.getSample().getImmutableDataDate() != null)
            {
                simpleDTO.setImmutableDataTimestamp(dataSet.getSample().getImmutableDataDate());
            }
        }

        return simpleDTO;
    }

}
