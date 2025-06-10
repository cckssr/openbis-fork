package ch.ethz.sis.afsserver.server.common;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.OutputStreamManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;

import ch.ethz.sis.shared.log.LogManager;
import ch.ethz.sis.shared.log.log4j2.Log4J2LogFactory;

public class TestLogger
{

    public static String DEFAULT_LOG_LAYOUT_PATTERN = "%-5p %c - %m%n";

    public static String DEFAULT_LOGGER_NAME_REGEX = ".*";

    private static LoggerContext loggerContext;

    private static Appender recordingAppender;

    private static ByteArrayOutputStream recordedLog;

    public static void configure()
    {
        loggerContext = Configurator.initialize(null, "log.xml");
        LogManager.setLogFactory(new Log4J2LogFactory());
    }

    public static void startLogRecording(Level level)
    {
        startLogRecording(level, DEFAULT_LOG_LAYOUT_PATTERN, DEFAULT_LOGGER_NAME_REGEX);
    }

    public static void startLogRecording(Level level, String logLayoutPattern, String loggerNameRegex)
    {
        if (loggerContext == null)
        {
            throw new RuntimeException("Test logger hasn't been configured yet.");
        }

        if (recordingAppender != null)
        {
            throw new RuntimeException("Test log recording has been already started.");
        }

        recordedLog = new ByteArrayOutputStream();
        recordingAppender =
                TestAppender.createAppender(recordedLog, level, PatternLayout.newBuilder().withPattern(logLayoutPattern).build(), loggerNameRegex);
        recordingAppender.start();
        loggerContext.getRootLogger().addAppender(recordingAppender);
    }

    public static void stopLogRecording()
    {
        if (loggerContext == null)
        {
            throw new RuntimeException("Test logger hasn't been configured yet.");
        }

        if (recordingAppender == null)
        {
            throw new RuntimeException("Test log recording hasn't been started yet.");
        }

        recordingAppender.stop();
        loggerContext.getRootLogger().removeAppender(recordingAppender);
        recordingAppender = null;
        recordedLog = null;
    }

    public static void resetRecordedLog()
    {
        if (recordingAppender == null)
        {
            throw new RuntimeException("Test log recording hasn't been started yet.");
        }
        recordedLog.reset();
    }

    public static String getRecordedLog()
    {
        if (recordingAppender == null)
        {
            throw new RuntimeException("Test log recording hasn't been started yet.");
        }
        return recordedLog.toString();
    }

    private static class TestOutputStreamManager extends OutputStreamManager
    {

        public TestOutputStreamManager(final OutputStream os, final String streamName, final Layout<?> layout, final boolean writeHeader)
        {
            super(os, streamName, layout, writeHeader);
        }
    }

    private static class TestAppender extends AbstractOutputStreamAppender<OutputStreamManager>
    {

        private TestAppender(final String name, final Layout layout, final Filter filter, final boolean ignoreExceptions,
                final boolean immediateFlush, final OutputStreamManager manager)
        {
            super(name, layout, filter, ignoreExceptions, immediateFlush, manager);
        }

        private static TestAppender createAppender(ByteArrayOutputStream output, Level logLevel, Layout logLayout, final String loggerNameRegex)
        {
            OutputStreamManager manager = OutputStreamManager.getManager("test-appender-stream-manager", (Object) null,
                    (name, data) -> new TestOutputStreamManager(output, name, logLayout, true));

            TestAppender appender = new TestAppender("test-appender", logLayout,
                    ThresholdFilter.createFilter(logLevel, Filter.Result.NEUTRAL, Filter.Result.DENY), false, true, manager);
            appender.addFilter(new LoggerNameFilter(loggerNameRegex));
            return appender;
        }
    }

    private static class LoggerNameFilter extends AbstractFilter
    {

        private final String loggerNameRegex;

        public LoggerNameFilter(String loggerNameRegex)
        {
            this.loggerNameRegex = loggerNameRegex;
        }

        private Result filterByLoggerName(String loggerName)
        {
            if (loggerName.matches(loggerNameRegex))
            {
                return Result.NEUTRAL;
            } else
            {
                return Result.DENY;
            }
        }

        @Override public Result filter(final LogEvent event)
        {
            return filterByLoggerName(event.getLoggerName());
        }

        @Override public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t)
        {
            return filterByLoggerName(logger.getName());
        }

        @Override public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t)
        {
            return filterByLoggerName(logger.getName());
        }

        @Override public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object... params)
        {
            return filterByLoggerName(logger.getName());
        }

        @Override public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0)
        {
            return filterByLoggerName(logger.getName());
        }

        @Override public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0,
                final Object p1)
        {
            return filterByLoggerName(logger.getName());
        }

        @Override public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0,
                final Object p1, final Object p2)
        {
            return filterByLoggerName(logger.getName());
        }

        @Override public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0,
                final Object p1, final Object p2, final Object p3)
        {
            return filterByLoggerName(logger.getName());
        }

        @Override public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0,
                final Object p1, final Object p2, final Object p3, final Object p4)
        {
            return filterByLoggerName(logger.getName());
        }

        @Override public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0,
                final Object p1, final Object p2, final Object p3, final Object p4,
                final Object p5)
        {
            return filterByLoggerName(logger.getName());
        }

        @Override public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0,
                final Object p1, final Object p2, final Object p3, final Object p4,
                final Object p5, final Object p6)
        {
            return filterByLoggerName(logger.getName());
        }

        @Override public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0,
                final Object p1, final Object p2, final Object p3, final Object p4,
                final Object p5, final Object p6, final Object p7)
        {
            return filterByLoggerName(logger.getName());
        }

        @Override public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0,
                final Object p1, final Object p2, final Object p3, final Object p4,
                final Object p5, final Object p6, final Object p7, final Object p8)
        {
            return filterByLoggerName(logger.getName());
        }

        @Override public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0,
                final Object p1, final Object p2, final Object p3, final Object p4,
                final Object p5, final Object p6, final Object p7, final Object p8, final Object p9)
        {
            return filterByLoggerName(logger.getName());
        }
    }

    public static void main(String[] args)
    {
        TestLogger.configure();
    }

}
