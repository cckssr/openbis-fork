package ch.systemsx.cisd.openbis.dss.generic.shared;

import java.util.Properties;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.IMultiDataSetArchiveCleaner;

public interface IArchiverServiceProvider
{

    IConfigProvider getConfigProvider();

    IMailClient createEMailClient();

    IHierarchicalContentProvider getHierarchicalContentProvider();

    IIncomingShareIdProvider getIncomingShareIdProvider();

    IDataSetDirectoryProvider getDataSetDirectoryProvider();

    IDataSetPathInfoProvider getDataSetPathInfoProvider();

    IPathInfoDataSourceProvider getPathInfoDataSourceProvider();

    IArchiverDataSourceProvider getArchiverDataSourceProvider();

    IDataSetDeleter getDataSetDeleter();

    IDataSetStatusUpdater getDataSetStatusUpdater();

    IMultiDataSetArchiveCleaner getDataSetArchiveCleaner(Properties properties);

    IShareIdManager getShareIdManager();

    IArchiverPlugin getArchiverPlugin();

    IArchiverTaskScheduler getArchiverTaskScheduler();

    Properties getArchiverProperties();

    IOpenBISService getOpenBISService();

    IApplicationServerApi getV3ApplicationService();

}
