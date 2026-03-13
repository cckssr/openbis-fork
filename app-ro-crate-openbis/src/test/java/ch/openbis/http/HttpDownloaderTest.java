package ch.openbis.http;

import static org.junit.Assert.*;
import org.junit.Test;

import java.nio.file.Files;import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public class HttpDownloaderTest {
    @Test
    public void apiErrorTest() throws Exception {
        AtomicReference<Exception> expected = new AtomicReference<>();

        HttpDownloader httpDownloader = new HttpDownloader();
        httpDownloader.add("non sense url to test the error handler", Path.of("non sense path to test the error handler"));

        httpDownloader.error((url, path, exception) -> {
            expected.set(exception);
        }).override((url, path) -> {
            return true;
        }).start();

        assertNotNull(expected.get());
    }

    @Test
    public void apiDownloadTest() throws Exception {
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        Path tempFile = tempDir.resolve("google.com.tmp");
        if (Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
        AtomicReference<Exception> expected = new AtomicReference<>();

        HttpDownloader httpDownloader = new HttpDownloader();
        httpDownloader.add("http://google.com", tempFile);

        httpDownloader.error((url, path, exception) -> {
            expected.set(exception);
        }).override((url, path) -> {
            return true;
        }).start();

        assertNull(expected.get());
        assertTrue(Files.exists(tempFile));
        assertTrue(Files.size(tempFile) > 0);
    }
}
