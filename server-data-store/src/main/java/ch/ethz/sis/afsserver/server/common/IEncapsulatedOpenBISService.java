package ch.ethz.sis.afsserver.server.common;

import java.util.List;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;

public interface IEncapsulatedOpenBISService
{
    List<Experiment> listExperiments(ExperimentSearchCriteria criteria, ExperimentFetchOptions fetchOptions);

    List<Sample> listSamples(SampleSearchCriteria criteria, SampleFetchOptions fetchOptions);

    List<SimpleDataSetInformationDTO> listDataSets(DataSetSearchCriteria criteria, DataSetFetchOptions fetchOptions);

    SimpleDataSetInformationDTO tryGetDataSet(String dataSetCode);

    void updateShareIdAndSize(String dataSetCode, String shareId, long size);
}
