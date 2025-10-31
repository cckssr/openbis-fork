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
import ch.ethz.sis.afs.dto.operation.Operation;
import ch.ethz.sis.afs.api.dto.File;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Path;

public interface OperationExecutor<OPERATION extends Operation, RESULT> {
    String HIDDEN_AFS_DIRECTORY = ".afs";
    String CACHED_MD5_SUFFIX = "-hash.md5";
    String CACHED_PREVIEW_SUFFIX = "-preview.jpg";

    static @NonNull
    String getTransactionLogDir(Transaction transaction) {
        return IOUtils.getPath(transaction.getWriteAheadLogRoot(), transaction.getUuid().toString());
    }

    static @NonNull
    String getTransactionLog(@NonNull File transactionDir, boolean isCommitted) {
        String name;
        if (isCommitted) {
            name = "transaction-committed.json";
        } else {
            name = "transaction-prepared.json";
        }
        return IOUtils.getPath(transactionDir.getPath(), name);
    }

    static @NonNull
    String getTransactionLog(@NonNull Transaction transaction, boolean isCommitted) {
        String name;
        if (isCommitted) {
            name = "transaction-committed.json";
        } else {
            name = "transaction-prepared.json";
        }
        return IOUtils.getPath(transaction.getWriteAheadLogRoot(), transaction.getUuid().toString(), name);
    }

    static @NonNull
    String getRealPath(@NonNull Transaction transaction, @NonNull String source) {
        return IOUtils.getPath(transaction.getStorageRoot(), source.substring(1));
    }

    static @NonNull
    String getStoragePath(@NonNull Transaction transaction, @NonNull String source) {
        return source.substring(transaction.getStorageRoot().length());
    }

    static @NonNull
    String getTempPath(@NonNull Transaction transaction, @NonNull String source) {
        String transDir = getTransactionLogDir(transaction);
        return IOUtils.getPath(transDir, source);
    }

    static @NonNull Path getHiddenAfsDirectoryForSource(@NonNull Path sourcePath) throws Exception {
        Path hiddenFolderPath = sourcePath.getParent().resolve(HIDDEN_AFS_DIRECTORY).toAbsolutePath();
        if ( !IOUtils.isDirectory(hiddenFolderPath.toString()) ) {
            IOUtils.createDirectories(hiddenFolderPath.toString());
        }
        return hiddenFolderPath;
    }

    static @NonNull Path getCachedPreviewPathForSource(@NonNull Path sourcePath) throws Exception {
        Path hiddenFolderPath = getHiddenAfsDirectoryForSource(sourcePath);
        return hiddenFolderPath.resolve( sourcePath.getFileName().toString() + CACHED_PREVIEW_SUFFIX).toAbsolutePath();
    }

    static @NonNull Path getCachedHashPathForSource(@NonNull Path sourcePath) throws Exception {
        Path hiddenFolderPath = getHiddenAfsDirectoryForSource(sourcePath);
        return hiddenFolderPath.resolve( sourcePath.getFileName().toString() + CACHED_MD5_SUFFIX).toAbsolutePath();
    }

    static void clearCaches(@NonNull String safeSourcePath) throws Exception {
        Path sourcePath = Path.of(safeSourcePath);
        String md5CachePath = getCachedHashPathForSource(sourcePath).toString();
        String previewCachePath = getCachedPreviewPathForSource(sourcePath).toString();
        if(IOUtils.exists(md5CachePath)) { IOUtils.delete(md5CachePath); }
        if(IOUtils.exists(previewCachePath)) { IOUtils.delete(previewCachePath); }
    }

    static void moveCaches(@NonNull String safeSourcePath, @NonNull String safeTargetPath) throws Exception {
        Path sourcePath = Path.of(safeSourcePath);
        Path targetPath = Path.of(safeTargetPath);

        String sourceMd5CachePath = getCachedHashPathForSource(sourcePath).toString();
        String targetMd5CachePath = getCachedHashPathForSource(targetPath).toString();
        String sourcePreviewCachePath = getCachedPreviewPathForSource(sourcePath).toString();
        String targetPreviewCachePath = getCachedPreviewPathForSource(targetPath).toString();

        if(IOUtils.exists(targetMd5CachePath)) { IOUtils.delete(targetMd5CachePath); }
        if(IOUtils.exists(targetPreviewCachePath)) { IOUtils.delete(targetPreviewCachePath); }

        if(IOUtils.exists(sourceMd5CachePath)) {
            IOUtils.createFile(targetMd5CachePath);
            IOUtils.write(targetMd5CachePath, 0L, IOUtils.readFully(sourceMd5CachePath));
            IOUtils.delete(sourceMd5CachePath);
        }
        if(IOUtils.exists(sourcePreviewCachePath)) {
            IOUtils.createFile(targetPreviewCachePath);
            IOUtils.write(targetPreviewCachePath, 0L, IOUtils.readFully(sourcePreviewCachePath));
            IOUtils.delete(sourcePreviewCachePath);
        }
    }

    /*
    * The first step
    * If the operation is a write operation is pre written to the transaction commit log directory.
    *
    * The idea is to reduce the commit operation to an atomic move or delete
    */
    RESULT prepare(@NonNull Transaction transaction, @NonNull OPERATION operation) throws Exception;

    /*
     * Commit operation should be reduced to atomic move and delete operation to avoid
     * file system corruption, this requires sometimes duplication of files,
     * exchanging performance and space for transaction safeness
     */
    boolean commit(@NonNull Transaction transaction, @NonNull OPERATION operation) throws Exception;

}
