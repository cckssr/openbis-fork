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

import static ch.ethz.sis.shared.collection.List.safe;

import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import ch.ethz.sis.afs.api.TransactionalFileSystem;
import ch.ethz.sis.afs.api.dto.File;
import ch.ethz.sis.afs.api.dto.FreeSpace;
import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.CopyOperation;
import ch.ethz.sis.afs.dto.operation.CreateOperation;
import ch.ethz.sis.afs.dto.operation.DeleteOperation;
import ch.ethz.sis.afs.dto.operation.ListOperation;
import ch.ethz.sis.afs.dto.operation.MoveOperation;
import ch.ethz.sis.afs.dto.operation.Operation;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.dto.operation.ReadOperation;
import ch.ethz.sis.afs.dto.operation.WriteOperation;
import ch.ethz.sis.afs.exception.AFSExceptions;
import ch.ethz.sis.afs.manager.operation.CopyOperationExecutor;
import ch.ethz.sis.afs.manager.operation.CreateOperationExecutor;
import ch.ethz.sis.afs.manager.operation.DeleteOperationExecutor;
import ch.ethz.sis.afs.manager.operation.ListOperationExecutor;
import ch.ethz.sis.afs.manager.operation.MoveOperationExecutor;
import ch.ethz.sis.afs.manager.operation.NonModifyingOperationExecutor;
import ch.ethz.sis.afs.manager.operation.OperationExecutor;
import ch.ethz.sis.afs.manager.operation.ReadOperationExecutor;
import ch.ethz.sis.afs.manager.operation.WriteOperationExecutor;
import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

public class TransactionConnection implements TransactionalFileSystem {

    private static final String RELATIVE = "/../";

    private static final String ROOT = "/";

    private static final Map<OperationName, NonModifyingOperationExecutor> nonModifyingOperationExecutor;

    private static final Map<OperationName, OperationExecutor> operationExecutors;

    static {
        nonModifyingOperationExecutor = Map.of(OperationName.Read, ReadOperationExecutor.getInstance(),
                OperationName.List, ListOperationExecutor.getInstance());

        operationExecutors = Map.of(OperationName.Copy, CopyOperationExecutor.getInstance(),
                OperationName.Delete, DeleteOperationExecutor.getInstance(),
                OperationName.Move, MoveOperationExecutor.getInstance(),
                OperationName.Write, WriteOperationExecutor.getInstance(),
                OperationName.Create, CreateOperationExecutor.getInstance());
    }

    private LockManager<UUID, String> lockManager;
    private JsonObjectMapper jsonObjectMapper;
    private Transaction transaction;
    private State state;
    private String writeAheadLogRoot;
    private String storageRoot;
    private RecoveredTransactions recoveredTransactions;

    /*
     * Used only to create new transactions
     */
    TransactionConnection(LockManager<UUID, String> lockManager,
                          JsonObjectMapper jsonObjectMapper,
                          String writeAheadLogRoot,
                          String storageRoot,
                          RecoveredTransactions recoveredTransactions) {
        this(lockManager, jsonObjectMapper, null);
        this.writeAheadLogRoot = writeAheadLogRoot;
        this.storageRoot = storageRoot;
        this.recoveredTransactions = recoveredTransactions;
    }

    /*
     * Can be used to recover a committed transactions after a crash
     */
    TransactionConnection(LockManager<UUID, String> lockManager,
                          JsonObjectMapper jsonObjectMapper,
                          Transaction transaction) {
        this.lockManager = lockManager;
        this.jsonObjectMapper = jsonObjectMapper;
        this.transaction = transaction;

        if (transaction != null) {
            state = State.Prepare;
            for (Operation operation : transaction.getOperations()) {
                boolean locksObtained = lockManager.add(operation.getLocks());
                if (!locksObtained) {
                    AFSExceptions.throwInstance(AFSExceptions.OperationCantBeRecovered, transaction.getUuid().toString(), operation.getName().toString());
                }
            }
        } else {
            state = State.New;
        }
    }

    //
    // Transaction control
    //

    public Transaction getTransaction() {
        return transaction;
    }

    public State getState() {
        return state;
    }

