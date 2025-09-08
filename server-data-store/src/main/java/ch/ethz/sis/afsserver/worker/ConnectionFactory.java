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
package ch.ethz.sis.afsserver.worker;

import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.afs.manager.TransactionManager;
import ch.ethz.sis.afs.startup.AtomicFileSystemParameter;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.shared.pool.AbstractFactory;
import ch.ethz.sis.shared.startup.Configuration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class ConnectionFactory extends AbstractFactory<Configuration, Configuration, TransactionConnection>
{

    private TransactionManager transactionManager;

    @Override
    public void init(Configuration configuration) throws Exception
    {
        String enabledPreviewTypeList = configuration.getStringProperty(AtomicFileSystemParameter.enablePreview);
        Collection<String> enabledPreviewTypes = enabledPreviewTypeList != null ? Arrays.asList(enabledPreviewTypeList.split(",")) : Collections.emptyList();
        long enablePreviewSizeInBytes = configuration.getIntegerProperty(AtomicFileSystemParameter.enablePreviewSizeInBytes);

        transactionManager = new TransactionManager(
                configuration.getSharableInstance(AtomicFileSystemServerParameter.lockMapperClass),
                configuration.getSharableInstance(AtomicFileSystemServerParameter.jsonObjectMapperClass),
                configuration.getStringProperty(AtomicFileSystemServerParameter.writeAheadLogRoot),
                configuration.getStringProperty(AtomicFileSystemServerParameter.storageRoot),
                enabledPreviewTypes, enablePreviewSizeInBytes);
        transactionManager.reCommitTransactionsAfterCrash();
    }

    @Override
    public TransactionConnection create(Configuration configuration) throws Exception
    {
        return transactionManager.getTransactionConnection();
    }

    public TransactionManager getTransactionManager()
    {
        return transactionManager;
    }
}
