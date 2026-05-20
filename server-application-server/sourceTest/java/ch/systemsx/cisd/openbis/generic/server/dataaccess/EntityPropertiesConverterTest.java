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
package ch.systemsx.cisd.openbis.generic.server.dataaccess;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jmock.Expectations;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ethz.sis.openbis.generic.server.sharedapi.v3.json.ObjectMapperResource;
import ch.rinn.restrictions.Friend;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.test.RecordingMatcher;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.systemsx.cisd.openbis.generic.server.business.ManagerTestTool;
import ch.systemsx.cisd.openbis.generic.server.business.bo.AbstractBOTest;
import ch.systemsx.cisd.openbis.generic.server.business.bo.CollectionMatcher;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.validators.PropertyValidator;
import ch.systemsx.cisd.openbis.generic.shared.basic.BasicConstant;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataTypeCode;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.EntityProperty;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IEntityProperty;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.PluginType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.PropertyType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ScriptType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.builders.PropertyBuilder;
import ch.systemsx.cisd.openbis.generic.shared.dto.DataTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityPropertyPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityPropertyWithSampleDataTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePropertyTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.PersonPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.PropertyTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SamplePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SamplePropertyPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SampleTypePropertyTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.ScriptPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.VocabularyPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.VocabularyTermPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.builders.SamplePEBuilder;
import ch.systemsx.cisd.openbis.generic.shared.dto.properties.EntityKind;

/**
 * Test cases for corresponding {@link EntityPropertiesConverter} class.
 * 
 * @author Christian Ribeaud
 */
// TODO 2011-01-11, Piotr Buczek: test dynamic and managed properties handling
@Friend(toClasses = EntityPropertiesConverter.class)
public final class EntityPropertiesConverterTest extends AbstractBOTest
{
    private static final String VARCHAR_PROPERTY_TYPE_CODE = "color";

    private static final String SAMPLE_TYPE_CODE = "MASTER_PLATE";

    private static final String SAMPLE_TYPE2_CODE = "SAMPLE_TYPE2";

    private IPropertyValueValidator propertyValueValidator;

    private IPropertyPlaceholderCreator placeholderCreator;

    @Override
    @BeforeMethod
    public final void beforeMethod()
    {
        super.beforeMethod();
        propertyValueValidator = context.mock(IPropertyValueValidator.class);
        placeholderCreator = context.mock(IPropertyPlaceholderCreator.class);
    }

    private final IEntityPropertiesConverter createEntityPropertiesConverter(
            final EntityKind entityKind)
    {
        return new EntityPropertiesConverter(entityKind, daoFactory, propertyValueValidator,
                placeholderCreator, null, managedPropertyEvaluatorFactory);
    }

    private void prepareForConvertion(final Expectations exp)
    {
        prepareForConvertion(exp, false);
    }

    private void prepareForConvertion(final Expectations exp, boolean mandatory)
    {
        final SampleTypePE sampleType = createSampleType(SAMPLE_TYPE_CODE);
        final SampleTypePropertyTypePE sampleTypePropertyTypePE = new SampleTypePropertyTypePE();
        sampleTypePropertyTypePE.setEntityType(sampleType);
        final PropertyTypePE propertyType = new PropertyTypePE();
        propertyType.setCode(VARCHAR_PROPERTY_TYPE_CODE);
        sampleTypePropertyTypePE.setPropertyType(propertyType);
        sampleTypePropertyTypePE.setMandatory(mandatory);

        exp.allowing(daoFactory).getEntityPropertyTypeDAO(EntityKind.SAMPLE);
        exp.will(Expectations.returnValue(entityPropertyTypeDAO));

        exp.allowing(daoFactory).getEntityTypeDAO(EntityKind.SAMPLE);
        exp.will(Expectations.returnValue(entityTypeDAO));

        exp.allowing(daoFactory).getPropertyTypeDAO();
        exp.will(Expectations.returnValue(propertyTypeDAO));

        exp.atLeast(1).of(entityTypeDAO).listEntityTypes();
        exp.will(Expectations.returnValue(Collections.singletonList(sampleType)));

        exp.allowing(entityPropertyTypeDAO).listEntityPropertyTypes(sampleType);
        exp.will(Expectations.returnValue(Collections.singletonList(sampleTypePropertyTypePE)));
    }

    private final static SampleTypePE createSampleType(final String sampleTypeCode)
    {
        final SampleTypePE sampleType = new SampleTypePE();
        sampleType.setCode(sampleTypeCode);
        return sampleType;
    }

    private final static IEntityProperty createVarcharSampleProperty(final boolean lowerCase,
            String code)
    {
        final IEntityProperty sampleProperty = new EntityProperty();
        sampleProperty.setValue("blue");
        final PropertyType propertyType = new PropertyType();
        String newCode = code;
        if (lowerCase)
        {
            newCode = newCode.toLowerCase();
        }
        propertyType.setLabel(newCode);
        propertyType.setCode(newCode);
        final DataType dataType = new DataType();
        dataType.setCode(DataTypeCode.VARCHAR);
        propertyType.setDataType(dataType);
        sampleProperty.setPropertyType(propertyType);
        return sampleProperty;
    }

    private final IEntityProperty[] createSampleProperties(final boolean lowerCase)
    {
        return new IEntityProperty[]
        { createVarcharSampleProperty(lowerCase, VARCHAR_PROPERTY_TYPE_CODE) };
    }

    @Test
    public final void testConvertPropertiesFailed()
    {
        final IEntityPropertiesConverter entityPropertiesConverter =
                createEntityPropertiesConverter(EntityKind.SAMPLE);
        boolean fail = true;
        try
        {
            entityPropertiesConverter.convertProperties(null, null, null);
        } catch (final AssertionError e)
        {
            fail = false;
        }
        assertFalse(fail);
        context.assertIsSatisfied();
    }

