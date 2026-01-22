package ch.ethz.sis.openbis.generic.excel.v3.to.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataPathHelper
{
    public static String getPath(OpenBisModel.FileInfo fileInfo,
            AbstractEntityPropertyHolder entity)
    {

        if (entity instanceof Sample)
        {
            Sample sample = (Sample) entity;

            List<String> parts = new ArrayList<>();

            parts.add("hierarchy");

            if (sample.getSpace() != null)
            {
                parts.add(sample.getSpace().getCode());
            }

            if (sample.getProject() != null)
            {

                parts.add(sample.getProject().getCode());
            }

            if (sample.getExperiment() != null)
            {

            }

            parts.add(getNamePart(sample));

            parts.add(ExcelWriter.DATA_DIRECTORY);

            String[] fileInfoParts = fileInfo.filePath().split("/");

            parts.add(fileInfoParts[fileInfoParts.length - 1]);

            return parts.stream().collect(Collectors.joining("/"));
        }
        throw new RuntimeException(entity.getClass().toString() + " not implemented yet");

    }

    private static String getNamePart(Sample sample)
    {
        return sample.getProperty("NAME").toString() + " (" + sample.getCode() + ")";

    }

}
