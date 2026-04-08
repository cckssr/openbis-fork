package ch.openbis.rocrate.app.openbis.structure;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StructureReconstructionTest extends TestCase
{
    @Test
    public void testSpaceSample() throws IOException
    {
        String location = "src/test/resources/openbis/structure/space_sample/";
        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateFolderReader.readCrate(location);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (var type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();
        AbstractEntityPropertyHolder abstractEntityPropertyHolder =
                openBisModel.getEntities().values().stream().findFirst().orElseThrow();
        Sample sample = (Sample) abstractEntityPropertyHolder;
        Assert.assertEquals("MATERIALS", sample.getSpace().getCode());
        Assert.assertNull(sample.getProject());

    }

    @Test
    public void testSpaceProjectCollectionSample() throws IOException
    {
        String location = "src/test/resources/openbis/structure/space_project_sample_collection/";
        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateFolderReader.readCrate(location);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (var type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();
        AbstractEntityPropertyHolder abstractEntityPropertyHolder =
                openBisModel.getEntities().values().stream().findFirst().orElseThrow();
        Sample sample = (Sample) abstractEntityPropertyHolder;
        Assert.assertEquals("SPACE", sample.getSpace().getCode());
        Assert.assertEquals("PROJECT", sample.getProject().getCode());
        Assert.assertNotNull(sample.getExperiment());

    }

    @Test
    public void testSpaceProjectSample() throws IOException
    {
        String location = "src/test/resources/openbis/structure/space_project_sample/";
        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateFolderReader.readCrate(location);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (var type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();
        AbstractEntityPropertyHolder abstractEntityPropertyHolder =
                openBisModel.getEntities().values().stream().findFirst().orElseThrow();
        Sample sample = (Sample) abstractEntityPropertyHolder;
        Assert.assertEquals("SPACE", sample.getSpace().getCode());
        Assert.assertEquals("PROJECT", sample.getProject().getCode());

    }


}
