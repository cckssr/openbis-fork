/*
 * Copyright ETH 2018 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.systemtest.asapi.v3;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.create.VocabularyCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.create.VocabularyTermCreation;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.update.VocabularyUpdate;
import ch.systemsx.cisd.common.action.IDelegatedAction;

/**
 * @author Franz-Josef Elmer
 */
public class UpdateVocabulariesTest extends AbstractVocabularyTest
{
    @Test
    public void testUpdateDescription()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        VocabularyUpdate update = new VocabularyUpdate();
        VocabularyPermId id = new VocabularyPermId("ORGANISM");
        update.setVocabularyId(id);
        update.setDescription("test description");

        // When
        v3api.updateVocabularies(sessionToken, Arrays.asList(update));

        // Then
        VocabularyFetchOptions fetchOptions = new VocabularyFetchOptions();
        Vocabulary vocabulary = v3api.getVocabularies(sessionToken, Arrays.asList(id), fetchOptions).get(id);
        assertEquals(vocabulary.getDescription(), update.getDescription().getValue());
        assertEquals(vocabulary.getPermId(), id);
        assertEquals(vocabulary.getCode(), id.getPermId());
        assertEquals(vocabulary.isChosenFromList(), true);
        assertEquals(vocabulary.getUrlTemplate(), null);

