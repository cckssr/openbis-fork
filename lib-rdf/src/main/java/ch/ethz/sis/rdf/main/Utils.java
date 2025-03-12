package ch.ethz.sis.rdf.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils {

    public static final String XLSX_EXTENSION = ".xlsx";
    public static final String PREFIX = "UID";


    public static Path createTemporaryFile() {
        Path tempFile = null;
        try
        {
            tempFile = Files.createTempFile(PREFIX, XLSX_EXTENSION);
        } catch (IOException e)
        {
            throw new RuntimeException("Could not create temporary file: ", e);
        }
        return tempFile;
    }


}
