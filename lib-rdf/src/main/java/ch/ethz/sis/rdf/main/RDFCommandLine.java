package ch.ethz.sis.rdf.main;

import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import ch.ethz.sis.rdf.main.parser.RDFReader;
import ch.ethz.sis.rdf.main.xlsx.write.XLSXWriter;
import org.apache.commons.cli.*;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RDFCommandLine {

    //private static final String openBISURL = "http://localhost:8888";

    private static final String openBISURL = "https://openbis-sis-ci-sprint.ethz.ch";

    private static final String asURL = "http://localhost:8888/openbis/openbis";

    private static final String dssURL = "http://localhost:8889/datastore_server";

    private static final String helperCommand =
            "java -jar lib-rdf-tool.jar -i <TTL> -o <XLSX, OPENBIS, OPENBIS-DEV> -f <TTL input file path> -r [<XLSX output file path>] -a [additional files] -pid <project identifier> [[[-u <username> -p] <openBIS AS URL>] <openBIS DSS URL>]";

    //!!! DEV_MODE is used only for pure dev turn it to FALSE for PRODUCTION !!!
    private static final boolean DEV_MODE = false;
    private static final String OPENBIS_HOME = "/home/mdanaila/Projects/rdf/openbis/";
    private static final String TEMP_OUTPUT_XLSX = OPENBIS_HOME + "lib-rdf/test-data/sphn-data-small/output.xlsx";

    public static final String ADDITIONALFILES = "additionalfiles";

    public static final String INPUT_PATHS = "inputpaths";

    public static final String RESULT_FILE = "resultfile";


    public static void main(String[] args) {

        if (DEV_MODE)
        {
            runTestCases();
        } else {
            Options options = createOptions();


            CommandLineParser parser = new DefaultParser();
            HelpFormatter formatter = new HelpFormatter();
            CommandLine cmd = null;
            if (Arrays.stream(args).anyMatch(x -> x.equals("--help") || x.equals("-h")))
            {
                formatter.printHelp(helperCommand, options);

            }

            try {
                cmd = parser.parse(options, args);
                if (cmd.hasOption("help"))
                {
                    formatter.printHelp(helperCommand, options);
                    return;
                }
                validateCommandLine(cmd);
                executeCommandLine(cmd);
            } catch (ParseException e) {
                System.out.println(e.getMessage());
                formatter.printHelp(helperCommand, options);
                System.exit(1);
            }
        }
    }

    private static void runTestCases()
    {
        final String USERNAME = "admin";
        final String PASSWORD = "changeit";
        final String PROJECT_ID = "/DEFAULT/SPHN";
        //final String PROJECT_ID = "/DEFAULT/PREMISE";
        //final String TTL_FILE_PATH = OPENBIS_HOME + "lib-rdf/test-data/herbie/new_binder_comp_1.0.0.ttl";
        //final String TTL_FILE_PATH = OPENBIS_HOME + "lib-rdf/test-data/link-ml/smallMaterialMLinfo.owl.ttl";
        //final String TTL_FILE_PATH = OPENBIS_HOME + "lib-rdf/test-data/sphn-data/mockdata.ttl";
        //final String TTL_FILE_PATH = OPENBIS_HOME + "lib-rdf/test-data/sphn-data-small/mockdata_allergy.ttl";
        //final String TTL_FILE_PATH = OPENBIS_HOME + "lib-rdf/test-data/sphn-model/rdf_schema_sphn_dataset_release_2024_2.ttl";
        final String TTL_FILE_PATH = OPENBIS_HOME + "lib-rdf/test-data/sphn-model/rdf_schema_sphn_dataset_release_2024_2_with_data.ttl";
        //final String TTL_FILE_PATH = OPENBIS_HOME + "lib-rdf/test-data/sphn-model/sphn_rdf_schema_2023_2.ttl";
        //final String TTL_FILE_PATH = OPENBIS_HOME + "lib-rdf/test-data/sphn-model/sphn_rdf_schema_2023_2_with_data.ttl";

        //handleXlsxOutput("TTL", TTL_FILE_PATH,
        //        TEMP_OUTPUT_XLSX,
        //        PROJECT_ID,
        //        false);

        //handleOpenBISOutput("TTL", TTL_FILE_PATH,
        //        openBISURL,
        //        USERNAME, PASSWORD,
        //        PROJECT_ID,
        //        false);

        handleOpenBISDevOutput("TTL", new String[] { TTL_FILE_PATH },
                asURL, dssURL,
                USERNAME, PASSWORD,
                PROJECT_ID,
                false, List.of("/home/meiandr/Downloads/snomed-ct-ch-20231201.ttl/snomed-ct-CH-20231201.ttl"));
    }

    //TODO: add flag -d for dependecies list of files or zip
    //TODO: change -i to take and process a list of files or zip
    private static Options createOptions()
    {
        Options options = new Options();

        Option input = Option.builder("i")
                .longOpt("input")
                .hasArgs()
                .numberOfArgs(1)
                .argName("format")
                .desc("Input format, supported: \n \t- TTL, turtle file (an input ttl file must be provided)")
                .required()
                .build();
        options.addOption(input);

        Option output = Option.builder("o")
                .longOpt("output")
                .hasArgs()
                .numberOfArgs(1)
                .argName("format")
                .desc("Output option, supported: \n \t- XLSX, return a XLSX file (an output XLSX file must be provided)\n" +
                        " - OPENBIS, the entities are stored directly in an openBIS instance (username, password and openbis URL must be provided)\n " +
                        " - OPENBIS-DEV, the entities are stored directly in an openBIS development instance (username, password, AS openbis URL and DSS openBIS URL must be provided)")
                .required()
                .build();
        options.addOption(output);

        Option user = new Option("u", "user", true, "openBIS instance user login");
        options.addOption(user);

        Option password = new Option("p", "password", false, "openBIS user password (will be prompted)");
        options.addOption(password);

        Option project = new Option("pid", "project", true, "openBIS project identifier");
        options.addOption(project);

        Option verbose = new Option("v", "verbose", false, "Display verbose output");
        options.addOption(verbose);

        Option help = new Option("h", "help", false, "Display this help message");
        options.addOption(help);

        Option additionalFiles = new Option("a", ADDITIONALFILES, true,
                "Additional files. These are additional ontologies that are referenced by the main import file. If an ontology is not found, this can lead to dummy entries in openBIS. At the moment, these have to be on the file system and are not fetched from remote sources.");
        additionalFiles.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(additionalFiles);

        Option filePaths = new Option("f", INPUT_PATHS, true,
                "Path to input files, has to be at least one. All files have to be of the same format.");
        filePaths.setArgs(Option.UNLIMITED_VALUES);
        filePaths.setRequired(true);
        options.addOption(filePaths);

        {
            Option resultFile = new Option("r", RESULT_FILE, true,
                    "Path to out file, there has to be exactly one.");
            resultFile.setArgs(1);
            resultFile.setRequired(true);
            options.addOption(resultFile);
        }


        return options;
    }

    private static String getPassword(CommandLine cmd)
    {
        return "changeit";
    }

    private static void validateCommandLine(CommandLine cmd)
    {
        if (cmd.hasOption("project"))
            if (!validateProjectIdentifier(cmd.getOptionValue("project")))
                throw new IllegalArgumentException("Project identifier not valid! PID must follow the openBIS standard e.g. /DEFAULT/DEFAULT ");
        String outputFormatValue = cmd.getOptionValue("output");
        String[] remainingArgs = cmd.getArgs();
        //Arrays.stream(remainingArgs).forEach(System.out::println);
        boolean areFileSpecified = cmd.hasOption(INPUT_PATHS) && cmd.hasOption(RESULT_FILE);
        switch (outputFormatValue.toUpperCase())
        {
            case "XLSX":
                if (!areFileSpecified)
                {
                    throw new IllegalArgumentException(
                            "For XLSX output, specify the input and output file path. \n " +
                                    "Usage: java -jar lib-rdf-tool.jar -i <input format> -o XLSX <file path> <XLSX output file path> \n");
                }
                break;
            case "OPENBIS":
                if (!areFileSpecified && !cmd.hasOption("username") && !cmd.hasOption("password"))
                {
                    throw new IllegalArgumentException("For OPENBIS output, specify input file path, username, password and openBIS URL. \n " +
                            "Usage: java -jar lib-rdf-tool.jar -i <input format> -o OPENBIS <file path> -u <username> -p <openBIS URL> \n");
                }
                break;
            case "OPENBIS-DEV":
                if (!areFileSpecified || !cmd.hasOption("username") && !cmd.hasOption("password"))
                {
                    throw new IllegalArgumentException(
                            "For OPENBIS-DEV output, specify input file path, username, password, AS openBIS URL and DSS openBIS URL. \n " +
                                    "Usage: java -jar lib-rdf-tool.jar -i <input format> -o OPENBIS-DEV <file path> -u <username> -p <openBIS AS URL> <openBIS DSS URL> \n");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported output type: " + outputFormatValue.toUpperCase());
        }
    }

    public static boolean validateProjectIdentifier(String projectIdentifier)
    {
        String PROJECT_IDENTIFIER_PATTERN = "^/[A-Z0-9_\\-.]+/[A-Z0-9_\\-.]+$";
        Pattern pattern = Pattern.compile(PROJECT_IDENTIFIER_PATTERN);
        Matcher matcher = pattern.matcher(projectIdentifier);

        return matcher.matches();
    }

    private static void executeCommandLine(CommandLine cmd)
    {
        String inputFormatValue = cmd.getOptionValue("input");
        String outputFormatValue = cmd.getOptionValue("output");
        String inputFilePath = null;
        String username = null;
        String password = null;
        String openbisASURL = null;
        String openBISDSSURL = null;
        String projectIdentifier = cmd.getOptionValue("project");
        boolean verbose = cmd.hasOption("verbose");
        String[] additionalFileOption = cmd.getOptionValues(ADDITIONALFILES);
        String[] inputFilePaths = cmd.getOptionValues(INPUT_PATHS);

        List<String> additionalFiles = additionalFileOption != null ? Arrays.stream(additionalFileOption).toList(): List.of();

        String[] remainingArgs = cmd.getArgs();
        //Arrays.stream(remainingArgs).forEach(System.out::println);
        switch (outputFormatValue.toUpperCase())
        {
            case "XLSX":
                String outputFilePath = cmd.getOptionValue(RESULT_FILE);
                System.out.println("Handling: " + inputFormatValue + " -> " + outputFormatValue);
                handleXlsxOutput(inputFormatValue, inputFilePaths, outputFilePath,
                        projectIdentifier, verbose, additionalFiles);
                break;
            case "OPENBIS":
                username = cmd.getOptionValue("user");
                password = getPassword(cmd);
                openbisASURL = remainingArgs[0];

                System.out.println("Handling: " + inputFormatValue + " -> " + outputFormatValue);
                System.out.println("Connect to openBIS instance " + openbisASURL + " with username[" + username + "]"); // and password[" + new String(password) + "]");
                handleOpenBISOutput(inputFormatValue, inputFilePaths, openbisASURL, username,
                        new String(password), projectIdentifier, verbose, additionalFiles);
                break;
            case "OPENBIS-DEV":
                username = cmd.getOptionValue("user");
                password = getPassword(cmd);
                openbisASURL = remainingArgs[0];
                openBISDSSURL = remainingArgs[1];

                System.out.println("Handling: " + inputFormatValue + " -> " + outputFormatValue);
                handleOpenBISDevOutput(inputFormatValue, inputFilePaths, openbisASURL,
                        openBISDSSURL, username, new String(password), projectIdentifier, verbose,
                        additionalFiles);
                break;
            default:
                throw new IllegalArgumentException("Unsupported output type: " + outputFormatValue.toUpperCase());
        }
    }

    private static void handleXlsxOutput(String inputFormatValue, String[] inputFilePaths,
            String outputFilePath, String projectIdentifier, boolean verbose,
            List<String> additionalFilePaths)
    {

        System.out.println(new Date());
        System.out.println("Reading ontModel");
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        for (String path : additionalFilePaths)
        {
            RDFDataMgr.read(ontModel,
                    path,
                    Lang.TTL);
        }

        OntModel additionalModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        for (String path : additionalFilePaths){
            RDFDataMgr.read(additionalModel, path);
        }



        System.out.println(new Date());
        System.out.println("Reading Ontology Model...");

        RDFReader rdfReader = new RDFReader();
        ModelRDF modelRDF =
                rdfReader.read(inputFilePaths, inputFormatValue, verbose, additionalModel);

        // Collect and map all RDF classes in JAVA obj
        System.out.println("Collecting RDF classes...");

        // Write model to an Excel file (apache POI dependency)
        System.out.println("Writing XLSX file...");
        XLSXWriter XLSXWriter = new XLSXWriter();
        XLSXWriter.write(modelRDF, outputFilePath, projectIdentifier);

        System.out.println("XLSX created successfully!");
    }

    private static void handleOpenBISOutput(String inputFormatValue, String[] inputFilePaths,
            String openbisURL, String username, String password, String projectIdentifier,
            boolean verbose, List<String> additionalPaths)
    {
        Path tempFile = Utils.createTemporaryFile();
        String tempFileOutput = tempFile.toString();

        if (DEV_MODE)
        {
            //change to your local path
            tempFileOutput = TEMP_OUTPUT_XLSX;
            tempFile = Path.of(tempFileOutput);
        }

        System.out.println("Created temporary XLSX output file: " + tempFileOutput);
        handleXlsxOutput(inputFormatValue, inputFilePaths, tempFileOutput, projectIdentifier,
                verbose, additionalPaths);

        System.out.println(
                "Connect to openBIS instance " + openbisURL + " with username[" + username + "]"); //and password[" + new String(password) +"]");

        new Importer(openbisURL, username, password, tempFile).connect(tempFile);
    }

    private static void handleOpenBISDevOutput(String inputFormatValue, String[] inputFilePaths,
            String openbisASURL, String openBISDSSURL,
            String username, String password, String projectIdentifier, boolean verbose, List<String> additionalFilePaths) {

        Path tempFile = Utils.createTemporaryFile();
        String tempFileOutput = tempFile.toString();
        if (DEV_MODE)
        {
            //change to your local path
            tempFileOutput = TEMP_OUTPUT_XLSX;
            tempFile = Path.of(tempFileOutput);
        }
        System.out.println("Created temporary XLSX output file: " + tempFileOutput);
        handleXlsxOutput(inputFormatValue, inputFilePaths, tempFileOutput, projectIdentifier,
                verbose, additionalFilePaths);

        System.out.println("Connect to openBIS-DEV instance AS[" + openbisASURL + "] DSS[" + openBISDSSURL + "] with username[" + username + "]"); //and password[" + new String(password) +"]");

        new Importer(openbisASURL, openBISDSSURL, username, password, tempFile).connect(tempFile);
    }
}
