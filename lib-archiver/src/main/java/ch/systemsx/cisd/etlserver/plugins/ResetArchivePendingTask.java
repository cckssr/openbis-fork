/*
 * Copyright ETH 2016 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.etlserver.plugins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import ch.systemsx.cisd.common.maintenance.IMaintenanceTask;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;
import ch.systemsx.cisd.openbis.generic.shared.dto.SimpleDataSetInformationDTO;

//
// This is how it looks on the logs
// 2016-05-24 13:02:02,551 INFO  [archive-task - Maintenance Plugin] OPERATION.ResetArchivePendingTask - ResetArchivePendingTask Started
// 2016-05-24 13:02:02,576 INFO  [archive-task - Maintenance Plugin] OPERATION.ResetArchivePendingTask - Found 3 datasets in ARCHIVE_PENDING status.
// 2016-05-24 13:02:02,576 INFO  [archive-task - Maintenance Plugin] OPERATION.ResetArchivePendingTask - Found 3 datasets in the command queue.
// 2016-05-24 13:02:02,576 INFO  [archive-task - Maintenance Plugin] OPERATION.ResetArchivePendingTask - Going to update 0 datasets.
// 2016-05-24 13:02:02,576 INFO  [archive-task - Maintenance Plugin] OPERATION.ResetArchivePendingTask - ResetArchivePendingTask Finished
//
// 2016-05-24 13:17:13,422 INFO  [archive-task - Maintenance Plugin] OPERATION.ResetArchivePendingTask - ResetArchivePendingTask Started
// 2016-05-24 13:17:13,443 INFO  [archive-task - Maintenance Plugin] OPERATION.ResetArchivePendingTask - Found 1 datasets in ARCHIVE_PENDING status.
// 2016-05-24 13:17:13,443 INFO  [archive-task - Maintenance Plugin] OPERATION.ResetArchivePendingTask - Found 0 datasets in the command queue.
// 2016-05-24 13:17:13,443 INFO  [archive-task - Maintenance Plugin] OPERATION.ResetArchivePendingTask - 20160523154603635-10 not found in command queue, scheduled to update.
// 2016-05-24 13:17:13,443 INFO  [archive-task - Maintenance Plugin] OPERATION.ResetArchivePendingTask - Going to update 1 datasets.
// 2016-05-24 13:17:13,444 INFO  [archive-task - Maintenance Plugin] OPERATION.ResetArchivePendingTask - ResetArchivePendingTask Finished
//

public class ResetArchivePendingTask implements IMaintenanceTask
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, ResetArchivePendingTask.class);

    @Override
    public void setUp(String pluginName, Properties properties)
    {
        operationLog.info("Task " + pluginName + " initialized.");
    }

    // @Transactional
    @Override
    public void execute()
    {
        operationLog.info(ResetArchivePendingTask.class.getSimpleName() + " Started");
        // 1. Find datasets with DataSetArchivingStatus.ARCHIVE_PENDING and not present in archive
        IOpenBISService service = ArchiverServiceProviderFactory.getInstance().getOpenBISService();
        List<SimpleDataSetInformationDTO> inArchivePendings
                = service.listPhysicalDataSetsByArchivingStatus(DataSetArchivingStatus.ARCHIVE_PENDING, false);
        if (inArchivePendings.isEmpty() == false)
        {
            operationLog.info("Found " + inArchivePendings.size() + " datasets in " + DataSetArchivingStatus.ARCHIVE_PENDING.name() + " status.");

            // 2. Filter out datasets that are not on the command queue
            Set<String> inQueue = new HashSet<>();
            inQueue.addAll(ArchiverServiceProviderFactory.getInstance().getOpenBISService().listDataSetCodesFromCommandQueue());

            operationLog.info("Found " + inQueue.size() + " datasets in the command queues.");

            List<String> dataSetsToUpdate = new ArrayList<>();
            for (SimpleDataSetInformationDTO inArchivePending : inArchivePendings)
            {
                if (inQueue.contains(inArchivePending.getDataSetCode()) == false)
                {
                    dataSetsToUpdate.add(inArchivePending.getDataSetCode());
                    operationLog.info(inArchivePending.getDataSetCode()
                            + " scheduled to update because not present in archive and not found in any command queue.");
                }
            }

            // 3. Update datasets status to AVAILABLE
            operationLog.info("Going to update " + dataSetsToUpdate.size() + " datasets.");
            ArchiverServiceProviderFactory.getInstance().getDataSetStatusUpdater().update(dataSetsToUpdate, DataSetArchivingStatus.AVAILABLE, false);
        }
        operationLog.info(ResetArchivePendingTask.class.getSimpleName() + " Finished");
    }

}
