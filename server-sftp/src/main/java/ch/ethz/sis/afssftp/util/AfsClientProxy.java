package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClient;
import lombok.NonNull;

/***
 * Proxy-class to ease unit-testing: AfsClient cannot be mocked because it is final
 */
public class AfsClientProxy {
    private final AfsClient afsClient;

    public AfsClientProxy(@NonNull AfsClient afsClient) {
        this.afsClient = afsClient;
    }

    public AfsClient getInnerClient() {
        return afsClient;
    }

    public void setSessionToken(final String sessionToken) {
        afsClient.setSessionToken(sessionToken);
    }

    public @NonNull String login(@NonNull final String userId, @NonNull final String password)
            throws Exception {
        return afsClient.login(userId, password);
    }

    public @NonNull Boolean isSessionValid() throws Exception {
        return afsClient.isSessionValid();
    }

    public @NonNull Boolean logout() throws Exception {
        return afsClient.logout();
    }

    public @NonNull File[] list(@NonNull final String owner, @NonNull final String source,
                                @NonNull final Boolean recursively) throws Exception {
        return afsClient.list(owner, source, recursively);
    }

    public byte[] read(@NonNull String owner, @NonNull String source, @NonNull Long offset, @NonNull Integer limit)
            throws Exception {
        return afsClient.read(owner, source, offset, limit);
    }

    public @NonNull Boolean write(@NonNull final String owner, @NonNull final String source,
                                  @NonNull final Long offset, @NonNull final byte[] data) throws Exception {
        return afsClient.write(owner, source, offset, data);
    }

    public @NonNull Boolean delete(@NonNull final String owner, @NonNull final String source, @NonNull final Boolean trash)
            throws Exception {
        return afsClient.delete(owner, source, trash);
    }

    public @NonNull Boolean copy(@NonNull final String sourceOwner, @NonNull final String source,
                                 @NonNull final String targetOwner,
                                 @NonNull final String target)
            throws Exception {
        return afsClient.copy(sourceOwner, source, targetOwner, target);
    }

    public @NonNull Boolean move(@NonNull final String sourceOwner, @NonNull final String source,
                                 @NonNull final String targetOwner,
                                 @NonNull final String target)
            throws Exception {
        return afsClient.move(sourceOwner, source, targetOwner, target);
    }

    public @NonNull Boolean create(@NonNull final String owner, @NonNull final String source, @NonNull final Boolean directory)
            throws Exception {
        return afsClient.create(owner, source, directory);
    }

    public @NonNull Boolean truncate(@NonNull final String owner, @NonNull final String source, @NonNull final Long size)
            throws Exception {
        return afsClient.truncate(owner, source, size);
    }

}
