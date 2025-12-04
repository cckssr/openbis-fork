package ch.ethz.sis.openbis.systemtests.suite.rocrate.environment;

import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestEnvironment;

public final class RoCrateServerIntegrationTestEnvironment
{

    public static IntegrationTestEnvironment environment;

    public static void start()
    {
        if (environment == null)
        {
            environment = new IntegrationTestEnvironment();
            environment.createApplicationServer();
            environment.createRoCrateServer();
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
