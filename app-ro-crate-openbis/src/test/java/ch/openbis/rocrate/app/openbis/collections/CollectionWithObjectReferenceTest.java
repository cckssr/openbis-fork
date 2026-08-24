package ch.openbis.rocrate.app.openbis.collections;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.writer.Writer;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.reader.ZipReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionWithObjectReferenceTest
{
    static final String EXCEL_IN =
            "src/test/resources/openbis/collections/collection_with_object_reference.xlsx";

    @Test
    public void testToRoCrate() throws IOException
    {
        Path path = Paths.get(EXCEL_IN);
        OpenBisModel excelModel = ExcelReader.convert(ExcelReader.Format.EXCEL, path);
        ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment experiment =
                excelModel.getEntities().values().stream().filter(x -> x instanceof Experiment)
                        .map(Experiment.class::cast)
                .findFirst().orElseThrow();
        ExperimentType experimentType = experiment.getType();
        Assert.assertEquals(16, experiment.getProperties().size());
        Assert.assertEquals(16, experimentType.getPropertyAssignments().size());
        Assert.assertEquals("/DEFAULT/DEFAULT/ENTRY6",
                experiment.getProperties().get("DEFAULT_EXPERIMENT.OBJECT").toString());

    }

    @Test
    public void testRoundTrip() throws Exception
    {
        Path path = Paths.get(EXCEL_IN);
        OpenBisModel excelModel = ExcelReader.convert(ExcelReader.Format.EXCEL, path);
        Writer writer = new Writer();
        Path outPath = Path.of("/tmp/ro-crate-collections-out.zip");
        writer.write(excelModel, outPath);
        RoCrateReader roCrateFolderReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateFolderReader.readCrate(outPath.toString());
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (IType type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();
        Sample sample =
                openBisModel.getEntities().values().stream().filter(x -> x instanceof Sample)
                        .map(Sample.class::cast)
                        .findFirst().orElseThrow();
        Experiment experiment = sample.getExperiment();
        ExperimentType experimentType = experiment.getType();
        Assert.assertEquals(3,
                experiment.getProperties().size()); // Empty properties are not included
        Assert.assertEquals("Collection Name",
                experiment.getProperties().get("NAME").toString());
        Assert.assertEquals(16, experimentType.getPropertyAssignments().size()); // excluding NAME
        String val = ((Serializable[]) experiment.getProperties()
                .get("DEFAULT_EXPERIMENT.OBJECT"))[0].toString();
        Assert.assertEquals("/DEFAULT/DEFAULT/ENTRY6", val);
        Assert.assertEquals(1,
                openBisModel.getEntities().values().stream().filter(x -> x instanceof Experiment)
                        .count());
        Assert.assertEquals(1, openBisModel.getEntityTypes().values().stream()
                .filter(x -> x instanceof ExperimentType).count());

        byte[] convert = ExcelWriter.convert(ExcelWriter.Format.ZIP_EXPORT, openBisModel);
        Files.write(Path.of("/tmp/lol.zip"), convert);

    }

}
