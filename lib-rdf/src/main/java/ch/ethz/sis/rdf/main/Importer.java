package ch.ethz.sis.rdf.main;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportMode;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportOptions;

import java.nio.file.Path;
import java.util.List;

public class Importer {

    private final int TIMEOUT = Integer.MAX_VALUE;

    private final OpenBIS openBIS;
    private final String username;
    private final String password;
    private final Path pathToFile;

    public Importer(String openbisASURL, String username, String password, Path pathToFile){
        this.openBIS = new OpenBIS(openbisASURL + "/openbis/openbis", openbisASURL + "/datastore_server", null, TIMEOUT);
        this.username = username;
        this.password = password;
        this.pathToFile = pathToFile;
    }

    public Importer(String openbisASURL, String openBISDSSURL, String username, String password, Path pathToFile){
        this.openBIS = new OpenBIS(openbisASURL, openBISDSSURL, null, TIMEOUT);
        this.username = username;
        this.password = password;
        this.pathToFile = pathToFile;
    }

    public void connect(Path tempFile) {
        //TODO: is AFS client needed ? AfsClient class not found exception

        String sessionToken = openBIS.login(username, password);
        System.out.println("Retrived sessionToken: " + sessionToken);

        String uploadId = openBIS.uploadToSessionWorkspace(pathToFile);
        System.out.println("Retrived uploadId: " + uploadId);
        // Call excel import
        ImportData importData = new ImportData();
        importData.setFormat(ImportFormat.EXCEL);
        //importData.setSessionWorkspaceFiles(List.of("output.xlsx").toArray(new String[0]));
        importData.setSessionWorkspaceFiles(List.of(tempFile.toFile().getName()).toArray(new String[0]));

        ImportOptions importOptions = new ImportOptions();
        importOptions.setMode(ImportMode.UPDATE_IF_EXISTS);

        System.out.println("Starting import...");

        openBIS.executeImport(importData, importOptions);

        System.out.println("Import complete.");
    }
}