        v3api.logout(sessionToken);
    }

    @Test
    public void testUpdateAll()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        VocabularyUpdate update = new VocabularyUpdate();
        VocabularyPermId id = new VocabularyPermId("organism");
        update.setVocabularyId(id);
        update.setDescription("test description");
        update.setChosenFromList(false);
        update.setUrlTemplate("https://www.ethz.ch");

        // When
        v3api.updateVocabularies(sessionToken, Arrays.asList(update));

        // Then
        VocabularyFetchOptions fetchOptions = new VocabularyFetchOptions();
        Vocabulary vocabulary = v3api.getVocabularies(sessionToken, Arrays.asList(id), fetchOptions).get(id);
        assertEquals(vocabulary.getDescription(), update.getDescription().getValue());
        assertEquals(vocabulary.getPermId(), id);
        assertEquals(vocabulary.getCode(), id.getPermId());
        assertEquals(vocabulary.isChosenFromList(), false);
        assertEquals(vocabulary.getUrlTemplate(), update.getUrlTemplate().getValue());

        v3api.logout(sessionToken);
    }

    @Test
    public void testUpdateInternal()
    {
        // Given
        String sessionToken = v3api.loginAsSystem();
        VocabularyUpdate update = new VocabularyUpdate();
        VocabularyPermId id = new VocabularyPermId("PLATE_GEOMETRY");
        update.setVocabularyId(id);
        update.setDescription("test internal");

        // When
        v3api.updateVocabularies(sessionToken, Arrays.asList(update));

        // Then
        Vocabulary vocabulary = v3api.getVocabularies(sessionToken, Arrays.asList(id), new VocabularyFetchOptions()).get(id);
        assertEquals(vocabulary.getDescription(), update.getDescription().getValue());
        assertEquals(vocabulary.isManagedInternally(), true);

        v3api.logout(sessionToken);
    }

    @Test
    public void testUpdateVocabularyWithMissingId()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        VocabularyUpdate update = new VocabularyUpdate();

        assertUserFailureException(new IDelegatedAction()
            {
                @Override
                public void execute()
                {
                    // When
                    v3api.updateVocabularies(sessionToken, Arrays.asList(update));
                }
            },
                // Then
                "Vocabulary id cannot be null.");

        v3api.logout(sessionToken);
    }

    @Test
    public void testUpdateVocabularyWithUnknownId()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        VocabularyUpdate update = new VocabularyUpdate();
        update.setVocabularyId(new VocabularyPermId("unknown"));

        assertUserFailureException(new IDelegatedAction()
            {
                @Override
                public void execute()
                {
                    // When
                    v3api.updateVocabularies(sessionToken, Arrays.asList(update));
                }
            },
                // Then
                "[UNKNOWN] has not been found.");

        v3api.logout(sessionToken);
    }

    @Test(dataProvider = PROVIDE_USERS_NOT_ALLOWED_TO_MANAGE_VOCABULARIES)
    public void testUpdateWithUserCausingAuthorizationFailure(final String user)
    {
        VocabularyPermId vocabularyId = new VocabularyPermId("ORGANISM");
        assertUnauthorizedObjectAccessException(new IDelegatedAction()
            {
                @Override
                public void execute()
                {
                    String sessionToken = v3api.login(user, PASSWORD);
                    VocabularyUpdate update = new VocabularyUpdate();
                    update.setVocabularyId(vocabularyId);
                    v3api.updateVocabularies(sessionToken, Arrays.asList(update));
                }
            }, vocabularyId);
    }

    @Test(dataProvider = PROVIDE_USERS_NOT_ALLOWED_TO_MANAGE_INTERNAL_VOCABULARIES)
    public void testUpdateInternalWithUserCausingAuthorizationFailure(final String user)
    {
        VocabularyPermId vocabularyId = new VocabularyPermId("PLATE_GEOMETRY");
        assertUnauthorizedObjectAccessException(new IDelegatedAction()
            {
                @Override
                public void execute()
                {
                    String sessionToken = v3api.login(user, PASSWORD);
                    VocabularyUpdate update = new VocabularyUpdate();
                    update.setVocabularyId(vocabularyId);
                    v3api.updateVocabularies(sessionToken, Arrays.asList(update));
                }
            }, vocabularyId);
    }

    @Test
    public void testLogging()
    {
        String sessionToken = v3api.loginAsSystem();

        VocabularyUpdate update = new VocabularyUpdate();
        update.setVocabularyId(new VocabularyPermId("PLATE_GEOMETRY"));

        VocabularyUpdate update2 = new VocabularyUpdate();
        update2.setVocabularyId(new VocabularyPermId("ORGANISM"));

        v3api.updateVocabularies(sessionToken, Arrays.asList(update, update2));

        assertAccessLog(
                "update-vocabularies  VOCABULARY_UPDATES('[VocabularyUpdate[vocabularyId=PLATE_GEOMETRY], VocabularyUpdate[vocabularyId=ORGANISM]]')");
    }

    @Test(expectedExceptions = UserFailureException.class, expectedExceptionsMessageRegExp = "(?s).*Vocabulary internal flag can be modified only by the system user.*")
    public void testUpdateMakeInternalAsAdmin_failure()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        VocabularyCreation vocabularyCreation = new VocabularyCreation();
        vocabularyCreation.setCode("NON_INTERNAL_TEST");
        vocabularyCreation.setManagedInternally(false);
        vocabularyCreation.setDescription("creation test");
        vocabularyCreation.setChosenFromList(true);
        vocabularyCreation.setUrlTemplate("https://en.wikipedia.org/wiki/${term}");
        VocabularyTermCreation term1 = new VocabularyTermCreation();
        term1.setCode("OMEGA");
        VocabularyTermCreation term2 = new VocabularyTermCreation();
        term2.setCode("ALPHA");
        vocabularyCreation.setTerms(Arrays.asList(term1, term2));
        List<VocabularyPermId> vocabularies = v3api.createVocabularies(sessionToken,
                Arrays.asList(vocabularyCreation));



        VocabularyUpdate update = new VocabularyUpdate();
        update.setManagedInternally(true);
        update.setVocabularyId(new VocabularyPermId("NON_INTERNAL_TEST"));
        v3api.updateVocabularies(sessionToken, Arrays.asList(update));
    }

    @Test
    public void testUpdateMakeInternalAsSystem()
    {
        String sessionToken = v3api.loginAsSystem();

        VocabularyCreation vocabularyCreation = new VocabularyCreation();
        vocabularyCreation.setCode("NON_INTERNAL_TEST");
        vocabularyCreation.setManagedInternally(false);
        vocabularyCreation.setDescription("creation test");
        vocabularyCreation.setChosenFromList(true);
        vocabularyCreation.setUrlTemplate("https://en.wikipedia.org/wiki/${term}");
        VocabularyTermCreation term1 = new VocabularyTermCreation();
        term1.setCode("OMEGA");
        VocabularyTermCreation term2 = new VocabularyTermCreation();
        term2.setCode("ALPHA");
        vocabularyCreation.setTerms(Arrays.asList(term1, term2));
        List<VocabularyPermId> vocabularies = v3api.createVocabularies(sessionToken,
                Arrays.asList(vocabularyCreation));



        VocabularyUpdate update = new VocabularyUpdate();
        update.setManagedInternally(true);
        update.setVocabularyId(new VocabularyPermId("NON_INTERNAL_TEST"));
        v3api.updateVocabularies(sessionToken, Arrays.asList(update));


        Vocabulary vocabulary = v3api.getVocabularies(sessionToken, Arrays.asList(new VocabularyPermId("NON_INTERNAL_TEST")), new VocabularyFetchOptions()).get(new VocabularyPermId("NON_INTERNAL_TEST"));
        assertTrue(vocabulary.isManagedInternally());
    }

    @Test(expectedExceptions = UserFailureException.class, expectedExceptionsMessageRegExp = "(?s).*Vocabulary can not be made non-internal if it has an internal term!.*")
    public void testUpdateMakeNonInternal_failure()
    {
        String sessionToken = v3api.loginAsSystem();

        VocabularyCreation vocabularyCreation = new VocabularyCreation();
        vocabularyCreation.setCode("NON_INTERNAL_TEST");
        vocabularyCreation.setManagedInternally(true);
        vocabularyCreation.setDescription("creation test");
        vocabularyCreation.setChosenFromList(true);
        vocabularyCreation.setUrlTemplate("https://en.wikipedia.org/wiki/${term}");
        VocabularyTermCreation term1 = new VocabularyTermCreation();
        term1.setCode("OMEGA");
        term1.setManagedInternally(true);
        vocabularyCreation.setTerms(Arrays.asList(term1));
        List<VocabularyPermId> vocabularies = v3api.createVocabularies(sessionToken,
                Arrays.asList(vocabularyCreation));

        VocabularyUpdate update = new VocabularyUpdate();
        update.setManagedInternally(false);
        update.setVocabularyId(new VocabularyPermId("NON_INTERNAL_TEST"));
        v3api.updateVocabularies(sessionToken, Arrays.asList(update));
    }

}
