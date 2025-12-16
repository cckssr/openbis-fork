/*
 * Copyright ETH 2012 - 2023 Zürich, Scientific IT Services
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

import java.util.Collection;
import java.util.List;

import ch.ethz.sis.shared.log.classic.impl.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IExternalDataManagementSystemDAO;
import ch.systemsx.cisd.openbis.generic.shared.basic.CodeConverter;
import ch.systemsx.cisd.openbis.generic.shared.dto.ExternalDataManagementSystemPE;

/**
 * @author Pawel Glyzewski
 */
public class ExternalDataManagementSystemDAO extends AbstractDAO implements
        IExternalDataManagementSystemDAO
{
    private final static Class<ExternalDataManagementSystemPE> ENTITY_CLASS =
            ExternalDataManagementSystemPE.class;

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            ExternalDataManagementSystemDAO.class);

    public ExternalDataManagementSystemDAO(SessionFactory sessionFactory)
    {
        super(sessionFactory);
    }

    @Override
    public void createOrUpdateExternalDataManagementSystem(
            ExternalDataManagementSystemPE externalDataManagementSystem)
    {
        assert externalDataManagementSystem != null : "Unspecified external data management system.";


        externalDataManagementSystem.setCode(CodeConverter
                .tryToDatabase(externalDataManagementSystem.getCode()));
        doExecute(session -> {
            session.saveOrUpdate(externalDataManagementSystem);
            session.flush();
            return null;
        });
        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format("SAVE/UPDATE: external data management system '%s'.",
                    externalDataManagementSystem));
        }
    }

    @Override
    public ExternalDataManagementSystemPE tryToFindExternalDataManagementSystemById(Long id)
    {
        assert id != null : "Unspecified external data management system id.";

        return currentSession()
                .createQuery(
                        "from " + ENTITY_CLASS.getName() + " e where e.id = :id",
                        ENTITY_CLASS
                )
                .setParameter("id", id)
                .uniqueResultOptional()
                .orElse(null);
    }

    @Override
    public ExternalDataManagementSystemPE tryToFindExternalDataManagementSystemByCode(
            String externalDataManagementSystemCode)
    {
        assert externalDataManagementSystemCode != null : "Unspecified external data management system code.";


        String code = CodeConverter.tryToDatabase(externalDataManagementSystemCode);

        return currentSession()
                .createQuery(
                        "from " + ENTITY_CLASS.getName() + " e where e.code = :code",
                        ENTITY_CLASS
                )
                .setParameter("code", code)
                .uniqueResultOptional()
                .orElse(null);
    }

    @Override
    public List<ExternalDataManagementSystemPE> listExternalDataManagementSystems()
    {
        List<ExternalDataManagementSystemPE> list = currentSession()
                .createQuery("select distinct e from " + ENTITY_CLASS.getName() + " e", ENTITY_CLASS)
                .list();
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format(
                    "%d external data management systems have been found.", list.size()));
        }
        return list;
    }

    @Override
    public void delete(Collection<ExternalDataManagementSystemPE> externalDms)
    {
        Session session = currentSession();

        String hql = "DELETE FROM ContentCopyPE WHERE externalDataManagementSystem IN :externalDms";
        session.createQuery(hql).setParameterList("externalDms", externalDms).executeUpdate();

        for (ExternalDataManagementSystemPE edms : externalDms)
        {
            session.delete(edms);
        }
    }

    @Override
    public List<ExternalDataManagementSystemPE> listExternalDataManagementSystems(Collection<Long> ids)
    {

        List<ExternalDataManagementSystemPE> list = currentSession()
                .createQuery(
                        "from " + ENTITY_CLASS.getName() + " e where e.id in :ids",
                        ENTITY_CLASS
                )
                .setParameter("ids", ids)
                .list();

        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format(
                    "%d external data management systems have been found.", list.size()));
        }
        return list;
    }
}
