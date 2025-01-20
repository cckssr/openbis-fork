package ch.ethz.sis.afsserver.server.common;

import java.util.List;
import java.util.UUID;

import ch.ethz.sis.afs.dto.LockType;

public interface ILockManager
{
    boolean lock(UUID owner, List<SimpleDataSetInformationDTO> dataSets, LockType lockType);

    boolean unlock(UUID owner, List<SimpleDataSetInformationDTO> dataSets, LockType lockType);
}
