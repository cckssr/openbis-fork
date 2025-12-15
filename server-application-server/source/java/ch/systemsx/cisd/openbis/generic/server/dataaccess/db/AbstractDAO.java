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

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.persistence.PersistenceException;
import javax.persistence.TemporalType;
import javax.validation.*;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ObjectDeletedException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.orm.hibernate5.SessionFactoryUtils;

import ch.systemsx.cisd.common.exceptions.ExceptionUtils;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.DynamicPropertyEvaluationOperation;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDynamicPropertyEvaluationScheduler;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.util.UpdateUtils;
import ch.systemsx.cisd.openbis.generic.shared.basic.IIdHolder;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.dto.IEntityInformationWithPropertiesHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Spring 6 / Hibernate 6 compatible base DAO.
 * Replaces HibernateDaoSupport + HibernateTemplate with direct Session usage.
 */
public abstract class AbstractDAO {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final int MAX_STRING_ERROR_LENGTH = 500;

    private final SessionFactory sessionFactory;

    protected AbstractDAO(final SessionFactory sessionFactory) {
        assert sessionFactory != null : "Unspecified session factory";
        this.sessionFactory = sessionFactory;
    }

    /* ---------- Validation ---------- */

    @SuppressWarnings({ "rawtypes" })
    protected final static <E> void validatePE(final E pe) throws DataIntegrityViolationException
    {

        Validator validator = factory.getValidator();

        final Set<ConstraintViolation<E>> violations = validator.validate(pe);

        if (violations.size() > 0)
        {
            String msg = "";
            for (ConstraintViolation v : violations)
            {
                String invalidValue = (v.getInvalidValue() != null) ? v.getInvalidValue().toString() : null;
                if (invalidValue != null && invalidValue.length() > MAX_STRING_ERROR_LENGTH)
                {
                    invalidValue =
                            String.format("%s... (complete value was %d characters)", invalidValue.substring(0, MAX_STRING_ERROR_LENGTH),
                                    invalidValue.length());
                }
                msg += ", " + String.format(v.getMessage(), invalidValue);
            }
            throw new DataIntegrityViolationException(msg.substring(2));
        }
    }

    /**
     * Casts given <var>list</var> to specified type.
     * <p>
     * The purpose of this method is to avoid <code>SuppressWarnings("unchecked")</code> in calling methods.
     * </p>
     */
    @SuppressWarnings("unchecked")
    protected static final <T> List<T> cast(final List<?> list)
    {
        return (List<T>) list;
    }

    /**
     * Ensures that given {@link List} contains one and only one entity.
     * 
     * @throws EmptyResultDataAccessException if given <var>entities</var> are <code>null</code> or empty.
     * @throws IncorrectResultSizeDataAccessException if more than one entity is found in given {@link List}.
     */
    @SuppressWarnings("unchecked")
    protected final static <T> T getEntity(final List<T> entities) throws DataAccessException
    {
        return (T) DataAccessUtils.requiredSingleResult(entities);
    }

    /**
     * Casts given <var>entity</var> to specified type.
     * 
     * @throws EmptyResultDataAccessException if given <var>entity</var> is <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    protected final static <T> T getEntity(final Object entity)
            throws EmptyResultDataAccessException
    {
        if (entity == null)
        {
            throw new EmptyResultDataAccessException(1);
        }
        return (T) entity;
    }

    /**
     * Casts given <var>entityOrNull</var> to specified type or returns null for null input.
     */
    @SuppressWarnings("unchecked")
    protected final static <T> T tryGetEntity(final Object entityOrNull)
    {
        return entityOrNull == null ? null : (T) entityOrNull;
    }

    /**
     * Checks given <var>entities</var> and throws a {@link IncorrectResultSizeDataAccessException} if it contains more than one item.
     * 
     * @return <code>null</code> or the entity found at index <code>0</code>.
     */
    protected final static <T> T tryFindEntity(final List<T> entities, final String entitiesName,
            final Object... parameters) throws IncorrectResultSizeDataAccessException
    {
        final int size = entities.size();
        switch (size)
        {
            case 0:
                return null;
            case 1:
                return entities.get(0);
            default:
                throw new IncorrectResultSizeDataAccessException(String.format(
                        "%d %s found for '%s'. Expected: 1 or 0.", size, entitiesName,
                        parameters.length == 1 ? parameters[0] : Arrays.asList(parameters)), 1,
                        size);
        }
    }

    protected Date getTransactionTimeStamp()
    {
        return UpdateUtils.getTransactionTimeStamp(getSessionFactory());
    }

    /**
     * Executes given <var>sql</var>.
     * <p>
     * Should be an <code>INSERT</code> or <code>UPDATE</code> statement.
     * </p>
     */
    protected final void executeUpdate(final String sql, final Serializable... parameters)
    {

        final NativeQuery<?> sqlQuery = currentSession().createNativeQuery(sql);
        for (int i = 0; i < parameters.length; i++)
        {
            Serializable parameter = parameters[i];
            int position = i + 1;
            if (parameter instanceof Date)
            {
                sqlQuery.setParameter(position, (Date) parameter, TemporalType.TIMESTAMP);
            } else
            {
                sqlQuery.setParameter(position, parameter);
            }
        }
        sqlQuery.executeUpdate();

    }

