package ch.openbis.rocrate.app.examples.files;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
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

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FilesTest
{

    static final String INPUT =
            "src/test/resources/files/openbis_export_with_file.zip";

    static final String INPUT_CRATE =
            "src/test/resources/files/out_crate.zip";

    static final String ENTITY_FILE_CRATE =
            "src/test/resources/files/entity_file_crate.zip";

    public static final String TMP_OPENBIS_TEST_RO_OUT_ZIP = "/tmp/openbis_test_ro_out.zip";

    static final String OUTPUT = "/tmp/out1.zip";

    @Test
    public void openBisToCrateTest() throws Exception
    {
        Path path = Paths.get(INPUT);
        OpenBisModel excelModel = ExcelReader.convert(ExcelReader.Format.ZIP_EXPORT, path,
                ExcelReader.FileMode.DUMMY);
        Assert.assertEquals(1, excelModel.getFiles().size());

        List<OpenBisModel.FileInfo> fileInfos =
                excelModel.getFiles().values().stream().findFirst().orElseThrow();
        Assert.assertEquals(1, fileInfos.size());

        Writer writer = new Writer();
        writer.write(excelModel, Path.of(OUTPUT));
    }

    @Test
    public void openBisToCrateTestNoSampleName() throws Exception
    {
        String input =
                "src/test/resources/files/export.2026-02-12-10-57-51-092-entity-without-name.zip";
        String output = "/tmp/" + UUID.randomUUID() + ".zip";

        Path path = Paths.get(input);
        OpenBisModel excelModel = ExcelReader.convert(ExcelReader.Format.ZIP_EXPORT, path);
        Assert.assertEquals(1, excelModel.getFiles().size());

        List<OpenBisModel.FileInfo> fileInfos =
                excelModel.getFiles().values().stream().findFirst().orElseThrow();
        Assert.assertEquals(3, fileInfos.size());

        Writer writer = new Writer();
        writer.write(excelModel, Path.of(output));
        RoCrateReader roCrateFolderReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateFolderReader.readCrate(output);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);
        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (var type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel openBisModel = RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                entryList.stream().toList(), "DEFAULT", "DEFAULT", schemaFacade);
        Assert.assertTrue(true);

    }

    @Test
    public void crateToOpenBisTest() throws Exception
    {

        RoCrateReader roCrateFolderReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateFolderReader.readCrate(INPUT_CRATE);
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
                        "DEFAULT", schemaFacade);

        Assert.assertEquals(1, openBisModel.getFiles().size());

        List<OpenBisModel.FileInfo> fileInfos =
                openBisModel.getFiles().values().stream().findFirst().orElseThrow();
        Assert.assertEquals(1, fileInfos.size());


        byte[] writtenStuff = ExcelWriter.convert(ExcelWriter.Format.ZIP_EXPORT, openBisModel);
        try (FileOutputStream byteArrayOutputStream = new FileOutputStream(
                TMP_OPENBIS_TEST_RO_OUT_ZIP))
        {
            byteArrayOutputStream.write(writtenStuff);
        }


    }

    @Test
    public void crateWithFileAttachedToMetadataTest() throws Exception
    {

        RoCrateReader roCrateFolderReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateFolderReader.readCrate(ENTITY_FILE_CRATE);
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
                        "DEFAULT", schemaFacade);

        Assert.assertEquals(1, openBisModel.getFiles().size());

        List<OpenBisModel.FileInfo> fileInfos =
                openBisModel.getFiles().values().stream().findFirst().orElseThrow();
        Assert.assertEquals(1, fileInfos.size());

        byte[] writtenStuff = ExcelWriter.convert(ExcelWriter.Format.ZIP_EXPORT, openBisModel);
        try (FileOutputStream byteArrayOutputStream = new FileOutputStream(
                TMP_OPENBIS_TEST_RO_OUT_ZIP))
        {
            byteArrayOutputStream.write(writtenStuff);
        }


    }

}
