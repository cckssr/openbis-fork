package ch.ethz.sis.afsserver.server.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import ch.ethz.sis.afsserver.server.OperationResult;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.ethz.sis.shared.startup.Configuration;

public class OperationResultCache implements ch.ethz.sis.afsserver.server.OperationResultCache
{

    private static final Logger logger = LogManager.getLogger(OperationResultCache.class);

    private static final int CLEAN_UP_INTERVAL = 1000;

    private final Map<UUID, OperationResult> results = new ConcurrentHashMap<>();

    private final Map<UUID, Long> timeoutTimes = new ConcurrentHashMap<>();

    private long thresholdTime;

    private Integer lastLoggedSize;

    private long timeout;

    @Override public void init(final Configuration configuration)
    {
        thresholdTime = configuration.getIntegerProperty(AtomicFileSystemServerParameter.operationResultCacheThresholdTime);
        timeout = configuration.getIntegerProperty(AtomicFileSystemServerParameter.operationResultCacheTimeout);

        Timer cleanupTask = new Timer();
        cleanupTask.schedule(
                new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        removeTimedOutEntries();
                        logCacheSizeIfChanged();
                    }
                }, 0, CLEAN_UP_INTERVAL
        );
    }

    @Override public void setResult(final UUID operationId, final String operationName, final long operationDuration, final OperationResult result)
    {
        if (operationDuration > thresholdTime && !"status".equals(operationName))
        {
            results.put(operationId, result);
            timeoutTimes.put(operationId, System.currentTimeMillis() + timeout);
        }
    }

    @Override public OperationResult getResult(final UUID operationId)
    {
        OperationResult result = results.get(operationId);
        // a result can be retrieved only once
        results.remove(operationId);
        timeoutTimes.remove(operationId);
        return result;
    }

    private void removeTimedOutEntries()
    {
        List<UUID> toRemove = new LinkedList<>();

        for (Map.Entry<UUID, Long> entry : timeoutTimes.entrySet())
        {
            UUID operationId = entry.getKey();
            long timeoutTime = entry.getValue();

            if (System.currentTimeMillis() > timeoutTime)
            {
                toRemove.add(operationId);
            }
        }

        for (UUID operationId : toRemove)
        {
            results.remove(operationId);
            timeoutTimes.remove(operationId);
        }
    }

    private void logCacheSizeIfChanged()
    {
        int currentSize = results.size();
        if (lastLoggedSize == null || lastLoggedSize != currentSize)
        {
            logger.info("Cached " + currentSize + " result(s)");
            lastLoggedSize = currentSize;
        }
    }

}
