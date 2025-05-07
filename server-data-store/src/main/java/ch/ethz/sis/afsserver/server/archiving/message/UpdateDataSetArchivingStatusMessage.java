package ch.ethz.sis.afsserver.server.archiving.message;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.messagesdb.Message;
import ch.systemsx.cisd.common.collection.CollectionUtils;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;

public class UpdateDataSetArchivingStatusMessage
{

    private static final String TYPE = "afs.delete.dataSets";

    private final List<String> dataSetCodes;

    private final DataSetArchivingStatus status;

    private final boolean presentInArchive;

    public UpdateDataSetArchivingStatusMessage(final List<String> dataSetCodes, final DataSetArchivingStatus status, final boolean presentInArchive)
    {
        this.dataSetCodes = dataSetCodes;
        this.status = status;
        this.presentInArchive = presentInArchive;
    }

    public Message serialize(JsonObjectMapper objectMapper)
    {
        Message message = new Message();
        message.setType(TYPE);
        message.setDescription("Update archiving status of " + CollectionUtils.abbreviate(dataSetCodes, CollectionUtils.DEFAULT_MAX_LENGTH)
                + " data sets");
        message.setCreationTimestamp(new Date());

        MetaData metaData = new MetaData();
        metaData.dataSetCodes = dataSetCodes;
        metaData.status = status;
        metaData.presentInArchive = presentInArchive;

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

    public static UpdateDataSetArchivingStatusMessage deserialize(JsonObjectMapper objectMapper, Message message)
    {
        try
        {
            MetaData metaData = objectMapper.readValue(new ByteArrayInputStream(message.getMetaData().getBytes()), MetaData.class);
            return new UpdateDataSetArchivingStatusMessage(metaData.dataSetCodes, metaData.status, metaData.presentInArchive);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static class MetaData
    {

        private List<String> dataSetCodes;

        private DataSetArchivingStatus status;

        private boolean presentInArchive;

    }
}
