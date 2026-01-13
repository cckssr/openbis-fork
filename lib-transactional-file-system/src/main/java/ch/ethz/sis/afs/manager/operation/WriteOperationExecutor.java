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
package ch.ethz.sis.afs.manager.operation;

import static ch.ethz.sis.afs.exception.AFSExceptions.PathIsDirectory;

import java.util.Map;

import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.Transaction.PathState;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.dto.operation.WriteOperation;
import ch.ethz.sis.afs.exception.AFSExceptions;
import ch.ethz.sis.shared.io.IOUtils;

public class WriteOperationExecutor implements OperationExecutor<WriteOperation, Void>
{

    //
    // Singleton
    //

    private static final WriteOperationExecutor instance;

    static
    {
        instance = new WriteOperationExecutor();
    }

    private WriteOperationExecutor()
    {
    }

    public static WriteOperationExecutor getInstance()
    {
        return instance;
    }

    //
    // Operation
    //

    @Override
    public Void prepare(Transaction transaction, WriteOperation operation) throws Exception
    {
        // 1. Check that if the file exists, is not a directory
        PathState pathState = OperationExecutor.getCachedPathState(transaction, operation.getSource());

        if (pathState.isExists() && pathState.isDirectory())
        {
            AFSExceptions.throwInstance(PathIsDirectory, OperationName.Write.name(), operation.getSource());
        }

        // 2. Update state of the path and its parents
        if (!pathState.isExists())
        {
            pathState.setExists(true);
            pathState.setDirectory(false);

            for (Map.Entry<String, PathState> pathStateEntry : transaction.getPathStateCache().entrySet())
            {
                if (pathStateEntry.getValue() == pathState)
                {
                    continue;
                }
                if (operation.getSource().startsWith(pathStateEntry.getKey()))
                {
                    pathStateEntry.getValue().setExists(true);
                    pathStateEntry.getValue().setDirectory(true);
                }
            }
        }

        //byte md5Hash = IOUtils.getMD5(operation.getData());

        // 3. Create temporary file if it has not been created already
        boolean tempSourceExists = IOUtils.exists(operation.getTempSource());
        if (!tempSourceExists)
        {
            IOUtils.createDirectories(IOUtils.getParentPath(operation.getTempSource()));
            IOUtils.createFile(operation.getTempSource());
        }

        // 4. Flush bytes
        IOUtils.write(operation.getTempSource(), 0, operation.getData());
        return null;
    }

    @Override
    public boolean commit(Transaction transaction, WriteOperation operation) throws Exception
    {
        if (!IOUtils.exists(operation.getSource()))
        {
            IOUtils.createDirectories(IOUtils.getParentPath(operation.getSource()));
            IOUtils.createFile(operation.getSource());
        }
        if (IOUtils.exists(operation.getTempSource()))
        { // Only copies if has not been done already
            byte[] data = (operation.getData() != null)
                    ? operation.getData()
                    : IOUtils.readFully(operation.getTempSource());

            IOUtils.write(operation.getSource(), operation.getOffset(), data);
            IOUtils.delete(operation.getTempSource());
            OperationExecutor.clearCaches(operation.getSource());
        }
        return true;
    }
}
