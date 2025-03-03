package ch.systemsx.cisd.openbis.dss.generic.shared;

import java.util.List;
import java.util.Map;

import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

public interface IArchiverTask
{

    ProcessingStatus process(List<DatasetDescription> datasets, Map<String, String> parameterBindings);

}
