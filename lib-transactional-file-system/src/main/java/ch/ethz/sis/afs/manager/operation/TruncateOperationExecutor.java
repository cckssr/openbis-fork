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

import static ch.ethz.sis.afs.dto.Transaction.PathState;
import static ch.ethz.sis.afs.exception.AFSExceptions.OperationParameterInvalid;
import static ch.ethz.sis.afs.exception.AFSExceptions.PathNotInStore;
import static ch.ethz.sis.afs.exception.AFSExceptions.PathNotRegularFile;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.dto.operation.TruncateOperation;
import ch.ethz.sis.afs.exception.AFSExceptions;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

public class TruncateOperationExecutor implements OperationExecutor<TruncateOperation, Void>
{

    //
    // Singleton
    //

    private static final TruncateOperationExecutor INSTANCE;

    static
    {
        INSTANCE = new TruncateOperationExecutor();
    }

    private TruncateOperationExecutor()
    {
    }

    public static TruncateOperationExecutor getInstance()
    {
        return INSTANCE;
    }

    //
    // Operation
    //

    @Override
    public Void prepare(final @NonNull Transaction transaction, final TruncateOperation operation) throws Exception
    {
        PathState pathState = OperationExecutor.getCachedPathState(transaction, operation.getSource());

        if (!pathState.isExists())
        {
            AFSExceptions.throwInstance(PathNotInStore, OperationName.Truncate.name(), operation.getSource());
        }

        if (pathState.isDirectory())
        {
            AFSExceptions.throwInstance(PathNotRegularFile, OperationName.Truncate.name(), operation.getSource());
        }

        if (operation.getSize() < 0)
        {
            AFSExceptions.throwInstance(OperationParameterInvalid, OperationName.Truncate.name(), "Size cannot be < 0");
        }

        pathState.setWritten(true);

        return null;
    }

    @Override
    public boolean commit(final @NonNull Transaction transaction, final TruncateOperation operation) throws Exception
    {
        if (IOUtils.exists(operation.getSource()))
        {
            try (FileChannel channel = FileChannel.open(Path.of(operation.getSource()), StandardOpenOption.WRITE))
            {
                channel.truncate(operation.getSize());
            }

            OperationExecutor.clearCaches(operation.getSource());
        }

        return true;
    }

}
