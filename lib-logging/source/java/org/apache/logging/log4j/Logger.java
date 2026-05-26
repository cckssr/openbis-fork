package org.apache.logging.log4j;

public interface Logger
{
    void debug(String message);

    void info(String message);

    void warn(String message);

    void warn(String message, Object p0);

    void warn(String message, Object p0, Object p1);

    void error(String message);

    void trace(String message);

    boolean isDebugEnabled();

    boolean isTraceEnabled();

    default LogBuilder atTrace()
    {
        return LogBuilder.NO_OP;
    }

    default LogBuilder atDebug()
    {
        return LogBuilder.NO_OP;
    }

    default LogBuilder atInfo()
    {
        return LogBuilder.NO_OP;
    }

    default LogBuilder atWarn()
    {
        return LogBuilder.NO_OP;
    }

    default LogBuilder atError()
    {
        return LogBuilder.NO_OP;
    }

    default LogBuilder atLevel(Level level)
    {
        return LogBuilder.NO_OP;
    }
}
