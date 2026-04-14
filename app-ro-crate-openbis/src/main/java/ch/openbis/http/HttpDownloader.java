package ch.openbis.http;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class HttpDownloader {
    private static final int MAX_DOWNLOAD_THREADS = 4;
    public interface HttpDownloaderError {
        public void error(String url, Path path, Exception exception);
    }
    HttpDownloaderError DefaultHttpDownloaderError = new HttpDownloaderError() {
        @Override
        public void error(String url, Path path, Exception exception) {

        }
    };

    public interface HttpDownloaderOverride {
        public boolean override(String url, Path path);
    }
    HttpDownloaderOverride DefaultHttpDownloaderOverride = new HttpDownloaderOverride() {
        @Override
        public boolean override(String url, Path path) {
            return true;
        }
    };

    public interface HttpDownloaderRedirectPolicy
    {
        HttpClient.Redirect getRedirect(String target);
    }

    HttpDownloaderRedirectPolicy
            defaultHttpDownloaderRedirectPolicy = new HttpDownloaderRedirectPolicy()
    {
        @Override
        public HttpClient.Redirect getRedirect(String target)
        {
            return HttpClient.Redirect.ALWAYS;
        }
    };

    private record Download(String url, Path path) {

    }

    private HttpDownloaderError httpDownloaderError;
    private HttpDownloaderOverride httpDownloaderOverride;
    private boolean started;
    private final Queue<Download> downloads;

    private HttpDownloaderRedirectPolicy httpDownloaderRedirectPolicy;

    private final ConcurrentHashMap<Downloader, Exception> exceptions = new ConcurrentHashMap<>();

    public HttpDownloader() {
        this.httpDownloaderError = DefaultHttpDownloaderError;
        this.httpDownloaderOverride = DefaultHttpDownloaderOverride;
        this.httpDownloaderRedirectPolicy = defaultHttpDownloaderRedirectPolicy;
        this.started = false;
        this.downloads = new LinkedList<>();
    }

    public HttpDownloader error(HttpDownloaderError httpDownloaderError) {
        this.httpDownloaderError = httpDownloaderError;
        return this;
    }

    public HttpDownloader override(HttpDownloaderOverride httpDownloaderOverride) {
        this.httpDownloaderOverride = httpDownloaderOverride;
        return this;
    }

    public HttpDownloader redirect(HttpDownloaderRedirectPolicy httpDownloaderRedirectPolicy)
    {
        this.httpDownloaderRedirectPolicy = httpDownloaderRedirectPolicy;
        return this;
    }


    public void add(String url, Path path) {
        if (!started) {
            downloads.add(new Download(url, path));
        } else {
            throw new RuntimeException("Downloads started, new items cannot be added");
        }
    }

    public void start() throws InterruptedException {
        started = true;
        Semaphore maxThreadLimitSemaphore = new Semaphore(MAX_DOWNLOAD_THREADS);
        while (!downloads.isEmpty()) {
            maxThreadLimitSemaphore.acquire();
            Download download = downloads.remove();
            Downloader downloader =
                    new Downloader(download, maxThreadLimitSemaphore, httpDownloaderError,
                            httpDownloaderOverride, this.httpDownloaderRedirectPolicy,
                            this.exceptions);
            Thread downloaderThread = new Thread(downloader);
            downloaderThread.start();
        }
        maxThreadLimitSemaphore.acquire(MAX_DOWNLOAD_THREADS);
        for (var keyVal : exceptions.entrySet())
        {
            httpDownloaderError.error(keyVal.getKey().download.url, keyVal.getKey().download.path,
                    keyVal.getValue());
        }


    }

    private static class Downloader implements Runnable {

        private final Download download;
        private final Semaphore maxThreadLimitSemaphore;
        private final HttpDownloaderError httpDownloaderError;
        private final HttpDownloaderOverride httpDownloaderOverride;

        private final HttpDownloaderRedirectPolicy redirectPolicy;

        private final ConcurrentHashMap exceptions;

        public Downloader(Download download, Semaphore maxThreadLimitSemaphore,
                HttpDownloaderError httpDownloaderError,
                HttpDownloaderOverride httpDownloaderOverride,
                HttpDownloaderRedirectPolicy redirectPolicy,
                ConcurrentHashMap exceptions)
        {
            this.download = download;
            this.maxThreadLimitSemaphore = maxThreadLimitSemaphore;
            this.httpDownloaderError = httpDownloaderError;
            this.httpDownloaderOverride = httpDownloaderOverride;
            this.exceptions = exceptions;
            this.redirectPolicy = redirectPolicy;
        }

        @Override
        public void run() {
            Path tempFile;
            try {
                // download

                HttpClient.Redirect redirect = redirectPolicy.getRedirect(download.url);
                HttpClient client =
                        HttpClient.newBuilder().followRedirects(redirect)
                                .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(download.url))
                        .GET()
                        .build();

                HttpResponse<InputStream> response = null;

                response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                    throw new RuntimeException("Http status: " + response.statusCode());
                }
                InputStream in = response.body();

                if (!Files.exists(download.path) || httpDownloaderOverride.override(download.url, download.path)) {
                    Path dir = download.path.getParent();
                    String name = download.path.getFileName().toString();
                    tempFile = Files.createTempFile(dir,name, ".tmp");
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(tempFile, download.path, StandardCopyOption.ATOMIC_MOVE);
                }
            } catch (Exception exception) {
                exceptions.put(this, exception);
            } finally {
                // release
                maxThreadLimitSemaphore.release();
            }
        }
    }
}