    /**
     * Natively performs given <var>sql</var> and returns an unique result by calling {@link Query#uniqueResult()}.
     */
    @SuppressWarnings("unchecked")
    protected final <T> T getUniqueResult(final String sql, final Object... parameters)
    {
        Query<?> q = currentSession().createNativeQuery(String.format(sql, parameters));
        return (T) q.uniqueResultOptional().orElse(null);
    }

    protected final static Object[] toArray(final Object... objects)
    {
        return objects;
    }

    protected final long getNextSequenceId(final String sequenceName)
    {
        // TT TODO : stop accepting arbitrary strings and force a whitelist/enum of allowed sequences.
        Object result = currentSession()
                .createNativeQuery("select nextval('" + sequenceName + "')")
                .getSingleResult();
        if (result instanceof BigInteger) {
            return ((BigInteger) result).longValue();
        }
        return ((Number) result).longValue();
    }

    protected final Object executeStatelessAction(final StatelessHibernateCallback action) throws DataAccessException
    {
        assert action != null;
        Objects.requireNonNull(action, "action");
        try {
            return currentSession().doReturningWork((ReturningWork<Object>) (Connection connection) -> {
                try (StatelessSession sls = getSessionFactory().openStatelessSession(connection)) {
                    return action.doInStatelessSession(sls);
                } catch (HibernateException ex) {
                    throw SessionFactoryUtils.convertHibernateAccessException(ex);
                } catch (RuntimeException ex) {
                    throw ex;
                }
            });
        } catch (HibernateException ex) {
            throw SessionFactoryUtils.convertHibernateAccessException(ex);
        }
    }

    /*
     * protected final Object executeStatelessAction(final StatelessHibernateCallback action) throws DataAccessException { assert action != null;
     * return getHibernateTemplate().execute(new HibernateCallback() {
     * @Override public final Object doInHibernate(final Session session) throws HibernateException { StatelessSession sls = null; try { sls =
     * this.getStatelessSession(); return action.doInStatelessSession(sls); } catch (HibernateException ex) { throw
     * SessionFactoryUtils.convertHibernateAccessException(ex); } catch (RuntimeException ex) { // Callback code threw application exception... throw
     * ex; } finally { if (sls != null) { releaseConnection(sls.connection()); sls.close(); } } } private StatelessSession getStatelessSession()
     * throws CannotGetJdbcConnectionException { return getSessionFactory().openStatelessSession(getThreadBoundConnection()); } private Connection
     * getThreadBoundConnection() throws CannotGetJdbcConnectionException { return DataSourceUtils.getConnection(SessionFactoryUtils
     * .getDataSource(getSessionFactory())); } private void releaseConnection(Connection connection) { DataSourceUtils.releaseConnection(connection,
     * SessionFactoryUtils.getDataSource(getSessionFactory())); } }); }
     */

    protected void lockEntities(Collection<? extends IIdHolder> entitiesOrNull)
    {
        if (entitiesOrNull == null)
        {
            return;
        }
        for (IIdHolder entity : entitiesOrNull)
        {
            lockEntity(entity);
        }
    }

    protected void lockEntity(IIdHolder entityOrNull)
    {
        var session = currentSession();
        if (entityOrNull != null && entityOrNull.getId() != null && session.contains(entityOrNull)) {
            try {
                session.lock(entityOrNull, LockMode.PESSIMISTIC_WRITE);
            } catch (ObjectDeletedException ode) {
                session.evict(entityOrNull);
            }
        }
    }

    /**
     * Callback interface for Hibernate code requiring a {@link org.hibernate.StatelessSession}. To be used with {@link HibernateTemplate}'s execution
     * methods, often as anonymous classes within a method implementation.
     */
    protected interface StatelessHibernateCallback
    {
        Object doInStatelessSession(StatelessSession sls);
    }


    protected void flushWithSqlExceptionHandling() throws DataAccessException {
        try {
            doExecute(session -> {
                session.flush();
                return null;
            });
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            translateUncategorizedSQLException(e);
        } catch (javax.validation.ConstraintViolationException e) {
            translateConstraintViolationException(e);
        } catch (PersistenceException e) {
            translatePersistenceConstraintViolationException(e);
        }
    }
    protected static void translatePersistenceConstraintViolationException(PersistenceException exception)
            throws DataAccessException
    {
        org.hibernate.exception.ConstraintViolationException constraintViolation =
                ExceptionUtils.tryGetThrowableOfClass(exception,
                        org.hibernate.exception.ConstraintViolationException.class);
        if (constraintViolation != null)
        {
            throw new DataIntegrityViolationException(constraintViolation.getMessage(), exception);
        }
        throw exception;
    }

