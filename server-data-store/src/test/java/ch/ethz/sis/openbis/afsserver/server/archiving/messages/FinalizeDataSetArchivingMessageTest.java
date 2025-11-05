package ch.ethz.sis.openbis.afsserver.server.archiving.messages;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.testng.Assert;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsjson.jackson.JacksonObjectMapper;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.messages.process.MessageProcessId;
import ch.ethz.sis.openbis.afsserver.server.archiving.messages.FinalizeDataSetArchivingMessage;
import ch.systemsx.cisd.common.utilities.SystemTimeProvider;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.MultiDataSetArchivingFinalizer;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

public class FinalizeDataSetArchivingMessageTest
{

    @Test
    public void testSerializeAndDeserialize()
    {
        JsonObjectMapper objectMapper = new JacksonObjectMapper();

        Properties cleanerProperties = new Properties();
        cleanerProperties.setProperty("test-property", "test-value");

        File pauseFile = new File("test/path");
        long pauseFilePollingTime = 1000;

        MultiDataSetArchivingFinalizer task = new MultiDataSetArchivingFinalizer(cleanerProperties, pauseFile, pauseFilePollingTime,
                SystemTimeProvider.SYSTEM_TIME_PROVIDER);

        Map<String, String> parameterBindings = new HashMap<>();
        parameterBindings.put("test-key-1", "test-value-1");
        parameterBindings.put("test-key-2", "test-value-2");

        DatasetDescription description1 = new DatasetDescription();
        description1.setDataSetCode("test-code-1");

        DatasetDescription description2 = new DatasetDescription();
        description1.setDataSetCode("test-code-2");

        FinalizeDataSetArchivingMessage originalMessage =
                new FinalizeDataSetArchivingMessage(MessageProcessId.getCurrentOrGenerateNew(), task, parameterBindings,
                        Arrays.asList(description1, description2));
        Message message = originalMessage.serialize(objectMapper);
        FinalizeDataSetArchivingMessage deserializedMessage = FinalizeDataSetArchivingMessage.deserialize(objectMapper, message);

        Assert.assertEquals(deserializedMessage.getProcessId(), originalMessage.getProcessId());
        Assert.assertEquals(deserializedMessage.getTask().getCleanerProperties(), originalMessage.getTask().getCleanerProperties());
        Assert.assertEquals(deserializedMessage.getTask().getPauseFile(), originalMessage.getTask().getPauseFile());
        Assert.assertEquals(deserializedMessage.getTask().getPauseFilePollingTime(),
                originalMessage.getTask().getPauseFilePollingTime());
        Assert.assertEquals(deserializedMessage.getDataSets(), originalMessage.getDataSets());
        Assert.assertEquals(deserializedMessage.getParameterBindings(), originalMessage.getParameterBindings());
    }

}
