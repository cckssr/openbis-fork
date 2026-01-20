/*
 * Copyright ETH 2011 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.common.api.retry;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.exception.ExceptionUtils;

import ch.ethz.sis.shared.log.classic.ISimpleLogger;
import ch.ethz.sis.shared.log.classic.core.LogLevel;
import ch.ethz.sis.shared.log.classic.impl.ConsoleLogger;
import ch.systemsx.cisd.common.api.retry.config.DefaultRetryConfiguration;
import ch.systemsx.cisd.common.api.retry.config.RetryConfiguration;
import org.springframework.remoting.RemoteConnectFailureException;

/**
 * @author pkupczyk
 */
public abstract class RetryCaller<T, E extends Throwable>
{
    private final RetryConfiguration configuration;

    private int retryCounter;

    private int waitingTime;

    private final ISimpleLogger logger;

    public RetryCaller()
    {
        this(DefaultRetryConfiguration.getInstance());
    }

    public RetryCaller(RetryConfiguration configuration)
    {
        this(configuration, new ConsoleLogger(System.err));
    }

    public RetryCaller(RetryConfiguration configuration, ISimpleLogger logger)
    {
        if (configuration == null)
        {
            throw new IllegalArgumentException("Configuration was null");
        }
        if (configuration.getMaximumNumberOfRetries() < 0)
        {
            throw new IllegalArgumentException("MaximumNumberOfRetries must be >= 0");
        }
        if (configuration.getWaitingTimeBetweenRetries() <= 0)
        {
            throw new IllegalArgumentException("WaitingTimeBetweenRetries must be > 0");
        }
        if (configuration.getWaitingTimeBetweenRetriesIncreasingFactor() <= 0)
        {
            throw new IllegalArgumentException(
                    "WaitingTimeBetweenRetriesIncreasingFactor must be > 0");
        }

        this.configuration = configuration;
        this.waitingTime = configuration.getWaitingTimeBetweenRetries();
        this.logger = logger;
    }

    protected abstract T call() throws E;

    public T callWithRetry() throws E
    {
        while (true)
        {
            try
            {
                return call();
            }
            catch (RuntimeException e)
            {
                if (isRetryableException(e))
                {
                    if (shouldRetry())
                    {
                        logger.log(LogLevel.WARN, "Call failed - will retry");
                        waitForRetry();
                        continue;
                    }
                    logger.log(LogLevel.WARN, "Call failed - will NOT retry");
                }
                throw e;
            }
        }
    }

    protected boolean isRetryableException(RuntimeException e)
    {
        if (e == null) return false;

        // Check direct cause chain
        for (Throwable t = e; t != null; t = t.getCause())
        {
            if (isRetryableRemoteAccessCause(t))
                return true;
        }

        Throwable root = ExceptionUtils.getRootCause(e);
        return isRetryableRemoteAccessCause(root);
    }

    private boolean isRetryableRemoteAccessCause(Throwable t)
    {
        if (t == null) return false;

        if (t instanceof ConnectException) return true;
        if (t instanceof UnknownHostException) return true;
        if (t instanceof NoRouteToHostException) return true;
        if (t instanceof SocketTimeoutException) return true;
        if (t instanceof SocketException) return true;
        if (t instanceof TimeoutException) return true;
        if (t instanceof RemoteConnectFailureException) return true;
        return t instanceof IOException;
    }

    private boolean shouldRetry()
    {
        return retryCounter < configuration.getMaximumNumberOfRetries();
    }

    private void waitForRetry()
    {
        try
        {
            Thread.sleep(waitingTime);
            waitingTime *= configuration.getWaitingTimeBetweenRetriesIncreasingFactor();
            retryCounter++;
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

}
