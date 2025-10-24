package ch.ethz.sis.openbis.afsserver.server.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ch.ethz.sis.openbis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.messages.consumer.IMessageHandler;
import ch.ethz.sis.messages.consumer.MessagesConsumer;
import ch.ethz.sis.messages.db.MessagesDatabase;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.maintenance.IMaintenanceTask;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import ch.systemsx.cisd.common.reflection.ClassUtils;

public class MessagesConsumerMaintenanceTask implements IMaintenanceTask
{

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private MessagesConsumer consumer;

    @Override public void setUp(final String pluginName, final Properties properties)
    {
        String consumerId = PropertyUtils.getProperty(properties, "consumerId");
        int batchSize = PropertyUtils.getInt(properties, "batchSize", DEFAULT_BATCH_SIZE);

        List<String> handlerClassNames = PropertyUtils.tryGetListInOriginalCase(properties, "handlers");
        List<IMessageHandler> handlerInstances = new ArrayList<>();

        if (handlerClassNames != null)
        {
            for (String handlerClassName : handlerClassNames)
            {
                handlerInstances.add(ClassUtils.create(IMessageHandler.class, handlerClassName));
            }
        }

        Configuration configuration = ServiceProvider.getInstance().getConfiguration();
        MessagesDatabaseConfiguration messagesDatabaseConfiguration = MessagesDatabaseConfiguration.getInstance(configuration);
        this.consumer =
                new MessagesConsumer(consumerId, handlerInstances, batchSize, new MessagesDatabase(messagesDatabaseConfiguration.getDataSource()));
    }

    @Override public void execute()
    {
        consumer.consume();
    }
}
