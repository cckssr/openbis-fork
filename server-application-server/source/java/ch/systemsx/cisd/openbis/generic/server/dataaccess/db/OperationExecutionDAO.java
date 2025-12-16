/*
 * Copyright ETH 2016 - 2023 Zürich, Scientific IT Services
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.common.reflection.MethodUtils;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IOperationExecutionDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.shared.dto.OperationExecutionAvailability;
import ch.systemsx.cisd.openbis.generic.shared.dto.OperationExecutionPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.OperationExecutionState;

/**
 * <i>Data Access Object</i> implementation for {@link OperationExecutionPE}.
 * 
 * @author pkupczyk
 */
final class OperationExecutionDAO extends AbstractGenericEntityDAO<OperationExecutionPE> implements IOperationExecutionDAO
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, OperationExecutionDAO.class);

    OperationExecutionDAO(final SessionFactory sessionFactory, EntityHistoryCreator historyCreator)
    {
        super(sessionFactory, OperationExecutionPE.class, historyCreator);
    }

    @Override
    public void createOrUpdate(OperationExecutionPE execution)
    {
        validatePE(execution);
        doExecute(session -> {
            session.saveOrUpdate(execution);
            session.flush();
            return null;
        });

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("Created or updated operation execution '%s'.", execution));
        }
    }

    @Override
    public OperationExecutionPE tryFindByCode(String code)
    {
        List<OperationExecutionPE> list = findByCodes(Arrays.asList(code));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<OperationExecutionPE> findByCodes(Collection<String> codes)
    {

        List<OperationExecutionPE> list = doExecute(session ->  session.createQuery(
                                "select oe from OperationExecutionPE oe " +
                                        "where oe.code in :codes " +
                                        "order by oe.code asc",
                                OperationExecutionPE.class)
                        .setParameter("codes", codes) // Hibernate 6: pass the Collection directly
                        .getResultList());

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(): %d executions(s) have been found.", MethodUtils
                    .getCurrentMethod().getName(), list.size()));
        }
        return list;
    }

    @Override
    public List<OperationExecutionPE> findByIds(Collection<Long> ids)
    {
        List<OperationExecutionPE> list = doExecute(session -> session.createQuery(
                                "select oe from OperationExecutionPE oe where oe.id in :ids",
                                OperationExecutionPE.class)
                        .setParameter("ids", ids)
                        .getResultList());


        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(): %d executions(s) have been found.", MethodUtils
                    .getCurrentMethod().getName(), list.size()));
        }
        return list;
    }

    @Override
    public List<OperationExecutionPE> getExecutionsToBeFailedAfterServerRestart(Date serverStartDate)
    {

        List<OperationExecutionState> states = Arrays.asList(
                OperationExecutionState.NEW,
                OperationExecutionState.SCHEDULED,
                OperationExecutionState.RUNNING
        );

        var se = doExecute(session ->
                    session.createQuery(
                        "from OperationExecutionPE e " +
                                "where e.state in :states and e.creationDate < :start " +
                                "order by e.creationDate asc, e.id asc",
                        OperationExecutionPE.class)
                    .setParameter("states", states)
                    .setParameter("start", serverStartDate)
                    .list());
        final List<OperationExecutionPE> executions = new ArrayList<>(se);
        sortFromOldestToNewest(executions);
        return executions;
    }

    @Override
    public List<OperationExecutionPE> getExecutionsToBeTimeOutPending()
    {

        List<OperationExecutionState> states = Arrays.asList(
                OperationExecutionState.FAILED,
                OperationExecutionState.FINISHED
        );

        List<OperationExecutionPE> prefiltered = doExecute(session ->
                session.createQuery(
                        "from OperationExecutionPE e " +
                                "where e.state in :states and " +
                                "      (e.availability = :avail " +
                                "       or e.summaryAvailability = :avail " +
                                "       or e.detailsAvailability = :avail) " +
                                "order by e.creationDate asc, e.id asc",
                        OperationExecutionPE.class)
                .setParameter("states", states)
                .setParameter("avail", OperationExecutionAvailability.AVAILABLE)
                .list());


        final List<OperationExecutionPE> executions = new ArrayList<OperationExecutionPE>();

        for (OperationExecutionPE execution : prefiltered)
        {
            boolean matches = false;

            if (OperationExecutionAvailability.AVAILABLE.equals(execution.getAvailability())
                    && execution.getAvailabilityTimeLeft() != null
                    && execution.getAvailabilityTimeLeft() <= 0)
            {
                matches = true;
            } else if (OperationExecutionAvailability.AVAILABLE.equals(execution.getSummaryAvailability())
                    && execution.getSummaryAvailabilityTimeLeft() != null
                    && execution.getSummaryAvailabilityTimeLeft() <= 0)
            {
                matches = true;
            } else if (OperationExecutionAvailability.AVAILABLE.equals(execution.getDetailsAvailability())
                    && execution.getDetailsAvailabilityTimeLeft() != null
                    && execution.getDetailsAvailabilityTimeLeft() <= 0)
            {
                matches = true;
            }

            if (matches)
            {
                executions.add(execution);
            }
        }

        sortFromOldestToNewest(executions);
        return executions;
    }

    @Override
    public List<OperationExecutionPE> getExecutionsToBeTimedOut()
    {
        List<OperationExecutionState> states = Arrays.asList(
                OperationExecutionState.FAILED,
                OperationExecutionState.FINISHED
        );

        List<OperationExecutionPE> executions = doExecute(session ->
                session.createQuery(
                        "from OperationExecutionPE e " +
                                "where e.state in :states and " +
                                "      (e.availability = :pending " +
                                "       or e.summaryAvailability = :pending " +
                                "       or e.detailsAvailability = :pending) " +
                                "order by e.creationDate asc, e.id asc",
                        OperationExecutionPE.class)
                .setParameter("states", states)
                .setParameter("pending", OperationExecutionAvailability.TIME_OUT_PENDING)
                .list());

        sortFromOldestToNewest(executions);
        return executions;
    }

    @Override
    public List<OperationExecutionPE> getExecutionsToBeDeleted()
    {
        List<OperationExecutionState> states = Arrays.asList(
                OperationExecutionState.FAILED,
                OperationExecutionState.FINISHED
        );

        List<OperationExecutionPE> executions = doExecute(session ->
                session
                .createQuery(
                        "from OperationExecutionPE e " +
                                "where e.state in :states and " +
                                "      (e.availability = :pending " +
                                "       or e.summaryAvailability = :pending " +
                                "       or e.detailsAvailability = :pending) " +
                                "order by e.creationDate asc, e.id asc",
                        OperationExecutionPE.class)
                .setParameter("states", states)
                .setParameter("pending", OperationExecutionAvailability.DELETE_PENDING)
                .list());

        sortFromOldestToNewest(executions);
        return executions;
    }

    private void sortFromOldestToNewest(List<OperationExecutionPE> executions)
    {
        Collections.sort(executions, new Comparator<OperationExecutionPE>()
            {
                @Override
                public int compare(OperationExecutionPE o1, OperationExecutionPE o2)
                {
                    return o1.getId().compareTo(o2.getId());
                }
            });
    }


}
