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
package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.datastore;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.IObjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.create.DataStoreCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.id.DataStorePermId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.context.IProgress;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity.AbstractCreateEntityExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.batch.CollectionBatch;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.batch.CollectionBatchProcessor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.batch.MapBatch;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.entity.progress.CreateProgress;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.business.bo.DataAccessExceptionTranslator;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.shared.dto.DataStorePE;

/**
 * @author pkupczyk
 */
@Component
public class CreateDataStoreExecutor extends AbstractCreateEntityExecutor<DataStoreCreation, DataStorePE, DataStorePermId> implements
        ICreateDataStoreExecutor
{

    @Autowired
    private IDAOFactory daoFactory;

    @Autowired
    private IDataStoreAuthorizationExecutor authorizationExecutor;

    @Override
    protected List<DataStorePE> createEntities(final IOperationContext context, CollectionBatch<DataStoreCreation> batch)
    {
        final List<DataStorePE> dataStores = new LinkedList<>();

        new CollectionBatchProcessor<>(context, batch)
        {
            @Override
            public void process(DataStoreCreation object)
            {
                DataStorePE dataStore = new DataStorePE();
                dataStore.setCode(object.getCode());
                dataStore.setDownloadUrl(object.getDownloadUrl() != null ? object.getDownloadUrl() : "");
                dataStore.setRemoteUrl(object.getRemoteUrl() != null ? object.getRemoteUrl() : "");
                dataStore.setDatabaseInstanceUUID(object.getStorageUuid() != null ? object.getStorageUuid() : "");
                dataStore.setSessionToken("");
                dataStores.add(dataStore);
            }

            @Override
            public IProgress createProgress(DataStoreCreation object, int objectIndex, int totalObjectCount)
            {
                return new CreateProgress(object, objectIndex, totalObjectCount);
            }
        };

        return dataStores;
    }

    @Override
    protected DataStorePermId createPermId(IOperationContext context, DataStorePE entity)
    {
        return new DataStorePermId(entity.getCode());
    }

    @Override
    protected void checkData(IOperationContext context, DataStoreCreation creation)
    {
        if (StringUtils.isEmpty(creation.getCode()))
        {
            throw new UserFailureException("Code cannot be empty.");
        }
    }

    @Override
    protected void checkAccess(IOperationContext context)
    {

    }

    @Override
    protected void checkAccess(IOperationContext context, DataStorePE entity)
    {
        authorizationExecutor.canCreate(context);
    }

    @Override
    protected void updateBatch(IOperationContext context, MapBatch<DataStoreCreation, DataStorePE> batch)
    {
        // nothing to do
    }

    @Override
    protected void updateAll(IOperationContext context, MapBatch<DataStoreCreation, DataStorePE> batch)
    {
        // nothing to do
    }

    @Override
    protected List<DataStorePE> list(IOperationContext context, Collection<Long> ids)
    {
        return daoFactory.getDataStoreDAO().listDataStores(true, true).stream().filter(store -> ids.contains(store.getId())).toList();
    }

    @Override
    protected void save(IOperationContext context, List<DataStorePE> entities, boolean clearCache)
    {
        for (DataStorePE entity : entities)
        {
            final DataStorePE existingEntity = daoFactory.getDataStoreDAO().tryToFindDataStoreByCode(entity.getCode());
            if (existingEntity == null)
            {
                daoFactory.getDataStoreDAO().createOrUpdateDataStore(entity);
            } else
            {
                existingEntity.setDownloadUrl(entity.getDownloadUrl());
                existingEntity.setRemoteUrl(entity.getRemoteUrl());
                existingEntity.setDatabaseInstanceUUID(entity.getDatabaseInstanceUUID());
                entity.setId(existingEntity.getId());
            }

        }

        daoFactory.getSessionFactory().getCurrentSession().flush();
    }

    @Override
    protected void handleException(DataAccessException e)
    {
        DataAccessExceptionTranslator.throwException(e, "dataStore", null);
    }

    @Override
    protected IObjectId getId(DataStorePE entity)
    {
        return new DataStorePermId(entity.getCode());
    }

}
