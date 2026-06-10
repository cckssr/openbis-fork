package ch.ethz.sis.rocrateserver.openapi.v1.service.helper;

import ch.ethz.sis.rocrateserver.openapi.v1.service.delegates.ImportDelegate;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.AsyncJobRegistry;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.ExportJob;
import ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.ImportJob;
import ch.ethz.sis.rocrateserver.startup.Configuration;
import ch.ethz.sis.rocrateserver.startup.RoCrateServerParameter;
import ch.ethz.sis.shared.log.classic.impl.Logger;

import java.io.IOException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;

public class SessionWorkSpaceCleanupTimer
{

    public static final int PERIOD = 30000;

    private static final Logger LOG = Logger.getLogger(ImportDelegate.class);

    public static final int DELAY = 5000;

    record Thresholds(long minutesDownloaded, long minutesNotDownloaded)
    {
    }

    public static Timer getTimer(Clock clock, Configuration configuration,
            AsyncJobRegistry asyncJobRegistry)
    {
        Timer timer = new Timer();
        long threshold1 =
                getProperty(configuration, RoCrateServerParameter.cleanupTimeDownloadedCrates,
                        60 * 12);
        long threshold2 =
                getProperty(configuration, RoCrateServerParameter.cleanupTimeWaitingCrates,
                        60 * 24);
        Thresholds thresholds = new Thresholds(threshold1, threshold2);

        timer.schedule(getTask(clock, asyncJobRegistry, thresholds), DELAY, PERIOD);

        return timer;

    }

    private static long getProperty(Configuration configuration, RoCrateServerParameter parameter,
            long defaultValue)
    {
        try
        {
            return configuration.getIntegerProperty(parameter);
        } catch (NumberFormatException e)
        {
            LOG.info("Cannot read value " + parameter.name() + ", defaulting to " + defaultValue);
            return defaultValue;
        }
    }


    ;

    private static TimerTask getTask(Clock clock, AsyncJobRegistry asyncJobRegistry,
            Thresholds thresholds)
    {
        TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                AsyncJobRegistry.CompletedAndFailedJobs completedAndFailedJobs =
                        asyncJobRegistry.getCompletedExportJobs();

                try
                {
                    for (

                            ExportJob failedExport : completedAndFailedJobs.failedExports()

                    )
                    {
                        LOG.info("Cleaning up failed export " + failedExport.getJobId());
                        cleanUp(failedExport, asyncJobRegistry);
                    }
                    for (
                            ExportJob exportJob : completedAndFailedJobs.downloaded()
                    )
                    {
                        if (exportJob.getCompletionTimestamp()
                                .isBefore(clock.instant().minus(thresholds.minutesDownloaded(),
                                        ChronoUnit.MINUTES)))
                        {
                            LOG.info("Cleaning up downloaded export " + exportJob.getJobId());

                            cleanUp(exportJob, asyncJobRegistry);
                        }
                    }
                    for (
                            ExportJob exportJob : completedAndFailedJobs.notDownloaded()
                    )
                    {
                        LOG.info("Cleaning up neglected export " + exportJob.getJobId());
                        if (exportJob.getCompletionTimestamp()
                                .isBefore(clock.instant().minus(thresholds.minutesNotDownloaded,
                                        ChronoUnit.MINUTES)))
                        {
                            cleanUp(exportJob, asyncJobRegistry);
                        }
                    }
                    for (ImportJob importJob : completedAndFailedJobs.failedAndCompletedImportJobs())
                    {
                        LOG.info("Cleaning up " + importJob.getStatus()
                                .toString() + " import " + importJob.getJobId());
                        if (importJob.getCompletionOrFailInstant()
                                .isBefore(clock.instant().minus(thresholds.minutesNotDownloaded,
                                        ChronoUnit.MINUTES)))
                        {
                            cleanUp(importJob, asyncJobRegistry);
                        }
                    }
                } catch (IOException ioException)
                {
                    LOG.error(ioException.getMessage(), ioException);

                }

            }
        };
        return timerTask;

    }

    private static void cleanUp(ExportJob exportJob, AsyncJobRegistry asyncJobRegistry)
            throws IOException
    {
        asyncJobRegistry.remove(exportJob.getUserId(), exportJob.getJobId());
        exportJob.delete();
    }

    private static void cleanUp(ImportJob importJob, AsyncJobRegistry asyncJobRegistry)
            throws IOException
    {
        importJob.delete();
    }

}
