/*
 * Copyright ETH 2026 Zurich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.common;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.afsclient.client.AfsClientUploadHelper;
import ch.systemsx.cisd.common.api.retry.RetryCaller;

/**
 * Wraps an {@link AfsClient}, exposing only the operations the sync code (source-side delivery and
 * harvester-side apply) actually calls, and retries each of them a few times, with increasing backoff, when
 * it fails with a transient network error (connection refused/reset, timeout, ...) - a single dropped
 * connection to an AFS server shouldn't abort a whole sync run. Non-transient failures (a missing path, a
 * path that is busy, ...) are not retried and surface on the first attempt, same as calling the wrapped
 * {@link AfsClient} directly.
 */
public class RetryingAfsClient
{
    private final AfsClient delegate;

    public RetryingAfsClient(AfsClient delegate)
    {
        this.delegate = delegate;
    }

    public void setSessionToken(String sessionToken)
    {
        delegate.setSessionToken(sessionToken);
    }

    public File[] list(String owner, String source, Boolean recursively) throws Exception
    {
        return withRetry(() -> delegate.list(owner, source, recursively));
    }

    public String hash(String owner, String source) throws Exception
    {
        return withRetry(() -> delegate.hash(owner, source));
    }

    public Boolean delete(String owner, String source, Boolean trash) throws Exception
    {
        return withRetry(() -> delegate.delete(owner, source, trash));
    }

    public Boolean copy(String sourceOwner, String source, String targetOwner, String target) throws Exception
    {
        return withRetry(() -> delegate.copy(sourceOwner, source, targetOwner, target));
    }

    public Boolean move(String sourceOwner, String source, String targetOwner, String target) throws Exception
    {
        return withRetry(() -> delegate.move(sourceOwner, source, targetOwner, target));
    }

    public Boolean create(String owner, String source, Boolean directory) throws Exception
    {
        return withRetry(() -> delegate.create(owner, source, directory));
    }

    public Boolean truncate(String owner, String source, Long size) throws Exception
    {
        return withRetry(() -> delegate.truncate(owner, source, size));
    }

    public Boolean snapshot(String owner, String source) throws Exception
    {
        return withRetry(() -> delegate.snapshot(owner, source));
    }

    public Boolean upload(Path sourcePath, String destinationOwner, Path destinationPath,
            ClientAPI.FileCollisionListener fileCollisionListener, ClientAPI.TransferMonitorListener transferMonitorListener)
            throws Exception
    {
        return withRetry(() -> delegate.upload(sourcePath, destinationOwner, destinationPath, fileCollisionListener,
                transferMonitorListener));
    }

    public Boolean download(String sourceOwner, Path sourcePath, Path destinationPath,
            ClientAPI.FileCollisionListener fileCollisionListener, ClientAPI.TransferMonitorListener transferMonitorListener)
            throws Exception
    {
        return withRetry(() -> delegate.download(sourceOwner, sourcePath, destinationPath, fileCollisionListener,
                transferMonitorListener));
    }

    /** Same lookup as {@code AfsClientUploadHelper.getServerFilePresence}, just going through the retried {@link #list}. */
    public Optional<File> getFilePresence(String owner, String path) throws Exception
    {
        try
        {
            File[] files = list(owner, path, false);
            if (files.length == 1 && files[0].getPath().equals(path))
            {
                return Optional.of(files[0]);
            }
            return Optional.of(new File(owner, path,
                    Optional.ofNullable(Path.of(path).getFileName()).map(Objects::toString).orElse(""), true, null, null));
        } catch (Exception e)
        {
            if (AfsClientUploadHelper.isPathNotInStoreError(e))
            {
                return Optional.empty();
            }
            throw e;
        }
    }

    private <T> T withRetry(Callable<T> operation) throws Exception
    {
        try
        {
            return new RetryCaller<T, RuntimeException>()
            {
                @Override
                protected T call()
                {
                    try
                    {
                        return operation.call();
                    } catch (RuntimeException e)
                    {
                        throw e;
                    } catch (Exception e)
                    {
                        throw new CheckedExceptionWrapper(e);
                    }
                }
            }.callWithRetry();
        } catch (CheckedExceptionWrapper e)
        {
            throw (Exception) e.getCause();
        }
    }

    /** Lets {@link RetryCaller}, which only ever sees {@link RuntimeException}s, inspect and retry on a checked cause. */
    private static final class CheckedExceptionWrapper extends RuntimeException
    {
        CheckedExceptionWrapper(Exception cause)
        {
            super(cause);
        }
    }
}
