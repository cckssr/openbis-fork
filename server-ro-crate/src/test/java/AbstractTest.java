import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.rocrateserver.startup.Configuration;
import ch.ethz.sis.rocrateserver.startup.StartupMain;

import java.util.LinkedHashMap;
import java.util.Map;

import static ch.ethz.sis.rocrateserver.startup.RoCrateServerParameter.*;

public class AbstractTest
{

    public static Configuration getConfiguration()
    {
        System.setProperty("quarkus.http.port", "8085");
        System.setProperty("quarkus.transaction-manager.default-transaction-timeout", "120s");
        System.setProperty("quarkus.rest-client.connect-timeout", "120s");
        System.setProperty("quarkus.rest-client.read-timeout", "120s");
        System.setProperty("quarkus.http.request-timeout", "120s");
        System.setProperty("quarkus.http.test-timeout", "120s");
        System.setProperty("quarkus.datasource.jdbc.idle-timeout", "120s");


        if (StartupMain.getConfiguration() == null) {
            Map<Enum, String> vals = new LinkedHashMap<>();
            vals.put(httpServerPort, "8085");
            vals.put(sessionWorkSpace, "/tmp/server-ro-crate/");

            vals.put(httpMaxContentLength, "1540165/");
            vals.put(maxReadSizeInBytes, "1048576");

            vals.put(openBISUrl, "http://localhost:8888");
            vals.put(openBISTimeout, Integer.toString(Integer.MAX_VALUE));
            vals.put(openBISUser, "system");
            vals.put(openBISPassword, "changeit");
            Configuration configuration = new Configuration(vals);
            StartupMain.setConfiguration(configuration);
        }

        return StartupMain.getConfiguration();
    }

    public static OpenBIS getOpenBis()
    {
        Configuration configuration = getConfiguration();
        return new OpenBIS(configuration.getStringProperty(openBISUrl),
                Integer.parseInt(configuration.getStringProperty(openBISTimeout)));

    }

}
