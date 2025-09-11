package ch.ethz.sis.rdf.main;

import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import ch.ethz.sis.rdf.main.model.xlsx.SampleObject;
import ch.ethz.sis.rdf.main.parser.RDFReader;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

public class CardinalityTest
{
    public final String inputFormatValue = "TTL";

    private final String input_schema =
            "src/test/resources/rdf_schema_sphn_dataset_release_2024_2.ttl";

    private final String input_file_too_many =
            "src/test/resources/cardinalities/swisspeddw_mock_data_too_many_entries.ttl";

    private final String input_file_okay =
            "src/test/resources/cardinalities/swisspeddw_mock_data_okay_resources.ttl";


    @Test
    public void testTooManyCardinalities()
    {
        Config.setConfig(false, false, false);
        RDFReader rdfReader = new RDFReader();
        OntModel ontModel = ModelFactory.createOntologyModel();

        // Dummied out because the conversion should not explode anymore. Reporting is still in place
        /*Assert.assertThrows(UserFailureException.class,
                () -> rdfReader.read(new String[] { input_schema, input_file_too_many },
                        inputFormatValue, false, ontModel));

         */
    }

    @Test
    public void testOkay()
    {
        Config.setConfig(false, false, false);
        RDFReader rdfReader = new RDFReader();
        OntModel ontModel = ModelFactory.createOntologyModel();
        ModelRDF modelRDF = rdfReader.read(new String[] { input_schema, input_file_okay },
                inputFormatValue, false, ontModel);// did not see anything assertDoesNotThrow()
        SampleObject sampleObject =
                modelRDF.sampleObjectsGroupedByTypeMap.values().stream().flatMap(Collection::stream)
                        .findFirst().get();
        Assert.assertEquals(2, sampleObject.getProperties().size());

    }

}
