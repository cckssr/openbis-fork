package ch.openbis.rocrate.app.reader.externalfile;

import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.AbstractEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public interface IFileDownloader
{

    public enum SupportedProtocol
    {

        S3("s3"),
        HTTP("http"),
        HTTPS("https");

        private final String scheme;

        SupportedProtocol(String scheme)
        {
            this.scheme = scheme;
        }

        public String getScheme()
        {
            return scheme;
        }

        public static Optional<SupportedProtocol> findHandledProtocol(String id)
        {

            URI uri;

            try
            {
                uri = new URI(id);
            } catch (URISyntaxException e)
            {
                return Optional.empty();
            }
            for (SupportedProtocol supportedProtocol : SupportedProtocol.values())
            {
                if (supportedProtocol.getScheme().equals(uri.getScheme()))
                {
                    return Optional.of(supportedProtocol);
                }
            }
            return Optional.empty();
        }

    }

    Map<AbstractEntity, Path> handleDownloads(RoCrate roCrate)
            throws Exception;

}
