package org.apache.logging.log4j;

public final class LogManager
{
    private LogManager()
    {
    }

    public static Logger getLogger(Class<?> clazz)
    {
        return JULLogger.getLogger(clazz);
    }

    public static Logger getLogger(String name)
    {
        return JULLogger.getLogger(name);
    }

    public static Logger getRootLogger()
    {
        return JULLogger.getRootLogger();
    }
}
