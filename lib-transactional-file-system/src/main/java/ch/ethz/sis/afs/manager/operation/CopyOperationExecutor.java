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

import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkExists;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotCopied;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotDeleted;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotInTrashOrSnapshots;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotMoved;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotWritten;

import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.CopyOperation;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.manager.TransactionFileSystemIO;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

public class CopyOperationExecutor implements OperationExecutor<CopyOperation, Void> {

    //
    // Singleton
    //

    private static final CopyOperationExecutor instance;

    static {
        instance = new CopyOperationExecutor();
    }

    private CopyOperationExecutor() {
    }

    public static CopyOperationExecutor getInstance() {
        return instance;
    }

    //
    // Operation
    //

    @Override
    public Void prepare(final @NonNull Transaction transaction, final @NonNull TransactionFileSystemIO transactionFileSystemIO, final @NonNull CopyOperation operation) throws Exception {
        checkNotWritten(transactionFileSystemIO, OperationName.Copy, operation.getSource());
        checkNotMoved(transactionFileSystemIO, OperationName.Copy, operation.getSource());
        checkNotCopied(transactionFileSystemIO, OperationName.Copy, operation.getSource());
        checkNotDeleted(transactionFileSystemIO, OperationName.Copy, operation.getSource());
        checkExists(transactionFileSystemIO, OperationName.Copy, operation.getSource());

        checkNotMoved(transactionFileSystemIO, OperationName.Copy, operation.getTarget());
        checkNotCopied(transactionFileSystemIO, OperationName.Copy, operation.getTarget());
        checkNotDeleted(transactionFileSystemIO, OperationName.Copy, operation.getTarget());
        checkNotInTrashOrSnapshots(transactionFileSystemIO, OperationName.Copy, operation.getTarget());

        String tempFileParent = IOUtils.getParentPath(OperationExecutor.getTempPath(transaction, operation.getTarget()));
        if (!IOUtils.exists(tempFileParent)) {
            IOUtils.createDirectories(tempFileParent);
        }
        IOUtils.copy(operation.getSource(), OperationExecutor.getTempPath(transaction, operation.getTarget()));

        transactionFileSystemIO.setCopied(operation.getTarget());

        return null;
    }

    @Override
    public boolean commit(final @NonNull Transaction transaction, final @NonNull CopyOperation operation) throws Exception {
        String tempFilePath = OperationExecutor.getTempPath(transaction, operation.getTarget());
        if (IOUtils.exists(tempFilePath)) {
            String targetFileParent = IOUtils.getParentPath(operation.getTarget());
            if (!IOUtils.exists(targetFileParent)) {
                IOUtils.createDirectories(targetFileParent);
            }
            IOUtils.move(tempFilePath, operation.getTarget());
            OperationExecutor.clearCaches(operation.getTarget());
        }
        return true;
    }
}
