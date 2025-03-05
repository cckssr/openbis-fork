/*
 * Copyright ETH 2013 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.springframework.beans.factory.BeanFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.base.tests.AbstractFileSystemTestCase;
import ch.systemsx.cisd.common.logging.BufferedAppender;
import ch.systemsx.cisd.common.logging.LogInitializer;
import ch.systemsx.cisd.common.logging.LogRecordingUtils;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverTaskContext;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverPlugin;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverServiceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverTaskScheduler;
import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDeleter;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetStatusUpdater;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.dss.generic.shared.IPathInfoDataSourceProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareFinder;
import ch.systemsx.cisd.openbis.dss.generic.shared.IShareIdManager;
import ch.systemsx.cisd.openbis.dss.generic.shared.IUnarchivingPreparation;
import ch.systemsx.cisd.openbis.dss.generic.shared.IncomingShareIdProviderTestWrapper;
import ch.systemsx.cisd.openbis.dss.generic.shared.utils.Share;
import ch.systemsx.cisd.openbis.generic.shared.dto.SimpleDataSetInformationDTO;

/**
 * @author Franz-Josef Elmer
 */
public abstract class AbstractArchiverTestCase extends AbstractFileSystemTestCase
{
    protected static final String DATA_STORE_CODE = "dss1";

    public static final class ShareFinder implements IShareFinder
    {
        static Properties properties;

        static SimpleDataSetInformationDTO recordedDataSet;

        static List<Share> recordedShares;

        private boolean alwaysReturnNull = false;

        public ShareFinder(Properties properties)
        {
            ShareFinder.properties = properties;
            if (properties.containsKey("alwaysReturnNull"))
            {
                this.alwaysReturnNull = true;
            }
        }

        @Override
        public Share tryToFindShare(SimpleDataSetInformationDTO dataSet, List<Share> shares)
        {
            ShareFinder.recordedDataSet = dataSet;
            ShareFinder.recordedShares = shares;
            if (shares.isEmpty() || alwaysReturnNull)
            {
                return null;
            } else
            {
                return shares.get(0);
            }
        }
    }

    protected BufferedAppender logRecorder;

    protected Mockery context;

    protected IDataSetDirectoryProvider dataSetDirectoryProvider;

    protected ArchiverTaskContext archiverTaskContext;

    protected IDataSetStatusUpdater statusUpdater;

    protected Properties properties;

    private BeanFactory beanFactory;

    protected IConfigProvider configProvider;

    protected IOpenBISService service;

    protected IShareIdManager shareIdManager;

    protected File store;

    protected File share1;

    protected IDataSetDeleter deleter;

    protected IHierarchicalContentProvider contentProvider;

    protected IDataSetFileOperationsManager fileOperationsManager;

    protected IDataSetFileOperationsManagerFactory fileOperationsManagerFactory;

    protected IUnarchivingPreparation unarchivingPreparation;

    private IArchiverServiceProvider originalServiceProvider;

    public AbstractArchiverTestCase()
    {
    }

    public AbstractArchiverTestCase(boolean cleanAfterMethod)
    {
        super(cleanAfterMethod);
    }

    @BeforeMethod
    public void beforeMethod(Method method)
    {
        System.out.println(">>>>>> set up for " + method.getName() + " " + Arrays.asList(workingDirectory.list()));
        LogInitializer.init();
        logRecorder = LogRecordingUtils.createRecorder("%-5p %c - %m%n", Level.DEBUG);
        context = new Mockery();
        fileOperationsManager = context.mock(IDataSetFileOperationsManager.class);
        dataSetDirectoryProvider = context.mock(IDataSetDirectoryProvider.class);
        contentProvider = context.mock(IHierarchicalContentProvider.class);
        unarchivingPreparation = context.mock(IUnarchivingPreparation.class);
        statusUpdater = context.mock(IDataSetStatusUpdater.class);
        configProvider = context.mock(IConfigProvider.class);
        service = context.mock(IOpenBISService.class);
        shareIdManager = context.mock(IShareIdManager.class);
        deleter = context.mock(IDataSetDeleter.class);
        fileOperationsManagerFactory = context.mock(IDataSetFileOperationsManagerFactory.class);

        originalServiceProvider = ArchiverServiceProviderFactory.getInstance();
        ArchiverServiceProviderFactory.setInstance(new IArchiverServiceProvider()
        {
            @Override public IConfigProvider getConfigProvider()
            {
                return configProvider;
            }

            @Override public IMailClient createEMailClient()
            {
                return null;
            }

            @Override public IHierarchicalContentProvider getHierarchicalContentProvider()
            {
                return contentProvider;
            }

            @Override public IDataSetDirectoryProvider getDataSetDirectoryProvider()
            {
                return dataSetDirectoryProvider;
            }

            @Override public IPathInfoDataSourceProvider getPathInfoDataSourceProvider()
            {
                return null;
            }

            @Override public IDataSourceProvider getDataSourceProvider()
            {
                return null;
            }

            @Override public IDataSetDeleter getDataSetDeleter()
            {
                return deleter;
            }

            @Override public IShareIdManager getShareIdManager()
            {
                return shareIdManager;
            }

            @Override public IArchiverPlugin getArchiverPlugin()
            {
                return null;
            }

            @Override public IArchiverTaskScheduler getArchiverTaskScheduler()
            {
                return null;
            }

            @Override public Properties getArchiverProperties()
            {
                return null;
            }

            @Override public IOpenBISService getOpenBISService()
            {
                return service;
            }

            @Override public IApplicationServerApi getV3ApplicationService()
            {
                return null;
            }
        });
        context.checking(new Expectations()
        {
            {
                allowing(dataSetDirectoryProvider).getStoreRoot();
                will(returnValue(store));

                allowing(dataSetDirectoryProvider).getShareIdManager();
                will(returnValue(shareIdManager));

                allowing(fileOperationsManagerFactory).create();
                will(returnValue(fileOperationsManager));

                allowing(shareIdManager).cleanupLocks();
            }
        });

        IncomingShareIdProviderTestWrapper.setShareIds(Arrays.asList("1"));
        store = new File(workingDirectory, "store");
        store.mkdirs();
        share1 = new File(store, "1");
        share1.mkdir();
        archiverTaskContext = new ArchiverTaskContext(dataSetDirectoryProvider, contentProvider);
        properties = new Properties();
    }

    @AfterMethod
    public void afterMethod(Method method)
    {
        System.out.println("======= Log content for " + method.getName() + "():");
        System.out.println(logRecorder.getLogContent());
        System.out.println("======================");
        logRecorder.reset();
        ArchiverServiceProviderFactory.setInstance(originalServiceProvider);
        IncomingShareIdProviderTestWrapper.restoreOriginalShareIds();
        try
        {
            context.assertIsSatisfied();
        } catch (Throwable t)
        {
            // assert expectations were met, including the name of the failed method
            throw new Error(method.getName() + "() : ", t);
        }
    }

}