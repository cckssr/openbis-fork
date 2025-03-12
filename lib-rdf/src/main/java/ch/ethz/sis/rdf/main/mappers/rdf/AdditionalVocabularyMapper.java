package ch.ethz.sis.rdf.main.mappers.rdf;

import ch.ethz.sis.rdf.main.model.xlsx.VocabularyType;
import ch.ethz.sis.rdf.main.model.xlsx.VocabularyTypeOption;
import ch.ethz.sis.rdf.main.parser.ResourceParsingResult;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

import java.util.*;

public class AdditionalVocabularyMapper
{

    public static AdditionalVocabularyStuff findVocabularyTypes(ResourceParsingResult resourceParsingResult, OntModel additionalOntModel, Set<String> baseVocabClassUris){
        Set<String> importedClasses = resourceParsingResult.getClassesImported();
        List<VocabularyType> res = new ArrayList<>();
        Map<String, List<VocabularyTypeOption>> temp = new HashMap<>();
        Set<String> vocabAnnotationIds = new HashSet<>();
        for (String classUri : importedClasses){
            OntClass ontClass = additionalOntModel.getOntClass(classUri);
            String baseClassUri = getBaseVocabClass(ontClass, baseVocabClassUris);
            if (baseClassUri == null)
            {
                continue;
            }
            if (temp.get(baseClassUri) == null){
                temp.put(baseClassUri, new ArrayList<>());
            }
            temp.get(baseClassUri).add(getTypeOption(ontClass));
            vocabAnnotationIds.add(classUri);
        }
        for (Map.Entry<String, List<VocabularyTypeOption>> entry : temp.entrySet()){
            OntClass ontClass = additionalOntModel.getOntClass(entry.getKey());
            String code = getCode(ontClass);
            String description = ontClass.getProperty(RDFS.label).getObject().toString();;
            String label = ontClass.getProperty(SKOS.prefLabel).getObject().toString();

            VocabularyType vocabularyType = new VocabularyType(code, description, ontClass.getURI(), entry.getValue());
            res.add(vocabularyType);


        }



        return new AdditionalVocabularyStuff(res, vocabAnnotationIds);

    }

    private static VocabularyTypeOption getTypeOption(OntClass ontClass){
        String code = getCode(ontClass);
        String description = ontClass.getProperty(RDFS.label).getObject().toString();;
        String label = ontClass.getProperty(SKOS.prefLabel).getObject().toString();
        return new VocabularyTypeOption(code, label, description);
    }

    private static String getCode(OntClass ontClass){
        if (ontClass.getURI().contains("snomed")){
            return "SNOMED-" + ontClass.getURI().replace("http://snomed.info/id/", "");
        }

        return "";



    }


    private static String getBaseVocabClass(OntClass cls, Set<String> baseVocabClassUris){
        if (baseVocabClassUris.contains(cls.getURI())){
            return cls.getURI();
        }
        if (cls.getSuperClass() == null || cls.getSuperClass().getURI() == null){
            return null;
        }
        return getBaseVocabClass(cls.getSuperClass(), baseVocabClassUris);

    }

    public static class  AdditionalVocabularyStuff{
        private final List<VocabularyType> vocabularyTypeList;
        private final Set<String> vocabAnnotationIds;

        public AdditionalVocabularyStuff(List<VocabularyType> vocabularyTypeList,
                Set<String> vocabAnnotationIds)
        {
            this.vocabularyTypeList = vocabularyTypeList;
            this.vocabAnnotationIds = vocabAnnotationIds;
        }

        public List<VocabularyType> getVocabularyTypeList()
        {
            return vocabularyTypeList;
        }

        public Set<String> getVocabAnnotationIds()
        {
            return vocabAnnotationIds;
        }
    }





}
