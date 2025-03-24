package ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver;

import javax.sql.DataSource;

public interface IMultiDataSetArchiverDataSourceProvider
{

    DataSource getDataSource();

}
