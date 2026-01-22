package ch.ethz.sis.openbis.generic.excel.v3.to.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.util.Map;

public class DataPathHelperTest
{

    @Test
    public void testObjectWithSpaceAndProject()
    {
        Sample sample = new Sample();

        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withProject();
        sampleFetchOptions.withSpace();
        sampleFetchOptions.withExperiment();
        sampleFetchOptions.withProperties();
        sample.setFetchOptions(sampleFetchOptions);

        sample.setProperties(Map.of("NAME", "Testentry"));
        sample.setCode("ENTRY1");
        Space space = new Space();
        space.setCode("SPACE1");
        sample.setSpace(space);
        Project project = new Project();
        project.setCode("PROJECT1");
        project.setSpace(space);
        sample.setProject(project);

        OpenBisModel.FileInfo fileInfo =
                new OpenBisModel.FileInfo("a", "/stuff/more/out.txt", new byte[] {});

        String path = DataPathHelper.getPath(fileInfo, sample);
        Assert.assertEquals("hierarchy/SPACE1/PROJECT1/Testentry (ENTRY1)/data/out.txt", path);

    }

}