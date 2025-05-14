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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.common.mail.IMailClientProvider;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import ch.systemsx.cisd.common.utilities.ITimeAndWaitingProvider;
import ch.systemsx.cisd.common.utilities.SystemTimeProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverServiceProviderFactory;

/**
 * Class doing clean ups of corrupted multi-data-set container files. Depending on the file path a container file is deleted immediately or later
 * (using a {@link FileDeleter}).
 *
 * @author Franz-Josef Elmer
 */
public class MultiDataSetArchiveCleaner extends AbstractMultiDataSetArchiveCleaner
{
    static final String DELETION_REQUESTS_DIR_KEY = "deletion-requests-dir";

    private static final Map<File, FileDeleter> globalDeleters = new HashMap<File, FileDeleter>();

    private File deletionRequestsDir;

    private final Map<File, FileDeleter> deleters;

    public MultiDataSetArchiveCleaner(Properties properties)
    {
        this(properties, SystemTimeProvider.SYSTEM_TIME_PROVIDER, globalDeleters);
    }

    MultiDataSetArchiveCleaner(Properties properties, ITimeAndWaitingProvider timeProvider, Map<File, FileDeleter> deleters)
    {
        super(properties);
        this.deleters = deleters;

        if (getFilePathPrefixesForAsyncDeletion().isEmpty())
        {
            return;
        }

        deletionRequestsDir = new File(PropertyUtils.getMandatoryProperty(properties, DELETION_REQUESTS_DIR_KEY));
        if (deletionRequestsDir.isFile())
        {
            throw new ConfigurationFailureException("Property '" + DELETION_REQUESTS_DIR_KEY
                    + "' denotes an existing file instead of a directory: " + deletionRequestsDir);
        }
        if (deletionRequestsDir.exists() == false)
        {
            if (deletionRequestsDir.mkdirs() == false)
            {
                throw new ConfigurationFailureException("Couldn't create directory: " + deletionRequestsDir);
            }
        }
        synchronized (deleters)
        {
            FileDeleter deleter = deleters.get(deletionRequestsDir);
            if (deleter == null)
            {
                deleter = new FileDeleter(deletionRequestsDir, timeProvider, new IMailClientProvider()
                {
                    @Override
                    public IMailClient getMailClient()
                    {
                        return ArchiverServiceProviderFactory.getInstance().createEMailClient();
                    }
                }, properties);
                deleters.put(deletionRequestsDir, deleter);
            }
            deleter.start();
        }
    }

    @Override protected void deleteAsync(final File file)
    {
        FileDeleter deleter = deleters.get(deletionRequestsDir);
        deleter.requestDeletion(file);
    }

}
