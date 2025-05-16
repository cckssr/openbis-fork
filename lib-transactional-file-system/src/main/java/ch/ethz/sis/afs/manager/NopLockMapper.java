package ch.ethz.sis.afs.manager;

import ch.ethz.sis.afs.dto.Lock;
import ch.ethz.sis.shared.startup.Configuration;

public class NopLockMapper<O, E> implements LockMapper<O, E>
{
    @Override public void init(final Configuration configuration) throws Exception
    {
    }

    @Override public Lock<O, E> mapLock(final Lock<O, E> lock)
    {
        return lock;
    }
}
