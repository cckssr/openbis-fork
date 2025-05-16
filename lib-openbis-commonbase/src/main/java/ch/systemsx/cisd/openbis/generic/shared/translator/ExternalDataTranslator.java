package ch.systemsx.cisd.openbis.generic.shared.translator;

import java.util.List;

import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractExternalData;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ContainerDataSet;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ExperimentType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.PhysicalDataSet;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Project;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Sample;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.SampleType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Space;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

public class ExternalDataTranslator
{

    public static DatasetDescription translateToDescription(AbstractExternalData data)
    {
        DatasetDescription description = new DatasetDescription();
        description.setDataSetCode(data.getCode());

        description.setDataStoreCode(data.getDataStore().getCode());
        description.setRegistrationTimestamp(data.getRegistrationDate());
        List<ContainerDataSet> containerDataSets = data.getContainerDataSets();
        for (ContainerDataSet containerDataSet : containerDataSets)
        {
            String containerDataSetCode = containerDataSet.getCode();
            description.addOrderInContainer(containerDataSetCode, data.getOrderInContainer(containerDataSetCode));
        }

        PhysicalDataSet dataSet = data.tryGetAsDataSet();
        if (dataSet != null)
        {
            description.setDataSetLocation(dataSet.getLocation());
            description.setDataSetShareId(dataSet.getShareId());
            description.setSpeedHint(dataSet.getSpeedHint());
            description.setFileFormatType(dataSet.getFileFormatType().getCode());
            description.setH5Folders(dataSet.isH5Folders());
            description.setH5arFolders(dataSet.isH5arFolders());
            description.setArchivingRequested(dataSet.isArchivingRequested());
        }
        description.setDataSetSize(data.getSize());
        DataSetType dataSetType = data.getDataSetType();
        if (dataSetType != null)
        {
            description.setDatasetTypeCode(dataSetType.getCode());
        }
        Experiment experiment = data.getExperiment();
        if (experiment != null)
        {
            description.setExperimentCode(experiment.getCode());
            description.setExperimentIdentifier(experiment.getIdentifier());
            Project project = experiment.getProject();
            if (project != null)
            {
                description.setProjectCode(project.getCode());
                Space space = project.getSpace();
                if (space != null)
                {
                    description.setSpaceCode(space.getCode());
                }
            }
            ExperimentType experimentType = experiment.getExperimentType();
            if (experimentType != null)
            {
                description.setExperimentTypeCode(experimentType.getCode());
            }
        }
        Sample sample = data.getSample();
        if (sample != null)
        {
            description.setSampleCode(sample.getCode());
            description.setSampleIdentifier(sample.getIdentifier());
            SampleType sampleType = sample.getSampleType();
            if (sampleType != null)
            {
                description.setSampleTypeCode(sampleType.getCode());
            }
            Project project = sample.getProject();
            if (project != null)
            {
                description.setProjectCode(project.getCode());
            }
            Space space = sample.getSpace();
            if (space != null)
            {
                description.setSpaceCode(space.getCode());
            }
        }
        return description;
    }

}
