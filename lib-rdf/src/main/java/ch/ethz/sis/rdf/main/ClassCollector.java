package ch.ethz.sis.rdf.main;

import ch.ethz.sis.rdf.main.mappers.rdf.DatatypeMapper;
import ch.ethz.sis.rdf.main.model.rdf.AdditionalProperty;
import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import ch.ethz.sis.rdf.main.model.rdf.OntClassExtension;
import ch.ethz.sis.rdf.main.model.rdf.PropertyTupleRDF;
import ch.ethz.sis.rdf.main.model.xlsx.SamplePropertyType;
import ch.ethz.sis.rdf.main.model.xlsx.SampleType;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ch.ethz.sis.openbis.generic.asapi.v3.dto.event.EntityType.SAMPLE;
import static ch.ethz.sis.rdf.main.Constants.COLON;
import static ch.ethz.sis.rdf.main.Constants.UNKNOWN;

public class ClassCollector {

    public final Map<String, List<String>> RDFtoOpenBISDataType;
    public Map<String, OntClassExtension> ontClass2OntClassExtensionMap;

    public ClassCollector(final OntModel ontModel)
    {
        this.RDFtoOpenBISDataType = DatatypeMapper.getRDFtoOpenBISDataTypeMap(ontModel);
        this.ontClass2OntClassExtensionMap = getOntClass2OntClassExtensionMap(ontModel);
    }

    private static void parseRestriction(Restriction restriction, OntClassExtension ontClassObject)
    {
        try {
            if (restriction.isCardinalityRestriction()) {
                addRestrictionSafely(ontClassObject, restriction.getOnProperty(), restriction.asCardinalityRestriction());
            } else if (restriction.isMinCardinalityRestriction()) {
                addRestrictionSafely(ontClassObject, restriction.getOnProperty(), restriction.asMinCardinalityRestriction());
            } else if (restriction.isMaxCardinalityRestriction()) {
                addRestrictionSafely(ontClassObject, restriction.getOnProperty(), restriction.asMaxCardinalityRestriction());
            } else if (restriction.isSomeValuesFromRestriction()) {
                SomeValuesFromRestriction svfRestriction = restriction.asSomeValuesFromRestriction();
                addRestrictionSafely(ontClassObject, restriction.getOnProperty(), svfRestriction);
                parseSomeValuesFromRestriction(svfRestriction, ontClassObject);
            } else if (restriction.isHasValueRestriction()) {
                addRestrictionSafely(ontClassObject, restriction.getOnProperty(), restriction.asHasValueRestriction());
            } else if (restriction.isAllValuesFromRestriction()) {
                addRestrictionSafely(ontClassObject, restriction.getOnProperty(), restriction.asAllValuesFromRestriction());
            } else {
                throw new ConversionException("Unknown restriction type: " + restriction.getClass().getName());
            }
        } catch (ConversionException e) {
            System.err.println("ConversionException: " + e.getMessage());
            if (e.getMessage().contains("Cannot convert node http://purl.org/dc/terms/conformsTo to OntProperty")){
                handleAdditionalProperty(ontClassObject, restriction, "conformsTo", "VARCHAR", DCTerms.conformsTo.getURI());
            }


        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        }
    }

    private static void handleAdditionalProperty(OntClassExtension ontClassObject, Restriction restriction, String name, String dataType, String uri
    ){
        String code = name.toUpperCase(Locale.ROOT);

        AdditionalProperty additionalProperty = Optional.ofNullable(ontClassObject.hackyProperties.get(code
    )).orElse(new AdditionalProperty(code
    ));
        additionalProperty.setDataType(dataType);
        additionalProperty.setProperty(name);


        if (restriction.isMaxCardinalityRestriction()){
            additionalProperty.setMax(restriction.asMaxCardinalityRestriction().getMaxCardinality());
        }
        if (restriction.isMinCardinalityRestriction()){
            additionalProperty.setMin(restriction.asMinCardinalityRestriction().getMinCardinality());
        }
        if (additionalProperty.getMin() == 1 && additionalProperty.getMax() == 1){
            additionalProperty.setMandatory(1);
            additionalProperty.setMultiValued(0);
        } else {
            additionalProperty.setMandatory(0);
            additionalProperty.setMultiValued(1);
        }

        additionalProperty.setUri(uri);
        additionalProperty.setDescription(name);




        ontClassObject.hackyProperties.put(name
    , additionalProperty);




    }

