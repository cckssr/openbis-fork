/*
 * Copyright ETH 2023 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ch.ethz.sis.openbis.systemtest.asapi.v3;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetTypeSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentTypeSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportMode;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleTypeSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.search.VocabularySearchCriteria;
import ch.systemsx.cisd.common.action.IDelegatedAction;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

public class UncompressedImportTest extends AbstractImportTest
{

    @Test
    public void testDataImport() throws Exception
    {
        final String[] sessionWorkspaceFiles = uploadToAsSessionWorkspace(sessionToken, "import.xlsx");

        final ImportData importData = new ImportData(ImportFormat.EXCEL, sessionWorkspaceFiles);
        final ImportOptions importOptions = new ImportOptions(ImportMode.UPDATE_IF_EXISTS);

        v3api.executeImport(sessionToken, importData, importOptions);

        final VocabularySearchCriteria vocabularySearchCriteria = new VocabularySearchCriteria();
        vocabularySearchCriteria.withCode().thatEquals("DETECTION");

        final VocabularyFetchOptions vocabularyFetchOptions = new VocabularyFetchOptions();
        vocabularyFetchOptions.withTerms();

        final SearchResult<Vocabulary> vocabularySearchResult =
                v3api.searchVocabularies(sessionToken, vocabularySearchCriteria, vocabularyFetchOptions);

        assertEquals(vocabularySearchResult.getTotalCount(), 1);

        final List<VocabularyTerm> vocabularyTerms = vocabularySearchResult.getObjects().get(0).getTerms();
        assertEquals(vocabularyTerms.size(), 2);
        assertEquals(vocabularyTerms.stream().map(VocabularyTerm::getCode).collect(Collectors.toSet()), Set.of("HRP", "AAA"));
    }

    @Test
    public void testLargeDataImport() throws Exception
    {
        final String[] sessionWorkspaceFiles = uploadToAsSessionWorkspace(sessionToken, "import_large_cell.xlsx", "data/value-M20.txt");

        final ImportData importData = new ImportData(ImportFormat.EXCEL, sessionWorkspaceFiles[0]);
        final ImportOptions importOptions = new ImportOptions(ImportMode.UPDATE_IF_EXISTS);

        v3api.executeImport(sessionToken, importData, importOptions);

        final SampleSearchCriteria sampleSearchCriteria = new SampleSearchCriteria();
        sampleSearchCriteria.withCode().thatEquals("AAA");

        final SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withProperties();

        final SearchResult<Sample> sampleSearchResult = v3api.searchSamples(sessionToken, sampleSearchCriteria, sampleFetchOptions);

        assertEquals(sampleSearchResult.getTotalCount(), 1);

        final String propertyValue = sampleSearchResult.getObjects().get(0).getStringProperty("NOTES");
        assertNotNull(propertyValue);
        assertTrue(propertyValue.startsWith("Lorem ipsum dolor sit amet"));
        assertTrue(propertyValue.length() > Short.MAX_VALUE);
    }

    @Test
    public void testImportOptionsUpdateIfExists() throws Exception
    {
        final String[] sessionWorkspaceFiles = uploadToAsSessionWorkspace(sessionToken, "existing_vocabulary.xlsx");

        final ImportData importData = new ImportData(ImportFormat.EXCEL, sessionWorkspaceFiles);
        final ImportOptions importOptions = new ImportOptions(ImportMode.UPDATE_IF_EXISTS);

        v3api.executeImport(sessionToken, importData, importOptions);

        final VocabularySearchCriteria vocabularySearchCriteria = new VocabularySearchCriteria();
        vocabularySearchCriteria.withCode().thatEquals("TEST_VOCABULARY");

        final VocabularyFetchOptions vocabularyFetchOptions = new VocabularyFetchOptions();
        vocabularyFetchOptions.withTerms();

        final SearchResult<Vocabulary> vocabularySearchResult =
                v3api.searchVocabularies(sessionToken, vocabularySearchCriteria, vocabularyFetchOptions);

        assertEquals(vocabularySearchResult.getTotalCount(), 1);
        assertEquals(vocabularySearchResult.getObjects().get(0).getDescription(), "Test vocabulary with modifications");

        final List<VocabularyTerm> vocabularyTerms = vocabularySearchResult.getObjects().get(0).getTerms();
        assertEquals(vocabularyTerms.stream().map(VocabularyTerm::getCode).collect(Collectors.toSet()),
                Set.of("TEST_TERM_A", "TEST_TERM_B", "TEST_TERM_C"));
        assertEquals(vocabularyTerms.stream().map(VocabularyTerm::getLabel).collect(Collectors.toSet()),
                Set.of("Test term A", "Test term B", "Test term C"));
    }

    @Test
    public void testImportOptionsIgnoreExisting() throws Exception
    {
        final String[] sessionWorkspaceFiles = uploadToAsSessionWorkspace(sessionToken, "existing_vocabulary.xlsx");

        final ImportData importData = new ImportData(ImportFormat.EXCEL, sessionWorkspaceFiles);
        final ImportOptions importOptions = new ImportOptions(ImportMode.IGNORE_EXISTING);

        v3api.executeImport(sessionToken, importData, importOptions);

        final VocabularySearchCriteria vocabularySearchCriteria = new VocabularySearchCriteria();
        vocabularySearchCriteria.withCode().thatEquals("TEST_VOCABULARY");

        final VocabularyFetchOptions vocabularyFetchOptions = new VocabularyFetchOptions();
        vocabularyFetchOptions.withTerms();

        final SearchResult<Vocabulary> vocabularySearchResult =
                v3api.searchVocabularies(sessionToken, vocabularySearchCriteria, vocabularyFetchOptions);

        assertEquals(vocabularySearchResult.getTotalCount(), 1);
        assertEquals(vocabularySearchResult.getObjects().get(0).getDescription(), "Test vocabulary");

        final List<VocabularyTerm> vocabularyTerms = vocabularySearchResult.getObjects().get(0).getTerms();
        assertEquals(vocabularyTerms.stream().map(VocabularyTerm::getCode).collect(Collectors.toSet()),
                Set.of("TEST_TERM_A", "TEST_TERM_B", "TEST_TERM_C"));
        final List<String> descriptions = vocabularyTerms.stream().map(VocabularyTerm::getLabel).collect(Collectors.toList());
        assertTrue(descriptions.containsAll(Arrays.asList(null, null, "Test term C")));
    }

    @Test(expectedExceptions = UserFailureException.class, expectedExceptionsMessageRegExp = ".*FAIL_IF_EXISTS.*")
    public void testImportOptionsFailIfExists() throws Exception
    {
        final String[] sessionWorkspaceFiles = uploadToAsSessionWorkspace(sessionToken, "existing_vocabulary.xlsx");

        final ImportData importData = new ImportData(ImportFormat.EXCEL, sessionWorkspaceFiles);
        final ImportOptions importOptions = new ImportOptions(ImportMode.FAIL_IF_EXISTS);

        v3api.executeImport(sessionToken, importData, importOptions);
    }

    @Test
    public void testWithValidationScript() throws Exception
    {
        final String name = "valid.py";
        final String source = "print 'Test validation script'";
        final String[] sessionWorkspaceFiles = uploadToAsSessionWorkspace(sessionToken, "validation_script.xls", "scripts/" + name);
        final ImportData importData = new ImportData(ImportFormat.EXCEL, sessionWorkspaceFiles[0]);
        final ImportOptions importOptions = new ImportOptions(ImportMode.UPDATE_IF_EXISTS);

        v3api.executeImport(sessionToken, importData, importOptions);

        final SampleTypeSearchCriteria sampleTypeSearchCriteria = new SampleTypeSearchCriteria();
        sampleTypeSearchCriteria.withCode().thatEquals("ANTIBODY");

        final SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
        sampleTypeFetchOptions.withValidationPlugin().withScript();

        final SearchResult<SampleType> sampleTypeSearchResult =
                v3api.searchSampleTypes(sessionToken, sampleTypeSearchCriteria, sampleTypeFetchOptions);

        assertEquals(sampleTypeSearchResult.getTotalCount(), 1);

        final SampleType sampleType = sampleTypeSearchResult.getObjects().get(0);
        final Plugin validationPlugin = sampleType.getValidationPlugin();
        final String validationPluginBareName = name.substring(0, name.lastIndexOf("."));

        assertEquals(validationPlugin.getName(), validationPluginBareName);
        assertEquals(validationPlugin.getScript(), source);
    }

    @Test
    public void testWithDynamicScript() throws Exception
    {
        final String name = "dynamic.py";
        final String source = "1+1";
        final String[] sessionWorkspaceFiles = uploadToAsSessionWorkspace(sessionToken, "dynamic_script.xls", "scripts/" + name);
        final ImportData importData = new ImportData(ImportFormat.EXCEL, sessionWorkspaceFiles[0]);
        final ImportOptions importOptions = new ImportOptions(ImportMode.UPDATE_IF_EXISTS);

        v3api.executeImport(sessionToken, importData, importOptions);

        final SampleTypeSearchCriteria sampleTypeSearchCriteria = new SampleTypeSearchCriteria();
        sampleTypeSearchCriteria.withCode().thatEquals("ANTIBODY");

        final SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
        final PropertyAssignmentFetchOptions propertyAssignmentFetchOptions = sampleTypeFetchOptions.withPropertyAssignments();
        propertyAssignmentFetchOptions.withPlugin().withScript();
        propertyAssignmentFetchOptions.withPropertyType();

        final SearchResult<SampleType> sampleTypeSearchResult =
                v3api.searchSampleTypes(sessionToken, sampleTypeSearchCriteria, sampleTypeFetchOptions);
        assertEquals(sampleTypeSearchResult.getTotalCount(), 1);

        final SampleType sampleType = sampleTypeSearchResult.getObjects().get(0);
        assertEquals(sampleType.getPropertyAssignments().size(), 1);

        final PropertyAssignment propertyAssignment = sampleType.getPropertyAssignments().get(0);
        final Plugin plugin = propertyAssignment.getPlugin();

        final String pluginBareName = name.substring(0, name.lastIndexOf("."));

        assertEquals(plugin.getName(), pluginBareName);
        assertEquals(plugin.getScript(), source);
    }

    @Test
    public void testInternalTypes_failForNormalUser() throws Exception
    {
        final String[] sessionWorkspaceFiles = uploadToAsSessionWorkspace(sessionToken, "import_internal_type.xlsx");

        final ImportData importData = new ImportData(ImportFormat.EXCEL, sessionWorkspaceFiles);
        final ImportOptions importOptions = new ImportOptions(ImportMode.UPDATE_IF_EXISTS);

        assertUserFailureException(new IDelegatedAction()
                                   {
                                       @Override
                                       public void execute()
                                       {
                                           v3api.executeImport(sessionToken, importData, importOptions);
                                       }
                                   },
                "Non-system user can not create new internal entity types!");
    }

    @Test
    public void testInternalTypes() throws Exception
    {
        final String systemSessionToken = v3api.loginAsSystem();

        final String[] sessionWorkspaceFiles = uploadToAsSessionWorkspace(systemSessionToken, "import_internal_type.xlsx");

        final ImportData importData = new ImportData(ImportFormat.EXCEL, sessionWorkspaceFiles);
        final ImportOptions importOptions = new ImportOptions(ImportMode.UPDATE_IF_EXISTS);

        v3api.executeImport(systemSessionToken, importData, importOptions);

        final DataSetTypeSearchCriteria dataSetTypeSearchCriteria = new DataSetTypeSearchCriteria();
        dataSetTypeSearchCriteria.withCode().thatEquals("INTERNAL_DATASET_TYPE");

        final DataSetTypeFetchOptions dataSetTypeFetchOptions = new DataSetTypeFetchOptions();

        SearchResult<DataSetType> dataSetTypeSearchResult =
                v3api.searchDataSetTypes(sessionToken, dataSetTypeSearchCriteria, dataSetTypeFetchOptions);

        assertEquals(dataSetTypeSearchResult.getTotalCount(), 1);
        assertEquals(dataSetTypeSearchResult.getObjects().get(0).getDescription(), "Internal Dataset Type");

        final SampleTypeSearchCriteria sampleTypeSearchCriteria = new SampleTypeSearchCriteria();
        sampleTypeSearchCriteria.withPermId().thatEquals("INTERNAL_SAMPLE_TYPE");

        final SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
        sampleTypeFetchOptions.withPropertyAssignments().withPropertyType();

        SearchResult<SampleType> sampleTypeSearchResult =
                v3api.searchSampleTypes(sessionToken, sampleTypeSearchCriteria, sampleTypeFetchOptions);

        assertEquals(sampleTypeSearchResult.getTotalCount(), 1);
        assertEquals(sampleTypeSearchResult.getObjects().get(0).getDescription(), "Internal Sample Type");

        SampleType sampleType = sampleTypeSearchResult.getObjects().get(0);
        List<PropertyAssignment> sampleTypePropertyAssignments = sampleType.getPropertyAssignments();
        Optional<PropertyAssignment> sampleTypeAssignmentOptional = sampleTypePropertyAssignments.stream().filter(x -> "FOR_WHAT_INTERNAL".equals(x.getPropertyType().getCode())).findFirst();
        assertTrue(sampleTypeAssignmentOptional.isPresent());
        PropertyAssignment sampleTypeAssignment = sampleTypeAssignmentOptional.get();
        assertFalse(sampleTypeAssignment.isManagedInternally());
        assertTrue(sampleTypeAssignment.getPropertyType().isManagedInternally());
        assertEquals(sampleTypeAssignment.getPatternType(), "PATTERN");
        assertEquals(sampleTypeAssignment.getPattern(), ".*");


        final ExperimentTypeSearchCriteria experimentTypeSearchCriteria = new ExperimentTypeSearchCriteria();
        experimentTypeSearchCriteria.withCode().thatEquals("INTERNAL_EXPERIMENT_TYPE");

        final ExperimentTypeFetchOptions experimentTypeFetchOptions = new ExperimentTypeFetchOptions();
        experimentTypeFetchOptions.withPropertyAssignments().withPropertyType();

        SearchResult<ExperimentType> experimentTypeSearchResult =
                v3api.searchExperimentTypes(sessionToken, experimentTypeSearchCriteria, experimentTypeFetchOptions);

        assertEquals(experimentTypeSearchResult.getTotalCount(), 1);
        ExperimentType experimentType = experimentTypeSearchResult.getObjects().get(0);
        assertEquals(experimentType.getDescription(), "Internal Experiment Type");

        List<PropertyAssignment> propertyAssignments = experimentType.getPropertyAssignments();
        assertEquals(propertyAssignments.size(), 2);
        Optional<PropertyAssignment> assignmentOptional = propertyAssignments.stream().filter(x -> "FOR_WHAT_INTERNAL".equals(x.getPropertyType().getCode())).findFirst();
        assertTrue(assignmentOptional.isPresent());
        assertTrue(assignmentOptional.get().isManagedInternally());
        assertTrue(assignmentOptional.get().getPropertyType().isManagedInternally());

        final VocabularySearchCriteria vocabularySearchCriteria = new VocabularySearchCriteria();
        vocabularySearchCriteria.withCode().thatEquals("INTERNAL_VOCABULARY_TYPE");

        final VocabularyFetchOptions vocabularyFetchOptions = new VocabularyFetchOptions();
        vocabularyFetchOptions.withTerms();

        SearchResult<Vocabulary> vocabularySearchResult = v3api.searchVocabularies(sessionToken, vocabularySearchCriteria, vocabularyFetchOptions);
        assertEquals(vocabularySearchResult.getTotalCount(), 1);
        Vocabulary vocabulary = vocabularySearchResult.getObjects().get(0);
        assertEquals(vocabulary.getDescription(), "Internal Vocabulary Type");
        assertEquals(vocabulary.getTerms().size(), 2);

        for(VocabularyTerm term : vocabulary.getTerms())
        {
            if(term.getCode().equals("INTERNAL_TERM"))
            {
                assertTrue(term.isManagedInternally());
            } else if (term.getCode().equals("REGULAR_TERM")) {
                assertFalse(term.isManagedInternally());
            } else {
                fail("UNKNOWN TERM!");
            }
        }
    }

}
