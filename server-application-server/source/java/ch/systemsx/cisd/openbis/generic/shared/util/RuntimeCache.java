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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder;

import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import org.ehcache.config.units.MemoryUnit;
import org.ehcache.event.EventType;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.systemsx.cisd.common.parser.MemorySizeFormatter;
import ch.systemsx.cisd.common.parser.PercentFormatter;

/**
 * @author Franz-Josef Elmer
 */
public class RuntimeCache<K, V>
{
    private final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, RuntimeCache.class);

    private final CacheManager cacheManager;

    private final String name;

    private final String cacheSizePropertyName;

    private final Class<K> keyType;

    private Cache<K, ValueWrapper<V>> cache;
    private final AtomicLong entryCount = new AtomicLong();

    public RuntimeCache(CacheManager cacheManager, String name, String cacheSizePropertyName, Class<K> keyType)
    {
        this.cacheManager = cacheManager;
        this.name = name;
        this.cacheSizePropertyName = cacheSizePropertyName;
        this.keyType = keyType;
    }

    @SuppressWarnings("unchecked")
    public V get(K key)
    {
        ensureCacheInitialized();
        ValueWrapper<V> vw = cache.get(key);
        return vw == null ? null : vw.getValue();
    }

    public void put(K key, V value)
    {
        ensureCacheInitialized();
        // In Ehcache 3, put is safe for updates; eviction/size are handled internally
        cache.put(key, new ValueWrapper<>(value));
    }

    public boolean remove(K key) {
        ensureCacheInitialized();
        ValueWrapper<V> current = cache.get(key);
        if (current == null) {
            return false;
        }
        return cache.remove(key, current);
    }

    public List<K> getKeys()
    {
        ensureCacheInitialized();
        List<K> keys = new ArrayList<>();
        cache.forEach(entry -> keys.add(entry.getKey()));
        return keys;
    }

    public List<V> getValues()
    {
        ensureCacheInitialized();
        List<V> values = new ArrayList<>();
        cache.forEach(entry -> {
            ValueWrapper<V> vw = entry.getValue();
            values.add(vw == null ? null : vw.getValue());
        });
        return values;
    }

    /**
     * Initializes the cache if it doesn't exist.
     * In Ehcache 3, caches are created via the CacheManager with a typed configuration.
     */
    public synchronized void initCache()
    {
        if (cache != null) return;

        Cache<K, ValueWrapper<V>> existing =
                cacheManager.getCache(name, keyType, (Class<ValueWrapper<V>>) (Class<?>) ValueWrapper.class);

        if (existing != null)
        {
            operationLog.info("The cache " + name + " already exists (Ehcache 3). Using the configured cache.");
            cache = existing;
            entryCount.set(0);
            existing.forEach(entry -> entryCount.incrementAndGet());
            return;
        }

        operationLog.info("Creating the cache: " + name);

        long heapBytes = getCacheSize();

//        // Optional: event listener to mirror previous logging on updates/puts/removes
//        CacheEventListenerConfigurationBuilder listenerCfg =
//                CacheEventListenerConfigurationBuilder
//                        .newEventListenerConfiguration(
//                                (event) -> {
//                                    // Keep it lightweight; you can adapt to your RuntimeCacheEventListenerFactory, if ported.
//                                    operationLog.debug("Cache event [" + name + "]: " + event.getType()
//                                            + " key=" + event.getKey());
//                                    RuntimeCacheEventListenerFactory.getListener().notifyElementUpdated(getCache(), event.getNewValue());
//                                },
//                                EventType.CREATED, EventType.UPDATED, EventType.REMOVED, EventType.EXPIRED)
//                        .unordered()
//                        .asynchronous();

        //synchronous makes sure that cache operations return only after done, like put
        CacheEventListenerConfigurationBuilder listenerCfg =
                CacheEventListenerConfigurationBuilder
                        .newEventListenerConfiguration(
                                new RuntimeCacheEventListener<K, ValueWrapper<V>>(operationLog, entryCount),
                                EventType.CREATED, EventType.UPDATED, EventType.REMOVED, EventType.EXPIRED, EventType.EVICTED)
                        .unordered()
                        .synchronous();

        CacheConfiguration<K, ValueWrapper<V>> cacheConfig =
                CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(
                                keyType,
                                (Class<ValueWrapper<V>>) (Class<?>) ValueWrapper.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(heapBytes, MemoryUnit.B))
                        // Ehcache 3 uses ExpiryPolicy for TTI/TTL. Here we set TTI=3600s and no TTL limit.
                        .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofSeconds(3600)))
                        .withSizeOfMaxObjectGraph(10_000_000L)          // replaces maxDepth
                        .withSizeOfMaxObjectSize(Long.MAX_VALUE, MemoryUnit.B)
                        .add(listenerCfg)
                        .build();

        cache = cacheManager.createCache(name, cacheConfig);
    }

    protected long getCacheSize()
    {
        String propertyValue = getSystemProperty(cacheSizePropertyName);

        if (propertyValue == null || propertyValue.trim().length() == 0)
        {
            return getCacheDefaultSize();
        } else
        {
            try
            {
                long cacheSize = MemorySizeFormatter.parse(propertyValue);
                operationLog.info("Cache size was set to '" + propertyValue + "' in '" + cacheSizePropertyName + "' system property.");
                return cacheSize;
            } catch (IllegalArgumentException e1)
            {
                try
                {
                    int cachePercent = PercentFormatter.parse(propertyValue);
                    long cacheSize = (long) ((cachePercent / 100.0) * getMemorySize());
                    operationLog.info("Cache size was set to '" + propertyValue + "' in '" + cacheSizePropertyName
                            + "' system property. The memory available to the JVM is " + MemorySizeFormatter.format(getMemorySize())
                            + " which gives a cache size of " + MemorySizeFormatter.format(cacheSize));
                    return cacheSize;
                } catch (IllegalArgumentException e2)
                {
                    throw new IllegalArgumentException("Cache size was set to '" + propertyValue + "' in '" + cacheSizePropertyName
                            + "' system property. This value is incorrect. Please set the property to an absolute value like '512m' or '1g'."
                            + " You can also use a value like '25%' to set the cache size relative to the memory available to the JVM.");
                }
            }
        }
    }

    protected long getMemorySize()
    {
        return Runtime.getRuntime().maxMemory();
    }

    protected String getSystemProperty(String propertyName)
    {
        return System.getProperty(propertyName);
    }

    private void ensureCacheInitialized()
    {
        if (cache == null)
        {
            initCache();
        }
    }

    private Cache<K, ValueWrapper<V>> getCache()
    {
        ensureCacheInitialized();
        return cache;
    }


    private long getCacheDefaultSize()
    {
        long memorySize = getMemorySize();
        long cacheSize = memorySize / 4;
        operationLog.info("Cache size has been set to its default value. The default value is 25% (" + MemorySizeFormatter.format(cacheSize) + ")"
                + " of the memory available to the JVM (" + MemorySizeFormatter.format(memorySize) + ")."
                + " If you would like to change this value, then please set '"
                + cacheSizePropertyName + "' system property in openbis.conf file.");
        return cacheSize;
    }

    private static final class ValueWrapper<V>
    {
        private V value;

        ValueWrapper(V value)
        {
            this.value = value;
        }

        public V getValue()
        {
            return value;
        }

        public void setValue(V value)
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return String.valueOf(value);
        }

    }
}
