#   Copyright ETH 2025 Zürich, Scientific IT Services
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#

import ch.systemsx.cisd.openbis.generic.server.ComponentNames as ComponentNames
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider as CommonServiceProvider
import ch.ethz.sis.openbis.generic.server.xls.export.XLSExportExtendedService as XLSExportExtendedService
import ch.systemsx.cisd.common.exceptions.UserFailureException as UserFailureException
import json
import re
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.AttributeValidator as AttributeValidator
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.SampleImportHelper as SampleImportHelper
import ch.systemsx.cisd.common.logging.LogCategory as LogCategory
import ch.systemsx.cisd.common.logging.LogFactory as LogFactory


OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, LogFactory)



def process(context, parameters):
    method = parameters.get("method")
    result = None

    if method == "unarchive":
        result = unarchive(context, parameters)

    return result


def unarchive(context, parameters):

    permId = parameters.get("permId")
    OPERATION_LOG.info("unarchive of AFS data: '"+permId+"' - START")

    from ch.ethz.sis.messages.db import MessagesDatabase
    from ch.systemsx.cisd.dbmigration import DatabaseConfigurationContext;
    from ch.ethz.sis.messages.process import MessageProcessId;
    from ch.ethz.sis.openbis.messages import UnarchiveDataSetMessage;
    from ch.ethz.sis.afsjson.jackson import JacksonObjectMapper;
    from java.util import List;

    messagesDatabaseConfiguration = CommonServiceProvider.getMessagesDatabaseConfigurationContext();
    messagesDatabase = MessagesDatabase(messagesDatabaseConfiguration.getDataSource());

    messagesDatabase.begin();
    unarchiveMessage = UnarchiveDataSetMessage(MessageProcessId.getCurrentOrGenerateNew(), List.of(permId));
    messagesDatabase.getMessagesDAO().create(unarchiveMessage.serialize(JacksonObjectMapper.getInstance()));
    messagesDatabase.commit();
    OPERATION_LOG.info("unarchive of AFS data: '"+permId+"' - END")

    result = {
        "status" : "OK",
    }
    return result

