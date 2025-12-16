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
import java.util.Collection;
import java.util.List;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate5.HibernateTemplate;

/**
 * Utility routines for DAOs.
 * 
 * @author Bernd Rinn
 */
final class DAOUtils
{

    private DAOUtils()
    {
        // Cannot be instantiated
    }

    /**
     * Don't try to get properties for more than 10000 entities.
     */
    public final static int MAX_COUNT_FOR_PROPERTIES = 20000;

//    /**
//     * Returns the number of entities that the given <var>critera</var> will return.
//     */
//    static int getCount(final Criteria criteria)
//    {
//        int count =
//                ((Number) criteria.setProjection(Projections.rowCount()).uniqueResult()).intValue();
//        // Undo the rowCount projection
//        criteria.setProjection(null);
//        criteria.setResultTransformer(CriteriaSpecification.ROOT_ENTITY);
//        return count;
//    }

    public static final int BATCH_SIZE = 1000;

//    /**
//     * Lists entities of type <code>T</code> by specified collection of identifiers. A Hibernate Criteria query for specified class is created with an
//     * {@link Restrictions#in(String, Collection)} restriction on the collection of identifiers. This is a helper method which reads the data in
//     * chunks because otherwise an exception will be thrown by PosgreSQL if more than 2<sup>15</sup>-1 identfiers.
//     */
//    static <T> List<T> listByCollection(HibernateTemplate hibernateTemplate,
//            final Class<?> entityClass, String columnName, Collection<?> identifiers)
//    {
//        return listByCollection(hibernateTemplate, new IDetachedCriteriaFactory()
//            {
//                @Override
//                public DetachedCriteria createCriteria()
//                {
//                    return DetachedCriteria.forClass(entityClass);
//                }
//            }, columnName, identifiers);
//    }
//
//    /**
//     * Lists entities of type <code>T</code> by specified collection of identifiers. A Hibernate Criteria query is created by the specified factory
//     * and an {@link Restrictions#in(String, Collection)} restriction on the collection of identifiers is added. This is a helper method which reads
//     * the data in chunks because otherwise an exception will be thrown by PosgreSQL if more than 2<sup>15</sup>-1 identfiers.
//     */
//    @SuppressWarnings("unchecked")
//    static <T> List<T> listByCollection(HibernateTemplate hibernateTemplate,
//            IDetachedCriteriaFactory factory, String columnName, Collection<?> identifiers)
//    {
//        List<?> parameters = new ArrayList<Object>(identifiers);
//        List<T> result = new ArrayList<T>();
//        for (int i = 0, n = parameters.size(); i < n; i += BATCH_SIZE)
//        {
//            DetachedCriteria criteria = factory.createCriteria();
//            List<?> subList = parameters.subList(i, Math.min(n, i + BATCH_SIZE));
//            if (subList.isEmpty() == false)
//            {
//                criteria.add(Restrictions.in(columnName, subList));
//                result.addAll((List<T>) hibernateTemplate.findByCriteria(criteria));
//            }
//        }
//        return result;
//    }

//    static <T> List<T> listByCollectionFetching(
//            HibernateTemplate hibernateTemplate,
//            Class<T> entityClass,
//            String idProperty,
//            Collection<?> identifiers,
//            String collectionPath) {
//
//        assert hibernateTemplate != null;
//        if (identifiers == null || identifiers.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        List<?> params = new ArrayList<>(identifiers);
//        List<T> result = new ArrayList<>();
//
//        for (int i = 0; i < params.size(); i += BATCH_SIZE) {
//            List<?> sub = params.subList(i, Math.min(params.size(), i + BATCH_SIZE));
//            if (!sub.isEmpty()) {
//                result.addAll(hibernateTemplate.execute(session ->
//                        session.createQuery(
//                                        "select distinct e " +
//                                                "from " + entityClass.getName() + " e " +
//                                                "left join fetch e." + collectionPath + " " +
//                                                "where e." + idProperty + " in (:ids)",
//                                        entityClass
//                                )
//                                .setParameter("ids", sub)
//                                .list()
//                ));
//            }
//        }
//        return result;
//    }

}
