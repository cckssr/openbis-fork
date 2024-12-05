package ch.ethz.sis.transaction;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;

public abstract class AbstractTransaction
{

    private final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, getClass());

    private final ReentrantLock lock = new ReentrantLock();

    private final UUID transactionId;

    private final String sessionToken;

    private TransactionStatus transactionStatus = TransactionStatus.NEW;

    private Date lastAccessedDate = new Date();

    public AbstractTransaction(final UUID transactionId, final String sessionToken)
    {
        this.transactionId = transactionId;
        this.sessionToken = sessionToken;
    }

    protected abstract <T> T executeAction(Callable<T> action) throws Exception;

    public UUID getTransactionId()
    {
        return transactionId;
    }

    public String getSessionToken()
    {
        return sessionToken;
    }

    public TransactionStatus getTransactionStatus()
    {
        return transactionStatus;
    }

    public void setTransactionStatus(final TransactionStatus transactionStatus)
    {
        this.transactionStatus = transactionStatus;
    }

    public Date getLastAccessedDate()
    {
        return lastAccessedDate;
    }

    public void setLastAccessedDate(final Date lastAccessedDate)
    {
        this.lastAccessedDate = lastAccessedDate;
    }

    public <T> T executeWithoutLock(Callable<T> action, boolean touch) throws Exception
    {
        if (touch)
        {
            setLastAccessedDate(new Date());
        }

        try
        {
            if (lock.isLocked())
            {
                throw new UserFailureException(
                        "Cannot execute a new action on transaction '" + getTransactionId() + "' as it is still busy executing a previous action.");
            } else
            {
                return executeAction(action);
            }
        } finally
        {
            if (touch)
            {
                setLastAccessedDate(new Date());
            }
        }
    }

    public void executeWithLockOrFail(Callable<?> action, boolean touch) throws Exception
    {
        executeWithLock(lock::tryLock, action, () ->
        {
            throw new UserFailureException(
                    "Cannot execute a new action on transaction '" + getTransactionId() + "' as it is still busy executing a previous action.");
        }, touch);
    }

    public void executeWithLockOrSkip(Callable<?> action, boolean touch) throws Exception
    {
        executeWithLock(lock::tryLock, action, () ->
        {
            operationLog.info(
                    "Cannot execute a new action on transaction '" + getTransactionId() + "' as it is still busy executing a previous action.");
            return null;
        }, touch);
    }

    public void executeWithLockOrWait(Callable<?> action, int timeoutInSeconds, boolean touch) throws Exception
    {
        long timestamp = System.currentTimeMillis();
        executeWithLock(() -> lock.tryLock(timeoutInSeconds, TimeUnit.SECONDS), action, () ->
        {
            throw new UserFailureException(
                    "Cannot execute a new action on transaction '" + getTransactionId()
                            + "' as it is still busy executing a previous action. Waited since '" + new Date(timestamp) + "'.");
        }, touch);
    }

    private void executeWithLock(Callable<Boolean> lockingAction, Callable<?> lockedAction, Callable<?> notLockedAction, boolean touch)
            throws Exception
    {
        if (lockingAction.call())
        {
            if (touch)
            {
                setLastAccessedDate(new Date());
            }

            try
            {
                executeAction(lockedAction);
            } finally
            {
                if (touch)
                {
                    setLastAccessedDate(new Date());
                }

                lock.unlock();
            }
        } else
        {
            notLockedAction.call();
        }
    }

}
