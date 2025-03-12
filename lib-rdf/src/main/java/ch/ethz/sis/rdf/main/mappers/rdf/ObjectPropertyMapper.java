package ch.ethz.sis.rdf.main.mappers.rdf;

import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.Resource;

import java.util.*;

/**
 * Utility class for mapping object properties to their ranges in an ontology model.
 */
public class ObjectPropertyMapper {

    /**
     * Maps object properties to their respective ranges in the given ontology model.
     *
     * @param model the ontology model to process
     * @return a map where keys are URIs of object properties and values are lists of URIs representing the ranges
     *
     * Example:
     *      https://biomedit.ch/rdf/sphn-schema/sphn#hasOriginLocation --> [https://biomedit.ch/rdf/sphn-schema/sphn#Location]
     *      https://biomedit.ch/rdf/sphn-schema/sphn#hasDrug --> [https://biomedit.ch/rdf/sphn-schema/sphn#Drug]
     */
    public static Map<String, List<String>> getObjectPropToOntClassMap(OntModel model,
            ModelRDF modelRDF)
    {
        Map<String, List<String>> objectPropertyMap = new HashMap<>();

        model.listObjectProperties().forEachRemaining(property -> {
            if (property.isURIResource()) {
                Resource range = property.getRange();
                if (range != null) {
                    List<String> objectPropertyRange = new ArrayList<>();

                    if (range.canAs(UnionClass.class)) {
                        // If the range is a union class, process each operand
                        // rdfs:range [ a owl:Class ;
                        //            owl:unionOf ( sphn:Terminology sphn:Code ) ] ;
                        UnionClass unionRange = range.as(UnionClass.class);
                        unionRange.listOperands().forEachRemaining(operand -> {
                            if (operand.isURIResource()) {
                                //objectPropertyRange.add(operand.getURI());
                                objectPropertyRange.add(operand.getLocalName().toUpperCase(Locale.ROOT));
                            }
                        });
                    } else if (range.isURIResource()) {
                        // If the range is a single URI resource
                        //objectPropertyRange.add(range.getURI());
                        objectPropertyRange.add(range.getLocalName().toUpperCase(Locale.ROOT));
                    }

                    if (!objectPropertyRange.isEmpty()) {
                        objectPropertyMap.put(property.getURI(), objectPropertyRange);
                    }
                }
            }
        });

        return objectPropertyMap;
    }
}
