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

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.SessionFactory;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.orm.hibernate5.HibernateTemplate;

import ch.systemsx.cisd.openbis.generic.server.dataaccess.ICorePluginDAO;
import ch.systemsx.cisd.openbis.generic.shared.dto.CorePluginPE;

/**
 * Hibernate-based implementation of {@link ICorePluginDAO}.
 * 
 * @author Kaloyan Enimanev
 */
public class CorePluginDAO extends AbstractDAO implements ICorePluginDAO
{
    private final static Class<CorePluginPE> ENTITY_CLASS = CorePluginPE.class;

    public CorePluginDAO(SessionFactory sessionFactory)
    {
        super(sessionFactory);
    }

    @Override
    public void createCorePlugins(List<CorePluginPE> corePlugins)
    {
        doExecute(session -> {
            for (CorePluginPE plugin : corePlugins)
            {
                session.saveOrUpdate(plugin);   // handles insert or update
            }
            session.flush();
            return null;
        });

    }

    @Override
    public List<CorePluginPE> listCorePluginsByName(String name)
    {
        return currentSession()
                .createQuery(
                        "select distinct c from " + ENTITY_CLASS.getName() + " c where c.name = :name",
                        ENTITY_CLASS
                )
                .setParameter("name", name)
                .list();
    }

}
