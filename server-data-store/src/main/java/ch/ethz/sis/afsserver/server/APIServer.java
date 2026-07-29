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
package ch.ethz.sis.afsserver.server;

import static ch.ethz.sis.afsserver.server.APIServerErrorType.IncorrectParameters;
import static ch.ethz.sis.afsserver.server.APIServerErrorType.MethodNotFound;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import ch.ethz.sis.afs.api.TwoPhaseTransactionAPI;
import ch.ethz.sis.afs.api.TransactionConnectionInformation;
import ch.ethz.sis.afs.api.dto.ExceptionReason;
import ch.ethz.sis.afsserver.exception.APIExceptions;
import ch.ethz.sis.afsserver.server.impl.OperationResult;
import ch.ethz.sis.afsserver.server.observer.APIServerObserver;
import ch.ethz.sis.afsserver.server.performance.PerformanceAuditor;
import ch.ethz.sis.shared.exception.ThrowableReason;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.ethz.sis.shared.pool.Pool;
import ch.ethz.sis.shared.reflect.Reflect;
import ch.ethz.sis.shared.startup.Configuration;
import lombok.NonNull;

/**
 * This class should be used as delegate by specific server transport classes
 *
 * The API Server allows the following modes of operation:
 * | Mode                | Authorization Keys    | Description                                                                                                                                                                                                                                                                                                                                                                                               |
 * |---------------------|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
 * | Non Interactive     | sessionToken          | Use cases: Standard mode of operation to develop command line applications and user interfaces. Batch call requests are executed on a single transaction. The system starts automatically every Batch call with a begin and ends it with a commit or rollback if exceptions happen. If the user tries to use transaction methods manually on a Batch call the server will reject the complete Batch call. |
 * | Interactive         | interactiveSessionKey | Use cases: Non-standard use cases that require to leave a transaction opened, attached to a particular sessionToken between Batch calls. The system will not execute the transaction control methods automatically, standard transaction methods begin and commit should be used manually. Rollback can be used manually but the server will still use it automatically if errors happen.                 |
 * | Transaction Manager | transactionManagerKey | Use cases: Implementing a two phase transaction manager to execute transactions between two systems. Is meant to be used together in conjunction with Interactive mode. Allows the usage of the two phase transaction methods prepare and recover.                                                                                                                                                        |
 */
public class APIServer<CONNECTION extends TransactionConnectionInformation, INPUT extends Request, OUTPUT extends Response, API> {

    private static final Logger logger = LogManager.getLogger(APIServer.class);

    private static final long IDLE_WORKER_TIMEOUT_CHECK_INTERVAL_IN_MILLIS = 1000;

    private final Pool<Configuration, CONNECTION> connectionsPool;
    private final Pool<Configuration, Worker<CONNECTION>> workersPool;
    private final OperationResultCache operationResultCache;

    private final Map<String, Worker<CONNECTION>> interactiveSessionWorkersInUse = new ConcurrentHashMap<>();
    private final Map<Worker<CONNECTION>, Worker<CONNECTION>> workersInUse = new ConcurrentHashMap<>();

    private final Map<String, Method> apiMethods = new ConcurrentHashMap<>();
    private final Map<Method, Parameter[]> apiMethodParameters = new ConcurrentHashMap<>();

    private final String interactiveSessionKey;
    private final String transactionManagerKey;
    private final int apiServerWorkerTimeout; // Maximum amount of time allowed for a request to do a piece of work, when exceeded, the server cancels the request.

    private Timer idleWorkerCleanupTask;
    private boolean shutdown;
    private APIServerObserver<CONNECTION> observer;