    @Test
    public final void testConvertPropertiesWithEmptyProperties()
    {
        final IEntityPropertiesConverter entityPropertiesConverter =
                createEntityPropertiesConverter(EntityKind.SAMPLE);

        final RecordingMatcher<Set<IEntityProperty>> definedPropertiesMatcher =
                RecordingMatcher.create();
        context.checking(new Expectations()
            {
                {
                    prepareForConvertion(this);
                    CollectionMatcher<Set<String>> dynamicPropertiesMatcher =
                            new CollectionMatcher<Set<String>>(new HashSet<String>(
                                    new ArrayList<String>()));
                    one(placeholderCreator).addDynamicPropertiesPlaceholders(
                            with(definedPropertiesMatcher), with(dynamicPropertiesMatcher));
                    one(placeholderCreator).addManagedPropertiesPlaceholders(
                            with(definedPropertiesMatcher), with(dynamicPropertiesMatcher));
                }
            });
        final List<EntityPropertyPE> properties =
                entityPropertiesConverter.convertProperties(IEntityProperty.EMPTY_ARRAY,
                        SAMPLE_TYPE_CODE, ManagerTestTool.EXAMPLE_PERSON);
        assertEquals(0, properties.size());
        context.assertIsSatisfied();
    }

    @Test
    public final void testConvertProperties()
    {
        final IEntityPropertiesConverter entityPropertiesConverter =
                createEntityPropertiesConverter(EntityKind.SAMPLE);
        final PropertyTypePE propertyTypePE = createPropertyType();
        final IEntityProperty[] properties = createSampleProperties(false);
        final String value = BasicConstant.MANAGED_PROPERTY_JSON_PREFIX + "[{\"A\":\"alpha\"}]";
        properties[0].setValue(value);

        final RecordingMatcher<Set<IEntityProperty>> definedPropertiesMatcher =
                RecordingMatcher.create();
        context.checking(new Expectations()
            {
                {

                    final SampleTypePE sampleType = createSampleType(SAMPLE_TYPE_CODE);
                    final SampleTypePropertyTypePE sampleTypePropertyTypePE =
                            createETPT(VARCHAR_PROPERTY_TYPE_CODE, sampleType);
                    sampleTypePropertyTypePE.setMandatory(true);
                    ScriptPE script = new ScriptPE();
                    script.setScriptType(ScriptType.MANAGED_PROPERTY);
                    script.setPluginType(PluginType.JYTHON);
                    script.setScript("def batchColumnNames():\n return ['A']\n"
                            + "def updateFromBatchInput(bindings):\n None\n"
                            + "def updateFromRegistrationForm(bindings):\n"
                            + " property.setValue('Hello ' + bindings.get(0).get('A'))");
                    sampleTypePropertyTypePE.setScript(script);

                    final SampleTypePE sampleType2 = createSampleType(SAMPLE_TYPE2_CODE);
                    final SampleTypePropertyTypePE sampleTypePropertyTypePE2 =
                            createETPT(VARCHAR_PROPERTY_TYPE_CODE, sampleType2);

                    this.allowing(daoFactory).getEntityPropertyTypeDAO(EntityKind.SAMPLE);
                    this.will(Expectations.returnValue(entityPropertyTypeDAO));

                    this.allowing(daoFactory).getEntityTypeDAO(EntityKind.SAMPLE);
                    this.will(Expectations.returnValue(entityTypeDAO));

                    this.atLeast(1).of(entityTypeDAO).listEntityTypes();
                    this.will(Expectations.returnValue(Arrays.asList(sampleType, sampleType2)));

                    this.allowing(entityPropertyTypeDAO).listEntityPropertyTypes(sampleType);
                    this.will(Expectations.returnValue(Arrays.asList(sampleTypePropertyTypePE)));

                    one(placeholderCreator).addManagedPropertiesPlaceholders(
                            new HashSet<IEntityProperty>(Arrays.asList(properties)),
                            new HashSet<String>(Arrays.asList(VARCHAR_PROPERTY_TYPE_CODE
                                    .toUpperCase())));

                    this.allowing(entityPropertyTypeDAO).listEntityPropertyTypes(sampleType2);
                    this.will(Expectations.returnValue(Arrays.asList(sampleTypePropertyTypePE2)));

                    one(propertyTypeDAO).tryFindPropertyTypeByCode(VARCHAR_PROPERTY_TYPE_CODE);
                    will(returnValue(propertyTypePE));

                    atLeast(1).of(propertyValueValidator).validatePropertyValue(propertyTypePE,
                            "Hello alpha");
                    will(returnValue("Hello alpha"));

                    CollectionMatcher<Set<String>> dynamicPropertiesMatcher =
                            new CollectionMatcher<Set<String>>(new HashSet<String>(
                                    new ArrayList<String>()));

                    ArrayList<IEntityProperty> listOfProperties = new ArrayList<IEntityProperty>();
                    for (IEntityProperty p : properties)
                    {
                        listOfProperties.add(p);
                    }
                    exactly(2).of(placeholderCreator).addDynamicPropertiesPlaceholders(
                            with(definedPropertiesMatcher), with(dynamicPropertiesMatcher));
                    one(placeholderCreator).addManagedPropertiesPlaceholders(
                            with(definedPropertiesMatcher), with(dynamicPropertiesMatcher));
                }
            });

        List<EntityPropertyPE> convertedProperties =
                entityPropertiesConverter.convertProperties(properties, SAMPLE_TYPE_CODE,
                        ManagerTestTool.EXAMPLE_PERSON);

        assertEquals(
                "[SamplePropertyPE{entityTypePropertyType="
                        + "SampleTypePropertyTypePE{managedInternally=false,mandatory=true,"
                        + "propertyType=COLOR,entityType=SampleTypePE{code=MASTER_PLATE,description=<null>,"
                        + "listable=<null>,containerHierarchyDepth=<null>,"
                        + "generatedFromHierarchyDepth=<null>},ordinal=<null>,"
                        + "section=<null>,dynamic=false,managed=true,unique=false},value=Hello alpha}]",
                convertedProperties.toString());

        // Check that for sample type SAMPLE_TYPE2_CODE property is not mandatory as for previous
        // sample type. This checks that for same property type but different sample types the
        // right sample-type-property-type has been picked
        properties[0].setValue(null);
        convertedProperties =
                entityPropertiesConverter.convertProperties(properties, SAMPLE_TYPE2_CODE,
                        ManagerTestTool.EXAMPLE_PERSON);

        context.assertIsSatisfied();
    }

