package ch.ethz.sis.openbis.systemtests.environment;

import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;

import java.io.*;
import java.util.Properties;

public class SftpServer
{

    private static final Logger log = LogFactory.getLogger(SftpServer.class);

    private Properties serviceProperties;

    public void configure(final Properties serviceProperties)
    {
        if (serviceProperties == null)
        {
            throw new RuntimeException("Service properties cannot be null");
        }
        this.serviceProperties = serviceProperties;
    }

    public void start()
    {
        if (serviceProperties == null)
        {
            throw new RuntimeException("SFTP server hasn't been configured.");
        }

        try
        {
            log.info("Starting SFTP server.");

            File tempConfigurationFile = File.createTempFile("server-sftp", ".properties");
            tempConfigurationFile.deleteOnExit();

            serviceProperties.store(new FileWriter(tempConfigurationFile), null);

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(
                    new String[] { "../test-integration/etc/default/sftp/start.sh",
                            tempConfigurationFile.getAbsolutePath() });

            Process process = processBuilder.start();

            InputStream in = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null)
            {
                System.out.println("[SFTP] " + line);

                if (line.contains("Server started") || line.contains("Server died"))
                {
                    log.info("Started SFTP server.");
                    break;
                }
            }
        } catch (Exception e)
        {
            log.error("Starting SFTP server failed.", e);
            throw new RuntimeException(e);
        }
    }

    public void stop()
    {
        try
        {
            Process process = Runtime.getRuntime().exec(new String[] { "../test-integration/etc/default/sftp/stop.sh" });

            InputStream in = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null)
            {
                System.out.println("[SFTP] " + line);
            }
            log.info("Stopped SFTP server.");
        } catch (Exception e)
        {
            log.error("Stopping SFTP server failed.", e);
            throw new RuntimeException(e);
        }
    }

    public Properties getServiceProperties()
    {
        return serviceProperties;
    }
}
