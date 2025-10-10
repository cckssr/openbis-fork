package ch.ethz.sis.rdf.main;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.rdf.main.mappers.openBis.RdfToOpenBisMapper;
import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import ch.ethz.sis.rdf.main.parser.RDFReader;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.util.Map;

public class RepairTest
{

    public static final String HAS_TARGET_LOCATION = "hasTargetLocation";

    public final String DATA_PATH = "src/test/resources/repair/swisspeddw_mock_data_bis_dev.ttl";

    public final String SCHEMA_PATH =
            "src/test/resources/rdf_schema_sphn_dataset_release_2024_2.ttl";

    @Test
    public void testRepairLogic()
    {
        Config.setConfig(true, false, false);
        Config.getINSTANCE().setRepair(true);
        RDFReader rdfReader = new RDFReader();
        OntModel ontModel = ModelFactory.createOntologyModel();
        ModelRDF modelRDF =
                rdfReader.read(new String[] { DATA_PATH, SCHEMA_PATH }, "TTL", false, ontModel);
        OpenBisModel openBisModel = RdfToOpenBisMapper.convert(modelRDF, "/DEFAULT/DEFAULT_DATA");
        AbstractEntityPropertyHolder sample123 = openBisModel.getEntities()
                .get(new SampleIdentifier(
                        "//DEFAULT/DEFAULT_DATA/DEFAULT:CHE-105.834.378-SPHN-HEALTHCAREENCOUNTER-123"));
        AbstractEntityPropertyHolder sample1235 = openBisModel.getEntities()
                .get(new SampleIdentifier(
                        "//DEFAULT/DEFAULT_DATA/DEFAULT:CHE-105.834.378-SPHN-HEALTHCAREENCOUNTER-1235"));
        Map<String, Serializable> properties = sample1235.getProperties();
        Assert.assertTrue(properties.get(HAS_TARGET_LOCATION).toString().contains("_1"));
        Assert.assertTrue(properties.get(HAS_TARGET_LOCATION).toString().contains("NORMAL"));

        Assert.assertTrue(
                sample123.getProperties().get(HAS_TARGET_LOCATION).toString().contains("_1"));

        Assert.assertNull(
                "This value should not be included as it does not have a property with the relevant encoding",
                openBisModel.getEntities().get(new SampleIdentifier(
                        "//DEFAULT/DEFAULT_DATA/DEFAULT:CHE-105.834.378-SPHN-HEALTHCAREENCOUNTER-1234")));

    }

    @Test
    public void testRepairLogicOff()
    {
        Config.setConfig(true, false, false);
        Config.getINSTANCE().setRepair(false);
        RDFReader rdfReader = new RDFReader();
        OntModel ontModel = ModelFactory.createOntologyModel();
        ModelRDF modelRDF =
                rdfReader.read(new String[] { DATA_PATH, SCHEMA_PATH }, "TTL", false, ontModel);
        OpenBisModel openBisModel = RdfToOpenBisMapper.convert(modelRDF, "/DEFAULT/DEFAULT_DATA");
        Assert.assertNotNull(openBisModel);

        Assert.assertNotNull("This value should be included as repair is off",
                openBisModel.getEntities().get(new SampleIdentifier(
                        "//DEFAULT/DEFAULT_DATA/DEFAULT:CHE-105.834.378-SPHN-HEALTHCAREENCOUNTER-1234")));

    }

    @Test
    public void testRepairLogicOffKeepDangling()
    {
        Config.setConfig(false, false, false);
        Config.getINSTANCE().setRepair(false);
        RDFReader rdfReader = new RDFReader();
        OntModel ontModel = ModelFactory.createOntologyModel();
        ModelRDF modelRDF =
                rdfReader.read(new String[] { DATA_PATH, SCHEMA_PATH }, "TTL", false, ontModel);
        OpenBisModel openBisModel = RdfToOpenBisMapper.convert(modelRDF, "/DEFAULT/DEFAULT_DATA");
        Assert.assertNotNull(openBisModel);

        AbstractEntityPropertyHolder sample123 = openBisModel.getEntities()
                .get(new SampleIdentifier(
                        "//DEFAULT/DEFAULT_DATA/DEFAULT:CHE-105.834.378-SPHN-HEALTHCAREENCOUNTER-123"));

        Map<String, Serializable> properties = sample123.getProperties();
        Assert.assertTrue(properties.get(HAS_TARGET_LOCATION).toString().contains("_1"));

        Assert.assertTrue(properties.get(HAS_TARGET_LOCATION).toString().contains("NOT"));

    }

    @Test
    public void testRepairLogicKeepDangling()
    {
        Config.setConfig(false, false, false);
        Config.getINSTANCE().setRepair(true);
        RDFReader rdfReader = new RDFReader();
        OntModel ontModel = ModelFactory.createOntologyModel();
        ModelRDF modelRDF =
                rdfReader.read(new String[] { DATA_PATH, SCHEMA_PATH }, "TTL", false, ontModel);
        OpenBisModel openBisModel = RdfToOpenBisMapper.convert(modelRDF, "/DEFAULT/DEFAULT_DATA");
        Assert.assertNotNull(openBisModel);

        AbstractEntityPropertyHolder sample123 = openBisModel.getEntities()
                .get(new SampleIdentifier(
                        "//DEFAULT/DEFAULT_DATA/DEFAULT:CHE-105.834.378-SPHN-HEALTHCAREENCOUNTER-123"));

        Assert.assertNull("This value should be included as repair is off",
                openBisModel.getEntities().get(new SampleIdentifier(
                        "//DEFAULT/DEFAULT_DATA/DEFAULT:CHE-105.834.378-SPHN-HEALTHCAREENCOUNTER-1234")));

        Map<String, Serializable> properties = sample123.getProperties();
        Assert.assertTrue(properties.get(HAS_TARGET_LOCATION).toString().contains("_1"));

        Assert.assertTrue(properties.get(HAS_TARGET_LOCATION).toString().contains("NOT"));

    }

}
