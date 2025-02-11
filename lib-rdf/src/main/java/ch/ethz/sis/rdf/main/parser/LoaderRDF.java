package ch.ethz.sis.rdf.main.parser;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManager;

import java.io.InputStream;

public class LoaderRDF {

    public static Model loadModel(String inputFileName, String inputFormatValue)
    {
        checkFileExists(inputFileName);
        Model model = ModelFactory.createDefaultModel();
        readModel(model, inputFileName, inputFormatValue);
        return model;
    }

    public static OntModel loadOntModel(String[] inputFileNames, String inputFormatValue)
    {

        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        for (String inputFileName : inputFileNames)
        {
            checkFileExists(inputFileName);
            readModel(model, inputFileName, inputFormatValue);

        }
        return model;
    }

    private static void checkFileExists(String inputFileName)
    {
        InputStream in = FileManager.getInternal().open(inputFileName);
        if (in == null)
        {
            throw new IllegalArgumentException("File: " + inputFileName + " not found");
        }
    }

    private static void readModel(Model model, String inputFileName, String inputFormatValue)
    {
        switch (inputFormatValue) {
            case "TTL":
                RDFDataMgr.read(model, inputFileName, Lang.TTL);
                break;
            case "JSONLD":
            case "XML":
            default:
                throw new IllegalArgumentException("Input format file: " + inputFormatValue + " not supported");
        }
    }
}
