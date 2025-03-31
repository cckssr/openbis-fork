package ch.systemsx.cisd.openbis.dss.generic.shared;

import java.io.Serializable;
import java.util.List;

import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

public class ArchiverProcessingPluginTask implements IProcessingPluginTask, Serializable
{

    private static final long serialVersionUID = 1L;

    private final IArchiverTask archiverTask;

    public ArchiverProcessingPluginTask(IArchiverTask archiverTask)
    {
        this.archiverTask = archiverTask;
    }

    @Override public ProcessingStatus process(final List<DatasetDescription> datasets, final DataSetProcessingContext context)
    {
        return archiverTask.process(datasets, context.getParameterBindings());
    }
}
