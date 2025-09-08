package ch.ethz.sis.foldermonitor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Properties;
import java.util.Timer;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ch.systemsx.cisd.common.filesystem.DirectoryScanningTimerTask;
import ch.systemsx.cisd.common.filesystem.FaultyPathDirectoryScanningHandler;
import ch.systemsx.cisd.common.filesystem.FileConstants;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.filesystem.IDirectoryScanningHandler;
import ch.systemsx.cisd.common.filesystem.IPathHandler;
import ch.systemsx.cisd.common.filesystem.IStoreItemFilter;
import ch.systemsx.cisd.common.filesystem.LastModificationChecker;
import ch.systemsx.cisd.common.filesystem.QuietPeriodFileFilter;
import ch.systemsx.cisd.common.filesystem.StoreItem;
import ch.systemsx.cisd.common.reflection.ClassUtils;

public class FolderMonitor
{

    private static final Logger logger = LogManager.getLogger(FolderMonitor.class);

    private final FolderMonitorConfiguration configuration;

    private Timer timer;

    public FolderMonitor(FolderMonitorConfiguration configuration)
    {
        if (configuration == null)
        {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        if (!Files.exists(configuration.getFolder()))
        {
            throw new IllegalArgumentException("Folder to be monitored does not exists");
        }

        if (!Files.isDirectory(configuration.getFolder()))
        {
            throw new IllegalArgumentException("Folder to be monitored is not a folder");
        }

        this.configuration = configuration;
    }

    public void start()
    {
        if (timer != null)
        {
            throw new RuntimeException("Monitor has been already started");
        }

        FileFilter fileFilter = createFileFilter(configuration);
        IDirectoryScanningHandler directoryScanningHandler = createDirectoryScanningHandler(configuration);
        IPathHandler pathHandler = createPathHandler(configuration);

        DirectoryScanningTimerTask task =
                new DirectoryScanningTimerTask(configuration.getFolder().toFile(), fileFilter, pathHandler, directoryScanningHandler);

        timer = new Timer();
        timer.schedule(task, 0L, configuration.getCheckingInterval());
    }

    public void stop()
    {
        if (timer == null)
        {
            throw new RuntimeException("Monitor hasn't been started yet");
        }

        timer.cancel();
    }

    private FileFilter createFileFilter(FolderMonitorConfiguration configuration)
    {
        if (FolderMonitorMode.MARKER_FILE.equals(configuration.getMode()))
        {
            return FileFilterUtils.prefixFileFilter(FileConstants.IS_FINISHED_PREFIX);
        } else if (FolderMonitorMode.QUIET_PERIOD.equals(configuration.getMode()))
        {
            int ignoredErrorCountBeforeNotification = 3;
            LastModificationChecker lastModificationChecker =
                    new LastModificationChecker(configuration.getFolder().toFile());
            final IStoreItemFilter quietPeriodFilter =
                    new QuietPeriodFileFilter(lastModificationChecker,
                            configuration.getQuietPeriod(), ignoredErrorCountBeforeNotification);
            return pathname ->
            {
                assert pathname.getParentFile().getAbsolutePath()
                        .equals(configuration.getFolder().toFile()
                                .getAbsolutePath()) : "The file should come to the filter only from the incoming directory";

                StoreItem storeItem = new StoreItem(pathname.getName());
                return quietPeriodFilter.accept(storeItem);
            };
        } else
        {
            throw new RuntimeException("Unsupported mode: " + configuration.getMode());
        }
    }

    private IDirectoryScanningHandler createDirectoryScanningHandler(FolderMonitorConfiguration configuration)
    {
        return new FaultyPathDirectoryScanningHandler(configuration.getFolder().toFile(), () -> false, null);
    }

    private IPathHandler createPathHandler(FolderMonitorConfiguration configuration)
    {
        return new IPathHandler()
        {
            @Override public void handle(final File incomingOrMarkerFile)
            {
                File incoming;

                if (FolderMonitorMode.MARKER_FILE.equals(configuration.getMode()))
                {
                    incoming = FileUtilities.removePrefixFromFileName(incomingOrMarkerFile, FileConstants.IS_FINISHED_PREFIX);
                } else
                {
                    incoming = incomingOrMarkerFile;
                }

                logger.info("Before processing: " + incoming);

                FolderMonitorTask task;

                try
                {
                    task = ClassUtils.create(FolderMonitorTask.class, configuration.getTaskClass());
                } catch (Exception e)
                {
                    throw new RuntimeException("Could not create an instance of task '" + configuration.getTaskClass().getName() + "'");
                }

                task.configure(configuration.getTaskProperties());
                task.process(incoming.toPath());

                logger.info("After processing: " + incoming);

                if (incoming.exists())
                {
                    logger.info("Deleting incoming: " + incoming);
                    FileUtilities.deleteRecursively(incoming);
                }

                if (FolderMonitorMode.MARKER_FILE.equals(configuration.getMode()) && incomingOrMarkerFile.exists())
                {
                    logger.info("Deleting marker file: " + incomingOrMarkerFile);
                    FileUtilities.delete(incomingOrMarkerFile);
                }
            }

            @Override public boolean isStopped()
            {
                return false;
            }
        };
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 1)
        {
            throw new RuntimeException("Expected only 1 argument - a path to the configuration file");
        }

        Properties properties = new Properties();
        properties.load(new BufferedInputStream(new FileInputStream(args[0])));

        FolderMonitor monitor = new FolderMonitor(new FolderMonitorConfiguration(properties));
        monitor.start();
    }

}
