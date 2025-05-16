package ch.systemsx.cisd.openbis.dss.generic.shared;

import javax.sql.DataSource;

public interface IPathInfoDataSourceProvider
{

    DataSource getDataSource();

    boolean isDataSourceDefined();

}
