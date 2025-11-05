package ch.ethz.sis.openbis.afsserver.server.messages;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.messages.db.Message;
import ch.systemsx.cisd.common.collection.CollectionUtils;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IDatasetLocation;
import lombok.Data;
import lombok.Getter;

public class DeleteDataSetFromStoreMessage
{

    public static final String TYPE = "afs.common.deleteDataSetFromStore";

    @Getter
    private final String processId;

    @Getter
    private final List<? extends IDatasetLocation> dataSets;

    @Getter
    private final int maxNumberOfRetries;

    @Getter
    private final long waitingTimeBetweenRetries;

    public DeleteDataSetFromStoreMessage(final String processId, final List<? extends IDatasetLocation> dataSets, final int maxNumberOfRetries,
            final long waitingTimeBetweenRetries)
    {
        if (processId == null)
        {
            throw new IllegalArgumentException();
        }
        if (dataSets == null)
        {
            throw new IllegalArgumentException();
        }

        this.processId = processId;
        this.dataSets = dataSets;
        this.maxNumberOfRetries = maxNumberOfRetries;
        this.waitingTimeBetweenRetries = waitingTimeBetweenRetries;
    }

    public Message serialize(JsonObjectMapper objectMapper)
    {
        Message message = new Message();
        message.setType(TYPE);
        message.setDescription("Delete datasets " + CollectionUtils.abbreviate(dataSets, CollectionUtils.DEFAULT_MAX_LENGTH,
                IDatasetLocation::getDataSetCode)
                + " from store");
        message.setProcessId(processId);
        message.setCreationTimestamp(new Date());

        try
        {
            MetaData metaData = new MetaData();
            metaData.setDataSets(new ArrayList<>(dataSets));
            metaData.setMaxNumberOfRetries(maxNumberOfRetries);
            metaData.setWaitingTimeBetweenRetries(waitingTimeBetweenRetries);
            message.setMetaData(new String(objectMapper.writeValue(metaData)));
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return message;
    }

    public static DeleteDataSetFromStoreMessage deserialize(JsonObjectMapper objectMapper, Message message)
    {
        try
        {
            MetaData metaData = objectMapper.readValue(new ByteArrayInputStream(message.getMetaData().getBytes()), MetaData.class);
            return new DeleteDataSetFromStoreMessage(message.getProcessId(), metaData.dataSets, metaData.maxNumberOfRetries,
                    metaData.waitingTimeBetweenRetries);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Data
    private static class MetaData
    {

        private List<? extends IDatasetLocation> dataSets;

        private int maxNumberOfRetries;

        private long waitingTimeBetweenRetries;

    }

}
