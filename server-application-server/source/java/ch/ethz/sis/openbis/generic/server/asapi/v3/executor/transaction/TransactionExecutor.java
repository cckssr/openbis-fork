package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionExecutor implements ITransactionExecutor
{

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeInSeparateTransaction(Runnable runnable) throws Exception
    {
        runnable.run();
    }
}
