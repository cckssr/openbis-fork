package ch.ethz.sis.afsserver.server.archiving.messages;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.testng.Assert;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsjson.jackson.JacksonObjectMapper;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.process.MessageProcessId;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;

public class UpdateDataSetArchivingStatusMessageTest
{

    @Test
    public void testSerializeAndDeserialize()
    {
        JsonObjectMapper objectMapper = new JacksonObjectMapper();

        List<String> dataSetCodes = Arrays.asList("test-code-1", "test-code-2");
        DataSetArchivingStatus archivingStatus = DataSetArchivingStatus.ARCHIVED;
        boolean presentInArchive = true;

        UpdateDataSetArchivingStatusMessage originalMessage =
                new UpdateDataSetArchivingStatusMessage(MessageProcessId.getCurrentOrGenerateNew(), dataSetCodes, archivingStatus, presentInArchive);
        Message message = originalMessage.serialize(objectMapper);
        UpdateDataSetArchivingStatusMessage deserializedMessage = UpdateDataSetArchivingStatusMessage.deserialize(objectMapper, message);

        Assert.assertEquals(deserializedMessage.getProcessId(), originalMessage.getProcessId());
        Assert.assertEquals(deserializedMessage.getDataSetCodes(), originalMessage.getDataSetCodes());
        Assert.assertEquals(deserializedMessage.getArchivingStatus(), originalMessage.getArchivingStatus());
        Assert.assertEquals(deserializedMessage.getPresentInArchive(), originalMessage.getPresentInArchive());
    }

}
