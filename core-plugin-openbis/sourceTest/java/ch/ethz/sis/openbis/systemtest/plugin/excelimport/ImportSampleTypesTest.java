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
package ch.ethz.sis.openbis.systemtest.plugin.excelimport;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.PluginType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.search.SemanticAnnotationSearchCriteria;
import ch.ethz.sis.openbis.generic.server.asapi.v3.IApplicationServerInternalApi;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.testng.Assert.*;

@ContextConfiguration(locations = "classpath:applicationContext.xml")
@Transactional(transactionManager = "transaction-manager")
@Rollback
public class ImportSampleTypesTest extends AbstractImportTest
{

    private static final String SAMPLE_TYPES_XLS = "sample_types/normal_samples.xls";

    private static final String SAMPLE_TYPES_XLS_DIFFERENT_PROPERTY_ASSIGN = "sample_types/normal_samples_v2.xls";

    private static final String SAMPLE_TYPES_WITH_DYNAMIC_SCRIPT = "sample_types/with_dynamic_script.xls";

    private static final String SAMPLE_TYPES_WITH_VALIDATION_SCRIPT = "sample_types/with_validation_script.xls";

    private static final String SAMPLE_TYPES_WITH_VOCABULARY = "sample_types/with_vocabulary_in_xls.xls";

    private static final String SAMPLE_TYPES_WITH_VOCABULARY_ON_SERVER = "sample_types/with_vocabulary_on_server.xls";

    private static final String VOCABULARY_DETECTION = "sample_types/vocabulary_detection.xls";

    private static final String SAMPLE_TYPES_WITH_AUTO_GENERATED_CODES = "sample_types/with_auto_generated_codes.xls";

    private static final String SAMPLE_TYPE_NO_CODE = "sample_types/no_code.xls";

    private static final String SAMPLE_TYPES_WITH_SEMANTIC_ANNOTATIONS_XLS =
            "sample_types/normal_samples_semantic_annotation.xls";

    private static final String SAMPLE_TYPES_WITH_SEMANTIC_ANNOTATIONS_BROKEN_XLS =
            "sample_types/normal_samples_semantic_annotation_broken.xls";

    private static final String SAMPLE_TYPES_WITH_MULTIPLE_SEMANTIC_ANNOTATIONS_XLS =
            "sample_types/normal_samples_semantic_multiple_annotations.xls";

    private static final String SAMPLE_TYPES_WITH_NO_SEMANTIC_ANNOTATIONS =
            "sample_types/normal_samples_no_semantic_annotations.xls";

    private static final String SAMPLE_TYPES_WITH_PROPERTY_SEMANTIC_ANNOATIONS =
            "sample_types/normal_samples_semantic_annotation_property_type.xls";

    private static final String SAMPLE_TYPES_WITH_PROPERTY_AND_MULITPLE_SEMANTIC_ANNOATIONS =
            "sample_types/normal_samples_multiple_semantic_annotations_property_type.xls";



    @Autowired
    private IApplicationServerInternalApi v3api;

    private static String FILES_DIR;

    @BeforeClass
    public void setupClass() throws IOException
    {
        String f = ImportSampleTypesTest.class.getName().replace(".", "/");
        FILES_DIR = f.substring(0, f.length() - ImportSampleTypesTest.class.getSimpleName().length()) + "test_files/";
    }

