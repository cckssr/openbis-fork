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

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.orm.hibernate5.HibernateTemplate;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.common.reflection.MethodUtils;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IVocabularyDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.shared.basic.CodeConverter;
import ch.systemsx.cisd.openbis.generic.shared.dto.VocabularyPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.VocabularyTermPE;

/**
 * Implementation of {@link IVocabularyDAO} for databases.
 * 
 * @author Christian Ribeaud
 */
final class VocabularyDAO extends AbstractGenericEntityDAO<VocabularyPE> implements IVocabularyDAO
{
    private static final Class<VocabularyPE> ENTITY_CLASS = VocabularyPE.class;

    private static final String TABLE_NAME = ENTITY_CLASS.getSimpleName();

    /**
     * This logger does not output any SQL statement. If you want to do so, you had better set an appropriate debugging level for class
     * {@link JdbcAccessor}.
     * </p>
     */
    private static final Logger operationLog =
            LogFactory.getLogger(LogCategory.OPERATION, VocabularyDAO.class);

    VocabularyDAO(final SessionFactory sessionFactory, EntityHistoryCreator historyCreator)
    {
        super(sessionFactory, ENTITY_CLASS, historyCreator);
    }

    //
    // IVocabularyDAO
    //

    @Override
    public final void createOrUpdateVocabulary(final VocabularyPE vocabularyPE)
    {
        assert vocabularyPE != null : "Given vocabulary can not be null.";
        validatePE(vocabularyPE);

        doExecute(session -> {
                    session.save(vocabularyPE);
                    session.flush();
                    return null;
                }
        );
        if (operationLog.isInfoEnabled())
        {
            operationLog.info(String.format("ADD/UPDATE: vocabulary '%s'.", vocabularyPE));
        }

    }

    @Override
    public final VocabularyPE tryFindVocabularyByCode(final String vocabularyCode)
    {
        assert vocabularyCode != null : "Unspecified vocabulary code.";

        final String mangledVocabularyCode = CodeConverter.tryToDatabase(vocabularyCode);
        final List<VocabularyPE> list =
                find(VocabularyPE.class,
                        String.format("select v from %s v where v.simpleCode = ?1 ",
                                TABLE_NAME),
                        toArray(mangledVocabularyCode));
        final VocabularyPE entity = tryFindEntity(list, "vocabulary", vocabularyCode);
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(%s): '%s'.", MethodUtils.getCurrentMethod()
                    .getName(), vocabularyCode, entity));
        }
        return entity;
    }

    @Override
    public final List<VocabularyPE> listVocabularies(boolean excludeInternal)
    {
        String excludeInternalQuery = " where v.managedInternally = false";
        final List<VocabularyPE> list =
                find(VocabularyPE.class,
                        String.format("from %s v "
                                + (excludeInternal ? excludeInternalQuery : ""), TABLE_NAME));
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(list.size() + " vocabulary(ies) have been found.");
        }
        return list;
    }

    // this one could be moved to VocabularyTermDAO if we create it
    @Override
    public VocabularyTermPE tryFindVocabularyTermByCode(VocabularyPE vocabulary, String code)
    {
        assert vocabulary != null : "Unspecified vocabulary.";
        assert code != null : "Unspecified code.";

        VocabularyTermPE found = currentSession()
            .createQuery(
            "from VocabularyTermPE vt " +
                    "where vt.simpleCode = :code and vt.vocabularyInternal = :vocab",
                    VocabularyTermPE.class)
            .setParameter("code", code)
            .setParameter("vocab", vocabulary)
            .uniqueResultOptional()
            .orElse(null);

        final VocabularyTermPE result = tryGetEntity(found);
        if (operationLog.isDebugEnabled())
        {
            operationLog.debug(String.format("%s(%s): '%s'.", vocabulary.getCode(), code, result));
        }
        return result;
    }

}
