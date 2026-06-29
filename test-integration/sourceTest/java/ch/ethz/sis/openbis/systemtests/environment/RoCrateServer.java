package ch.ethz.sis.openbis.systemtests.environment;

import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;

import java.io.*;
import java.util.Properties;

public class RoCrateServer
{

    private static final Logger log = LogFactory.getLogger(RoCrateServer.class);

    private Properties serviceProperties;
    private String loggingConfigurationFilePath;

    private IntegrationTestEnvironment.RoCrateServerArgs roCrateServerArgs;

    public IntegrationTestEnvironment.RoCrateServerArgs getRoCrateServerArgs()
    {
        return roCrateServerArgs;
    }

    public void setRoCrateServerArgs(
            IntegrationTestEnvironment.RoCrateServerArgs roCrateServerArgs)
    {
        this.roCrateServerArgs = roCrateServerArgs;
    }

    public void configure(final Properties serviceProperties)
    {
        if (serviceProperties == null)
        {
            throw new RuntimeException("Service properties cannot be null");
        }
        this.serviceProperties = serviceProperties;
    }

    public void configureLoggingConfigurationFilePath(final String loggingConfigurationFilePath)
    {
        this.loggingConfigurationFilePath = loggingConfigurationFilePath;
    }

    public void start()
    {
        if (serviceProperties == null)
        {
            throw new RuntimeException("Ro-crate server hasn't been configured.");
        }

        try
        {
            log.info("Starting ro-crate server.");

            File tempConfigurationFile = File.createTempFile("ro-crate-server", ".properties");
            tempConfigurationFile.deleteOnExit();

            serviceProperties.store(new FileWriter(tempConfigurationFile), null);

            ProcessBuilder processBuilder = new ProcessBuilder();
            if(loggingConfigurationFilePath != null) {
                processBuilder.environment().put("ro-crate.logging.configuration",
                        loggingConfigurationFilePath);
            }
            processBuilder.command(
                    new String[] { "../test-integration/etc/default/ro-crate/start.sh",
                            tempConfigurationFile.getAbsolutePath() });
            if (roCrateServerArgs != null)
            {
                processBuilder.environment().put("RO_CRATE_SERVER_LOCAL_DOWNLOAD_PORT",
                        Integer.toString(roCrateServerArgs.port()));
            }
            Process process = processBuilder.start();

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

    public void stop()
    {
        try
        {
            Process process = Runtime.getRuntime().exec(new String[] { "../test-integration/etc/default/ro-crate/stop.sh" });

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

    public Properties getServiceProperties()
    {
        return serviceProperties;
    }
}
