package ch.ethz.sis.foldermonitor;

import java.nio.file.Path;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class NopFolderMonitorTask implements FolderMonitorTask
{
    private static final Logger logger = LogManager.getLogger(NopFolderMonitorTask.class);

    @Override public void configure(final Properties properties)
    {
        logger.info("Configured with properties: " + properties);
    }

    @Override public void process(final Path incoming)
    {
        logger.info("Processed path: " + incoming);
    }
}
