/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.afsserver.worker.proxy;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsapi.dto.FreeSpace;
import ch.ethz.sis.afsserver.exception.FSExceptions;
import ch.ethz.sis.afsserver.worker.AbstractProxy;
import ch.ethz.sis.afsserver.worker.providers.AuthenticationInfoProvider;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import lombok.NonNull;

public class AuthenticationProxy extends AbstractProxy {

    private static final Logger logger = LogManager.getLogger(AuthenticationProxy.class);

    private final AuthenticationInfoProvider authenticationInfoProvider;
    private final Integer authenticationProxyCacheIdleTimeout;
    private static final long IDLE_SESSION_TIMEOUT_CHECK_INTERVAL_IN_MILLIS = 1000;

    /*
     * Information is kept across calls to reuse with a due time
     */
    private static final Map<String,Integer> userSessionsCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> userSessionLastAccessed = new ConcurrentHashMap<>();

    private Timer cacheTimeoutCleanup;


    public AuthenticationProxy(AbstractProxy nextProxy, AuthenticationInfoProvider authenticationInfoProvider,
            Integer authenticationProxyCacheIdleTimeout) {
        super(nextProxy);
        this.authenticationInfoProvider = authenticationInfoProvider;
        this.authenticationProxyCacheIdleTimeout = authenticationProxyCacheIdleTimeout;
        scheduleIdleWorkerCleanupTask();
    }

    //
    //
    //

    @Override
    public void begin(UUID transactionId) throws Exception {
        if (!workerContext.isTransactionManagerMode()) {
            validateSessionAvailable();
        }
        nextProxy.begin(transactionId);
    }

    @Override
    public Boolean prepare() throws Exception {
        if (workerContext.isTransactionManagerMode()) {
            validateSessionAvailable();
        } else {
            throw FSExceptions.PREPARE_REQUIRES_TM.getInstance();
        }
        return nextProxy.prepare();
    }

    @Override
    public void commit() throws Exception {
        if (!workerContext.isTransactionManagerMode()) {
            validateSessionAvailable();
        }
        nextProxy.commit();
    }

    @Override
    public void rollback() throws Exception {
        if (!workerContext.isTransactionManagerMode()) {
            validateSessionAvailable();
        }
        nextProxy.rollback();
    }

    @Override
    public List<UUID> recover() throws Exception {
        if (!workerContext.isTransactionManagerMode()) {
            throw FSExceptions.RECOVER_REQUIRES_TM.getInstance();
        }
        return nextProxy.recover();
    }

    //
    //
    //

    @Override
    public String login(@NonNull String userId, @NonNull String password) throws Exception {
        return authenticationInfoProvider.login(userId, password);
    }

    @Override
    public Boolean isSessionValid() throws Exception {
        return authenticationInfoProvider.isSessionValid(getSessionToken());
    }

    @Override
    public Boolean logout() throws Exception {
        return authenticationInfoProvider.logout(getSessionToken());
    }

    //
    //
    //

    @Override
    public File[] list(@NonNull String owner, @NonNull String source, @NonNull Boolean recursively) throws Exception {
        validateSessionAvailable();
        return nextProxy.list(owner, source, recursively);
    }

    @Override
    public Chunk[] read(@NonNull Chunk[] chunks) throws Exception {
        validateSessionAvailable();
        return nextProxy.read(chunks);
    }

    @Override
    public Boolean write(@NonNull Chunk[] chunks) throws Exception {
        validateSessionAvailable();
        return nextProxy.write(chunks);
    }

    @Override
    public Boolean delete(@NonNull String owner, @NonNull String source) throws Exception {
        validateSessionAvailable();
        return nextProxy.delete(owner, source);
    }

    @Override
    public Boolean copy(@NonNull String sourceOwner, @NonNull String source, @NonNull String targetOwner, @NonNull String target) throws Exception {
        validateSessionAvailable();
        return nextProxy.copy(sourceOwner, source, targetOwner, target);
    }

    @Override
    public Boolean move(@NonNull String sourceOwner, @NonNull String source, @NonNull String targetOwner, @NonNull String target) throws Exception {
        validateSessionAvailable();
        return nextProxy.move(sourceOwner, source, targetOwner, target);
    }

    @Override
    public @NonNull Boolean create(@NonNull final String owner, @NonNull final String source, @NonNull final Boolean directory) throws Exception
    {
        validateSessionAvailable();
        return nextProxy.create(owner, source, directory);
    }

    @Override
    public @NonNull FreeSpace free(@NonNull final String owner, @NonNull final String source) throws Exception
    {
        validateSessionAvailable();
        return nextProxy.free(owner, source);
    }

    @Override
    public String hash(@NonNull String owner, @NonNull String source) throws Exception {
        validateSessionAvailable();
        return nextProxy.hash(owner, source);
    }

    @Override
    public byte[] preview(@NonNull String owner, @NonNull String source) throws Exception {
        validateSessionAvailable();
        return nextProxy.preview(owner, source);
    }

    //
    //
    //

    private void validateSessionAvailable() throws Exception {
        if (workerContext.getSessionExists() == null)
        {
            String sessionToken = workerContext.getSessionToken();

            if (isSessionCached(sessionToken))
            {
                workerContext.setSessionExists(true);
                return;
            }

            boolean isValid = authenticationInfoProvider.isSessionValid(sessionToken);
            workerContext.setSessionExists(isValid);
            if (isValid)
            {
                cacheSession(sessionToken);
                return;
            }
        }

        if (!workerContext.getSessionExists()) {
            throw FSExceptions.SESSION_NOT_FOUND.getInstance(workerContext.getSessionToken());
        }
    }

    private boolean isSessionCached(String sessionToken) {
        if (isCacheEnabled()
                && userSessionsCache.containsKey(sessionToken)) {
            userSessionLastAccessed.put(sessionToken, System.currentTimeMillis());
            return true;
        }
        return false;
    }

    private boolean isCacheEnabled()
    {
        return authenticationProxyCacheIdleTimeout != null && authenticationProxyCacheIdleTimeout > 0;
    }

    private void cacheSession(String sessionToken) {
        if (isCacheEnabled()) {
            userSessionsCache.put(sessionToken,0);
            userSessionLastAccessed.put(sessionToken, System.currentTimeMillis());
        }
    }

    private void scheduleIdleWorkerCleanupTask() {
        if (isCacheEnabled()) {
            cacheTimeoutCleanup = new Timer();
            cacheTimeoutCleanup.schedule(new TimerTask()
                                         {
                                             @Override
                                             public void run()
                                             {
                                                 cleanUpExpiredSessions();
                                             }
                                         }, 0, IDLE_SESSION_TIMEOUT_CHECK_INTERVAL_IN_MILLIS
            );
        }
    }

    private void cleanUpExpiredSessions() {
        try {
            for (String sessionToken:userSessionsCache.keySet()) {
                Long lastAccessed = userSessionLastAccessed.get(sessionToken);
                if(lastAccessed != null)
                {
                    boolean isTimeout = lastAccessed +
                            authenticationProxyCacheIdleTimeout < System.currentTimeMillis();
                    // Remove from cache
                    if (isTimeout)
                    {
                        userSessionsCache.remove(sessionToken);
                        userSessionLastAccessed.remove(sessionToken);
                    }
                }
            }
        } catch (Exception ex) {
            logger.catching(ex);
        }
    }


    public void shutdown() {
        if (cacheTimeoutCleanup != null) {
            cacheTimeoutCleanup.cancel();
        }
    }

    public static void resetCache(){
        userSessionsCache.clear();
        userSessionLastAccessed.clear();
    }

}
