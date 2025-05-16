package ch.systemsx.cisd.openbis.dss.generic.shared;

import java.util.List;
import java.util.Map;

import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

public interface IArchiverTaskScheduler
{

    public void scheduleTask(String taskKey, IArchiverTask task, Map<String, String> parameterBindings,
            List<DatasetDescription> datasets, String userId, String userEmailOrNull, String userSessionToken);

}
