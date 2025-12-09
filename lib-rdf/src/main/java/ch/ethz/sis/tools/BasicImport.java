package ch.ethz.sis.tools;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.ImportOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportMode;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.*;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.fetchoptions.OperationExecutionFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.id.IOperationExecutionId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.id.OperationExecutionPermId;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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

            ImportOperation importOperation = new ImportOperation();
            importOperation.setImportData(importData);
            importOperation.setImportOptions(importOptions);

            AsynchronousOperationExecutionResults ongoingOperations =
                    (AsynchronousOperationExecutionResults) openBIS.executeOperations(List.of(importOperation),
                            new AsynchronousOperationExecutionOptions());
            // ImportResult importResult = openBIS.executeImport(importData, importOptions);

            OperationExecutionPermId executionId = ongoingOperations.getExecutionId();
            boolean isOperationFinished = false;
            int minutesRunning = 0;
            OperationExecutionFetchOptions ongoingOperationsFechOptions =
                    new OperationExecutionFetchOptions();
            ongoingOperationsFechOptions.withDetails();
            ongoingOperationsFechOptions.withNotification();
            ongoingOperationsFechOptions.withOwner();
            ongoingOperationsFechOptions.withSummary();
            ongoingOperationsFechOptions.withSummary().withError();

            while (isOperationFinished == false)
            {
                Map<IOperationExecutionId, OperationExecution> operationExecutions =
                        openBIS.getOperationExecutions(List.of(executionId),
                                ongoingOperationsFechOptions);
                OperationExecution operationExecution = operationExecutions.get(executionId);
                if (operationExecution != null)
                {
                    OperationExecutionState state = operationExecution.getState();
                    if (state == OperationExecutionState.FINISHED)
                    {
                        isOperationFinished = true;
                        System.out.println("Import operation finished");
                        System.exit(0);
                    } else if (state == OperationExecutionState.FAILED)
                    {
                        isOperationFinished = true;
                        OperationExecutionSummary summary = operationExecution.getSummary();
                        if (summary != null)
                        {
                            System.out.println("Import operation Error:" + summary.getError());
                        }
                        System.exit(2);
                    } else
                    {
                        System.out.println("Import operation on state: " + state);
                    }
                }
                Thread.sleep(1000 * 60); // Wait one minute before retry
                minutesRunning++;
                System.out.println(
                        "Operation " + executionId.getPermId() + " has been running for " + minutesRunning + " minutes");
            }
            System.out.println("Import complete.");

        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
