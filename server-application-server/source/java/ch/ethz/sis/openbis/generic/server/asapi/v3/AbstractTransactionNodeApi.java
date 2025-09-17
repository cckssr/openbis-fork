package ch.ethz.sis.openbis.generic.server.asapi.v3;

import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.support.AbstractApplicationContext;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;

public abstract class AbstractTransactionNodeApi implements ApplicationListener<ApplicationEvent>
{

    protected final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, getClass());

    private static final String FINISH_TRANSACTIONS_THREAD_NAME = "finish-transactions";

    protected final TransactionConfiguration transactionConfiguration;

    private Timer finishTransactionsTimer;

    public AbstractTransactionNodeApi(final TransactionConfiguration transactionConfiguration)
    {
        this.transactionConfiguration = transactionConfiguration;
    }

    protected abstract void recoverTransactionsFromTransactionLog();

    protected abstract void finishFailedOrAbandonedTransactions();

    @Override public void onApplicationEvent(final ApplicationEvent event)
    {
        Object source = event.getSource();
        if (source instanceof AbstractApplicationContext)
        {
            AbstractApplicationContext appContext = (AbstractApplicationContext) source;
            if ((event instanceof ContextStartedEvent) || (event instanceof ContextRefreshedEvent))
            {
                if (appContext.getParent() != null && finishTransactionsTimer == null)
                {
                    if (transactionConfiguration.isEnabled())
                    {
                        recoverTransactionsFromTransactionLog();

                        finishTransactionsTimer = new Timer(FINISH_TRANSACTIONS_THREAD_NAME, true);
                        finishTransactionsTimer.schedule(new TimerTask()
                                                         {
                                                             @Override public void run()
                                                             {
                                                                 finishFailedOrAbandonedTransactions();
                                                             }
                                                         },
                                transactionConfiguration.getFinishTransactionsIntervalInSeconds() * 1000L,
                                transactionConfiguration.getFinishTransactionsIntervalInSeconds() * 1000L);
                    } else
                    {
                        operationLog.info(
                                "Transactions are disabled in service.properties file. No transactions will be recovered from the transaction log. No tasks will be scheduled to periodically finish failed or abandoned transactions.");
                    }
                }
            } else if (event instanceof ContextClosedEvent)
            {
                if (appContext.getParent() == null && finishTransactionsTimer != null)
                {
                    finishTransactionsTimer.cancel();
                }
            }
        }
    }

    protected void checkTransactionsEnabled()
    {
        if (!transactionConfiguration.isEnabled())
        {
            RuntimeException e = new RuntimeException("Transactions are disabled in service.properties file.");
            operationLog.warn(e.getMessage(), e);
            throw e;
        }
    }

    public TransactionConfiguration getTransactionConfiguration()
    {
        return transactionConfiguration;
    }

}
