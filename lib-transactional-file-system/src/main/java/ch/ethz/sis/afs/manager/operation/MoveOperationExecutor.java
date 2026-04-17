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

import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkDoesntExist;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkExists;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotCopied;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotDeleted;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotInTrashOrSnapshots;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotMoved;

import java.nio.file.Path;

import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.MoveOperation;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.manager.TransactionFileSystemIO;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

public class MoveOperationExecutor implements OperationExecutor<MoveOperation, Void>
{

    //
    // Singleton
    //

    private static final MoveOperationExecutor instance;

    static
    {
        instance = new MoveOperationExecutor();
    }

    private MoveOperationExecutor()
    {
    }

    public static MoveOperationExecutor getInstance()
    {
        return instance;
    }

    //
    // Operation
    //

    @Override
    public Void prepare(final @NonNull Transaction transaction, final @NonNull TransactionFileSystemIO transactionFileSystemIO,
            final @NonNull MoveOperation operation) throws Exception
    {
        checkNotMoved(transactionFileSystemIO, OperationName.Move, operation.getSource());
        checkNotCopied(transactionFileSystemIO, OperationName.Move, operation.getSource());
        checkNotDeleted(transactionFileSystemIO, OperationName.Move, operation.getSource());
        checkExists(transactionFileSystemIO, OperationName.Move, operation.getSource());

        checkNotMoved(transactionFileSystemIO, OperationName.Move, operation.getTarget());
        checkNotCopied(transactionFileSystemIO, OperationName.Move, operation.getTarget());
        checkNotDeleted(transactionFileSystemIO, OperationName.Move, operation.getTarget());
        checkNotInTrashOrSnapshots(transactionFileSystemIO, OperationName.Move, operation.getTarget());
        checkDoesntExist(transactionFileSystemIO, OperationName.Move, operation.getTarget());

        transactionFileSystemIO.setMoved(operation.getSource());
        transactionFileSystemIO.setMoved(operation.getTarget());

        return null;
    }

    @Override
    public boolean commit(final @NonNull Transaction transaction, final @NonNull MoveOperation operation) throws Exception
    {
        if (IOUtils.exists(operation.getSource()))
        {
            if (!IOUtils.exists(operation.getTarget()))
            {
                IOUtils.createDirectories(IOUtils.getParentPath(operation.getTarget()));
            }
            IOUtils.move(operation.getSource(), operation.getTarget());
            OperationExecutor.moveCaches(operation.getSource(), operation.getTarget());
            OperationExecutor.moveSnapshotsDirectory(OperationExecutor.getSnapshotsDirectoryForSource(Path.of(operation.getSource())),
                    OperationExecutor.getSnapshotsDirectoryForSource(Path.of(operation.getTarget())));
        }
        return false;
    }
}
