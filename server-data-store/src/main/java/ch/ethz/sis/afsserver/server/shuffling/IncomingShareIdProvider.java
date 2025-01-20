/*
 * Copyright ETH 2011 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.afsserver.server.shuffling;

import java.util.Collections;
import java.util.Set;

import ch.ethz.sis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.shared.startup.Configuration;

/**
 * Provider of IDs of incoming shares. This is a helper class to avoid direct connection between ETLDaemon (which determines incoming share
 * IDs) and consumers of these IDs.
 *
 * @author Franz-Josef Elmer
 */
public class IncomingShareIdProvider
{

    private static volatile boolean initialized;

    private static Configuration configuration;

    private static Set<String> incomingShareIds;

    public static void configure(final Configuration configuration)
    {
        IncomingShareIdProvider.configuration = configuration;
    }

    public static Set<String> getIdsOfIncomingShares()
    {
        initialize();
        return Collections.unmodifiableSet(incomingShareIds);
    }

    private static void initialize()
    {
        // initialize lazily only to verify configuration properties if they are really needed

        if (!initialized)
        {
            synchronized (ServiceProvider.class)
            {
                if (!initialized)
                {
                    if (configuration == null)
                    {
                        throw new RuntimeException("Cannot initialize with null configuration");
                    }

                    incomingShareIds = Set.of(AtomicFileSystemServerParameterUtil.getStorageIncomingShareId(configuration).toString());
                    initialized = true;
                }
            }
        }
    }

}
