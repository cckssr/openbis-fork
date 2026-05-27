/*
 * Copyright ETH 2020 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.server.business.bo;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.update.VocabularyUpdate;
import ch.systemsx.cisd.common.exceptions.AuthorizationFailureException;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IVocabularyTermUpdates;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IVocabularyUpdates;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.UpdatedVocabularyTerm;
import ch.systemsx.cisd.openbis.generic.shared.dto.*;

public class InternalVocabularyAuthorization
{

    public void canCreateVocabulary(Session session, VocabularyPE vocabulary)
    {
        checkVocabulary(session, vocabulary);
    }

    public void canUpdateVocabulary(Session session, VocabularyPE vocabulary, VocabularyUpdate update)
    {
        checkVocabulary(session, vocabulary);
        if(isSystemUser(session) == false)
        {
            if ((!vocabulary.isManagedInternally() && update.getManagedInternally().isModified() &&
                    update.getManagedInternally().getValue() == true) ||
                (vocabulary.isManagedInternally() && update.getManagedInternally().isModified() &&
                        update.getManagedInternally().getValue() == false))
            {
                throw new AuthorizationFailureException(
                        "Vocabulary internal flag can be modified only by the system user.");
            }
        }
    }

    public void canUpdateVocabulary(Session session, VocabularyPE vocabulary, IVocabularyUpdates update)
    {
        checkVocabulary(session, vocabulary);
        if(((!vocabulary.isManagedInternally() && update.isManagedInternally()) || (vocabulary.isManagedInternally() && !update.isManagedInternally()))
                && isSystemUser(session) == false) {
            throw new AuthorizationFailureException("Vocabulary internal flag can be modified only by the system user.");
        }
    }

    public void canUpdateVocabulary(Session session, VocabularyPE vocabulary, VocabularyUpdatesDTO update)
    {
        checkVocabulary(session, vocabulary);
        if(((!vocabulary.isManagedInternally() && update.isManagedInternally()) || (vocabulary.isManagedInternally() && !update.isManagedInternally()))
                && isSystemUser(session) == false) {
            throw new AuthorizationFailureException("Vocabulary internal flag can be modified only by the system user.");
        }
    }

    public void canDeleteVocabulary(Session session, VocabularyPE vocabulary)
    {
        checkVocabulary(session, vocabulary);
    }

    public void canCreateTerm(Session session, VocabularyPE vocabulary, VocabularyTermPE term)
    {
        // do not check anything - allow new terms to be created even in internally managed vocabularies
    }

    public void canUpdateTermToOfficial(Session session, VocabularyPE vocabulary, VocabularyTermPE term)
    {
        checkTerm(session, vocabulary, term);
    }

    public void canUpdateTerm(Session session, VocabularyPE vocabulary, VocabularyTermPE term, IVocabularyTermUpdates updates)
    {
        checkTerm(session, vocabulary, term);
        if(((!term.isManagedInternally() && updates.isManagedInternally()) || (term.isManagedInternally() && !updates.isManagedInternally()))
                && isSystemUser(session) == false) {
            throw new AuthorizationFailureException("Vocabulary Term internal flag can only be modified by the system user.");
        }
        if(!vocabulary.isManagedInternally() &&  updates.isManagedInternally()) {
            throw new UserFailureException("Only internal vocabularies can have internal terms.");
        }
    }

    public void canUpdateTerm(Session session, VocabularyPE vocabulary, VocabularyTermPE term, UpdatedVocabularyTerm updates)
    {
        checkTerm(session, vocabulary, term);
        if(((!term.isManagedInternally() && updates.isManagedInternally()) || (term.isManagedInternally() && !updates.isManagedInternally()))
                && isSystemUser(session) == false) {
            throw new AuthorizationFailureException("Vocabulary Term internal flag can only be modified by the system user.");
        }
        if(!vocabulary.isManagedInternally() &&  updates.isManagedInternally()) {
            throw new UserFailureException("Only internal vocabularies can have internal terms.");
        }
    }

    public void canDeleteTerm(Session session, VocabularyPE vocabulary, VocabularyTermPE term)
    {
        checkTerm(session, vocabulary, term);
    }

    private void checkVocabulary(Session session, VocabularyPE vocabulary)
    {
        if (vocabulary.isManagedInternally() && isSystemUser(session) == false)
        {
            throw new AuthorizationFailureException("Internal vocabularies can be managed only by the system user.");
        }
    }

    private void checkTerm(Session session, VocabularyPE vocabulary, VocabularyTermPE term)
    {
        if (vocabulary.isManagedInternally() && term.isManagedInternally() && isSystemUser(session) == false)
        {
            throw new AuthorizationFailureException(
                    "Internal vocabulary terms can be managed only by the system user.");
        }
    }

    private boolean isSystemTerm(VocabularyTermPE term)
    {
        PersonPE registrator = term.getRegistrator();

        if (registrator == null)
        {
            throw new AuthorizationFailureException("Could not check access because the vocabulary term does not have any registrator assigned.");
        } else
        {
            return registrator.isSystemUser();
        }
    }

    private boolean isSystemUser(Session session)
    {
        PersonPE user = session.tryGetPerson();

        if (user == null)
        {
            throw new AuthorizationFailureException("Could not check access because the current session does not have any user assigned.");
        } else
        {
            return user.isSystemUser();
        }
    }

}
