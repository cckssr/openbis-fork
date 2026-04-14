package ch.openbis.rocrate.app.examples.files;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.IFileInfo;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.writer.Writer;
import ch.openbis.rocrate.app.writer.mapping.Mapper;
import ch.openbis.rocrate.app.writer.mapping.types.MapResult;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImageFileTest
{
    static final String INPUT =
            "src/test/resources/files/image/RICH_TEXT_IMAGE";

    static final String INPUT_OPENBIS_XLSX_ZIP =
            "src/test/resources/files/image/export.2026-01-28-11-10-06-076.zip";

    public static final String TMP_OPENBIS_TEST_RO_OUT_ZIP = "/tmp/openbis_test_ro_out.zip";

    @Test
    public void openBisToCrateTest() throws Exception
    {

        String output = "/tmp/openbis_test_ro_out.zip";
        Path path = Paths.get(INPUT_OPENBIS_XLSX_ZIP);
        OpenBisModel excelModel = ExcelReader.convert(ExcelReader.Format.ZIP_EXPORT, path,
                ExcelReader.FileMode.DUMMY);
        Mapper mapper = new Mapper();
        MapResult rocrateModel = mapper.transform(
                excelModel);
        Assert.assertEquals(1, excelModel.getImageFiles().size());
        Assert.assertEquals(0, excelModel.getFiles().size());

        List<IFileInfo> fileInfos =
                excelModel.getImageFiles().values().stream().findFirst().orElseThrow();
        Assert.assertEquals(1, fileInfos.size());
        IFileInfo fileInfo = fileInfos.get(0);
        Assert.assertTrue(fileInfo.filePath()
                .equals("xlsx/miscellaneous/file-service/eln-lims/5c/37/0f/5c370fb6-e2be-4472-ae2a-640e13b2d763/87d28f0c-7f83-449c-a20e-fb445cf968f1.png"));

        Writer writer = new Writer();
        writer.write(excelModel, Path.of(output));
    }

    @Test
    public void crateToOpenBisTest() throws Exception
    {

        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateFolderReader.readCrate(INPUT);
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
                        "DEFAULT", schemaFacade, Map.of());

        Assert.assertEquals(2, openBisModel.getFiles().size());

        List<IFileInfo> fileInfos =
                openBisModel.getImageFiles().values().stream().findFirst().orElseThrow();
        Assert.assertEquals(1, fileInfos.size());
        Assert.assertEquals(
                "xlsx/miscellaneous/file-service/eln-lims/file-service/eln-lims/5c/37/0f/5c370fb6-e2be-4472-ae2a-640e13b2d763/87d28f0c-7f83-449c-a20e-fb445cf968f1.png",
                fileInfos.get(0).originalPath());

        byte[] writtenStuff = ExcelWriter.convert(ExcelWriter.Format.ZIP_EXPORT, openBisModel);
        try (FileOutputStream byteArrayOutputStream = new FileOutputStream(
                TMP_OPENBIS_TEST_RO_OUT_ZIP))
        {
            byteArrayOutputStream.write(writtenStuff);
        }

    }

    @Test
    public void openBisToCrateTestWithImageAndOtherFile() throws Exception
    {

        String input =
                "src/test/resources/files/image/export.2026-01-28-11-10-06-076-combined.zip";

        String output = "/tmp/openbis_test_ro_out.zip";
        Path path = Paths.get(input);
        OpenBisModel excelModel = ExcelReader.convert(ExcelReader.Format.ZIP_EXPORT, path);
        Assert.assertEquals(1, excelModel.getImageFiles().size());
        Assert.assertEquals(1, excelModel.getFiles().size());

        List<IFileInfo> fileInfos =
                excelModel.getImageFiles().values().stream().findFirst().orElseThrow();
        Assert.assertEquals(1, fileInfos.size());
        Assert.assertEquals(
                "xlsx/miscellaneous/file-service/eln-lims/5c/37/0f/5c370fb6-e2be-4472-ae2a-640e13b2d763/87d28f0c-7f83-449c-a20e-fb445cf968f1.png",
                fileInfos.get(0).originalPath());

        Writer writer = new Writer();
        writer.write(excelModel, Path.of(output));
    }



}