    private SampleTypePropertyTypePE createETPT(String code, final SampleTypePE sampleType)
    {
        final SampleTypePropertyTypePE sampleTypePropertyTypePE = new SampleTypePropertyTypePE();
        sampleTypePropertyTypePE.setEntityType(sampleType);

        final PropertyTypePE propertyType = new PropertyTypePE();
        propertyType.setCode(code);
        sampleTypePropertyTypePE.setPropertyType(propertyType);
        sampleTypePropertyTypePE.setMandatory(false);
        return sampleTypePropertyTypePE;
    }

    @Test
    public final void testConvertPropertiesWithLowerCase()
    {
        final IEntityPropertiesConverter entityPropertiesConverter =
                createEntityPropertiesConverter(EntityKind.SAMPLE);
        final PropertyTypePE propertyTypePE = createPropertyType();
        final IEntityProperty[] properties = createSampleProperties(true);

        final RecordingMatcher<Set<IEntityProperty>> definedPropertiesMatcher =
                RecordingMatcher.create();
        context.checking(new Expectations()
            {
                {
                    prepareForConvertion(this);

                    one(propertyTypeDAO).tryFindPropertyTypeByCode(VARCHAR_PROPERTY_TYPE_CODE);
                    will(returnValue(propertyTypePE));

                    one(propertyValueValidator).validatePropertyValue(propertyTypePE, "blue");

                    CollectionMatcher<Set<String>> dynamicPropertiesMatcher =
                            new CollectionMatcher<Set<String>>(new HashSet<String>(
                                    new ArrayList<String>()));

                    ArrayList<IEntityProperty> listOfProperties = new ArrayList<IEntityProperty>();
                    for (IEntityProperty p : properties)
                    {
                        listOfProperties.add(p);
                    }
                    one(placeholderCreator).addDynamicPropertiesPlaceholders(
                            with(definedPropertiesMatcher), with(dynamicPropertiesMatcher));
                    one(placeholderCreator).addManagedPropertiesPlaceholders(
                            with(definedPropertiesMatcher), with(dynamicPropertiesMatcher));
                }
            });
        final List<EntityPropertyPE> convertedProperties =
                entityPropertiesConverter.convertProperties(properties,
                        SAMPLE_TYPE_CODE.toLowerCase(), ManagerTestTool.EXAMPLE_PERSON);
        assertEquals(1, convertedProperties.size());
        context.assertIsSatisfied();
    }

