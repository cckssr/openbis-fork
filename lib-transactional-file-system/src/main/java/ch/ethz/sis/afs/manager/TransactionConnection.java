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
package ch.ethz.sis.afs.manager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ch.ethz.sis.afs.api.TransactionalFileSystem;
import ch.ethz.sis.afs.api.dto.File;
import ch.ethz.sis.afs.api.dto.FreeSpace;
import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.CopyOperation;
import ch.ethz.sis.afs.dto.operation.CreateOperation;
import ch.ethz.sis.afs.dto.operation.DeleteOperation;
import ch.ethz.sis.afs.dto.operation.FreeOperation;
import ch.ethz.sis.afs.dto.operation.HashOperation;
import ch.ethz.sis.afs.dto.operation.ListOperation;
import ch.ethz.sis.afs.dto.operation.MoveOperation;
import ch.ethz.sis.afs.dto.operation.Operation;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.dto.operation.PreviewOperation;
import ch.ethz.sis.afs.dto.operation.ReadOperation;
import ch.ethz.sis.afs.dto.operation.SnapshotOperation;
import ch.ethz.sis.afs.dto.operation.TruncateOperation;
import ch.ethz.sis.afs.dto.operation.WriteOperation;
import ch.ethz.sis.afs.exception.AFSExceptions;
import ch.ethz.sis.afs.manager.operation.CopyOperationExecutor;
import ch.ethz.sis.afs.manager.operation.CreateOperationExecutor;
import ch.ethz.sis.afs.manager.operation.DeleteOperationExecutor;
import ch.ethz.sis.afs.manager.operation.FreeOperationExecutor;
import ch.ethz.sis.afs.manager.operation.HashOperationExecutor;
import ch.ethz.sis.afs.manager.operation.ListOperationExecutor;
import ch.ethz.sis.afs.manager.operation.MoveOperationExecutor;
import ch.ethz.sis.afs.manager.operation.NonModifyingOperationExecutor;
import ch.ethz.sis.afs.manager.operation.OperationExecutor;
import ch.ethz.sis.afs.manager.operation.PreviewOperationExecutor;
import ch.ethz.sis.afs.manager.operation.ReadOperationExecutor;
import ch.ethz.sis.afs.manager.operation.SnapshotOperationExecutor;
import ch.ethz.sis.afs.manager.operation.TruncateOperationExecutor;
import ch.ethz.sis.afs.manager.operation.WriteOperationExecutor;
import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

public class TransactionConnection implements TransactionalFileSystem
{

    private static final String RELATIVE = "/../";

    private static final String ROOT = "/";

    private static final Map<OperationName, NonModifyingOperationExecutor> nonModifyingOperationExecutor;

    private static final Map<OperationName, OperationExecutor> operationExecutors;

    static
    {
        nonModifyingOperationExecutor = Map.of(OperationName.Read, ReadOperationExecutor.getInstance(),
                OperationName.List, ListOperationExecutor.getInstance(),
                OperationName.Free, FreeOperationExecutor.getInstance());

        operationExecutors = Map.of(OperationName.Copy, CopyOperationExecutor.getInstance(),
                OperationName.Delete, DeleteOperationExecutor.getInstance(),
                OperationName.Move, MoveOperationExecutor.getInstance(),
                OperationName.Write, WriteOperationExecutor.getInstance(),
                OperationName.Create, CreateOperationExecutor.getInstance(),
                OperationName.Truncate, TruncateOperationExecutor.getInstance(),
                OperationName.Snapshot, SnapshotOperationExecutor.getInstance(),
                OperationName.Hash, HashOperationExecutor.getInstance(),
                OperationName.Preview, PreviewOperationExecutor.getInstance());
    }

    private LockManager<UUID, String> lockManager;

    private JsonObjectMapper jsonObjectMapper;

    private Transaction transaction;

    private TransactionFileSystemIO transactionFileSystemIO;

    private State state;

    private String writeAheadLogRoot;

    private String storageRoot;

    private TrashRootProvider trashRootProvider;

    private Set<String> enabledPreviewFileTypes;

    private long enablePreviewSizeInBytes;

    private RecoveredTransactions recoveredTransactions;

