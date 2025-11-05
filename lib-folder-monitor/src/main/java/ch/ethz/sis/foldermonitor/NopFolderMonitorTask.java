package ch.ethz.sis.foldermonitor;

import java.nio.file.Path;
import java.util.Properties;

import ch.ethz.sis.openbis.generic.foldermonitor.v3.FolderMonitorTask;
import ch.ethz.sis.shared.log.classic.impl.LogManager;
import ch.ethz.sis.shared.log.classic.impl.Logger;

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
