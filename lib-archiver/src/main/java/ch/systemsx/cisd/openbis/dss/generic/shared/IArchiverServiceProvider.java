package ch.systemsx.cisd.openbis.dss.generic.shared;

import java.util.Properties;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.IMultiDataSetArchiverDataSourceProvider;

public interface IArchiverServiceProvider
{

    IConfigProvider getConfigProvider();

    IMailClient createEMailClient();

    IHierarchicalContentProvider getHierarchicalContentProvider();

    IDataSetDirectoryProvider getDataSetDirectoryProvider();

    IDataSetPathInfoProvider getDataSetPathInfoProvider();

    IPathInfoDataSourceProvider getPathInfoDataSourceProvider();

    IMultiDataSetArchiverDataSourceProvider getMultiDataSetArchiverDataSourceProvider();

    IDataSetDeleter getDataSetDeleter();

    IShareIdManager getShareIdManager();

    IArchiverPlugin getArchiverPlugin();

    IArchiverTaskScheduler getArchiverTaskScheduler();

    Properties getArchiverProperties();

    IOpenBISService getOpenBISService();

    IApplicationServerApi getV3ApplicationService();

}
