package ch.openbis.rocrate.app.examples.files;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.excel.v3.model.IFileInfo;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;
import ch.openbis.rocrate.app.reader.RdfToModel;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileOutputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CombinedExternalCrateTest
{
    static final String INPUT =
            "src/test/resources/files/image/h4x";

    public static final String TMP_OPENBIS_TEST_RO_OUT_ZIP = "/tmp/openbis_test_ro_out.zip";

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
                        "DEFAULT", schemaFacade);

        List<IFileInfo> fileInfos =
                openBisModel.getImageFiles().values().stream().filter(x -> !x.isEmpty()).findFirst()
                        .orElseThrow();
        Assert.assertEquals(1, fileInfos.size());

        List<IFileInfo> fileInfosData =
                openBisModel.getFiles().values().stream().filter(x -> !x.isEmpty()).findFirst()
                        .orElse(null);
        Assert.assertNotNull(fileInfosData);
        Assert.assertTrue(fileInfosData.stream().findFirst()
                .filter(x -> x.originalPath().equals("data/stuff.csv")).isPresent());

        String actual = fileInfos.get(0).originalPath();
        String prefix = "xlsx/miscellaneous/file-service/eln-lims/image/test";
        Assert.assertTrue(
                actual.startsWith(prefix));
        String suffix = ".png";
        Assert.assertTrue(actual.endsWith(suffix));
        try
        {

            String replace = actual.replace(prefix + "-", "").replace(suffix, "");
            UUID.fromString(replace);
        } catch (RuntimeException e)
        {
            Assert.fail();
        }
        ObjectIdentifier objectIdentifier = openBisModel.getImageFiles().entrySet().stream()
                .filter(x -> !x.getValue().isEmpty())
                .map(x -> x.getKey())
                .findFirst().orElseThrow();
        Sample sample = (Sample) openBisModel.getEntities().get(objectIdentifier);

        String abstractString = sample.getProperties().get("abstract").toString();
        Assert.assertTrue(abstractString.contains(actual));

        byte[] writtenStuff = ExcelWriter.convert(ExcelWriter.Format.ZIP_EXPORT, openBisModel);
        try (FileOutputStream byteArrayOutputStream = new FileOutputStream(
                TMP_OPENBIS_TEST_RO_OUT_ZIP))
        {
            byteArrayOutputStream.write(writtenStuff);
        }

    }

}
