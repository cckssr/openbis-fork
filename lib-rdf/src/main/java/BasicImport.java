import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.ImportResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportMode;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportOptions;

import java.nio.file.Path;
import java.util.List;

public class BasicImport
{
    public static void main(String[] args)
    {
        if (args.length != 3)
        {
            System.out.println("This requires 3 arguments:\n- input file\n- token\n- openBIS URL");
            System.exit(1);
        }

        try
        {
            String file = args[0];
            String token = args[1];
            String url = args[2];

            OpenBIS openBIS = new OpenBIS(url, Integer.MAX_VALUE);
            openBIS.setSessionToken(token);

            Path tempFile = Path.of(file);
            String uploadId = openBIS.uploadToSessionWorkspace(tempFile);
            System.out.println("Retrived uploadId: " + uploadId);
            // Call excel import
            ImportData importData = new ImportData();
            importData.setFormat(ImportFormat.EXCEL);
            //importData.setSessionWorkspaceFiles(List.of("output.xlsx").toArray(new String[0]));
            importData.setSessionWorkspaceFiles(
                    List.of(tempFile.toFile().getName()).toArray(new String[0]));

            ImportOptions importOptions = new ImportOptions();
            importOptions.setMode(ImportMode.UPDATE_IF_EXISTS);

            System.out.println("Starting import...");

            ImportResult importResult = openBIS.executeImport(importData, importOptions);

            System.out.println("Import complete.");

        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);

        }
    }

}
