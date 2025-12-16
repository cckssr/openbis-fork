/*
 * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
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

import java.util.Arrays;
import java.util.List;

import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.hibernate5.SessionFactoryUtils;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IEntityTypeDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ISampleDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.shared.basic.CodeConverter;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SamplePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.properties.EntityKind;

import javax.persistence.PersistenceException;

/**
 * Database based implementation of {@link IEntityTypeDAO}.
 * 
 * @author Franz-Josef Elmer
 */
final class EntityTypeDAO extends AbstractTypeDAO<EntityTypePE> implements IEntityTypeDAO
{
    private static final Logger operationLog =
            LogFactory.getLogger(LogCategory.OPERATION, EntityTypeDAO.class);

    private final EntityKind entityKind;

    private final IDAOFactory daoFactory;

    EntityTypeDAO(final EntityKind entityKind, final SessionFactory sessionFactory,
            EntityHistoryCreator historyCreator, IDAOFactory daoFactory)
    {
        super(sessionFactory, entityKind.getTypeClass(), historyCreator);
        this.entityKind = entityKind;
        this.daoFactory = daoFactory;
    }

    //
    // IEntityTypeDAO
    //

    @Override
    public final EntityTypePE tryToFindEntityTypeByCode(final String code)
            throws DataAccessException
    {
        return super.tryFindTypeByCode(code);
    }

    @Override
    public final List<EntityTypePE> tryToFindEntityTypeByCodes(final List<String> codes)
            throws DataAccessException
    {
        return super.tryFindTypeByCodes(codes, true);
    }

    @Override
    public final <T extends EntityTypePE> List<T> listEntityTypes() throws DataAccessException
    {
        final String entityKindName = entityKind.getLabel(); // e.g., "sample", "dataSet", "experiment", ...
        final Class<T> clazz = (Class<T>) getEntityClass();

        return currentSession()
                .createQuery(
                        "select distinct t " +
                                "from " + clazz.getName() + " t " +
                                "left join fetch t." + entityKindName + "TypePropertyTypesInternal",
                        clazz
                )
                .list();


    }

    @Override
    public final <T extends EntityTypePE> void createOrUpdateEntityType(T entityType)
            throws DataAccessException
    {
        assert entityType != null : "entityType is null";

        validatePE(entityType);
        String strippedCode = CodeConverter.tryToDatabase(entityType.getCode());
        boolean isInternalNamespace = entityType.isManagedInternally();
        entityType.setCode(strippedCode);
        entityType.setManagedInternally(isInternalNamespace);
        doExecute(session -> {
            session.saveOrUpdate(entityType);
            session.flush();
            return null;
        });
        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format("ADD: entity type '%s'.", entityType));
        }

        if (entityType instanceof SampleTypePE)
        {
            ISampleDAO sampleDAO = daoFactory.getSampleDAO();
            List<TechId> sampleIds = sampleDAO.listSampleIdsBySampleTypeIds(Arrays.asList(new TechId(entityType.getId())));
            scheduleDynamicPropertiesEvaluationForIds(
                    daoFactory.getPersistencyResources().getDynamicPropertyEvaluationScheduler(),
                    SamplePE.class, TechId.asLongs(sampleIds));
        }
    }

    @Override
    public final <T extends EntityTypePE> void deleteEntityType(final T entityType)
    {
        assert entityType != null : "Entity Type unspecified";

        doExecute(session -> {
            session.delete(entityType);
            session.flush();
            return null;
        });
        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format("DELETE: entity type '%s'.", entityType));
        }
    }

}
