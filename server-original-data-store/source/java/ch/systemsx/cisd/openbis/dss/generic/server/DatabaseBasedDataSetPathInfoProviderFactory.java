package ch.systemsx.cisd.openbis.dss.generic.server;

import ch.systemsx.cisd.openbis.dss.generic.shared.utils.PathInfoDataSourceProvider;

public class DatabaseBasedDataSetPathInfoProviderFactory
{

    public DatabaseBasedDataSetPathInfoProvider createProvider()
    {
        return new DatabaseBasedDataSetPathInfoProvider(PathInfoDataSourceProvider::getDataSource);
    }
}
