/*
 * Copyright ETH 2015 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.openbis.generic.shared.util;

import java.util.concurrent.atomic.AtomicLong;

import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventType;

public final class RuntimeCacheEventListener<K, V> implements CacheEventListener<K, V>
{

    private final Logger operationLog;
    private final AtomicLong entryCount;
    private PendingCreated pendingCreated;

    public RuntimeCacheEventListener(Logger operationLog, AtomicLong entryCount)
    {
        this.operationLog = operationLog;
        this.entryCount = entryCount;
    }

    @Override
    public void onEvent(CacheEvent<? extends K, ? extends V> event)
    {
        if (!operationLog.isInfoEnabled())
        {
            return;
        }

        Object val = event.getNewValue() != null ? event.getNewValue() : event.getOldValue();
        int identity = (val == null) ? 0 : System.identityHashCode(val);

        EventType type = event.getType();

        if (type == EventType.CREATED)
        {
            long size = entryCount.incrementAndGet();
            logMessage(identity, val, "put to the cache", size);
            pendingCreated = new PendingCreated(identity, String.valueOf(val));
            return;
        }

        if (type == EventType.UPDATED)
        {
            pendingCreated = null;
            return;
        }

        switch (type)
        {
            case EVICTED:
            {
                long afterEvict = entryCount.updateAndGet(prev -> prev > 0 ? prev - 1 : 0);
                long sizeForEvict = Math.max(0, afterEvict - (pendingCreated != null ? 1 : 0));
                logMessage(identity, val, "evicted from the cache", sizeForEvict);

                if (pendingCreated != null)
                {
                    logMessage(pendingCreated.identity, pendingCreated.valueDescription, "put to the cache",
                            afterEvict);
                    pendingCreated = null;
                }

                entryCount.set(afterEvict);
                return;
            }
            case REMOVED:
            {
                long size = entryCount.updateAndGet(prev -> prev > 0 ? prev - 1 : 0);
                pendingCreated = null;
                logMessage(identity, val, "removed from the cache", size);
                return;
            }
            case EXPIRED:
            {
                long size = entryCount.updateAndGet(prev -> prev > 0 ? prev - 1 : 0);
                pendingCreated = null;
                logMessage(identity, val, "expired from the cache", size);
                return;
            }
            default:
            {
                long size = entryCount.get();
                logMessage(identity, val, type.name().toLowerCase(), size);
            }
        }
    }

    private void logMessage(int identity, Object val, String action, long size)
    {
        String base = String.format(
                "Cache entry %d that contains %s has been %s.",
                identity, String.valueOf(val), action
        );
        String tail = " Cache now contains " + size + (size == 1 ? " entry." : " entries.");

        operationLog.info(base + tail);
    }

    private static final class PendingCreated
    {
        private final int identity;
        private final String valueDescription;

        private PendingCreated(int identity, String valueDescription)
        {
            this.identity = identity;
            this.valueDescription = valueDescription;
        }
    }
}
