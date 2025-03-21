package ch.ethz.sis.openbis.generic.server.xls.export;

import java.io.PrintWriter;
import java.io.Serializable;
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

    public static String export(String sessionToken, Map<String, Object> parameters) {
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

        IApplicationServerInternalApi api = CommonServiceProvider.getApplicationServerApi();
        ExportData exportData = new ExportData();
        Set<ExportablePermId> allPermIds = new HashSet<>();

        for (Map<String, Object> nodeExportMap : nodeExportMaps) {
            String kind = (String) nodeExportMap.get("kind");
            String permId = (String) nodeExportMap.get("permId");
            boolean withLevelsAbove = (boolean) nodeExportMap.get("withLevelsAbove");
            boolean withLevelsBelow = (boolean) nodeExportMap.get("withLevelsBelow");
            boolean withObjectsAndDataSetsParents = (boolean) nodeExportMap.get("withObjectsAndDataSetsParents");
            boolean withObjectsAndDataSetsOtherSpaces = (boolean) nodeExportMap.get("withObjectsAndDataSetsOtherSpaces");

            ExportableKind rootKind = ExportableKind.valueOf(kind);
            ExportablePermId root = new ExportablePermId(rootKind, permId);
            collectEntities(api, sessionToken, allPermIds, root, withLevelsAbove, withLevelsBelow, withObjectsAndDataSetsParents, withObjectsAndDataSetsOtherSpaces);
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
        exportOptions.setFormats(formats);
        exportOptions.setXlsTextFormat(XlsTextFormat.RICH);
        exportOptions.setWithReferredTypes(Boolean.TRUE);
        exportOptions.setWithImportCompatibility(withImportCompatibility);
        exportOptions.setZipSingleFiles(Boolean.TRUE);

        ExportThread exportThread = new ExportThread(api, sessionToken, exportData, exportOptions, withEmail);
        if (withEmail) {
            Thread thread = new Thread(exportThread);
            thread.start();
            return Boolean.TRUE.toString();
        } else {
            exportThread.run();
            if (exportThread.getExportException() != null) {
                throw new RuntimeException(exportThread.getExportException());
            } else {
                return exportThread.getExportResult().getDownloadURL();
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

    private static void collectEntities(
            IApplicationServerInternalApi api,
            String sessionToken,
            Set<ExportablePermId> collection,
            ExportablePermId root,
            boolean withLevelsAbove,
            boolean withLevelsBelow,
            boolean withObjectsAndDataSetsParents,
            boolean withObjectsAndDataSetsOtherSpaces)
    {
        Set<ExportablePermId> collectedLevelsAbove = new HashSet<>(); // Stores nodes who levels above have been collected to avoid repeating paths
        Deque<ExportablePermId> todo = new LinkedList<>();
        todo.add(root);

        while(todo.isEmpty() == false)
        {
            ExportablePermId current = todo.removeFirst();

            if (collection.contains(current))
            { // Check to avoid loops
                continue;
            }

            collection.add(current);

            /*
             * These are not added to the TO-DO
             * These will create a lot of redundant queries, a more optimal implementation is doable with bigger changes.
             */
            if (withLevelsAbove && !withLevelsBelow && !collectedLevelsAbove.contains(current)) {
                switch (current.getExportableKind())
                {
                    case PROJECT:
                        ProjectFetchOptions projectFetchOptions = new ProjectFetchOptions();
                        projectFetchOptions.withSpace();
                        Map<IProjectId, Project> projects = api.getProjects(sessionToken,
                                List.of(new ProjectPermId(current.getPermId())),
                                projectFetchOptions);
                        collectLevelsAboveProject(projects, collection, collectedLevelsAbove);
                        break;
                    case EXPERIMENT:
                        ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
                        experimentFetchOptions.withProject().withSpace();
                        Map<IExperimentId, Experiment> experiments =
                                api.getExperiments(sessionToken,
                                        List.of(new ExperimentPermId(current.getPermId())),
                                        experimentFetchOptions);
                        collectLevelsAboveExperiment(experiments, collection, collectedLevelsAbove);
                        break;
                    case SAMPLE:
                        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
                        sampleFetchOptions.withExperiment().withProject().withSpace();
                        sampleFetchOptions.withSpace();
                        Map<ISampleId, Sample> samples = api.getSamples(sessionToken,
                                List.of(new SamplePermId(current.getPermId())),
                                sampleFetchOptions);
                        collectLevelsAboveSample(samples, collection, collectedLevelsAbove);
                        break;
                    case DATASET:
                        DataSetFetchOptions dataSetFetchOptions = new DataSetFetchOptions();
                        dataSetFetchOptions.withExperiment().withProject().withSpace();
                        dataSetFetchOptions.withSample().withExperiment().withProject().withSpace();
                        Map<IDataSetId, DataSet> dataSets = api.getDataSets(sessionToken,
                                List.of(new DataSetPermId(current.getPermId())),
                                dataSetFetchOptions);
                        collectLevelsAboveDataSet(dataSets, collection, collectedLevelsAbove);
                        break;
                }
            }

            if (withLevelsBelow)
            {
                switch (current.getExportableKind())
                {
                    case SPACE:
                        SpaceFetchOptions spaceFetchOptions = new SpaceFetchOptions();
                        spaceFetchOptions.withProjects();
                        Map<ISpaceId, Space> spaces = api.getSpaces(sessionToken,
                                List.of(new SpacePermId(current.getPermId())),
                                spaceFetchOptions);
                        for (Space space : spaces.values())
                        {
                            for (Project project : space.getProjects())
                            {
                                ExportablePermId next = new ExportablePermId(ExportableKind.PROJECT,
                                        project.getPermId().getPermId());
                                todo.add(next);
                                collectedLevelsAbove.add(next);
                            }
                        }
                        break;
                    case PROJECT:
                        ProjectFetchOptions projectFetchOptions = new ProjectFetchOptions();
                        projectFetchOptions.withExperiments();
                        if (withLevelsAbove && !collectedLevelsAbove.contains(current)) {
                            projectFetchOptions.withSpace();
                        }
                        Map<IProjectId, Project> projects = api.getProjects(sessionToken,
                                List.of(new ProjectPermId(current.getPermId())),
                                projectFetchOptions);
                        if (withLevelsAbove && !collectedLevelsAbove.contains(current)) {
                            collectLevelsAboveProject(projects, collection, collectedLevelsAbove);
                        }
                        for (Project project : projects.values())
                        {
                            for (Experiment experiment : project.getExperiments())
                            {
                                ExportablePermId next = new ExportablePermId(ExportableKind.EXPERIMENT,
                                        experiment.getPermId().getPermId());
                                todo.add(next);
                                collectedLevelsAbove.add(next);
                            }
                        }
                        break;
                    case EXPERIMENT:
                        ExperimentFetchOptions experimentFetchOptions =
                                new ExperimentFetchOptions();
                        experimentFetchOptions.withSampleProperties();
                        experimentFetchOptions.withSamples();
                        experimentFetchOptions.withDataSets().withSample();
                        if (withLevelsAbove && !collectedLevelsAbove.contains(current)) {
                            experimentFetchOptions.withProject().withSpace();
                        }
                        Map<IExperimentId, Experiment> experiments =
                                api.getExperiments(sessionToken,
                                        List.of(new ExperimentPermId(current.getPermId())),
                                        experimentFetchOptions);
                        if (withLevelsAbove && !collectedLevelsAbove.contains(current)) {
                            collectLevelsAboveExperiment(experiments, collection, collectedLevelsAbove);
                        }
                        for (Experiment experiment : experiments.values())
                        {
                            addObjectProperties(todo, experiment.getSampleProperties());
                            for (Sample sample : experiment.getSamples())
                            {
                                ExportablePermId next = new ExportablePermId(ExportableKind.SAMPLE,
                                        sample.getPermId().getPermId());
                                todo.add(next);
                            }
                            for (DataSet dataSet : experiment.getDataSets())
                            {
                                ExportablePermId next = new ExportablePermId(ExportableKind.DATASET,
                                        dataSet.getPermId().getPermId());
                                todo.add(next);
                                if (dataSet.getSample() == null) { // DataSet don't belong to a sample, only to this experiment
                                    collectedLevelsAbove.add(next);
                                }
                            }
                        }
                        break;
                    case SAMPLE:
                        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
                        sampleFetchOptions.withSampleProperties();
                        sampleFetchOptions.withChildren();
                        sampleFetchOptions.withDataSets();
                        if (withLevelsAbove && !collectedLevelsAbove.contains(current)) {
                            sampleFetchOptions.withExperiment().withProject().withSpace();
                            sampleFetchOptions.withSpace();
                        }
                        if (withObjectsAndDataSetsParents)
                        {
                            sampleFetchOptions.withParents();
                        }

                        Map<ISampleId, Sample> samples = api.getSamples(sessionToken,
                                List.of(new SamplePermId(current.getPermId())),
                                sampleFetchOptions);
                        if (withLevelsAbove && !collectedLevelsAbove.contains(current)) {
                            collectLevelsAboveSample(samples, collection, collectedLevelsAbove);
                        }
                        for (Sample sample : samples.values())
                        {
                            addObjectProperties(todo, sample.getSampleProperties());
                            // TODO This optimization can lead to over-include samples. Having shared samples and spaces with the same codes will lead to include samples on the space with the same code as the shared sample.
                            String sampleSpaceCode =
                                    sample.getIdentifier().getIdentifier().split("/")[1];
                            if (withObjectsAndDataSetsParents)
                            {
                                for (Sample parent : sample.getParents())
                                {
                                    // TODO This optimization can lead to over-include samples. Having shared samples and spaces with the same codes will lead to include samples on the space with the same code as the shared sample.
                                    String parentSpaceCode =
                                            parent.getIdentifier().getIdentifier().split("/")[1];
                                    if (sampleSpaceCode.equals(
                                            parentSpaceCode) || withObjectsAndDataSetsOtherSpaces)
                                    {
                                        ExportablePermId next = new ExportablePermId(ExportableKind.SAMPLE,
                                                parent.getPermId().getPermId());
                                        todo.add(next);
                                    }
                                }
                            }
                            for (Sample child : sample.getChildren())
                            {
                                // TODO This optimization can lead to over-include samples. Having shared samples and spaces with the same codes will lead to include samples on the space with the same code as the shared sample.
                                String childSpaceCode =
                                        child.getIdentifier().getIdentifier().split("/")[1];
                                if (sampleSpaceCode.equals(
                                        childSpaceCode) || withObjectsAndDataSetsOtherSpaces)
                                {
                                    ExportablePermId next = new ExportablePermId(ExportableKind.SAMPLE,
                                            child.getPermId().getPermId());
                                    todo.add(next);
                                }
                            }
                            for (DataSet dataSet : sample.getDataSets())
                            {
                                ExportablePermId next = new ExportablePermId(ExportableKind.DATASET,
                                        dataSet.getPermId().getPermId());
                                todo.add(next);
                                collectedLevelsAbove.add(next);
                            }
                        }
                        break;
                    case DATASET:
                        DataSetFetchOptions dataSetFetchOptions = new DataSetFetchOptions();
                        dataSetFetchOptions.withSampleProperties();
                        dataSetFetchOptions.withSample();
                        dataSetFetchOptions.withExperiment();
                        final DataSetFetchOptions childrenDataSetFetchOptions =
                                dataSetFetchOptions.withChildren();
                        childrenDataSetFetchOptions.withExperiment();
                        if (withLevelsAbove && !collectedLevelsAbove.contains(current)) {
                            dataSetFetchOptions.withExperiment().withProject().withSpace();
                            dataSetFetchOptions.withSample().withExperiment().withProject().withSpace();
                        }
                        if (withObjectsAndDataSetsParents)
                        {
                            final DataSetFetchOptions parentDataSetFetchOptions =
                                    dataSetFetchOptions.withParents();
                            parentDataSetFetchOptions.withExperiment();
                            parentDataSetFetchOptions.withSample();
                        }

                        Map<IDataSetId, DataSet> dataSets = api.getDataSets(sessionToken,
                                List.of(new DataSetPermId(current.getPermId())),
                                dataSetFetchOptions);
                        if (withLevelsAbove && !collectedLevelsAbove.contains(current)) {
                            collectLevelsAboveDataSet(dataSets, collection, collectedLevelsAbove);
                        }
                        for (DataSet dataset : dataSets.values())
                        {
                            addObjectProperties(todo, dataset.getSampleProperties());
                            String datasetSpaceCode =
                                    dataset.getExperiment().getIdentifier().getIdentifier()
                                            .split("/")[1];

                            if (withObjectsAndDataSetsParents)
                            {
                                for (DataSet parent : dataset.getParents())
                                {
                                    String parentDatasetSpaceCode =
                                            parent.getExperiment().getIdentifier().getIdentifier()
                                                    .split("/")[1];
                                    if (datasetSpaceCode.equals(
                                            parentDatasetSpaceCode) || withObjectsAndDataSetsOtherSpaces)
                                    {
                                        ExportablePermId next = new ExportablePermId(ExportableKind.DATASET,
                                                parent.getPermId().getPermId());
                                        todo.add(next);
                                    }
                                }
                            }

                            for (DataSet child : dataset.getChildren())
                            {
                                String childDatasetSpaceCode =
                                        child.getExperiment().getIdentifier().getIdentifier()
                                                .split("/")[1];
                                if (datasetSpaceCode.equals(
                                        childDatasetSpaceCode) || withObjectsAndDataSetsOtherSpaces)
                                {
                                    ExportablePermId next = new ExportablePermId(ExportableKind.DATASET,
                                            child.getPermId().getPermId());
                                    todo.add(next);
                                }
                            }

                        }
                        break;
                }
            }
        }
    }

    private static void collectLevelsAboveDataSet(Map<IDataSetId, DataSet> dataSets, Set<ExportablePermId> collection, Set<ExportablePermId> collectedLevelsAbove) {
        for (DataSet dataset : dataSets.values())
        {
            Sample sample = dataset.getSample();
            if (sample != null) {
                collectLevelsAboveSample(Map.of(sample.getIdentifier(), sample), collection, collectedLevelsAbove);
            } else {
                Experiment experiment = dataset.getExperiment();
                if (experiment != null) {
                    collectLevelsAboveExperiment(Map.of(experiment.getIdentifier(), experiment), collection, collectedLevelsAbove);
                }
            }
            collectedLevelsAbove.add(new ExportablePermId(ExportableKind.DATASET,
                    dataset.getPermId().getPermId()));
        }
    }

    private static void collectLevelsAboveSample(Map<ISampleId, Sample> samples, Set<ExportablePermId> collection, Set<ExportablePermId> collectedLevelsAbove) {
        for (Sample sample : samples.values())
        {
            Experiment experiment = sample.getExperiment();
            if (experiment != null) {
                ExportablePermId previousExperiment = new ExportablePermId(ExportableKind.EXPERIMENT,
                        experiment.getPermId().getPermId());
                collection.add(previousExperiment);

                collectLevelsAboveExperiment(Map.of(experiment.getIdentifier(), experiment), collection, collectedLevelsAbove);
            } else {
                Space space = sample.getSpace();
                if (space != null)
                {
                    ExportablePermId previousSpace = new ExportablePermId(ExportableKind.SPACE,
                            space.getPermId().getPermId());
                    collection.add(previousSpace);
                }
            }
            collectedLevelsAbove.add(new ExportablePermId(ExportableKind.SAMPLE,
                    sample.getPermId().getPermId()));
        }
    }

    private static void collectLevelsAboveExperiment(Map<IExperimentId, Experiment> experiments, Set<ExportablePermId> collection, Set<ExportablePermId> collectedLevelsAbove) {
        for (Experiment experiment : experiments.values())
        {
            Project project = experiment.getProject();
            if (project != null)
            {
                ExportablePermId previousProject = new ExportablePermId(ExportableKind.PROJECT,
                        project.getPermId().getPermId());
                collection.add(previousProject);

                collectLevelsAboveProject(Map.of(project.getIdentifier(), project), collection, collectedLevelsAbove);
            }

            collectedLevelsAbove.add(new ExportablePermId(ExportableKind.EXPERIMENT,
                    experiment.getPermId().getPermId()));
        }
    }

    private static void collectLevelsAboveProject(Map<IProjectId, Project> projects, Set<ExportablePermId> collection, Set<ExportablePermId> collectedLevelsAbove) {
        for (Project project : projects.values())
        {
            Space space = project.getSpace();
            ExportablePermId previousSpace = new ExportablePermId(ExportableKind.SPACE,
                    space.getPermId().getPermId());
            collection.add(previousSpace);

            collectedLevelsAbove.add(new ExportablePermId(ExportableKind.PROJECT,
                    project.getPermId().getPermId()));
        }
    }

    private static void addObjectProperties(Deque<ExportablePermId> todo,
            Map<String, Sample[]> sampleProperties)
    {
        for (Sample[] samples: safe(sampleProperties).values()) {
            for (Sample sample:samples) {
                todo.add(new ExportablePermId(ExportableKind.SAMPLE,
                        sample.getPermId().getPermId()));
            }
        }
    }

    private static <K, V> Map<K, V> safe(Map<K, V> mapOrNull) {
        if (mapOrNull == null) {
            return Map.of();
        } else {
            return mapOrNull;
        }
    }
}
