package ch.ethz.sis.afs.manager;

import ch.ethz.sis.afs.dto.Lock;
import ch.ethz.sis.shared.startup.Configuration;

public interface LockMapper<O, E>
{

    void init(Configuration configuration) throws Exception;

    Lock<O, E> mapLock(Lock<O, E> lock);

}
