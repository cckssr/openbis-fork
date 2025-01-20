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

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsapi.dto.FreeSpace;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.afsserver.worker.AbstractProxy;
import ch.ethz.sis.shared.io.IOUtils;
import ch.ethz.sis.shared.startup.Configuration;
import lombok.NonNull;

public class ExecutorProxy extends AbstractProxy
{

    private final String storageRoot;

    public ExecutorProxy(final Configuration configuration)
    {
        super(null);
        storageRoot = configuration.getStringProperty(AtomicFileSystemServerParameter.storageRoot);
    }

    //
    // Transaction Management
    //

    @Override
    public void begin(UUID transactionId) throws Exception
    {
        workerContext.setTransactionId(transactionId);
        workerContext.getConnection().begin(transactionId);
    }

    @Override
    public Boolean prepare() throws Exception
    {
        return workerContext.getConnection().prepare();
    }

    @Override
    public void commit() throws Exception
    {
        workerContext.getConnection().commit();
    }

    @Override
    public void rollback() throws Exception
    {
        workerContext.getConnection().rollback();
    }

    @Override
    public List<UUID> recover() throws Exception
    {
        return workerContext.getConnection().recover();
    }

    //
    // File System Operations
    //

    private String getOwnerPath(String owner)
    {
        if (workerContext.getOwnerPathMap().containsKey(owner))
        {
            return workerContext.getOwnerPathMap().get(owner);
        } else
        {
            return IOUtils.getPath("", owner);
        }
    }

    private String getSourcePath(String owner, String source)
    {
        return IOUtils.getPath(getOwnerPath(owner), source);
    }

    @Override
    public List<File> list(String owner, String source, Boolean recursively) throws Exception
    {
        return workerContext.getConnection().list(getSourcePath(owner, source), recursively)
                .stream()
                .map(file -> convertToFile(owner, file))
                .collect(Collectors.toList());
    }

    private File convertToFile(String owner, ch.ethz.sis.afs.api.dto.File file)
    {
        try
        {
            String ownerFullPath = new java.io.File(IOUtils.getPath(this.storageRoot, getOwnerPath(owner))).getCanonicalPath();
            String fileFullPath;

            if (file.getPath().startsWith(this.storageRoot))
            {
                fileFullPath = new java.io.File(file.getPath()).getCanonicalPath();
            } else
            {
                fileFullPath = new java.io.File(IOUtils.getPath(this.storageRoot, file.getPath())).getCanonicalPath();
            }

            return new File(owner, fileFullPath.substring(ownerFullPath.length()), file.getName(), file.getDirectory(), file.getSize(),
                    file.getLastModifiedTime());
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] read(String owner, String source, Long offset, Integer limit) throws Exception
    {
        return workerContext.getConnection().read(getSourcePath(owner, source), offset, limit);
    }

    @Override
    public Boolean write(String owner, String source, Long offset, byte[] data) throws Exception
    {
        return workerContext.getConnection().write(getSourcePath(owner, source), offset, data);
    }

    @Override
    public Boolean delete(String owner, String source) throws Exception
    {
        return workerContext.getConnection().delete(getSourcePath(owner, source));
    }

    @Override
    public Boolean copy(String sourceOwner, String source, String targetOwner, String target) throws Exception
    {
        return workerContext.getConnection().copy(getSourcePath(sourceOwner, source), getSourcePath(targetOwner, target));
    }

    @Override
    public Boolean move(String sourceOwner, String source, String targetOwner, String target) throws Exception
    {
        return workerContext.getConnection().move(getSourcePath(sourceOwner, source), getSourcePath(targetOwner, target));
    }

    @Override
    public @NonNull Boolean create(@NonNull final String owner, @NonNull final String source, @NonNull final Boolean directory)
            throws Exception
    {
        return workerContext.getConnection().create(getSourcePath(owner, source), directory);
    }

    @Override
    public @NonNull FreeSpace free(@NonNull final String owner, @NonNull final String source) throws Exception
    {
        final ch.ethz.sis.afs.api.dto.FreeSpace freeSpace = workerContext.getConnection().free(getSourcePath(owner, source));
        return new FreeSpace(freeSpace.getTotal(), freeSpace.getFree());
    }

}