    public APIServer(
            @NonNull Pool<Configuration, CONNECTION> connectionsPool,
            @NonNull Pool<Configuration, Worker<CONNECTION>> workersPool,
            @NonNull OperationResultCache operationResultCache,
            @NonNull Class<API> apiClassDefinition,
            @NonNull String interactiveSessionKey,
            @NonNull String transactionManagerKey,
            int apiServerWorkerTimeout,
            @NonNull APIServerObserver observer) {
        this.shutdown = false;
        this.connectionsPool = connectionsPool;
        this.workersPool = workersPool;
        this.operationResultCache = operationResultCache;

        for (Method method : apiClassDefinition.getMethods()) {
            apiMethods.put(method.getName(), method);
            apiMethodParameters.put(method, method.getParameters());
        }

        this.apiServerWorkerTimeout = apiServerWorkerTimeout;
        this.interactiveSessionKey = interactiveSessionKey;
        this.transactionManagerKey = transactionManagerKey;
        this.observer = observer;

        scheduleIdleWorkerCleanupTask();
    }

    public void shutdown() {
        idleWorkerCleanupTask.cancel();
        shutdown = true;
    }

    public boolean hasWorkersInUse() {
        return !workersInUse.isEmpty();
    }

    private void scheduleIdleWorkerCleanupTask() {
        idleWorkerCleanupTask = new Timer();
        idleWorkerCleanupTask.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        runIdleWorkerCleanupTask();
                    }
                }, 0, IDLE_WORKER_TIMEOUT_CHECK_INTERVAL_IN_MILLIS
        );
    }

    void runIdleWorkerCleanupTask(){
        for (String sessionToken : interactiveSessionWorkersInUse.keySet()) {
            try {
                Worker<CONNECTION> workerInUse = interactiveSessionWorkersInUse.get(sessionToken);
                if (workerInUse != null) {
                    if (workerInUse.timeout(apiServerWorkerTimeout)) {
                        checkIn(true,
                                false,
                                true,
                                false,
                                false,
                                true,
                                sessionToken,
                                workerInUse);
                    }
                }
            } catch (Exception ex) {
                logger.catching(ex);
            }
        }
    }

    private static final Set<String> twoPhaseTransactionAPIMethods = Reflect.getMethodNames(TwoPhaseTransactionAPI.class);

    private boolean isValidNonInteractiveSession(INPUT request) {
        return !twoPhaseTransactionAPIMethods.contains(request.getMethod());
    }

    private boolean isValidInteractiveSessionExpectedToBeFinished(INPUT request) {
        return request.getMethod().equals("commit") || request.getMethod().equals("rollback");
    }

    private UUID getTransactionId(INPUT request){
        if (request.getParams() != null) {
            return (UUID) request.getParams().get("transactionId");
        } else {
            return null;
        }
    }

    public OUTPUT processOperation(INPUT request, ResponseBuilder<OUTPUT> responseBuilder, PerformanceAuditor performanceAuditor) throws APIServerException {
        logger.traceAccess(null, request);

        // Shutting down?
        if (shutdown) {
            throw new APIServerException(null, APIServerErrorType.InternalError, APIExceptions.SHUTTING_DOWN.getCause());
        }

        // Requests validation
        // begin/rollback can only be called if the session token is present
        String sessionToken = request.getSessionToken();
        boolean sessionTokenFound = sessionToken != null;
        boolean isValidTransactionManagerMode = transactionManagerKey.equals(request.getTransactionManagerKey());
        boolean isValidInteractiveSession = interactiveSessionKey.equals(request.getInteractiveSessionKey());

        boolean isValidInteractiveSessionExpectedToBeFinished = false;

        if (isValidInteractiveSession) {
            isValidInteractiveSessionExpectedToBeFinished = isValidInteractiveSessionExpectedToBeFinished(request) || !sessionTokenFound;
        }

        boolean isValidNonInteractiveSession = false;
        if (!isValidInteractiveSession) {
            isValidNonInteractiveSession = isValidNonInteractiveSession(request);
        }

        if (!isValidInteractiveSession && !isValidNonInteractiveSession) {
            throw new APIServerException(null, IncorrectParameters, APIExceptions.NON_INTERACTIVE_WITH_TRANSACTION_CONTROL.getCause());
        }

        UUID transactionId = getTransactionId(request);

        // Process requests separately
        Worker<CONNECTION> worker = null;
        String currentRequestId = null;
        OUTPUT response = null;
        boolean errorFound = false;

        try {
            worker = checkOut(performanceAuditor,
                                isValidTransactionManagerMode,
                                isValidInteractiveSession,
                                isValidNonInteractiveSession,
                                sessionTokenFound,
                                sessionToken,
                                transactionId);

            response = dispatcher(worker, request, responseBuilder);
            currentRequestId = request.getId();
            currentRequestId = null;
            errorFound = response.getError() != null;
        } catch (Exception exception) {
            errorFound = true;
            logger.catching(exception);
            APIServerException apiException;
            if (exception instanceof APIServerException) {
                apiException = (APIServerException) exception;
            } else if(exception.getCause() != null && (exception.getCause() instanceof ThrowableReason)) {
                ThrowableReason throwableReason = (ThrowableReason) exception.getCause();
                apiException = new APIServerException(currentRequestId, APIServerErrorType.InternalError, throwableReason.getReason());
            } else if (exception instanceof InvocationTargetException) { // When calling methods using reflection the real cause is wrapped
                Throwable originalException = exception.getCause();
                ExceptionReason reason;
                if (originalException != null)
                {
                    if (originalException.getCause() instanceof ThrowableReason)
                    {
                        ThrowableReason throwableReason = (ThrowableReason) originalException.getCause();
                        reason = (ExceptionReason) throwableReason.getReason();
                    } else
                    {
                        reason = APIExceptions.UNKNOWN.getCause(originalException.getClass().getSimpleName(), originalException.getMessage());
                    }
                } else
                { // This error branch has never been hit during testing
                    reason = APIExceptions.UNKNOWN.getCause(exception.getClass().getSimpleName(), exception.getMessage());
                }
                apiException = new APIServerException(currentRequestId, APIServerErrorType.InternalError, reason);
            } else { // This error branch has never been hit during testing
                ExceptionReason cause = APIExceptions.UNKNOWN.getCause(exception.getClass().getSimpleName(), exception.getMessage());
                apiException = new APIServerException(currentRequestId, APIServerErrorType.InternalError, cause);
            }
            logger.throwing(apiException);
            throw apiException;
        } finally {
            checkIn(isValidInteractiveSession,
                    isValidInteractiveSessionExpectedToBeFinished,
                    false,
                    isValidNonInteractiveSession,
                    errorFound,
                    sessionTokenFound,
                    sessionToken,
                    worker);
        }

        return logger.traceExit(response);
    }

    private Worker<CONNECTION> checkOut(PerformanceAuditor performanceAuditor,
                                        boolean isValidTransactionManagerMode,
                                        boolean isValidInteractiveSession,
                                        boolean isValidNonInteractiveSession,
                                        boolean sessionTokenFound,
                                        String sessionToken,
                                        UUID transactionId) throws Exception {
        Worker<CONNECTION> worker = null;
        boolean isValidInteractiveSessionExpectedToBeFinished = false;

        try {
            Worker<CONNECTION> workerInUse = sessionToken != null ? interactiveSessionWorkersInUse.get(sessionToken) : null;

            if (isValidInteractiveSession && sessionTokenFound && workerInUse != null) {
                if(workerInUse.acquire())
                {
                    worker = workerInUse;
                    if (transactionId != null && !transactionId.equals(worker.getTransactionId())) {
                        throw new APIServerException(null, IncorrectParameters, APIExceptions.SESSION_IN_USE_BY_DIFFERENT_TRANSACTION.getCause(sessionToken, worker.getTransactionId()));
                    }
                } else {
                    throw new APIServerException(null, IncorrectParameters, APIExceptions.SESSION_IN_USE_BY_DIFFERENT_OPERATION.getCause(sessionToken));
                }
            } else {
                // Recovery flow that assigns an existing transactionId to a different session token since it was not found before
                if (sessionTokenFound && isValidInteractiveSession && transactionId != null) {
                    workerInUse = interactiveSessionWorkersInUse.values().stream().filter(w -> Objects.equals(w.getTransactionId(), transactionId)).findFirst().orElse(null);
                    if(workerInUse != null) {
                        if (workerInUse.acquire())
                        {
                            interactiveSessionWorkersInUse.remove(workerInUse.getSessionToken());
                            interactiveSessionWorkersInUse.put(sessionToken, workerInUse);
                            workerInUse.setSessionToken(sessionToken);
                            worker = workerInUse;
                        } else {
                            throw new APIServerException(null, IncorrectParameters, APIExceptions.SESSION_IN_USE_BY_DIFFERENT_OPERATION.getCause(sessionToken));
                        }
                    }
                }

                // Standard begin for both interactive and non-interactive session
                if (worker == null)
                {
                    CONNECTION connection = connectionsPool.checkOut();
                    try
                    {
                        worker = workersPool.checkOut();
                    } catch (Exception exceptionAtCheckout) {
                        connectionsPool.checkIn(connection);
                        throw exceptionAtCheckout;
                    }
                    worker.createContext(performanceAuditor);
                    worker.setConnection(connection);
                    worker.setTransactionManagerMode(isValidTransactionManagerMode);
                    worker.setInteractiveSessionMode(isValidInteractiveSession);
                    worker.acquire();
                }

                if (sessionTokenFound)
                {
                    worker.setSessionToken(sessionToken);

                    if (isValidInteractiveSession)
                    {
                        Worker<CONNECTION> concurrentlyCreatedWorker = interactiveSessionWorkersInUse.putIfAbsent(sessionToken, worker);

                        if (concurrentlyCreatedWorker != null && concurrentlyCreatedWorker != worker) {
                            isValidInteractiveSessionExpectedToBeFinished = true;
                            throw new APIServerException(null, IncorrectParameters, APIExceptions.SESSION_IN_USE_BY_DIFFERENT_OPERATION.getCause(sessionToken));
                        }
                    } else if (isValidNonInteractiveSession)
                    {
                        worker.begin(UUID.randomUUID());
                    }
                }
            }

            workersInUse.put(worker, worker);

        } catch (Exception exceptionAtCheckout) {
            checkIn(isValidInteractiveSession,
                    isValidInteractiveSessionExpectedToBeFinished,
                    false,
                    isValidNonInteractiveSession,
                    true,
                    sessionTokenFound,
                    sessionToken,
                    worker);
            throw exceptionAtCheckout;
        }

        return worker;
    }

    private void checkIn(boolean isValidInteractiveSession,
                             boolean isValidInteractiveSessionExpectedToBeFinished,
                             boolean isValidInteractiveSessionTimedOut,
                             boolean isValidNonInteractiveSession,
                             boolean errorFound,
                             boolean sessionTokenFound,
                             String sessionToken,
                             Worker<CONNECTION> worker) {
        if (worker == null) {
            return;
        }

        try {
            if(sessionTokenFound && isValidInteractiveSessionTimedOut){
                worker.rollback();
            }
        } catch (Exception ex) {
            logger.catching(ex);
        }

        try {
            if (sessionTokenFound && isValidInteractiveSession) {
                if (isValidInteractiveSessionExpectedToBeFinished || isValidInteractiveSessionTimedOut) {
                    interactiveSessionWorkersInUse.remove(sessionToken, worker);
                } else {
                    worker.release(); // There is only need to release on interactive sessions to reuse the worker, on non-interactive the context is cleanup
                }
            }
            workersInUse.remove(worker);
        } catch (Exception ex) {
            logger.catching(ex);
        }

        try {
            if (sessionTokenFound && isValidNonInteractiveSession && !errorFound) {
                worker.commit();
            }
        } catch (Exception ex) {
            logger.catching(ex);
        }

        boolean doCleanAndReturnWorker = isValidInteractiveSessionExpectedToBeFinished || isValidInteractiveSessionTimedOut || isValidNonInteractiveSession;

        if (doCleanAndReturnWorker) {
            CONNECTION connection = null;

            try {
                connection = worker.getConnection(); // Connection saved before clean it
            } catch (Exception ex) {
                logger.catching(ex);
            }

            try {
                worker.release();
                worker.cleanConnection(); // This also does logically worker.release()
            } catch (Exception ex) {
                logger.catching(ex);
            }

            try {
                connectionsPool.checkIn(connection);
            } catch (Exception ex) {
                logger.catching(ex);
            }

            try {
                worker.cleanContext();
            } catch (Exception ex) {
                logger.catching(ex);
            }

            try {
                workersPool.checkIn(worker);
            } catch (Exception ex) {
                logger.catching(ex);
            }
        }
    }

    //
    // Dispatcher, picks the correct handler and executes the method
    //

    private OUTPUT dispatcher(Worker<CONNECTION> api, Request request, ResponseBuilder<OUTPUT> responseBuilder) throws Exception {
        Method apiMethod = apiMethods.get(request.getMethod());
        Object[] requestParamsForApiMethod = null;

        if (apiMethod != null) {
            Parameter[] apiParams = apiMethodParameters.get(apiMethod);

            Map<String, Object> requestParams = request.getParams();

            // Parameters size check
            if ((requestParams == null && apiParams.length != 0) ||
                    (requestParams != null && apiParams.length != requestParams.size())) {
                throw new APIServerException(request.getId(), IncorrectParameters, APIExceptions.WRONG_PARAMETER_LIST_LENGTH.getCause());
            }

            // Parameters present check
            requestParamsForApiMethod = new Object[apiParams.length];
            for (int pIdx = 0; pIdx < apiParams.length; pIdx++) {
                Parameter parameter = apiParams[pIdx];
                Object requestParam = requestParams.get(parameter.getName());

                // Parameter present
                if (requestParam == null) {
                    throw new APIServerException(request.getId(), IncorrectParameters, APIExceptions.MISSING_METHOD_PARAMETER.getCause(parameter.getName(), apiMethod));
                }

                // Parameter of the expected type
                if (!parameter.getType().isInstance(requestParam)) {
                    throw new APIServerException(request.getId(), IncorrectParameters, APIExceptions.METHOD_PARAMETER_WRONG_TYPE.getCause(parameter.getName(), apiMethod));
                }

                requestParamsForApiMethod[pIdx] = requestParam;
            }

            long operationStartTime = System.currentTimeMillis();
            Object operationResult = null;
            Throwable operationException = null;

            try
            {
                observer.beforeAPICall(api, request);
                operationResult = apiMethod.invoke(api, requestParamsForApiMethod);
                OUTPUT response = responseBuilder.build(request.getId(), operationResult);
                observer.afterAPICall(api, request, response);
                return response;
            } catch(InvocationTargetException e) {
                operationException = e.getCause();
                throw e;
            } catch(Exception e) {
                operationException = e;
                throw e;
            } finally
            {
                if (request.getOperationId() != null)
                {
                    operationResultCache.setResult(request.getOperationId(), request.getMethod(), System.currentTimeMillis() - operationStartTime,
                            OperationResult.builder().result(operationResult).exception(operationException).build());
                }
            }
        } else {
            throw new APIServerException(request.getId(), MethodNotFound, APIExceptions.METHOD_NOT_FOUND.getCause(request.getMethod()));
        }
    }

    //
    // Public API to request workers to the APIServer by extensions
    //

    public Worker<CONNECTION> checkOut() throws Exception {
        PerformanceAuditor performanceAuditor = new PerformanceAuditor();
        performanceAuditor.start();
        return checkOut(performanceAuditor,
                false,
                false,
                true,
                false,
                null,
                null);
    }

    public void checkIn(boolean errorFound,
                        Worker<CONNECTION> worker) {
        checkIn(false,
                false,
                false,
                true,
                errorFound,
                worker.getSessionToken() != null,
                worker.getSessionToken(),
                worker);
    }

}