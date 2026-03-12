package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.download;

import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.importjob.download.SessionWorkSpacveSaving;
import ch.ethz.sis.rocrateserver.startup.Configuration;
import ch.ethz.sis.rocrateserver.startup.RoCrateServerParameter;
import ch.ethz.sis.rocrateserver.startup.StartupMain;
import ch.openbis.rocrate.app.reader.externalfile.FileDownloader;
import com.sun.net.httpserver.HttpExchange;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.AbstractEntity;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.reader.ZipReader;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class RemoteFileTest
{

    public static final String SESSION_TOKEN = "system-123";

    private static FakeHttpServer fakeHttpServer;

    @BeforeClass
    public static void setUp() throws IOException
    {
        StartupMain.setConfiguration(new Configuration(List.of(RoCrateServerParameter.class),
                "src/test/resources/files/remote/service.properties"));
        fakeHttpServer = FakeHttpServer.build("localhost", 8100);
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
                new FileDownloader(FileDownloader.getLocalMapping(8100),
                        new SessionWorkSpacveSaving(SESSION_TOKEN));
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
                new FileDownloader(FileDownloader.getLocalMapping(8100),
                        new SessionWorkSpacveSaving(SESSION_TOKEN));
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
                exchange.sendResponseHeaders(200, 10);
                exchange.getResponseBody().write(bytes);

            }
        });

        RoCrateReader roCrateReader = new RoCrateReader(new ZipReader());
        RoCrate roCrate =
                roCrateReader.readCrate("src/test/resources/files/remote/one_http_link.zip");

        FileDownloader fileDownloader =
                new FileDownloader(FileDownloader.getLocalMapping(8100),
                        new SessionWorkSpacveSaving(SESSION_TOKEN));

        Map<AbstractEntity, Path> abstractEntityPathMap =
                fileDownloader.handleDownloads(roCrate);
        Path path = abstractEntityPathMap.values().stream().findFirst().orElseThrow();
        Assert.assertArrayEquals(bytes, Files.readAllBytes(path));
        ;

    }

    @Test
    public void testDownloadedFileGoesToSessionWorkspace() throws Exception
    {
        byte[] bytes = "abcdefghij".getBytes();

        fakeHttpServer.setSpecialHandler(new FakeHttpServer.SpecialHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                exchange.sendResponseHeaders(200, 10);
                exchange.getResponseBody().write(bytes);

            }
        });

        RoCrateReader roCrateReader = new RoCrateReader(new ZipReader());
        RoCrate roCrate =
                roCrateReader.readCrate("src/test/resources/files/remote/one_http_link.zip");

        FileDownloader fileDownloader =
                new FileDownloader(FileDownloader.getLocalMapping(8100),
                        new SessionWorkSpacveSaving(SESSION_TOKEN));

        Map<AbstractEntity, Path> abstractEntityPathMap =
                fileDownloader.handleDownloads(roCrate);
        Path path = abstractEntityPathMap.values().stream().findFirst().orElseThrow();
        Assert.assertArrayEquals(bytes, Files.readAllBytes(path));
        Assert.assertTrue(path.toString().contains(SESSION_TOKEN));

    }

}