    /*
     * Used only to create new transactions
     */
    TransactionConnection(LockManager<UUID, String> lockManager,
            JsonObjectMapper jsonObjectMapper,
            String writeAheadLogRoot,
            String storageRoot,
            TrashRootProvider trashRootProvider,
            RecoveredTransactions recoveredTransactions,
            Set<String> enabledPreviewFileTypes,
            long enablePreviewSizeInBytes)
    {
        this(lockManager, jsonObjectMapper, trashRootProvider, null, enabledPreviewFileTypes, enablePreviewSizeInBytes);
        this.writeAheadLogRoot = writeAheadLogRoot;
        this.storageRoot = storageRoot;
        this.recoveredTransactions = recoveredTransactions;
    }

    /*
     * Can be used to recover a committed transactions after a crash
     */
    TransactionConnection(LockManager<UUID, String> lockManager,
            JsonObjectMapper jsonObjectMapper,
            TrashRootProvider trashRootProvider,
            Transaction transaction,
            Set<String> enabledPreviewFileTypes,
            long enablePreviewSizeInBytes)
    {
        this.lockManager = lockManager;
        this.jsonObjectMapper = jsonObjectMapper;
        this.trashRootProvider = trashRootProvider;
        this.transaction = transaction;

        if (transaction != null)
        {
            state = State.Prepare;
            for (Operation operation : transaction.getOperations())
            {
                boolean locksObtained = lockManager.add(operation.getLocks());
                if (!locksObtained)
                {
                    AFSExceptions.throwInstance(AFSExceptions.OperationCantBeRecovered, transaction.getUuid().toString(),
                            operation.getName().toString());
                }
            }
        } else
        {
            state = State.New;
        }

        this.enabledPreviewFileTypes = enabledPreviewFileTypes;
        this.enablePreviewSizeInBytes = enablePreviewSizeInBytes;
    }

    //
    // Transaction control
    //

    public Transaction getTransaction()
    {
        return transaction;
    }

    public State getState()
    {
        return state;
    }

    @Override
    public void begin(UUID transactionId) throws Exception
    {
        /*
         * This resets the transaction, in practice to make the connection reusable across workers
         */
        if (state == State.New)
        {
            // New just created transaction
        } else if (state == State.Executed || state == State.Rollback || state == State.Prepare)
        {
            // Clean transaction, can ve reused
            transaction = null;
            transactionFileSystemIO = null;
            state = State.New;
        } else
        {
            AFSExceptions.throwInstance(AFSExceptions.TransactionReuse, transaction.getUuid(), state.name());
        }

        if (recoveredTransactions.contains(transactionId))
        {
            transaction = recoveredTransactions.getRecovered(transactionId);
            state = State.Prepare;
        } else if (state == State.New)
        {
            transaction = new Transaction(writeAheadLogRoot, storageRoot, transactionId, new ArrayList<>());
            transactionFileSystemIO = new TransactionFileSystemIO(storageRoot, trashRootProvider);
            String transactionLogDir = OperationExecutor.getTransactionLogDir(transaction);
            IOUtils.createDirectories(transactionLogDir);
            state = State.Begin;
        }
    }

    private void writeTransactionLog(boolean isCommitted) throws Exception
    {
        byte[] bytes = jsonObjectMapper.writeValue(transaction);
        String transactionLog = OperationExecutor.getTransactionLog(transaction, isCommitted);
        IOUtils.createFile(transactionLog);
        IOUtils.write(transactionLog, 0, bytes);
    }

    @Override
    public Boolean prepare() throws Exception
    {
        if (state == State.Begin)
        {
            writeTransactionLog(false);
            state = State.Prepare;
            if (!recoveredTransactions.contains(transaction.getUuid()))
            {
                recoveredTransactions.addRecovered(transaction);
            }
            return Boolean.TRUE;
        } else
        {
            return Boolean.FALSE;
        }
    }

