package ch.ethz.sis.rocrateserver.startup;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import java.io.File;
import java.io.IOException;
import java.util.List;

@QuarkusMain
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
        System.setProperty("quarkus.http.port", "8086");
        System.setProperty("quarkus.transaction-manager.default-transaction-timeout", "120s");
        System.setProperty("quarkus.rest-client.connect-timeout", "120s");
        System.setProperty("quarkus.rest-client.read-timeout", "120s");
        System.setProperty("quarkus.http.request-timeout", "120s");
        System.setProperty("quarkus.http.test-timeout", "120s");
        System.setProperty("quarkus.datasource.jdbc.idle-timeout", "120s");



        System.out.println("Current Working Directory: " + (new File("")).getCanonicalPath());
        System.out.println("Configuration Location: " + (new File(args[0])).getCanonicalPath());
        configuration = new Configuration(List.of(RoCrateServerParameter.class), args[0]);
        validate(configuration);
        System.out.println(">> Quarkus app running. Press Ctrl+C to exit.");
        Quarkus.waitForExit();
        return 0;
    }

    private void validate(Configuration configuration) throws IllegalArgumentException {
        if (configuration.getStringProperty(RoCrateServerParameter.sessionWorkSpace) == null)
        {
            throw new IllegalArgumentException("Setting a session workspace is mandatory! ");
        }
    }
}
