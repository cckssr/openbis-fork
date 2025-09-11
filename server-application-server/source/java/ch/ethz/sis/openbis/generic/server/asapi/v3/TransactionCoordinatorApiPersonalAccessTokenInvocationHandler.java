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
package ch.ethz.sis.openbis.generic.server.asapi.v3;

import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import ch.ethz.sis.openbis.generic.asapi.v3.ITransactionCoordinatorApi;
import ch.ethz.sis.transaction.TransactionCoordinator;
import ch.ethz.sis.transaction.api.TransactionOperationException;
import ch.systemsx.cisd.openbis.common.pat.IPersonalAccessTokenInvocation;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.systemsx.cisd.openbis.generic.shared.pat.IPersonalAccessTokenConverter;

public class TransactionCoordinatorApiPersonalAccessTokenInvocationHandler
        implements ITransactionCoordinatorApi, ITransactionCoordinatorInternalApi, ApplicationListener<ApplicationEvent>
{

    private final IPersonalAccessTokenInvocation invocation;

    private final IPersonalAccessTokenConverter converter;

    public TransactionCoordinatorApiPersonalAccessTokenInvocationHandler(final IPersonalAccessTokenInvocation invocation)
    {
        this.invocation = invocation;
        this.converter = CommonServiceProvider.getPersonalAccessTokenConverter();
    }

    @Override public int getMajorVersion()
    {
        return invocation.proceedWithOriginalArguments();
    }

    @Override public int getMinorVersion()
    {
        return invocation.proceedWithOriginalArguments();
    }

    @Override public void beginTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey)
    {
        invocation.proceedWithNewNthArgument(1, converter.convert(sessionToken));
    }

    @Override public <T> T executeOperation(final UUID transactionId, final String sessionToken, final String interactiveSessionKey,
            final String participantId,
            final String operationName, final Object[] operationArguments) throws TransactionOperationException
    {
        return invocation.proceedWithNewNthArgument(1, converter.convert(sessionToken));
    }

    @Override public void commitTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey)
    {
        invocation.proceedWithNewNthArgument(1, converter.convert(sessionToken));
    }

    @Override public void rollbackTransaction(final UUID transactionId, final String sessionToken, final String interactiveSessionKey)
    {
        invocation.proceedWithNewNthArgument(1, converter.convert(sessionToken));
    }

    @Override public Map<UUID, TransactionCoordinator.Transaction> getTransactionMap()
    {
        return invocation.proceedWithOriginalArguments();
    }

    @Override public void onApplicationEvent(final ApplicationEvent event)
    {
        invocation.proceedWithOriginalArguments();
    }

}
