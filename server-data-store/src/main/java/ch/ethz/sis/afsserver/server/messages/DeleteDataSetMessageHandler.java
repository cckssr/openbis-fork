package ch.ethz.sis.afsserver.server.messages;

import java.util.Set;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.messages.consumer.IMessageHandler;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.openbis.dss.generic.server.DeletionCommand;

public class DeleteDataSetMessageHandler implements IMessageHandler
{
    @Override public Set<String> getSupportedMessageTypes()
    {
        return Set.of(DeleteDataSetMessage.TYPE);
    }

    @Override public void handleMessage(final Message message)
    {
        Configuration configuration = ServiceProvider.getInstance().getConfiguration();
        JsonObjectMapper jsonObjectMapper = AtomicFileSystemServerParameterUtil.getJsonObjectMapper(configuration);
        DeleteDataSetMessage deleteMessage = DeleteDataSetMessage.deserialize(jsonObjectMapper, message);
        DeletionCommand deleteCommand =
                new DeletionCommand(deleteMessage.getDataSets(), deleteMessage.getMaxNumberOfRetries(), deleteMessage.getWaitingTimeBetweenRetries());
        deleteCommand.execute(ServiceProvider.getInstance().getHierarchicalContentProvider(),
                ServiceProvider.getInstance().getDataSetDirectoryProvider());
    }
}
