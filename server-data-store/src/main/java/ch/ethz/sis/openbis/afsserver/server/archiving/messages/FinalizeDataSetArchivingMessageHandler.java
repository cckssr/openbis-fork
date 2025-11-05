package ch.ethz.sis.openbis.afsserver.server.archiving.messages;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ethz.sis.shared.log.classic.impl.Logger;

import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.openbis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.messages.consumer.IMessageHandler;
import ch.ethz.sis.messages.db.Message;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.collection.CollectionUtils;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;

public class FinalizeDataSetArchivingMessageHandler implements IMessageHandler
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, FinalizeDataSetArchivingMessageHandler.class);

    @Override public Set<String> getSupportedMessageTypes()
    {
        return Set.of(FinalizeDataSetArchivingMessage.TYPE);
    }

    @Override public void handleMessage(final Message message)
    {
        Configuration configuration = ServiceProvider.getInstance().getConfiguration();
        JsonObjectMapper jsonObjectMapper = AtomicFileSystemServerParameterUtil.getJsonObjectMapper(configuration);
        FinalizeDataSetArchivingMessage finalizeArchivingMessage = FinalizeDataSetArchivingMessage.deserialize(jsonObjectMapper, message);
        List<DatasetDescription> dataSets = finalizeArchivingMessage.getDataSets();

        try
        {
            ServiceProvider.getInstance().getShareIdManager().lock(codesList(dataSets));
            finalizeArchivingMessage.getTask().process(dataSets, finalizeArchivingMessage.getParameterBindings());
        } catch (Exception e)
        {
            operationLog.error(
                    "Archiving finalization failed for data sets " + CollectionUtils.abbreviate(codesList(dataSets),
                            CollectionUtils.DEFAULT_MAX_LENGTH), e);
        } finally
        {
            ServiceProvider.getInstance().getShareIdManager().releaseLocks(codesList(dataSets));
        }
    }

    private List<String> codesList(List<DatasetDescription> dataSets)
    {
        return dataSets.stream().map(DatasetDescription::getDataSetCode).collect(Collectors.toList());
    }

}
