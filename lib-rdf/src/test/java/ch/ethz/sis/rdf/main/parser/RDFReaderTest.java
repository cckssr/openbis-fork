package ch.ethz.sis.rdf.main.parser;

import ch.ethz.sis.rdf.main.ClassCollector;
import ch.ethz.sis.rdf.main.mappers.NamedIndividualMapper;
import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import ch.ethz.sis.rdf.main.model.xlsx.SamplePropertyType;
import ch.ethz.sis.rdf.main.model.xlsx.SampleType;
import ch.ethz.sis.rdf.main.model.xlsx.VocabularyType;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;
public class RDFReaderTest {

    public final String inputFileName = "src/test/resources/sphn_test_reader.ttl";
    public final String inputFormatValue = "TTL";
    private RDFReader rdfReader;
    private Model model;
    private OntModel ontModel;

    @Before
    public void setup() {
        rdfReader = new RDFReader();
        model = LoaderRDF.loadModel(inputFileName, inputFormatValue);
        ontModel = LoaderRDF.loadOntModel(new String[] { inputFileName }, inputFormatValue);
    }

    @Test
    public void testReadBasicRDFModel() {
        OntModel ontModel1 = ModelFactory.createOntologyModel();
        ModelRDF modelRDF =
                rdfReader.read(new String[] { inputFileName }, inputFormatValue, false, ontModel1);

        assertNotNull(modelRDF);
        assertEquals("https://biomedit.ch/rdf/sphn-schema/sphn#", modelRDF.ontNamespace);
        assertEquals("https://biomedit.ch/rdf/sphn-schema/sphn/2024/2", modelRDF.ontVersion);
    }

    @Test
    public void testSubclassChainsExtraction() {
        Map<String, List<String>> chains = rdfReader.getSubclassChainsEndingWithClass(model, model.listStatements(null, RDFS.subClassOf, (RDFNode) null));
        assertNotNull(chains);
        assertTrue(chains.containsKey("https://biomedit.ch/rdf/sphn-schema/sphn#Code"));

        List<String> chain = chains.get("https://biomedit.ch/rdf/sphn-resource/ucum/Torr");
        assertEquals(2, chain.size());
        assertEquals("https://biomedit.ch/rdf/sphn-resource/ucum/Torr", chain.get(0));
        assertEquals("https://biomedit.ch/rdf/sphn-schema/sphn#Terminology", chain.get(1));
    }

    @Test
    public void testVocabularyTypeListExtraction() {
        List<VocabularyType> vocabList = NamedIndividualMapper.getVocabularyTypeList(ontModel);
        assertNotNull(vocabList);
        assertEquals(1, vocabList.size());

        VocabularyType vocabularyType = new VocabularyType("COMPARATOR", "", new ArrayList<>());
        assertTrue(vocabList.contains(vocabularyType));
    }

    @Test
    public void testSampleTypeProcessing() {
        List<SampleType> sampleTypeList = ClassCollector.getSampleTypeList(ontModel, Map.of(),
                Set.of(), new ModelRDF());
        sampleTypeList.forEach(System.out::println);
        assertNotNull(sampleTypeList);
        assertEquals(6, sampleTypeList.size());
        SampleType sampleType1 = new SampleType("Allergen", "https://biomedit.ch/rdf/sphn-schema/sphn#Allergen");
        assertTrue(sampleTypeList.contains(sampleType1));
        SampleType sampleType2 = new SampleType("Code", "https://biomedit.ch/rdf/sphn-schema/sphn#Code");
        assertTrue(sampleTypeList.contains(sampleType2));
    }

    @Test
    public void testPropertyTypeValidation() {
        List<SampleType> sampleTypes = new ArrayList<>();
        SampleType sampleType = new SampleType("Allergen", "https://biomedit.ch/rdf/sphn-schema/sphn#Allergen");
        SamplePropertyType prop = new SamplePropertyType("PropCode", "PropAnnotation");
        sampleType.properties.add(prop);
        sampleTypes.add(sampleType);

        Map<String, List<String>> RDFtoOpenBISDataTypeMap = new HashMap<>();
        RDFtoOpenBISDataTypeMap.put("PropAnnotation", List.of("STRING"));

        rdfReader.verifyPropertyTypes(sampleTypes, RDFtoOpenBISDataTypeMap, new HashMap<>(), new HashMap<>(), Map.of());

        assertEquals("STRING", sampleType.properties.get(0).dataType);
    }

    @Test
    public void testInvalidModelFormat() {
        String invalidFileName = "invalid.ttl";
        String invalidFormat = "UNKNOWN";
        OntModel addiionalModel = ModelFactory.createOntologyModel();

        assertThrows(Exception.class,
                () -> rdfReader.read(new String[] { invalidFileName }, invalidFormat, false,
                        addiionalModel));
    }
}