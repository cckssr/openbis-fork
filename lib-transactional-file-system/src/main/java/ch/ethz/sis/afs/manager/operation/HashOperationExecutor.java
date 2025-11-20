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
import java.nio.file.Path;

import static ch.ethz.sis.afs.exception.AFSExceptions.PathNotInStore;
import static ch.ethz.sis.afs.exception.AFSExceptions.PathNotRegularFile;

public class HashOperationExecutor implements OperationExecutor<HashOperation, String> {
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
    public String prepare(@NonNull Transaction transaction, @NonNull HashOperation operation) throws Exception {

        if(IOUtils.exists(operation.getSource())) {
            if(IOUtils.isRegularFile(operation.getSource())) {
                Path sourcePath = Path.of(operation.getSource());
                String cachePath = OperationExecutor.getCachedHashPathForSource(sourcePath).toString();
                if ( IOUtils.isRegularFile(cachePath) ) {
                    return new String(IOUtils.readFully(cachePath), StandardCharsets.UTF_8);
                } else {
                    String md5Value = IOUtils.getMD5ForFileAsHex(operation.getSource());

                    String temporaryNewCachePath = Path.of(OperationExecutor.getTempPath(transaction, cachePath)).toAbsolutePath().normalize().toString();
                    if(IOUtils.exists(temporaryNewCachePath)) {
                        IOUtils.delete(temporaryNewCachePath);
                    }
                    IOUtils.createDirectories(IOUtils.getParentPath(temporaryNewCachePath));
                    IOUtils.createFile(temporaryNewCachePath);
                    IOUtils.write(temporaryNewCachePath, 0L, md5Value.getBytes(StandardCharsets.UTF_8));

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

    @Override
    public boolean commit(@NonNull Transaction transaction, @NonNull HashOperation operation) throws Exception {
        Path sourcePath = Path.of(operation.getSource());
        String cachePath = OperationExecutor.getCachedHashPathForSource(sourcePath).toString();
        String temporaryNewCachePath = Path.of(OperationExecutor.getTempPath(transaction, cachePath)).toAbsolutePath().normalize().toString();

        if (IOUtils.isRegularFile(temporaryNewCachePath)) { // Only copies if it has not been done yet
            IOUtils.move(temporaryNewCachePath, cachePath);
        }
        return true;
    }
}
