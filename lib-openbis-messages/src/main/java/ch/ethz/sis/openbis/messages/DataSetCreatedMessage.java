package ch.ethz.sis.openbis.messages;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.messages.db.Message;
import ch.systemsx.cisd.common.collection.CollectionUtils;
import lombok.Data;
import lombok.Getter;

public class DataSetCreatedMessage
{

    public static final String TYPE = "openbis.common.dataSetCreated";

    @Getter
    private final String processId;

    @Getter
    private final List<String> dataSetCodes;

    public DataSetCreatedMessage(final String processId, final List<String> dataSetCodes)
    {
        if (processId == null)
        {
            throw new IllegalArgumentException();
        }
        if (dataSetCodes == null)
        {
            throw new IllegalArgumentException();
        }

        this.processId = processId;
        this.dataSetCodes = dataSetCodes;
    }

    public Message serialize(JsonObjectMapper objectMapper)
    {
        Message message = new Message();
        message.setType(TYPE);
        message.setDescription(
                "Created " + CollectionUtils.abbreviate(dataSetCodes, CollectionUtils.DEFAULT_MAX_LENGTH) + " data sets");
        message.setProcessId(processId);
        message.setCreationTimestamp(new Date());

        DataSetCreatedMessage.MetaData metaData = new DataSetCreatedMessage.MetaData();
        metaData.setDataSetCodes(new ArrayList<>(dataSetCodes));

        try
        {
            message.setMetaData(new String(objectMapper.writeValue(metaData)));
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return message;
    }

    public static DataSetCreatedMessage deserialize(JsonObjectMapper objectMapper, Message message)
    {
        try
        {
            DataSetCreatedMessage.MetaData
                    metaData =
                    objectMapper.readValue(new ByteArrayInputStream(message.getMetaData().getBytes()), DataSetCreatedMessage.MetaData.class);
            return new DataSetCreatedMessage(message.getProcessId(), metaData.getDataSetCodes());
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Data
    private static class MetaData
    {
        private List<String> dataSetCodes;
    }

}
