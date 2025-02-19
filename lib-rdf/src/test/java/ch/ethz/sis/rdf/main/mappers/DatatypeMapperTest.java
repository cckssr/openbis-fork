package ch.ethz.sis.rdf.main.mappers;

import ch.ethz.sis.rdf.main.parser.LoaderRDF;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DatatypeMapperTest
{
    private OntModel model;

    @Before
    public void setUp(){
        String inputFilePath = "src/test/resources/sphn_test_datatype_prop.ttl";
        model = LoaderRDF.loadOntModel(new String[] { inputFilePath }, "TTL");
    }

    @Test
    public void testGetRDFtoOpenBISDataTypeMap() {
        // Call the method to test
        Map<String, List<String>> dataTypeMap = DatatypeMapper.getRDFtoOpenBISDataTypeMap(model);

        // Validate that the map is not null or empty
        assertNotNull(dataTypeMap);
        assertFalse(dataTypeMap.isEmpty());

        // Validate mappings for specific datatype properties
        assertTrue(dataTypeMap.containsKey("https://biomedit.ch/rdf/sphn-schema/sphn#hasDateTime"));
        assertTrue(dataTypeMap.containsKey("https://biomedit.ch/rdf/sphn-schema/sphn#hasName"));
        assertTrue(dataTypeMap.containsKey("https://biomedit.ch/rdf/sphn-schema/sphn#hasMonth"));

        // Check expected OpenBIS data types
        List<String> collectionDateTimeTypes = dataTypeMap.get("https://biomedit.ch/rdf/sphn-schema/sphn#hasDateTime");
        assertEquals(1, collectionDateTimeTypes.size());
        assertEquals("TIMESTAMP", collectionDateTimeTypes.get(0));

        List<String> templateIdentifierTypes = dataTypeMap.get("https://biomedit.ch/rdf/sphn-schema/sphn#hasName");
        assertEquals(1, templateIdentifierTypes.size());
        assertEquals("VARCHAR", templateIdentifierTypes.get(0));

        List<String> monthTypes = dataTypeMap.get("https://biomedit.ch/rdf/sphn-schema/sphn#hasMonth");
        assertEquals(1, monthTypes.size());
        assertEquals("INTEGER", monthTypes.get(0));
    }

    @Test
    public void testGetRDFtoOpenBISDataTypeMapWithUnionRange() {
        // Call the method to test
        Map<String, List<String>> dataTypeMap = DatatypeMapper.getRDFtoOpenBISDataTypeMap(model);

        // Validate the union class property
        assertTrue(dataTypeMap.containsKey("https://biomedit.ch/rdf/sphn-schema/sphn#hasValue"));

        // Check the values for the union range
        List<String> unionRangeTypes = dataTypeMap.get("https://biomedit.ch/rdf/sphn-schema/sphn#hasValue");
        assertEquals(2, unionRangeTypes.size());
        assertTrue(unionRangeTypes.contains("REAL"));
        assertTrue(unionRangeTypes.contains("VARCHAR"));
    }

    @Test
    public void testEmptyModel() {
        // Create an empty model
        OntModel emptyModel = ModelFactory.createOntologyModel();

        // Call the method with the empty model
        Map<String, List<String>> dataTypeMap = DatatypeMapper.getRDFtoOpenBISDataTypeMap(emptyModel);

        // Validate that the result is an empty map
        assertNotNull(dataTypeMap);
        assertTrue(dataTypeMap.isEmpty());
    }
}