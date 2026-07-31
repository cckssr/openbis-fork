package ch.openbis.rocrate.app.openbis.collections;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;
import ch.openbis.rocrate.app.reader.RdfToModel;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionRegressionTest
{

    @Test
    public void testRegressionCase() throws IOException
    {

        String inPath = "src/test/resources/openbis/collections/regression/2026-07-31";
        String outPath = "/tmp/xlsx_out.zip";
        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());

        RoCrate crate = roCrateFolderReader.readCrate(inPath);
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

        Assert.assertTrue(openBisModel.getCollections().stream()
                .allMatch(x -> StringUtils.isNotBlank(x.getCode())));
        Assert.assertTrue(
                openBisModel.getCollections().stream().noneMatch(x -> x.getProject() == null));

        byte[] convert = ExcelWriter.convert(ExcelWriter.Format.ZIP_EXPORT, openBisModel);
        Files.write(Path.of(outPath), convert);

    }

}