    @Override
    public void begin(UUID transactionId) throws Exception {
        /*
         * This resets the transaction, in practice to make the connection reusable across workers
         */
        if (state == State.New) {
            // New just created transaction
        } else if (state == State.Executed || state == State.Rollback || state == State.Prepare) {
            // Clean transaction, can ve reused
            transaction = null;
            state = State.New;
            copiedSourceToTarget.clear();
            copiedTargetToSource.clear();
            movedSourceToTarget.clear();
            movedTargetToSource.clear();
            written.clear();
            deleted.clear();
        } else {
            AFSExceptions.throwInstance(AFSExceptions.TransactionReuse, transaction.getUuid(), state.name());
        }

        if (recoveredTransactions.contains(transactionId)) {
            transaction = recoveredTransactions.getRecovered(transactionId);
            state = State.Prepare;
        } else if (state == State.New) {
            transaction = new Transaction(writeAheadLogRoot, storageRoot, transactionId, new ArrayList<>());
            String transactionLogDir = OperationExecutor.getTransactionLogDir(transaction);
            IOUtils.createDirectories(transactionLogDir);
            state = State.Begin;
        }
    }

    private void writeTransactionLog(boolean isCommitted) throws Exception {
        // Clone transaction and Removing data from write operations,
        // then writing the clone without data as transaction log leaving original unchanged.
        Transaction transactionForLog = new Transaction(
                transaction.getWriteAheadLogRoot(),
                transaction.getStorageRoot(),
                transaction.getUuid(),
                new ArrayList<>());

        transaction.getOperations().forEach(operation ->{
            OperationName operationName = operation.getName();
            if (Objects.requireNonNull(operationName) == OperationName.Write) {
                transactionForLog.getOperations()
                        .add(((WriteOperation) operation).toBuilder().data(null).build());
            } else {
                transactionForLog.getOperations().add(operation);
            }
        });

        byte[] bytes = jsonObjectMapper.writeValue(transactionForLog);
        String transactionLog = OperationExecutor.getTransactionLog(transactionForLog, isCommitted);
        IOUtils.createFile(transactionLog);
        IOUtils.write(transactionLog, 0, bytes);
    }