    private static void addRestrictionSafely(OntClassExtension ontClassObject, Property onProperty, Restriction restriction)
    {
        if (onProperty.canAs(OntProperty.class)) {
            //ontClassObject.addRestriction(onProperty.as(OntProperty.class), restriction);
            ontClassObject.addRestriction(onProperty.getURI(), restriction);
        } else {
            System.err.println("Cannot convert node " + onProperty + " to OntProperty");
        }
    }

    private static void parseSomeValuesFromRestriction(SomeValuesFromRestriction svfRestriction, OntClassExtension ontClassObject)
    {
        RDFNode someValuesFrom = svfRestriction.getSomeValuesFrom();
        if (someValuesFrom.isAnon()) { // Directly handle anonymous cases
            OntClass someValuesClass = someValuesFrom.as(OntClass.class);
            // Recursively handle the anonymous class, be it union, intersection, etc.
            parseAnonymousClass(someValuesClass, ontClassObject);
        } else if (someValuesFrom.isURIResource()) {
            // Here, you handle URIResource cases, possibly adding them directly as restrictions
            //ontClassObject.addRestriction(svfRestriction.getOnProperty(), svfRestriction.asSomeValuesFromRestriction());
            ontClassObject.addRestriction(svfRestriction.getOnProperty().getURI(), svfRestriction.asSomeValuesFromRestriction());
            //System.out.println("     - Class URI Resource: " + someValuesFrom.asResource().getURI());
        } else {
            //System.out.println("     - Non-Class URI Resource (skipped): " + someValuesFrom.asResource().getURI());
            throw new ConversionException("Unknown some values from restriction type: " + svfRestriction.getClass().getName());
        }
    }

    private static void parseIntersection(IntersectionClass intersectionClass, OntClassExtension ontClassObject)
    {
        intersectionClass.listOperands().forEachRemaining(operand -> {
            if (operand.isAnon()) {
                // Now, handle different types of anonymous superclasses
                parseAnonymousClass(operand, ontClassObject);
            } else {
                //System.out.println("Found UNKWON operand [" + operand + "] in intersection: " + intersectionClass);
                throw new ConversionException("Unknown intersection operand: " + operand.getClass().getName());
            }
        });
    }

    private static void parseUnion(UnionClass unionClass, OntClassExtension ontClassObject)
    {
        List<String> operands = new ArrayList<>();
        for(int i=0; i<unionClass.getOperands().size(); i++) {
            //System.out.println(unionClass.getOperands().get(i).as(Restriction.class));
            RDFNode item = unionClass.getOperands().get(i);
            // Check if the current item can be a Restriction and process it
            if (item.canAs(OntClass.class)) {
                OntClass ontClass = item.as(OntClass.class);
                if (item.isAnon()) {
                    //System.out.println("Item at index " + i + " is Anon. " + item);
                    parseAnonymousClass(ontClass, ontClassObject);
                }
                // Now, you have OntClass restriction, and you can process it
                //System.out.println("     - Found OntClass operand ["+ontClass+"] at index " + i + " in union: "+unionClass);
                operands.add(ontClass.getURI());
            } else  {
                //System.out.println("     - Found not-OntClass operand [" + item + "] at index " +i+ " in union: " + unionClass);
                operands.add(item.toString());
            }
        }
        ontClassObject.addUnion(unionClass, operands);
    }

    private static void parseAnonymousClass(OntClass anonClass, OntClassExtension ontClassObject)
    {
        //TODO implement missing classes
        if (anonClass.isRestriction()) {
            Restriction restriction = anonClass.asRestriction();
            //System.out.println("- Restriction: " + restriction);
            parseRestriction(restriction, ontClassObject);
        } else if (anonClass.isUnionClass()) {
            UnionClass unionClass = anonClass.asUnionClass();
            //System.out.println("- Union Of: " + unionClass.getOperands().size());
            parseUnion(unionClass, ontClassObject);
        } else if (anonClass.isIntersectionClass()) {
            IntersectionClass intersectionClass = anonClass.asIntersectionClass();
            //System.out.println("- Intersection Of: " + intersectionClass);
            parseIntersection(intersectionClass, ontClassObject);
        } else if (anonClass.isComplementClass()) {
            ComplementClass complementClass = anonClass.asComplementClass();
            //System.out.println("- Complemented Of: " + complementClass);
            throw new ConversionException("Complement class [" + complementClass + "] is not implemented!");
        } else if (anonClass.isEnumeratedClass()) {
            EnumeratedClass enumeratedClass = anonClass.asEnumeratedClass();
            //System.out.println("- Enumerated: " + enumeratedClass);
            throw new ConversionException("Enumerated class [" + enumeratedClass + "] is not implemented!");
        }else {
            //System.out.println("- Anonymous Superclass (Complex Class Expression): " + anonClass);
            throw new ConversionException("Anonymous Superclass (Complex Class Expression) ["+anonClass+"] is not implemented!");
            // Handle other complex expressions as needed
        }
    }

