package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afssftp.authentication.AuthenticationProvider;
import ch.ethz.sis.afssftp.authentication.User;
import ch.ethz.sis.openbis.generic.OpenBIS;
import lombok.NonNull;

import java.util.concurrent.atomic.AtomicReference;

public class OpenBISClientUtil implements AuthenticationProvider
{
    public static final AtomicReference<String> applicationServerUrl = new AtomicReference<>(null);
    public static final AtomicReference<String> afsUrl = new AtomicReference<>(null);

    public OpenBIS getOpenBISClient() {
        return new OpenBIS(
                getApplicationServerUrl() + "/openbis/openbis",
                getApplicationServerUrl() + "/datastore_server",
                getAfsUrl()
        );
    }

    public OpenBIS getOpenBISClient(@NonNull User user) {
        OpenBIS openBIS = getOpenBISClient();
        user.checkAndRenewSessionIfNecessary(openBIS);
        return openBIS;
    }

    public OpenBIS.AfsServerFacade getAfsClient(@NonNull User user) {
        return getOpenBISClient(user).getAfsServerFacade();
    }

    String getApplicationServerUrl() {
        return applicationServerUrl.get();
    }

    String getAfsUrl() {
        return afsUrl.get();
    }

    @Override
    public String login(String userId, String password)
    {
        return getOpenBISClient().login(userId, password);
    }
}
