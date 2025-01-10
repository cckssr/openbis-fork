package ch.ethz.sis.afsserver.server.shuffling;

import java.util.List;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;

public interface IEncapsulatedOpenBISService
{
    List<SimpleDataSetInformationDTO> listDataSets(DataSetSearchCriteria criteria, DataSetFetchOptions fetchOptions);

    SimpleDataSetInformationDTO tryGetDataSet(String dataSetCode);

    void updateShareIdAndSize(String dataSetCode, String shareId, long size);
}
