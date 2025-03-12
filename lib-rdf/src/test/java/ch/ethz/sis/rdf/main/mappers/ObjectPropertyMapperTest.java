package ch.ethz.sis.rdf.main.mappers;

import ch.ethz.sis.rdf.main.mappers.rdf.ObjectPropertyMapper;
import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ObjectPropertyMapperTest
{
    private OntModel model;

    @Before
    public void setUp() {
        model = ModelFactory.createOntologyModel();

        // Create ontology object properties
        ObjectProperty hasOriginLocation = model.createObjectProperty("https://biomedit.ch/rdf/sphn-schema/sphn#hasOriginLocation");
        ObjectProperty hasDrug = model.createObjectProperty("https://biomedit.ch/rdf/sphn-schema/sphn#hasDrug");

        // Create ontology classes for ranges
        OntClass locationClass = model.createClass("https://biomedit.ch/rdf/sphn-schema/sphn#Location");
        OntClass drugClass = model.createClass("https://biomedit.ch/rdf/sphn-schema/sphn#Drug");

        // Set the range for each property
        hasOriginLocation.addRange(locationClass);
        hasDrug.addRange(drugClass);

        // Create a union class for a property
        ObjectProperty hasCode = model.createObjectProperty("https://biomedit.ch/rdf/sphn-schema/sphn#hasCode");
        OntClass terminologyClass = model.createClass("https://biomedit.ch/rdf/sphn-schema/sphn#Terminology");
        OntClass codeClass = model.createClass("https://biomedit.ch/rdf/sphn-schema/sphn#Code");

        UnionClass unionClass = model.createUnionClass(null, model.createList(new RDFNode[]{terminologyClass, codeClass}));
        hasCode.addRange(unionClass);
    }

    @Test
    public void testGetObjectPropToOntClassMap() {
        // Call the method to test
        Map<String, List<String>> objectPropertyMap =
                ObjectPropertyMapper.getObjectPropToOntClassMap(model,
                        new ModelRDF());

        // Validate that the map is not null or empty
        assertNotNull(objectPropertyMap);
        assertFalse(objectPropertyMap.isEmpty());

        // Check that specific object properties map to the expected ranges
        assertTrue(objectPropertyMap.containsKey("https://biomedit.ch/rdf/sphn-schema/sphn#hasOriginLocation"));
        assertTrue(objectPropertyMap.containsKey("https://biomedit.ch/rdf/sphn-schema/sphn#hasDrug"));

        // Validate the values for each property
        List<String> originLocationRanges = objectPropertyMap.get("https://biomedit.ch/rdf/sphn-schema/sphn#hasOriginLocation");
        assertEquals(1, originLocationRanges.size());
        assertEquals("LOCATION", originLocationRanges.get(0)); // LocalName in upper case

        List<String> drugRanges = objectPropertyMap.get("https://biomedit.ch/rdf/sphn-schema/sphn#hasDrug");
        assertEquals(1, drugRanges.size());
        assertEquals("DRUG", drugRanges.get(0)); // LocalName in upper case
    }

    @Test
    public void testGetObjectPropToOntClassMapWithUnionRange() {
        // Call the method to test
        Map<String, List<String>> objectPropertyMap =
                ObjectPropertyMapper.getObjectPropToOntClassMap(model, new ModelRDF());

        // Check that the property with a union range is correctly processed
        assertTrue(objectPropertyMap.containsKey("https://biomedit.ch/rdf/sphn-schema/sphn#hasCode"));

        // Validate the values for the union class
        List<String> codeRanges = objectPropertyMap.get("https://biomedit.ch/rdf/sphn-schema/sphn#hasCode");
        assertEquals(2, codeRanges.size());
        assertTrue(codeRanges.contains("TERMINOLOGY"));
        assertTrue(codeRanges.contains("CODE"));
    }

    @Test
    public void testEmptyModel() {
        // Create an empty model
        OntModel emptyModel = ModelFactory.createOntologyModel();

        // Call the method with the empty model
        Map<String, List<String>> objectPropertyMap =
                ObjectPropertyMapper.getObjectPropToOntClassMap(emptyModel, new ModelRDF());

        // Validate that the result is an empty map
        assertNotNull(objectPropertyMap);
        assertTrue(objectPropertyMap.isEmpty());
    }
}