/*
 * Copyright ETH 2015 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ch.ethz.sis.shared.log.classic.impl.Logger;

import ch.systemsx.cisd.common.collection.CollectionUtils;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.common.maintenance.IMaintenanceTask;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.dataaccess.IMultiDataSetArchiverDBTransaction;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.dataaccess.IMultiDataSetArchiverReadonlyQueryDAO;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.dataaccess.MultiDataSetArchiverContainerDTO;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.dataaccess.MultiDataSetArchiverDBTransaction;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.dataaccess.MultiDataSetArchiverDataSetDTO;
import ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.dataaccess.MultiDataSetArchiverDataSourceUtil;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverServiceProviderFactory;
import ch.systemsx.cisd.openbis.dss.generic.shared.ArchiverTaskContext;
import ch.systemsx.cisd.openbis.dss.generic.shared.IArchiverPlugin;
import ch.systemsx.cisd.openbis.dss.generic.shared.IDataSetDirectoryProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.IOpenBISService;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractExternalData;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;
import ch.systemsx.cisd.openbis.generic.shared.translator.ExternalDataTranslator;

/**
 * Maintenance task for unarchiving multi data set archives.
 *
 * @author Franz-Josef Elmer
 */
public class MultiDataSetUnarchivingMaintenanceTask implements IMaintenanceTask
{
    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            MultiDataSetUnarchivingMaintenanceTask.class);

    @Override
    public void setUp(String pluginName, Properties properties)
    {
    }

    @Override
    public void execute()
    {
        IMultiDataSetArchiverReadonlyQueryDAO dao = getReadonlyQuery();
        List<MultiDataSetArchiverContainerDTO> containersForUnarchiving = dao.listContainersForUnarchiving();
        IArchiverPlugin archiverPlugin = getArchiverPlugin();
        IDataSetDirectoryProvider directoryProvider = getDirectoryProvider();
        IHierarchicalContentProvider hierarchicalContentProvider = getHierarchicalContentProvider();
        ArchiverTaskContext context = new ArchiverTaskContext(directoryProvider, hierarchicalContentProvider);
        context.setForceUnarchiving(true);
        for (MultiDataSetArchiverContainerDTO container : containersForUnarchiving)
        {
            List<MultiDataSetArchiverDataSetDTO> dataSets = dao.listDataSetsForContainerId(container.getId());
            List<String> dataSetCodes = extractCodes(dataSets);
            List<DatasetDescription> loadedDataSets = loadDataSets(dataSetCodes);
            if (!loadedDataSets.isEmpty())
            {
                operationLog.info("Start unarchiving " + CollectionUtils.abbreviate(dataSetCodes, 20));
                archiverPlugin.unarchive(loadedDataSets, context);
                resetRequestUnarchiving(container);
                operationLog.info("Unarchiving finished for " + CollectionUtils.abbreviate(dataSetCodes, 20));
            }
        }
    }

    private void resetRequestUnarchiving(MultiDataSetArchiverContainerDTO container)
    {
        IMultiDataSetArchiverDBTransaction transaction = getTransaction();
        try
        {
            transaction.resetRequestUnarchiving(container.getId());
            transaction.commit();
            transaction.close();
        } catch (Exception e)
        {
            operationLog.warn("Reset request unarchiving of container " + container + " failed", e);
            try
            {
                transaction.rollback();
                transaction.close();
            } catch (Exception ex)
            {
                operationLog.warn("Rollback of multi dataset db transaction failed", ex);
            }
        }
    }

    private List<DatasetDescription> loadDataSets(List<String> dataSetCodes)
    {
        IOpenBISService service = getASService();
        List<DatasetDescription> result = new ArrayList<DatasetDescription>();
        for (AbstractExternalData dataSet : service.listDataSetsByCode(dataSetCodes))
        {
            result.add(ExternalDataTranslator.translateToDescription(dataSet));
        }
        return result;
    }

    private List<String> extractCodes(List<MultiDataSetArchiverDataSetDTO> dataSets)
    {
        List<String> codes = new ArrayList<String>();
        for (MultiDataSetArchiverDataSetDTO dataSet : dataSets)
        {
            codes.add(dataSet.getCode());
        }
        return codes;
    }

    IOpenBISService getASService()
    {
        return ArchiverServiceProviderFactory.getInstance().getOpenBISService();
    }

    IHierarchicalContentProvider getHierarchicalContentProvider()
    {
        return ArchiverServiceProviderFactory.getInstance().getHierarchicalContentProvider();
    }

    IArchiverPlugin getArchiverPlugin()
    {
        return ArchiverServiceProviderFactory.getInstance().getArchiverPlugin();
    }

    IDataSetDirectoryProvider getDirectoryProvider()
    {
        return ArchiverServiceProviderFactory.getInstance().getDataSetDirectoryProvider();
    }

    IMultiDataSetArchiverReadonlyQueryDAO getReadonlyQuery()
    {
        return MultiDataSetArchiverDataSourceUtil.getReadonlyQueryDAO(MultiDataSetArchivingUtils.getMutiDataSetArchiverDataSource());
    }

    IMultiDataSetArchiverDBTransaction getTransaction()
    {
        return new MultiDataSetArchiverDBTransaction(MultiDataSetArchivingUtils.getMutiDataSetArchiverDataSource());
    }
}