    private static void ontClassPropertiesParser(OntModel model, OntClass cls, OntClassExtension ontClassExtension, Map<String, List<String>> mappedDataTypes)
    {
        if (!cls.isAnon())
        { // Exclude anonymous classes for intersectionOf
            // Find all properties where the class is the domain
            StmtIterator propIterator = model.listStatements(null, RDFS.domain, (RDFNode) null);
            while (propIterator.hasNext())
            {
                Statement stmt = propIterator.nextStatement();
                Property prop = stmt.getSubject().as(Property.class);
                Resource domain = stmt.getObject().asResource();

                if (isClassInDomain(cls, domain))
                {
                    // Find the range of the property
                    StmtIterator rangeIterator = model.listStatements(prop, RDFS.range, (RDFNode) null);
                    while (rangeIterator.hasNext()) {
                        Statement rangeStmt = rangeIterator.nextStatement();
                        Resource range = rangeStmt.getObject().asResource();
                        if (range.canAs(UnionClass.class))
                        {
                            // If the range is a union class, process each operand
                            //System.out.println("Class [" + prop.getURI() + "] might be of multiple types: " + range.as(UnionClass.class));
                            ontClassExtension.propertyTuples.add(new PropertyTupleRDF(prop.getURI(), SAMPLE.name()));
                            // If the range is a union class, process each operand
                            // To add multi ranged properties like hasCode [CODE] and hasCode [TERMINOLOGY]
                            UnionClass unionClass = range.as(UnionClass.class);
                            List<String> operands = new ArrayList<>();
                            for(int i=0; i<unionClass.getOperands().size(); i++)
                            {
                                //System.out.println(unionClass.getOperands().get(i).as(Restriction.class));
                                RDFNode item = unionClass.getOperands().get(i);
                                if (item.canAs(OntClass.class))
                                {
                                    OntClass ontClass = item.as(OntClass.class);
                                    if (item.isAnon())
                                    {
                                        System.out.println("Item at index " + i + " is Anon. " + item);
                                    }
                                    // Now, you have OntClass restriction, and you can process it
                                    //System.out.println("     - Found OntClass operand ["+ontClass+"] at index " + i + " in union: "+unionClass);
                                    operands.add(ontClass.getURI());
                                } else
                                {
                                    //System.out.println("     - Found not-OntClass operand [" + item + "] at index " +i+ " in union: " + unionClass);
                                    operands.add(item.toString());
                                }
                            }
                            //System.out.println("Class <" + prop.getURI() + "> might be of multiple types: " + operands);
                        } else if (range.isURIResource())
                        {
                            // If the range is a single URI resource
                            String defaultSampleType = SAMPLE.name() + COLON + range.getLocalName().toUpperCase(Locale.ROOT);
                            String mappedType = mappedDataTypes.getOrDefault(prop.getURI(), List.of(defaultSampleType)).get(0);
                            //System.out.println("range is a single URI resource: " + prop.getURI() + " -> " + mappedType);
                            ontClassExtension.propertyTuples.add(new PropertyTupleRDF(prop.getURI(), mappedType));
                        } else
                        {
                            ontClassExtension.propertyTuples.add(new PropertyTupleRDF(prop.getURI(), UNKNOWN));
                        }
                    }
                }
            }
        }
    }

    private static List<String> getUnionClassOperands(final UnionClass unionClass)
    {
        List<String> operands = new ArrayList<>();
        for(int i=0; i<unionClass.getOperands().size(); i++)
        {
            RDFNode item = unionClass.getOperands().get(i);
            if (item.canAs(OntClass.class))
            {
                OntClass ontClass = item.as(OntClass.class);
                operands.add(ontClass.getURI());
            } else
            {
                operands.add(item.toString());
            }
        }
        return operands;
    }

