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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.common.reflection.MethodUtils;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ITypeGroupAssignmentDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.shared.basic.ComplexIdHolder;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypeTypeGroupsTechId;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.hibernate.SessionFactory;

import org.springframework.orm.hibernate5.HibernateTemplate;

import java.util.Collection;
import java.util.List;

final class TypeGroupAssignmentDAO extends AbstractGenericEntityWithCustomIdDAO<SampleTypeTypeGroupsPE, ComplexIdHolder>  implements
        ITypeGroupAssignmentDAO
{
    private static final Logger
            operationLog = LogFactory.getLogger(LogCategory.OPERATION, TypeGroupAssignmentDAO.class);

    public static final Class<SampleTypeTypeGroupsPE> ENTITY_CLASS = SampleTypeTypeGroupsPE.class;

    TypeGroupAssignmentDAO(SessionFactory sessionFactory, EntityHistoryCreator historyCreator)
    {
        super(sessionFactory, ENTITY_CLASS, historyCreator);
    }

    @Override
    public void createTypeGroupAssignment(SampleTypeTypeGroupsPE typeGroupAssignment)
    {
        assert typeGroupAssignment != null : "type group assignment unspecified";

        validatePE(typeGroupAssignment);

        doExecute(session -> {
            session.save(typeGroupAssignment);
    //        template.flush();
            return null;
        });

        flushWithSqlExceptionHandling();

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("ADD: type group assignment '%s'.", typeGroupAssignment));
        }
    }

    @Override
    public List<SampleTypeTypeGroupsPE> findByTechId(List<SampleTypeTypeGroupsTechId> ids)
    {

        StringBuilder hql = new StringBuilder()
                .append("from " + SampleTypeTypeGroupsPE.class.getName() + " x ")
                .append("join fetch x.sampleType st ")
                .append("join fetch x.typeGroup tg ")
                .append("where ");

        // Build: (st.id = :s0 and tg.id = :t0) or (st.id = :s1 and tg.id = :t1) or ...
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) hql.append(" or ");
            hql.append("(st.id = :s").append(i).append(" and tg.id = :t").append(i).append(")");
        }

        List<SampleTypeTypeGroupsPE> list = doExecute(session -> {
            var query = session.createQuery(hql.toString(), SampleTypeTypeGroupsPE.class);

            for (int i = 0; i < ids.size(); i++)
            {
                SampleTypeTypeGroupsTechId id = ids.get(i);
                query.setParameter("s" + i, id.getSampleTypeTechId());
                query.setParameter("t" + i, id.getTypeGroupTechId());
            }

          return query.list();
        });

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(): %d type group assignment(s) have been found.", MethodUtils
                    .getCurrentMethod().getName(), list.size()));
        }
        return list;
    }

    @Override
    public List<SampleTypeTypeGroupsPE> findByIds(Collection<TypeGroupAssignmentId> ids)
    {

        StringBuilder hql = new StringBuilder()
                .append("from " + SampleTypeTypeGroupsPE.class.getName() + " x ")
                .append("join fetch x.sampleType st ")
                .append("join fetch x.typeGroup tg ")
                .append("where ");



        List<SampleTypeTypeGroupsPE> list = doExecute(session -> {
            int i = 0;
            for (TypeGroupAssignmentId id : ids) {
                if (i > 0) hql.append(" or ");
                hql.append("(st.code = :s").append(i).append(" and tg.code = :t").append(i).append(")");
                i++;
            }
            var query = session.createQuery(hql.toString(), SampleTypeTypeGroupsPE.class);

            i = 0;
            for (TypeGroupAssignmentId id : ids) {
                EntityTypePermId st = (EntityTypePermId) id.getSampleTypeId();
                TypeGroupId tg = (TypeGroupId) id.getTypeGroupId();
                query.setParameter("s" + i, st.getPermId());
                query.setParameter("t" + i, tg.getPermId());
                i++;
            }

            return query.list();
        });

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(): %d type group assignment(s) have been found.", MethodUtils
                    .getCurrentMethod().getName(), list.size()));
        }
        return list;

    }

}