    @Override
    public void commit() throws Exception
    {
        if (state == State.Begin || state == State.Prepare)
        {
            writeTransactionLog(true);
            state = State.Commit;
        }

        if (state == State.Commit)
        {
            for (Operation operation : transaction.getOperations())
            {
                operationExecutors.get(operation.getName()).commit(transaction, operation);
            }

            // Additionally to the write operations, there is read operations that generate metadata like the md5 and the preview
            // These operations all stored on the .afs folders and all these files should override whatever is on the destination
            {
                // ./transaction-log/.afs
                // ./transaction-log/folderA/.afs
            }
            //

            cleanTransaction();
            state = State.Executed;
        }
    }

    @Override
    public void rollback() throws Exception
    {
        if (state == State.Begin || state == State.Prepare)
        {
            cleanTransaction();
            state = State.Rollback;
        }
    }

    @Override
    public List<UUID> recover() throws Exception
    {
        if (recoveredTransactions == null)
        {
            return List.of();
        } else
        {
            Set<UUID> recovered = recoveredTransactions.getRecovered();
            return new ArrayList<>(recovered);
        }
    }

    private void cleanTransaction() throws Exception
    {
        String transactionLogDir = OperationExecutor.getTransactionLogDir(transaction);
        IOUtils.delete(transactionLogDir);

        for (Operation operation : transaction.getOperations())
        {
            lockManager.remove(operation.getLocks());
        }
        if (recoveredTransactions.contains(transaction.getUuid()))
        {
            recoveredTransactions.removeCommitted(transaction.getUuid());
        }
    }

    //
    // Operations
    //

    @Override
    public File[] list(String source, boolean recursively) throws Exception
    {
        String safePath = getSafePath(OperationName.List, source);
        ListOperation operation = new ListOperation(transaction.getUuid(), safePath, recursively);
        return executeNonModifyingOperation(operation, safePath);
    }

    @Override
    public byte[] read(String source, long offset, int limit) throws Exception
    {
        String safePath = getSafePath(OperationName.Read, source);
        Operation operation = new ReadOperation(transaction.getUuid(), safePath, offset, limit);
        return executeNonModifyingOperation(operation, safePath);
    }

    public <RESULT> RESULT executeNonModifyingOperation(Operation operation, String source) throws Exception
    {
        checkTransactionStarted(operation.getName());
        boolean locksObtained = lockManager.add(operation.getLocks());
        if (locksObtained)
        {
            try
            {
                NonModifyingOperationExecutor<Operation> operationExecutor = nonModifyingOperationExecutor.get(operation.getName());
                return operationExecutor.executeOperation(transaction, transactionFileSystemIO, operation);
            } finally
            {
                lockManager.remove(operation.getLocks());
            }
        } else
        {
            if (source != null)
            {
                AFSExceptions.throwInstance(AFSExceptions.PathBusy, operation.getName(), IOUtils.getRelativePath(storageRoot, source));
            }
        }
        throw AFSExceptions.Unknown.getInstance(IllegalStateException.class.getSimpleName(), "Statement should be unreachable.");
    }

    @Override
    public boolean write(String source, long offset, byte[] data) throws Exception
    {
        String tempSource = OperationExecutor.getTempPath(transaction, source) + "." + UUID.randomUUID();
        source = getSafePath(OperationName.Write, source);
        WriteOperation operation = new WriteOperation(transaction.getUuid(), source, tempSource, offset, data);
        prepare(operation, source, null);
        return Boolean.TRUE;
    }

    @Override
    public boolean delete(String source, boolean trash) throws Exception
    {
        source = getSafePath(OperationName.Delete, source);
        DeleteOperation operation = new DeleteOperation(transaction.getUuid(), source, trash, trash ? trashRootProvider.getTrashRoot(source) : null);
        prepare(operation, source, null);
        return Boolean.TRUE;
    }

    @Override
    public boolean copy(String source, String target) throws Exception
    {
        source = getSafePath(OperationName.Copy, source);
        target = getSafePath(OperationName.Copy, target);
        CopyOperation operation = new CopyOperation(transaction.getUuid(), source, target);
        prepare(operation, source, target);
        return Boolean.TRUE;
    }

    @Override
    public boolean move(String source, String target) throws Exception
    {
        source = getSafePath(OperationName.Move, source);
        target = getSafePath(OperationName.Move, target);
        MoveOperation operation = new MoveOperation(transaction.getUuid(), source, target);
        prepare(operation, source, target);
        return Boolean.TRUE;
    }

