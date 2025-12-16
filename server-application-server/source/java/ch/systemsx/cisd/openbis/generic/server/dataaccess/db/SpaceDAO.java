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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import ch.ethz.sis.shared.log.classic.impl.Logger;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.orm.hibernate5.HibernateTemplate;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.common.reflection.MethodUtils;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ISpaceDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.shared.basic.CodeConverter;
import ch.systemsx.cisd.openbis.generic.shared.dto.SpacePE;

import static ch.systemsx.cisd.openbis.generic.server.dataaccess.db.DAOUtils.BATCH_SIZE;

/**
 * <i>Data Access Object</i> implementation for {@link SpacePE}.
 * 
 * @author Christian Ribeaud
 */
final class SpaceDAO extends AbstractGenericEntityDAO<SpacePE> implements ISpaceDAO
{

    /**
     * This logger does not output any SQL statement. If you want to do so, you had better set an appropriate debugging level for class
     * {@link JdbcAccessor}.
     * </p>
     */
    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            SpaceDAO.class);

    SpaceDAO(final SessionFactory sessionFactory, EntityHistoryCreator historyCreator)
    {
        super(sessionFactory, SpacePE.class, historyCreator);
    }

    //
    // ISpaceDAO
    //

    @Override
    public final SpacePE tryFindSpaceByCode(final String spaceCode) throws DataAccessException
    {
        assert spaceCode != null : "Unspecified space code.";

        final List<SpacePE> list =
                find(SpacePE.class,
                        String.format("select g from %s g where g.code = ?1", getEntityClass().getSimpleName()),
                        toArray(CodeConverter.tryToDatabase(spaceCode)));
        final SpacePE entity = tryFindEntity(list, "space");
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(%s): '%s'.", MethodUtils.getCurrentMethod()
                    .getName(), spaceCode, entity));
        }
        return entity;
    }

    @Override
    public List<SpacePE> tryFindSpaceByCodes(List<String> spaceCodes) throws DataAccessException
    {
        List<SpacePE> list = doExecute(session -> session.createQuery("select s from SpacePE s where s.code in :codes", SpacePE.class)
                        .setParameter("codes", spaceCodes)
                        .getResultList());

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(): %d space(s) have been found.", MethodUtils
                    .getCurrentMethod().getName(), list.size()));
        }
        return list;
    }

    @Override
    public final List<SpacePE> listSpaces() throws DataAccessException
    {

        // hibernate 6  fails on the join
        List<SpacePE> list = doExecute(session->
                                session.createQuery(
                                "select distinct s from SpacePE s ",
                                SpacePE.class)
                        .getResultList());
        list.forEach(s -> org.hibernate.Hibernate.initialize(s.getRegistrator()));


        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(): %d space(s) have been found.", MethodUtils
                    .getCurrentMethod().getName(), list.size()));
        }
        return list;
    }

    @Override
    public List<SpacePE> listByIDs(Collection<Long> ids)
    {
        return listByIDsOfName("id", ids);
    }

    private List<SpacePE> listByIDsOfName(String idName, Collection<?> ids)
    {
        if (ids == null || ids.isEmpty())
        {
            return new ArrayList<SpacePE>();
        }

        List<?> allIds = new ArrayList<>(ids);
        List<SpacePE> list = new ArrayList<>(allIds.size());

        for (int i = 0; i < allIds.size(); i += BATCH_SIZE)
        {
            List<?> slice = allIds.subList(i, Math.min(allIds.size(), i + BATCH_SIZE));
            if (slice.isEmpty())
                continue;

            List<SpacePE> batch = doExecute(session -> session.createQuery(
                                            "from " + SpacePE.class.getName() + " e where e.id  in (:ids)",
                                            SpacePE.class
                                    )
                                    .setParameter("ids", slice)
                                    .list());

            list.addAll(batch);
        }
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%d spaces(s) have been found.", list.size()));
        }
        return list;
    }

    @Override
    public final void createSpace(final SpacePE space) throws DataAccessException
    {
        assert space != null : "Unspecified space";
        validatePE(space);

        space.setCode(CodeConverter.tryToDatabase(space.getCode()));
        doExecute(session -> {
           session.persist(space);
           session.flush();
           return null;
        });
        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format("ADD: space '%s'.", space));
        }
    }

}
