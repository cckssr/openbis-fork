package ch.ethz.sis.afsserver.server.archiving.messages;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.testng.Assert;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsjson.jackson.JacksonObjectMapper;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.process.MessageProcessId;

public class ArchiveDataSetMessageTest
{

    @Test
    public void testSerializeAndDeserialize()
    {
        JsonObjectMapper objectMapper = new JacksonObjectMapper();

        List<String> dataSetCodes = Arrays.asList("test-code-1", "test-code-2");
        boolean removeFromDataStore = true;
        Map<String, String> options = new HashMap<>();
        options.put("test-option-1", "test-value-1");
        options.put("test-option-2", "test-value-2");

        ArchiveDataSetMessage originalMessage =
                new ArchiveDataSetMessage(MessageProcessId.getCurrentOrGenerateNew(), dataSetCodes, removeFromDataStore, options);
        Message message = originalMessage.serialize(objectMapper);
        ArchiveDataSetMessage deserializedMessage = ArchiveDataSetMessage.deserialize(objectMapper, message);

        Assert.assertEquals(deserializedMessage.getProcessId(), originalMessage.getProcessId());
        Assert.assertEquals(deserializedMessage.getDataSetCodes(), originalMessage.getDataSetCodes());
        Assert.assertEquals(deserializedMessage.isRemoveFromDataStore(), originalMessage.isRemoveFromDataStore());
        Assert.assertEquals(deserializedMessage.getOptions(), originalMessage.getOptions());
    }

}
