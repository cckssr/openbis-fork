import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.ros.startup.Configuration;
import ch.ethz.sis.openbis.ros.startup.StartupMain;

import java.util.LinkedHashMap;
import java.util.Map;

import static ch.ethz.sis.openbis.ros.startup.RoCrateServerParameter.*;

public class AbstractTest
{

    public Configuration getConfiguration()
    {
        Map<Enum, String> vals = new LinkedHashMap<>();
        vals.put(httpServerPort, "8085");
        vals.put(sessionWorkSpace, "1048576");

        vals.put(httpMaxContentLength, "1540165/");
        vals.put(maxReadSizeInBytes, "/tmp/ro-crate-server/");

        vals.put(openBISUrl, "http://localhost:8888");
        vals.put(openBISTimeout, "30000");
        vals.put(openBISUser, "system");
        vals.put(openBISPassword, "changeit");

        Configuration configuration = new Configuration(vals);

        StartupMain.setConfiguration(configuration);

        return configuration;

    }

    public OpenBIS getOpenBis()
    {
        Configuration configuration = getConfiguration();
        return new OpenBIS(configuration.getStringProperty(openBISUrl),
                Integer.parseInt(configuration.getStringProperty(openBISTimeout)));

    }

}
