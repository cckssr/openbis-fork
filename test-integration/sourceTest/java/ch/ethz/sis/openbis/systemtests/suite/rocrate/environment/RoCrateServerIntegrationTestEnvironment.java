package ch.ethz.sis.openbis.systemtests.suite.rocrate.environment;

import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestEnvironment;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public final class RoCrateServerIntegrationTestEnvironment
{

    public static IntegrationTestEnvironment environment;


    public static void start() throws IOException
    {
        if (environment == null)
        {
            try
            {
                int randomNum = ThreadLocalRandom.current().nextInt(49152,
                        65535); // ephemeral port https://unix.stackexchange.com/questions/65475/ephemeral-port-what-is-it-and-what-does-it-do

                System.setProperty("RO_CRATE_SERVER_LOCAL_DOWNLOAD_PORT", Integer.toString(randomNum));
                environment = new IntegrationTestEnvironment();
                environment.createApplicationServer();
                environment.createRoCrateServer(
                        new IntegrationTestEnvironment.RoCrateServerArgs(randomNum));
                environment.enableSystemUser();
                environment.createFakeHttpServer(randomNum);
                environment.start();
            } catch (IOException e)
            {
                stop();
                throw e;
            } catch (RuntimeException | Error e)
            {
                stop();
                throw e;
            }

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
