package ch.ethz.sis.openbis.ros.startup;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class StartupMain implements QuarkusApplication
{
    private static Configuration configuration;

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static void setConfiguration(Configuration configuration)
    {
        StartupMain.configuration = configuration;
    }

    public static void main(String[] args) throws IOException {
        Quarkus.run(StartupMain.class, args);
    }

    @Override
    public int run(String... args) throws Exception
    {

        System.out.println("Current Working Directory: " + (new File("")).getCanonicalPath());
        System.out.println("Configuration Location: " + (new File(args[0])).getCanonicalPath());
        configuration = new Configuration(List.of(RoCrateServerParameter.class), args[0]);
        if (configuration.getStringProperty(RoCrateServerParameter.sessionWorkSpace) == null)
        {
            throw new Exception("Setting a session workspace is mandatory! ");
        }

        System.out.println(">> Quarkus app running. Press Ctrl+C to exit.");
        Quarkus.waitForExit();
        return 0;
    }
}
