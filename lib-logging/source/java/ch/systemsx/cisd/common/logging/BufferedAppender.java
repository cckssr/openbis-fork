/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.systemsx.cisd.common.logging;

import ch.systemsx.cisd.common.logging.ext.LoggingUtils;
import ch.systemsx.cisd.common.logging.ext.PatternFormatter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import static org.apache.log4j.Logger.getRootLogger;

/**
 * A java.util.logging.Handler that buffers log output in memory until requested.
 * Supports filtering by logger name and suppression patterns.
 */
public final class BufferedAppender extends Handler {

    // Default max buffer size set to 10 MB.
    private static final int DEFAULT_MAX_BUFFER_SIZE = 10 * 1024 * 1024;

    // Maximum allowed buffer size in bytes.
    private final int maxBufferSize;

    // Buffer used to collect log messages.
    private final ByteArrayOutputStream logRecorder = new ByteArrayOutputStream();

    private final List<Pattern> suppressedPatterns = new ArrayList<>();
    private final Pattern includePattern;
    private final Formatter formatter;

    private final List<org.apache.log4j.spi.Filter> log4jFilters = new ArrayList<>();

    // Constructors

    public BufferedAppender() {
        // this was default for log4j
        this(Level.DEBUG);
    }

    public BufferedAppender(Level level) {
        // this was default for log4j
        this("%m%n", level);
    }

    public BufferedAppender(String messagePattern, Level level) {
        this(messagePattern, level, null);
    }

    public BufferedAppender(String messagePattern, Level level, String loggerNameRegex) {
        this(new PatternFormatter(messagePattern), level, loggerNameRegex, DEFAULT_MAX_BUFFER_SIZE);
    }

    public BufferedAppender(Formatter formatter, Level level, String loggerNameRegex) {
        this(formatter, level, loggerNameRegex, DEFAULT_MAX_BUFFER_SIZE);
    }

    public BufferedAppender(Formatter formatter, Level level, String loggerNameRegex, int maxBufferSize) {
        this.includePattern = loggerNameRegex != null ? Pattern.compile(loggerNameRegex) : null;
        this.formatter = formatter;
        this.maxBufferSize = maxBufferSize;
        setLevel(LoggingUtils.mapToJULLevel(level.toInt()));
        getRootLogger().addHandler(this);
    }

    public void addRegexForLoggingEventsToBeDropped(String regex) {
        suppressedPatterns.add(Pattern.compile(regex));
    }

    public void addFilter(org.apache.log4j.spi.Filter filter) {
        log4jFilters.add(filter);
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        for (org.apache.log4j.spi.Filter filter : log4jFilters) {
            if (filter.decide(record) == org.apache.log4j.spi.Filter.DENY) {
                return;
            }
        }

        String loggerName = record.getLoggerName();
        if (includePattern != null && !includePattern.matcher(loggerName).matches()) {
            return;
        }
        String message = formatter.format(record);
        for (Pattern pattern : suppressedPatterns) {
            if (pattern.matcher(message).find()) {
                return;
            }
        }

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

        // If the new message itself is larger than the maxBufferSize,
        // trim it to keep only its tail (the most recent part).
        if (messageBytes.length > maxBufferSize) {
            int start = messageBytes.length - maxBufferSize;
            byte[] trimmed = new byte[maxBufferSize];
            System.arraycopy(messageBytes, start, trimmed, 0, maxBufferSize);
            LoggerDiagnostics.debug("BufferedAppender: New log message is too large; only its tail is kept.");
            messageBytes = trimmed;
            logRecorder.reset();
        }
        // Otherwise, if adding the new message would exceed maxBufferSize,
        // remove enough bytes from the beginning (oldest data).
        else if (logRecorder.size() + messageBytes.length > maxBufferSize) {
            byte[] current = logRecorder.toByteArray();
            int totalSize = current.length + messageBytes.length;
            int dropLength = maxBufferSize/4;
            LoggerDiagnostics.debug(
                    "BufferedAppender: Log buffer exceeded capacity. Dropping "+ dropLength +" bytes (approx. 25%% of max capacity)" +
                            " from oldest log entries.");

            logRecorder.reset();
            logRecorder.write(current, dropLength, current.length - dropLength);
        }

        try {
            logRecorder.write(messageBytes);
        } catch (IOException ignored) {
            // This should not occur with ByteArrayOutputStream, so ignore.
        }
    }


    public void append(LogRecord record) {
        publish(record);
    }

    @Override
    public void flush() {
        try {
            logRecorder.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws SecurityException {
        try {
            logRecorder.close();
        } catch (IOException e) {
            throw new SecurityException(e);
        }
    }

    public String getLogContent() {
        return logRecorder.toString(StandardCharsets.UTF_8).trim();
    }

    public List<String> getLogLines() {
        String content = getLogContent();
        return content.isEmpty() ? List.of() : List.of(content.split("\\r?\\n"));
    }

    public void resetLogContent() {
        logRecorder.reset();
    }

    public final void reset() {
        Logger.getRootLogger().removeHandler(this);
        resetLogContent();
    }

    @Override
    public String toString() {
        return getEncoding();
    }
}
