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
import static ch.ethz.sis.afs.exception.AFSExceptions.PathNotInStore;

import java.util.Map;

import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.DeleteOperation;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.exception.AFSExceptions;
import ch.ethz.sis.shared.io.IOUtils;

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

    @Override
    public Void prepare(Transaction transaction, DeleteOperation operation) throws Exception
    {
        // Check that file/directory exist
        PathState pathState = OperationExecutor.getCachedPathState(transaction, operation.getSource());

        if (!pathState.isExists())
        {
            AFSExceptions.throwInstance(PathNotInStore, OperationName.Delete.name(), operation.getSource());
        }

        // Update state of the path and its children
        pathState.setExists(false);
        pathState.setDeleted(true);

        for (Map.Entry<String, PathState> pathStateEntry : transaction.getPathStateCache().entrySet())
        {
            if (pathStateEntry.getValue() == pathState)
            {
                continue;
            }
            if (pathStateEntry.getKey().startsWith(operation.getSource()))
            {
                pathStateEntry.getValue().setExists(false);
                pathStateEntry.getValue().setDeleted(true);
            }
        }

        return null;
    }

    @Override
    public boolean commit(Transaction transaction, DeleteOperation operation) throws Exception
    {
        if (IOUtils.exists(operation.getSource()))
        {
            IOUtils.delete(operation.getSource());
            OperationExecutor.clearCaches(operation.getSource());
        }
        return true;
    }
}
