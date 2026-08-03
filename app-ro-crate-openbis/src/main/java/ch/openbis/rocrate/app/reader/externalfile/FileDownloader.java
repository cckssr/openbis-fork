package ch.openbis.rocrate.app.reader.externalfile;

import ch.openbis.http.HttpDownloader;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.AbstractEntity;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static ch.openbis.rocrate.app.Constants.FILE_TYPES;

public class FileDownloader implements IFileDownloader
{
    Function<URL, URL> mapUrl;

    IFileDownloadStrategy iFileDownloadStrategy;


    public static Function<URL, URL> getLocalMapping(int port)
    {
        return getLocalMapping("http", "localhost", port);
    }

    public static Function<URL, URL> getLocalMapping(String scheme, String host, int port)
    {
        return url -> {
            try
            {
                return new URL(scheme, host, port, url.getFile());
            } catch (MalformedURLException e)
            {
                throw new RuntimeException(e);
            }
        };
    }

    public static Function<URL, URL> getRealMapping()
    {
        return url -> {
            if (url.getProtocol().contains("s3"))
            {
                try
                {
                    return new URL("https", url.getHost(), url.getPort(), url.getFile());
                } catch (MalformedURLException e)
                {
                    throw new RuntimeException(e);
                }
            }
            return url;

        };
    }

    public FileDownloader(Function<URL, URL> mapUrl, IFileDownloadStrategy iFileDownloadStrategy)
    {
        this.mapUrl = mapUrl;
        this.iFileDownloadStrategy = iFileDownloadStrategy;
    }

    @Override
    public Map<AbstractEntity, Path> handleDownloads(RoCrate roCrate) throws Exception
    {
        HttpDownloader httpDownloader = new HttpDownloader();
        httpDownloader.error((url, path, exception) -> {
            throw new RuntimeException(exception);
        });
        httpDownloader.override((url, path) -> {
            return true;
        });
        Map<AbstractEntity, Path> downloadPaths = new LinkedHashMap<>();

        UUID downloadUUid = UUID.randomUUID();
        List<AbstractEntity> abstractEntityList = new ArrayList<>();
        abstractEntityList.addAll(roCrate.getAllContextualEntities());
        abstractEntityList.addAll(roCrate.getAllDataEntities());
        for (AbstractEntity dataEntity : abstractEntityList)
        {
            String typerooni = dataEntity.getProperty("@type").asText();
            if (!FILE_TYPES.contains(typerooni))
            {
                continue;
            }

            Optional<SupportedProtocol> supportedProtocol =
                    SupportedProtocol.findHandledProtocol(dataEntity.getId());
            if (supportedProtocol.isEmpty())
            {
                continue;
            }
            URL parsedUrl = new URL(dataEntity.getId());
            URL workingUrl = this.mapUrl.apply(parsedUrl);

            Path tempFile = iFileDownloadStrategy.getPath("download" + downloadUUid);
            httpDownloader.add(workingUrl.toString(), tempFile);
            downloadPaths.put(dataEntity, tempFile);

        }

        try
        {
            httpDownloader.start();
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return downloadPaths;

    }
}
