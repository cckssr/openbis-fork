package ch.ethz.sis.rdf.main.parser.repair;

import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.rdf.main.Config;
import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import ch.ethz.sis.rdf.main.model.xlsx.SampleObject;
import ch.ethz.sis.rdf.main.parser.ParserUtils;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.SplitIRI;
import org.apache.jena.vocabulary.RDF;

import java.util.*;

public class RepairUtil
{
    public static void repairEncodingIssueSamples(Model model, ModelRDF modelRDF)
    {

        Map<SampleObject, String> sampleToType = new LinkedHashMap<>();
        for (Map.Entry<String, List<SampleObject>> entry : modelRDF.sampleObjectsGroupedByTypeMap.entrySet())
        {
            for (SampleObject sampleObject : entry.getValue())
            {
                sampleToType.put(sampleObject, entry.getKey());
            }
        }

        Set<String> includeCodes = new LinkedHashSet<>();
        model.listStatements(null, RDF.type, (Resource) null).forEachRemaining(statement ->
        {

            Resource subject = statement.getSubject();
            if (subject.getURI() == null)
            {
                return;
            }
            if (shouldIncludeBasedOnRepair(model, subject))
            {
                includeCodes.add(OpenBisModel.makeOpenBisCodeCompliant(
                        ParserUtils.getResourceCode(subject)));
            }
        });

        Map<String, List<SampleObject>> editedMap = new LinkedHashMap<>();
        for (Map.Entry<SampleObject, String> entry : sampleToType.entrySet())
        {
            List<SampleObject> vals = editedMap.getOrDefault(entry.getValue(), new ArrayList<>());
            if (includeCodes.contains(entry.getKey().code))
            {
                vals.add(entry.getKey());
                editedMap.put(entry.getValue(), vals);
            }

        }
        modelRDF.sampleObjectsGroupedByTypeMap = editedMap;

    }

    private static boolean shouldIncludeBasedOnRepair(Model model, Resource resource)
    {
        if (!Config.getINSTANCE().isRepair())
        {
            return true;
        }

        String query = """ 
                                PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                SELECT *
                WHERE {
                  $s ?p ?o .
                  FILTER(?p != rdf:type)
                  }
                """;

        ParameterizedSparqlString parameterizedSparqlString = new ParameterizedSparqlString(query);
        parameterizedSparqlString.setIri("s",
                resource.getURI());

        boolean shouldInclude = false;

        ResultSet resultSet =
                QueryExecutionFactory.create(parameterizedSparqlString.asQuery(),
                        model).execSelect();
        while (resultSet.hasNext())
        {
            QuerySolution querySolution = resultSet.next();
            RDFNode o = querySolution.get("o");
            if (!o.isResource() || o.asResource().getURI() == null)
            {
                continue;
            }
            Resource objectResource = o.asResource();
            String objectSplitIri = SplitIRI.localname(objectResource.getURI());
            String objectName = objectResource.getLocalName();

            if (!model.contains(objectResource, RDF.type))
            {
                continue;
            }

            if (!objectSplitIri.equals(objectName))
            {
                shouldInclude = true;
            }

        }

        return shouldInclude;

    }

}
