package ch.openbis.rocrate.app.examples.files.remote;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.excel.v3.model.IFileInfo;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.reader.externalfile.FileDownloader;
import ch.openbis.rocrate.app.reader.externalfile.saving.TempDirSaving;
import com.sun.net.httpserver.HttpExchange;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.AbstractEntity;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.reader.ZipReader;
import org.junit.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemoteFileTest
{

    private static FakeHttpServer fakeHttpServer;

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

    @After
    public void cleanUp()
    {
        fakeHttpServer.setSpecialHandler(null);
    }


    @Test
    public void testGettingFiles() throws Exception
    {
        RoCrateReader roCrateReader = new RoCrateReader(new ZipReader());
        RoCrate roCrate =
                roCrateReader.readCrate("src/test/resources/files/remote/one_http_link.zip");

        FileDownloader fileDownloader =
                new FileDownloader(FileDownloader.getLocalMapping(8100), new TempDirSaving());
        Map<AbstractEntity, Path> abstractEntityPathMap =
                fileDownloader.handleDownloads(roCrate);
        Assert.assertTrue(true);
        Assert.assertFalse(abstractEntityPathMap.isEmpty());

    }

    @Test
    public void testFailingFile() throws Exception
    {
        fakeHttpServer.setSpecialHandler(new FakeHttpServer.SpecialHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                exchange.sendResponseHeaders(404, 0);

            }
        });

        RoCrateReader roCrateReader = new RoCrateReader(new ZipReader());
        RoCrate roCrate =
                roCrateReader.readCrate("src/test/resources/files/remote/one_http_link.zip");

        FileDownloader fileDownloader =
                new FileDownloader(FileDownloader.getLocalMapping(8100), new TempDirSaving());
        Assert.assertThrows(RuntimeException.class,
                () -> fileDownloader.handleDownloads(roCrate));

    }


    @Test
    public void testHandlingDownloadedFiles() throws Exception
    {
        byte[] bytes = "abcdefghij".getBytes();

        fakeHttpServer.setSpecialHandler(new FakeHttpServer.SpecialHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);

            }
        });

        RoCrateReader roCrateReader = new RoCrateReader(new ZipReader());
        RoCrate roCrate =
                roCrateReader.readCrate("src/test/resources/files/remote/one_http_link.zip");

        FileDownloader fileDownloader =
                new FileDownloader(FileDownloader.getLocalMapping(8100), new TempDirSaving());

        Map<AbstractEntity, Path> abstractEntityPathMap =
                fileDownloader.handleDownloads(roCrate);
        Path path = abstractEntityPathMap.values().stream().findFirst().orElseThrow();
        Assert.assertArrayEquals(bytes, Files.readAllBytes(path));
        ;

    }

    @Test
    public void testMultipleLinks() throws Exception
    {
        byte[] bytes = "abcdefghij".getBytes();

        fakeHttpServer.setSpecialHandler(new FakeHttpServer.SpecialHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);

            }
        });

        RoCrateReader roCrateReader = new RoCrateReader(new ZipReader());
        RoCrate roCrate =
                roCrateReader.readCrate("src/test/resources/files/remote/one_http_link.zip");

        FileDownloader fileDownloader =
                new FileDownloader(FileDownloader.getLocalMapping(8100), new TempDirSaving());

        Map<AbstractEntity, Path> abstractEntityPathMap =
                fileDownloader.handleDownloads(roCrate);
        Path path = abstractEntityPathMap.values().stream().findFirst().orElseThrow();
        Assert.assertArrayEquals(bytes, Files.readAllBytes(path));
        ;

    }

    @Test
    public void testMultipleLinksOnMultipleEntities() throws Exception
    {
        byte[] bytes = "abcdefghij".getBytes();

        fakeHttpServer.setSpecialHandler(new FakeHttpServer.SpecialHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);

            }
        });

        RoCrateReader roCrateReader = new RoCrateReader(new ZipReader());
        RoCrate roCrate =
                roCrateReader.readCrate("src/test/resources/files/remote/one_http_link.zip");

        FileDownloader fileDownloader =
                new FileDownloader(FileDownloader.getLocalMapping(8100), new TempDirSaving());

        Map<AbstractEntity, Path> abstractEntityPathMap =
                fileDownloader.handleDownloads(roCrate);
        Path path = abstractEntityPathMap.values().stream().findFirst().orElseThrow();
        Assert.assertArrayEquals(bytes, Files.readAllBytes(path));
        ;

    }

    @Test
    public void testPsiExample() throws Exception
    {
        byte[] bytes = "abcdefghij".getBytes();

        fakeHttpServer.setSpecialHandler(new FakeHttpServer.SpecialHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);

            }
        });

        RoCrateReader roCrateReader = new RoCrateReader(new FolderReader());
        RoCrate roCrate =
                roCrateReader.readCrate("src/test/resources/files/remote/PsiWithLink");

        FileDownloader fileDownloader =
                new FileDownloader(FileDownloader.getLocalMapping(8100), new TempDirSaving());

        Map<AbstractEntity, Path> abstractEntityPathMap =
                fileDownloader.handleDownloads(roCrate);
        Path path = abstractEntityPathMap.values().stream().findFirst().orElseThrow();
        Assert.assertArrayEquals(bytes, Files.readAllBytes(path));
        ;

    }

    @Test
    public void testPsiExampleWithSchemaConversion() throws Exception
    {
        byte[] bytes = "abcdefghij".getBytes();

        fakeHttpServer.setSpecialHandler(new FakeHttpServer.SpecialHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);

            }
        });

        RoCrateReader roCrateReader = new RoCrateReader(new FolderReader());
        RoCrate roCrate =
                roCrateReader.readCrate("src/test/resources/files/remote/PsiWithLink");

        FileDownloader fileDownloader =
                new FileDownloader(FileDownloader.getLocalMapping(8100), new TempDirSaving());
        SchemaFacade schemaFacade = SchemaFacade.of(roCrate);

        Map<AbstractEntity, Path> abstractEntityPathMap =
                fileDownloader.handleDownloads(roCrate);
        Path path = abstractEntityPathMap.values().stream().findFirst().orElseThrow();
        Assert.assertArrayEquals(bytes, Files.readAllBytes(path));
        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        List<IType> types = schemaFacade.getTypes();

        for (var type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, abstractEntityPathMap);
        List<IFileInfo> iFileInfos = openBisModel.getFiles().get(new SampleIdentifier(
                "/DEFAULT/DEFAULT/SCHEMA_CREATIVEWORK_SCICAT_PUBLISHEDDATA_7EB141D3-11F1-47A6-9D0E-76F8832ED1B2"));
        Assert.assertFalse(iFileInfos.isEmpty());

        System.out.println("lol");

    }

}