    private static boolean isClassInDomain(OntClass cls, Resource domain)
    {
        if (domain.equals(cls)) {
            return true;
        } else if (domain.canAs(UnionClass.class)) {
            UnionClass unionClass = domain.as(UnionClass.class);
            return unionClass.listOperands().toList().contains(cls);
        }
        return false;
    }

    private static Map<OntClass, OntClassExtension> getInitOntClass2OntClassExtension(final OntModel ontModel)
    {
        Map<OntClass, OntClassExtension> ontClass2OntClassExtensionMap = new HashMap<>();

        ontModel.listClasses().forEachRemaining(cls -> {
            if (!cls.isAnon()) { // Esclude anonymous classes for intersectionOf
                ontClass2OntClassExtensionMap.put(cls, new OntClassExtension(cls));
            }
        });

        return ontClass2OntClassExtensionMap;
    }
    
    private static Map<String, OntClassExtension> toOntClass2StringOntClassExtension(
            final Map<OntClass, OntClassExtension> ontClass2OntClassExtension)
    {
        Map<String, OntClassExtension> ontClass2StringOntClassExtension = new HashMap<>();
        ontClass2OntClassExtension.forEach((key, value) -> ontClass2StringOntClassExtension.put(key.getURI(), value));

        return ontClass2StringOntClassExtension;    
    }
    
