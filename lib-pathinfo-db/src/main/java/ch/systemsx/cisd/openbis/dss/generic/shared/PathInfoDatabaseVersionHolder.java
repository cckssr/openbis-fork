package ch.systemsx.cisd.openbis.dss.generic.shared;

import ch.systemsx.cisd.common.db.IDatabaseVersionHolder;

public class PathInfoDatabaseVersionHolder implements IDatabaseVersionHolder
{
    @Override public String getDatabaseVersion()
    {
        return "010";
    }
}
