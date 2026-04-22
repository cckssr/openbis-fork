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

import static ch.ethz.sis.afs.exception.AFSExceptions.PathNotDirectory;
import static ch.ethz.sis.afs.exception.AFSExceptions.PathNotInStore;
import static ch.ethz.sis.afs.exception.AFSExceptions.PathNotRegularFile;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotCopied;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotDeleted;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotInTrashOrSnapshots;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotMoved;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotWritten;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.dto.operation.SnapshotOperation;
import ch.ethz.sis.afs.exception.AFSExceptions;
import ch.ethz.sis.afs.manager.TransactionFileSystemIO;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

public class SnapshotOperationExecutor implements OperationExecutor<SnapshotOperation, Void>
{

    //
    // Singleton
    //

    private static final SnapshotOperationExecutor INSTANCE;

    static
    {
        INSTANCE = new SnapshotOperationExecutor();
    }

    private SnapshotOperationExecutor()
    {
    }

    public static SnapshotOperationExecutor getInstance()
    {
        return INSTANCE;
    }

    //
    // Operation
    //

    public static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SSS");

    @Override
    public Void prepare(final @NonNull Transaction transaction, final @NonNull TransactionFileSystemIO transactionFileSystemIO,
            final @NonNull SnapshotOperation operation) throws Exception
    {
        checkNotWritten(transactionFileSystemIO, OperationName.Snapshot, operation.getSource());
        checkNotMoved(transactionFileSystemIO, OperationName.Snapshot, operation.getSource());
        checkNotCopied(transactionFileSystemIO, OperationName.Snapshot, operation.getSource());
        checkNotDeleted(transactionFileSystemIO, OperationName.Snapshot, operation.getSource());
        checkNotInTrashOrSnapshots(transactionFileSystemIO, OperationName.Snapshot, operation.getSource());

        if (IOUtils.exists(operation.getSource()))
        {
            if (IOUtils.isRegularFile(operation.getSource()))
            {
                Path sourcePath = Path.of(operation.getSource());
                Path snapshotsFolderInStorage = OperationExecutor.getSnapshotsDirectoryForSource(sourcePath);

                if (IOUtils.exists(snapshotsFolderInStorage.toString()) && !IOUtils.isDirectory(snapshotsFolderInStorage.toString()))
                {
                    AFSExceptions.throwInstance(PathNotDirectory, OperationName.Snapshot.name(), IOUtils.getRelativePath(transaction.getStorageRoot(), snapshotsFolderInStorage.toString()));
                }

                String snapshotsFolderInTransaction =
                        Path.of(OperationExecutor.getTempPath(transaction, snapshotsFolderInStorage.toString())).toString();

                if (!IOUtils.exists(snapshotsFolderInTransaction))
                {
                    IOUtils.createDirectories(snapshotsFolderInTransaction);
                } else if (!IOUtils.isDirectory(snapshotsFolderInTransaction))
                {
                    AFSExceptions.throwInstance(PathNotDirectory, OperationName.Snapshot.name(), IOUtils.getRelativePath(transaction.getWriteAheadLogRoot(), snapshotsFolderInTransaction));
                }

                String snapshotFileName = LocalDateTime.now().atZone(ZoneId.systemDefault()).format(TIMESTAMP_FORMAT);
                Path snapshotFileInTransaction = Path.of(snapshotsFolderInTransaction).resolve(snapshotFileName);

                Path uniqueSnapshotFileInTransaction = OperationExecutor.getUniqueSnapshotFile(snapshotFileInTransaction);
                Files.copy(sourcePath, uniqueSnapshotFileInTransaction);

            } else
            {
                AFSExceptions.throwInstance(PathNotRegularFile, OperationName.Snapshot.name(), IOUtils.getRelativePath(transaction.getStorageRoot(), operation.getSource()));
            }
        } else
        {
            AFSExceptions.throwInstance(PathNotInStore, OperationName.Snapshot.name(), IOUtils.getRelativePath(transaction.getStorageRoot(), operation.getSource()));
        }

        return null;
    }

    @Override
    public boolean commit(final @NonNull Transaction transaction, final @NonNull SnapshotOperation operation) throws Exception
    {
        Path snapshotsFolderInStorage = OperationExecutor.getSnapshotsDirectoryForSource(Path.of(operation.getSource()));
        Path snapshotsFolderInTransaction = Path.of(OperationExecutor.getTempPath(transaction, snapshotsFolderInStorage.toString()));
        OperationExecutor.moveSnapshotsDirectory(snapshotsFolderInTransaction, snapshotsFolderInStorage);
        return true;
    }

}
