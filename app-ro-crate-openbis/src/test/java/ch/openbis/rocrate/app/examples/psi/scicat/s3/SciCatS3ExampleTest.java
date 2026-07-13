package ch.openbis.rocrate.app.examples.psi.scicat.s3;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.examples.files.remote.FakeHttpServer;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.reader.externalfile.FileDownloader;
import ch.openbis.rocrate.app.reader.externalfile.IFileDownloader;
import ch.openbis.rocrate.app.reader.externalfile.saving.TempDirSaving;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.AbstractEntity;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SciCatS3ExampleTest
{
    static final String INPUT = "src/test/resources/scicat/example-export-with-s3-links";

    static FakeHttpServer fakeHttpServer;

    @BeforeClass
    public static void setUp() throws IOException
    {
        fakeHttpServer = FakeHttpServer.build("localhost", 8100);
    }

    @AfterClass
    public static void tearDown()
    {
        fakeHttpServer.stop();
    }

    @Test
    public void testReading() throws Exception
    {
        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateFolderReader.readCrate(INPUT);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);
        List<IType> types = schemaFacade.getTypes();
        Set<IMetadataEntry> entryList = new LinkedHashSet<>();

        entryList.addAll(schemaFacade.getAllEntries());
        IFileDownloader fileDownloader =
                new FileDownloader(FileDownloader.getLocalMapping(8100), new TempDirSaving());
        Map<AbstractEntity, Path> abstractEntityPathMap = fileDownloader.handleDownloads(crate);

        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, abstractEntityPathMap).openBisModel();

        Assert.assertEquals(openBisModel.getFiles().get(new SampleIdentifier(
                        "/DEFAULT/DEFAULT/SCHEMA_MEDIAOBJECT_4022AFAA-E4D7-49A0-80F9-D6F98E580A53_0_2022-07-22-12-40-56_TAR"))
                .size(), 1);
        Assert.assertEquals(openBisModel.getFiles()
                .get(new SampleIdentifier("/DEFAULT/DEFAULT/1FB48C26-60F7-487F-A0DE-BBA17FC7D755"))
                .size(), 2);
        Assert.assertEquals(openBisModel.getFiles()
                .get(new SampleIdentifier("/DEFAULT/DEFAULT/ED381760-8933-4109-BAB9-EA7C2F50BB87"))
                .size(), 0);

    }

}
