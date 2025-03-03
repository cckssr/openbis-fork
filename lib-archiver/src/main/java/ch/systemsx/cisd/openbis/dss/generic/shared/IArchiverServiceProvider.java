package ch.systemsx.cisd.openbis.dss.generic.shared;

import java.util.Properties;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.common.mail.IMailClient;

public interface IArchiverServiceProvider
{

    IConfigProvider getConfigProvider();

    IMailClient createEMailClient();

    IHierarchicalContentProvider getHierarchicalContentProvider();

    IDataSetDirectoryProvider getDataSetDirectoryProvider();

    IPathInfoDataSourceProvider getPathInfoDataSourceProvider();

    IDataSourceProvider getDataSourceProvider();

    IDataSetDeleter getDataSetDeleter();

    IShareIdManager getShareIdManager();

    IArchiverPlugin getArchiverPlugin();

    IArchiverTaskScheduler getArchiverTaskScheduler();

    Properties getArchiverProperties();

    IOpenBISService getOpenBISService();

    IApplicationServerApi getV3ApplicationService();

}
