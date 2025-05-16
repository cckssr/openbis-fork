/*
 * Copyright ETH 2015 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import ch.systemsx.cisd.common.properties.PropertyUtils;

public abstract class AbstractMultiDataSetArchiveCleaner implements IMultiDataSetArchiveCleaner
{
    public static final String FILE_PATH_PREFIXES_FOR_ASYNC_DELETION_KEY = "file-path-prefixes-for-async-deletion";

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            AbstractMultiDataSetArchiveCleaner.class);

    private final Properties properties;

    private final List<String> filePathPrefixesForAsyncDeletion;

    public AbstractMultiDataSetArchiveCleaner(Properties properties)
    {
        this.properties = properties;
        this.filePathPrefixesForAsyncDeletion = getPathPrefixesForAsyncDeletion(properties);
    }

    private List<String> getPathPrefixesForAsyncDeletion(Properties properties)
    {
        List<String> relativeFilePathPrefixesForAsyncDeletion = PropertyUtils.getList(properties, FILE_PATH_PREFIXES_FOR_ASYNC_DELETION_KEY);
        ArrayList<String> result = new ArrayList<String>();
        for (String path : relativeFilePathPrefixesForAsyncDeletion)
        {
            result.add(new File(path).getAbsolutePath());
        }
        return result;
    }

    @Override
    public void delete(File file)
    {
        if (isFileForAsyncDeletion(file))
        {
            deleteAsync(file);
        } else
        {
            deleteSync(file);
        }
    }

    protected abstract void deleteAsync(File file);

    private void deleteSync(File file)
    {
        if (file.delete())
        {
            operationLog.info("File immediately deleted: " + file);
        } else
        {
            operationLog.warn("Failed to delete file immediately: " + file);
        }
    }

    private boolean isFileForAsyncDeletion(File file)
    {
        String path = file.getAbsolutePath();
        for (String prefix : filePathPrefixesForAsyncDeletion)
        {
            if (path.startsWith(prefix))
            {
                return true;
            }
        }
        return false;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public List<String> getFilePathPrefixesForAsyncDeletion()
    {
        return filePathPrefixesForAsyncDeletion;
    }
}
