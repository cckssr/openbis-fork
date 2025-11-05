package ch.ethz.sis.openbis.afsserver.server.messages;

import java.io.File;
import java.util.Date;

import org.junit.Test;
import org.testng.Assert;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsjson.jackson.JacksonObjectMapper;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.process.MessageProcessId;
import ch.ethz.sis.openbis.afsserver.server.messages.DeleteFileMessage;

public class DeleteFileMessageTest
{

    @Test
    public void testSerializeAndDeserialize()
    {
        JsonObjectMapper objectMapper = new JacksonObjectMapper();

        File file = new File("/test/file");

        DeleteFileMessage originalMessage =
                new DeleteFileMessage(MessageProcessId.getCurrentOrGenerateNew(), file, new Date(), 1234L, "test-email", "test-template",
                        "test-subject", "test-from-email");
        Message message = originalMessage.serialize(objectMapper);
        DeleteFileMessage deserializedMessage = DeleteFileMessage.deserialize(objectMapper, message);

        Assert.assertEquals(deserializedMessage.getProcessId(), originalMessage.getProcessId());
        Assert.assertEquals(deserializedMessage.getFile(), originalMessage.getFile());
        Assert.assertEquals(deserializedMessage.getTimestamp(), originalMessage.getTimestamp());
        Assert.assertEquals(deserializedMessage.getTimeout(), originalMessage.getTimeout());
        Assert.assertEquals(deserializedMessage.getEmail(), originalMessage.getEmail());
        Assert.assertEquals(deserializedMessage.getEmailFromAddress(), originalMessage.getEmailFromAddress());
        Assert.assertEquals(deserializedMessage.getEmailTemplate(), originalMessage.getEmailTemplate());
        Assert.assertEquals(deserializedMessage.getEmailSubject(), originalMessage.getEmailSubject());
    }

}
