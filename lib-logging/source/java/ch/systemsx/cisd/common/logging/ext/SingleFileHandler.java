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

public class SingleFileHandler extends AbstractFileHandler
{

    public SingleFileHandler(String logFileName,
            int maxLogFileSize,
            boolean append, boolean forceReInit) throws IOException
    {
        this(logFileName, maxLogFileSize, append, forceReInit, DEFAULT_ENCODING);
    }

    public SingleFileHandler(String logFileName,
            int maxLogFileSize,
            boolean append, boolean forceReInit,
            Charset encoding) throws IOException
    {
        super(logFileName, maxLogFileSize, append, encoding);
        openActiveFile(forceReInit);
    }

    private void openActiveFile(boolean forceReInit) throws IOException {
        currentFile = new File(logFileName);

        File parent = currentFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.isDirectory()) {
                throw new IOException("Failed to create parent directories for log file: " + parent);
            }
        }

        if (!append && currentFile.exists() && !forceReInit) {
            File[] existing = parent.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(currentFile.getName()) && name.substring(currentFile.getName().length()).matches("_\\d+");
                }
            });
            for(File file : existing)
            {
                if (!file.delete()) {
                    throw new IOException("Failed to delete existing log file: " + file);
                }
            }

            if (!currentFile.delete()) {
                throw new IOException("Failed to delete existing log file: " + currentFile);
            }
        }
        outputStream = new FileOutputStream(currentFile, append || forceReInit);
        currentSize = currentFile.exists() ? currentFile.length() : 0;
    }


    private void rotateBySize(byte[] bytes) throws IOException {
        File parentDir = currentFile.getParentFile() != null ? currentFile.getParentFile() : new File(".");
        final String prefix = new File(logFileName).getName() + "_";
        int nextIndex = getNextIndex(parentDir, prefix);
        File archive = new File(parentDir, prefix + nextIndex);
        String currentFilePath = currentFile.getPath();

        currentFile.renameTo(archive);
        close();

        currentFile = new File(currentFilePath);
        outputStream = new FileOutputStream(currentFile, false);
        currentSize = 0;

    }

    @Override
    public void customPublishLogic(byte[] bytes) throws IOException
    {
        if (maxLogFileSize > 0 && currentSize + bytes.length > maxLogFileSize)
        {
            rotateBySize(bytes);
        }

    }


}
