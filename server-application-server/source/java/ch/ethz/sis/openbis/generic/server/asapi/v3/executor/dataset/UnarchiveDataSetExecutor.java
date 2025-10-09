/*
 * Copyright ETH 2014 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.dataset;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.ethz.sis.afsjson.jackson.JacksonObjectMapper;
import ch.ethz.sis.messages.db.MessagesDatabase;
import ch.ethz.sis.messages.db.MessagesDatabaseUtil;
import ch.ethz.sis.messages.process.MessageProcessId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.IDataSetId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.unarchive.DataSetUnarchiveOptions;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.utils.DataSetUtils;
import ch.ethz.sis.openbis.messages.UnarchiveDataSetMessage;
import ch.systemsx.cisd.dbmigration.DatabaseConfigurationContext;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.systemsx.cisd.openbis.generic.server.business.bo.IDataSetTable;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.shared.dto.DataPE;

/**
 * @author pkupczyk
 */
@Component
public class UnarchiveDataSetExecutor extends AbstractArchiveUnarchiveDataSetExecutor implements IUnarchiveDataSetExecutor
{

    @Autowired
    private IDataSetAuthorizationExecutor authorizationExecutor;

    @Autowired
    private IDAOFactory daoFactory;

    @Override
    public void unarchive(final IOperationContext context, final List<? extends IDataSetId> dataSetIds, final DataSetUnarchiveOptions options)
    {
        DataSetUtils.executeWithAfsDataVisible(daoFactory, dataSetIds, () ->
        {
            doArchiveUnarchive(context, dataSetIds, options, new IDssArchiveUnarchiveAction()
            {
                @Override
                public void execute(IDataSetTable dataSetTable)
                {
                    dataSetTable.unarchiveDatasets();
                }
            }, new IAfsArchiveUnarchiveAction()
            {
                @Override public void execute(final List<String> dataSetCodes)
                {
                    DatabaseConfigurationContext messagesDatabaseConfiguration = CommonServiceProvider.getMessagesDatabaseConfigurationContext();
                    MessagesDatabase messagesDatabase = new MessagesDatabase(messagesDatabaseConfiguration.getDataSource());

                    UnarchiveDataSetMessage unarchiveMessage = new UnarchiveDataSetMessage(MessageProcessId.getCurrentOrGenerateNew(), dataSetCodes);
                    MessagesDatabaseUtil.execute(messagesDatabase, () ->
                    {
                        messagesDatabase.getMessagesDAO().create(unarchiveMessage.serialize(JacksonObjectMapper.getInstance()));
                        return null;
                    });
                }
            });
            return null;
        });
    }

    @Override
    protected void assertAuthorization(IOperationContext context, IDataSetId dataSetId, DataPE dataSet)
    {
        authorizationExecutor.canUnarchive(context, dataSetId, dataSet);
    }

}
