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

import static ch.ethz.sis.afs.exception.AFSExceptions.OperationParameterInvalid;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkExists;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotCopied;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotDeleted;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.checkNotMoved;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.stream.Stream;

import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.DeleteOperation;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.exception.AFSExceptions;
import ch.ethz.sis.afs.manager.TransactionFileSystemIO;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

public class DeleteOperationExecutor implements OperationExecutor<DeleteOperation, Void>
{

    //
    // Singleton
    //

    private static final DeleteOperationExecutor instance;

    static
    {
        instance = new DeleteOperationExecutor();
    }

    private DeleteOperationExecutor()
    {
    }

    public static DeleteOperationExecutor getInstance()
    {
        return instance;
    }

    //
    // Operation
    //

    public static final DateTimeFormatter TIMESTAMP_SUFFIX_FORMAT = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SSS");

    @Override
    public Void prepare(final @NonNull Transaction transaction, final @NonNull TransactionFileSystemIO transactionFileSystemIO,
            DeleteOperation operation) throws Exception
    {
        checkNotMoved(transactionFileSystemIO, OperationName.Delete, operation.getSource());
        checkNotCopied(transactionFileSystemIO, OperationName.Delete, operation.getSource());
        checkNotDeleted(transactionFileSystemIO, OperationName.Delete, operation.getSource());
        checkExists(transactionFileSystemIO, OperationName.Delete, operation.getSource());

        if (operation.isTrash())
        {
            Path normalizedSource = Path.of(operation.getSource()).toAbsolutePath().normalize();
            Path normalizedTrashRoot = Path.of(operation.getTrashRoot()).toAbsolutePath().normalize();
            if (normalizedTrashRoot.startsWith(normalizedSource) && !normalizedTrashRoot.equals(normalizedSource))
            {
                AFSExceptions.throwInstance(OperationParameterInvalid, OperationName.Delete.name(),
                        "Deleted source cannot be an ancestor of the trash root.");
            }
        }

        transactionFileSystemIO.setDeleted(operation.getSource());

        return null;
    }

    @Override
    public boolean commit(final @NonNull Transaction transaction, final @NonNull DeleteOperation operation) throws Exception
    {
        Path sourcePath = Path.of(operation.getSource());

        if (IOUtils.exists(operation.getSource()))
        {
            if (operation.isTrash())
            {
                Path trashRootPath = Path.of(operation.getTrashRoot());

                boolean sourceInTrash = sourcePath.toAbsolutePath().normalize().startsWith(trashRootPath.toAbsolutePath().normalize());

                if (!sourceInTrash)
                {
                    moveToTrash(trashRootPath, sourcePath);
                    return true;
                }
            }

            IOUtils.delete(operation.getSource());
            OperationExecutor.clearCaches(operation.getSource());
            OperationExecutor.clearSnapshotsDirectory(OperationExecutor.getSnapshotsDirectoryForSource(sourcePath));
        }

        return true;
    }

    private void moveToTrash(Path trashRootPath, Path sourcePath) throws Exception
    {
        if (!Files.exists(sourcePath))
        {
            return;
        }

        Path relativePathInTrash = trashRootPath.getParent().relativize(sourcePath);
        Path pathInTrash = trashRootPath.resolve(relativePathInTrash);

        if (Files.exists(pathInTrash))
        {
            if (Files.isDirectory(sourcePath) && Files.isDirectory(pathInTrash))
            {
                // merge folders (start with regular files to potentially rename existing conflicting snapshot folders first)
                try (Stream<Path> paths = Files.list(sourcePath).filter(Files::isRegularFile))
                {
                    Iterator<Path> iterator = paths.iterator();
                    while (iterator.hasNext())
                    {
                        moveToTrash(trashRootPath, iterator.next());
                    }
                }
                try (Stream<Path> paths = Files.list(sourcePath).filter(Files::isDirectory))
                {
                    Iterator<Path> iterator = paths.iterator();
                    while (iterator.hasNext())
                    {
                        moveToTrash(trashRootPath, iterator.next());
                    }
                }
                IOUtils.delete(sourcePath.toString());
            } else
            {
                // rename the existing file/folder, its cache and its snapshots in the trash
                // to safely move the just deleted file/folder, its cache and its snapshots to the trash

                Path uniquePathInTrash = getUniqueTrashPath(pathInTrash);

                IOUtils.move(pathInTrash.toString(), uniquePathInTrash.toString());
                OperationExecutor.moveCaches(pathInTrash.toString(), uniquePathInTrash.toString());
                OperationExecutor.moveSnapshotsDirectory(OperationExecutor.getSnapshotsDirectoryForSource(pathInTrash),
                        OperationExecutor.getSnapshotsDirectoryForSource(uniquePathInTrash));

                IOUtils.move(sourcePath.toString(), pathInTrash.toString());
                OperationExecutor.moveCaches(sourcePath.toString(), pathInTrash.toString());
                OperationExecutor.moveSnapshotsDirectory(OperationExecutor.getSnapshotsDirectoryForSource(sourcePath),
                        OperationExecutor.getSnapshotsDirectoryForSource(pathInTrash));
            }
        } else
        {
            IOUtils.createDirectories(pathInTrash.getParent().toString());
            IOUtils.move(sourcePath.toString(), pathInTrash.toString());
            OperationExecutor.moveCaches(sourcePath.toString(), pathInTrash.toString());
            OperationExecutor.moveSnapshotsDirectory(OperationExecutor.getSnapshotsDirectoryForSource(sourcePath),
                    OperationExecutor.getSnapshotsDirectoryForSource(pathInTrash));
        }
    }

    private static Path getUniqueTrashPath(final Path trashPath) throws IOException
    {
        Path uniqueTrashPath;
        int counter = 1;
        do
        {
            ZonedDateTime lastModificationTime = Files.getLastModifiedTime(trashPath).toInstant().atZone(ZoneId.systemDefault());

            StringBuilder fileName = new StringBuilder(trashPath.getFileName().toString());
            fileName.append("#");
            fileName.append(lastModificationTime.format(TIMESTAMP_SUFFIX_FORMAT));
            if (counter > 1)
            {
                fileName.append("_");
                fileName.append(counter);
            }

            uniqueTrashPath = trashPath.getParent().resolve(fileName.toString());
            counter++;
        } while (Files.exists(uniqueTrashPath));
        return uniqueTrashPath;
    }
}
