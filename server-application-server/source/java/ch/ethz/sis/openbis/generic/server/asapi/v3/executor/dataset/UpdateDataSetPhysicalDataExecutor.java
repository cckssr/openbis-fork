/*
 * Copyright ETH 2012 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.dataset;

import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.update.FieldUpdateValue;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.ArchivingStatus;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.PhysicalDataUpdate;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.batch.MapBatch;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;
import ch.systemsx.cisd.openbis.generic.shared.dto.DataPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.ExternalDataPE;

/**
 * @author pkupczyk
 */
@Component
public class UpdateDataSetPhysicalDataExecutor implements IUpdateDataSetPhysicalDataExecutor
{

    @Autowired
    private IUpdateDataSetFileFormatTypeExecutor updateDataSetFileFormatTypeExecutor;

    @Autowired
    private IDataSetAuthorizationExecutor authorizationExecutor;

    @Override
    public void update(IOperationContext context, MapBatch<DataSetUpdate, DataPE> batch)
    {
        for (Entry<DataSetUpdate, DataPE> entry : batch.getObjects().entrySet())
        {
            DataSetUpdate dataSetUpdate = entry.getKey();
            DataPE dataSet = entry.getValue();

            if (dataSetUpdate.getPhysicalData() != null && dataSetUpdate.getPhysicalData().getValue() != null)
            {
                authorizationExecutor.canUpdateSystemFields(context, dataSetUpdate.getDataSetId(), dataSet);
            }
        }

        updateDataSetFileFormatTypeExecutor.update(context, batch);

        for (Entry<DataSetUpdate, DataPE> entry : batch.getObjects().entrySet())
        {
            DataSetUpdate dataSetUpdate = entry.getKey();
            DataPE dataPE = entry.getValue();
            if (dataPE instanceof ExternalDataPE)
            {
                ExternalDataPE externalDataPE = (ExternalDataPE) dataPE;
                FieldUpdateValue<PhysicalDataUpdate> physicalData = dataSetUpdate.getPhysicalData();
                if (physicalData != null && physicalData.getValue() != null)
                {
                    FieldUpdateValue<Boolean> archivingRequested = physicalData.getValue().isArchivingRequested();
                    if (archivingRequested != null && archivingRequested.isModified())
                    {
                        externalDataPE.setArchivingRequested(archivingRequested.getValue());
                    }

                    FieldUpdateValue<Boolean> unarchivingRequested = physicalData.getValue().isUnarchivingRequested();
                    if (unarchivingRequested != null && unarchivingRequested.isModified())
                    {
                        externalDataPE.setUnarchivingRequested(unarchivingRequested.getValue());
                    }

                    FieldUpdateValue<Boolean> presentInArchive = physicalData.getValue().isPresentInArchive();
                    if (presentInArchive != null && presentInArchive.isModified())
                    {
                        externalDataPE.setPresentInArchive(presentInArchive.getValue());
                    }

                    FieldUpdateValue<ArchivingStatus> status = physicalData.getValue().getStatus();
                    if (status != null && status.isModified())
                    {
                        externalDataPE.setStatus(DataSetArchivingStatus.valueOf(status.getValue().name()));
                    }

                    FieldUpdateValue<String> shareId = physicalData.getValue().getShareId();
                    if (shareId != null && shareId.isModified())
                    {
                        externalDataPE.setShareId(shareId.getValue());
                    }

                    FieldUpdateValue<Long> size = physicalData.getValue().getSize();
                    if (size != null && size.isModified())
                    {
                        externalDataPE.setSize(size.getValue());
                    }
                }
            }
        }
    }
}
