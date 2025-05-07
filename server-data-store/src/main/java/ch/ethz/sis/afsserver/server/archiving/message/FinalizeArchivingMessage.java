package ch.ethz.sis.afsserver.server.archiving.message;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.messagesdb.Message;
import ch.systemsx.cisd.common.collection.CollectionUtils;
import ch.systemsx.cisd.common.utilities.SystemTimeProvider;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.MultiDataSetArchivingFinalizer;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

public class FinalizeArchivingMessage
{

    private static final String TYPE = "afs.archiving.finalize";

    private final MultiDataSetArchivingFinalizer task;

    private final Map<String, String> parameterBindings;

    private final List<DatasetDescription> dataSets;

    public FinalizeArchivingMessage(MultiDataSetArchivingFinalizer task, Map<String, String> parameterBindings,
            List<DatasetDescription> dataSets)
    {
        if (task == null)
        {
            throw new IllegalArgumentException();
        }
        if (parameterBindings == null)
        {
            throw new IllegalArgumentException();
        }
        if (dataSets == null)
        {
            throw new IllegalArgumentException();
        }

        this.task = task;
        this.parameterBindings = parameterBindings;
        this.dataSets = dataSets;
    }

    public Message serialize(JsonObjectMapper objectMapper)
    {
        Message message = new Message();
        message.setType(TYPE);
        message.setDescription(
                "Finalize archiving of " + CollectionUtils.abbreviate(dataSets, 10, DatasetDescription::getDataSetCode) + " data sets");
        message.setCreationTimestamp(new Date());

        MetaDataTask metaDataTask = new MetaDataTask();
        metaDataTask.pauseFilePollingTime = task.getPauseFilePollingTime();

        if (task.getCleanerProperties() != null)
        {
            Map<String, String> cleanerProperties = new HashMap<>();
            for (String propertyName : task.getCleanerProperties().stringPropertyNames())
            {
                cleanerProperties.put(propertyName, task.getCleanerProperties().getProperty(propertyName));
            }
            metaDataTask.cleanerProperties = cleanerProperties;
        }

        if (task.getPauseFile() != null)
        {
            metaDataTask.pauseFile = task.getPauseFile().getPath();
        }

        MetaData metaData = new MetaData();
        metaData.task = metaDataTask;
        metaData.parameterBindings = parameterBindings;
        metaData.dataSets = dataSets;

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

    public static FinalizeArchivingMessage deserialize(JsonObjectMapper objectMapper, Message message)
    {
        try
        {
            MetaData metaData = objectMapper.readValue(new ByteArrayInputStream(message.getMetaData().getBytes()), MetaData.class);

            Properties cleanerProperties = new Properties();
            if (metaData.task.cleanerProperties != null)
            {
                cleanerProperties.putAll(metaData.task.cleanerProperties);
            }

            File pauseFile = null;
            if (metaData.task.pauseFile != null)
            {
                pauseFile = new File(metaData.task.pauseFile);
            }

            MultiDataSetArchivingFinalizer task =
                    new MultiDataSetArchivingFinalizer(cleanerProperties, pauseFile, metaData.task.pauseFilePollingTime,
                            SystemTimeProvider.SYSTEM_TIME_PROVIDER);

            return new FinalizeArchivingMessage(task, metaData.parameterBindings, metaData.dataSets);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static class MetaData
    {

        private MetaDataTask task;

        private Map<String, String> parameterBindings;

        private List<DatasetDescription> dataSets;

    }

    private static class MetaDataTask
    {

        private String pauseFile;

        private long pauseFilePollingTime;

        private Map<String, String> cleanerProperties;

    }

}
