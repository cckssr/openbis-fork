package ch.ethz.sis.openbis.afsserver.server.archiving;

import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverTaskContext;

public interface IArchiverContextFactory
{
    ArchiverTaskContext createContext();
}
