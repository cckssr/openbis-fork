package ch.ethz.sis.openbis.generic.asapi.v3.exporter;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class ExportEntityTypeCollectorTest
{

    private static final String SESSION_TOKEN = "test-token";

    private Mockery mockery;

    private IApplicationServerApi api;

    @BeforeMethod
    public void beforeMethod()
    {
        mockery = new Mockery();
        api = mockery.mock(IApplicationServerApi.class);
    }

    @AfterMethod
    public void afterMethod()
    {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testExpandReferenceWithSampleTypeWithoutReferences()
    {
        final SampleType sampleType = sampleType("ST_NO_REFS", List.of(), List.of());
        mockery.checking(new Expectations()
        {
            {
                allowing(api).getSampleTypes(with(SESSION_TOKEN),
                        with(Collections.singletonList(new EntityTypePermId("ST_NO_REFS", EntityKind.SAMPLE))),
                        with(fullSampleTypeFetchOptionsMatcher()));
                will(returnValue(Collections.singletonMap(sampleType.getPermId(), sampleType)));
            }
        });

        final ExportablePermId input = new ExportablePermId(ExportableKind.SAMPLE_TYPE, "ST_NO_REFS");
        final List<ExportablePermId> result = ExportEntityTypeCollector.expandReference(api, SESSION_TOKEN, List.of(input));

        assertEquals(new HashSet<>(result), Set.of(input));
    }

    @Test
    public void testExpandReferenceWithVocabularyProperty()
    {
        final SampleTypeFetchOptions fetchOptions = fullSampleTypeFetchOptions();
        final PropertyAssignment vocabularyProperty = vocabularyProperty(fetchOptions, "ANTIBODY.HOST", "ANTIBODY.HOST");
        final SampleType sampleType = sampleType("ANTIBODY", List.of(vocabularyProperty), List.of(), fetchOptions);
        mockery.checking(new Expectations()
        {
            {
                allowing(api).getSampleTypes(with(SESSION_TOKEN),
                        with(Collections.singletonList(new EntityTypePermId("ANTIBODY", EntityKind.SAMPLE))),
                        with(fullSampleTypeFetchOptionsMatcher()));
                will(returnValue(Collections.singletonMap(sampleType.getPermId(), sampleType)));
            }
        });

        final ExportablePermId input = new ExportablePermId(ExportableKind.SAMPLE_TYPE, "ANTIBODY");
        final List<ExportablePermId> result = ExportEntityTypeCollector.expandReference(api, SESSION_TOKEN, List.of(input));

        assertEquals(new HashSet<>(result), Set.of(input,
                new ExportablePermId(ExportableKind.VOCABULARY_TYPE, "ANTIBODY.HOST")));
    }

    @Test
    public void testExpandReferenceWithSampleProperty()
    {
        final SampleType childSampleType = sampleType("ST_CHILD", List.of(), List.of());

        final SampleTypeFetchOptions parentFetchOptions = fullSampleTypeFetchOptions();
        final PropertyAssignment sampleProperty = sampleProperty(parentFetchOptions, "TEACHER", "ST_CHILD");
        final SampleType parentSampleType = sampleType("ST_PARENT", List.of(sampleProperty), List.of(), parentFetchOptions);

        mockery.checking(new Expectations()
        {
            {
                allowing(api).getSampleTypes(with(SESSION_TOKEN),
                        with(Collections.singletonList(new EntityTypePermId("ST_PARENT", EntityKind.SAMPLE))),
                        with(fullSampleTypeFetchOptionsMatcher()));
                will(returnValue(Collections.singletonMap(parentSampleType.getPermId(), parentSampleType)));

                allowing(api).getSampleTypes(with(SESSION_TOKEN),
                        with(Collections.singletonList(new EntityTypePermId("ST_CHILD", EntityKind.SAMPLE))),
                        with(fullSampleTypeFetchOptionsMatcher()));
                will(returnValue(Collections.singletonMap(childSampleType.getPermId(), childSampleType)));
            }
        });

        final ExportablePermId input = new ExportablePermId(ExportableKind.SAMPLE_TYPE, "ST_PARENT");
        final List<ExportablePermId> result = ExportEntityTypeCollector.expandReference(api, SESSION_TOKEN, List.of(input));

        assertEquals(new HashSet<>(result), Set.of(input,
                new ExportablePermId(ExportableKind.SAMPLE_TYPE, "ST_CHILD")));
    }


    @Test
    public void testExpandReferenceWithCyclicSampleProperties()
    {
        final SampleTypeFetchOptions fetchOptionsA = fullSampleTypeFetchOptions();
        final PropertyAssignment referencesB = sampleProperty(fetchOptionsA, "B_REF", "ST_B");
        final SampleType sampleTypeA = sampleType("ST_A", List.of(referencesB), List.of(), fetchOptionsA);

        final SampleTypeFetchOptions fetchOptionsB = fullSampleTypeFetchOptions();
        final PropertyAssignment referencesA = sampleProperty(fetchOptionsB, "A_REF", "ST_A");
        final SampleType sampleTypeB = sampleType("ST_B", List.of(referencesA), List.of(), fetchOptionsB);

        mockery.checking(new Expectations()
        {
            {
                allowing(api).getSampleTypes(with(SESSION_TOKEN),
                        with(Collections.singletonList(new EntityTypePermId("ST_A", EntityKind.SAMPLE))),
                        with(fullSampleTypeFetchOptionsMatcher()));
                will(returnValue(Collections.singletonMap(sampleTypeA.getPermId(), sampleTypeA)));

                allowing(api).getSampleTypes(with(SESSION_TOKEN),
                        with(Collections.singletonList(new EntityTypePermId("ST_B", EntityKind.SAMPLE))),
                        with(fullSampleTypeFetchOptionsMatcher()));
                will(returnValue(Collections.singletonMap(sampleTypeB.getPermId(), sampleTypeB)));
            }
        });

        final ExportablePermId input = new ExportablePermId(ExportableKind.SAMPLE_TYPE, "ST_A");
        final List<ExportablePermId> result = ExportEntityTypeCollector.expandReference(api, SESSION_TOKEN, List.of(input));

        assertEquals(new HashSet<>(result), Set.of(input,
                new ExportablePermId(ExportableKind.SAMPLE_TYPE, "ST_B")));
    }

    @Test
    public void testExpandReferenceWithTypeGroup()
    {
        final TypeGroup typeGroup = new TypeGroup();
        typeGroup.setCode("TG1");

        final TypeGroupAssignment typeGroupAssignment = new TypeGroupAssignment();
        typeGroupAssignment.setTypeGroup(typeGroup);

        final SampleType sampleType = sampleType("ST_GROUP", List.of(), List.of(typeGroupAssignment));
        mockery.checking(new Expectations()
        {
            {
                allowing(api).getSampleTypes(with(SESSION_TOKEN),
                        with(Collections.singletonList(new EntityTypePermId("ST_GROUP", EntityKind.SAMPLE))),
                        with(fullSampleTypeFetchOptionsMatcher()));
                will(returnValue(Collections.singletonMap(sampleType.getPermId(), sampleType)));
            }
        });

        final ExportablePermId input = new ExportablePermId(ExportableKind.SAMPLE_TYPE, "ST_GROUP");
        final List<ExportablePermId> result = ExportEntityTypeCollector.expandReference(api, SESSION_TOKEN, List.of(input));

        assertEquals(new HashSet<>(result), Set.of(input,
                new ExportablePermId(ExportableKind.TYPE_GROUP, "TG1")));
    }

    @Test
    public void testExpandReferenceWithExperimentType()
    {
        final ExperimentTypeFetchOptions fetchOptions = new ExperimentTypeFetchOptions();
        fetchOptions.withPropertyAssignments();

        final ExperimentType experimentType = new ExperimentType();
        experimentType.setFetchOptions(fetchOptions);
        experimentType.setPermId(new EntityTypePermId("ET1", EntityKind.EXPERIMENT));
        experimentType.setCode("ET1");
        experimentType.setPropertyAssignments(List.of());

        mockery.checking(new Expectations()
        {
            {
                allowing(api).getExperimentTypes(with(SESSION_TOKEN),
                        with(Collections.singletonList(new EntityTypePermId("ET1", EntityKind.EXPERIMENT))),
                        with(fullExperimentTypeFetchOptionsMatcher()));
                will(returnValue(Collections.singletonMap(experimentType.getPermId(), experimentType)));
            }
        });

        final ExportablePermId input = new ExportablePermId(ExportableKind.EXPERIMENT_TYPE, "ET1");
        final List<ExportablePermId> result = ExportEntityTypeCollector.expandReference(api, SESSION_TOKEN, List.of(input));

        assertEquals(new HashSet<>(result), Set.of(input));
    }

    @Test
    public void testExpandReferenceWithDataSetType()
    {
        final DataSetTypeFetchOptions fetchOptions = new DataSetTypeFetchOptions();
        fetchOptions.withPropertyAssignments();

        final DataSetType dataSetType = new DataSetType();
        dataSetType.setFetchOptions(fetchOptions);
        dataSetType.setPermId(new EntityTypePermId("DT1", EntityKind.DATA_SET));
        dataSetType.setCode("DT1");
        dataSetType.setPropertyAssignments(List.of());

        mockery.checking(new Expectations()
        {
            {
                allowing(api).getDataSetTypes(with(SESSION_TOKEN),
                        with(Collections.singletonList(new EntityTypePermId("DT1", EntityKind.DATA_SET))),
                        with(fullDataSetTypeFetchOptionsMatcher()));
                will(returnValue(Collections.singletonMap(dataSetType.getPermId(), dataSetType)));
            }
        });

        final ExportablePermId input = new ExportablePermId(ExportableKind.DATASET_TYPE, "DT1");
        final List<ExportablePermId> result = ExportEntityTypeCollector.expandReference(api, SESSION_TOKEN, List.of(input));

        assertEquals(new HashSet<>(result), Set.of(input));
    }


    @Test
    public void testExpandReferenceWithUnsupportedKindIsPassthrough()
    {
        mockery.checking(new Expectations()
        {
            {
                // no calls expected on api
            }
        });

        final ExportablePermId input = new ExportablePermId(ExportableKind.SPACE, "SP1");
        final List<ExportablePermId> result = ExportEntityTypeCollector.expandReference(api, SESSION_TOKEN, List.of(input));

        assertEquals(new HashSet<>(result), Set.of(input));
    }

    private static SampleTypeFetchOptions fullSampleTypeFetchOptions()
    {
        final SampleTypeFetchOptions fetchOptions = new SampleTypeFetchOptions();
        final PropertyAssignmentFetchOptions propertyAssignmentFetchOptions = fetchOptions.withPropertyAssignments();
        final PropertyTypeFetchOptions propertyTypeFetchOptions = propertyAssignmentFetchOptions.withPropertyType();
        propertyTypeFetchOptions.withVocabulary();
        propertyTypeFetchOptions.withSampleType();
        fetchOptions.withTypeGroupAssignments().withTypeGroup();
        return fetchOptions;
    }

    private static SampleType sampleType(final String code, final List<PropertyAssignment> propertyAssignments,
            final List<TypeGroupAssignment> typeGroupAssignments)
    {
        return sampleType(code, propertyAssignments, typeGroupAssignments, fullSampleTypeFetchOptions());
    }

    private static SampleType sampleType(final String code, final List<PropertyAssignment> propertyAssignments,
            final List<TypeGroupAssignment> typeGroupAssignments, final SampleTypeFetchOptions fetchOptions)
    {
        final SampleType sampleType = new SampleType();
        sampleType.setFetchOptions(fetchOptions);
        sampleType.setPermId(new EntityTypePermId(code, EntityKind.SAMPLE));
        sampleType.setCode(code);
        sampleType.setPropertyAssignments(propertyAssignments);
        sampleType.setTypeGroupAssignments(typeGroupAssignments);
        return sampleType;
    }

    private static PropertyAssignment vocabularyProperty(final SampleTypeFetchOptions ownerFetchOptions,
            final String propertyTypeCode, final String vocabularyCode)
    {
        final PropertyAssignmentFetchOptions propertyAssignmentFetchOptions = ownerFetchOptions.withPropertyAssignments();
        final PropertyTypeFetchOptions propertyTypeFetchOptions = propertyAssignmentFetchOptions.withPropertyType();
        propertyTypeFetchOptions.withVocabulary();

        final Vocabulary vocabulary = new Vocabulary();
        vocabulary.setCode(vocabularyCode);

        final PropertyType propertyType = new PropertyType();
        propertyType.setFetchOptions(propertyTypeFetchOptions);
        propertyType.setCode(propertyTypeCode);
        propertyType.setDataType(DataType.CONTROLLEDVOCABULARY);
        propertyType.setVocabulary(vocabulary);

        final PropertyAssignment propertyAssignment = new PropertyAssignment();
        propertyAssignment.setFetchOptions(propertyAssignmentFetchOptions);
        propertyAssignment.setPropertyType(propertyType);
        return propertyAssignment;
    }

    private static PropertyAssignment sampleProperty(final SampleTypeFetchOptions ownerFetchOptions,
            final String propertyTypeCode, final String referencedSampleTypeCode)
    {
        final PropertyAssignmentFetchOptions propertyAssignmentFetchOptions = ownerFetchOptions.withPropertyAssignments();
        final PropertyTypeFetchOptions propertyTypeFetchOptions = propertyAssignmentFetchOptions.withPropertyType();
        propertyTypeFetchOptions.withSampleType();

        final SampleType referencedSampleType = new SampleType();
        referencedSampleType.setCode(referencedSampleTypeCode);

        final PropertyType propertyType = new PropertyType();
        propertyType.setFetchOptions(propertyTypeFetchOptions);
        propertyType.setCode(propertyTypeCode);
        propertyType.setDataType(DataType.SAMPLE);
        propertyType.setSampleType(referencedSampleType);

        final PropertyAssignment propertyAssignment = new PropertyAssignment();
        propertyAssignment.setFetchOptions(propertyAssignmentFetchOptions);
        propertyAssignment.setPropertyType(propertyType);
        return propertyAssignment;
    }

    private static boolean hasFullPropertyAssignmentFetchOptions(final PropertyAssignmentFetchOptions propertyAssignmentFetchOptions)
    {
        if (!propertyAssignmentFetchOptions.hasPropertyType())
        {
            return false;
        }
        final PropertyTypeFetchOptions propertyTypeFetchOptions = propertyAssignmentFetchOptions.withPropertyType();
        if (!propertyTypeFetchOptions.hasVocabulary() || !propertyTypeFetchOptions.hasSampleType())
        {
            return false;
        }
        return propertyAssignmentFetchOptions.hasPlugin() && propertyAssignmentFetchOptions.withPlugin().hasScript();
    }

    private static Matcher<SampleTypeFetchOptions> fullSampleTypeFetchOptionsMatcher()
    {
        return new TypeSafeMatcher<SampleTypeFetchOptions>()
        {
            @Override
            protected boolean matchesSafely(final SampleTypeFetchOptions item)
            {
                if (!item.hasValidationPlugin() || !item.withValidationPlugin().hasScript())
                {
                    return false;
                }
                if (!item.hasTypeGroupAssignments() || !item.withTypeGroupAssignments().hasTypeGroup())
                {
                    return false;
                }
                return item.hasPropertyAssignments() && hasFullPropertyAssignmentFetchOptions(item.withPropertyAssignments());
            }

            @Override
            public void describeTo(final Description description)
            {
                description.appendText("a SampleTypeFetchOptions requesting validation plugin script, "
                        + "type group assignments, and full property assignment (plugin script, vocabulary, sample type) fetch options");
            }
        };
    }

    private static Matcher<ExperimentTypeFetchOptions> fullExperimentTypeFetchOptionsMatcher()
    {
        return new TypeSafeMatcher<ExperimentTypeFetchOptions>()
        {
            @Override
            protected boolean matchesSafely(final ExperimentTypeFetchOptions item)
            {
                if (!item.hasValidationPlugin() || !item.withValidationPlugin().hasScript())
                {
                    return false;
                }
                return item.hasPropertyAssignments() && hasFullPropertyAssignmentFetchOptions(item.withPropertyAssignments());
            }

            @Override
            public void describeTo(final Description description)
            {
                description.appendText("an ExperimentTypeFetchOptions requesting validation plugin script "
                        + "and full property assignment (plugin script, vocabulary, sample type) fetch options");
            }
        };
    }

    private static Matcher<DataSetTypeFetchOptions> fullDataSetTypeFetchOptionsMatcher()
    {
        return new TypeSafeMatcher<DataSetTypeFetchOptions>()
        {
            @Override
            protected boolean matchesSafely(final DataSetTypeFetchOptions item)
            {
                if (!item.hasValidationPlugin() || !item.withValidationPlugin().hasScript())
                {
                    return false;
                }
                return item.hasPropertyAssignments() && hasFullPropertyAssignmentFetchOptions(item.withPropertyAssignments());
            }

            @Override
            public void describeTo(final Description description)
            {
                description.appendText("a DataSetTypeFetchOptions requesting validation plugin script "
                        + "and full property assignment (plugin script, vocabulary, sample type) fetch options");
            }
        };
    }

}
