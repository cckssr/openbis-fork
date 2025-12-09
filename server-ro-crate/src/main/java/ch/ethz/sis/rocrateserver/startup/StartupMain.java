package ch.ethz.sis.rocrateserver.startup;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@QuarkusMain
public class StartupMain implements QuarkusApplication
{
    private static Configuration configuration;

    public static Configuration getConfiguration()
    {
        return configuration;
    }

    public static void setConfiguration(Configuration configuration)
    {
        StartupMain.configuration = configuration;
    }

    public static void main(String[] args) throws IOException
    {
        System.out.println("Current Working Directory: " + (new File("")).getCanonicalPath());

        if (args != null && args.length > 0)
        {
            System.out.println("Loading configuration from: " + (new File(args[0])).getCanonicalPath());
            configuration = new Configuration(List.of(RoCrateServerParameter.class), args[0]);
            validate(configuration);
            cleanBeforeRun(configuration);

        }

        System.setProperty("quarkus.http.port", configuration.getStringProperty(RoCrateServerParameter.httpServerPort));
        System.setProperty("quarkus.http.idle-timeout", configuration.getStringProperty(RoCrateServerParameter.httpServerTimeout));
        System.setProperty("quarkus.http.read-timeout", configuration.getStringProperty(RoCrateServerParameter.httpServerTimeout));


        if (LaunchMode.current().isDevOrTest())
        {
            System.setProperty("quarkus.http.test-port", configuration.getStringProperty(RoCrateServerParameter.httpServerPort));
            System.setProperty("quarkus.http.test-timeout", configuration.getStringProperty(RoCrateServerParameter.httpServerTimeout));
        }

        Quarkus.run(StartupMain.class, args);
    }

    private static void validate(Configuration configuration) throws IllegalArgumentException
    {
        if (configuration.getStringProperty(RoCrateServerParameter.sessionWorkSpace) == null)
        {
            throw new IllegalArgumentException("Setting a session workspace is mandatory! ");
        }
    }

    @Override
    public int run(String... args) throws Exception
    {
        System.out.println(">> Quarkus app running. Press Ctrl+C to exit.");
        Quarkus.waitForExit();
        return 0;
    }

    private static void cleanBeforeRun(Configuration configuration) throws IOException
    {
        if (LaunchMode.current().isDevOrTest())
        {
            // Locally, we might want to keep outputs from tests and such for the moment.
            return;
        }

        Path sessionWorkSpacePath =
                Path.of(configuration.getStringProperty(RoCrateServerParameter.sessionWorkSpace));
        Stream<Path> list = Files.list(sessionWorkSpacePath);
        list.forEach(x -> {
            try
            {

                FileUtils.deleteDirectory(new File(x.toString()));
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }

        });

    }

}
