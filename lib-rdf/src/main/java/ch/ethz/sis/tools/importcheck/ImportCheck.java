package ch.ethz.sis.tools.importcheck;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.rdf.main.Config;
import ch.ethz.sis.rdf.main.mappers.openBis.RdfToOpenBisMapper;
import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import ch.ethz.sis.rdf.main.parser.RDFReader;
import ch.ethz.sis.tools.importcheck.helper.Connection;
import org.apache.commons.cli.*;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static ch.ethz.sis.rdf.main.RDFCommandLine.INPUT_PATHS;

public class ImportCheck
{

    private static final String helperCommand =
            "java -jar import-check.java -f <file1> -f <file2> -u <url> -t <token> -s <space> -p <project> -n <num_samples>";

    public static void main(String[] args)
    {
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        Options options = createOptions();
        CommandLine cmd = null;

        try
        {
            cmd = parser.parse(options, args);
        } catch (ParseException ex)
        {
            formatter.printHelp(helperCommand, options);
            ex.printStackTrace();
        }

        Config.setConfig(true, false, false);

        if (Arrays.stream(args).anyMatch(x -> x.equals("--help") || x.equals("-h")))
        {
            formatter.printHelp(helperCommand, options);

        }

        String[] inputFilePaths = cmd.getOptionValues(INPUT_PATHS);
        boolean verbose = false;
        String inputFormatValue = "TTL";

        OntModel additionalModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        String url = cmd.getOptionValue('u');
        String token = cmd.getOptionValue('t');

        OpenBIS openBIS = new OpenBIS(url);
        openBIS.setSessionToken(token);
        openBIS.isSessionActive();

        String space = cmd.getOptionValue('s');
        String project = cmd.getOptionValue('p');
        String projectIdentifier = "/" + space + "/" + project;

        int numSamples = Integer.parseInt(cmd.getOptionValue('n'));

        RDFReader rdfReader = new RDFReader();
        ModelRDF modelRDF =
                rdfReader.read(inputFilePaths, inputFormatValue, verbose, additionalModel);
        OpenBisModel openBisModel = RdfToOpenBisMapper.convert(modelRDF,
                projectIdentifier);

        List<Sample> samples =
                openBisModel.getEntities().values().stream().filter(x -> x instanceof Sample)
                        .map(Sample.class::cast)
                        .collect(Collectors.toList());

        Random random = new Random();

        List<Sample> toCheck = new ArrayList<>();
        for (int i = 0; i < numSamples; i++)
        {
            int randomIndex = random.nextInt(samples.size());
            toCheck.add(samples.get(randomIndex));

        }
        Connection.CheckResult checkResult = Connection.check(openBIS, toCheck);

        for (Sample missingSample : checkResult.getMissing())
        {
            System.out.println(String.format("Missing resource \"%s\"", missingSample.getCode()));
        }


    }

    private static Options createOptions()
    {
        Options options = new Options();

        Option url = new Option("u", "URL of openBIS instance", true, "openBIS instance token");
        url.setRequired(true);
        options.addOption(url);

        Option token = new Option("t", "token", true, "openBIS instance token");
        token.setRequired(true);
        options.addOption(token);

        Option project = new Option("s", "space", true, "openBIS space code");

        options.addOption(project);

        Option space = new Option("p", "project", true, "openBIS project code");
        space.setRequired(true);
        options.addOption(space);

        Option numSamples =
                new Option("n", "num-samples", true, "Number of random Samples to check");
        numSamples.setRequired(true);
        options.addOption(numSamples);

        Option regression = new Option("r", "regression", true,
                "Look for specific samples to deal with regression stuff");
        regression.setRequired(false);
        options.addOption(regression);


        Option help = new Option("h", "help", false, "Display this help message");
        options.addOption(help);

        Option filePaths = new Option("f", INPUT_PATHS, true,
                "Path to input files, has to be at least one. All files have to be of the same format.");
        filePaths.setArgs(Option.UNLIMITED_VALUES);
        filePaths.setRequired(true);
        options.addOption(filePaths);

        return options;
    }

}
