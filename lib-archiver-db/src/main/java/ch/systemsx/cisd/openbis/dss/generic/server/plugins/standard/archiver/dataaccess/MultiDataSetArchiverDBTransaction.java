/*
 * Copyright ETH 2014 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.dss.generic.server.plugins.standard.archiver.dataaccess;

import java.util.List;

import javax.sql.DataSource;

/**
 * @author Jakub Straszewski
 */
public class MultiDataSetArchiverDBTransaction implements IMultiDataSetArchiverDBTransaction
{

    private final IMultiDataSetArchiverQueryDAO query;

    public MultiDataSetArchiverDBTransaction(DataSource dataSource)
    {
        this.query = MultiDataSetArchiverDataSourceUtil.getTransactionalQuery(dataSource);
    }

    @Override
    public List<MultiDataSetArchiverDataSetDTO> getDataSetsForContainer(MultiDataSetArchiverContainerDTO container)
    {
        return query.listDataSetsForContainerId(container.getId());
    }

    /**
     * Creates a new container
     */
    @Override
    public MultiDataSetArchiverContainerDTO createContainer(String path)
    {

        MultiDataSetArchiverContainerDTO container =
                new MultiDataSetArchiverContainerDTO(0, path);

        long id = query.addContainer(container);
        container.setId(id);

        return container;
    }

    @Override
    public void deleteContainer(long containerId)
    {
        query.deleteContainer(containerId);
    }

    @Override
    public void deleteContainer(String container)
    {
        query.deleteContainer(container);
    }

    @Override
    public MultiDataSetArchiverDataSetDTO insertDataset(String code, Long size,
            MultiDataSetArchiverContainerDTO container)
    {
        MultiDataSetArchiverDataSetDTO mads = getDataSetForCode(code);

        if (mads != null)
        {
            throw new IllegalStateException("Dataset " + code + "is already archived in other container");
        }

        mads = new MultiDataSetArchiverDataSetDTO(0, code, container.getId(), size);

        long id = query.addDataSet(mads);
        mads.setId(id);

        return mads;
    }

    @Override
    public MultiDataSetArchiverDataSetDTO getDataSetForCode(String code)
    {
        return query.getDataSetForCode(code);
    }

    @Override
    public void requestUnarchiving(List<String> dataSetCodes)
    {
        query.requestUnarchiving(dataSetCodes.toArray(new String[dataSetCodes.size()]));
    }

    @Override
    public void resetRequestUnarchiving(long containerId)
    {
        query.resetRequestUnarchiving(containerId);
    }

    /**
     * @see net.lemnik.eodsql.TransactionQuery#commit()
     */
    @Override
    public void commit()
    {
        query.commit();
    }

    /**
     * @see net.lemnik.eodsql.TransactionQuery#rollback()
     */
    @Override
    public void rollback()
    {
        query.rollback();
    }

    /**
     * @see net.lemnik.eodsql.TransactionQuery#close()
     */
    @Override
    public void close()
    {
        query.close();
    }
}
