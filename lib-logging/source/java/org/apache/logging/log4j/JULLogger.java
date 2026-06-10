package org.apache.logging.log4j;

import ch.ethz.sis.shared.log.standard.core.Priority;

/**
 * Compatibility bridge for libraries expecting Log4j 2.x Logger API.
 *
 * @deprecated Use {@code ch.ethz.sis.shared.log.standard} classes instead.
 */
@Deprecated
public class JULLogger extends ch.ethz.sis.shared.log.classic.impl.Logger implements Logger
{
    private static final String LOG4J_TAG = "[Log4j log]";

    protected JULLogger(String name)
    {
        super(name);
    }

    public static JULLogger getLogger(String name)
    {
        return new JULLogger(name);
    }

    public static JULLogger getLogger(Class<?> clazz)
    {
        return getLogger(clazz.getName());
    }

    public static JULLogger getRootLogger()
    {
        return getLogger("");
    }

    private String decorate(Object message)
    {
        return LOG4J_TAG + "[" + getName() + "] " + String.valueOf(message);
    }

    private LogBuilder logBuilder(Level level)
    {
        return new JULLogBuilder(this, level);
    }

    @Override
    public void debug(Object message)
    {
        super.debug(decorate(message));
    }

    @Override
    public void debug(String message)
    {
        debug((Object) message);
    }

    @Override
    public void debug(Object message, Throwable t)
    {
        super.debug(decorate(message), t);
    }

    @Override
    public void info(Object message)
    {
        super.info(decorate(message));
    }

    @Override
    public void info(String message)
    {
        info((Object) message);
    }

    @Override
    public void info(Object message, Throwable t)
    {
        super.info(decorate(message), t);
    }

    @Override
    public void warn(Object message)
    {
        super.warn(decorate(message));
    }

    @Override
    public void warn(String message)
    {
        warn((Object) message);
    }

    @Override
    public void warn(String message, Object p0)
    {
        warn(Log4jMessageFormatter.format(message, p0));
    }

    @Override
    public void warn(String message, Object p0, Object p1)
    {
        warn(Log4jMessageFormatter.format(message, p0, p1));
    }

    @Override
    public void warn(Object message, Throwable t)
    {
        super.warn(decorate(message), t);
    }

    @Override
    public void error(Object message)
    {
        super.error(decorate(message));
    }

    @Override
    public void error(String message)
    {
        error((Object) message);
    }

    @Override
    public void error(Object message, Throwable t)
    {
        super.error(decorate(message), t);
    }

    @Override
    public void fatal(Object message)
    {
        super.fatal(decorate("FATAL: " + message));
    }

    @Override
    public void fatal(Object message, Throwable t)
    {
        super.fatal(decorate("FATAL: " + message), t);
    }

    @Override
    public void trace(String message)
    {
        super.trace(decorate(message));
    }

    @Override
    public LogBuilder atTrace()
    {
        return logBuilder(Level.TRACE);
    }

    @Override
    public LogBuilder atDebug()
    {
        return logBuilder(Level.DEBUG);
    }

    @Override
    public LogBuilder atInfo()
    {
        return logBuilder(Level.INFO);
    }

    @Override
    public LogBuilder atWarn()
    {
        return logBuilder(Level.WARN);
    }

    @Override
    public LogBuilder atError()
    {
        return logBuilder(Level.ERROR);
    }

    @Override
    public LogBuilder atLevel(Level level)
    {
        return logBuilder(level);
    }

    @Override
    public void log(Priority priority, Object message)
    {
        super.log(priority, decorate(message));
    }

    @Override
    public void log(Priority priority, Object message, Throwable t)
    {
        super.log(priority, decorate(message), t);
    }

    @Override
    public void catching(Throwable ex)
    {
        super.catching(ex);
    }

    @Override
    public void setLevel(ch.ethz.sis.shared.log.standard.core.Level level)
    {
        super.setLevel(level);
    }

    @Override
    public ch.ethz.sis.shared.log.standard.core.Level getLevel()
    {
        return super.getLevel();
    }

    @Override
    public ch.ethz.sis.shared.log.standard.core.Level getEffectiveLevel()
    {
        return super.getEffectiveLevel();
    }
}
