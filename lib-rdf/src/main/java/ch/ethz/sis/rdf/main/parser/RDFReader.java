package ch.ethz.sis.rdf.main.parser;

import ch.ethz.sis.rdf.main.ClassCollector;
import ch.ethz.sis.rdf.main.mappers.AdditionalVocabularyMapper;
import ch.ethz.sis.rdf.main.mappers.DatatypeMapper;
import ch.ethz.sis.rdf.main.mappers.NamedIndividualMapper;
import ch.ethz.sis.rdf.main.mappers.ObjectPropertyMapper;
import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import ch.ethz.sis.rdf.main.model.rdf.OntClassExtension;
import ch.ethz.sis.rdf.main.model.xlsx.*;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RDFReader
{
    public ModelRDF read(String[] inputFileNames, String inputFormatValue, OntModel additionalModel)
    {
        return read(inputFileNames, inputFormatValue, false, additionalModel);
    }

    public ModelRDF read(String[] inputFileNames, String inputFormatValue, boolean verbose,
            OntModel additionalModel)
    {
        Model model = ModelFactory.createDefaultModel();

        for (String inputFileName : inputFileNames)
        {

            Model partialModel = LoaderRDF.loadModel(inputFileName, inputFormatValue);
            model.add(partialModel);
        }

        ;
        ModelRDF modelRDF = initializeModelRDF(model);

        handleSubclassChains(model, modelRDF);
        handleOntologyModel(model, inputFileNames, inputFormatValue, modelRDF);
        ResourceParsingResult resourceParsingResult =  handleResources(model, modelRDF, additionalModel);
        printResourceParsingResult(resourceParsingResult);


        if (verbose) ParserUtils.extractGeneralInfo(model, model.getNsPrefixURI(""));

        return modelRDF;
    }

    private ModelRDF initializeModelRDF(Model model)
    {
        ModelRDF modelRDF = new ModelRDF();
        modelRDF.ontNamespace = model.getNsPrefixURI("");
        modelRDF.ontVersion = ParserUtils.getVersionIRI(model);
        modelRDF.ontMetadata = ParserUtils.getOntologyMetadataMap(model);
        modelRDF.nsPrefixes = model.getNsPrefixMap();
        modelRDF.vocabularyTypeList = NamedIndividualMapper.getVocabularyTypeList(model);
        modelRDF.vocabularyTypeListGroupedByType = NamedIndividualMapper.getVocabularyTypeListGroupedByType(model);
        return modelRDF;
    }

    private void handleSubclassChains(Model model, ModelRDF modelRDF)
    {
        modelRDF.subClassChanisMap = getSubclassChainsEndingWithClass(model, model.listStatements(null, RDFS.subClassOf, (RDFNode) null));
    }

    private void handleOntologyModel(Model model, String[] inputFileNames, String inputFormatValue,
            ModelRDF modelRDF)
    {
        if (canCreateOntModel(model)) {
            OntModel ontModel = LoaderRDF.loadOntModel(inputFileNames, inputFormatValue);
            processOntologyModel(ontModel, modelRDF);
        } else {
            modelRDF.sampleTypeList = new ArrayList<>();
            modelRDF.stringOntClassExtensionMap = new HashMap<>();
        }
    }

    private void processOntologyModel(OntModel ontModel, ModelRDF modelRDF)
    {


        Map<String, List<String>> RDFtoOpenBISDataTypeMap = DatatypeMapper.getRDFtoOpenBISDataTypeMap(ontModel);
        //modelRDF.RDFtoOpenBISDataType = RDFtoOpenBISDataTypeMap;
        Map<String, List<String>> objectPropToOntClassMap = ObjectPropertyMapper.getObjectPropToOntClassMap(ontModel);
        //modelRDF.objectPropertyMap = objectPropToOntClassMap;
        Map<String, OntClassExtension> ontClass2OntClassExtensionMap = ClassCollector.getOntClass2OntClassExtensionMap(ontModel);
        modelRDF.stringOntClassExtensionMap = ontClass2OntClassExtensionMap;
        Set<String> vocabUnionTypes = handleVocabularyUnion(ontModel, "https://biomedit.ch/rdf/sphn-schema/sphn#Terminology", modelRDF);


        List<SampleType> sampleTypeList = ClassCollector.getSampleTypeList(ontModel, ontClass2OntClassExtensionMap, vocabUnionTypes);

        sampleTypeList.removeIf(sampleType -> modelRDF.vocabularyTypeListGroupedByType.containsKey(sampleType.code));
        restrictionsToSampleMetadata(sampleTypeList, ontClass2OntClassExtensionMap);
        verifyPropertyTypes(sampleTypeList, RDFtoOpenBISDataTypeMap, objectPropToOntClassMap, modelRDF.vocabularyTypeListGroupedByType, modelRDF.stringOntClassExtensionMap);

        modelRDF.sampleTypeList = sampleTypeList; //ClassCollector.getSampleTypeList(ontModel);
    }

    private Set<String> handleVocabularyUnion(OntModel ontModel, String vocabTypeUri, ModelRDF modelRDF){
        Set<String> vocabUris = new HashSet<>();
        ontModel.listStatements().forEach(x -> {
            boolean subClass = x.getPredicate().equals(RDFS.subClassOf);
            var isVocabulary = x.getObject().canAs(OntClass.class) && vocabTypeUri.equals(x.getObject().as(OntClass.class).getURI());
            if (subClass && isVocabulary){
                vocabUris.add(x.getSubject().getURI());
            }

        });

        List<VocabularyType> vocabularyTypeList = new ArrayList<>(modelRDF.vocabularyTypeList);
        vocabUris.forEach(x -> {
            VocabularyTypeOption vocabularyTypeOption = new VocabularyTypeOption("DUMMY", "dummy", "dummy");
            String[] parts = x.split("/");
            String code = parts[parts.length - 1];

            List<VocabularyTypeOption> vocabularyTypeOptions = List.of(vocabularyTypeOption);
            VocabularyType vocabularyType =
                    new VocabularyType(code, "description", x, vocabularyTypeOptions);

            vocabularyTypeList.add(vocabularyType);
        });
        modelRDF.vocabularyTypeList = vocabularyTypeList;

        return vocabUris;



    }

    private ResourceParsingResult handleResources(Model model, ModelRDF modelRDF, OntModel additionalOntModel)
    {
        boolean modelContainsResources = ParserUtils.containsResources(model);
        System.out.println("Model contains Resources ? " + (modelContainsResources ? "YES" : "NO"));
        Map<String, List<String>>
                additionalChains = getSubclassChainsEndingWithClass(additionalOntModel,
                additionalOntModel.listStatements(null, RDFS.subClassOf, (RDFNode) null));


        modelRDF.resourcesGroupedByType =
                modelContainsResources ? ParserUtils.getResourceMap(model) : Collections.emptyMap();

        Map<String, List<SampleObject>> sampleObjectsGroupedByTypeMap =
                modelContainsResources ?
                        ParserUtils.getSampleObjectsGroupedByTypeMap(model) :
                        Collections.emptyMap();

        List<String> sampleObjectMapKeyList =
                sampleObjectsGroupedByTypeMap.keySet().stream().toList();
        Map<String, String> sampleTypeUriToCodeMap = modelRDF.sampleTypeList.stream()
                .collect(Collectors.toMap(
                        sampleType -> sampleType.ontologyAnnotationId,
                        sampleType -> sampleType.code
                ));

        modelRDF.sampleObjectsGroupedByTypeMap =
                checkForNotSampleTypeInSampleObjectMap(sampleObjectMapKeyList,
                        sampleTypeUriToCodeMap, sampleObjectsGroupedByTypeMap,
                        modelRDF.subClassChanisMap);
        ResourceParsingResult resourceParsingResult =
                ParserUtils.removeObjectsOfUnknownType(modelRDF, sampleObjectsGroupedByTypeMap, additionalChains, additionalOntModel);

        AdditionalVocabularyMapper.AdditionalVocabularyStuff vocabTypes = AdditionalVocabularyMapper.findVocabularyTypes(resourceParsingResult, additionalOntModel, Set.of("http://snomed.info/id/138875005"));
        List<VocabularyType> tempVocabTypes = new ArrayList<>();
        tempVocabTypes.addAll(modelRDF.vocabularyTypeList);

        modelRDF.vocabularyTypeList = tempVocabTypes;

        Map<String, OntClassExtension> ontClass2OntClassExtensionMap = ClassCollector.getOntClass2OntClassExtensionMap(additionalOntModel);




        List<SampleType> sampleTypeList = ClassCollector.getSampleTypeList(additionalOntModel, ontClass2OntClassExtensionMap, List.of())
                .stream().filter(sampleType ->  resourceParsingResult.getClassesImported().contains(sampleType.ontologyAnnotationId))
                .filter(x -> vocabTypes.getVocabAnnotationIds().contains(x.ontologyAnnotationId)).toList();
        modelRDF.sampleTypeList.addAll(sampleTypeList);




        modelRDF.sampleObjectsGroupedByTypeMap = Stream.concat( resourceParsingResult.getUnchangedObjects()
                .stream(), resourceParsingResult.getEditedObjects().stream() ).collect(Collectors.groupingBy(x -> x.type));
        return resourceParsingResult;

    }





    private Map<String, List<SampleObject>> checkForNotSampleTypeInSampleObjectMap(List<String> sampleObjectMapKeyList,
            Map<String, String> sampleTypeUriToCodeMap,
            Map<String, List<SampleObject>> sampleObjectsGroupedByTypeMap,
            Map<String, List<String>> chainsMap)
    {
        List<String> notSampleTypeKeyList = new ArrayList<>();
        for (String key : sampleObjectMapKeyList)
        {
            if (sampleTypeUriToCodeMap.containsKey(key))
            {
                sampleObjectsGroupedByTypeMap.put(sampleTypeUriToCodeMap.get(key), sampleObjectsGroupedByTypeMap.get(key));
                sampleObjectsGroupedByTypeMap.remove(key);
            } else
            {
                notSampleTypeKeyList.add(key);
            }
        }

        for (String key : notSampleTypeKeyList)
        {
            if (chainsMap.containsKey(key))
            {
                //System.out.println("CHAIN: " + key + " -> " + chainsMap.get(key));
                // CHAIN: http://snomed.info/id/138875005 -> [http://snomed.info/id/138875005, https://biomedit.ch/rdf/sphn-schema/sphn#Terminology]
                // store the new key, that should be a sample type, as a code instead as URI
                String newKeyURI = sampleTypeUriToCodeMap.get(chainsMap.get(key).get(1));
                // change sampleObject type from ext unknown to new key type
                sampleObjectsGroupedByTypeMap.get(key).forEach(sampleObject -> sampleObject.type = newKeyURI);

                // Append the old key list to the new key list
                sampleObjectsGroupedByTypeMap.merge(newKeyURI, sampleObjectsGroupedByTypeMap.get(key),
                        (oldList, newList) -> {
                            if (newList == null)
                            {
                                return oldList; // If the new list is null, return the old list
                            }
                            if (oldList != null)
                            {
                                oldList.addAll(newList); // Combine the old list and new list
                            } else
                            {
                                oldList = newList; // If the old list is null, use the new list
                            }
                            return oldList;
                        }
                );

                // Remove the old key
                sampleObjectsGroupedByTypeMap.remove(key);
            }
        }
        return sampleObjectsGroupedByTypeMap;
    }

    //TODO: there is no direct connection from hasComparator to Comparator, from prop to vocabulary type
    void verifyPropertyTypes(List<SampleType> sampleTypeList, Map<String, List<String>> RDFtoOpenBISDataTypeMap,
            Map<String, List<String>> objectPropToOntClassMap, Map<String, List<VocabularyType>> vocabularyTypeListGroupedByTypeMap, Map<String, OntClassExtension> ontClassExtensionMap)
    {
        for(SampleType sampleType: sampleTypeList)
        {
            //System.out.println("SAMPLE - VACAB_TYPE: "+ sampleType.code + " -> " + sampleType.ontologyAnnotationId + " -> " + vocabularyTypeListGroupedByTypeMap.get(sampleType.code));
            for(SamplePropertyType samplePropertyType: sampleType.properties)
            {
                if (!Objects.equals(samplePropertyType.dataType, "SAMPLE"))
                {

                    var restrictions = findSomeValueRestrictions(ontClassExtensionMap, samplePropertyType.code);
                    var restrictionCodes = restrictions.stream()
                            .filter(Objects::nonNull)
                            .map(x -> getCodeFromSphnUri(x)).toList();

                    if (vocabularyTypeListGroupedByTypeMap.containsKey(samplePropertyType.code) || restrictionCodes.stream().anyMatch(
                            vocabularyTypeListGroupedByTypeMap::containsKey) || vocabularyTypeListGroupedByTypeMap.keySet().stream().anyMatch(key -> samplePropertyType.code.contains(key)))
                    {
                        //System.out.println("GET: "+ vocabularyTypeListGroupedByTypeMap.keySet().stream().filter(key -> samplePropertyType.code.contains(key)).findFirst().orElseGet(null));
                        samplePropertyType.dataType = "CONTROLLEDVOCABULARY";
                        samplePropertyType.vocabularyCode = vocabularyTypeListGroupedByTypeMap.keySet().stream().filter(key -> samplePropertyType.code.contains(key)).findFirst().orElseGet(() -> {
                            return restrictionCodes.stream().filter(
                                    vocabularyTypeListGroupedByTypeMap::containsKey).findFirst().orElse("UNKNOWN");
                        });
                        //System.out.println("  VACAB_TYPE: "+ samplePropertyType.dataType + " -> " + samplePropertyType.code + " -> " + vocabularyTypeListGroupedByTypeMap.get(samplePropertyType.code));
                    } else if (RDFtoOpenBISDataTypeMap.get(samplePropertyType.ontologyAnnotationId) != null)
                    {
                        samplePropertyType.dataType = RDFtoOpenBISDataTypeMap.get(samplePropertyType.ontologyAnnotationId).get(0);
                        //System.out.println("    DATATYPE: "+ samplePropertyType.dataType + " -> " + samplePropertyType.ontologyAnnotationId + " -> " + RDFtoOpenBISDataTypeMap.get(samplePropertyType.ontologyAnnotationId).get(0));

                    } else if (objectPropToOntClassMap.get(samplePropertyType.ontologyAnnotationId) != null && !samplePropertyType.dataType.equals("VARCHAR") )
                    {
                        samplePropertyType.dataType = "SAMPLE"+ ":" + objectPropToOntClassMap.get(samplePropertyType.ontologyAnnotationId).get(0);
                        //System.out.println(" OBJECT_PROP: "+ samplePropertyType.dataType + " -> " + samplePropertyType.ontologyAnnotationId + " -> " + objectPropToOntClassMap.get(samplePropertyType.ontologyAnnotationId).get(0));
                    }
                    //System.out.println("  VACAB_TYPE: "+ samplePropertyType.dataType + " -> " + samplePropertyType.code + " -> " + vocabularyTypeListGroupedByTypeMap.get(samplePropertyType.code));
                }
            }
        }
    }



    private Set<String> findSomeValueRestrictions(Map<String, OntClassExtension> ontClassExtensionMap, String code){
        return ontClassExtensionMap.values()
                .stream()
                .flatMap(x -> x.restrictions.values().stream())
                .flatMap(Collection::stream)
                .filter(x -> x.getOnProperty().getURI().toLowerCase().contains(code.toLowerCase()))
                .filter(x -> {
                    try {
                        x.asSomeValuesFromRestriction();
                        return true;
                    }
                    catch (RuntimeException e ){
                        return false;
                    }

                })
                .map(x -> x.asSomeValuesFromRestriction())
                .map(x -> x.getSomeValuesFrom().getURI())
                .collect(Collectors.toSet());
    }

    private String getCodeFromSphnUri(String uri){
       return uri.replaceAll("https://biomedit.ch/rdf/sphn-schema/sphn#", "").toUpperCase(Locale.ROOT);
    }



    private void restrictionsToSampleMetadata(List<SampleType> sampleTypeList, Map<String, OntClassExtension> ontClass2OntClassExtensionMap)
    {
        for(SampleType sampleType: sampleTypeList)
        {
            Map<String, Map<String, String>> sampleMetadata = new HashMap<>();
            OntClassExtension ontClassExtension = ontClass2OntClassExtensionMap.get(sampleType.ontologyAnnotationId);
            Map<String, List<Restriction>> restrictionsMap = ontClassExtension.restrictions;
            for(SamplePropertyType samplePropertyType: sampleType.properties)
            {
                Map<String, String> propertyMetadata = new HashMap<>();
                List<Restriction> propertyTypeRestrictionList = restrictionsMap.get(samplePropertyType.ontologyAnnotationId);
                if (propertyTypeRestrictionList != null)
                {
                    for(Restriction restriction: propertyTypeRestrictionList){
                        hanldeRestriction(restriction, propertyMetadata, ontClassExtension);
                    }
                    samplePropertyType.metadata.putAll(propertyMetadata);
                    samplePropertyType.setMultiValue(checkMultiValue(propertyMetadata));
                    //samplePropertyType.setMandatory(checkMandatory(propertyMetadata));
                }
                sampleMetadata.put(samplePropertyType.code, propertyMetadata);
            }
            sampleType.metadata = sampleMetadata;
        }
    }

    private void hanldeRestriction(Restriction restriction, Map<String, String> propertyMetadata, OntClassExtension ontClassExtension)
    {
        if (restriction.isCardinalityRestriction())
        {
            propertyMetadata.put("CardinalityRestriction", String.valueOf(restriction.asCardinalityRestriction().getCardinality()));
        } else if (restriction.isMinCardinalityRestriction())
        {
            propertyMetadata.put("MinCardinalityRestriction", String.valueOf(restriction.asMinCardinalityRestriction().getMinCardinality()));
        } else if (restriction.isMaxCardinalityRestriction())
        {
            propertyMetadata.put("MaxCardinalityRestriction", String.valueOf(restriction.asMaxCardinalityRestriction().getMaxCardinality()));
        } else if (restriction.isSomeValuesFromRestriction())
        {
            handleSomeValuesFromRestriction(restriction, propertyMetadata, ontClassExtension);
        } else
        {
            propertyMetadata.put("UNHANDLED_Restriction", restriction.toString());
        }
    }

    private void handleSomeValuesFromRestriction(Restriction restriction, Map<String, String> propertyMetadata, OntClassExtension ontClassExtension)
    {
        RDFNode someValuesFrom = restriction.asSomeValuesFromRestriction().getSomeValuesFrom();
        if (someValuesFrom.isURIResource()) {
            propertyMetadata.put("SomeValuesFromRestriction", someValuesFrom.asResource().getURI());
        } else if (someValuesFrom.isAnon() && someValuesFrom.canAs(OntClass.class)){
            OntClass anonClass = someValuesFrom.as(OntClass.class);
            // Recursively handle the anonymous class, be it union, intersection, etc.
            if (anonClass.isUnionClass()) {
                UnionClass unionClass = anonClass.asUnionClass();
                propertyMetadata.put("SomeValuesFromRestriction", ontClassExtension.unions.get(unionClass).toString());
            }
        }
    }

    private static boolean checkMultiValue(Map<String, String> propertyMetadata)
    {
        return propertyMetadata.containsKey("MinCardinalityRestriction") && !propertyMetadata.containsKey("MaxCardinalityRestriction");
    }

    private static int checkMandatory(Map<String, String> propertyMetadata)
    {
        return (Objects.equals(propertyMetadata.get("MinCardinalityRestriction"), "1") &&
                Objects.equals(propertyMetadata.get("MaxCardinalityRestriction"), "1")) ? 1 : 0;
    }

    private boolean canCreateOntModel(Model model)
    {
        // Count RDFS Classes
        int rdfsClassCount = model.listSubjectsWithProperty(RDF.type, RDFS.Class).filterDrop(RDFNode::isAnon).toList().size();
        // Count OWL Classes
        int owlClassCount = model.listSubjectsWithProperty(RDF.type, OWL.Class).filterDrop(RDFNode::isAnon).toList().size();

        System.out.println("Total RDFS Classes: " + rdfsClassCount);
        System.out.println("Total OWL Classes: " + owlClassCount);
        //Create an OntModel only if there are no RDFS classes
        return (rdfsClassCount == 0 && owlClassCount > 0);
    }

    Map<String, List<String>> getSubclassChainsEndingWithClass(Model model, StmtIterator iter)
    {
        // Clear previous chains
        Map<String, List<String>> chainsMap = new HashMap<>();

        // Process each statement
        while (iter.hasNext()) {
            Statement stmt = iter.nextStatement();
            Resource subclassRes = stmt.getSubject();
            String subclass = subclassRes.isURIResource() ? subclassRes.getURI() : "Anonymous Node";

            if (subclassRes.isURIResource()) {
                Resource superclass = stmt.getObject().asResource();

                Set<String> visited = new HashSet<>();
                List<String> chain = new ArrayList<>();
                chain.add(subclass);

                if (findSubclassChain(model, superclass, visited, chain)) {
                    chainsMap.put(subclass, new ArrayList<>(chain));
                }
            } else {
                System.out.println("Skipping anonymous subclass: " + subclass);
            }
        }
        return chainsMap;
    }

    private boolean findSubclassChain(Model model, Resource superclass, Set<String> visited, List<String> chain)
    {
        if (superclass == null || !superclass.isURIResource()) {
            return false;
        }

        String superclassURI = superclass.getURI();
        if (!visited.add(superclassURI)) {
            // Already visited this class, no valid chain here
            return false;
        }

        // Add superclass to chain
        chain.add(superclass.getURI());

        // Check if the superclass is of type owl:Class or rdfs:Class or owl:DatatypeProperty or owl:DatatypeProperty
        if (model.contains(superclass, RDF.type, OWL.Class)
                || model.contains(superclass, RDF.type, RDFS.Class)
                || model.contains(superclass, RDF.type, OWL2.Class)
                || model.contains(superclass, RDF.type, OWL.DatatypeProperty)
                || model.contains(superclass, RDF.type, OWL.ObjectProperty)) {
            return true;
        }

        // Recur for each superclass
        StmtIterator superIter = model.listStatements(superclass, RDFS.subClassOf, (RDFNode) null);
        while (superIter.hasNext()) {
            Statement stmt = superIter.nextStatement();
            Resource nextSuper = stmt.getObject().asResource();
            if (findSubclassChain(model, nextSuper, visited, chain)) {
                return true;
            }
        }

        // Remove the last element added if no valid chain is found
        chain.remove(chain.size() - 1);
        return false;
    }

    private void printResourceParsingResult(ResourceParsingResult resourceParsingResult){
        if (resourceParsingResult.getDeletedObjects().isEmpty()){
            return;
        }
        System.out.println("There were resources whose types could not be resolved");
        resourceParsingResult.getDeletedObjects().forEach(x -> System.out.println(x.code));

        if (resourceParsingResult.getEditedObjects().isEmpty()){
            return;
        }
        System.out.println("------------------------------");
        System.out.println("The resources were referenced in the following objects, these references are now deleted");
        resourceParsingResult.getEditedObjects().forEach(x -> System.out.println(x.code));



    }
}
