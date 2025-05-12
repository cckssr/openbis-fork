package ch.ethz.sis.afsserver.server.archiving.messages;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.messages.db.Message;
import ch.systemsx.cisd.common.collection.CollectionUtils;
import ch.systemsx.cisd.common.utilities.SystemTimeProvider;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.MultiDataSetArchivingFinalizer;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;
import lombok.Data;
import lombok.Getter;

public class FinalizeDataSetArchivingMessage
{

    public static final String TYPE = "afs.archiving.finalizeDataSetArchiving";

    @Getter
    private final MultiDataSetArchivingFinalizer task;

    @Getter
    private final Map<String, String> parameterBindings;

    @Getter
    private final List<DatasetDescription> dataSets;

    public FinalizeDataSetArchivingMessage(MultiDataSetArchivingFinalizer task, Map<String, String> parameterBindings,
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
                "Finalize archiving of " + CollectionUtils.abbreviate(dataSets, CollectionUtils.DEFAULT_MAX_LENGTH,
                        DatasetDescription::getDataSetCode) + " data sets");
        message.setCreationTimestamp(new Date());

        MetaDataTask metaDataTask = new MetaDataTask();
        metaDataTask.setPauseFilePollingTime(task.getPauseFilePollingTime());

        if (task.getCleanerProperties() != null)
        {
            Map<String, String> cleanerProperties = new HashMap<>();
            for (String propertyName : task.getCleanerProperties().stringPropertyNames())
            {
                cleanerProperties.put(propertyName, task.getCleanerProperties().getProperty(propertyName));
            }
            metaDataTask.setCleanerProperties(cleanerProperties);
        }

        if (task.getPauseFile() != null)
        {
            metaDataTask.setPauseFile(task.getPauseFile().getPath());
        }

        MetaData metaData = new MetaData();
        metaData.setTask(metaDataTask);
        metaData.setParameterBindings(parameterBindings);
        metaData.setDataSets(dataSets);

        try
        {
            message.setMetaData(new String(objectMapper.writeValue(metaData)));
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return message;
    }

    public static FinalizeDataSetArchivingMessage deserialize(JsonObjectMapper objectMapper, Message message)
    {
        try
        {
            MetaData metaData = objectMapper.readValue(new ByteArrayInputStream(message.getMetaData().getBytes()), MetaData.class);

            Properties cleanerProperties = new Properties();
            if (metaData.getTask().getCleanerProperties() != null)
            {
                cleanerProperties.putAll(metaData.getTask().getCleanerProperties());
            }

            File pauseFile = null;
            if (metaData.getTask().getPauseFile() != null)
            {
                pauseFile = new File(metaData.getTask().getPauseFile());
            }

            MultiDataSetArchivingFinalizer task =
                    new MultiDataSetArchivingFinalizer(cleanerProperties, pauseFile, metaData.getTask().getPauseFilePollingTime(),
                            SystemTimeProvider.SYSTEM_TIME_PROVIDER);

            return new FinalizeDataSetArchivingMessage(task, metaData.getParameterBindings(), metaData.getDataSets());
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Data
    private static class MetaData
    {

        MetaDataTask task;

        Map<String, String> parameterBindings;

        List<DatasetDescription> dataSets;

    }

    @Data
    private static class MetaDataTask
    {

        private String pauseFile;

        private long pauseFilePollingTime;

        private Map<String, String> cleanerProperties;

    }

}
