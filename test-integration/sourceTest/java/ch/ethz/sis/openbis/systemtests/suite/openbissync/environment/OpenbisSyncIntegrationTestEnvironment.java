/*
 * Copyright ETH 2024 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.systemtests.suite.openbissync.environment;

import java.io.File;
import java.nio.file.Path;

import ch.ethz.sis.openbis.systemtests.environment.ForkedOpenbisInstance;
import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestEnvironment;
import ch.systemsx.cisd.common.filesystem.SoftLinkMaker;

/**
 * Environment for the openbis-sync round-trip suite, using two independent openBIS instances:
 *
 */
public final class OpenbisSyncIntegrationTestEnvironment
{

    public static final String INSTANCE_ADMIN = "admin";

    public static final String PASSWORD = "password";

    public static final String DATA_SOURCE_ALIAS = "DS1";

    public static final String NAME_PREFIX = DATA_SOURCE_ALIAS + "_";

    public static final String SOURCE_PROJECT_NAME = "test-integration-fork";

    public static final String SOURCE_LOG_LABEL = "test-integration-sync-source";

    private static final File SYNC_PLUGIN_SOURCE = new File("../core-plugin-openbis/dist/core-plugins/openbis-sync");

    private static final File HARVESTER_SYNC_PLUGIN_LINK = new File("etc/suite/openbis-sync/dss/core-plugins/openbis-sync");

    private static final File SOURCE_SYNC_PLUGIN_LINK = new File("etc/suite/openbis-sync/source/dss/core-plugins/openbis-sync");

    public static IntegrationTestEnvironment environment;

    public static ForkedOpenbisInstance source;

    public static void start()
    {
        if (environment != null)
        {
            return;
        }

        try
        {
            HARVESTER_SYNC_PLUGIN_LINK.getParentFile().mkdirs();
            SOURCE_SYNC_PLUGIN_LINK.getParentFile().mkdirs();
            SoftLinkMaker.createSymbolicLink(SYNC_PLUGIN_SOURCE, HARVESTER_SYNC_PLUGIN_LINK);
            SoftLinkMaker.createSymbolicLink(SYNC_PLUGIN_SOURCE, SOURCE_SYNC_PLUGIN_LINK);

            new File("targets/source/incoming-default").mkdirs();
            source = new ForkedOpenbisInstance(SOURCE_PROJECT_NAME)
                    .withLabel(SOURCE_LOG_LABEL)
                    .withApplicationServer("etc/suite/openbis-sync/source/as/service.properties")
                    .withDataStoreServer("etc/suite/openbis-sync/source/dss/service.properties");
            source.start();

            // Bring up the in-JVM HARVESTER instance.
            environment = new IntegrationTestEnvironment();
            environment.createApplicationServer();
            new File("targets/openbis-sync/datastore_commandqueue").mkdirs();
            // avoid errors in logs
            new File("targets/incoming-default").mkdirs();
            environment.createDataStoreServer(
                    IntegrationTestEnvironment.loadProperties(Path.of("etc/suite/openbis-sync/dss/service.properties")));
            environment.start();
        } catch (RuntimeException | Error e)
        {
            stop();
            throw e;
        }
    }

    public static void stop()
    {
        RuntimeException failure = null;
        if (environment != null)
        {
            try
            {
                environment.stop();
            } catch (RuntimeException e)
            {
                failure = e;
            } finally
            {
                environment = null;
            }
        }
        if (source != null)
        {
            try
            {
                source.stop();
            } catch (RuntimeException e)
            {
                if (failure == null)
                {
                    failure = e;
                } else
                {
                    failure.addSuppressed(e);
                }
            } finally
            {
                source = null;
            }
        }
        if (failure != null)
        {
            throw failure;
        }
    }

    private OpenbisSyncIntegrationTestEnvironment()
    {
    }

}
