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

import static ch.ethz.sis.afs.exception.AFSExceptions.PathIsDirectory;

import ch.ethz.sis.afs.api.dto.File;
import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.dto.operation.WriteOperation;
import ch.ethz.sis.afs.exception.AFSExceptions;
import ch.ethz.sis.shared.io.IOUtils;

public class WriteOperationExecutor implements OperationExecutor<WriteOperation> {

    //
    // Singleton
    //

    private static final WriteOperationExecutor instance;

    static {
        instance = new WriteOperationExecutor();
    }

    private WriteOperationExecutor() {
    }

    public static WriteOperationExecutor getInstance() {
        return instance;
    }

    //
    // Operation
    //


    @Override
    public boolean prepare(Transaction transaction, WriteOperation operation) throws Exception {
        // 1. Check that if the file exists, is not a directory
        boolean sourceExists = IOUtils.exists(operation.getSource());
        if (sourceExists) {
            File existingFile = IOUtils.getFile(operation.getSource());
            if (existingFile.getDirectory()) {
                AFSExceptions.throwInstance(PathIsDirectory, OperationName.Write.name(), operation.getSource());
            }
        }
        //byte md5Hash = IOUtils.getMD5(operation.getData());

        // 1. Create temporary file if it has not been created already
        boolean tempSourceExists = IOUtils.exists(operation.getTempSource());
        if (!tempSourceExists) {
            IOUtils.createDirectories(IOUtils.getParentPath(operation.getTempSource()));
            IOUtils.createFile(operation.getTempSource());
        }

        // 2. Flush bytes
        IOUtils.write(operation.getTempSource(), 0, operation.getData());
        return true;
    }

    @Override
    public boolean commit(Transaction transaction, WriteOperation operation) throws Exception {
        if (!IOUtils.exists(operation.getSource())) {
            IOUtils.createDirectories(IOUtils.getParentPath(operation.getSource()));
            IOUtils.createFile(operation.getSource());
        }
        if (IOUtils.exists(operation.getTempSource())) { // Only copies if has not been done already
            byte[] data = (operation.getData() != null)
                    ? operation.getData()
                    : IOUtils.readFully(operation.getTempSource());

            IOUtils.write(operation.getSource(), operation.getOffset(), data);
            IOUtils.delete(operation.getTempSource());
        }
        return true;
    }
}
