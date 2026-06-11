package ch.ethz.sis.afssftp.conf;

import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.afssftp.startup.AfsSftpServerParameter;
import ch.ethz.sis.shared.startup.Configuration;
import lombok.NonNull;

public class Parameters {
    private static int maxFileChannelsPerSession = 20;
    private static int maxAfsClientChunkSize = AfsClient.DEFAULT_PACKAGE_SIZE_IN_BYTES;
    private static int afsCacheTimeoutMillis = 300;

    private static final boolean SKIP_AFS_CHANNEL_CACHE =
            "true".equalsIgnoreCase(System.getenv("AFS_SFTP_SKIP_AFS_CHANNEL_CACHE"));

    public static void initialize(
            @NonNull Configuration configuration
    ) {
        if (configuration.getStringProperty(AfsSftpServerParameter.maxFileChannelsPerSession) != null) {
            maxFileChannelsPerSession = configuration.getIntegerProperty(
                    AfsSftpServerParameter.maxFileChannelsPerSession
            );
        }
        if (configuration.getStringProperty(AfsSftpServerParameter.maxAfsClientChunkSize) != null) {
            maxAfsClientChunkSize = configuration.getIntegerProperty(
                    AfsSftpServerParameter.maxAfsClientChunkSize
            );
        }
        if (configuration.getStringProperty(AfsSftpServerParameter.afsCacheTimeoutMillis) != null) {
            afsCacheTimeoutMillis = configuration.getIntegerProperty(
                    AfsSftpServerParameter.afsCacheTimeoutMillis
            );
        }
    }

    public static int getMaxFileChannelsPerSession() {
        return maxFileChannelsPerSession;
    }

    public static int getMaxAfsClientChunkSize() {
        return maxAfsClientChunkSize;
    }

    public static int getAfsCacheTimeoutMillis() {
        return afsCacheTimeoutMillis;
    }

    public static boolean isSkipAfsChannelCaching() {
        return SKIP_AFS_CHANNEL_CACHE;
    }
}
