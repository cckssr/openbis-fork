package ch.ethz.sis.openbis.systemtests.environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;

public class RoCrateServer implements Server<RoCrateServerConfiguration>
{

    private static final Logger log = LogFactory.getLogger(RoCrateServer.class);

    private RoCrateServerConfiguration configuration;

    @Override public void configure(final RoCrateServerConfiguration configuration)
    {
        this.configuration = configuration;
    }

    @Override public void start()
    {
        if (configuration == null)
        {
            throw new RuntimeException("Ro-crate server hasn't been configured.");
        }

        try
        {
            log.info("Starting ro-crate server.");

            File tempConfigurationFile = File.createTempFile("ro-crate-server", ".properties");
            tempConfigurationFile.deleteOnExit();

            Properties serviceProperties = configuration.getServiceProperties();
            serviceProperties.store(new FileWriter(tempConfigurationFile), null);

            Process process = Runtime.getRuntime()
                    .exec(new String[] { "../test-integration/etc/ro-crate/start.sh", tempConfigurationFile.getAbsolutePath() });

            InputStream in = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null)
            {
                System.out.println("[RO-CRATE] " + line);

                if (line.contains("Started RO-CRATE server") || line.contains("Startup of RO-CRATE server failed"))
                {
                    log.info("Started ro-crate server.");
                    break;
                }
            }
        } catch (Exception e)
        {
            log.error("Starting ro-crate server failed.", e);
            throw new RuntimeException(e);
        }
    }

    @Override public void stop()
    {
        try
        {
            Process process = Runtime.getRuntime().exec(new String[] { "../test-integration/etc/ro-crate/stop.sh" });

            InputStream in = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null)
            {
                System.out.println("[RO-CRATE] " + line);
            }
            log.info("Stopped ro-crate server.");
        } catch (Exception e)
        {
            log.error("Stopping ro-crate server failed.", e);
            throw new RuntimeException(e);
        }
    }

    @Override public RoCrateServerConfiguration getConfiguration()
    {
        return configuration;
    }

    @Override public StringBuffer getLogs()
    {
        return null;
    }
}
