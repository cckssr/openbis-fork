/*
 * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.dss.generic;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

import org.apache.log4j.Logger;

import ch.systemsx.cisd.common.filesystem.QueueingPathRemoverService;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import ch.systemsx.cisd.common.logging.LogInitializer;
import ch.systemsx.cisd.common.properties.ExtendedProperties;
import ch.systemsx.cisd.etlserver.ETLDaemon;
import ch.systemsx.cisd.etlserver.PathInfoServiceProviderFactory;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.HierarchicalContentServiceProviderFactory;
import ch.systemsx.cisd.openbis.common.spring.SpringEoDSQLExceptionTranslator;
import ch.systemsx.cisd.openbis.dss.BuildAndEnvironmentInfo;
import ch.systemsx.cisd.openbis.dss.generic.server.CommandQueueLister;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.HierarchicalContentServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.PathInfoServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.QueueingDataSetStatusUpdaterService;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProviderImpl;
import ch.systemsx.cisd.openbis.dss.generic.shared.ShufflingServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.ShufflingServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.utils.DssPropertyParametersUtil;
import ch.systemsx.cisd.openbis.dss.generic.shared.utils.RSyncConfig;

/**
 * Main class starting {@link ch.systemsx.cisd.openbis.dss.generic.server.DataStoreServer}, {@link ETLDaemon}.
 *
 * @author Franz-Josef Elmer
 */
public class DataStoreServer
{
    static
    {
        SpringEoDSQLExceptionTranslator.activate();
    }

    private static final Logger notificationLog =
            LogFactory.getLogger(LogCategory.NOTIFY, DataStoreServer.class);

    private static final UncaughtExceptionHandler loggingExceptionHandler =
            new UncaughtExceptionHandler()
            {

                //
                // UncaughtExceptionHandler
                //

                @Override
                public final void uncaughtException(final Thread t, final Throwable e)
                {
                    notificationLog.error("An exception has occurred [thread: '" + t.getName()
                            + "'].", e);
                }
            };

    private static void initLog()
    {
        LogInitializer.init();
        Thread.setDefaultUncaughtExceptionHandler(loggingExceptionHandler);
    }

    public static void main(String[] args)
    {
        if (args.length > 0 && args[0].equals("--version"))
        {
            System.err
                    .println("Data Store Server version " + BuildAndEnvironmentInfo.INSTANCE.getFullVersion());
            System.exit(0);
        }
        initLog();
        final boolean showShredder = (args.length > 0 && args[0].equals("--show-shredder"));
        if (showShredder)
        {
            ETLDaemon.listShredder();
            System.exit(0);
        }
        final boolean showUpdaterQueue =
                (args.length > 0 && args[0].equals("--show-updater-queue"));
        if (showUpdaterQueue)
        {
            ETLDaemon.listUpdaterQueue();
            System.exit(0);
        }
        final boolean showCommandQueue =
                (args.length > 0 && args[0].equals("--show-command-queue"));
        if (showCommandQueue)
        {
            CommandQueueLister.listQueuedCommand();
            System.exit(0);
        }

        ServiceProviderFactory.setInstance(new ServiceProviderImpl());
        ArchiverServiceProviderFactory.setInstance(new ArchiverServiceProvider());
        ShufflingServiceProviderFactory.setInstance(new ShufflingServiceProvider());
        HierarchicalContentServiceProviderFactory.setInstance(new HierarchicalContentServiceProvider());
        PathInfoServiceProviderFactory.setInstance(new PathInfoServiceProvider());

        ExtendedProperties props = DssPropertyParametersUtil.loadProperties(DssPropertyParametersUtil.SERVICE_PROPERTIES_FILE);
        File storeRootDir = DssPropertyParametersUtil.getStoreRootDir(props);
        List<String> rsyncOps = DssPropertyParametersUtil.getRsyncOptions(props);
        notificationLog.info("Rsync configured with additional options: " + rsyncOps);
        RSyncConfig.getInstance(rsyncOps);

        // Initialize the shredder and updater _before_ the DataSetCommandExecutor which uses them.
        QueueingPathRemoverService.start(storeRootDir, ETLDaemon.shredderQueueFile);
        QueueingDataSetStatusUpdaterService.start(ETLDaemon.updaterQueueFile);
        ch.systemsx.cisd.openbis.dss.generic.server.DataStoreServer.main(args);
        ETLDaemon.main(args);
    }

}
