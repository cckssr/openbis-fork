/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.systemsx.cisd.openbis.generic.server.dataaccess.db;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.common.reflection.MethodUtils;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ICustomIdDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.shared.basic.ICustomIdHolder;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import org.apache.commons.lang3.StringUtils;
import ch.ethz.sis.shared.log.classic.impl.Logger;

import org.hibernate.SessionFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ch.systemsx.cisd.openbis.generic.server.dataaccess.db.DAOUtils.BATCH_SIZE;

public abstract class AbstractGenericEntityWithCustomIdDAO<T extends ICustomIdHolder<?>, X extends ICustomIdHolder> extends AbstractDAO implements
        ICustomIdDAO<T, X>
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            AbstractGenericEntityWithCustomIdDAO.class);

    private final Class<T> entityClass;

    protected final EntityHistoryCreator historyCreator;

    protected AbstractGenericEntityWithCustomIdDAO(final SessionFactory sessionFactory, final Class<T> entityClass,
            EntityHistoryCreator historyCreator)
    {
        super(sessionFactory);
        this.entityClass = entityClass;
        this.historyCreator = historyCreator;
    }

    protected Class<T> getEntityClass()
    {
        return entityClass;
    }

    @Override
    public T getById(X techId)
    {
        assert techId != null : "Technical identifier unspecified.";
        return doExecute( session -> {
            final Object entity = session.get(getEntityClass(), techId.getId());
            T result = null;
            if (entity == null)
            {
                throw new DataRetrievalFailureException(getEntityDescription() + " with ID "
                        + techId.getId() + " does not exist. Maybe someone has just deleted it.");
            } else
            {
                result = getEntity(entity);
            }
            if (operationLog.isDebugEnabled())
            {
                operationLog.debug(String.format("%s(%s): '%s'.", MethodUtils.getCurrentMethod()
                        .getName(), techId, result));
            }
            return result;
        });
    }

    public final T loadByTechId(final TechId techId) throws DataAccessException
    {
        assert techId != null : "Technical identifier unspecified.";
        T result = doExecute(session -> session.load(getEntityClass(), techId.getId()));
        return getEntity(result);
    }


    @Deprecated
    public T tryGetById(X techId)
    {
        assert techId != null : "Technical identifier unspecified.";
        final T result = tryGetEntity(
                currentSession().get(getEntityClass(), techId.getId())
        );
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(%s): '%s'.", MethodUtils.getCurrentMethod()
                    .getName(), techId, result));
        }
        return result;
    }

    private String getEntityDescription()
    {
        String nameWithoutPE = getEntityClass().getSimpleName().replace("PE", "");
        String words[] = StringUtils.splitByCharacterTypeCamelCase(nameWithoutPE);
        return StringUtils.join(words, " ");
    }

    protected <T> List<T> listByIDsOfName(Class<T> clazz, String idName, Collection<?> ids)
    {
        if (ids == null || ids.isEmpty())
        {
            return new ArrayList<T>();
        }
        List<?> all = new ArrayList<>(ids);
        List<T> list = new ArrayList<>(all.size());

        for (int i = 0; i < all.size(); i += BATCH_SIZE) {
            List<?> slice = all.subList(i, Math.min(all.size(), i + BATCH_SIZE));
            if (!slice.isEmpty()) {
                List<T> subList = doExecute(session -> session.createQuery(
                                        "from " + clazz.getName() + " e where e." + idName + " in (:ids)",
                                        clazz)
                                .setParameter("ids", slice)
                                .getResultList());
                if(subList != null)
                {
                    list.addAll(subList);
                }
            }
        }
        if (operationLog.isDebugEnabled())
        {
            String name = clazz.getSimpleName();
            if (name.endsWith("PE"))
            {
                name = name.substring(0, name.length() - 2);
            }
            operationLog.debug(String.format("%d " + name.toLowerCase() + "(s) have been found.", list.size()));
        }
        return list;
    }

    @Override
    public void validateAndSaveUpdatedEntity(T entity)
    {
        assert entity != null : "entity is null";

        // as long as CODE cannot be edited we don't have to translate it with a converter here
        // because the code set in updated entity should be the one already translated during save
        // but if we allow it this will have to be changed for entities with codes e.g.
        // like for experiment:
        // experiment.setCode(CodeConverter.tryToDatabase(experiment.getCode()));

        validatePE(entity);
        flushWithSqlExceptionHandling();
    }

    @Override
    public void validate(T entity)
    {
        assert entity != null : "entity is null";

        // as long as CODE cannot be edited we don't have to translate it with a converter here
        // because the code set in updated entity should be the one already translated during save
        // but if we allow it this will have to be changed for entities with codes e.g.
        // like for experiment:
        // experiment.setCode(CodeConverter.tryToDatabase(experiment.getCode()));

        validatePE(entity);
    }

    @Override
    public void persist(T entity)
            throws DataAccessException
    {
        assert entity != null : "entity unspecified";

        validatePE(entity);
        doExecute( session -> {
                    session.save(entity);
                    session.flush();
                    return null;
                });

        if (operationLog.isInfoEnabled()) {
            operationLog.debug(String.format("%s(%s)", MethodUtils.getCurrentMethod().getName(), entity));
        }

    }

    @Override
    public void delete(T entity)
            throws DataAccessException
    {
        assert entity != null : "entity unspecified";

            doExecute( session -> {
                        //            Session session = currentSession();
                        T managed = (T) session.merge(entity);
                        session.delete(managed); // Or session.remove(entity) in pure JPA
                        session.flush();
                        return  null;
                    });
            if (operationLog.isInfoEnabled()) {
                operationLog.debug(String.format("%s(%s)", MethodUtils.getCurrentMethod().getName(), entity));
            }
    }

    @Override
    public List<T> listAllEntities() throws DataAccessException
    {
        return loadAll(getEntityClass());
    }
}
