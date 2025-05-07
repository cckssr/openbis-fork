package ch.ethz.sis.afsserver.server.messages;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.messagesdb.Message;
import ch.systemsx.cisd.common.collection.CollectionUtils;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IDatasetLocation;

public class DeleteDataSetMessage
{

    private static final String TYPE = "afs.delete.dataSets";

    private final List<? extends IDatasetLocation> dataSets;

    private final int maxNumberOfRetries;

    private final long waitingTimeBetweenRetries;

    public DeleteDataSetMessage(final List<? extends IDatasetLocation> dataSets, final int maxNumberOfRetries, final long waitingTimeBetweenRetries)
    {
        if (dataSets == null)
        {
            throw new IllegalArgumentException();
        }

        this.dataSets = dataSets;
        this.maxNumberOfRetries = maxNumberOfRetries;
        this.waitingTimeBetweenRetries = waitingTimeBetweenRetries;
    }

    public Message serialize(JsonObjectMapper objectMapper)
    {
        Message message = new Message();
        message.setType(TYPE);
        message.setDescription("Delete " + CollectionUtils.abbreviate(dataSets, CollectionUtils.DEFAULT_MAX_LENGTH, IDatasetLocation::getDataSetCode)
                + " data sets");
        message.setCreationTimestamp(new Date());

        MetaData metaData = new MetaData();
        metaData.dataSets = dataSets;
        metaData.maxNumberOfRetries = maxNumberOfRetries;
        metaData.waitingTimeBetweenRetries = waitingTimeBetweenRetries;

        try
        {
            message.setMetaData(new String(objectMapper.writeValue(metaData)));
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return message;
    }

    public static boolean canDeserialize(Message message)
    {
        return TYPE.equals(message.getType());
    }

    public static DeleteDataSetMessage deserialize(JsonObjectMapper objectMapper, Message message)
    {
        try
        {
            MetaData metaData = objectMapper.readValue(new ByteArrayInputStream(message.getMetaData().getBytes()), MetaData.class);
            return new DeleteDataSetMessage(metaData.dataSets, metaData.maxNumberOfRetries, metaData.waitingTimeBetweenRetries);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static class MetaData
    {

        private List<? extends IDatasetLocation> dataSets;

        private int maxNumberOfRetries;

        private long waitingTimeBetweenRetries;

    }
}
