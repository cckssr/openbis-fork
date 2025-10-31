package ch.ethz.sis.tools;

import ch.ethz.sis.rdf.main.parser.LoaderRDF;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ResourceCount
{

    public static void main(String[] args)
    {

        Set<String> filesToDo = new LinkedHashSet<>();
        File dir = new File(".");
        File[] filesList = dir.listFiles();
        for (File file : filesList)
        {
            if (file.getPath().endsWith(".ttl"))
                filesToDo.add(file.getPath());
        }
        Map<String, Integer> counts = new LinkedHashMap<>();

        int num = 0;
        for (String file : filesToDo)
        {
            System.out.println("Processing file " + num + " of " + filesToDo.size() + ": " + file);
            Model model = ModelFactory.createDefaultModel();
            Model partialModel = LoaderRDF.loadModel(file, "TTL");
            model.add(partialModel);
            String query = """ 
                                    PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                    SELECT *
                    WHERE {
                      ?s a ?o .
                      FILTER(STRSTARTS(STR(?o), "https://biomedit.ch/rdf/sphn-schema"))
                                   
                      }
                    """;
            ResultSet resultSet =
                    QueryExecutionFactory.create(query,
                            model).execSelect();
            while (resultSet.hasNext())
            {
                QuerySolution next = resultSet.next();
                RDFNode s = next.get("s");
                RDFNode o = next.get("o");
                if (!s.isResource())
                {
                    throw new RuntimeException(s + " is not a resource!");
                }

                String uri = s.asResource().getURI();
                Integer val = counts.getOrDefault(uri, 0);
                int newVal = val + 1;
                counts.put(uri, newVal);
            }
            num++;
        }
        System.out.println("Int total found " + counts.size() + " resources");

        System.out.println(
                "Found in multiple files: " + counts.values().stream().filter(x -> x > 1).count());
    }
}
