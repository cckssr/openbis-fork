package ch.ethz.sis.afsserver.server.messages;

import java.util.Arrays;

import org.junit.Test;
import org.testng.Assert;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsjson.jackson.JacksonObjectMapper;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.process.MessageProcessId;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DatasetLocation;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

public class DeleteDataSetMessageTest
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

        DeleteDataSetMessage originalDeleteMessage =
                new DeleteDataSetMessage(MessageProcessId.getCurrentOrGenerateNew(), Arrays.asList(location, description), maxNumberOfRetries,
                        waitingTimeBetweenRetries);
        Message message = originalDeleteMessage.serialize(objectMapper);
        DeleteDataSetMessage deserializedDeleteMessage = DeleteDataSetMessage.deserialize(objectMapper, message);

        Assert.assertEquals(deserializedDeleteMessage.getDataSets(), originalDeleteMessage.getDataSets());
        Assert.assertEquals(deserializedDeleteMessage.getMaxNumberOfRetries(), originalDeleteMessage.getMaxNumberOfRetries());
        Assert.assertEquals(deserializedDeleteMessage.getWaitingTimeBetweenRetries(), originalDeleteMessage.getWaitingTimeBetweenRetries());
    }

}
