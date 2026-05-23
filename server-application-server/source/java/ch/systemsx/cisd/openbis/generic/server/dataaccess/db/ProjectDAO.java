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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ch.ethz.sis.shared.log.classic.impl.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate5.HibernateTemplate;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.common.reflection.MethodUtils;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IProjectDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.shared.basic.CodeConverter;
import ch.systemsx.cisd.openbis.generic.shared.dto.PersonPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.ProjectPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SpacePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.IdentifierHelper;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.ProjectIdentifier;

import static ch.systemsx.cisd.openbis.generic.server.dataaccess.db.DAOUtils.BATCH_SIZE;

/**
 * Implementation of {@link IProjectDAO}.
 * 
 * @author Izabela Adamczyk
 */
public class ProjectDAO extends AbstractGenericEntityDAO<ProjectPE> implements IProjectDAO
{
    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            ProjectDAO.class);

    protected ProjectDAO(final SessionFactory sessionFactory, EntityHistoryCreator historyCreator)
    {
        super(sessionFactory, ProjectPE.class, historyCreator);
    }

    @Override
    public List<ProjectPE> listProjects()
    {
        final List<ProjectPE> list = loadAll(ProjectPE.class);
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(): %d projects(s) have been found.", MethodUtils
                    .getCurrentMethod().getName(), list.size()));
        }
        return list;
    }

    @Override
    public List<ProjectPE> listProjects(final SpacePE space)
    {
        assert space != null : "Unspecified space.";

        List<ProjectPE> list = doExecute( session -> session
                .createQuery("from ProjectPE p where p.space = :space", ProjectPE.class)
                .setParameter("space", space)
                .list());
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(%s): %d project(s) have been found.", MethodUtils
                    .getCurrentMethod().getName(), space, list.size()));
        }
        return list;
    }

    @Override
    public ProjectPE tryGetByPermID(String permId)
    {
        ProjectPE projectOrNull = currentSession()
                .createQuery(
                        "from " + getEntityClass().getName() + " p where p.permId = :permId",
                        getEntityClass()
                )
                .setParameter("permId", permId)
                .uniqueResultOptional()
                .orElse(null);
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String
                    .format("Following project '%s' has been found for permId '%s'.",
                            projectOrNull, permId));
        }
        return projectOrNull;
    }

    @Override
    public ProjectPE tryFindProject(final String spaceCode, final String projectCode)
    {
        assert projectCode != null : "Unspecified project code.";
        assert spaceCode != null : "Unspecified space code.";

        String pCode = CodeConverter.tryToDatabase(projectCode);
        String sCode = CodeConverter.tryToDatabase(spaceCode);

        return currentSession()
                .createQuery(
                        "select p " +
                                "from ProjectPE p " +
                                "join p.space s " +
                                "where p.code = :pCode and s.code = :sCode",
                        ProjectPE.class)
                .setParameter("pCode", pCode)
                .setParameter("sCode", sCode)
                .uniqueResultOptional()
                .orElse(null);
    }

    @Override
    public List<ProjectPE> tryFindProjects(List<ProjectIdentifier> projectIdentifiers)
    {
        List<ProjectPE> allProjects = listProjects();
        List<ProjectPE> matchingProjects = new LinkedList<ProjectPE>();

        Set<ProjectIdentifier> projectIdentifiersSet = new HashSet<ProjectIdentifier>();
        for (ProjectIdentifier projectIdentifier : projectIdentifiers)
        {
            projectIdentifiersSet.add(projectIdentifier);
        }

        for (ProjectPE project : allProjects)
        {
            if (projectIdentifiersSet.contains(IdentifierHelper.createProjectIdentifier(project)))
            {
                matchingProjects.add(project);
            }
        }

        return matchingProjects;
    }

    @Override
    public List<ProjectPE> listByPermID(Collection<String> values)
    {
        return listByIDsOfName("permId", values);
    }

    @Override
    public List<ProjectPE> listByIDs(Collection<Long> values)
    {
        return listByIDsOfName("id", values);
    }

    private List<ProjectPE> listByIDsOfName(String idName, Collection<?> values)
    {
        if (values == null || values.isEmpty())
        {
            return new ArrayList<ProjectPE>();
        }
        List<?> allValues = new ArrayList<>(values);
        List<ProjectPE> list = new ArrayList<>(allValues.size());

        for (int i = 0; i < allValues.size(); i += BATCH_SIZE)
        {
            List<?> slice = allValues.subList(i, Math.min(allValues.size(), i + BATCH_SIZE));
            if (slice.isEmpty())
                continue;

            List<ProjectPE> batch = doExecute(session -> session.createQuery(
                                            "from " + ProjectPE.class.getName() + " e where e." + idName + " in (:ids)",
                                            ProjectPE.class
                                    )
                                    .setParameter("ids", slice)
                                    .list());

            list.addAll(batch);
        }

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%d projects(s) have been found.", list.size()));
        }
        return list;
    }

    @Override
    public void createProject(ProjectPE project, PersonPE modifier)
    {
        assert project != null : "Missing project.";
        validatePE(project);

        project.setCode(CodeConverter.tryToDatabase(project.getCode()));
        project.setModifier(modifier);
        project.setModificationDate(getTransactionTimeStamp());
        doExecute( session -> {
            session.saveOrUpdate(project);
            session.flush();
            return null;
        });
        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format("SAVE: project '%s'.", project));
        }
    }

}
