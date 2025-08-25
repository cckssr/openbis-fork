package ch.ethz.sis.rdf.main.mappers.rdf;

import ch.ethz.sis.rdf.main.model.xlsx.VocabularyType;
import ch.ethz.sis.rdf.main.model.xlsx.VocabularyTypeOption;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SPHN project uses the property owl:NamedIndividual as enum
 * other project might use this in a different way
 **/
public class NamedIndividualMapper
{
    public static List<VocabularyType> getVocabularyTypeList(Model model)
    {
        Map<String, List<VocabularyType>> groupedByCode = getVocabularyTypeListGroupedByType(model);

        List<VocabularyType> mergedList = groupedByCode.entrySet().stream().map(entry -> {
            String code = getVocabEntryCode(entry.getKey());
            List<VocabularyTypeOption> mergedOptions = new ArrayList<>();

            entry.getValue().forEach(vocabularyType -> mergedOptions.addAll(vocabularyType.getOptions()));

            String description = entry.getValue().get(0).getDescription();

            return new VocabularyType(code, description, mergedOptions);
        }).toList();

        //mergedList.forEach(System.out::println);

        return mergedList;
    }

    public static String getVocabEntryCode(String key)
    {
        return key.length() > 50 ? key.substring(0, 49) : key;
    }

    public static Map<String, List<VocabularyType>> getVocabularyTypeListGroupedByType(Model model)
    {
        List<VocabularyType> vocabularyTypeList = new ArrayList<>();
        model.listSubjectsWithProperty(RDF.type, OWL2.NamedIndividual).forEachRemaining(subject -> {
            processIndividual(model, subject, vocabularyTypeList);
        });
        return vocabularyTypeList.stream().collect(Collectors.groupingBy(VocabularyType::getCode));
    }

    private static void processIndividual(Model model, Resource subject, List<VocabularyType> vocabularyTypeList)
    {
        try {
            String optionLabel = model.getProperty(subject, RDFS.label).getString();
            //TODO check with Juan for increasing vocabulary type code length > 50
            String optionCode = subject.getLocalName().length() > 50 ? subject.getLocalName().substring(0, 49).toLowerCase(Locale.ROOT) : subject.getLocalName().toUpperCase(Locale.ROOT);
            VocabularyTypeOption option = new VocabularyTypeOption(
                    optionCode,
                    optionLabel,
                    subject.getURI());

            model.listObjectsOfProperty(subject, RDF.type)
                    .filterKeep(rdfNode -> rdfNode.canAs(Resource.class))
                    .filterDrop(rdfNode -> rdfNode.asResource().equals(OWL2.NamedIndividual.asResource()))
                    .forEach(rdfNode -> {
                        VocabularyType vocabularyType = createVocabularyType(model, (Resource) rdfNode, option);
                        vocabularyTypeList.add(vocabularyType);
                    });
        } catch (Exception e) {
            System.out.println("Error processing OWL2.NamedIndividual: " + subject + " | Error: " + e.getMessage());
        }
    }

    private static VocabularyType createVocabularyType(Model model, Resource resource, VocabularyTypeOption option)
    {
        String description = getPropertySafely(model, resource, SKOS.definition, "");
        String subClassOf = getPropertySafely(model, resource, RDFS.subClassOf, resource.toString());

        return new VocabularyType(
                resource.getLocalName().toUpperCase(Locale.ROOT),
                description + "\n" + resource.getURI() + "\n" + subClassOf,
                resource.getURI(),
                new ArrayList<>(Collections.singletonList(option))
        );
    }

    private static String getPropertySafely(Model model, Resource resource, Property property, String defaultValue)
    {
        Statement statement = model.getProperty(resource, property);
        return statement != null ? statement.getObject().toString() : defaultValue;
    }
}

