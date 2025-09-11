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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public abstract class AbstractFileHandler extends Handler
{
    public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;
    public static final int DEFAULT_MAX_LOG_FILE_SIZE = -1;

    protected final int maxLogFileSize;
    protected final boolean append;
    protected final String logFileName;
    protected long currentSize;
    protected File currentFile;
    protected FileOutputStream outputStream;
    protected final Charset encoding;
    protected final ReentrantLock lock = new ReentrantLock();

    public AbstractFileHandler(String logFileName,
            int maxLogFileSize,
            boolean append) {
        this(logFileName, maxLogFileSize, append, DEFAULT_ENCODING);
    }

    public AbstractFileHandler(String logFileName,
            int maxLogFileSize,
            boolean append,
            Charset encoding) {
        this.logFileName = logFileName;
        this.maxLogFileSize = maxLogFileSize;
        this.append = append;
        this.encoding = encoding;
    }

    protected byte[] getMessageBytes(LogRecord record)
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

    protected static int getNextIndex(File parentDir, String prefix)
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

    public abstract void customPublishLogic(byte[] bytes) throws IOException;

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
            customPublishLogic(bytes);

            outputStream.write(bytes);
            outputStream.flush();
            currentSize += bytes.length;
        } catch (IOException e) {
            reportError(null, e, ErrorManager.WRITE_FAILURE);
        } finally {
            lock.unlock();
        }
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

}
