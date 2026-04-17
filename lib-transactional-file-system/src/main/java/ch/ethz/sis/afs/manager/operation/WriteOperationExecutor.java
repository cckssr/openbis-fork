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

import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotCopied;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotInTrashOrSnapshots;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotMoved;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkRegularFile;

import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.dto.operation.WriteOperation;
import ch.ethz.sis.afs.manager.TransactionFileSystemIO;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

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
    public Void prepare(final @NonNull Transaction transaction, final @NonNull TransactionFileSystemIO transactionFileSystemIO,
            final @NonNull WriteOperation operation) throws Exception
    {
        checkNotMoved(transactionFileSystemIO, OperationName.Write, operation.getSource());
        checkNotCopied(transactionFileSystemIO, OperationName.Write, operation.getSource());
        checkNotInTrashOrSnapshots(transactionFileSystemIO, OperationName.Write, operation.getSource());
        checkRegularFile(transactionFileSystemIO, OperationName.Write, operation.getSource());

        transactionFileSystemIO.setWritten(operation.getSource());

        boolean tempSourceExists = IOUtils.exists(operation.getTempSource());
        if (!tempSourceExists)
        {
            IOUtils.createDirectories(IOUtils.getParentPath(operation.getTempSource()));
            IOUtils.createFile(operation.getTempSource());
        }

        IOUtils.write(operation.getTempSource(), 0, operation.getData());
        return null;
    }

    @Override
    public boolean commit(final @NonNull Transaction transaction, final @NonNull WriteOperation operation) throws Exception
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
