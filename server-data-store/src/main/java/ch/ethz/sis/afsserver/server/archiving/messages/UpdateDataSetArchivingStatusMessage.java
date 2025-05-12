package ch.ethz.sis.afsserver.server.archiving.messages;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.messages.db.Message;
import ch.systemsx.cisd.common.collection.CollectionUtils;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;
import lombok.Data;
import lombok.Getter;

public class UpdateDataSetArchivingStatusMessage
{

    public static final String TYPE = "afs.archiving.updateArchivingStatus";

    @Getter
    private final List<String> dataSetCodes;

    @Getter
    private final DataSetArchivingStatus archivingStatus;

    @Getter
    private final boolean presentInArchive;

    public UpdateDataSetArchivingStatusMessage(final List<String> dataSetCodes, final DataSetArchivingStatus archivingStatus,
            final boolean presentInArchive)
    {
        this.dataSetCodes = dataSetCodes;
        this.archivingStatus = archivingStatus;
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
        metaData.setDataSetCodes(dataSetCodes);
        metaData.setArchivingStatus(archivingStatus);
        metaData.setPresentInArchive(presentInArchive);

        try
        {
            message.setMetaData(new String(objectMapper.writeValue(metaData)));
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return message;
    }

    public static UpdateDataSetArchivingStatusMessage deserialize(JsonObjectMapper objectMapper, Message message)
    {
        try
        {
            MetaData metaData = objectMapper.readValue(new ByteArrayInputStream(message.getMetaData().getBytes()), MetaData.class);
            return new UpdateDataSetArchivingStatusMessage(metaData.getDataSetCodes(), metaData.getArchivingStatus(), metaData.isPresentInArchive());
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Data
    private static class MetaData
    {

        private List<String> dataSetCodes;

        private DataSetArchivingStatus archivingStatus;

        private boolean presentInArchive;

    }
}