    private static void ontClassesParser(OntClass cls, OntClassExtension ontClassExtension)
    {
        cls.listSuperClasses().forEachRemaining((superClass) -> {
            try {
                // Check if the superclass is an anonymous class
                if (superClass.isAnon()) {
                    // Now, handle different types of anonymous superclasses
                    parseAnonymousClass(superClass.as(OntClass.class), ontClassExtension);
                } else {
                    // Check if the superClass can be cast to OntClass
                    if (superClass.canAs(OntClass.class)) {
                        OntClass superOntClass = superClass.as(OntClass.class);
                        //System.out.println("* Not-Anon superClass: " + superOntClass);
                        ontClassExtension.setSuperClass(superOntClass);
                    } else {
                        System.err.println("Cannot convert node " + superClass + " to OntClass");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error handling superClass " + superClass + ": " + e.getMessage());
            }
        });

    }


    public static Map<String, OntClassExtension> getOntClass2OntClassExtensionMap(final OntModel ontModel)
    {
        Map<OntClass, OntClassExtension> ontClass2OntClassExtensionMap = getInitOntClass2OntClassExtension(ontModel);
        final Map<String, List<String>> RDFtoOpenBISDataType = DatatypeMapper.getRDFtoOpenBISDataTypeMap(ontModel);

        ontClass2OntClassExtensionMap.forEach((cls, ontClassExtension) -> {
            ontClassPropertiesParser(ontModel, cls, ontClassExtension, RDFtoOpenBISDataType);
            //System.out.println("*** Class: " + cls);
            ontClassesParser(cls, ontClassExtension);
        });

        return toOntClass2StringOntClassExtension(ontClass2OntClassExtensionMap);
    }

    private static List<SamplePropertyType> getPropertyTypeList(final OntModel ontModel, final OntClass currentCls, Collection<String> generalVocabularyTypes)
    {
        List<SamplePropertyType> propertyTypeList = new ArrayList<>();
        // Find all properties where the class is the domain
        StmtIterator propIterator = ontModel.listStatements(null, RDFS.domain, (RDFNode) null);
        while (propIterator.hasNext())
        {
            Statement stmt = propIterator.nextStatement();
            Property prop = stmt.getSubject().as(Property.class);
            Resource domain = stmt.getObject().asResource();

            if (isClassInDomain(currentCls, domain))
            {
                SamplePropertyType propertyType = new SamplePropertyType(prop.getLocalName(), prop.getURI());
                // Find the range of the property
                StmtIterator rangeIterator = ontModel.listStatements(prop, RDFS.range, (RDFNode) null);
                while (rangeIterator.hasNext())
                {
                    Statement rangeStmt = rangeIterator.nextStatement();
                    Resource range = rangeStmt.getObject().asResource();
                    if (range.canAs(UnionClass.class))
                    {
                        var unionOperands = getUnionClassOperands(range.as(UnionClass.class));
                        if (isUnionWithPrimitiveTypes(unionOperands)){
                            propertyType.dataType = "VARCHAR";
                        } else {
                            propertyType.dataType = "SAMPLE";
                        }
                        boolean vocabTypesFromUnion = isUnionWithVocabularyTypes(unionOperands, Set.of("https://biomedit.ch/rdf/sphn-schema/sphn#Terminology"),  ontModel);

                        if (vocabTypesFromUnion)
                        {
                            propertyType.metadata.put("VOCABULARY_UNION", "It's a union!");
                            propertyType.dataType = "VARCHAR";

                            propertyType.metadata.put("TYPE",
                                    "The type was a union of " + unionOperands.stream().collect(
                                            Collectors.joining(", ")));

                        }

                        propertyType.metadata.put("UNION_TYPE", getUnionClassOperands(range.as(UnionClass.class)).toString());
                    } else if (range.isURIResource())
                    {
                        propertyType.dataType = range.getURI(); //range.getLocalName().toUpperCase(Locale.ROOT);
                    } else
                    {
                        propertyType.dataType = "UNKNOWN";
                    }
                }

                propertyTypeList.add(propertyType);
            }
        }

        return propertyTypeList;
    }

    static boolean isUnionWithPrimitiveTypes(Collection<String> unionOperands){
        Set<String> primitiveTypes = Set.of(
                "http://www.w3.org/2001/XMLSchema#string",
                "http://www.w3.org/2001/XMLSchema#double",
                "http://www.w3.org/2001/XMLSchema#int",
                "http://www.w3.org/2001/XMLSchema#boolean"

        );
        return unionOperands.stream().anyMatch(primitiveTypes::contains);



    }

    static boolean isUnionWithVocabularyTypes(Collection<String> unionOperands, Collection<String> vocabularyTypes, OntModel ontModel){
        Set<String> vocabTypes = new HashSet<>();
        for (String operand : unionOperands){
            OntClass ontClass = ontModel.getOntClass(operand);
            while (ontClass != null && ontClass.getSuperClass() != null)
            {
                if (vocabularyTypes.contains(ontClass.getURI())){
                    return true;
                }
                ontClass = ontClass.getSuperClass();
            }

        }
        return false;

    }

    public static List<SampleType> getSampleTypeList(final OntModel ontModel,
            Map<String, OntClassExtension> ontClassExtensionMap,
            Collection<String> generalVocabTypes, ModelRDF modelRDF)
    {
        List<SampleType> sampleTypeList = new ArrayList<>();

        ontModel.listClasses()
                .filterDrop(RDFNode::isAnon)
                .forEach(ontClass -> {

                    SampleType sampleType = new SampleType(ontClass);
                    sampleType.properties = getPropertyTypeList(ontModel, ontClass, generalVocabTypes);
                    OntClassExtension extension = ontClassExtensionMap.get(sampleType.ontologyAnnotationId);
                    if (extension != null)
                    {
                        extension.getHackyProperties().values().forEach(x -> {
                                    SamplePropertyType hackySampleType =
                                            new SamplePropertyType(x.getProperty(), x.getUri());
                                    hackySampleType.ontologyAnnotationId = x.getOntologyAnnotationId();
                                    hackySampleType.code = x.getCode();
                                    hackySampleType.isMandatory = x.getMandatory();
                                    hackySampleType.dataType = x.getDataType();
                                    hackySampleType.description = x.getDescription();
                                    hackySampleType.isMandatory = x.getMultiValued();
                                    sampleType.properties.add(hackySampleType);

                                }
                        );
                    }


                    sampleTypeList.add(sampleType);


                });

        Map<String, SampleType> ontologyToSampleType =
                sampleTypeList.stream().filter(x -> x.ontologyAnnotationId != null)
                        .collect(
                                Collectors.toMap(x -> x.ontologyAnnotationId, Function.identity()));

        // resolve properties per chain.

        for (SampleType sampleType : sampleTypeList)
        {
            Optional.ofNullable(modelRDF.subClassChanisMap)
                    .map(x -> x.get(sampleType.ontologyAnnotationId))
                    .ifPresent(subClassChain ->
                            subClassChain.stream().map(ontologyToSampleType::get)
                                    .filter(Objects::nonNull)
                                    .forEach(x -> {
                                        for (SamplePropertyType samplePropertyType : x.properties)
                                        {
                                            if (!sampleType.properties.contains(samplePropertyType))
                                            {
                                                sampleType.properties.add(samplePropertyType);
                                            }
                                        }
                                    }));

        }



        return sampleTypeList;
    }

}
