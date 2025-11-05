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

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsapi.dto.FreeSpace;
import ch.ethz.sis.afsserver.exception.FSExceptions;
import ch.ethz.sis.afsserver.server.performance.Pair;
import ch.ethz.sis.afsserver.worker.AbstractProxy;
import ch.ethz.sis.afsserver.worker.providers.AuthorizationInfoProvider;
import ch.ethz.sis.shared.io.FilePermission;
import ch.ethz.sis.shared.io.IOUtils;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import lombok.NonNull;

public class AuthorizationProxy extends AbstractProxy {

    private static final Logger logger = LogManager.getLogger(AuthorizationProxy.class);

    AuthorizationInfoProvider authorizationInfoProvider;

    /*
     * Map<String, Set<FilePermission>>
     * Where String is "sessionToken-owner-source"
     */

    private final Integer authorizationProxyCacheIdleTimeout;
    private static final long IDLE_SESSION_TIMEOUT_CHECK_INTERVAL_IN_MILLIS = 1000;


    private static final Map<String, Pair<Set<FilePermission>, String>> userRightsCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> userRightsLastAccessed = new ConcurrentHashMap<>();

    private Timer cacheTimeoutCleanup;

    public AuthorizationProxy(AbstractProxy nextProxy,
            AuthorizationInfoProvider authorizationInfoProvider,
            Integer authorizationProxyCacheIdleTimeout) {
        super(nextProxy);
        this.authorizationInfoProvider = authorizationInfoProvider;
        this.authorizationProxyCacheIdleTimeout = authorizationProxyCacheIdleTimeout;

        scheduleIdleWorkerCleanupTask();
    }

    @Override
    public File[] list(String owner, String source, Boolean recursively) throws Exception {
        validateUserRights(owner, source, IOUtils.readPermissions, OperationName.List);
        return nextProxy.list(owner, source, recursively);
    }

    @Override
    public Chunk[] read(@NonNull Chunk[] chunks) throws Exception {
        for (Chunk chunk:chunks) {
            validateUserRights(chunk.getOwner(), chunk.getSource(), IOUtils.readPermissions, OperationName.Read);
        }
        return nextProxy.read(chunks);
    }

    @Override
    public Boolean write(@NonNull Chunk[] chunks) throws Exception {
        for (Chunk chunk:chunks) {
            validateUserRights(chunk.getOwner(), chunk.getSource(), IOUtils.writePermissions, OperationName.Write);
        }
        return nextProxy.write(chunks);
    }

    @Override
    public Boolean delete(String owner, String source) throws Exception {
        validateUserRights(owner, source, IOUtils.writePermissions, OperationName.Delete);
        return nextProxy.delete(owner, source);
    }

    @Override
    public Boolean copy(String sourceOwner, String source, String targetOwner, String target) throws Exception {
        validateUserRights(sourceOwner, source, IOUtils.readPermissions, OperationName.Copy);
        validateUserRights(targetOwner, target, IOUtils.writePermissions, OperationName.Copy);
        return nextProxy.copy(sourceOwner, source, targetOwner, target);
    }

    @Override
    public Boolean move(String sourceOwner, String source, String targetOwner, String target) throws Exception {
        validateUserRights(sourceOwner, source, IOUtils.readWritePermissions, OperationName.Move);
        validateUserRights(targetOwner, target, IOUtils.writePermissions, OperationName.Move);
        return nextProxy.move(sourceOwner, source, targetOwner, target);
    }

    @Override
    public @NonNull Boolean create(@NonNull final String owner, @NonNull final String source, @NonNull final Boolean directory)
            throws Exception
    {
        validateUserRights(owner, source, IOUtils.writePermissions, OperationName.Create);
        return nextProxy.create(owner, source, directory);
    }

    @Override
    public FreeSpace free(@NonNull final String owner, @NonNull final String source) throws Exception
    {
        validateUserRights(owner, source, IOUtils.readPermissions, OperationName.Free);
        return nextProxy.free(owner, source);
    }

    @Override
    public String hash(@NonNull final String owner, @NonNull final String source) throws Exception
    {
        validateUserRights(owner, source, IOUtils.readPermissions, OperationName.Hash);
        return nextProxy.hash(owner, source);
    }

    @Override
    public byte[] preview(@NonNull final String owner, @NonNull final String source) throws Exception
    {
        validateUserRights(owner, source, IOUtils.readPermissions, OperationName.Preview);
        return nextProxy.preview(owner, source);
    }

    //
    //
    //

    private void validateUserRights(String owner, String source, Set<FilePermission> permissions,
            OperationName operationName) throws Exception
    {

        String permissionsCacheKey = workerContext.getSessionToken() + "-" + owner + "-" + source;

        if (hasCachedRights(permissions, permissionsCacheKey))
        {
            workerContext.getOwnerPathMap()
                    .put(owner, userRightsCache.get(permissionsCacheKey).getValue());
            return;
        }

        if (authorizationInfoProvider.doesSessionHaveRights(workerContext, owner, permissions))
        {
            cacheUserRightsAndOwnerPath(permissionsCacheKey, permissions,
                    workerContext.getOwnerPathMap().get(owner));
            return;
        }

        throw FSExceptions.USER_NO_ACL_RIGHTS.getInstance(workerContext.getSessionToken(),
                permissions, owner, source, operationName);

    }

    private boolean hasCachedRights(Set<FilePermission> permissions, String permissionsCacheKey)
    {
        if (isCacheEnabled() && userRightsCache.get(permissionsCacheKey) != null
                    && userRightsCache.get(permissionsCacheKey).getKey().containsAll(permissions)){
            userRightsLastAccessed.put(permissionsCacheKey, System.currentTimeMillis());
            return true;
        }
        return false;
    }

    private boolean isCacheEnabled() {
        return authorizationProxyCacheIdleTimeout != null && authorizationProxyCacheIdleTimeout > 0;
    }

    private void cacheUserRightsAndOwnerPath(String cacheKey, Set<FilePermission> permissions, String ownerPath) {
        if (isCacheEnabled()) {
            userRightsCache.put(cacheKey,new Pair<>(permissions, ownerPath));
            userRightsLastAccessed.put(cacheKey, System.currentTimeMillis());
        }
    }


    private void scheduleIdleWorkerCleanupTask() {
        if (isCacheEnabled())
        {
            cacheTimeoutCleanup = new Timer();
            cacheTimeoutCleanup.schedule(
                    new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            cleanUpExpiredUserRights();
                        }
                    }, 0, IDLE_SESSION_TIMEOUT_CHECK_INTERVAL_IN_MILLIS
            );
        }
    }

    private void cleanUpExpiredUserRights()
    {
        try {
            for (Map.Entry<String, ?> userRights:userRightsCache.entrySet()) {
                Long lastAccessed = userRightsLastAccessed.get(userRights.getKey());
                if(lastAccessed != null)
                {
                    boolean isTimeout = lastAccessed +
                            authorizationProxyCacheIdleTimeout < System.currentTimeMillis();
                    // Remove from cache
                    if (isTimeout)
                    {
                        userRightsCache.remove(userRights.getKey());
                        userRightsLastAccessed.remove(userRights.getKey());
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
        userRightsCache.clear();
        userRightsLastAccessed.clear();
    }
}
