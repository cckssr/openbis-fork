package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyAssignmentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.RoCrateSchemaValidation;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.ValidationResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RoCrateSchemaValidationTest
{

    @Test
    public void emptyModelIsOkay()
    {
        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                        Map.of(), Map.of());
        ValidationResult result =
                RoCrateSchemaValidation.validate(openBisModel);
        Assert.assertTrue(result.getEntitiesToMissingProperties().isEmpty());
        Assert.assertTrue(result.getEntititesToUndefinedProperties().isEmpty());

    }

    @Test
    public void objectWithKnownPropertyIsOkay()
    {
        String typeCode = "PERSON";
        String propertyCode = "PERSON.FIRSTNAME";
        SampleType entityType = new SampleType();

        Map<EntityTypePermId, IEntityType> entityTypes = new LinkedHashMap<>();
        {
            PropertyType propertyType = new PropertyType();
            propertyType.setCode(propertyCode);
            PropertyTypeFetchOptions propertyTypeFetchOptions = new PropertyTypeFetchOptions();
            propertyType.setFetchOptions(propertyTypeFetchOptions);
            PropertyAssignmentFetchOptions propertyAssignmentFetchOptions =
                    new PropertyAssignmentFetchOptions();
            propertyType.setDataType(DataType.VARCHAR);
            propertyType.setPermId(new PropertyTypePermId(propertyCode));

            entityType.setPropertyAssignments(new ArrayList<>());
            EntityTypePermId permId = new EntityTypePermId(typeCode, EntityKind.SAMPLE);
            entityType.setPermId(permId);

            SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
            sampleTypeFetchOptions.withPropertyAssignments();
            entityType.setFetchOptions(sampleTypeFetchOptions);

            entityTypes.put(permId, entityType);

        }
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> entities = new LinkedHashMap<>();
        {
            String sampleCode = "PERSON1";
            Sample sample = new Sample();
            SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
            sampleFetchOptions.withExperiment();
            sampleFetchOptions.withProject();
            sampleFetchOptions.withSpace();
            sampleFetchOptions.withType();
            sampleFetchOptions.withProperties();
            sample.setFetchOptions(sampleFetchOptions);
            sample.setType(entityType);
            sample.setProperty(propertyCode, "asdf");
            entities.put(new SampleIdentifier("/DEFAULT/DEFAULT/" + sampleCode), sample);
        }

        PropertyType propertyType = new PropertyType();
        propertyType.setPermId(new PropertyTypePermId(propertyCode));
        propertyType.setDataType(DataType.VARCHAR);
        propertyType.setCode(propertyCode);

        PropertyAssignment propertyAssignment = new PropertyAssignment();

        PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
        fetchOptions.withEntityType();
        fetchOptions.withPropertyType();
        propertyAssignment.setFetchOptions(fetchOptions);
        propertyAssignment.setPermId(
                new PropertyAssignmentPermId(entityType.getPermId(), propertyType.getPermId()));
        propertyAssignment.setPropertyType(propertyType);
        propertyAssignment.setMandatory(false);
        entityType.setPropertyAssignments(List.of(propertyAssignment));


        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), entityTypes, Map.of(), Map.of(), entities, Map.of(),
                        Map.of(), Map.of());

        ValidationResult result =
                RoCrateSchemaValidation.validate(openBisModel);
        Assert.assertTrue(result.getEntitiesToMissingProperties().isEmpty());
        Assert.assertTrue(result.getEntititesToUndefinedProperties().isEmpty());

    }

    @Test
    public void objectWithKnownMandatoryPropertyIsOkay()
    {
        String typeCode = "PERSON";
        String propertyCode = "PERSON.FIRSTNAME";
        SampleType entityType = new SampleType();

        Map<EntityTypePermId, IEntityType> entityTypes = new LinkedHashMap<>();
        {
            PropertyType propertyType = new PropertyType();
            propertyType.setCode(propertyCode);
            PropertyTypeFetchOptions propertyTypeFetchOptions = new PropertyTypeFetchOptions();
            propertyType.setFetchOptions(propertyTypeFetchOptions);
            PropertyAssignmentFetchOptions propertyAssignmentFetchOptions =
                    new PropertyAssignmentFetchOptions();
            propertyType.setDataType(DataType.VARCHAR);
            propertyType.setPermId(new PropertyTypePermId(propertyCode));

            entityType.setPropertyAssignments(new ArrayList<>());
            EntityTypePermId permId = new EntityTypePermId(typeCode, EntityKind.SAMPLE);
            entityType.setPermId(permId);

            SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
            sampleTypeFetchOptions.withPropertyAssignments();
            entityType.setFetchOptions(sampleTypeFetchOptions);

            entityTypes.put(permId, entityType);

        }
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> entities = new LinkedHashMap<>();
        {
            String sampleCode = "PERSON1";
            Sample sample = new Sample();
            SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
            sampleFetchOptions.withExperiment();
            sampleFetchOptions.withProject();
            sampleFetchOptions.withSpace();
            sampleFetchOptions.withType();
            sampleFetchOptions.withProperties();
            sample.setFetchOptions(sampleFetchOptions);
            sample.setType(entityType);
            sample.setProperty(propertyCode, "asdf");
            entities.put(new SampleIdentifier("/DEFAULT/DEFAULT/" + sampleCode), sample);
        }

        PropertyType propertyType = new PropertyType();
        propertyType.setCode(propertyCode);
        propertyType.setDataType(DataType.VARCHAR);
        propertyType.setPermId(new PropertyTypePermId(propertyCode));
        PropertyAssignment propertyAssignment = new PropertyAssignment();

        PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
        fetchOptions.withEntityType();
        fetchOptions.withPropertyType();
        propertyAssignment.setFetchOptions(fetchOptions);
        propertyAssignment.setPermId(
                new PropertyAssignmentPermId(entityType.getPermId(), propertyType.getPermId()));
        propertyAssignment.setPropertyType(propertyType);
        propertyAssignment.setMandatory(true);
        entityType.setPropertyAssignments(List.of(propertyAssignment));

        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), entityTypes, Map.of(), Map.of(), entities, Map.of(),
                        Map.of(), Map.of());

        ValidationResult result =
                RoCrateSchemaValidation.validate(openBisModel);
        Assert.assertTrue(result.getEntitiesToMissingProperties().isEmpty());
        Assert.assertTrue(result.getEntititesToUndefinedProperties().isEmpty());

    }

    @Test
    public void objectWithMissingMandatoryPropertyIsNotOkay()
    {
        String typeCode = "PERSON";
        String propertyCode = "PERSON.FIRSTNAME";
        SampleType entityType = new SampleType();

        Map<EntityTypePermId, IEntityType> entityTypes = new LinkedHashMap<>();
        {
            PropertyType propertyType = new PropertyType();
            propertyType.setCode(propertyCode);
            PropertyTypeFetchOptions propertyTypeFetchOptions = new PropertyTypeFetchOptions();
            propertyType.setFetchOptions(propertyTypeFetchOptions);
            PropertyAssignmentFetchOptions propertyAssignmentFetchOptions =
                    new PropertyAssignmentFetchOptions();
            propertyType.setDataType(DataType.VARCHAR);
            propertyType.setPermId(new PropertyTypePermId(propertyCode));

            entityType.setPropertyAssignments(new ArrayList<>());
            EntityTypePermId permId = new EntityTypePermId(typeCode, EntityKind.SAMPLE);
            entityType.setPermId(permId);

            SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
            sampleTypeFetchOptions.withPropertyAssignments();
            entityType.setFetchOptions(sampleTypeFetchOptions);

            entityTypes.put(permId, entityType);

        }
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> entities = new LinkedHashMap<>();
        {
            String sampleCode = "PERSON1";
            Sample sample = new Sample();
            SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
            sampleFetchOptions.withExperiment();
            sampleFetchOptions.withProject();
            sampleFetchOptions.withSpace();
            sampleFetchOptions.withType();
            sampleFetchOptions.withProperties();
            sample.setFetchOptions(sampleFetchOptions);
            sample.setType(entityType);
            entities.put(new SampleIdentifier("/DEFAULT/DEFAULT/" + sampleCode), sample);
        }

        PropertyType propertyType = new PropertyType();
        propertyType.setCode(propertyCode);
        propertyType.setPermId(new PropertyTypePermId(propertyCode));
        propertyType.setDataType(DataType.VARCHAR);

        PropertyAssignment propertyAssignment = new PropertyAssignment();

        PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
        fetchOptions.withEntityType();
        fetchOptions.withPropertyType();
        propertyAssignment.setFetchOptions(fetchOptions);
        propertyAssignment.setPermId(
                new PropertyAssignmentPermId(entityType.getPermId(), propertyType.getPermId()));
        propertyAssignment.setPropertyType(propertyType);
        propertyAssignment.setMandatory(true);
        entityType.setPropertyAssignments(List.of(propertyAssignment));

        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), entityTypes, Map.of(), Map.of(), entities, Map.of(),
                        Map.of(), Map.of());

        ValidationResult result =
                RoCrateSchemaValidation.validate(openBisModel);
        Assert.assertFalse(result.getEntitiesToMissingProperties().isEmpty());
        Assert.assertTrue(result.getEntititesToUndefinedProperties().isEmpty());

    }

    @Test
    public void objectWithUnkownAssingmentIsNotOkay()
    {
        String typeCode = "PERSON";
        String propertyCode = "PERSON.FIRSTNAME";
        SampleType entityType = new SampleType();

        Map<EntityTypePermId, IEntityType> entityTypes = new LinkedHashMap<>();
        {
            PropertyType propertyType = new PropertyType();
            propertyType.setCode(propertyCode);
            PropertyTypeFetchOptions propertyTypeFetchOptions = new PropertyTypeFetchOptions();
            propertyType.setFetchOptions(propertyTypeFetchOptions);
            PropertyAssignmentFetchOptions propertyAssignmentFetchOptions =
                    new PropertyAssignmentFetchOptions();
            propertyType.setDataType(DataType.VARCHAR);
            propertyType.setPermId(new PropertyTypePermId(propertyCode));

            entityType.setPropertyAssignments(new ArrayList<>());
            EntityTypePermId permId = new EntityTypePermId(typeCode, EntityKind.SAMPLE);
            entityType.setPermId(permId);

            SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
            sampleTypeFetchOptions.withPropertyAssignments();
            entityType.setFetchOptions(sampleTypeFetchOptions);

            entityTypes.put(permId, entityType);

        }
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> entities = new LinkedHashMap<>();
        {
            String sampleCode = "PERSON1";
            Sample sample = new Sample();
            SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
            sampleFetchOptions.withExperiment();
            sampleFetchOptions.withProject();
            sampleFetchOptions.withSpace();
            sampleFetchOptions.withType();
            sampleFetchOptions.withProperties();
            sample.setFetchOptions(sampleFetchOptions);
            sample.setType(entityType);
            sample.setProperty(propertyCode, "asdf");
            entities.put(new SampleIdentifier("/DEFAULT/DEFAULT/" + sampleCode), sample);
        }

        PropertyType propertyType = new PropertyType();
        propertyType.setCode(propertyCode);
        propertyType.setPermId(new PropertyTypePermId(propertyCode));
        PropertyAssignment propertyAssignment = new PropertyAssignment();

        PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
        fetchOptions.withEntityType();
        fetchOptions.withPropertyType();
        propertyAssignment.setFetchOptions(fetchOptions);
        propertyAssignment.setPermId(
                new PropertyAssignmentPermId(entityType.getPermId(), propertyType.getPermId()));
        propertyAssignment.setPropertyType(propertyType);

        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), entityTypes, Map.of(), Map.of(), entities, Map.of(),
                        Map.of(), Map.of());

        ValidationResult result =
                RoCrateSchemaValidation.validate(openBisModel);
        Assert.assertTrue(result.getEntitiesToMissingProperties().isEmpty());
        Assert.assertFalse(result.getEntititesToUndefinedProperties().isEmpty());

    }

    @Test
    public void checkDataTypes()
    {
        String typeCode = "PERSON";
        String propertyCode = "PERSON.FIRSTNAME";
        SampleType entityType = new SampleType();

        Map<EntityTypePermId, IEntityType> entityTypes = new LinkedHashMap<>();
        {

            entityType.setPropertyAssignments(new ArrayList<>());
            EntityTypePermId permId = new EntityTypePermId(typeCode, EntityKind.SAMPLE);
            entityType.setPermId(permId);

            SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
            sampleTypeFetchOptions.withPropertyAssignments();
            entityType.setFetchOptions(sampleTypeFetchOptions);

            entityTypes.put(permId, entityType);

        }
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> entities = new LinkedHashMap<>();
        Sample sample = new Sample();

        {
            String sampleCode = "PERSON1";
            SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
            sampleFetchOptions.withExperiment();
            sampleFetchOptions.withProject();
            sampleFetchOptions.withSpace();
            sampleFetchOptions.withType();
            sampleFetchOptions.withProperties();
            sample.setFetchOptions(sampleFetchOptions);
            sample.setType(entityType);
            sample.setProperty(propertyCode, "asdf");
            entities.put(new SampleIdentifier("/DEFAULT/DEFAULT/" + sampleCode), sample);
        }

        List<PropertyAssignment> propertyAssignments = new ArrayList<>();
        {
        }
        Map<String, Serializable> propertyValues = new LinkedHashMap<>();
        for (PropertyAssignmentCase propertyAssignmentCase : getPropertyAssignmentInfo())
        {

            {
                String name = propertyAssignmentCase.baseName + "CORRECT";
                PropertyType propertyType = new PropertyType();
                propertyType.setCode(name);
                propertyType.setDataType(propertyAssignmentCase.dataType);
                propertyType.setPermId(
                        new PropertyTypePermId(name));
                PropertyAssignment propertyAssignment = new PropertyAssignment();

                PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
                fetchOptions.withEntityType();
                fetchOptions.withPropertyType();
                propertyAssignment.setFetchOptions(fetchOptions);
                propertyAssignment.setPermId(
                        new PropertyAssignmentPermId(entityType.getPermId(),
                                propertyType.getPermId()));
                propertyAssignment.setPropertyType(propertyType);
                propertyAssignment.setMandatory(false);
                propertyAssignments.add(propertyAssignment);
                propertyValues.put(name, propertyAssignmentCase.correctValue);

            }
            {
                String name = propertyAssignmentCase.baseName + "WRONG";
                PropertyType propertyType = new PropertyType();
                propertyType.setCode(name);
                propertyType.setDataType(propertyAssignmentCase.dataType);
                propertyType.setPermId(new PropertyTypePermId(name));
                PropertyAssignment propertyAssignment = new PropertyAssignment();

                PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
                fetchOptions.withEntityType();
                fetchOptions.withPropertyType();
                propertyAssignment.setFetchOptions(fetchOptions);
                propertyAssignment.setPermId(
                        new PropertyAssignmentPermId(entityType.getPermId(),
                                propertyType.getPermId()));
                propertyAssignment.setPropertyType(propertyType);
                propertyAssignment.setMandatory(false);
                propertyAssignments.add(propertyAssignment);
                propertyValues.put(name, propertyAssignmentCase.incorrectValue);
            }
            sample.setProperties(propertyValues);

        }
        entityType.setPropertyAssignments(propertyAssignments);


        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), entityTypes, Map.of(), Map.of(), entities, Map.of(),
                        Map.of(), Map.of());

        ValidationResult result =
                RoCrateSchemaValidation.validate(openBisModel);
        Assert.assertTrue(result.getEntitiesToMissingProperties().isEmpty());
        Assert.assertTrue(result.getEntititesToUndefinedProperties().isEmpty());
        Assert.assertFalse(result.getWrongDataTypes().isEmpty());
        for (PropertyAssignmentCase propertyAssignmentCase : getPropertyAssignmentInfo())
        {
            Assert.assertFalse(result.getWrongDataTypes().get(entityType.getPermId().getPermId())
                            .contains(propertyAssignmentCase.baseName + "CORRECT"),
                    "Data type " + propertyAssignmentCase.dataType.name() + " with property name \"" + propertyAssignmentCase.baseName + "CORRECT\" does not recognize correct value " + propertyAssignmentCase.correctValue);
            Assert.assertTrue(result.getWrongDataTypes().get(entityType.getPermId().getPermId())
                            .stream().anyMatch(
                                    x -> x.getProperty().equals(propertyAssignmentCase.baseName + "WRONG")),
                    "Data type " + propertyAssignmentCase.dataType.name() + " with property name \"" + propertyAssignmentCase.baseName + "WRONG\" does not recognize incorrect value " + propertyAssignmentCase.incorrectValue);
        }


    }

    private static List<PropertyAssignmentCase> getPropertyAssignmentInfo()
    {
        List<PropertyAssignmentCase> res = new ArrayList<>();
        res.add(new PropertyAssignmentCase(DataType.INTEGER, "INTEGER", "123", "123a"));
        res.add(new PropertyAssignmentCase(DataType.REAL, "REAL", "123.0", "123a"));
        res.add(new PropertyAssignmentCase(DataType.TIMESTAMP, "TIMESTAMP",
                "2025-07-16T09:08:14+0000",
                "123a"));
        res.add(new PropertyAssignmentCase(DataType.SAMPLE, "SAMPLE", "/EXAMPLE/EXAMPLE/SAMPLE1",
                "SAMPLE1"));
        res.add(new PropertyAssignmentCase(DataType.BOOLEAN, "BOOLEAN", "true",
                "verily"));
        res.add(new PropertyAssignmentCase(DataType.DATE, "DATE",
                "2025-07-16T09:08:14+0000",
                "123a"));
        return res;

    }

    private static class PropertyAssignmentCase
    {
        DataType dataType;

        String baseName;

        String correctValue;

        String incorrectValue;

        public PropertyAssignmentCase(DataType dataType, String baseName, String correctValue,
                String incorrectValue)
        {
            this.dataType = dataType;
            this.baseName = baseName;
            this.correctValue = correctValue;
            this.incorrectValue = incorrectValue;
        }
    }

}