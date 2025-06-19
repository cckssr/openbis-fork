package ch.systemsx.cisd.common.logging.ext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class DailyRollingFileHandler extends Handler {

    public static final String DEFAULT_FILE_NAME_DATE_PATTERN = ".yyyy-MM-dd";
    public static final int DEFAULT_MAX_LOG_ROTATIONS = 7;
    public static final int DEFAULT_MAX_LOG_FILE_SIZE = -1;
    public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

    private final int maxLogFileSize;
    private final boolean append;
    private final String logFileName;
    private final Charset encoding;

    private File currentFile;
    private FileOutputStream outputStream;
    private final DateTimeFormatter dateFormatter;
    private final int maxLogRotations;
    private LocalDate currentDate;
    private long currentSize;

    private final ReentrantLock lock = new ReentrantLock();

    public DailyRollingFileHandler(String logFileName) throws IOException {
        this(logFileName, DEFAULT_MAX_LOG_FILE_SIZE, true, DEFAULT_FILE_NAME_DATE_PATTERN,
                DEFAULT_MAX_LOG_ROTATIONS, DEFAULT_ENCODING);
    }

    public DailyRollingFileHandler(String logFileName,
            int maxLogFileSize,
            boolean append,
            int maxLogRotations) throws IOException {
        this(logFileName, maxLogFileSize, append, DEFAULT_FILE_NAME_DATE_PATTERN, maxLogRotations, DEFAULT_ENCODING);
    }

    public DailyRollingFileHandler(String logFileName,
            int maxLogFileSize,
            boolean append,
            String datePattern,
            int maxLogRotations
            ) throws IOException {
        this(logFileName, maxLogFileSize, append, datePattern, maxLogRotations, DEFAULT_ENCODING);
    }

    public DailyRollingFileHandler(String logFileName,
            int maxLogFileSize,
            boolean append,
            String datePattern,
            int maxLogRotations,
            Charset encoding) throws IOException {
        this.logFileName = logFileName;
        this.maxLogFileSize = maxLogFileSize;
        this.append = append;
        this.dateFormatter = DateTimeFormatter.ofPattern(datePattern);
        this.currentDate = LocalDate.now();
        this.maxLogRotations = maxLogRotations;
        this.encoding = encoding;
        openActiveFile();
    }

    private void openActiveFile() throws IOException {
        currentFile = new File(logFileName);

        File parent = currentFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.isDirectory()) {
                throw new IOException("Failed to create parent directories for log file: " + parent);
            }
        }

        if (!append && currentFile.exists()) {
            if (!currentFile.delete()) {
                throw new IOException("Failed to delete existing log file: " + currentFile);
            }
        }
        outputStream = new FileOutputStream(currentFile, append);
        currentSize = currentFile.exists() ? currentFile.length() : 0;
    }

    private File datedFile(LocalDate date) {
        String suffix = date.format(dateFormatter);
        return new File(logFileName + suffix);
    }

    private void rotateByDate() throws IOException {
        if (maxLogRotations == 0) {
            truncateActive();
        } else {
            File archived = datedFile(currentDate);
            archiveAndTruncate(archived);
        }
        cleanupArchives();
        currentDate = LocalDate.now();
    }

    private void rotateBySize() throws IOException {
        if (maxLogRotations == 0) {
            truncateActive();
        } else {
            LocalDate date = currentDate;
            String suffix = date.format(dateFormatter);
            File parentDir = currentFile.getParentFile() != null ? currentFile.getParentFile() : new File(".");
            final String prefix = new File(logFileName).getName() + suffix + "_";
            int nextIndex = getNextIndex(parentDir, prefix);
            File archive = new File(parentDir, prefix + nextIndex);
            archiveAndTruncate(archive);
        }
        cleanupArchives();
    }

    private void archiveAndTruncate(File target) throws IOException {
        flush();
        //Files.copy(currentFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        copyFile(currentFile, target);
        truncateActive();
    }

    // handles large files
    public static void copyFile(File source, File destination) throws IOException {
        // Validate source file
        if (!source.exists() || !source.isFile()) {
            throw new FileNotFoundException("Source file not found or not a file: " + source.getAbsolutePath());
        }

        // Ensure parent directory exists
        File parentDir = destination.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create parent directories for: " + destination.getAbsolutePath());
            }
        }

        try (
                FileChannel sourceChannel = new FileInputStream(source).getChannel();
                FileChannel destChannel = new FileOutputStream(destination).getChannel()
        ) {
            long size = sourceChannel.size();
            long position = 0;
            long chunkSize = 32 * 1024 * 1024;

            while (position < size) {
                long transferred = sourceChannel.transferTo(position, chunkSize, destChannel);
                if (transferred <= 0) {
                    break;
                }
                position += transferred;
            }
        }
    }

    private void truncateActive() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(currentFile, "rw")) {
            raf.setLength(0L);
            currentSize = 0;
        } catch (IOException e) {
            reportError("Failed to truncate log file", e, ErrorManager.WRITE_FAILURE);
            throw e;
        }
    }

    private void cleanupArchives() {
        if (maxLogRotations < 0) {
            return;
        }

        File parentDir = currentFile.getParentFile() != null ? currentFile.getParentFile() : new File(".");
        String baseName = new File(logFileName).getName();

        File[] archives = parentDir.listFiles((dir, name) -> name.startsWith(baseName) && !name.equals(baseName));

        if (archives == null || archives.length <= maxLogRotations) {
            return;
        }

        Arrays.sort(archives, DailyRollingFileHandler::compareLogFiles);

        int deleteCount = archives.length - maxLogRotations;
        for (int i = 0; i < deleteCount; i++) {
            if (!archives[i].delete()) {
                reportError("Could not delete old log file: " + archives[i].getName(), null, ErrorManager.WRITE_FAILURE);
            }
        }
    }

    private static int compareLogFiles(File f1, File f2)
    {
        String name1 = f1.getName();
        String name2 = f2.getName();

        int i1_lastUnderscore = name1.lastIndexOf('_');
        int i2_lastUnderscore = name2.lastIndexOf('_');

        String base1 = name1;
        int index1 = DEFAULT_MAX_LOG_FILE_SIZE;
        if (i1_lastUnderscore != DEFAULT_MAX_LOG_FILE_SIZE) {
            try {
                index1 = Integer.parseInt(name1.substring(i1_lastUnderscore + 1));
                base1 = name1.substring(0, i1_lastUnderscore);
            } catch (NumberFormatException e) {
                // Not a numeric suffix, treat the whole name as the base.
            }
        }

        String base2 = name2;
        int index2 = DEFAULT_MAX_LOG_FILE_SIZE;
        if (i2_lastUnderscore != DEFAULT_MAX_LOG_FILE_SIZE) {
            try {
                index2 = Integer.parseInt(name2.substring(i2_lastUnderscore + 1));
                base2 = name2.substring(0, i2_lastUnderscore);
            } catch (NumberFormatException e) {
                // Not a numeric suffix.
            }
        }

        int baseCompare = base1.compareTo(base2);
        if (baseCompare != 0) {
            return baseCompare;
        }

        return Integer.compare(index1, index2);
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
        if (existing != null) {
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
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

        byte[] bytes = getMessageBytes(record);
        if (bytes == null){
            return;
        }

        lock.lock();
        try {
            handleLogRotation(bytes);

            outputStream.write(bytes);
            outputStream.flush();
            currentSize += bytes.length;
        } catch (IOException e) {
            reportError(null, e, ErrorManager.WRITE_FAILURE);
        } finally {
            lock.unlock();
        }
    }

    private void handleLogRotation(byte[] bytes) throws IOException
    {
        try
        {
            LocalDate now = LocalDate.now();
            // Daily rollover at midnight
            if (!now.equals(currentDate))
            {
                rotateByDate();
            }

            if (maxLogFileSize > 0 && currentSize + bytes.length > maxLogFileSize)
            {
                rotateBySize();
            }

        } catch (IOException e) {
            reportError("log rotation has issues : ", e, ErrorManager.WRITE_FAILURE);
        }
    }

    private byte[] getMessageBytes(LogRecord record)
    {
        byte[] bytes;

        try {
            String msg = getFormatter().format(record);
            bytes = msg.getBytes(encoding);
        } catch (Exception e) {
            reportError("Formatting error", e, ErrorManager.FORMAT_FAILURE);
            return null;
        }
        return bytes;
    }

    @Override
    public void flush() {
        lock.lock();
        try {
            if (outputStream != null) {
                outputStream.flush();
            }
        } catch (IOException e) {
            reportError(null, e, ErrorManager.FLUSH_FAILURE);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws SecurityException {
        lock.lock();
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            reportError(null, e, ErrorManager.CLOSE_FAILURE);
        } finally {
            lock.unlock();
        }
    }

    public String getLogFileName() { return logFileName; }

    public int getMaxLogFileSize() { return maxLogFileSize; }

    public boolean isAppend() { return append; }

    public Charset getEncodingUsed() { return encoding; }

    void setCurrentDate(LocalDate currentDate){
        this.currentDate =currentDate;
    }


}
