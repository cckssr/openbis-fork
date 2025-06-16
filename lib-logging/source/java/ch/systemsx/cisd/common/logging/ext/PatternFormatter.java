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

package ch.systemsx.cisd.common.logging.ext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternFormatter extends Formatter {

    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";

    public static final Pattern LOGGER_PATTERN = Pattern.compile("%logger(?:\\{(\\d+)})?");

    public static final Pattern DATE_PATTERN = Pattern.compile("%d(?:\\{([^}]+)})?");

    public static final Pattern LEVEL_PATTERN = Pattern.compile("%(-?\\d+)?(p|level)");

    private final String messagePattern;

    /**
     * Constructs a PatternFormatter with the given pattern.
     * If the pattern includes a date token with a custom format, you'll need to
     * extend the logic to extract and use that format. Here, we use a default date format.
     *
     * @param messagePattern The pattern string (e.g. "%d %-5p [%t] %c - %m%n")
     */
    public PatternFormatter(String messagePattern) {
        this.messagePattern = messagePattern;
    }

    @Override
    public String format(LogRecord record) {
        String formatted = messagePattern;

        if(formatted == null || formatted.isEmpty()){
            return record.getMessage();
        }

        // Process date tokens: supports both %d and %d{customFormat}
        formatted = formatDate(record, formatted);

        formatted = formatLevel(record, formatted);

        // Replace logger name token (%c) with the logger name.
        if (formatted.contains("%c")) {
            formatted = formatted.replace("%c", record.getLoggerName());
        }
        if (formatted.contains("%logger")) {
            formatted = formatLogger(record, formatted);
        }

        // Replace thread name token (%t) with the thread name.
        if (formatted.contains("%t")) {
            formatted = formatThreadName(record, formatted);
        }

        if (formatted.contains("%msg")) {
            formatted = formatted.replace("%msg", record.getMessage() != null ? formatMessage(record) : "");
        }

        // Replace message token (%m) with the formatted log message.
        if (formatted.contains("%m")) {
            formatted = formatted.replace("%m", record.getMessage() != null ? formatMessage(record) : "");
        }

        // Replace newline token (%n) with the system's line separator.
        if (formatted.contains("%n")) {
            formatted = formatted.replace("%n", System.lineSeparator());
        }

        formatted = formatExceptionIfAvailable(record, formatted);

        return formatted;
    }

    private static String formatExceptionIfAvailable(LogRecord record, String formatted)
    {
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            formatted += sw.toString();
        }
        return formatted;
    }

    private static String formatThreadName(LogRecord record, String formatted)
    {
        String threadName;
        if (record instanceof ExtendedLogRecord) {
            threadName = ((ExtendedLogRecord) record).getThreadName();
        } else {
            threadName = Thread.currentThread().getName();
        }
        formatted = formatted.replace("%t", threadName);
        return formatted;
    }

    private static String formatLogger(LogRecord record, String formatted)
    {
        Matcher loggerMatcher = LOGGER_PATTERN.matcher(formatted);
        StringBuilder loggerBuffer = new StringBuilder();
        while (loggerMatcher.find()) {
            String loggerName = getLoggerName(record, loggerMatcher);
            loggerMatcher.appendReplacement(loggerBuffer, Matcher.quoteReplacement(loggerName));
        }
        loggerMatcher.appendTail(loggerBuffer);
        formatted = loggerBuffer.toString();
        return formatted;
    }

    private static String getLoggerName(LogRecord record, Matcher loggerMatcher)
    {
        String widthStr = loggerMatcher.group(1);
        String loggerName = record.getLoggerName();
        if (widthStr != null) {
            try {
                int width = Integer.parseInt(widthStr);
                if (loggerName.length() > width) {
                    loggerName = loggerName.substring(loggerName.length() - width);
                }
            } catch (NumberFormatException e) {
                // If parsing fails, just use the full logger name.
            }
        }
        return loggerName;
    }

    private String formatDate(LogRecord record, String formatted)
    {
        Matcher dateMatcher = DATE_PATTERN.matcher(formatted);
        StringBuilder dateBuffer = new StringBuilder();
        while (dateMatcher.find()) {
            String dateFormatStr = dateMatcher.group(1);
            SimpleDateFormat sdf = new SimpleDateFormat(
                    Objects.requireNonNullElse(dateFormatStr, DEFAULT_DATE_PATTERN));
            String dateStr = sdf.format(new Date(record.getMillis()));
            dateMatcher.appendReplacement(dateBuffer, Matcher.quoteReplacement(dateStr));
        }
        dateMatcher.appendTail(dateBuffer);
        formatted = dateBuffer.toString();
        return formatted;
    }

    private static String formatLevel(LogRecord record, String formatted)
    {
        Matcher levelMatcher = LEVEL_PATTERN.matcher(formatted);
        StringBuilder levelBuffer = new StringBuilder();
        while (levelMatcher.find()) {
            String widthStr = levelMatcher.group(1);
            int width = 0;
            boolean leftAlign = false;
            if (widthStr != null) {
                if (widthStr.startsWith("-")) {
                    leftAlign = true;
                    widthStr = widthStr.substring(1);
                }
                try {
                    width = Integer.parseInt(widthStr);
                } catch (NumberFormatException e) {
                    // do nothing
                }
            }
            String level = LoggingUtils.mapFromJUL(record.getLevel()).getName();
            //String level = record.getLevel().getName();
            String replacement;
            if (width > 0) {
                if (leftAlign) {
                    replacement = String.format("%-" + width + "s", level);
                } else {
                    replacement = String.format("%" + width + "s", level);
                }
            } else {
                replacement = level;
            }
            // Ensure any special characters in the replacement are escaped.
            levelMatcher.appendReplacement(levelBuffer, Matcher.quoteReplacement(replacement));
        }
        levelMatcher.appendTail(levelBuffer);
        formatted = levelBuffer.toString();
        return formatted;
    }
}
