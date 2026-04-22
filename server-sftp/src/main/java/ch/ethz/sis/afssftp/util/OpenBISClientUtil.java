package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.afssftp.authentication.OpenBISUser;
import ch.ethz.sis.openbis.generic.OpenBIS;
import lombok.NonNull;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

public class OpenBISClientUtil {
    public static final AtomicReference<String> applicationServerUrl = new AtomicReference<>(null);
    public static final AtomicReference<String> afsUrl = new AtomicReference<>(null);

    public OpenBIS getOpenBISClient() {
        return new OpenBIS(getApplicationServerUrl());
    }

    public OpenBIS getOpenBISClient(@NonNull OpenBISUser openBISUser) {
        OpenBIS openBIS = getOpenBISClient();
        openBISUser.checkAndRenewSessionIfNecessary(openBIS);
        return openBIS;
    }

    public AfsClient getAfsClient(@NonNull OpenBISUser openBISUser) {
        AfsClient afsClient = new AfsClient(URI.create(getAfsUrl()));
        openBISUser.checkAndRenewSessionIfNecessary(afsClient);
        return afsClient;
    }

    String getApplicationServerUrl() {
        return applicationServerUrl.get();
    }

    String getAfsUrl() {
        return afsUrl.get();
    }
}
