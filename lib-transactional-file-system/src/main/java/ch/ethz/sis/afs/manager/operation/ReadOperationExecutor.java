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

import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.dto.operation.ReadOperation;
import ch.ethz.sis.afs.exception.AFSExceptions;
import ch.ethz.sis.afs.manager.TransactionFileSystemIO;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

public class ReadOperationExecutor implements NonModifyingOperationExecutor<ReadOperation>
{
    //
    // Singleton
    //

    private static final ReadOperationExecutor instance;

    static
    {
        instance = new ReadOperationExecutor();
    }

    private ReadOperationExecutor()
    {
    }

    public static ReadOperationExecutor getInstance()
    {
        return instance;
    }

    @Override
    public byte[] executeOperation(final @NonNull Transaction transaction, @NonNull TransactionFileSystemIO transactionFileSystemIO,
            final @NonNull ReadOperation operation) throws Exception
    {
        OperationExecutor.checkNotWritten(transactionFileSystemIO, OperationName.Read, operation.getSource());
        OperationExecutor.checkNotMoved(transactionFileSystemIO, OperationName.Read, operation.getSource());
        OperationExecutor.checkNotCopied(transactionFileSystemIO, OperationName.Read, operation.getSource());
        OperationExecutor.checkNotDeleted(transactionFileSystemIO, OperationName.Read, operation.getSource());

        if (IOUtils.getFile(operation.getSource()).getDirectory())
        {
            AFSExceptions.throwInstance(AFSExceptions.PathIsDirectory, OperationName.Read,
                    IOUtils.getRelativePath(transaction.getStorageRoot(), operation.getSource()));
        }

        return IOUtils.read(operation.getSource(), operation.getOffset(), operation.getLimit());
    }
}
