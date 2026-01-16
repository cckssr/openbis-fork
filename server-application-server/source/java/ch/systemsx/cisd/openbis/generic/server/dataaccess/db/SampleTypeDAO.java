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

import java.util.List;

import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.dao.DataAccessException;

import ch.rinn.restrictions.Private;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.common.reflection.MethodUtils;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ISampleTypeDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypePE;
import jakarta.persistence.criteria.Predicate;

/**
 * Data access object for {@link SampleTypePE}. <br>
 * Note: this class has been copied from old lims project.
 * 
 * @author Christian Ribeaud
 */
final class SampleTypeDAO extends AbstractTypeDAO<SampleTypePE> implements ISampleTypeDAO
{
    @Private
    final static Logger operationLog =
            LogFactory.getLogger(LogCategory.OPERATION, SampleTypeDAO.class);

    SampleTypeDAO(final SessionFactory sessionFactory, EntityHistoryCreator historyCreator)
    {
        super(sessionFactory, SampleTypePE.class, historyCreator);
    }

    //
    // ISampleTypeDAO
    //

    @Override
    public final List<SampleTypePE> listSampleTypes() throws DataAccessException
    {
        List<SampleTypePE> list = doExecute(session ->
                session.createQuery(
                        "select distinct st " +
                                "from " + getEntityClass().getName() + " st " +
                                "left join fetch st.sampleTypePropertyTypesInternal",
                        getEntityClass()
                )
                .list());


        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s: %d sample type(s) have been found.", MethodUtils
                    .getCurrentMethod().getName(), list.size()));
        }
        return list;
    }

    // TODO check if anything more than code is needed
    @Override
    public SampleTypePE tryFindSampleTypeByExample(final SampleTypePE example) {
        assert example != null : "Unspecified sample type.";

        final java.util.List<SampleTypePE> list =  doExecute(session -> {
            // Use the tx-bound EntityManager/Session (configure/inject as you do elsewhere)
// May be better in Hibernate 6
//            return session
//                    .createQuery(
//                            "from SampleTypePE",
//                            SampleTypePE.class
//                    )
//                    .setExample(ex)
//                    .list();

            final var cb = session.getCriteriaBuilder();
            final var cq = cb.createQuery(SampleTypePE.class);
            final var root = cq.from(SampleTypePE.class);

            // Build predicates only for fields that matter in your “example” semantics.
            // Add/remove fields to mirror what your old Example matched on.
            final var predicates = new java.util.ArrayList<Predicate>();

            if (example.getCode() != null)
            {
                predicates.add(cb.equal(root.get("code"), example.getCode()));
            }
            //        if (example.getDatabaseInstance() != null) {
            //            predicates.add(cb.equal(root.get("databaseInstance"), example.getDatabaseInstance()));
            //        }
            //        if (example.getPermId() != null) { // only if your entity has it
            //            predicates.add(cb.equal(root.get("permId"), example.getPermId()));
            //        }

            cq.select(root);
            if (!predicates.isEmpty())
            {
                cq.where(cb.and(predicates.toArray(Predicate[]::new)));
            }

            final var query = session.createQuery(cq);
            return query.getResultList();
        });

        final SampleTypePE result = tryFindEntity(list, "sample type");
        if (operationLog.isDebugEnabled()) {
            operationLog.debug(String.format("%s(%s): Sample type '%s' found.",
                    MethodUtils.getCurrentMethod().getName(), example, result));
        }
        return result;
    }

    @Override
    public final SampleTypePE tryFindSampleTypeByCode(final String code) throws DataAccessException
    {
        return tryFindTypeByCode(code);
    }

}
