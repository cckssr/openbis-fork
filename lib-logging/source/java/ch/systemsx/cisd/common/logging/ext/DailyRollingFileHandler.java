package ch.systemsx.cisd.common.logging.ext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Custom JUL handler with daily rollover (date suffix on rollover) and size-based rotation.
 * Active log file remains at basePattern without date; rotated files get date suffix or date+index for size.
 */
public class DailyRollingFileHandler extends Handler {
    private final int maxLogFileSize;       // max bytes per file before size-based rotation
    private final boolean append;
    private final String logFileNamePattern; // base filename, e.g. "logs/openbis_log.txt"

    private File currentFile;
    private FileOutputStream outputStream;

    private final DateTimeFormatter dateFormatter;
    private LocalDate currentDate;


    public DailyRollingFileHandler(String logFileNamePattern) throws IOException {
        this(logFileNamePattern, Integer.MAX_VALUE, true, ".yyyy-MM-dd");
    }

    /**
     * @param logFileNamePattern Base file name (active file name), e.g. "logs/openbis_log.txt"
     * @param maxLogFileSize   Maximum bytes per file before size-based rotation
     * @param append  Whether to append to existing file on startup
     */
    public DailyRollingFileHandler(String logFileNamePattern,
            int maxLogFileSize,
            boolean append) throws IOException {
        this(logFileNamePattern, maxLogFileSize, append, ".yyyy-MM-dd");
    }

    /**
     * Full constructor allowing custom date pattern for rollover.
     */
    public DailyRollingFileHandler(String logFileNamePattern,
            int maxLogFileSize,
            boolean append,
            String datePattern) throws IOException {
        this.logFileNamePattern = logFileNamePattern;
        this.maxLogFileSize = maxLogFileSize;
        this.append = append;
        this.dateFormatter = DateTimeFormatter.ofPattern(datePattern);
        this.currentDate = LocalDate.now();
        openActiveFile();
    }

    // Opens the active log file at basePattern
    private void openActiveFile() throws IOException {
        currentFile = new File(logFileNamePattern);
        if (!append && currentFile.exists()) {
            currentFile.delete();
        }
        File parent = currentFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        outputStream = new FileOutputStream(currentFile, append);
    }

    // Generates filename for a given date: basePattern.DATE
    private File datedFile(LocalDate date) {
        String suffix = date.format(dateFormatter);
        return new File(logFileNamePattern + suffix);
    }

    // Rollover active file by renaming to date suffix
    private void rotateByDate() throws IOException {
        outputStream.close();
        File renamed = datedFile(currentDate);
        if (!currentFile.renameTo(renamed)) {
            throw new IOException("Failed to rollover " + currentFile + " to " + renamed);
        }
        openActiveFile();
        currentDate = LocalDate.now();
    }

    // Size-based rotation on active file with ".1, .2 .. " index
    private void rotateBySize() throws IOException {
        outputStream.close();
        LocalDate date = currentDate;
        String suffix = date.format(dateFormatter);
        File parentDir = currentFile.getParentFile() != null ? currentFile.getParentFile() : new File(".");
        final String prefix = new File(logFileNamePattern).getName() + suffix + ".";
        int nextIndex = getNextIndex(parentDir, prefix);
        File rotated = new File(parentDir, new File(logFileNamePattern).getName() + suffix + "." + nextIndex);
        if (!currentFile.renameTo(rotated)) {
            throw new IOException("Failed to rename " + currentFile + " to " + rotated);
        }
        openActiveFile();
    }

    private static int getNextIndex(File parentDir, String prefix)
    {
        File[] existing = parentDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(prefix) && name.substring(prefix.length()).matches("\\d+");
            }
        });
        int nextIndex = 1;
        if (existing != null && existing.length > 0) {
            for (File f : existing) {
                try {
                    int idx = Integer.parseInt(f.getName().substring(prefix.length()));
                    if (idx >= nextIndex) {
                        nextIndex = idx + 1;
                    }
                } catch (NumberFormatException e) {
                    // skip
                }
            }
        }
        return nextIndex;
    }

    @Override
    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        try {
            LocalDate now = LocalDate.now();
            // Daily rollover at midnight
            if (!now.equals(currentDate)) {
                rotateByDate();
            }
            String msg = getFormatter().format(record);
            byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
            // Size-based rollover
            if (currentFile.length() + bytes.length > maxLogFileSize) {
                rotateBySize();
            }
            outputStream.write(bytes);
            outputStream.flush();
        } catch (IOException e) {
            reportError(null, e, ErrorManager.WRITE_FAILURE);
        }
    }

    @Override
    public void flush() {
        try {
            if (outputStream != null) {
                outputStream.flush();
            }
        } catch (IOException e) {
            reportError(null, e, ErrorManager.FLUSH_FAILURE);
        }
    }

    @Override
    public void close() throws SecurityException {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            reportError(null, e, ErrorManager.CLOSE_FAILURE);
        }
    }

    /** File pattern, e.g. "logs/openbis_auth_log.txt" */
    public String getLogFileNamePattern() {
        return logFileNamePattern;
    }

    /** Max size in bytes before rotation, e.g. 10485760 */
    public int getMaxLogFileSize() {
        return maxLogFileSize;
    }

    /** True if appending to existing files */
    public boolean isAppend() {
        return append;
    }
}
