package ch.openbis.rocrate.app.examples.files.image;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.IFileInfo;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.writer.Writer;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.reader.ZipReader;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MultipleImageReferencesTest
{
    private static final String INPUT =
            "src/test/resources/examples/regression/bis-3040-file-name-collisions/bis-3040-file-name-collisions.zip";

    private static final String OUTPUT = "/tmp/multiple-image-references-crate.zip";

    @Test
    public void testMultipleReferences() throws Exception
    {
        OpenBisModel openBisModel = ExcelReader.convert(ExcelReader.Format.EXCEL, Path.of(INPUT));
        Writer writer = new Writer();
        writer.write(openBisModel, Path.of(OUTPUT));

        RoCrateReader roCrateFolderReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateFolderReader.readCrate(OUTPUT);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (IType type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel
                openBisModel2 =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();

        SampleIdentifier sampleIdentifier =
                new SampleIdentifier("/ARANTXA_AGOTE_ARAN/JJJ2/EXP12399");
        List<IFileInfo> filesExcel = openBisModel.getImageFiles().get(sampleIdentifier);

        int numDistinctFiles =
                filesExcel.stream().map(x -> x.filePath()).collect(Collectors.toSet()).size();

        List<IFileInfo> filesRoCrate = openBisModel2.getImageFiles().get(sampleIdentifier);

        Assert.assertEquals(numDistinctFiles, filesRoCrate.size());

    }

}
