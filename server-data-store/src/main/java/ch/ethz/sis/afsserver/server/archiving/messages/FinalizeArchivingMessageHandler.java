package ch.ethz.sis.afsserver.server.archiving.messages;

import java.util.Set;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.messages.consumer.IMessageHandler;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.shared.startup.Configuration;

public class FinalizeArchivingMessageHandler implements IMessageHandler
{

    @Override public Set<String> getSupportedMessageTypes()
    {
        return Set.of(FinalizeArchivingMessage.TYPE);
    }

    @Override public void handleMessage(final Message message)
    {
        Configuration configuration = ServiceProvider.getInstance().getConfiguration();
        JsonObjectMapper jsonObjectMapper = AtomicFileSystemServerParameterUtil.getJsonObjectMapper(configuration);
        FinalizeArchivingMessage finalizeArchivingMessage = FinalizeArchivingMessage.deserialize(jsonObjectMapper, message);
        finalizeArchivingMessage.getTask().process(finalizeArchivingMessage.getDataSets(), finalizeArchivingMessage.getParameterBindings());
    }
}
