package ch.ethz.sis.pathinfo;

import ch.systemsx.cisd.common.db.IDatabaseVersionHolder;

public class PathInfoDatabaseVersionHolder implements IDatabaseVersionHolder
{
    @Override public String getDatabaseVersion()
    {
        return "010";
    }
}
