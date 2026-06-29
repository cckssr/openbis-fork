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

import java.util.List;
import java.util.stream.Collectors;

import ch.ethz.sis.afs.api.dto.File;
import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.ListOperation;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.manager.TransactionFileSystemIO;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

public class ListOperationExecutor implements NonModifyingOperationExecutor<ListOperation>
{
    //
    // Singleton
    //

    private static final ListOperationExecutor instance;

    static
    {
        instance = new ListOperationExecutor();
    }

    private ListOperationExecutor()
    {
    }

    public static ListOperationExecutor getInstance()
    {
        return instance;
    }

    @Override
    public File[] executeOperation(@NonNull Transaction transaction, @NonNull TransactionFileSystemIO transactionFileSystemIO,
            @NonNull ListOperation operation) throws Exception
    {
        OperationExecutor.checkNotWritten(transactionFileSystemIO, OperationName.List, operation.getSource());
        OperationExecutor.checkNotMoved(transactionFileSystemIO, OperationName.List, operation.getSource());
        OperationExecutor.checkNotCopied(transactionFileSystemIO, OperationName.List, operation.getSource());
        OperationExecutor.checkNotDeleted(transactionFileSystemIO, OperationName.List, operation.getSource());

        if (!IOUtils.isDirectory(operation.getSource())) // Is a file and exists
        {
            File file = IOUtils.getFile(operation.getSource());
            file = file.toBuilder().path(OperationExecutor.getStoragePath(transaction, file.getPath())).build();
            return new File[] { file };
        } else
        {
            List<File> files = IOUtils.list(operation.getSource(), operation.isRecursively())
                    .stream().filter(file -> !IOUtils.isAfsHiddenFile(file.getPath())).collect(Collectors.toList());

            File[] filesFromRoot = new File[files.size()];
            int index = 0;
            for (File file : files)
            {
                filesFromRoot[index] = file.toBuilder().path(OperationExecutor.getStoragePath(transaction, file.getPath())).build();
                index++;
            }
            return filesFromRoot;
        }
    }
}
