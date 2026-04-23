package ch.ethz.sis.afssftp.authentication;

import ch.ethz.sis.afssftp.util.AfsClientProxy;
import ch.ethz.sis.openbis.generic.OpenBIS;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;

@Data
@Builder
public class OpenBISUser {
    @NonNull final String username;
    @NonNull final String password;
    volatile String sessionToken;

    public synchronized void checkAndRenewSessionIfNecessary(@NonNull OpenBIS openBIS) {
        openBIS.setSessionToken(sessionToken);
        if ( !openBIS.isSessionActive() ) {
            openBIS.login(username, password);
        }
    }

    @SneakyThrows
    public synchronized void checkAndRenewSessionIfNecessary(@NonNull AfsClientProxy afsClient) {
        afsClient.setSessionToken(sessionToken);
        if ( !afsClient.isSessionValid() ) {
            afsClient.login(username, password);
        }
    }
}
