/*
 * Copyright ETH 2023 - 2023 Zürich, Scientific IT Services
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

import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.shared.basic.BasicConstant;
import ch.systemsx.cisd.openbis.generic.shared.dto.ISampleRelationshipDAO;
import ch.systemsx.cisd.openbis.generic.shared.dto.RelationshipTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleRelationshipPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SequenceNames;
import org.hibernate.Session;
import org.hibernate.SessionFactory;


import java.util.Collection;
import java.util.List;

public class SampleRelationshipDAO extends AbstractGenericEntityDAO<SampleRelationshipPE> implements ISampleRelationshipDAO {

    private Long parentChildRelationshipId = null;

    protected SampleRelationshipDAO(SessionFactory sessionFactory, EntityHistoryCreator historyCreator)
    {
        super(sessionFactory, SampleRelationshipPE.class, historyCreator);
    }

    //
    // Helper Methods to obtain relationship ID "fast" and using detached criteria that should not require a session
    //

    private Long getParentChildRelationshipId()
    {
        if (parentChildRelationshipId == null)
        {
            synchronized(SampleRelationshipDAO.class)
            {
                if (parentChildRelationshipId == null)
                {
                    parentChildRelationshipId = getParentChildRelationship().getId();
                }
            }
        }
        return parentChildRelationshipId;
    }

    private RelationshipTypePE getParentChildRelationship()
    {
        return doExecute(session ->
                session.createQuery(
                        "from RelationshipTypePE r where r.simpleCode = :code",
                        RelationshipTypePE.class)
                .setParameter("code", BasicConstant.PARENT_CHILD_DB_RELATIONSHIP)
                .uniqueResultOptional()
                .orElse(null));
    }

    //
    // DAO Methods
    //

    public void persist(Collection<SampleRelationshipPE> sampleRelationships)
    {
        RelationshipTypePE relationshipType = getParentChildRelationship();
        for (SampleRelationshipPE sampleRelationship : sampleRelationships)
        {
            sampleRelationship.setRelationship(relationshipType);
// This alternative implementations attaches the object to the session without flushing it to the database
//            // Set id so PE object can be attached to session, if the id is null an Exception is thrown.
//            if (sampleRelationship.getId() == null) {
//                sampleRelationship.setId(getNextId());
//            }
//            // Attach object to session if is not already, attaching an already attach object results in an Exception.
//            if (getHibernateTemplate().contains(sampleRelationship) == false) {
//                getHibernateTemplate().update(sampleRelationship);
//            }
            doExecute(session-> {
                session.persist(sampleRelationship);
                return null;
            });
        }
    }

    public void delete(Collection<SampleRelationshipPE> sampleRelationships)
    {

        doExecute( session -> {
            int i = 0;
            for (SampleRelationshipPE rel : sampleRelationships) {
                session.remove(rel);
                // batching for large collections
                if (++i % 100 == 0) {
                    session.flush();
                    session.clear();
                }
            }
            session.flush();
            return null;
         });
    }

    public List<SampleRelationshipPE> listSampleParents(List<Long> childrenTechIds)
    {
        Long relId = getParentChildRelationshipId();

        return doExecute(session ->
                session.createQuery(
                        "from SampleRelationshipPE sr " +
                                "where sr.relationship.id = :relId " +
                                "  and sr.childSample.id in :childIds",
                        SampleRelationshipPE.class)
                .setParameter("relId", relId)
                .setParameter("childIds", childrenTechIds)
                .list());

    }

    public List<SampleRelationshipPE> listSampleChildren(List<Long> parentTechIds)
    {
        Long relId = getParentChildRelationshipId();
        return doExecute(session ->
                session.createQuery(
                        "from SampleRelationshipPE sr " +
                                "where sr.relationship.id = :relId " +
                                "  and sr.parentSample.id in :parentIds",
                        SampleRelationshipPE.class)
                .setParameter("relId", relId)
                .setParameter("parentIds", parentTechIds)
                .list());
    }
}
