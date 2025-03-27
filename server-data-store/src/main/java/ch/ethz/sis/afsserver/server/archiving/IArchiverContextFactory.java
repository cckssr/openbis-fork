package ch.ethz.sis.afsserver.server.archiving;

import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverTaskContext;

public interface IArchiverContextFactory
{
    ArchiverTaskContext createContext();
}