    @Test
    @DirtiesContext
    public void testNormalSampleTypesAreCreated() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, SAMPLE_TYPES_XLS));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");

        // THEN
        assertFalse(antibody.isAutoGeneratedCode());
    }

    @Test
    @DirtiesContext
    public void testPropertyTypeAssignmentsFromNormalSampleTypesAreCreated() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, SAMPLE_TYPES_XLS));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");

        // THEN
        boolean allMandatory = antibody.getPropertyAssignments().stream().allMatch(propAssignment -> propAssignment.isMandatory() == true);
        boolean allShownInEditView =
                antibody.getPropertyAssignments().stream().allMatch(propAssignment -> propAssignment.isShowInEditView() == true);
        boolean generalInformationExists =
                antibody.getPropertyAssignments().stream().anyMatch(propAssignment -> propAssignment.getSection().equals("General information"));
        boolean someOtherSectionExists =
                antibody.getPropertyAssignments().stream().anyMatch(propAssignment -> propAssignment.getSection().equals("Some other section"));
        boolean threePropertyAssignments = antibody.getPropertyAssignments().size() == 3;
        assertTrue(threePropertyAssignments);
        assertTrue(generalInformationExists);
        assertTrue(someOtherSectionExists);
        assertTrue(allShownInEditView);
        assertTrue(allMandatory);
    }

    @Test
    @DirtiesContext
    public void testPropertyTypeAssignmentsFromNormalv2SampleTypesAreCreated() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPES_XLS_DIFFERENT_PROPERTY_ASSIGN));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");

        // THEN
        boolean allNotMandatory = antibody.getPropertyAssignments().stream().allMatch(propAssignment -> propAssignment.isMandatory() == false);
        boolean allNotShownInEditView =
                antibody.getPropertyAssignments().stream().allMatch(propAssignment -> propAssignment.isShowInEditView() == false);
        boolean threePropertyAssignments = antibody.getPropertyAssignments().size() == 3;
        assertTrue(threePropertyAssignments);
        assertTrue(allNotShownInEditView);
        assertTrue(allNotMandatory);
    }

    @Test
    @DirtiesContext
    public void testPropertyTypesFromNormalSampleTypesAreCreated() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, SAMPLE_TYPES_XLS));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");

        // THEN
        boolean namePropertyExists =
                antibody.getPropertyAssignments().stream().anyMatch(propAssignment -> propAssignment.getPropertyType().getCode().equals("NAME"));
        boolean forWhatPropertyExists =
                antibody.getPropertyAssignments().stream().anyMatch(propAssignment -> propAssignment.getPropertyType().getCode().equals("FOR_WHAT"));
        boolean epitopePropertyExists =
                antibody.getPropertyAssignments().stream().anyMatch(propAssignment -> propAssignment.getPropertyType().getCode().equals("EPITOPE"));

        assertNotNull(antibody);
        assertTrue(namePropertyExists);
        assertTrue(forWhatPropertyExists);
        assertTrue(epitopePropertyExists);
    }

    @Test
    @DirtiesContext
    public void testSampleTypesWithPropertyHavingDynamicScript() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String[] sessionWorkspaceFilePaths = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPES_WITH_DYNAMIC_SCRIPT), FilenameUtils.concat(FILES_DIR, DYNAMIC_SCRIPT), FilenameUtils.concat(FILES_DIR, VALIDATION_SCRIPT));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePaths[0]));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");

        // THEN
        final Plugin dynamicScript = antibody.getPropertyAssignments().get(0).getPlugin();
        assertNotNull(dynamicScript);
        assertEquals(dynamicScript.getName().toUpperCase(), "DYNAMIC");
        assertEquals(dynamicScript.getScript(), "def calculate():\n    return 1");
        assertEquals(dynamicScript.getPluginType(), PluginType.DYNAMIC_PROPERTY);
    }

    @Test
    @DirtiesContext
    public void testSampleTypesWithPropertyHavingValidationScript() throws IOException
    {
        // GIVEN
        final String[] sessionWorkspaceFilePaths = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPES_WITH_VALIDATION_SCRIPT), FilenameUtils.concat(FILES_DIR, VALIDATION_SCRIPT));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePaths[0]));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");

        // THEN
        Plugin validationScript = antibody.getValidationPlugin();
        assertNotNull(validationScript);
        assertEquals(validationScript.getName().toUpperCase(), "VALID");
        assertEquals(validationScript.getScript(), "def validate(entity, isNew):\n  if isNew:\n    return");
        assertEquals(validationScript.getPluginType(), PluginType.ENTITY_VALIDATION);
    }

    @Test
    @DirtiesContext
    public void testSampleTypesWithVocabularyInXls() throws IOException
    {
        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPES_WITH_VOCABULARY));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");

        // THEN
        PropertyAssignment propertyAssignment = antibody.getPropertyAssignments().get(0);
        assertNotNull(propertyAssignment);
        assertEquals(propertyAssignment.getPropertyType().getVocabulary().getCode(), "DETECTION");
    }

    @Test
    @DirtiesContext
    public void testSampleTypesWithVocabularyOnServer() throws IOException
    {
        // GIVEN
        final String vocabularySessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, VOCABULARY_DETECTION));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(vocabularySessionWorkspaceFilePath));

        final String sampleTypesSessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPES_WITH_VOCABULARY_ON_SERVER));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sampleTypesSessionWorkspaceFilePath));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");

        // THEN
        PropertyAssignment propertyAssignment = antibody.getPropertyAssignments().get(0);
        assertNotNull(propertyAssignment);
        assertEquals(propertyAssignment.getPropertyType().getVocabulary().getCode(), "DETECTION");
    }

    @Test
    @DirtiesContext
    public void testSampleTypesWithAutoGeneratedCodeAttribute() throws IOException
    {
        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPES_WITH_AUTO_GENERATED_CODES));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "SECONDBODY");

        // THEN
        assertTrue(antibody.isAutoGeneratedCode());
    }

    @Test(expectedExceptions = UserFailureException.class, expectedExceptionsMessageRegExp = "(s?).*Mandatory field is missing or empty: Code.*")
    public void shouldThrowExceptionIfNoSampleCode() throws IOException
    {
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken, FilenameUtils.concat(FILES_DIR, SAMPLE_TYPE_NO_CODE));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));
    }

    @Test
    @DirtiesContext
    public void testSingleSemanticAnnotationIsCreated() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPES_WITH_SEMANTIC_ANNOTATIONS_XLS));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");
        List<SemanticAnnotation> semanticAnnotations = antibody.getSemanticAnnotations();

        // THEN
        assertTrue(semanticAnnotations.get(0).getDescriptorOntologyId()
                .equals("https://en.wikipedia.org/"));
        assertTrue(semanticAnnotations.get(0).getDescriptorOntologyVersion()
                .equals("https://en.wikipedia.org/wiki/Wikipedia:About"));
        assertTrue(semanticAnnotations.get(0).getDescriptorAccessionId()
                .equals("https://en.wikipedia.org/wiki/Antibody"));

    }

    @Test(expectedExceptions = { UserFailureException.class })
    @DirtiesContext
    public void testInconsistentNumberOfAnnotationsThrowException() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPES_WITH_SEMANTIC_ANNOTATIONS_BROKEN_XLS));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));
    }

    @Test
    @DirtiesContext
    public void testMultipleSemanticAnnotationAreCreated() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR,
                        SAMPLE_TYPES_WITH_MULTIPLE_SEMANTIC_ANNOTATIONS_XLS));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");
        List<SemanticAnnotation> semanticAnnotations = antibody.getSemanticAnnotations();

        // THEN
        assertTrue(semanticAnnotations.get(0).getDescriptorOntologyId()
                .equals("https://en.wikipedia.org/"));
        assertTrue(semanticAnnotations.get(0).getDescriptorOntologyVersion()
                .equals("https://en.wikipedia.org/wiki/Wikipedia:About"));
        assertTrue(semanticAnnotations.get(0).getDescriptorAccessionId()
                .equals("https://en.wikipedia.org/wiki/Antibody"));
        assertTrue(semanticAnnotations.get(1).getDescriptorOntologyId()
                .equals("https://en.wikipedia.org/"));
        assertTrue(semanticAnnotations.get(1).getDescriptorOntologyVersion()
                .equals("https://en.wikipedia.org/wiki/Wikipedia:About"));
        assertTrue(semanticAnnotations.get(1).getDescriptorAccessionId()
                .equals("https://en.wikipedia.org/wiki/Antibody2"));

    }

    @Test
    @DirtiesContext
    public void testNoSemanticAnnoationsMeansNoSemanticAnnotationsCreated() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPES_WITH_NO_SEMANTIC_ANNOTATIONS));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");
        List<SemanticAnnotation> semanticAnnotations = antibody.getSemanticAnnotations();
        assertTrue(semanticAnnotations.isEmpty());

    }

    @Test
    @DirtiesContext
    public void testSemanticAnnotationsWithProperties() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR, SAMPLE_TYPES_WITH_PROPERTY_SEMANTIC_ANNOATIONS));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");
        var propertyType = TestUtils.getPropertyType(v3api, sessionToken, "NAME");
        propertyType.getSampleType();
        SemanticAnnotationFetchOptions semanticAnnotationFetchOptions =
                new SemanticAnnotationFetchOptions();
        semanticAnnotationFetchOptions.withPropertyAssignment();
        semanticAnnotationFetchOptions.withPropertyType();
        semanticAnnotationFetchOptions.withEntityType();

        SemanticAnnotationSearchCriteria semanticAnnotationSearchCriteria =
                new SemanticAnnotationSearchCriteria();
        semanticAnnotationSearchCriteria.withPredicateOntologyId().thatEquals("Ontology Id");

        SearchResult<SemanticAnnotation> semanticAnnotations =
                v3api.searchSemanticAnnotations(sessionToken, semanticAnnotationSearchCriteria,
                        semanticAnnotationFetchOptions);

        assertEquals(semanticAnnotations.getObjects().get(0).getPredicateOntologyId(),
                "Ontology Id");
        assertEquals(semanticAnnotations.getObjects().get(0).getPredicateOntologyVersion(),
                "Ontology Version");
        assertEquals(semanticAnnotations.getObjects().get(0).getPredicateAccessionId(),
                "Ontology Annotation Id");
        assertEquals(semanticAnnotations.getObjects().get(0).getPropertyAssignment().getPermId()
                .getEntityTypeId().toString(), "ANTIBODY (SAMPLE)");
        assertEquals(semanticAnnotations.getObjects().get(0).getPropertyAssignment().getPermId()
                .getPropertyTypeId().toString(), "NAME");


    }

    @Test
    @DirtiesContext
    public void testMultipleSemanticAnnotationsWithProperties() throws IOException
    {
        // the Excel contains internally managed property types which can be only manipulated by the system user
        sessionToken = v3api.loginAsSystem();

        // GIVEN
        final String sessionWorkspaceFilePath = uploadToAsSessionWorkspace(sessionToken,
                FilenameUtils.concat(FILES_DIR,
                        SAMPLE_TYPES_WITH_PROPERTY_AND_MULITPLE_SEMANTIC_ANNOATIONS));
        TestUtils.createFrom(v3api, sessionToken, Paths.get(sessionWorkspaceFilePath));

        // WHEN
        SampleType antibody = TestUtils.getSampleType(v3api, sessionToken, "ANTIBODY");
        var propertyType = TestUtils.getPropertyType(v3api, sessionToken, "NAME");
        propertyType.getSampleType();
        SemanticAnnotationFetchOptions semanticAnnotationFetchOptions =
                new SemanticAnnotationFetchOptions();
        semanticAnnotationFetchOptions.withPropertyAssignment();
        semanticAnnotationFetchOptions.withPropertyType();
        semanticAnnotationFetchOptions.withEntityType();

        {
            SemanticAnnotationSearchCriteria semanticAnnotationSearchCriteria =
                    new SemanticAnnotationSearchCriteria();

            semanticAnnotationSearchCriteria.withPredicateOntologyId().thatEquals("Ontology Id");

            SearchResult<SemanticAnnotation> semanticAnnotations =
                    v3api.searchSemanticAnnotations(sessionToken, semanticAnnotationSearchCriteria,
                            semanticAnnotationFetchOptions);

            assertEquals(semanticAnnotations.getObjects().get(0).getPredicateOntologyId(),
                    "Ontology Id");
            assertEquals(semanticAnnotations.getObjects().get(0).getPredicateOntologyVersion(),
                    "Ontology Version");
            assertEquals(semanticAnnotations.getObjects().get(0).getPredicateAccessionId(),
                    "Ontology Annotation Id");
            assertEquals(semanticAnnotations.getObjects().get(0).getPropertyAssignment().getPermId()
                    .getEntityTypeId().toString(), "ANTIBODY (SAMPLE)");
            assertEquals(semanticAnnotations.getObjects().get(0).getPropertyAssignment().getPermId()
                    .getPropertyTypeId().toString(), "NAME");
        }

        {
            SemanticAnnotationSearchCriteria semanticAnnotationSearchCriteria =
                    new SemanticAnnotationSearchCriteria();
            semanticAnnotationSearchCriteria.withPredicateOntologyId().thatEquals("Wikipedia");

            SearchResult<SemanticAnnotation> semanticAnnotations =
                    v3api.searchSemanticAnnotations(sessionToken, semanticAnnotationSearchCriteria,
                            semanticAnnotationFetchOptions);

            assertEquals(semanticAnnotations.getObjects().get(0).getPredicateOntologyId(),
                    "Wikipedia");
            assertEquals(semanticAnnotations.getObjects().get(0).getPredicateOntologyVersion(),
                    "Ontology Version 2");
            assertEquals(semanticAnnotations.getObjects().get(0).getPredicateAccessionId(),
                    "Ontology Annotation Id 2");
            assertEquals(semanticAnnotations.getObjects().get(0).getPropertyAssignment().getPermId()
                    .getEntityTypeId().toString(), "ANTIBODY (SAMPLE)");
            assertEquals(semanticAnnotations.getObjects().get(0).getPropertyAssignment().getPermId()
                    .getPropertyTypeId().toString(), "NAME");

        }

    }


}
