package ch.systemsx.cisd.openbis.dss.generic.shared;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;

public interface IServiceProvider
{
    IOpenBISService getOpenBISService();

    IConfigProvider getConfigProvider();

    IShareIdManager getShareIdManager();

    IDataSetDirectoryProvider getDataSetDirectoryProvider();

    IDataSetDeleter getDataSetDeleter();

    IApplicationServerApi getV3ApplicationService();

    IIncomingShareIdProvider getIncomingShareIdProvider();
}
