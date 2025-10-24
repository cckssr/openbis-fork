package ch.ethz.sis.openbis.afsserver.server.messages;

import java.util.Arrays;

import org.junit.Test;
import org.testng.Assert;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsjson.jackson.JacksonObjectMapper;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.process.MessageProcessId;
import ch.ethz.sis.openbis.afsserver.server.messages.DeleteDataSetFromStoreMessage;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DatasetLocation;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

public class DeleteDataSetFromStoreMessageTest
{

    @Test
    public void testSerializeAndDeserialize()
    {
        JsonObjectMapper objectMapper = new JacksonObjectMapper();

        DatasetLocation location = new DatasetLocation();
        location.setDatasetCode("test-code-1");

        DatasetDescription description = new DatasetDescription();
        description.setDataSetCode("test-code-2");

        int maxNumberOfRetries = 1;
        long waitingTimeBetweenRetries = 100;

        DeleteDataSetFromStoreMessage originalMessage =
                new DeleteDataSetFromStoreMessage(MessageProcessId.getCurrentOrGenerateNew(), Arrays.asList(location, description),
                        maxNumberOfRetries,
                        waitingTimeBetweenRetries);
        Message message = originalMessage.serialize(objectMapper);
        DeleteDataSetFromStoreMessage deserializedMessage = DeleteDataSetFromStoreMessage.deserialize(objectMapper, message);

        Assert.assertEquals(deserializedMessage.getProcessId(), originalMessage.getProcessId());
        Assert.assertEquals(deserializedMessage.getDataSets(), originalMessage.getDataSets());
        Assert.assertEquals(deserializedMessage.getMaxNumberOfRetries(), originalMessage.getMaxNumberOfRetries());
        Assert.assertEquals(deserializedMessage.getWaitingTimeBetweenRetries(), originalMessage.getWaitingTimeBetweenRetries());
    }

}
