///*
// *  Copyright ETH 2025 Zürich, Scientific IT Services
// *
// *  Licensed under the Apache License, Version 2.0 (the "License");
// *  you may not use this file except in compliance with the License.
// *  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// *
// */
//
//package ch.systemsx.cisd.common.logging.log;
//
//import org.apache.log4j.Level;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.logging.Handler;
//import java.util.logging.LogRecord;
//import org.apache.log4j.Logger;
//import java.util.logging.Formatter;
//import java.util.logging.SimpleFormatter;
//import java.util.regex.Pattern;
//
///**
// * A java.util.logging.Handler that buffers log output in memory until requested.
// * Supports filtering by logger name and suppression patterns.
// */
//public final class BufferedAppender extends Handler {
//
//    private final ByteArrayOutputStream logRecorder = new ByteArrayOutputStream();
//    private final List<Pattern> suppressedPatterns = new ArrayList<>();
//    private final Pattern includePattern;
//    private final Formatter formatter;
//
//    public BufferedAppender() {
//        this(Level.DEBUG, null, new SimpleFormatter());
//    }
//
//    public BufferedAppender(Level level) {
//        this(level, null, new SimpleFormatter());
//    }
//
//    public BufferedAppender(Level level, String loggerNameRegex) {
//        this(level, loggerNameRegex, new SimpleFormatter());
//    }
//
//    public BufferedAppender(Level level, String loggerNameRegex, Formatter formatter) {
//        this.includePattern = loggerNameRegex != null ? Pattern.compile(loggerNameRegex) : null;
//        this.formatter = formatter;
//        setLevel(Logger.mapToJULLevel(level.toInt()));
//        Logger.getRootLogger().addHandler(this);
//    }
//
//    public void addRegexForLoggingEventsToBeDropped(String regex) {
//        suppressedPatterns.add(Pattern.compile(regex));
//    }
//
//    @Override
//    public void publish(LogRecord record) {
//        if (!isLoggable(record)) {
//            return;
//        }
//        String loggerName = record.getLoggerName();
//        if (includePattern != null && !includePattern.matcher(loggerName).matches()) {
//            return;
//        }
//        String message = formatter.format(record);
//        for (Pattern pattern : suppressedPatterns) {
//            if (pattern.matcher(message).find()) {
//                return;
//            }
//        }
//        try {
//            logRecorder.write(message.getBytes(StandardCharsets.UTF_8));
//        } catch (IOException ignored) {
//        }
//    }
//
//
//    public void append(LogRecord record) {
//        publish(record);
//    }
//
//    @Override
//    public void flush() {
//        try
//        {
//            logRecorder.flush();
//        } catch (IOException e)
//        {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Override
//    public void close() throws SecurityException {
//        try
//        {
//            logRecorder.close();
//        } catch (IOException e)
//        {
//            throw new SecurityException(e);
//        }
//    }
//
//    public String getLogContent() {
//        return new String(logRecorder.toByteArray(), StandardCharsets.UTF_8).trim();
//    }
//
//    public List<String> getLogLines() {
//        String content = getLogContent();
//        return content.isEmpty() ? List.of() : List.of(content.split("\\r?\\n"));
//    }
//
//    public void resetLogContent() {
//        logRecorder.reset();
//    }
//
//    public final void reset()
//    {
//        org.apache.log4j.Logger.getRootLogger().removeAppender(this);
//        super.reset();
//    }
//
//    @Override
//    public String toString() {
//        return getEncoding();
//    }
//}
