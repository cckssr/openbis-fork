package ch.openbis.rocrate.app.reader.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PropertyTypePruningTest
{

    @Test
    public void testPruningEmptyCase()
    {
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> entities = new LinkedHashMap<>();

        SampleType sampleType = new SampleType();
        {
            SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
            sampleTypeFetchOptions.withPropertyAssignments();
            sampleType.setFetchOptions(sampleTypeFetchOptions);
        }
        sampleType.setCode("SAMPLETYPE");

        Sample sample = new Sample();
        {
            SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
            sampleFetchOptions.withProperties();
            sampleFetchOptions.withType().withPropertyAssignments().withPropertyType();
            sample.setFetchOptions(sampleFetchOptions);
            sample.setType(sampleType);

        }
        sample.setCode("TETLAPALOLTILONI");
        {
            PropertyType propertyType = new PropertyType();
            propertyType.setCode("PROPERTY");

            PropertyAssignmentFetchOptions propertyAssignmentFetchOptions =
                    new PropertyAssignmentFetchOptions();
            propertyAssignmentFetchOptions.withPropertyType();

            PropertyAssignment propertyAssignment = new PropertyAssignment();
            propertyAssignment.setPropertyType(propertyType);
            propertyAssignment.setMandatory(false);
            propertyAssignment.setFetchOptions(propertyAssignmentFetchOptions);
            sampleType.setPropertyAssignments(List.of(propertyAssignment));
        }
        entities.put(new SampleIdentifier("aaaaa"), sample);

        PropertyTypePruning.prune(entities);
        Assert.assertTrue(sampleType.getPropertyAssignments().isEmpty());

    }

    @Test
    public void testPruningMandatoryPropertyRemains()
    {
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> entities = new LinkedHashMap<>();

        SampleType sampleType = new SampleType();
        {
            SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
            sampleTypeFetchOptions.withPropertyAssignments();
            sampleType.setFetchOptions(sampleTypeFetchOptions);
        }
        sampleType.setCode("SAMPLETYPE");

        Sample sample = new Sample();
        {
            SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
            sampleFetchOptions.withProperties();
            sampleFetchOptions.withType().withPropertyAssignments().withPropertyType();
            sample.setFetchOptions(sampleFetchOptions);
            sample.setType(sampleType);

        }
        sample.setProperties(Map.of("SOMETHINGELSE", "stuff"));
        sample.setCode("TEST");
        {
            PropertyType propertyType = new PropertyType();
            propertyType.setCode("PROPERTY");

            PropertyAssignmentFetchOptions propertyAssignmentFetchOptions =
                    new PropertyAssignmentFetchOptions();
            propertyAssignmentFetchOptions.withPropertyType();

            PropertyAssignment propertyAssignment = new PropertyAssignment();
            propertyAssignment.setPropertyType(propertyType);
            propertyAssignment.setMandatory(true);
            propertyAssignment.setFetchOptions(propertyAssignmentFetchOptions);
            sampleType.setPropertyAssignments(List.of(propertyAssignment));
        }
        entities.put(new SampleIdentifier("aaaaa"), sample);

        PropertyTypePruning.prune(entities);
        Assert.assertFalse(sample.getType().getPropertyAssignments().isEmpty());

    }

    @Test
    public void testPruningNonMandatoryNonEmptyCaseIsRetained()
    {
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> entities = new LinkedHashMap<>();

        SampleType sampleType = new SampleType();
        {
            SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
            sampleTypeFetchOptions.withPropertyAssignments();
            sampleType.setFetchOptions(sampleTypeFetchOptions);
        }
        sampleType.setCode("SAMPLETYPE");

        Sample sample = new Sample();
        {
            SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
            sampleFetchOptions.withProperties();
            sampleFetchOptions.withType().withPropertyAssignments().withPropertyType();
            sample.setFetchOptions(sampleFetchOptions);
            sample.setType(sampleType);

        }
        sample.setProperties(Map.of("SOMETHINGELSE", "stuff"));
        sample.setCode("TEST");
        {
            PropertyType propertyType = new PropertyType();
            propertyType.setCode("PROPERTY");

            PropertyAssignmentFetchOptions propertyAssignmentFetchOptions =
                    new PropertyAssignmentFetchOptions();
            propertyAssignmentFetchOptions.withPropertyType();

            PropertyAssignment propertyAssignment = new PropertyAssignment();
            propertyAssignment.setPropertyType(propertyType);
            propertyAssignment.setMandatory(true);
            sampleType.setPropertyAssignments(List.of(propertyAssignment));
            propertyAssignment.setFetchOptions(propertyAssignmentFetchOptions);
        }
        entities.put(new SampleIdentifier("aaaaa"), sample);

        PropertyTypePruning.prune(entities);
        Assert.assertFalse(sample.getType().getPropertyAssignments().isEmpty());

    }

    @Test
    public void testPruningNonMandatorySometimesNonEmptyCaseIsRetained()
    {
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> entities = new LinkedHashMap<>();

        SampleType sampleType = new SampleType();
        {
            SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
            sampleTypeFetchOptions.withPropertyAssignments();
            sampleType.setFetchOptions(sampleTypeFetchOptions);
        }
        sampleType.setCode("SAMPLETYPE");

        Sample sample = new Sample();
        {
            SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
            sampleFetchOptions.withProperties();
            sampleFetchOptions.withType().withPropertyAssignments().withPropertyType();
            sample.setFetchOptions(sampleFetchOptions);
            sample.setType(sampleType);

        }

        Sample sample2 = new Sample();
        {
            SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
            sampleFetchOptions.withProperties();
            sampleFetchOptions.withType().withPropertyAssignments().withPropertyType();
            sample2.setFetchOptions(sampleFetchOptions);
            sample2.setType(sampleType);

        }
        sample.setProperties(Map.of("PROPERTY", "stuff"));
        sample.setCode("TEST");

        sample2.setProperties(Map.of("PROPERTYasd", "stuff"));
        sample2.setCode("TEST2");

        {
            PropertyType propertyType = new PropertyType();
            propertyType.setCode("PROPERTY");

            PropertyAssignmentFetchOptions propertyAssignmentFetchOptions =
                    new PropertyAssignmentFetchOptions();
            propertyAssignmentFetchOptions.withPropertyType();

            PropertyAssignment propertyAssignment = new PropertyAssignment();
            propertyAssignment.setPropertyType(propertyType);
            propertyAssignment.setMandatory(true);
            sampleType.setPropertyAssignments(List.of(propertyAssignment));
            propertyAssignment.setFetchOptions(propertyAssignmentFetchOptions);
        }
        entities.put(new SampleIdentifier("aaaaa"), sample);
        entities.put(new SampleIdentifier("aaaaasda"), sample2);

        PropertyTypePruning.prune(entities);
        Assert.assertFalse(sample.getType().getPropertyAssignments().isEmpty());

    }


    @Test
    public void testPruningRemoveEmptyProperty()
    {
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> entities = new LinkedHashMap<>();

        SampleType sampleType = new SampleType();
        {
            SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
            sampleTypeFetchOptions.withPropertyAssignments();
            sampleType.setFetchOptions(sampleTypeFetchOptions);
        }
        sampleType.setCode("SAMPLETYPE");

        Sample sample = new Sample();
        {
            SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
            sampleFetchOptions.withType().withPropertyAssignments().withPropertyType();
            sampleFetchOptions.withProperties();
            sample.setFetchOptions(sampleFetchOptions);
            sample.setType(sampleType);

        }
        sample.setProperties(Map.of());
        sample.setCode("TEST");
        {
            PropertyType propertyType = new PropertyType();
            propertyType.setCode("PROPERTY");

            PropertyAssignmentFetchOptions propertyAssignmentFetchOptions =
                    new PropertyAssignmentFetchOptions();
            propertyAssignmentFetchOptions.withPropertyType();

            PropertyAssignment propertyAssignment = new PropertyAssignment();
            propertyAssignment.setPropertyType(propertyType);
            propertyAssignment.setMandatory(false);
            propertyAssignment.setFetchOptions(propertyAssignmentFetchOptions);
            sampleType.setPropertyAssignments(List.of(propertyAssignment));
        }
        entities.put(new SampleIdentifier("aaaaa"), sample);

        PropertyTypePruning.prune(entities);
        Assert.assertTrue(sample.getType().getPropertyAssignments().isEmpty());

    }

}
