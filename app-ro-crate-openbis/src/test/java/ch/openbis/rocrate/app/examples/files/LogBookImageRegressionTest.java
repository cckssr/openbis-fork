package ch.openbis.rocrate.app.examples.files;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.writer.Writer;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.reader.ZipReader;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LogBookImageRegressionTest
{

    public static final String TEMP_FILE = "/tmp/2026-07-29-regression-case.zip";

    @Test
    public void testCrateToOpenBisWithCollectionFile() throws Exception
    {
        Path path = Paths.get(
                "src/test/resources/files/2026-07-29-regression-case.zip");
        OpenBisModel excelModel = ExcelReader.convert(ExcelReader.Format.ZIP_EXPORT, path);
        Writer writer = new Writer();
        writer.write(excelModel, Path.of(TEMP_FILE));
        RoCrateReader roCrateFolderReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateFolderReader.readCrate(TEMP_FILE);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);
        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (IType type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        RdfToModel.ConversionResult convert =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of());
        OpenBisModel
                openBisModel =
                convert.openBisModel();

        Assert.assertTrue(
                openBisModel.getImageFiles().values().stream().anyMatch(x -> !x.isEmpty()));

    }

}
