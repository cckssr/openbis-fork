package ch.ethz.sis.afsserver.server.common;

import org.apache.commons.logging.Log;

import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;

public class ApacheCommonsLoggingConfiguration implements Log
{

    public static void reconfigureToUseAFSLogging()
    {
        System.setProperty("org.apache.commons.logging.Log", ApacheCommonsLoggingConfiguration.class.getName());
    }

    private final Logger logger;

    public ApacheCommonsLoggingConfiguration(String name) throws Exception
    {
        this.logger = LogManager.getLogger(Class.forName(name));
    }

    @Override public boolean isTraceEnabled()
    {
        return logger.isTraceEnabled();
    }

    @Override public boolean isDebugEnabled()
    {
        return logger.isTraceEnabled();
    }

    @Override public boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }

    @Override public boolean isWarnEnabled()
    {
        return logger.isErrorEnabled();
    }

    @Override public boolean isErrorEnabled()
    {
        return logger.isErrorEnabled();
    }

    @Override public boolean isFatalEnabled()
    {
        return logger.isErrorEnabled();
    }

    @Override public void trace(final Object o)
    {
        logger.traceAccess(String.valueOf(o));
    }

    @Override public void trace(final Object o, final Throwable throwable)
    {
        logger.traceAccess(String.valueOf(o), throwable);
    }

    @Override public void debug(final Object o)
    {
        trace(o);
    }

    @Override public void debug(final Object o, final Throwable throwable)
    {
        trace(o, throwable);
    }

    @Override public void info(final Object o)
    {
        logger.info(String.valueOf(o));
    }

    @Override public void info(final Object o, final Throwable throwable)
    {
        logger.info(String.valueOf(o), throwable);
    }

    @Override public void warn(final Object o)
    {
        error(o);
    }

    @Override public void warn(final Object o, final Throwable throwable)
    {
        error(o, throwable);
    }

    @Override public void error(final Object o)
    {
        logger.catching(new RuntimeException(String.valueOf(o)));
    }

    @Override public void error(final Object o, final Throwable throwable)
    {
        logger.catching(new RuntimeException(String.valueOf(o), throwable));
    }

    @Override public void fatal(final Object o)
    {
        error(o);
    }

    @Override public void fatal(final Object o, final Throwable throwable)
    {
        error(o, throwable);
    }
}
