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

import ch.ethz.sis.afs.api.dto.FreeSpace;
import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.FreeOperation;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.manager.TransactionFileSystemIO;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

public class FreeOperationExecutor implements NonModifyingOperationExecutor<FreeOperation>
{
    //
    // Singleton
    //

    private static final FreeOperationExecutor instance;

    static
    {
        instance = new FreeOperationExecutor();
    }

    private FreeOperationExecutor()
    {
    }

    public static FreeOperationExecutor getInstance()
    {
        return instance;
    }

    @Override
    public FreeSpace executeOperation(final @NonNull Transaction transaction, @NonNull TransactionFileSystemIO transactionFileSystemIO,
            final @NonNull FreeOperation operation) throws Exception
    {
        OperationExecutor.checkNotMoved(transactionFileSystemIO, OperationName.Free, operation.getSource());
        OperationExecutor.checkNotCopied(transactionFileSystemIO, OperationName.Free, operation.getSource());
        OperationExecutor.checkNotDeleted(transactionFileSystemIO, OperationName.Free, operation.getSource());

        String safeExistingSource = operation.getSource();
        while (safeExistingSource != null && !safeExistingSource.isEmpty()
                && !IOUtils.exists(safeExistingSource))
        {
            safeExistingSource = IOUtils.getParentPath(safeExistingSource);
        }

        return IOUtils.getSpace(safeExistingSource);
    }
}
