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
import ch.ethz.sis.afs.dto.operation.HashOperation;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.exception.AFSExceptions;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static ch.ethz.sis.afs.exception.AFSExceptions.PathNotInStore;
import static ch.ethz.sis.afs.exception.AFSExceptions.PathNotRegularFile;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.CACHED_MD5_SUFFIX;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.HIDDEN_AFS_DIRECTORY;

public class HashOperationExecutor implements NonModifyingOperationExecutor<HashOperation> {
    //
    // Singleton
    //

    private static final HashOperationExecutor instance;

    static {
        instance = new HashOperationExecutor();
    }

    private HashOperationExecutor() {
    }

    public static HashOperationExecutor getInstance() {
        return instance;
    }

    @Override
    public String executeOperation(@NonNull Transaction transaction, @NonNull HashOperation operation) throws Exception {

        if(IOUtils.exists(operation.getSource())) {
            if(IOUtils.isRegularFile(operation.getSource())) {
                Path sourcePath = Path.of(operation.getSource());
                Path hiddenFolderPath = sourcePath.getParent().resolve(HIDDEN_AFS_DIRECTORY).toAbsolutePath();
                if ( !IOUtils.isDirectory(hiddenFolderPath.toString()) ) {
                    IOUtils.createDirectories(hiddenFolderPath.toString());
                }
                Path sourceName = sourcePath.getFileName();
                String cachePath = hiddenFolderPath.resolve( sourceName.toString() + CACHED_MD5_SUFFIX).toAbsolutePath().toString();
                if ( IOUtils.isRegularFile(cachePath) ) {
                    String cachedValue = new String(IOUtils.readFully(cachePath), StandardCharsets.UTF_8);
                    return cachedValue;
                } else {

                    long size = Files.size(sourcePath);
                    long offset = 0;

                    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                    while ( offset < size ) {
                        int toBeRead = (int) Math.min(10240L, size - offset);
                        byte[] readBytes = IOUtils.read(operation.getSource(), offset, toBeRead);
                        messageDigest.update(readBytes);
                        offset+=readBytes.length;
                    }

                    String md5Value = IOUtils.asHex(messageDigest.digest());

                    if(IOUtils.exists(cachePath)) {
                        IOUtils.delete(cachePath);
                    }
                    IOUtils.createFile(cachePath);
                    IOUtils.write(cachePath, 0L, md5Value.getBytes(StandardCharsets.UTF_8));

                    return md5Value;
                }
            } else {
                AFSExceptions.throwInstance(PathNotRegularFile, OperationName.Hash.name(), operation.getSource());
                return null;
            }
        } else {
            AFSExceptions.throwInstance(PathNotInStore, OperationName.Hash.name(), operation.getSource());
            return null;
        }
    }
}