    @Override
    public Boolean prepare() throws Exception {
        if (state == State.Begin) {
            writeTransactionLog(false);
            state = State.Prepare;
            if (!recoveredTransactions.contains(transaction.getUuid())) {
                recoveredTransactions.addRecovered(transaction);
            }
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    public void commit() throws Exception {
        if (state == State.Begin || state == State.Prepare) {
            writeTransactionLog(true);
            state = State.Commit;
        }

        if (state == State.Commit) {
            for (Operation operation : transaction.getOperations()) {
                operationExecutors.get(operation.getName()).commit(transaction, operation);
            }
            cleanTransaction();
            state = State.Executed;
        }
    }

    @Override
    public void rollback() throws Exception {
        if (state == State.Begin || state == State.Prepare) {
            cleanTransaction();
            state = State.Rollback;
        }
    }

    @Override
    public List<UUID> recover() throws Exception {
        if (recoveredTransactions == null) {
            return List.of();
        } else {
            Set<UUID> recovered = recoveredTransactions.getRecovered();
            return new ArrayList<>(recovered);
        }
    }

    private void cleanTransaction() throws Exception {
        String transactionLogDir = OperationExecutor.getTransactionLogDir(transaction);
        IOUtils.delete(transactionLogDir);

        for (Operation operation : transaction.getOperations()) {
            lockManager.remove(operation.getLocks());
        }
        if (recoveredTransactions.contains(transaction.getUuid())) {
            recoveredTransactions.removeCommitted(transaction.getUuid());
        }
    }

    //
    // Operations
    //

    @Override
    public File[] list(String source, boolean recursively) throws Exception {
        String safePath = getSafePath(OperationName.List, source);
        validateOperationAndPaths(OperationName.List, safePath, null);
        validateWritten(OperationName.List, safePath);
        if (!IOUtils.isDirectory(safePath)) // Is a file and exists
        {
            return new File[]{ IOUtils.getFile(safePath) };
        } else
        {
            ListOperation operation = new ListOperation(transaction.getUuid(), safePath, recursively);
            return executeNonModifyingOperation(operation, safePath);
        }
    }

    @Override
    public byte[] read(String source, long offset, int limit) throws Exception {
        String safePath = getSafePath(OperationName.Read, source);
        validateOperationAndPaths(OperationName.Read, safePath, null);
        validateWritten(OperationName.Read, safePath);
        if (IOUtils.getFile(safePath).getDirectory()) {
            AFSExceptions.throwInstance(AFSExceptions.PathIsDirectory, OperationName.Read, source);
        }
        Operation operation = new ReadOperation(transaction.getUuid(), safePath, offset, limit);
        return executeNonModifyingOperation(operation, safePath);
    }

    public <RESULT> RESULT executeNonModifyingOperation(Operation operation, String source) throws Exception {
        boolean locksObtained = lockManager.add(operation.getLocks());
        if (locksObtained) {
            try {
                NonModifyingOperationExecutor<Operation> operationExecutor = nonModifyingOperationExecutor.get(operation.getName());
                return operationExecutor.executeOperation(transaction, operation);
            } finally {
                lockManager.remove(operation.getLocks());
            }
        } else {
            if (source != null) {
                AFSExceptions.throwInstance(AFSExceptions.PathBusy, operation.getName(), source);
            }
        }
        throw AFSExceptions.Unknown.getInstance(IllegalStateException.class.getSimpleName(), "Statement should be unreachable.");
    }

    private final Set<String> written = new HashSet<>();

    @Override
    public boolean write(String source, long offset, byte[] data) throws Exception {
        String tempSource = OperationExecutor.getTempPath(transaction, source) + "." + UUID.randomUUID();
        source = getSafePath(OperationName.Write, source);
        WriteOperation operation = new WriteOperation(transaction.getUuid(), source, tempSource, offset, data);
        boolean prepared = prepare(operation, source, null);
        if (prepared) {
            written.add(source);
        }
        return prepared;
    }

    private final Set<String> deleted = new HashSet<>();

    @Override
    public boolean delete(String source) throws Exception {
        source = getSafePath(OperationName.Delete, source);
        DeleteOperation operation = new DeleteOperation(transaction.getUuid(), source);
        boolean prepared = prepare(operation, source, null);
        if (prepared) {
            deleted.add(source);
        }
        return prepared;
    }

    private final Map<String, String> copiedSourceToTarget = new HashMap<>();
    private final Map<String, String> copiedTargetToSource = new HashMap<>();

    @Override
    public boolean copy(String source, String target) throws Exception {
        source = getSafePath(OperationName.Copy, source);
        target = getSafePath(OperationName.Copy, target);
        CopyOperation operation = new CopyOperation(transaction.getUuid(), source, target);
        boolean prepared = prepare(operation, source, target);
        if (prepared) {
            copiedSourceToTarget.put(source, target);
            copiedTargetToSource.put(target, source);
        }
        return prepared;
    }

    private final Map<String, String> movedSourceToTarget = new HashMap<>();
    private final Map<String, String> movedTargetToSource = new HashMap<>();

    @Override
    public boolean move(String source, String target) throws Exception {
        source = getSafePath(OperationName.Move, source);
        target = getSafePath(OperationName.Move, target);
        MoveOperation operation = new MoveOperation(transaction.getUuid(), source, target);
        boolean prepared = prepare(operation, source, target);
        if (prepared) {
            movedSourceToTarget.put(source, target);
            movedTargetToSource.put(target, source);
        }
        return prepared;
    }

    @Override
    public boolean create(@NonNull String source, final boolean directory) throws Exception
    {
        source = getSafePath(OperationName.Create, source);
        final CreateOperation operation = new CreateOperation(transaction.getUuid(), source, directory);
        boolean prepared = prepare(operation, source, null);
        if (prepared) {
            written.add(source);
        }
        return prepared;
    }

    @Override
    public FreeSpace free(@NonNull String source) throws Exception
    {
        source = getSafePath(OperationName.Free, source);
        validateOperationAndPaths(OperationName.Free, source, null);
        validateWritten(OperationName.Free, source);
        String safeExistingSource = source;
        while (safeExistingSource != null && !safeExistingSource.isEmpty()
                && !IOUtils.exists(safeExistingSource))
        {
            safeExistingSource = IOUtils.getParentPath(safeExistingSource);
        }
        return IOUtils.getSpace(safeExistingSource);
    }

    private boolean prepare(Operation operation, String source, String target) throws Exception {
        validateOperationAndPaths(operation.getName(), source, target);
        boolean locksObtained = false;
        boolean prepared = false;
        try {
            locksObtained = lockManager.add(operation.getLocks());
            final OperationName operationName = operation.getName();
            if (locksObtained) {
                prepared = operationExecutors.get(operationName).prepare(transaction, operation);
            }
            if (prepared) {
                transaction.getOperations().add(operation);

            }
        } catch (Exception ex) {
            if (locksObtained) {
                lockManager.remove(operation.getLocks());
                throw ex;
            }
        }
        return prepared;
    }

    private String getSafePath(OperationName operationName, String source) {
        if (source.contains(RELATIVE)) {
            AFSExceptions.throwInstance(AFSExceptions.PathInStoreCantBeRelative, operationName.name(), source);
        }
        if (!source.startsWith(ROOT)) {
            AFSExceptions.throwInstance(AFSExceptions.PathNotStartWithRoot, operationName.name(), source);
        }
        if(!source.equals(ROOT) && !IOUtils.isValidFilename(source)){
            AFSExceptions.throwInstance(AFSExceptions.PathInvalid, source);
        }
        return OperationExecutor.getRealPath(transaction, source);
    }

    private void validateWritten(OperationName operationName, String finalSource) {
        List<String> sourceSubPaths = null;
        if (finalSource != null) {
            sourceSubPaths = PathLockFinder.getParentSubPaths(finalSource);
        }
        for (String source:safe(sourceSubPaths)) {
            if (written.contains(source)) {
                AFSExceptions.throwInstance(AFSExceptions.PathCantBeReadAfterWritten, operationName.name(), source);
            }
        }
    }

    private void validateOperationAndPaths(OperationName operationName, String finalSource, String finalTarget) {
        List<String> sourceSubPaths = null;
        if (finalSource != null) {
            sourceSubPaths = PathLockFinder.getParentSubPaths(finalSource);
        }
        List<String> targetSubPaths = null;
        if (finalTarget != null) {
            targetSubPaths = PathLockFinder.getParentSubPaths(finalTarget);
        }
        if (state != State.Begin) {
            AFSExceptions.throwInstance(AFSExceptions.OperationNotAddedDueToState, operationName.name(), state, State.Begin);
        }
        for (String source:safe(sourceSubPaths)) {
            if (deleted.contains(source)) {
                AFSExceptions.throwInstance(AFSExceptions.PathCantBeOperatedAfterDeleted, operationName.name(), source);
            }
        }
        for (String target:safe(targetSubPaths)) {
            if (deleted.contains(target)) {
                AFSExceptions.throwInstance(AFSExceptions.PathCantBeOperatedAfterDeleted, operationName.name(), target);
            }
        }
        for (String source:safe(sourceSubPaths)) {
            if (movedSourceToTarget.containsKey(source)) {
                AFSExceptions.throwInstance(AFSExceptions.PathCantBeOperatedAfterMoved, operationName.name(), source);
            }
        }
        for (String target:safe(targetSubPaths)) {
            if (movedSourceToTarget.containsKey(target)) {
                AFSExceptions.throwInstance(AFSExceptions.PathCantBeOperatedAfterMoved, operationName.name(), target);
            }
        }
        for (String source:safe(sourceSubPaths)) {
            if (movedTargetToSource.containsKey(source)) {
                AFSExceptions.throwInstance(AFSExceptions.PathCantBeOperatedAfterMoved, operationName.name(), source);
            }
        }
        for (String target:safe(targetSubPaths)) {
            if (movedTargetToSource.containsKey(target)) {
                AFSExceptions.throwInstance(AFSExceptions.PathCantBeOperatedAfterMoved, operationName.name(), target);
            }
        }
        for (String source:safe(sourceSubPaths)) {
            if (copiedTargetToSource.containsKey(source)) {
                AFSExceptions.throwInstance(AFSExceptions.PathCantBeOperatedAfterCopied, operationName.name(), source);
            }
        }
    }

    @Override
    public boolean isTwoPhaseCommit() {
        return state == State.Prepare;
    }
}
