package ch.ethz.sis.openbis.systemtests.suite.rocrate.environment;

import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestEnvironment;

import java.io.IOException;

public final class RoCrateServerIntegrationTestEnvironment
{

    public static IntegrationTestEnvironment environment;

    public static void start() throws IOException
    {
        if (environment == null)
        {
            System.setProperty("RO_CRATE_SERVER_LOCAL_DOWNLOAD_PORT", "8100");
            environment = new IntegrationTestEnvironment();
            environment.createApplicationServer();
            environment.createRoCrateServer();
            environment.enableSystemUser();
            environment.createFakeHttpServer();
            environment.start();

        }
    }

    public static void stop()
    {
        if (environment != null)
        {
            environment.stop();
            environment = null;
        }
    }

}
