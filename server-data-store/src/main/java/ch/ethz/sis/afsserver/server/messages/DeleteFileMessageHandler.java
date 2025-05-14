package ch.ethz.sis.afsserver.server.messages;

import java.io.File;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.messages.consumer.IMessageHandler;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.filesystem.FileOperations;
import ch.systemsx.cisd.common.filesystem.IFileOperations;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;

public class DeleteFileMessageHandler implements IMessageHandler
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, DeleteFileMessageHandler.class);

    @Override public Set<String> getSupportedMessageTypes()
    {
        return Set.of(DeleteFileMessage.TYPE);
    }

    @Override public void handleMessage(final Message message)
    {
        Configuration configuration = ServiceProvider.getInstance().getConfiguration();
        JsonObjectMapper jsonObjectMapper = AtomicFileSystemServerParameterUtil.getJsonObjectMapper(configuration);
        DeleteFileMessage deleteMessage = DeleteFileMessage.deserialize(jsonObjectMapper, message);

        if (delete(deleteMessage.getFile()))
        {
            operationLog.info("Successfully deleted file: " + deleteMessage.getFile());
        } else
        {
            operationLog.error("Could not delete file: " + deleteMessage.getFile());
        }
    }

    private boolean delete(File file)
    {
        try
        {
            IFileOperations fileOperations = FileOperations.getMonitoredInstanceForCurrentThread();
            if (fileOperations.delete(file))
            {
                return true;
            }
            return !fileOperations.exists(file);
        } catch (Throwable t)
        {
            return false;
        }
    }
}