    protected static void translateConstraintViolationException(ConstraintViolationException exception) throws DataAccessException
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Insert/Update failed - ");
        for(ConstraintViolation<?> violation : exception.getConstraintViolations())
        {
            builder.append(violation.getMessage());
            builder.append("\n");
        }
        throw new DataIntegrityViolationException(builder.toString());
    }

    protected static void translateUncategorizedSQLException(UncategorizedSQLException exception)
            throws DataAccessException
    {
        final SQLException sqlExceptionOrNull =
                ExceptionUtils.tryGetThrowableOfClass(exception, SQLException.class);
        if (sqlExceptionOrNull != null && sqlExceptionOrNull.getNextException() != null)
        {
            throw new DataIntegrityViolationException(sqlExceptionOrNull.getNextException()
                    .getMessage());
        } else
        {
            throw exception;
        }
    }

    protected static Set<TechId> transformNumbers2TechIdSet(Collection<? extends Number> numbers)
    {
        final Set<TechId> result = new HashSet<TechId>();
        for (Number number : numbers)
        {
            result.add(new TechId(number));
        }
        return result;
    }

    protected static List<TechId> transformNumbers2TechIdList(Collection<? extends Number> numbers)
    {
        final List<TechId> result = new ArrayList<TechId>();
        for (Number number : numbers)
        {
            result.add(new TechId(number));
        }
        return result;
    }

    protected static <T extends IEntityInformationWithPropertiesHolder> List<Long> transformEntities2Longs(
            Collection<T> entities)
    {
        final List<Long> result = new ArrayList<Long>();
        for (IEntityInformationWithPropertiesHolder entity : entities)
        {
            result.add(entity.getId());
        }
        return result;
    }

    protected static <T extends IEntityInformationWithPropertiesHolder> void scheduleDynamicPropertiesEvaluationWithIds(
            IDynamicPropertyEvaluationScheduler scheduler, Class<T> entityClass, List<Long> ids)
    {
        scheduleDynamicPropertiesEvaluationForIds(scheduler, entityClass, ids);
    }

    protected static <T extends IEntityInformationWithPropertiesHolder> void scheduleDynamicPropertiesEvaluation(
            IDynamicPropertyEvaluationScheduler scheduler, Class<T> entityClass, List<T> entities)
    {
        List<Long> ids = transformEntities2Longs(entities);
        scheduleDynamicPropertiesEvaluationForIds(scheduler, entityClass, ids);
    }

    protected static <T extends IEntityInformationWithPropertiesHolder> void scheduleDynamicPropertiesEvaluationForIds(
            IDynamicPropertyEvaluationScheduler scheduler, Class<T> entityClass,
            List<Long> entityIds)
    {
        scheduler.scheduleUpdate(DynamicPropertyEvaluationOperation
                .evaluate(entityClass, entityIds));
    }

    public void flush() throws DataAccessException {
        flushWithSqlExceptionHandling();
    }

    @SuppressWarnings("resource") // Spring-managed, transaction-bound Session; do not close here
    public void clear()
    {
        currentSession().clear();
    }

    protected final static <T> Class<T> cast(final Class<?> clazz)
    {
        return (Class<T>) clazz;
    }


    protected final SessionFactory getSessionFactory()
    {
        return this.sessionFactory;
    }

    protected final Session currentSession()
    {
        return this.sessionFactory.getCurrentSession();
    }

    protected <T> List<T> loadAll(Class<T> type) {
        return doExecute(session ->
                session.createQuery("from " + type.getName(), type)
                        .getResultList()
        );
    }

    // inspired by HibernateTemplate#doExecute
    protected <T> T doExecute(HibernateCallback<T> action) throws DataAccessException {
        Assert.notNull(action, "Callback object must not be null");

        Session session = null;
        boolean isNew = false;
        try {
            session = currentSession();
        }
        catch (HibernateException ex) {
//            logger.debug("Could not retrieve pre-bound Hibernate session", ex);
        }
        if (session == null) {
            session = sessionFactory.openSession();
            session.setHibernateFlushMode(FlushMode.MANUAL);
            isNew = true;
        }

        try {
            return action.doInHibernate(session);
        }
        catch (HibernateException ex) {
            throw SessionFactoryUtils.convertHibernateAccessException(ex);
        }
        catch (PersistenceException ex) {
            if (ex.getCause() instanceof HibernateException) {
                throw SessionFactoryUtils.convertHibernateAccessException((HibernateException) ex.getCause());
            }
            throw ex;
        }
        catch (RuntimeException ex) {
            // Callback code threw application exception...
            throw ex;
        }
        finally
        {
            if (isNew)
            {
                SessionFactoryUtils.closeSession(session);
            }
        }
    }


    protected <T> List<T> find(Class<T> resultType, String hql, Object... params) throws DataAccessException {
        return doExecute(session -> {
            var q = session.createQuery(hql, resultType);

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    q.setParameter(i + 1, params[i]); // 1-based positional params
                }
            }

            return q.getResultList();
        });

    }


}
