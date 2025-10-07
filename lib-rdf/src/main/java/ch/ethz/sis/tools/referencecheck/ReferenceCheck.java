package ch.ethz.sis.tools.referencecheck;

import ch.ethz.sis.rdf.main.parser.LoaderRDF;
import org.apache.commons.cli.*;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.SplitIRI;

import java.util.ArrayList;
import java.util.List;

public class ReferenceCheck
{
    public static void main(String[] args) throws ParseException
    {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = parser.parse(options, args);

        String inFile = cmd.getOptionValue('i');
        String outfile = cmd.getOptionValue('o');
        String space = cmd.getOptionValue('s');
        String project = cmd.getOptionValue('p');
        String url = cmd.getOptionValue('u');
        String token = cmd.getOptionValue('t');

        Model model = ModelFactory.createDefaultModel();
        Model partialModel = LoaderRDF.loadModel(inFile, "TTL");
        model.add(partialModel);

        List<StatementToFix> statemenstToFixe = findStuff(model);
        List<OpenBisCheck.CheckResult> checkResults =
                OpenBisCheck.checkResultList(url, token, space, project, statemenstToFixe);
        checkResults.stream().filter(x -> x.found() == false).forEach(System.out::println);

    }

    private static List<StatementToFix> findStuff(Model model)
    {
        List<StatementToFix> res = new ArrayList<>();
        String query = """ 
                                PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                SELECT *
                WHERE {
                  ?s ?p ?o .
                  }
                """;
        ParameterizedSparqlString parameterizedSparqlString =
                new ParameterizedSparqlString(query);

        ResultSet resultSet =
                QueryExecutionFactory.create(parameterizedSparqlString.asQuery(),
                        model).execSelect();
        while (resultSet.hasNext())
        {
            QuerySolution next = resultSet.next();
            RDFNode p = next.get("p");
            RDFNode o = next.get("o");
            if (!o.isResource() || o.asResource().getURI()
                    .startsWith("https://biomedit.ch/rdf/sphn-schema/sphn#"))
            {
                continue;
            }

            o.isResource();
            Resource objectResource = o.asResource();
            objectResource.getURI();

            Resource s = next.getResource("s").asResource();

            String objectSplitIri = SplitIRI.localname(objectResource.getURI());
            String objectName = objectResource.getLocalName();
            String subjectIri = SplitIRI.localname(s.getURI());
            String predicateIri = SplitIRI.localname(p.asResource().getURI());

            if (!objectSplitIri.equals(objectName))
            {
                StatementToFix statementToFix =
                        new StatementToFix(subjectIri, predicateIri, objectSplitIri);
                res.add(statementToFix);
                System.out.println(
                        String.format("Difference found: %s %s (%s | %s)", subjectIri,
                                predicateIri, objectName, objectSplitIri));
            }

        }
        return res;
    }

    public record StatementToFix(String subjectName, String predicateName, String objectName)
    {
        @Override
        public String subjectName()
        {
            return subjectName;
        }

        @Override
        public String predicateName()
        {
            return predicateName;
        }

        @Override
        public String objectName()
        {
            return objectName;
        }
    }

    private static Options createOptions()
    {
        Options options = new Options();

        Option input = Option.builder("i")
                .longOpt("input")
                .hasArgs()
                .numberOfArgs(1)
                .argName("infile")
                .desc("Input format, supported: \n \t- TTL, turtle file (an input ttl file must be provided)")
                .required()
                .build();
        options.addOption(input);

        Option output = Option.builder("o")
                .longOpt("output")
                .hasArgs()
                .numberOfArgs(1)
                .argName("outfile")
                .desc("Output file")
                .required()
                .build();
        options.addOption(output);

        Option space = Option.builder("s")
                .longOpt("input")
                .hasArgs()
                .numberOfArgs(1)
                .argName("space")
                .desc("Space code")
                .required()
                .build();
        options.addOption(space);

        Option project = Option.builder("p")
                .longOpt("output")
                .hasArgs()
                .numberOfArgs(1)
                .argName("project")
                .desc("Project code")
                .required()
                .build();
        options.addOption(project);

        Option url = Option.builder("u")
                .longOpt("input")
                .hasArgs()
                .numberOfArgs(1)
                .argName("space")
                .desc("openBIS URL")
                .required()
                .build();
        options.addOption(url);

        Option token = Option.builder("t")
                .longOpt("output")
                .hasArgs()
                .numberOfArgs(1)
                .argName("project")
                .desc("openBIS token")
                .required()
                .build();
        options.addOption(token);

        Option armed = Option.builder("a")
                .longOpt("output")
                .hasArgs()
                .numberOfArgs(1)
                .argName("project")
                .desc("armed: do it for realsies")
                .build();
        options.addOption(armed);


        return options;
    }

}
