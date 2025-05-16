package ch.ethz.sis.afsserver.server.messages;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Date;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.messages.db.Message;
import lombok.Data;
import lombok.Getter;

public class DeleteFileMessage
{
    public static final String TYPE = "afs.common.deleteFile";

    @Getter
    private final String processId;

    @Getter
    private final File file;

    @Getter
    private final Date timestamp;

    @Getter
    private final long timeout;

    @Getter
    private final String email;

    @Getter
    private final String emailTemplate;

    @Getter
    private final String emailSubject;

    @Getter
    private final String emailFromAddress;

    public DeleteFileMessage(final String processId, final File file, final Date timestamp, final long timeout, String email, String emailTemplate,
            String emailSubject, String emailFromAddress)
    {
        if (processId == null)
        {
            throw new IllegalArgumentException();
        }
        if (file == null)
        {
            throw new IllegalArgumentException();
        }

        this.processId = processId;
        this.file = file;
        this.timestamp = timestamp;
        this.timeout = timeout;
        this.email = email;
        this.emailTemplate = emailTemplate;
        this.emailSubject = emailSubject;
        this.emailFromAddress = emailFromAddress;
    }

    public Message serialize(JsonObjectMapper objectMapper)
    {
        Message message = new Message();
        message.setType(TYPE);
        message.setDescription("Delete file " + file.getAbsolutePath());
        message.setProcessId(processId);
        message.setCreationTimestamp(new Date());

        try
        {
            MetaData metaData = new MetaData();
            metaData.setFile(file.getAbsolutePath());
            metaData.setTimestamp(timestamp);
            metaData.setTimeout(timeout);
            metaData.setEmail(email);
            metaData.setEmailTemplate(emailTemplate);
            metaData.setEmailSubject(emailSubject);
            metaData.setEmailFromAddress(emailFromAddress);
            message.setMetaData(new String(objectMapper.writeValue(metaData)));
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return message;
    }

    public static DeleteFileMessage deserialize(JsonObjectMapper objectMapper, Message message)
    {
        try
        {
            MetaData metaData = objectMapper.readValue(new ByteArrayInputStream(message.getMetaData().getBytes()), MetaData.class);
            return new DeleteFileMessage(message.getProcessId(), new File(metaData.getFile()), metaData.getTimestamp(), metaData.getTimeout(),
                    metaData.getEmail(), metaData.getEmailTemplate(), metaData.getEmailSubject(), metaData.getEmailFromAddress());
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Data
    private static class MetaData
    {

        private String file;

        private Date timestamp;

        private long timeout;

        private String email;

        private String emailTemplate;

        private String emailSubject;

        private String emailFromAddress;

    }
}
