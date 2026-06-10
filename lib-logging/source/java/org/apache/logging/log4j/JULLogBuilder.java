package org.apache.logging.log4j;

import org.apache.logging.log4j.util.Supplier;

final class JULLogBuilder implements LogBuilder
{
    private final JULLogger logger;

    private final Level level;

    private Throwable throwable;

    JULLogBuilder(JULLogger logger, Level level)
    {
        this.logger = logger;
        this.level = level;
    }

    @Override
    public LogBuilder withThrowable(Throwable throwable)
    {
        this.throwable = throwable;
        return this;
    }

    @Override
    public void log(String message)
    {
        logFormatted(Log4jMessageFormatter.format(message));
    }

    @Override
    public void log(String message, Object p0)
    {
        logFormatted(Log4jMessageFormatter.format(message, p0));
    }

    @Override
    public void log(String message, Object p0, Object p1)
    {
        logFormatted(Log4jMessageFormatter.format(message, p0, p1));
    }

    @Override
    public void log(String message, Object p0, Object p1, Object p2)
    {
        logFormatted(Log4jMessageFormatter.format(message, p0, p1, p2));
    }

    @Override
    public void log(String message, Object p0, Object p1, Object p2, Object p3)
    {
        logFormatted(Log4jMessageFormatter.format(message, p0, p1, p2, p3));
    }

    @Override
    public void log(String message, Supplier<?>... paramSuppliers)
    {
        Object[] params = null;
        if (paramSuppliers != null)
        {
            params = new Object[paramSuppliers.length];
            for (int i = 0; i < paramSuppliers.length; i++)
            {
                params[i] = paramSuppliers[i] == null ? null : paramSuppliers[i].get();
            }
        }
        logFormatted(Log4jMessageFormatter.format(message, params));
    }

    @Override
    public void log(Supplier<?> messageSupplier)
    {
        logFormatted(String.valueOf(messageSupplier == null ? null : messageSupplier.get()));
    }

    private void logFormatted(String message)
    {
        ch.ethz.sis.shared.log.standard.core.Level targetLevel = toStandardLevel(level);
        if (targetLevel != null)
        {
            logger.log(targetLevel, message, throwable);
        }
    }

    private ch.ethz.sis.shared.log.standard.core.Level toStandardLevel(Level level)
    {
        if (level == null)
        {
            return ch.ethz.sis.shared.log.standard.core.Level.INFO;
        }

        switch (level)
        {
            case ALL:
            case TRACE:
                return ch.ethz.sis.shared.log.standard.core.Level.TRACE;
            case DEBUG:
                return ch.ethz.sis.shared.log.standard.core.Level.DEBUG;
            case INFO:
                return ch.ethz.sis.shared.log.standard.core.Level.INFO;
            case WARN:
                return ch.ethz.sis.shared.log.standard.core.Level.WARN;
            case ERROR:
                return ch.ethz.sis.shared.log.standard.core.Level.ERROR;
            case FATAL:
                return ch.ethz.sis.shared.log.standard.core.Level.FATAL;
            case OFF:
                return null;
            default:
                return ch.ethz.sis.shared.log.standard.core.Level.INFO;
        }
    }
}
