package ch.ethz.sis.rdf.main.mappers;

import ch.ethz.sis.rdf.main.mappers.rdf.NamedIndividualMapper;
import ch.ethz.sis.rdf.main.model.xlsx.VocabularyType;
import ch.ethz.sis.rdf.main.model.xlsx.VocabularyTypeOption;
import ch.ethz.sis.rdf.main.parser.LoaderRDF;
import org.apache.jena.rdf.model.Model;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NamedIndividualMapperTest
{
    private Model model;

    @Before
    public void setUp(){
        String inputFilePath = "src/test/resources/sphn_test_named_individual.ttl";
        model = LoaderRDF.loadModel(inputFilePath, "TTL");
    }

    @Test
    public void testGetVocabularyTypeList()
    {
        List<VocabularyType> vocabularyTypeList = NamedIndividualMapper.getVocabularyTypeList(model);
        assertEquals(3, vocabularyTypeList.size());

        // Check the first VocabularyType
        VocabularyType vocabularyType1 = vocabularyTypeList.get(0);

        assertEquals("DATAFILE_ENCODING", vocabularyType1.getCode());
        assertTrue(vocabularyType1.getDescription().contains("sphn#DataFile_encoding"));

        assertEquals(5, vocabularyType1.getOptions().size());
        VocabularyTypeOption other = new VocabularyTypeOption("OTHER", "Other", "https://biomedit.ch/rdf/sphn-schema/sphn/individual#Other");
        assertTrue(vocabularyType1.getOptions().contains(other));
    }

    @Test
    public void testGetVocabularyTypeListGroupedByType()
    {
        Map<String, List<VocabularyType>> vocabularyTypeListGroupedByTypeMap = NamedIndividualMapper.getVocabularyTypeListGroupedByType(model);
        assertEquals(3, vocabularyTypeListGroupedByTypeMap.keySet().size());

        assertTrue(vocabularyTypeListGroupedByTypeMap.containsKey("DATAFILE_ENCODING"));
        assertEquals(5, vocabularyTypeListGroupedByTypeMap.get("DATAFILE_ENCODING").size());

        assertTrue(vocabularyTypeListGroupedByTypeMap.containsKey("HASH_ALGORITHM"));
        assertEquals(3, vocabularyTypeListGroupedByTypeMap.get("HASH_ALGORITHM").size());
        List<VocabularyType> hashAlgorithm = vocabularyTypeListGroupedByTypeMap.get("HASH_ALGORITHM");
        VocabularyTypeOption md5 = new VocabularyTypeOption("MD5", "MD5", "https://biomedit.ch/rdf/sphn-schema/sphn/individual#MD5");
        assertTrue(hashAlgorithm.stream().anyMatch(vc -> vc.getOptions().contains(md5)));

        assertTrue(vocabularyTypeListGroupedByTypeMap.containsKey("THERAPEUTICAREA_SPECIALTYNAME"));
        assertEquals(2, vocabularyTypeListGroupedByTypeMap.get("THERAPEUTICAREA_SPECIALTYNAME").size());
    }
}