package ch.ethz.sis.afsserver.server.messages;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import ch.systemsx.cisd.common.mail.EMailAddress;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.common.string.Template;
import ch.systemsx.cisd.common.time.DateTimeUtils;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.FileDeleter;

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

            Date timeoutDate =
                    deleteMessage.getTimestamp() != null ? new Date(deleteMessage.getTimestamp().getTime() + deleteMessage.getTimeout()) : null;

            if (timeoutDate != null && timeoutDate.after(new Date()))
            {
                operationLog.error("Scheduling another attempt to delete file: " + deleteMessage.getFile());
                MessagesDatabaseFacade messagesDatabaseFacade = MessagesDatabaseConfiguration.getInstance(configuration).getMessagesDatabaseFacade();
                messagesDatabaseFacade.create(message);
            } else if (deleteMessage.getEmail() != null)
            {
                StringBuilder fileList = new StringBuilder();
                fileList.append(deleteMessage.getFile()).append("\n");
                fileList.append("Deletion requested at ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(deleteMessage.getTimestamp()));

                if (deleteMessage.getTimestamp() != null)
                {
                    fileList.append(" (")
                            .append(DateTimeUtils.renderDuration(System.currentTimeMillis() - deleteMessage.getTimestamp().getTime()))
                            .append(" elapsed)");
                }

                Template template = new Template(deleteMessage.getEmailTemplate());
                template.attemptToBind(FileDeleter.FILE_LIST_VARIABLE, fileList.toString());
                String emailMessage = template.createText();
                IMailClient mailClient = ServiceProvider.getInstance().createEMailClient();
                mailClient.sendEmailMessage(deleteMessage.getEmailSubject(), emailMessage, null,
                        new EMailAddress(deleteMessage.getEmailFromAddress()), new EMailAddress(deleteMessage.getEmail()));
            }
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
