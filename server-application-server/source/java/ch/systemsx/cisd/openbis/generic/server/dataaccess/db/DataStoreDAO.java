/*
 * Copyright ETH 2009 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.server.dataaccess.db;

import java.util.List;

import ch.ethz.sis.shared.log.classic.impl.Logger;

import org.hibernate.SessionFactory;


import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDataStoreDAO;
import ch.systemsx.cisd.openbis.generic.shared.Constants;
import ch.systemsx.cisd.openbis.generic.shared.basic.CodeConverter;
import ch.systemsx.cisd.openbis.generic.shared.dto.DataStorePE;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Hibernate-based implementation of {@link IDataStoreDAO}.
 *
 * @author Franz-Josef Elmer
 */
public class DataStoreDAO extends AbstractDAO implements IDataStoreDAO
{
    private final static Class<DataStorePE> ENTITY_CLASS = DataStorePE.class;

    private static final Logger operationLog =
            LogFactory.getLogger(LogCategory.OPERATION, DataStoreDAO.class);

    public DataStoreDAO(SessionFactory sessionFactory)
    {
        super(sessionFactory);
    }

    @Override
    public void createOrUpdateDataStore(DataStorePE dataStore)
    {
        assert dataStore != null : "Unspecified data store";
        dataStore.setCode(CodeConverter.tryToDatabase(dataStore.getCode()));

        doExecute(session -> {
            session.saveOrUpdate(dataStore);
            session.flush();
            return null;
        });
        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format("SAVE/UPDATE: data store '%s'.", dataStore));
        }
    }

    @Override
    public DataStorePE tryToFindDataStoreByCode(final String dataStoreCode)
    {
        assert dataStoreCode != null : "Unspecified data store code.";

        return doExecute(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<DataStorePE> cq = cb.createQuery(DataStorePE.class);
            Root<DataStorePE> root = cq.from(DataStorePE.class);

            cq.select(root).where(
                    cb.equal(root.get("code"), CodeConverter.tryToDatabase(dataStoreCode))
            );

            var results = session.createQuery(cq).getResultList();
            return results.isEmpty() ? null : results.get(0);
        });
    }

    @Override
    public List<DataStorePE> listDataStores()
    {
        return listDataStores(true, false);
    }

    @Override
    public List<DataStorePE> listDataStores(final boolean includeDss, final boolean includeAfs)
    {
        if (!includeAfs && !includeDss) {
            return List.of();
        }

        return doExecute(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<DataStorePE> cq = cb.createQuery(DataStorePE.class);
            Root<DataStorePE> root = cq.from(DataStorePE.class);

            if (includeAfs)
            {
                if (!includeDss) {
                    cq.where(cb.equal(root.get("code"), Constants.AFS_DATA_STORE_CODE));
                }
            } else if (includeDss) {
                cq.where(cb.notEqual(root.get("code"), Constants.AFS_DATA_STORE_CODE));
            } else
            {
                return List.of();
            }

            root.fetch("servicesInternal", javax.persistence.criteria.JoinType.LEFT);
            cq.select(root).distinct(true);
            List<DataStorePE> list = session.createQuery(cq).getResultList();

            if (operationLog.isDebugEnabled()) {
                operationLog.debug(String.format("%d data stores have been found.", list.size()));
            }
            return list;
        });
    }

}
