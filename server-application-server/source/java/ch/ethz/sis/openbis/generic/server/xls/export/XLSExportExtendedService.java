package ch.ethz.sis.openbis.generic.server.xls.export;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.IDataSetId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.ExportResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.AllFields;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.XlsTextFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.IProjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.CustomASServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.session.SessionInformation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.ISpaceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.plugin.service.ICustomASServiceExecutor;
import ch.ethz.sis.openbis.generic.asapi.v3.plugin.service.context.CustomASServiceContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.IApplicationServerInternalApi;
import ch.systemsx.cisd.common.mail.EMailAddress;
import ch.systemsx.cisd.common.mail.IMailClient;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;

public class XLSExportExtendedService implements ICustomASServiceExecutor
{

    private final String code = "xls-export-extended";

    private final String label = "XLS Export Extended Service";

    private final String description = "XLS export for internal and external services";

    @Override
    public Object executeService(CustomASServiceContext context, CustomASServiceExecutionOptions options)
    {
        return export(context.getSessionToken(), options.getParameters());
    }

    public String getCode()
    {
        return code;
    }

    public String getLabel()
    {
        return label;
    }

    public String getDescription()
    {
        return description;
    }

    public static Map<String, String> export(String sessionToken, Map<String, Object> parameters) {
        System.out.println("sessionToken: " + sessionToken);
        System.out.println("parameters: " + parameters);

        List<Map<String, Object>> nodeExportMaps = (List<Map<String, Object>>) parameters.get("nodeExportList");
        if (nodeExportMaps == null || nodeExportMaps.isEmpty()) {
            throw new IllegalArgumentException("The parameter nodeExportList cannot be null or empty.");
        }

        // Options
        boolean withEmail = (boolean) parameters.get("withEmail");
        boolean withImportCompatibility = (boolean) parameters.get("withImportCompatibility");
        // Formats
        boolean pdf = ((Map<String, Boolean>) parameters.get("formats")).get("pdf");
        boolean xlsx = ((Map<String, Boolean>) parameters.get("formats")).get("xlsx");
        boolean data = ((Map<String, Boolean>) parameters.get("formats")).get("data");
        boolean afsData = ((Map<String, Boolean>) parameters.get("formats")).get("afsData");

        IApplicationServerInternalApi api = CommonServiceProvider.getApplicationServerApi();
        ExportData exportData = new ExportData();
        Set<ExportablePermId> allPermIds = new HashSet<>();

        for (Map<String, Object> nodeExportMap : nodeExportMaps) {
            String kind = (String) nodeExportMap.get("kind");
            String permId = (String) nodeExportMap.get("permId");
            boolean withLevelsAbove = (boolean) nodeExportMap.get("withLevelsAbove");
            boolean withLevelsBelow = (boolean) nodeExportMap.get("withLevelsBelow");
            boolean withObjectsAndDataSetsParents = (boolean) nodeExportMap.get("withObjectsAndDataSetsParents");
            boolean withObjectsAndDataSetsChildren = (boolean) nodeExportMap.get("withObjectsAndDataSetsChildren");
            boolean withObjectsAndDataSetsOtherSpaces = (boolean) nodeExportMap.get("withObjectsAndDataSetsOtherSpaces");

            ExportableKind rootKind = ExportableKind.valueOf(kind);
            ExportablePermId root = new ExportablePermId(rootKind, permId);
            ExportEntityCollector.collectEntities(api, sessionToken, allPermIds, root, withLevelsAbove, withLevelsBelow, withObjectsAndDataSetsParents, withObjectsAndDataSetsChildren, withObjectsAndDataSetsOtherSpaces);
        }

        exportData.setPermIds(new ArrayList<>(allPermIds));
        exportData.setFields(new AllFields());
        ExportOptions exportOptions = new ExportOptions();
        Set<ExportFormat> formats = new HashSet<>();
        if (pdf) {
            formats.add(ExportFormat.PDF);
        }
        if (xlsx) {
            formats.add(ExportFormat.XLSX);
        }
        if (data) {
            formats.add(ExportFormat.DATA);
        }
        if (afsData) {
            formats.add(ExportFormat.AFS_DATA);
        }
        exportOptions.setFormats(formats);
        exportOptions.setXlsTextFormat(XlsTextFormat.RICH);
        exportOptions.setWithReferredTypes(Boolean.TRUE);
        exportOptions.setWithImportCompatibility(withImportCompatibility);
        exportOptions.setZipSingleFiles(Boolean.TRUE);

        ExportThread exportThread = new ExportThread(api, sessionToken, exportData, exportOptions, withEmail);

        Map<String, String> downloadResultMap = new HashMap<>();
        if (withEmail) {
            Thread thread = new Thread(exportThread);
            thread.start();
            downloadResultMap.put("canonicalPath", Boolean.TRUE.toString());
            downloadResultMap.put("downloadURL", Boolean.TRUE.toString());
            return downloadResultMap;
        } else {
            exportThread.run();
            if (exportThread.getExportException() != null) {
                throw new RuntimeException(exportThread.getExportException());
            } else {
                String downloadURL = exportThread.getExportResult().getDownloadURL();
                String canonicalPath;
                try {
                    String filePath = downloadURL.substring(downloadURL.indexOf("filePath=") + "filePath=".length());
                    canonicalPath = CommonServiceProvider.getSessionWorkspaceProvider()
                            .getCanonicalFile(sessionToken, filePath)
                            .getCanonicalPath();
                } catch (IOException e)
                {
                    throw new RuntimeException("Can't get canonical path from session workspace. ", e);
                }
                downloadResultMap.put("canonicalPath", canonicalPath);
                downloadResultMap.put("downloadURL", downloadURL);
                return downloadResultMap;
            }
        }
    }

    private static class ExportThread implements Runnable {

        private final IApplicationServerInternalApi api;
        private final String sessionToken;
        private final ExportData exportData;
        private final ExportOptions exportOptions;
        private final boolean withEmail;
        private ExportResult exportResult = null;
        private Exception exportException = null;

        public ExportThread(IApplicationServerInternalApi api,
                String sessionToken,
                ExportData exportData,
                ExportOptions exportOptions,
                boolean withEmail)
        {
            this.api = api;
            this.sessionToken = sessionToken;
            this.exportData = exportData;
            this.exportOptions = exportOptions;
            this.withEmail = withEmail;
        }

        @Override
        public void run()
        {
            try
            {
                exportResult = api.executeExport(sessionToken, exportData, exportOptions);
            } catch (Exception ex) {
                exportException = ex;
            }

            if (withEmail) {
                sentEmail();
            }
        }

        private void sentEmail() {
            String content = null;
            if (exportResult != null)
            {
                content = exportResult.getDownloadURL();
            }
            if (exportException != null)
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                exportException.printStackTrace(pw);
                content = sw.toString();
            }
            SessionInformation sessionInformation = api.getSessionInformation(sessionToken);
            EMailAddress eMailAddress = new EMailAddress(sessionInformation.getPerson().getEmail());
            IMailClient eMailClient = CommonServiceProvider.createEMailClient();
            String subject = "openBIS Export Download Ready";
            eMailClient.sendEmailMessage(subject, content, null, null, eMailAddress);
        }

        public ExportResult getExportResult() {
            return exportResult;
        }

        public Exception getExportException() {
            return exportException;
        }
    }
}
