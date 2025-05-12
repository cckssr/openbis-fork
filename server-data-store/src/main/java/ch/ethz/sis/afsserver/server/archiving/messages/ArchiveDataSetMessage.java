package ch.ethz.sis.afsserver.server.archiving.messages;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.messages.db.Message;
import ch.systemsx.cisd.common.collection.CollectionUtils;
import lombok.Data;
import lombok.Getter;

public class ArchiveDataSetMessage
{

    public static final String TYPE = "afs.archiving.archiveDataSet";

    @Getter
    private final List<String> dataSetCodes;

    @Getter
    private final boolean removeFromDataStore;

    @Getter
    private final Map<String, String> options;

    public ArchiveDataSetMessage(final List<String> dataSetCodes, final boolean removeFromDataStore, final Map<String, String> options)
    {
        if (dataSetCodes == null)
        {
            throw new IllegalArgumentException();
        }
        if (options == null)
        {
            throw new IllegalArgumentException();
        }

        this.dataSetCodes = dataSetCodes;
        this.removeFromDataStore = removeFromDataStore;
        this.options = options;
    }

    public Message serialize(JsonObjectMapper objectMapper)
    {
        Message message = new Message();
        message.setType(TYPE);
        message.setDescription(
                "Archive " + CollectionUtils.abbreviate(dataSetCodes, CollectionUtils.DEFAULT_MAX_LENGTH) + " data sets");
        message.setCreationTimestamp(new Date());

        MetaData metaData = new MetaData();
        metaData.setDataSetCodes(dataSetCodes);
        metaData.setRemoveFromDataStore(removeFromDataStore);
        metaData.setOptions(options);

        try
        {
            message.setMetaData(new String(objectMapper.writeValue(metaData)));
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return message;
    }

    public static ArchiveDataSetMessage deserialize(JsonObjectMapper objectMapper, Message message)
    {
        try
        {
            MetaData metaData = objectMapper.readValue(new ByteArrayInputStream(message.getMetaData().getBytes()), MetaData.class);
            return new ArchiveDataSetMessage(metaData.getDataSetCodes(), metaData.isRemoveFromDataStore(), metaData.getOptions());
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Data
    private static class MetaData
    {
        private List<String> dataSetCodes;

        private boolean removeFromDataStore;

        private Map<String, String> options;
    }

}
