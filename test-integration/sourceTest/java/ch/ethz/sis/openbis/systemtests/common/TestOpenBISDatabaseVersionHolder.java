package ch.ethz.sis.openbis.systemtests.common;

import ch.systemsx.cisd.common.db.IDatabaseVersionHolder;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.DatabaseVersionHolder;

public class TestOpenBISDatabaseVersionHolder implements IDatabaseVersionHolder
{
    @Override public String getDatabaseVersion()
    {
        return DatabaseVersionHolder.getDatabaseVersion();
    }
}