    @Override
    public boolean create(@NonNull String source, final boolean directory) throws Exception
    {
        source = getSafePath(OperationName.Create, source);
        final CreateOperation operation = new CreateOperation(transaction.getUuid(), source, directory);
        prepare(operation, source, null);
        return Boolean.TRUE;
    }

    @Override
    public boolean truncate(@NonNull String source, long size) throws Exception
    {
        source = getSafePath(OperationName.Truncate, source);
        final TruncateOperation operation = new TruncateOperation(transaction.getUuid(), source, size);
        prepare(operation, source, null);
        return Boolean.TRUE;
    }

    @Override
    public boolean snapshot(@NonNull String source) throws Exception
    {
        source = getSafePath(OperationName.Snapshot, source);
        final SnapshotOperation operation = new SnapshotOperation(transaction.getUuid(), source);
        prepare(operation, source, null);
        return Boolean.TRUE;
    }

    @Override
    public FreeSpace free(@NonNull String source) throws Exception
    {
        String safePath = getSafePath(OperationName.Free, source);
        FreeOperation operation = new FreeOperation(transaction.getUuid(), safePath);
        return executeNonModifyingOperation(operation, safePath);
    }

    @Override
    public @NonNull String hash(@NonNull String source) throws Exception
    {
        source = getSafePath(OperationName.Hash, source);
        HashOperation operation = new HashOperation(transaction.getUuid(), source);
        return prepare(operation, source, null);
    }

    @Override
    public @NonNull byte[] preview(@NonNull String source) throws Exception
    {
        source = getSafePath(OperationName.Preview, source);
        PreviewOperation operation = new PreviewOperation(transaction.getUuid(), source, enabledPreviewFileTypes, enablePreviewSizeInBytes);
        return prepare(operation, source, null);
    }

    /*
     * Prepare prepares the operation to not fail con COMMIT
     * AS FAR AS an exception is not thrown is supposed to be PREPARED
     */
    @SuppressWarnings("unchecked")
    private <RESULT extends Serializable> RESULT prepare(Operation operation, String source, String target) throws Exception
    {
        checkTransactionStarted(operation.getName());
        boolean locksObtained = false;
        RESULT result = null;
        try
        {
            locksObtained = lockManager.add(operation.getLocks());
            final OperationName operationName = operation.getName();
            if (locksObtained)
            {
                result = (RESULT) operationExecutors.get(operationName).prepare(transaction, transactionFileSystemIO, operation);
                transaction.getOperations().add(operation.toLog()); // This clears any attributes that are not to be kept in memory
            } else
            {
                AFSExceptions.throwInstance(AFSExceptions.PathLocksCannotBeObtained, operationName.name(),
                        IOUtils.getRelativePath(storageRoot, source));
            }
        } catch (Exception ex)
        {
            if (locksObtained)
            {
                lockManager.remove(operation.getLocks());
                throw ex;
            }
        }
        return result;
    }

    private String getSafePath(OperationName operationName, String source)
    {
        if (source.contains(RELATIVE))
        {
            AFSExceptions.throwInstance(AFSExceptions.PathInStoreCantBeRelative, operationName.name(), IOUtils.getRelativePath(storageRoot, source));
        }
        if (!source.startsWith(ROOT))
        {
            AFSExceptions.throwInstance(AFSExceptions.PathNotStartWithRoot, operationName.name(), IOUtils.getRelativePath(storageRoot, source));
        }
        if (!source.equals(ROOT) && !IOUtils.isValidFilename(source))
        {
            AFSExceptions.throwInstance(AFSExceptions.PathInvalid, IOUtils.getRelativePath(storageRoot, source));
        }
        return OperationExecutor.getRealPath(transaction, source);
    }

    private void checkTransactionStarted(OperationName operationName) throws Exception
    {
        if (state != State.Begin)
        {
            AFSExceptions.throwInstance(AFSExceptions.OperationNotAddedDueToState, operationName.name(), state, State.Begin);
        }
    }

    @Override
    public boolean isTwoPhaseCommit()
    {
        return state == State.Prepare;
    }
}
