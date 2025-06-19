package ch.ethz.sis.openbis.messages;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsjson.jackson.JacksonObjectMapper;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.process.MessageProcessId;

public class UnarchiveDataSetMessageTest
{

    @Test
    public void testSerializeAndDeserialize()
    {
        JsonObjectMapper objectMapper = new JacksonObjectMapper();

        List<String> dataSetCodes = Arrays.asList("test-code-1", "test-code-2");

        UnarchiveDataSetMessage originalMessage =
                new UnarchiveDataSetMessage(MessageProcessId.getCurrentOrGenerateNew(), dataSetCodes);
        Message message = originalMessage.serialize(objectMapper);
        UnarchiveDataSetMessage deserializedMessage = UnarchiveDataSetMessage.deserialize(objectMapper, message);

        Assert.assertEquals(deserializedMessage.getProcessId(), originalMessage.getProcessId());
        Assert.assertEquals(deserializedMessage.getDataSetCodes(), originalMessage.getDataSetCodes());
    }

}
