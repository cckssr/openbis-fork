package ch.ethz.sis.rdf.main.parser;

import ch.ethz.sis.rdf.main.Constants;
import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import ch.ethz.sis.rdf.main.model.rdf.PropertyTupleRDF;
import ch.ethz.sis.rdf.main.model.rdf.ResourceRDF;
import ch.ethz.sis.rdf.main.model.xlsx.SampleObject;
import ch.ethz.sis.rdf.main.model.xlsx.SampleObjectProperty;
import ch.ethz.sis.rdf.main.model.xlsx.SamplePropertyType;
import ch.ethz.sis.rdf.main.model.xlsx.SampleType;
import ch.systemsx.cisd.common.shared.basic.string.StringUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.SplitIRI;
import org.apache.jena.vocabulary.*;
import org.apache.thrift.annotation.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ParserUtils {

    public static boolean isNamedIndividual(Model model, RDFNode cls) {
        return model.listSubjectsWithProperty(RDF.type, cls)
                .filterKeep(subject -> model.contains(subject, RDF.type, OWL2.NamedIndividual))
                .hasNext();
    }

    //---------- RESOURCE PREFIX EXTRACTION ------------------

    public static Map<String, List<SampleObject>> getSampleObjectsGroupedByTypeMap(Model model)
    {
        List<String> sampleObjectPrefixList = getSampleObjectPrefixList(model);

        // Map to store the resources grouped by type
        Map<String, List<SampleObject>> sampleObjectsGroupedByTypeMap = new HashMap<>();

        if(!sampleObjectPrefixList.isEmpty()){
            for (String prefix: sampleObjectPrefixList){
                // Iterate through all statements with rdf:type predicate
                model.listStatements(null, RDF.type, (Resource) null).forEachRemaining(statement -> {
                    Resource subject = statement.getSubject();
                    if (subject.isURIResource() && subject.getURI().startsWith(prefix)) {
                        Resource object = statement.getObject().asResource();
                        String typeURI = object.getURI();
                        String type = object.getLocalName();

                        SampleObject sampleObject =
                                new SampleObject(findIdentifier(subject), typeURI,
                                        type);


                        sampleObjectsGroupedByTypeMap.putIfAbsent(typeURI, new ArrayList<>());
                        sampleObjectsGroupedByTypeMap.get(typeURI).add(sampleObject);

                        model.listStatements(subject, null, (Resource) null)
                                .filterDrop(propStatement -> propStatement.getPredicate().equals(RDF.type))
                                .forEachRemaining(propStatement -> {
                                    Property predicate = propStatement.getPredicate();
                                    RDFNode propObject = propStatement.getObject();

                                    //need to differentiate from resources and primitive objects: https://biomedit.ch/rdf/sphn-resource/Code-SNOMED-CT-419199007 || "2011-07-28T16:16:28+00:00"^^xsd:dateTime
                                    SampleObjectProperty sampleObjectProperty = propObject.isResource() ?
                                            new SampleObjectProperty(predicate.getURI(), predicate.getLocalName(), propObject.asResource().getLocalName(), propObject.asResource().getURI()) :
                                            new SampleObjectProperty(predicate.getURI(), predicate.getLocalName(), propObject.toString(), propObject.toString());

                                    sampleObject.addProperty(sampleObjectProperty);
                                });
                    }
                });
            }
        }
        return sampleObjectsGroupedByTypeMap;
    }

    public static Map<String, List<ResourceRDF>> getResourceMap(Model model)
    {
        // Namespace prefix
        //String prefix = model.getNsPrefixURI(RESOURCE_URI_NS);
        List<String> resourcePrefixes = getSampleObjectPrefixList(model);

        // Map to store the resources grouped by type
        Map<String, List<ResourceRDF>> groupedResources = new HashMap<>();

        if(!resourcePrefixes.isEmpty()){
            for (String prefix: resourcePrefixes){
                // Iterate through all statements with rdf:type predicate
                model.listStatements(null, RDF.type, (Resource) null).forEachRemaining(statement -> {
                    Resource subject = statement.getSubject();
                    if (subject.isURIResource() && subject.getURI().startsWith(prefix)) {

                        var parts = subject.getURI().split("/");


                        // Create a new ResourceRDF object
                        ResourceRDF resource =
                                new ResourceRDF(findIdentifier(subject));

                        // Set the type of the resource
                        Resource type = statement.getObject().asResource();
                        resource.setType(type.getURI());

                        // Add the resource to the appropriate group based on its type
                        groupedResources.putIfAbsent(type.getURI(), new ArrayList<>());
                        groupedResources.get(type.getURI()).add(resource);

                        // Iterate over all properties of the subject
                        model.listStatements(subject, null, (Resource) null)
                                .filterDrop(statement1 -> statement1.getPredicate().equals(RDF.type))
                                .forEachRemaining(propStatement -> {
                                    Property predicate = propStatement.getPredicate();
                                    String objectValue;

                                    // Get the object value
                                    if (propStatement.getObject().isResource()) {
                                        Resource object = propStatement.getObject().asResource();
                                        objectValue = object.getURI();
                                    } else {
                                        objectValue = propStatement.getObject().toString();
                                    }

                                    // Add the predicate and object as a PropertyTuple to the ResourceRDF
                                    resource.properties.add(new PropertyTupleRDF(predicate.getLocalName(), objectValue));
                                });
                    }
                });
            }
        }
        return groupedResources;
    }

    private static String findIdentifier(Resource subject)
    {
        return SplitIRI.localname(subject.getURI());
    }


    public static List<String> getSampleObjectPrefixList(Model model){
        Set<String> resourcePossibleNSs = new HashSet<>();
        resourcePossibleNSs.addAll(getClassResources(model, RDFS.Class));
        resourcePossibleNSs.addAll(getClassResources(model, OWL.Class));
        resourcePossibleNSs.addAll(getSubClassOfResources(model));
        return resourcePossibleNSs.stream().toList();
    }

    public static boolean containsResources(Model model) {
        // need to check for RDFS and OWL classes
        return containsResources(model, RDFS.Class) || containsResources(model, OWL.Class);
    }

    private static boolean containsResources(Model model, RDFNode rdfNode) {
        AtomicBoolean containsResources = new AtomicBoolean(false);
        // usually data are noted as triplet [resource, type, some class]
        // iterate over all Classes
        model.listSubjectsWithProperty(RDF.type, rdfNode).forEachRemaining(cls -> {
            // exclude anonymous classes
            if(!cls.isAnon()) {
                //check if the model contains resources that are usually noted as triplet [resource, type, some class]
                if(model.contains(null, RDF.type, cls)){
                    // exclude NamedIndividual cases that are noted like resources
                    if (!isNamedIndividual(model, cls)) containsResources.set(true);
                    /*if(isNamedIndividual(model, cls)) {
                        System.out.println(cls + " -> is a OWL NamedIndividual");
                    } else {
                        containsResources.set(true);
                    }*/
                }
            }
        });
        return containsResources.get();
    }

    //Can't avoid overlapping with the getSubClassOfResources because all classes are subClassOf SPHNConcept
    private static Set<String> getSubClassOfResources(Model model) {
        Set<String> resourcePossibleNSs = new HashSet<>();
        model.listSubjectsWithProperty(RDF.type, OWL.Class).forEachRemaining(cls -> {
            if (!cls.isAnon()) {
                model.listSubjectsWithProperty(RDFS.subClassOf, cls).forEachRemaining(subClass -> {
                    //System.out.println(subClass + " -> subClassOf -> " + cls);
                    if (model.contains(null, RDF.type, subClass) && !isNamedIndividual(model, subClass)) {
                        //System.out.println(model.listSubjectsWithProperty(RDF.type, subClass).toList());
                        model.listSubjectsWithProperty(RDF.type, subClass).forEachRemaining(subject -> {
                            //System.out.println(subject2.getNameSpace());
                            resourcePossibleNSs.add(subject.getNameSpace());
                        });
                    }
                });
            }
        });
        return resourcePossibleNSs;
    }

    private static Set<String> getClassResources(Model model, RDFNode rdfNode) {
        Set<String> resourcePossibleNSs = new HashSet<>();
        model.listSubjectsWithProperty(RDF.type, rdfNode).forEachRemaining(cls -> {
            if(!cls.isAnon()) {
                if(!isNamedIndividual(model, cls)){
                    //System.out.println(model.listSubjectsWithProperty(RDF.type, cls).toList());
                    model.listSubjectsWithProperty(RDF.type, cls).forEachRemaining(subject -> {
                        //System.out.println(subject.getNameSpace());
                        resourcePossibleNSs.add(subject.getNameSpace());
                    });
                }
            }
        });
        return resourcePossibleNSs;
    }

    //---------- GENERAL INFO ------------------

    public static Map<String, String> getOntologyMetadataMap(Model model) {
        Map<String, String> ontMetadata = new HashMap<>();
        // Extract ontology metadata by checking for owl:Ontology
        ResIterator ontologies = model.listResourcesWithProperty(RDF.type, OWL.Ontology);
        if (ontologies.hasNext()) {
            System.out.println("Ontology metadata found:");
            while (ontologies.hasNext()) {
                Resource ontology = ontologies.nextResource();
                ontMetadata.put(DC.description.getLocalName(), getPropertySafely(ontology, DC.description));
                ontMetadata.put(DC.rights.getLocalName(), getPropertySafely(ontology, DC.rights));
                ontMetadata.put(DC.title.getLocalName(), getPropertySafely(ontology, DC.title));
                ontMetadata.put(DCTerms.bibliographicCitation.getLocalName(), getPropertySafely(ontology, DCTerms.bibliographicCitation));
                ontMetadata.put(DCTerms.license.getLocalName(), getPropertySafely(ontology, DCTerms.license));
                ontMetadata.put(OWL.priorVersion.getLocalName(), getPropertySafely(ontology, OWL.priorVersion));
                ontMetadata.put(OWL2.versionIRI.getLocalName(), getPropertySafely(ontology, OWL2.versionIRI));
            }
        } else {
            System.out.println("No specific ontology metadata found.");
        }
        return ontMetadata;
    }

    private static String getPropertySafely(Resource ontology, Property property){
        return (ontology != null && ontology.getProperty(property) != null) ? ontology.getProperty(property).getObject().toString() : "";
    }

    public static String getVersionIRI(Model model){
        StmtIterator iter = model.listStatements(null, OWL2.versionIRI, (RDFNode) null);
        if (iter.hasNext()) {
            Statement stmt = iter.nextStatement();
            return stmt.getObject().toString();
        }
        return null;
    }

    public static void extractGeneralInfo(Model model, String ontNamespace) {
        System.out.println("General Information: ");
        // Count schema resources
        countSchemaResources(model);

        // Print all RDF types and their counts
        Map<Resource, Integer> rdfTypeCounts = getAllRdfTypeCounts(model);

        // Count resources with types in the specific namespace
        int countNamespaceTypes = countResourcesWithNamespaceType(model, ontNamespace);
        System.out.println("\tTotal Objects with types starting with default namespace <" + ontNamespace + ">: " + countNamespaceTypes);

        // Count subjects with a specific prefix
        List<String> resourcePrefixes = getSampleObjectPrefixList(model);
        for (String prefix: resourcePrefixes){
            int countSubjectsWithPrefix = 0;
            if (prefix != null) {
                countSubjectsWithPrefix = countSubjectsWithPrefix(model, prefix);
            }
            System.out.println("\tTotal resource Subjects with prefix <" + prefix + ">: " + countSubjectsWithPrefix);
        }

        rdfTypeCounts.forEach((type, count) -> System.out.println("\t"+type + ": " + count));
    }

    private static int countResourcesWithNamespaceType(Model model, String namespace) {
        return model.listStatements(null, RDF.type, (Resource) null).filterKeep(statement -> {
            Resource type = statement.getObject().asResource();
            return type.getURI().startsWith(namespace);
        }).toList().size();
    }

    private static int countSubjectsWithPrefix(Model model, String prefix) {
        List<Statement> statements = model.listStatements(null, RDF.type, (Resource) null).filterKeep(statement -> {
            Resource subject = statement.getSubject();
            return subject.isURIResource() && subject.getURI().startsWith(prefix);
        }).toList();
        //statements.forEach(System.out::println);
        return statements.size();
    }

    private static void countSchemaResources(Model model) {
        int rdfsClassCount = model.listResourcesWithProperty(RDF.type, RDFS.Class).filterDrop(RDFNode::isAnon).toList().size();
        int rdfsDatatypeCount = model.listResourcesWithProperty(RDF.type, RDFS.Datatype).toList().size();
        int propertyCount = model.listResourcesWithProperty(RDF.type, RDF.Property).toList().size();
        int owlClassCount = model.listResourcesWithProperty(RDF.type, OWL.Class).filterDrop(RDFNode::isAnon).toList().size();
        int objectPropertyCount = model.listResourcesWithProperty(RDF.type, OWL.ObjectProperty).toList().size();
        int restrictionCount = model.listResourcesWithProperty(RDF.type, OWL.Restriction).toList().size();
        int datatypePropertyCount = model.listResourcesWithProperty(RDF.type, OWL.DatatypeProperty).toList().size();
        int annotationPropertyCount = model.listResourcesWithProperty(RDF.type, OWL.AnnotationProperty).toList().size();
        int namedIndividualCount = model.listResourcesWithProperty(RDF.type, OWL2.NamedIndividual).toList().size();
        int ontologyCount = model.listResourcesWithProperty(RDF.type, OWL.Ontology).toList().size();

        System.out.println("\tTotal RDFS Classes (no anon): " + rdfsClassCount);
        System.out.println("\tTotal RDFS Datatype: " + rdfsDatatypeCount);
        System.out.println("\tTotal RDF Properties: " + propertyCount);
        System.out.println("\tTotal OWL Ontology Metadata: " + ontologyCount);
        System.out.println("\tTotal OWL Classes (no anon): " + owlClassCount);
        System.out.println("\tTotal OWL Object Properties: " + objectPropertyCount);
        System.out.println("\tTotal OWL Datatype Properties: " + datatypePropertyCount);
        System.out.println("\tTotal OWL Annotation Properties: " + annotationPropertyCount);
        System.out.println("\tTotal OWL Named IndividualCount: " + namedIndividualCount);
        System.out.println("\tTotal OWL Restriction: " + restrictionCount);
    }

    private static Map<Resource, Integer> getAllRdfTypeCounts(Model model) {
        // Use a TreeMap with a custom comparator to sort based on resource.toString()
        Map<Resource, Integer> rdfTypeCounts = new TreeMap<>(Comparator.comparing(Resource::getURI));

        // List all rdf:type statements
        model.listStatements(null, RDF.type, (Resource) null).forEachRemaining(statement -> {
            Resource type = statement.getObject().asResource();
            // Exclude anonymous classes and value sets or subsets
            if (!type.isAnon() && !isValueSetOrSubset(type)) {
                rdfTypeCounts.put(type, rdfTypeCounts.getOrDefault(type, 0) + 1);
            }
        });

        return rdfTypeCounts;
    }

    private static boolean isValueSetOrSubset(Resource resource) {
        // Logic to determine if a resource is a value set or subset based on specific properties or classes
        return resource.hasProperty(RDF.type, OWL.Restriction) || resource.hasProperty(RDF.type, OWL.Class)
                && (resource.hasProperty(OWL.unionOf) || resource.hasProperty(OWL.intersectionOf) || resource.hasProperty(OWL.allValuesFrom));
    }

    public static ResourceParsingResult removeObjectsOfUnknownType(ModelRDF modelRDF,
            Map<String, List<SampleObject>> sampleObjectsGroupedByTypeMap, Map<String, List<String>>
            additionalChains,@Nullable OntModel additionModel)
    {
        Set<String> knownObjects = new LinkedHashSet<>();
        Set<String> unknownObjects = new LinkedHashSet<>();

        Map<String, List<SampleObject>> unknownTypeSampleObjects =
                sampleObjectsGroupedByTypeMap.values().stream()
                        .flatMap(Collection::stream)
                        .filter(x -> !canResolveSampleType(modelRDF, x.typeURI, additionalChains) || StringUtils.isBlank(x.type))
                        .collect( Collectors.groupingBy( x -> x.typeURI));
                ;



        Map<String, List<SampleObject>> objectsKnownTypes =
                sampleObjectsGroupedByTypeMap.values().stream()
                        .flatMap(Collection::stream)
                        .filter(x -> canResolveSampleType(modelRDF, x.typeURI, additionalChains))
                        .filter(x -> !StringUtils.isBlank(x.type))
                        .collect( Collectors.groupingBy( x -> x.typeURI));
        ;
        Set<String> importedTypes = objectsKnownTypes.keySet().stream().filter(x -> additionalChains.containsKey(x))
                .map( x -> additionalChains.get(x))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());



        List<String> classesToImport = new ArrayList<>();
        List<String> propertiesToImport = new ArrayList<>();


        List<SampleObject> objects =
                unknownTypeSampleObjects.entrySet().stream().map(x -> x.getValue())
                        .flatMap(Collection::stream).toList();
        List<SampleObject> objectsWritten =
                objectsKnownTypes.entrySet().stream().map(x -> x.getValue())
                        .flatMap(Collection::stream).toList();
        Set<String> deletedCodes = objects.stream().map(x -> x.code).collect(Collectors.toSet());

        Map<String, SamplePropertyType> codeToPropertyType = modelRDF.sampleTypeList.stream().map(x -> x.properties)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toMap(x -> x.code, Function.identity()));

        List<SampleObject> changedObjects = new ArrayList<>();
        List<SampleObject> unchangedObjects = new ArrayList<>();

        for (SampleObject object : objectsWritten)
        {
            List<SampleObjectProperty> tempProperties = new ArrayList<>();

            boolean change = false;
            for (var property : object.properties)
            {
                if (deletedCodes.contains(property.getValue()))
                {

                    change = true;
                    SampleType sampleType = modelRDF.sampleTypeList.stream().filter(x -> x.properties.stream().anyMatch(y -> y.ontologyAnnotationId.equals(property.propertyURI))).findFirst().get();
                    String code = "138875005";
                    sampleType.properties.stream()
                            .filter(x -> x.ontologyAnnotationId.equals(property.propertyURI))
                            .filter(x -> x.code.toLowerCase().contains(code))
                            .findFirst()
                            .ifPresent(x -> {
                                String value = extractValue(x, additionModel, property);

                                SampleObjectProperty sampleObjectProperty = new SampleObjectProperty(property.propertyURI, x.propertyLabel, value, property.valueURI);
                                tempProperties.add(sampleObjectProperty);
                            });


                    boolean required =
                            codeToPropertyType.get(property.label.toUpperCase()).isMandatory == 1;
                    if (required){
                        SampleObjectProperty dummyProperty = new SampleObjectProperty(property.propertyURI , Constants.UNKNOWN, property.value, property.valueURI);
                        tempProperties.add(dummyProperty);
                    }
                } else
                {
                    tempProperties.add(property);
                }



            }
            if (change)
            {
                changedObjects.add(object);
                object.properties = tempProperties;

            } else
            {
                unchangedObjects.add(object);
            }

        }

        return new ResourceParsingResult(objects, unchangedObjects, changedObjects, importedTypes, List.of());
    }

    static String extractValue(SamplePropertyType samplePropertyType, OntModel additionalOntModel,
            SampleObjectProperty sampleObjectProperty)
    {
        if (additionalOntModel == null)
        {
            return sampleObjectProperty.getValue();
        }
        Optional<String> maybeValue = Optional.ofNullable(samplePropertyType.metadata.get("SomeValuesFromRestriction"))
                .map( x -> additionalOntModel.getOntClass(x))
                .map(x -> x.getProperty(RDFS.label))
                .map(Statement::getObject)
                .map(RDFNode::toString);
        if (maybeValue.isPresent()){
            return maybeValue.get();
        }


        return sampleObjectProperty.getValue();
    }

    private static boolean canResolveSampleType(ModelRDF modelRDF, String sampleType, Map<String, List<String>> additionalTypes){
        if (StringUtils.isBlank(sampleType)){
            return false;
        }

        Optional<SampleType> typeFound = modelRDF.sampleTypeList.stream().filter(x -> x.code.equals(sampleType)).findFirst();
        if (typeFound.isPresent()){
            if (StringUtils.isBlank(typeFound.get().code) ){
                return false;
            }


            return true;
        }

        if (additionalTypes.keySet().contains(sampleType)){
            return true;
        }

        List<String> typeFoundChain = modelRDF.subClassChanisMap.get(sampleType);
        if (typeFoundChain==null){
            return false;
        }


        return typeFoundChain.contains(sampleType);



    }

}