    @Test
    public void testConvertArrayProperties() throws Exception
    {
        final IEntityPropertiesConverter entityPropertiesConverter =
                new EntityPropertiesConverter(EntityKind.SAMPLE, daoFactory,
                        new PropertyValidator(), placeholderCreator, null,
                        managedPropertyEvaluatorFactory);

        final SampleTypePE sampleType = createSampleType(SAMPLE_TYPE_CODE);
        final PropertyTypePE stringArrayPropertyType =
                createPropertyType("STRING_ARRAY", DataTypeCode.ARRAY_STRING);
        final PropertyTypePE integerArrayPropertyType =
                createPropertyType("INTEGER_ARRAY", DataTypeCode.ARRAY_INTEGER);
        final PropertyTypePE realArrayPropertyType =
                createPropertyType("REAL_ARRAY", DataTypeCode.ARRAY_REAL);
        final PropertyTypePE timestampArrayPropertyType =
                createPropertyType("TIMESTAMP_ARRAY", DataTypeCode.ARRAY_TIMESTAMP);

        final IEntityProperty[] properties = new IEntityProperty[]
        {
                createSampleProperty("STRING_ARRAY", DataTypeCode.ARRAY_STRING,
                        "[\"alpha\", \"beta\"]"),
                createSampleProperty("INTEGER_ARRAY", DataTypeCode.ARRAY_INTEGER,
                        "[\"1\", \"2\", \"3\"]"),
                createSampleProperty("REAL_ARRAY", DataTypeCode.ARRAY_REAL,
                        "[\"1.5\", \"2.5\"]"),
                createSampleProperty("TIMESTAMP_ARRAY", DataTypeCode.ARRAY_TIMESTAMP,
                        "[\"2026-01-30 10:18:30 +0400\", \"2026-01-31 11:19:31 +0400\"]")
        };

        final RecordingMatcher<Set<IEntityProperty>> definedPropertiesMatcher =
                RecordingMatcher.create();
        context.checking(new Expectations()
            {
                {
                    allowing(daoFactory).getEntityPropertyTypeDAO(EntityKind.SAMPLE);
                    will(returnValue(entityPropertyTypeDAO));

                    allowing(daoFactory).getEntityTypeDAO(EntityKind.SAMPLE);
                    will(returnValue(entityTypeDAO));

                    allowing(daoFactory).getPropertyTypeDAO();
                    will(returnValue(propertyTypeDAO));

                    atLeast(1).of(entityTypeDAO).listEntityTypes();
                    will(returnValue(Collections.singletonList(sampleType)));

                    allowing(entityPropertyTypeDAO).listEntityPropertyTypes(sampleType);
                    will(returnValue(Arrays.asList(
                            createETPT(stringArrayPropertyType, sampleType),
                            createETPT(integerArrayPropertyType, sampleType),
                            createETPT(realArrayPropertyType, sampleType),
                            createETPT(timestampArrayPropertyType, sampleType))));

                    one(propertyTypeDAO).tryFindPropertyTypeByCode("STRING_ARRAY");
                    will(returnValue(stringArrayPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("INTEGER_ARRAY");
                    will(returnValue(integerArrayPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("REAL_ARRAY");
                    will(returnValue(realArrayPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("TIMESTAMP_ARRAY");
                    will(returnValue(timestampArrayPropertyType));

                    CollectionMatcher<Set<String>> dynamicPropertiesMatcher =
                            new CollectionMatcher<Set<String>>(new HashSet<String>(
                                    new ArrayList<String>()));
                    one(placeholderCreator).addDynamicPropertiesPlaceholders(
                            with(definedPropertiesMatcher), with(dynamicPropertiesMatcher));
                    one(placeholderCreator).addManagedPropertiesPlaceholders(
                            with(definedPropertiesMatcher), with(dynamicPropertiesMatcher));
                }
            });

        final List<EntityPropertyPE> convertedProperties =
                entityPropertiesConverter.convertProperties(properties, SAMPLE_TYPE_CODE,
                        ManagerTestTool.EXAMPLE_PERSON);

        assertEquals(4, convertedProperties.size());
        assertArrayEquals(convertedProperties.get(0).getStringArrayValue(),
                new String[] { "alpha", "beta" });
        assertArrayEquals(convertedProperties.get(1).getIntegerArrayValue(),
                new Long[] { 1L, 2L, 3L });
        assertArrayEquals(convertedProperties.get(2).getRealArrayValue(),
                new Double[] { 1.5, 2.5 });
        assertArrayEquals(convertedProperties.get(3).getTimestampArrayValue(),
                new Date[]
                {
                        parseTimestamp("2026-01-30 10:18:30 +0400"),
                        parseTimestamp("2026-01-31 11:19:31 +0400")
                });
        context.assertIsSatisfied();
    }

    @Test
    public void testConvertMultiValueProperties() throws Exception
    {
        final IEntityPropertiesConverter entityPropertiesConverter =
                new EntityPropertiesConverter(EntityKind.SAMPLE, daoFactory,
                        new PropertyValidator(), placeholderCreator, null,
                        managedPropertyEvaluatorFactory);

        final SampleTypePE sampleType = createSampleType(SAMPLE_TYPE_CODE);
        final PropertyTypePE varcharPropertyType =
                createPropertyType("MV_VARCHAR", DataTypeCode.VARCHAR, true);
        final PropertyTypePE multilinePropertyType =
                createPropertyType("MV_MULTILINE", DataTypeCode.MULTILINE_VARCHAR, true);
        final PropertyTypePE integerPropertyType =
                createPropertyType("MV_INTEGER", DataTypeCode.INTEGER, true);
        final PropertyTypePE realPropertyType =
                createPropertyType("MV_REAL", DataTypeCode.REAL, true);
        final PropertyTypePE timestampPropertyType =
                createPropertyType("MV_TIMESTAMP", DataTypeCode.TIMESTAMP, true);
        final PropertyTypePE booleanPropertyType =
                createPropertyType("MV_BOOLEAN", DataTypeCode.BOOLEAN, true);
        final PropertyTypePE hyperlinkPropertyType =
                createPropertyType("MV_HYPERLINK", DataTypeCode.HYPERLINK, true);
        final PropertyTypePE datePropertyType =
                createPropertyType("MV_DATE", DataTypeCode.DATE, true);
        final PropertyTypePE xmlPropertyType =
                createPropertyType("MV_XML", DataTypeCode.XML, true);
        final PropertyTypePE jsonPropertyType =
                createPropertyType("MV_JSON", DataTypeCode.JSON, true);
        final PropertyTypePE vocabularyPropertyType =
                createPropertyType("MV_VOCABULARY", DataTypeCode.CONTROLLEDVOCABULARY, true);
        vocabularyPropertyType.setVocabulary(createVocabulary("TEST_VOCAB", "DOG", "HUMAN"));
        final PropertyTypePE samplePropertyType =
                createPropertyType("MV_SAMPLE", DataTypeCode.SAMPLE, true);

        final SamplePE sample1 = createSample("200811050919915-1");
        final SamplePE sample2 = createSample("200811050919915-2");

        final IEntityProperty[] properties = new IEntityProperty[]
        {
                createSampleProperty("MV_VARCHAR", DataTypeCode.VARCHAR,
                        toJsonArray("alpha", "beta")),
                createSampleProperty("MV_MULTILINE", DataTypeCode.MULTILINE_VARCHAR,
                        toJsonArray("line1\nline2", "line3\nline4")),
                createSampleProperty("MV_INTEGER", DataTypeCode.INTEGER,
                        toJsonArray("001", "2")),
                createSampleProperty("MV_REAL", DataTypeCode.REAL,
                        toJsonArray("1.50", "2.0")),
                createSampleProperty("MV_TIMESTAMP", DataTypeCode.TIMESTAMP,
                        toJsonArray("2026-01-30 10:18:30", "2026-01-31 11:19:31")),
                createSampleProperty("MV_BOOLEAN", DataTypeCode.BOOLEAN,
                        toJsonArray("TRUE", "false")),
                createSampleProperty("MV_HYPERLINK", DataTypeCode.HYPERLINK,
                        toJsonArray("https://openbis.ch", "https://ethz.ch")),
                createSampleProperty("MV_DATE", DataTypeCode.DATE,
                        toJsonArray("2026-01-30", "2026-01-31")),
                createSampleProperty("MV_XML", DataTypeCode.XML,
                        toJsonArray("<value>one</value>", "<value>two</value>")),
                createSampleProperty("MV_JSON", DataTypeCode.JSON,
                        toJsonArray("{\"key\":\"value1\"}", "{\"key\":\"value2\"}")),
                createSampleProperty("MV_VOCABULARY", DataTypeCode.CONTROLLEDVOCABULARY,
                        toJsonArray("dog", "human")),
                createSampleProperty("MV_SAMPLE", DataTypeCode.SAMPLE,
                        toJsonArray(sample1.getPermId(), sample2.getPermId()))
        };

        final RecordingMatcher<Set<IEntityProperty>> definedPropertiesMatcher =
                RecordingMatcher.create();
        context.checking(new Expectations()
            {
                {
                    allowing(daoFactory).getEntityPropertyTypeDAO(EntityKind.SAMPLE);
                    will(returnValue(entityPropertyTypeDAO));

                    allowing(daoFactory).getEntityTypeDAO(EntityKind.SAMPLE);
                    will(returnValue(entityTypeDAO));

                    allowing(daoFactory).getPropertyTypeDAO();
                    will(returnValue(propertyTypeDAO));

                    atLeast(1).of(entityTypeDAO).listEntityTypes();
                    will(returnValue(Collections.singletonList(sampleType)));

                    allowing(entityPropertyTypeDAO).listEntityPropertyTypes(sampleType);
                    will(returnValue(Arrays.asList(
                            createETPT(varcharPropertyType, sampleType),
                            createETPT(multilinePropertyType, sampleType),
                            createETPT(integerPropertyType, sampleType),
                            createETPT(realPropertyType, sampleType),
                            createETPT(timestampPropertyType, sampleType),
                            createETPT(booleanPropertyType, sampleType),
                            createETPT(hyperlinkPropertyType, sampleType),
                            createETPT(datePropertyType, sampleType),
                            createETPT(xmlPropertyType, sampleType),
                            createETPT(jsonPropertyType, sampleType),
                            createETPT(vocabularyPropertyType, sampleType),
                            createETPT(samplePropertyType, sampleType))));

                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_VARCHAR");
                    will(returnValue(varcharPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_MULTILINE");
                    will(returnValue(multilinePropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_INTEGER");
                    will(returnValue(integerPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_REAL");
                    will(returnValue(realPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_TIMESTAMP");
                    will(returnValue(timestampPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_BOOLEAN");
                    will(returnValue(booleanPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_HYPERLINK");
                    will(returnValue(hyperlinkPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_DATE");
                    will(returnValue(datePropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_XML");
                    will(returnValue(xmlPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_JSON");
                    will(returnValue(jsonPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_VOCABULARY");
                    will(returnValue(vocabularyPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_SAMPLE");
                    will(returnValue(samplePropertyType));

                    one(sampleDAO).listByPermID(Collections.singleton(sample1.getPermId()));
                    will(returnValue(Collections.singletonList(sample1)));
                    one(sampleDAO).listByPermID(Collections.singleton(sample2.getPermId()));
                    will(returnValue(Collections.singletonList(sample2)));

                    CollectionMatcher<Set<String>> dynamicPropertiesMatcher =
                            new CollectionMatcher<Set<String>>(new HashSet<String>(
                                    new ArrayList<String>()));
                    one(placeholderCreator).addDynamicPropertiesPlaceholders(
                            with(definedPropertiesMatcher), with(dynamicPropertiesMatcher));
                    one(placeholderCreator).addManagedPropertiesPlaceholders(
                            with(definedPropertiesMatcher), with(dynamicPropertiesMatcher));
                }
            });

        final List<EntityPropertyPE> convertedProperties =
                entityPropertiesConverter.convertProperties(properties, SAMPLE_TYPE_CODE,
                        ManagerTestTool.EXAMPLE_PERSON);

        assertEquals(24, convertedProperties.size());
        assertPropertyValues(convertedProperties, "MV_VARCHAR", "alpha", "beta");
        assertPropertyValues(convertedProperties, "MV_MULTILINE", "line1\nline2", "line3\nline4");
        assertPropertyValues(convertedProperties, "MV_INTEGER", "1", "2");
        assertPropertyValues(convertedProperties, "MV_REAL", "1.5", "2.0");
        assertPropertyValues(convertedProperties, "MV_TIMESTAMP", "2026-01-30 10:18:30 +0100",
                "2026-01-31 11:19:31 +0100");
        assertPropertyValues(convertedProperties, "MV_BOOLEAN", "true", "false");
        assertPropertyValues(convertedProperties, "MV_HYPERLINK", "https://openbis.ch",
                "https://ethz.ch");
        assertPropertyValues(convertedProperties, "MV_DATE", "2026-01-30", "2026-01-31");
        assertPropertyValues(convertedProperties, "MV_XML", "<value>one</value>",
                "<value>two</value>");
        assertPropertyValues(convertedProperties, "MV_JSON", "{\"key\":\"value1\"}",
                "{\"key\":\"value2\"}");
        assertVocabularyPropertyValues(convertedProperties, "MV_VOCABULARY", "DOG", "HUMAN");
        assertSamplePropertyValues(convertedProperties, "MV_SAMPLE", sample1, sample2);
        context.assertIsSatisfied();
    }

    @Test
    public void testConvertMultiValueArrayProperties() throws Exception
    {
        final IEntityPropertiesConverter entityPropertiesConverter =
                new EntityPropertiesConverter(EntityKind.SAMPLE, daoFactory,
                        new PropertyValidator(), placeholderCreator, null,
                        managedPropertyEvaluatorFactory);

        final SampleTypePE sampleType = createSampleType(SAMPLE_TYPE_CODE);
        final PropertyTypePE stringArrayPropertyType =
                createPropertyType("MV_ARRAY_STRING", DataTypeCode.ARRAY_STRING, true);
        final PropertyTypePE integerArrayPropertyType =
                createPropertyType("MV_ARRAY_INTEGER", DataTypeCode.ARRAY_INTEGER, true);
        final PropertyTypePE realArrayPropertyType =
                createPropertyType("MV_ARRAY_REAL", DataTypeCode.ARRAY_REAL, true);
        final PropertyTypePE timestampArrayPropertyType =
                createPropertyType("MV_ARRAY_TIMESTAMP", DataTypeCode.ARRAY_TIMESTAMP, true);

        final IEntityProperty[] properties = new IEntityProperty[]
        {
                createSampleProperty("MV_ARRAY_STRING", DataTypeCode.ARRAY_STRING,
                        toJsonArrayMatrix(new String[] { "alpha", "beta" },
                                new String[] { "gamma", "delta" })),
                createSampleProperty("MV_ARRAY_INTEGER", DataTypeCode.ARRAY_INTEGER,
                        toJsonArrayMatrix(new String[] { "1", "2" },
                                new String[] { "3", "4" })),
                createSampleProperty("MV_ARRAY_REAL", DataTypeCode.ARRAY_REAL,
                        toJsonArrayMatrix(new String[] { "1.5", "2.5" },
                                new String[] { "3.5", "4.5" })),
                createSampleProperty("MV_ARRAY_TIMESTAMP", DataTypeCode.ARRAY_TIMESTAMP,
                        toJsonArrayMatrix(
                                new String[] { "2026-01-30 10:18:30 +0500", "2026-01-31 11:19:31 +0500" },
                                new String[] { "2026-02-01 12:20:32 +0500", "2026-02-02 13:21:33 +0500" }))
        };

        final RecordingMatcher<Set<IEntityProperty>> definedPropertiesMatcher =
                RecordingMatcher.create();
        context.checking(new Expectations()
            {
                {
                    allowing(daoFactory).getEntityPropertyTypeDAO(EntityKind.SAMPLE);
                    will(returnValue(entityPropertyTypeDAO));

                    allowing(daoFactory).getEntityTypeDAO(EntityKind.SAMPLE);
                    will(returnValue(entityTypeDAO));

                    allowing(daoFactory).getPropertyTypeDAO();
                    will(returnValue(propertyTypeDAO));

                    atLeast(1).of(entityTypeDAO).listEntityTypes();
                    will(returnValue(Collections.singletonList(sampleType)));

                    allowing(entityPropertyTypeDAO).listEntityPropertyTypes(sampleType);
                    will(returnValue(Arrays.asList(
                            createETPT(stringArrayPropertyType, sampleType),
                            createETPT(integerArrayPropertyType, sampleType),
                            createETPT(realArrayPropertyType, sampleType),
                            createETPT(timestampArrayPropertyType, sampleType))));

                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_ARRAY_STRING");
                    will(returnValue(stringArrayPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_ARRAY_INTEGER");
                    will(returnValue(integerArrayPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_ARRAY_REAL");
                    will(returnValue(realArrayPropertyType));
                    one(propertyTypeDAO).tryFindPropertyTypeByCode("MV_ARRAY_TIMESTAMP");
                    will(returnValue(timestampArrayPropertyType));

                    CollectionMatcher<Set<String>> dynamicPropertiesMatcher =
                            new CollectionMatcher<Set<String>>(new HashSet<String>(
                                    new ArrayList<String>()));
                    one(placeholderCreator).addDynamicPropertiesPlaceholders(
                            with(definedPropertiesMatcher), with(dynamicPropertiesMatcher));
                    one(placeholderCreator).addManagedPropertiesPlaceholders(
                            with(definedPropertiesMatcher), with(dynamicPropertiesMatcher));
                }
            });

        final List<EntityPropertyPE> convertedProperties =
                entityPropertiesConverter.convertProperties(properties, SAMPLE_TYPE_CODE,
                        ManagerTestTool.EXAMPLE_PERSON);

        assertEquals(8, convertedProperties.size());
        assertStringArrayPropertyValues(convertedProperties, "MV_ARRAY_STRING",
                new String[] { "alpha", "beta" }, new String[] { "gamma", "delta" });
        assertIntegerArrayPropertyValues(convertedProperties, "MV_ARRAY_INTEGER",
                new Long[] { 1L, 2L }, new Long[] { 3L, 4L });
        assertRealArrayPropertyValues(convertedProperties, "MV_ARRAY_REAL",
                new Double[] { 1.5, 2.5 }, new Double[] { 3.5, 4.5 });
        assertTimestampArrayPropertyValues(convertedProperties, "MV_ARRAY_TIMESTAMP",
                new Date[] { parseTimestamp("2026-01-30 10:18:30 +0500"),
                        parseTimestamp("2026-01-31 11:19:31 +0500") },
                new Date[] { parseTimestamp("2026-02-01 12:20:32 +0500"),
                        parseTimestamp("2026-02-02 13:21:33 +0500") });
        context.assertIsSatisfied();
    }

    // @Test
    public void testUpdateProperties()
    {
        final SampleTypePE entityType = createSampleType(SAMPLE_TYPE_CODE);
        PersonPE registrator = new PersonPE();
        SamplePEBuilder builder =
                new SamplePEBuilder().property("TEXT", DataTypeCode.VARCHAR, "hello").property(
                        "NUMBER", DataTypeCode.INTEGER, "123");
        SamplePEBuilder builder2 = new SamplePEBuilder().property("C", DataTypeCode.INTEGER, "123");
        Set<SamplePropertyPE> oldProperties = builder.getSample().getProperties();
        final List<EntityTypePropertyTypePE> assignments =
                new ArrayList<EntityTypePropertyTypePE>();
        for (SamplePropertyPE sampleProperty : oldProperties)
        {
            assignments.add(sampleProperty.getEntityTypePropertyType());
        }
        for (SamplePropertyPE sampleProperty : builder2.getSample().getProperties())
        {
            assignments.add(sampleProperty.getEntityTypePropertyType());
        }
        PropertyBuilder p1 = new PropertyBuilder("NUMBER").type(DataTypeCode.INTEGER).value("42");
        PropertyBuilder p2 = new PropertyBuilder("C").type(DataTypeCode.INTEGER).value("137");
        List<IEntityProperty> newProperties =
                Arrays.<IEntityProperty> asList(p1.getProperty(), p2.getProperty());
        IEntityPropertiesConverter entityPropertiesConverter =
                createEntityPropertiesConverter(EntityKind.SAMPLE);
        final RecordingMatcher<Set<IEntityProperty>> matcherOfProperties =
                new RecordingMatcher<Set<IEntityProperty>>();
        context.checking(new Expectations()
            {
                {
                    allowing(daoFactory).getEntityTypeDAO(EntityKind.SAMPLE);
                    will(Expectations.returnValue(entityTypeDAO));

                    one(entityTypeDAO).listEntityTypes();
                    will(returnValue(Arrays.asList(entityType)));

                    allowing(daoFactory).getEntityPropertyTypeDAO(EntityKind.SAMPLE);
                    will(Expectations.returnValue(entityPropertyTypeDAO));

                    allowing(entityPropertyTypeDAO).listEntityPropertyTypes(entityType);
                    will(returnValue(assignments));

                    one(placeholderCreator).addDynamicPropertiesPlaceholders(
                            with(matcherOfProperties), with(Collections.<String> emptySet()));

                    for (String[] codeAndValue : new String[][]
                    {
                            { "NUMBER", "42" },
                            { "C", "137" } })
                    {
                        String code = codeAndValue[0];
                        one(propertyTypeDAO).tryFindPropertyTypeByCode(code);
                        PropertyTypePE type = tryToFind(assignments, code);
                        if (type == null)
                        {
                            type = new PropertyTypePE();
                            type.setCode(code);
                        }
                        will(returnValue(type));

                        String value = codeAndValue[1];
                        one(propertyValueValidator).validatePropertyValue(type, value);
                        will(returnValue(value));
                    }
                }
            });

        Set<SamplePropertyPE> properties =
                entityPropertiesConverter.updateProperties(oldProperties, entityType,
                        newProperties, registrator);

        List<SamplePropertyPE> props = new ArrayList<SamplePropertyPE>(properties);
        Collections.sort(props, new Comparator<SamplePropertyPE>()
            {
                @Override
                public int compare(SamplePropertyPE o1, SamplePropertyPE o2)
                {
                    return o1.getEntityTypePropertyType().getPropertyType().getCode()
                            .compareTo(o2.getEntityTypePropertyType().getPropertyType().getCode());
                }
            });
        assertEquals("C", props.get(0).getEntityTypePropertyType().getPropertyType().getCode());
        assertEquals(DataTypeCode.INTEGER, props.get(0).getEntityTypePropertyType()
                .getPropertyType().getType().getCode());
        assertEquals("137", props.get(0).getValue());
        assertEquals("NUMBER", props.get(1).getEntityTypePropertyType().getPropertyType().getCode());
        assertEquals(DataTypeCode.INTEGER, props.get(1).getEntityTypePropertyType()
                .getPropertyType().getType().getCode());
        assertEquals("42", props.get(1).getValue());
        assertEquals("TEXT", props.get(2).getEntityTypePropertyType().getPropertyType().getCode());
        assertEquals(DataTypeCode.VARCHAR, props.get(2).getEntityTypePropertyType()
                .getPropertyType().getType().getCode());
        assertEquals("hello", props.get(2).getValue());
        assertEquals(3, props.size());
        context.assertIsSatisfied();
    }

    private PropertyTypePE tryToFind(List<EntityTypePropertyTypePE> assignments, String code)
    {
        for (EntityTypePropertyTypePE entityTypePropertyTypePE : assignments)
        {
            if (entityTypePropertyTypePE.getPropertyType().getCode().equals(code))
            {
                return entityTypePropertyTypePE.getPropertyType();
            }
        }
        return null;
    }

    private PropertyTypePE createPropertyType()
    {
        final PropertyTypePE propertyTypePE = new PropertyTypePE();
        propertyTypePE.setCode(VARCHAR_PROPERTY_TYPE_CODE.toLowerCase());
        DataTypePE type = new DataTypePE();
        type.setCode(DataTypeCode.VARCHAR);
        propertyTypePE.setType(type);
        return propertyTypePE;
    }

    private PropertyTypePE createPropertyType(String code, DataTypeCode dataTypeCode)
    {
        final PropertyTypePE propertyTypePE = new PropertyTypePE();
        propertyTypePE.setCode(code);
        final DataTypePE type = new DataTypePE();
        type.setCode(dataTypeCode);
        propertyTypePE.setType(type);
        return propertyTypePE;
    }

    private PropertyTypePE createPropertyType(String code, DataTypeCode dataTypeCode,
            boolean multiValue)
    {
        final PropertyTypePE propertyTypePE = createPropertyType(code, dataTypeCode);
        propertyTypePE.setMultiValue(multiValue);
        return propertyTypePE;
    }

    private IEntityProperty createSampleProperty(String code, DataTypeCode dataTypeCode,
            String value)
    {
        final IEntityProperty sampleProperty = new EntityProperty();
        sampleProperty.setValue(value);
        final PropertyType propertyType = new PropertyType();
        propertyType.setLabel(code);
        propertyType.setCode(code);
        final DataType dataType = new DataType();
        dataType.setCode(dataTypeCode);
        propertyType.setDataType(dataType);
        sampleProperty.setPropertyType(propertyType);
        return sampleProperty;
    }

    private SampleTypePropertyTypePE createETPT(PropertyTypePE propertyType,
            final SampleTypePE sampleType)
    {
        final SampleTypePropertyTypePE sampleTypePropertyTypePE = new SampleTypePropertyTypePE();
        sampleTypePropertyTypePE.setEntityType(sampleType);
        sampleTypePropertyTypePE.setPropertyType(propertyType);
        sampleTypePropertyTypePE.setMandatory(false);
        return sampleTypePropertyTypePE;
    }

    private Date parseTimestamp(String value) throws Exception
    {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").parse(value);
    }

    private String toJsonArray(String... values) throws Exception
    {
        return new ObjectMapper().writeValueAsString(values);
    }

    private String toJsonArrayMatrix(String[]... values) throws Exception
    {
        return new ObjectMapper().writeValueAsString(values);
    }

    private VocabularyPE createVocabulary(String code, String... termCodes)
    {
        final VocabularyPE vocabulary = new VocabularyPE();
        vocabulary.setCode(code);
        vocabulary.setChosenFromList(true);
        for (int i = 0; i < termCodes.length; i++)
        {
            final VocabularyTermPE term = new VocabularyTermPE();
            term.setCode(termCodes[i]);
            term.setOrdinal((long) i);
            vocabulary.addTerm(term);
        }
        return vocabulary;
    }

    private SamplePE createSample(String permId)
    {
        final SamplePE sample = new SamplePE();
        sample.setPermId(permId);
        return sample;
    }

    private void assertPropertyValues(List<EntityPropertyPE> properties, String code,
            String... expectedValues)
    {
        final List<EntityPropertyPE> matchingProperties = findPropertiesByCode(properties, code);
        assertEquals(expectedValues.length, matchingProperties.size());
        for (int i = 0; i < expectedValues.length; i++)
        {
            assertEquals(expectedValues[i], matchingProperties.get(i).tryGetUntypedValue());
        }
    }

    private void assertVocabularyPropertyValues(List<EntityPropertyPE> properties, String code,
            String... expectedValues)
    {
        final List<EntityPropertyPE> matchingProperties = findPropertiesByCode(properties, code);
        assertEquals(expectedValues.length, matchingProperties.size());
        for (int i = 0; i < expectedValues.length; i++)
        {
            assertEquals(expectedValues[i], matchingProperties.get(i).getVocabularyTerm().getCode());
        }
    }

    private void assertSamplePropertyValues(List<EntityPropertyPE> properties, String code,
            SamplePE... expectedValues)
    {
        final List<EntityPropertyPE> matchingProperties = findPropertiesByCode(properties, code);
        assertEquals(expectedValues.length, matchingProperties.size());
        for (int i = 0; i < expectedValues.length; i++)
        {
            assertSame(expectedValues[i], ((EntityPropertyWithSampleDataTypePE) matchingProperties.get(i))
                    .getSampleValue());
        }
    }

    private void assertStringArrayPropertyValues(List<EntityPropertyPE> properties, String code,
            String[]... expectedValues)
    {
        final List<EntityPropertyPE> matchingProperties = findPropertiesByCode(properties, code);
        assertEquals(expectedValues.length, matchingProperties.size());
        for (int i = 0; i < expectedValues.length; i++)
        {
            assertArrayEquals(expectedValues[i], matchingProperties.get(i).getStringArrayValue());
        }
    }

    private void assertIntegerArrayPropertyValues(List<EntityPropertyPE> properties, String code,
            Long[]... expectedValues)
    {
        final List<EntityPropertyPE> matchingProperties = findPropertiesByCode(properties, code);
        assertEquals(expectedValues.length, matchingProperties.size());
        for (int i = 0; i < expectedValues.length; i++)
        {
            assertArrayEquals(expectedValues[i], matchingProperties.get(i).getIntegerArrayValue());
        }
    }

    private void assertRealArrayPropertyValues(List<EntityPropertyPE> properties, String code,
            Double[]... expectedValues)
    {
        final List<EntityPropertyPE> matchingProperties = findPropertiesByCode(properties, code);
        assertEquals(expectedValues.length, matchingProperties.size());
        for (int i = 0; i < expectedValues.length; i++)
        {
            assertArrayEquals(expectedValues[i], matchingProperties.get(i).getRealArrayValue());
        }
    }

    private void assertTimestampArrayPropertyValues(List<EntityPropertyPE> properties, String code,
            Date[]... expectedValues)
    {
        final List<EntityPropertyPE> matchingProperties = findPropertiesByCode(properties, code);
        assertEquals(expectedValues.length, matchingProperties.size());
        for (int i = 0; i < expectedValues.length; i++)
        {
            assertArrayEquals(expectedValues[i], matchingProperties.get(i).getTimestampArrayValue());
        }
    }

    private List<EntityPropertyPE> findPropertiesByCode(List<EntityPropertyPE> properties,
            String code)
    {
        final List<EntityPropertyPE> matchingProperties = new ArrayList<EntityPropertyPE>();
        for (EntityPropertyPE property : properties)
        {
            if (code.equals(property.getEntityTypePropertyType().getPropertyType().getCode()))
            {
                matchingProperties.add(property);
            }
        }
        return matchingProperties;
    }

    @Test
    public void testCreateProperty() throws Exception
    {
        final IEntityPropertiesConverter entityPropertiesConverter =
                createEntityPropertiesConverter(EntityKind.SAMPLE);
        final PropertyTypePE propertyType = createPropertyType();
        EntityKind entityKind = EntityKind.EXPERIMENT;
        EntityTypePropertyTypePE assignment =
                EntityTypePropertyTypePE.createEntityTypePropertyType(entityKind);
        PersonPE registrator = new PersonPE();
        final String defaultValue = "val";
        context.checking(new Expectations()
            {
                {
                    one(propertyValueValidator).validatePropertyValue(propertyType, defaultValue);
                }
            });
        entityPropertiesConverter.tryCreateValidatedPropertyValue(propertyType, assignment,
                defaultValue);
        assertEquals(
                registrator,
                entityPropertiesConverter.createValidatedProperty(propertyType, assignment,
                        registrator, defaultValue).get(0).getRegistrator());
        context.assertIsSatisfied();
    }

    @Test(expectedExceptions = UserFailureException.class)
    public void testCreateValidatedPropertyValueMandatoryWithNullGlobal() throws Exception
    {
        final IEntityPropertiesConverter entityPropertiesConverter =
                createEntityPropertiesConverter(EntityKind.SAMPLE);
        final PropertyTypePE propertyType = new PropertyTypePE();
        EntityKind entityKind = EntityKind.EXPERIMENT;
        EntityTypePropertyTypePE assignment =
                EntityTypePropertyTypePE.createEntityTypePropertyType(entityKind);
        assignment.setMandatory(true);
        final String defaultValue = null;

        entityPropertiesConverter.tryCreateValidatedPropertyValue(propertyType, assignment,
                defaultValue);
        context.assertIsSatisfied();
    }

    @Test
    public void testCreateValidatedPropertyValueNotMandatoryWithNullGlobal() throws Exception
    {
        final IEntityPropertiesConverter entityPropertiesConverter =
                createEntityPropertiesConverter(EntityKind.EXPERIMENT);
        final PropertyTypePE propertyType = new PropertyTypePE();
        EntityKind entityKind = EntityKind.EXPERIMENT;
        EntityTypePropertyTypePE assignment =
                EntityTypePropertyTypePE.createEntityTypePropertyType(entityKind);
        assignment.setMandatory(false);
        final String defaultValue = null;
        assertNull(entityPropertiesConverter.tryCreateValidatedPropertyValue(propertyType,
                assignment, defaultValue));
        context.assertIsSatisfied();
    }

}
