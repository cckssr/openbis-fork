package ch.ethz.sis.openbis.generic.server.xls.export;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.IDataSetId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.IProjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.ISpaceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.IApplicationServerInternalApi;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ExportEntityCollector {
    private ExportEntityCollector() {}

    /*
     * Used by the V3 API to select samples for export
     */
    public static ExportData collectEntities(
            IApplicationServerInternalApi api,
            String sessionToken,
            ExportData exportData,
            ExportOptions exportOptions) {

        Set<ExportablePermId> allPermIds = new HashSet<>();

        boolean withLevelsAbove = Boolean.TRUE.equals(exportOptions.isWithLevelsAbove());
        boolean withLevelsBelow = Boolean.TRUE.equals(exportOptions.isWithLevelsBelow());
        boolean withObjectsAndDataSetsParents = Boolean.TRUE.equals(exportOptions.isWithObjectsAndDataSetsParents());
        boolean withObjectsAndDataSetsChildren = Boolean.TRUE.equals(exportOptions.isWithObjectsAndDataSetsChildren());
        boolean withObjectsAndDataSetsOtherSpaces = Boolean.TRUE.equals(exportOptions.isWithObjectsAndDataSetsOtherSpaces());

        for (ExportablePermId exportablePermId : exportData.getPermIds()) {
            collectEntities(api, sessionToken, allPermIds, exportablePermId,
                    withLevelsAbove, withLevelsBelow, withObjectsAndDataSetsParents, withObjectsAndDataSetsChildren, withObjectsAndDataSetsOtherSpaces);
        }

        ExportData exportData1 = new ExportData();
        exportData1.setFields(exportData.getFields());
        exportData1.setPermIds(new ArrayList<>(allPermIds));
        return exportData1;
    }

    /*
     * Used by the extended service
     */
    public static void collectEntities(
            IApplicationServerInternalApi api,
            String sessionToken,
            Set<ExportablePermId> collection,
            ExportablePermId root,
            boolean withLevelsAbove,
            boolean withLevelsBelow,
            boolean withObjectsAndDataSetsParents,
            boolean withObjectsAndDataSetsChildren,
            boolean withObjectsAndDataSetsOtherSpaces)
    {
        Set<ExportablePermId> collectedLevelsAbove = new HashSet<>(); // Stores nodes who levels above have been collected to avoid repeating paths
        Deque<ExportablePermId> todo = new LinkedList<>();
        todo.add(root);
        while(todo.isEmpty() == false)
        {
            ExportablePermId current = todo.removeFirst();

            if (collection.contains(current))  // Check to avoid loops, breaking them
            {
                continue;
            }

            collection.add(current); // The current is added

            switch (current.getExportableKind())
            {
                case SPACE:
                    /*
                     * # Space
                     * Space only have levels below, no other selection flags affect it:
                     *  ## Below
                     *  - Projects
                     *  - Space Samples without a project
                     */
                    if (withLevelsBelow) {
                        SpaceFetchOptions spaceFetchOptions = new SpaceFetchOptions();
                        spaceFetchOptions.withProjects();
                        Map<ISpaceId, Space> spaces = api.getSpaces(sessionToken,
                                List.of(new SpacePermId(current.getPermId())),
                                spaceFetchOptions);
                        // Projects
                        for (Space space : spaces.values()) {
                            for (Project project: space.getProjects()) {
                                ExportablePermId projectId = new ExportablePermId(ExportableKind.PROJECT,
                                        project.getPermId().getPermId());
                                todo.add(projectId);
                            }
                        }
                        // Space Samples without a project
                        SampleSearchCriteria sampleSearchCriteria = new SampleSearchCriteria();
                        sampleSearchCriteria.withSpace().withPermId().thatEquals(current.getPermId());
                        sampleSearchCriteria.withoutProject();
                        SearchResult<Sample> sampleSearchResult = api.searchSamples(sessionToken, sampleSearchCriteria, new SampleFetchOptions());
                        for (Sample sample : sampleSearchResult.getObjects()) {
                            ExportablePermId sampleId = new ExportablePermId(ExportableKind.SAMPLE,
                                    sample.getPermId().getPermId());
                            todo.add(sampleId);
                        }
                    }
                    break;
                case PROJECT:
                    /*
                     * # Project
                     * Project have levels above and below, but can't look into other spaces, no other selection flags affect it:
                     *  ## Above
                     *  - Space
                     *  ## Below
                     *  - Experiments
                     *  - Project Samples without an Experiment
                     */
                    if (withLevelsAbove) {
                        // Space
                        ProjectFetchOptions projectFetchOptions = new ProjectFetchOptions();
                        projectFetchOptions.withSpace();
                        Map<IProjectId, Project> projects = api.getProjects(sessionToken,
                                List.of(new ProjectPermId(current.getPermId())),
                                projectFetchOptions);
                        for (Project project : projects.values()) {
                            Space space = project.getSpace();
                            ExportablePermId spaceId = new ExportablePermId(ExportableKind.SPACE, space.getPermId().getPermId());
                            todo.add(spaceId);
                        }
                    }
                    if (withLevelsBelow) {
                        // Experiments
                        ProjectFetchOptions projectFetchOptions = new ProjectFetchOptions();
                        projectFetchOptions.withExperiments();
                        Map<IProjectId, Project> projects = api.getProjects(sessionToken,
                                List.of(new ProjectPermId(current.getPermId())),
                                projectFetchOptions);
                        for (Project project : projects.values()) {
                            List<Experiment> experiments = project.getExperiments();
                            for (Experiment experiment:experiments) {
                                ExportablePermId experimentId = new ExportablePermId(ExportableKind.EXPERIMENT, experiment.getPermId().getPermId());
                                todo.add(experimentId);
                            }
                        }
                        // Project Samples without an Experiment
                        SampleSearchCriteria sampleSearchCriteria = new SampleSearchCriteria();
                        sampleSearchCriteria.withProject().withPermId().thatEquals(current.getPermId());
                        sampleSearchCriteria.withoutExperiment();
                        SearchResult<Sample> sampleSearchResult = api.searchSamples(sessionToken, sampleSearchCriteria, new SampleFetchOptions());
                        for (Sample sample : sampleSearchResult.getObjects()) {
                            ExportablePermId sampleId = new ExportablePermId(ExportableKind.SAMPLE,
                                    sample.getPermId().getPermId());
                            todo.add(sampleId);
                        }
                    }
                    break;
                case EXPERIMENT:
                    /*
                     * # Experiment
                     * Experiment have levels above and below, but can't look into other spaces, no other selection flags affect it:
                     *  ## Always
                     *  - Sample Properties
                     *  ## Above
                     *  - Project
                     *  ## Below
                     *  - Experiment Samples
                     *  - Experiment DataSets NOT belonging to a Sample
                     */
                    SpacePermId enforceExperimentSpaceId = null; // Only used if withObjectsAndDataSetsOtherSpaces == false
                    if (true) {
                        ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
                        experimentFetchOptions.withSampleProperties();
                        if (enforceExperimentSpaceId == null && withObjectsAndDataSetsOtherSpaces == false) {
                            experimentFetchOptions.withProject().withSpace();
                        }
                        Map<IExperimentId, Experiment> experiments = api.getExperiments(sessionToken,
                                List.of(new ExperimentPermId(current.getPermId())),
                                experimentFetchOptions);
                        for (Experiment experiment : experiments.values())
                        {
                            if (enforceExperimentSpaceId == null && withObjectsAndDataSetsOtherSpaces == false) {
                                enforceExperimentSpaceId = experiment.getProject().getSpace().getPermId();
                            }
                            // Sample Properties (Might be in another space)
                            for (Sample[] sampleValues:safe(experiment.getSampleProperties()).values()) {
                                for (Sample sampleValue : sampleValues) {
                                    if (isSampleInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, enforceExperimentSpaceId, sampleValue)) { continue; }

                                    ExportablePermId sampleId = new ExportablePermId(ExportableKind.SAMPLE,
                                            sampleValue.getPermId().getPermId());
                                    todo.add(sampleId);
                                }
                            }
                        }
                    }
                    if (withLevelsAbove) {
                        // Project
                        ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
                        experimentFetchOptions.withProject();
                        Map<IExperimentId, Experiment> experiments = api.getExperiments(sessionToken,
                                        List.of(new ExperimentPermId(current.getPermId())),
                                        experimentFetchOptions);
                        for (Experiment experiment : experiments.values())
                        {
                            Project project = experiment.getProject();
                            ExportablePermId projectId = new ExportablePermId(ExportableKind.PROJECT,
                                    project.getPermId().getPermId());
                            todo.add(projectId);
                        }
                    }
                    if (withLevelsBelow) {
                        // Experiment Samples
                        SampleSearchCriteria sampleSearchCriteria = new SampleSearchCriteria();
                        sampleSearchCriteria.withExperiment().withPermId().thatEquals(current.getPermId());
                        SearchResult<Sample> sampleSearchResult = api.searchSamples(sessionToken, sampleSearchCriteria, new SampleFetchOptions());
                        for (Sample sample : sampleSearchResult.getObjects()) {
                            ExportablePermId sampleId = new ExportablePermId(ExportableKind.SAMPLE,
                                    sample.getPermId().getPermId());
                            todo.add(sampleId);
                        }

                        // Experiment DataSets NOT belonging to a Sample
                        DataSetSearchCriteria dataSetSearchCriteria = new DataSetSearchCriteria();
                        dataSetSearchCriteria.withExperiment().withPermId().thatEquals(current.getPermId());
                        dataSetSearchCriteria.withoutSample();
                        SearchResult<DataSet> dataSetSearchResult = api.searchDataSets(sessionToken, dataSetSearchCriteria, new DataSetFetchOptions());
                        for (DataSet dataSet : dataSetSearchResult.getObjects()) {
                            ExportablePermId next = new ExportablePermId(ExportableKind.DATASET,
                                    dataSet.getPermId().getPermId());
                            todo.add(next);
                        }
                    }
                    break;
                case SAMPLE:
                    /*
                     * # Sample
                     * Sample have levels above and below, and CAN look into other spaces, all flags affect it:
                     *  The strategy is to try to fetch everything from a sample in a single call, it will over fetch if filtering by spaces is needed
                     *  but in some cases this is not avoidable due to API limitations even if we made more than one search call.
                     *  ## Always
                     *  - Sample Properties (looking into other spaces)
                     *  ## Above
                     *  - Experiment / Project / Space
                     *  - Sample Parents (looking into other spaces)
                     *  ## Below
                     *  - Sample Children (looking into other spaces)
                     *  - DataSets
                     */
                    SpacePermId enforceSampleSpaceId = null; // Only used if withObjectsAndDataSetsOtherSpaces == false
                    //
                    // Sample Fetch Preparation
                    //
                    SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
                    if (enforceSampleSpaceId == null && withObjectsAndDataSetsOtherSpaces == false) {
                        sampleFetchOptions.withSpace();
                    }

                    if (true) {
                        // Sample Properties (Might be in another space)
                        if (withObjectsAndDataSetsOtherSpaces == false) {
                            sampleFetchOptions.withSampleProperties().withSpace();
                        } else {
                            sampleFetchOptions.withSampleProperties();
                        }
                    }

                    if (withLevelsAbove) {
                        // Experiment / Project / Space
                        sampleFetchOptions.withExperiment();
                        sampleFetchOptions.withProject();
                        sampleFetchOptions.withSpace();

                        // Parents  (Might be in another space)
                        if (withObjectsAndDataSetsParents) {
                            if (withObjectsAndDataSetsOtherSpaces == false) {
                                sampleFetchOptions.withParents().withSpace();
                            } else {
                                sampleFetchOptions.withParents();
                            }
                        }
                    }

                    if (withLevelsBelow) {
                        // DataSets
                        sampleFetchOptions.withDataSets();

                        // Children  (Might be in another space)
                        if (withObjectsAndDataSetsChildren) {
                            if (withObjectsAndDataSetsOtherSpaces == false) {
                                sampleFetchOptions.withChildren().withSpace();
                            } else {
                                sampleFetchOptions.withChildren();
                            }
                        }
                    }

                    //
                    // Fetch execution
                    //
                    Map<ISampleId, Sample> samples = api.getSamples(sessionToken, List.of(new SamplePermId(current.getPermId())), sampleFetchOptions);
                    Sample sample = samples.values().iterator().next();

                    if (enforceSampleSpaceId == null && withObjectsAndDataSetsOtherSpaces == false) {
                        Space sampleSpace = sample.getSpace();
                        if (sampleSpace != null) {
                            enforceSampleSpaceId = sampleSpace.getPermId();
                        }
                    }

                    //
                    // Iterate over results (filter other spaces if needed)
                    //
                    if (true) {
                        // Sample Properties (Might be in another space)
                        for (Sample[] sampleValues:safe(sample.getSampleProperties()).values()) {
                            for (Sample sampleValue : sampleValues) {
                                if (isSampleInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, enforceSampleSpaceId, sampleValue)) { continue; }
                                ExportablePermId sampleId = new ExportablePermId(ExportableKind.SAMPLE,
                                        sampleValue.getPermId().getPermId());
                                todo.add(sampleId);
                            }
                        }
                    }

                    if (withLevelsAbove) {
                        // Experiment / Project / Space
                        if (sample.getExperiment() != null) {
                            ExportablePermId experimentId = new ExportablePermId(ExportableKind.EXPERIMENT, sample.getExperiment().getPermId().getPermId());
                            todo.add(experimentId);
                        } else if (sample.getProject() != null) {
                            ExportablePermId projectId = new ExportablePermId(ExportableKind.PROJECT, sample.getProject().getPermId().getPermId());
                            todo.add(projectId);
                        } else if (sample.getSpace() != null) {
                            ExportablePermId spaceId = new ExportablePermId(ExportableKind.SPACE, sample.getSpace().getPermId().getPermId());
                            todo.add(spaceId);
                        }

                        // Sample Parents (Might be in another space)
                        if (withObjectsAndDataSetsParents) {
                            for (Sample sampleParent : sample.getParents()) {
                                if (isSampleInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, enforceSampleSpaceId, sampleParent)) {
                                    continue;
                                }

                                ExportablePermId sampleId = new ExportablePermId(ExportableKind.SAMPLE,
                                        sampleParent.getPermId().getPermId());
                                todo.add(sampleId);
                            }
                        }
                    }

                    if (withLevelsBelow) {
                        // DataSets
                        for (DataSet dataSet : sample.getDataSets()) {
                            ExportablePermId sampleId = new ExportablePermId(ExportableKind.DATASET,
                                    dataSet.getPermId().getPermId());
                            todo.add(sampleId);
                        }

                        // Sample Children (Might be in another space)
                        if (withObjectsAndDataSetsChildren) {
                            for (Sample sampleChild : sample.getChildren()) {
                                if (isSampleInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, enforceSampleSpaceId, sampleChild)) {
                                    continue;
                                }

                                ExportablePermId sampleId = new ExportablePermId(ExportableKind.SAMPLE,
                                        sampleChild.getPermId().getPermId());
                                todo.add(sampleId);
                            }
                        }
                    }
                    break;
                case DATASET:
                    /*
                     * # DataSet
                     * DataSet have levels above and below, and CAN look into other spaces, all flags affect it:
                     *  The strategy is to try to fetch everything from a dataset in a single call, it will over fetch if filtering by spaces is needed
                     *  but in some cases this is not avoidable due to API limitations even if we made more than one search call.
                     *  ## Always
                     *  - Sample Properties (looking into other spaces)
                     *  ## Above
                     *  - Experiment / Sample
                     *  - DataSet Parents (looking into other spaces)
                     *  ## Below
                     *  - DataSet Children (looking into other spaces)
                     */
                    SpacePermId enforceDataSetSpaceId = null; // Only used if withObjectsAndDataSetsOtherSpaces == false
                    //
                    // DataSet Fetch Preparation
                    //
                    DataSetFetchOptions dataSetFetchOptions = new DataSetFetchOptions();
                    if (enforceDataSetSpaceId == null && withObjectsAndDataSetsOtherSpaces == false) {
                        dataSetFetchOptions.withSample().withSpace();
                        dataSetFetchOptions.withExperiment().withProject().withSpace();
                    }

                    if (true) {
                        // Sample Properties (Might be in another space)
                        if (withObjectsAndDataSetsChildren) {
                            if (withObjectsAndDataSetsOtherSpaces == false) {
                                dataSetFetchOptions.withSampleProperties().withSpace();
                            } else {
                                dataSetFetchOptions.withSampleProperties();
                            }
                        }
                    }

                    if (withLevelsAbove) {
                        // Experiment / Sample
                        dataSetFetchOptions.withSample();
                        dataSetFetchOptions.withExperiment();

                        // DataSet Parents  (Might be in another space)
                        if (withObjectsAndDataSetsParents) {
                            if (withObjectsAndDataSetsOtherSpaces == false) {
                                dataSetFetchOptions.withParents().withSample().withSpace();
                                dataSetFetchOptions.withParents().withExperiment().withProject().withSpace();
                            } else {
                                dataSetFetchOptions.withParents();
                            }
                        }
                    }

                    if (withLevelsBelow) {
                        // DataSet Children (Might be in another space)
                        if (withObjectsAndDataSetsChildren) {
                            if (withObjectsAndDataSetsOtherSpaces == false) {
                                dataSetFetchOptions.withChildren().withSample().withSpace();
                                dataSetFetchOptions.withChildren().withExperiment().withProject().withSpace();
                            } else {
                                dataSetFetchOptions.withChildren();
                            }
                        }
                    }

                    //
                    // Fetch execution
                    //
                    Map<IDataSetId, DataSet> datasets = api.getDataSets(sessionToken, List.of(new DataSetPermId(current.getPermId())), dataSetFetchOptions);
                    DataSet dataSet = datasets.values().iterator().next();

                    if (enforceDataSetSpaceId == null && withObjectsAndDataSetsOtherSpaces == false) {
                        if (dataSet.getSample() != null) {
                            Sample datasetSample = dataSet.getSample();
                            if (datasetSample != null) {
                                Space sampleSpace = datasetSample.getSpace();
                                if (sampleSpace != null) {
                                    enforceDataSetSpaceId = sampleSpace.getPermId();
                                }
                            }
                        } else if (dataSet.getExperiment() != null) {
                            Experiment dataSetExperiment = dataSet.getExperiment();
                            if (dataSetExperiment != null) {
                                Space experimentSpace = dataSetExperiment.getProject().getSpace();
                                if (experimentSpace != null) {
                                    enforceDataSetSpaceId = experimentSpace.getPermId();
                                }
                            }
                        }
                    }

                    //
                    // Iterate over results (filter other spaces if needed)
                    //
                    if (true) {
                        // Sample Properties (Might be in another space)
                        for (Sample[] sampleValues : safe(dataSet.getSampleProperties()).values()) {
                            for (Sample sampleValue : sampleValues) {
                                if (isSampleInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, enforceDataSetSpaceId, sampleValue)) {
                                    continue;
                                }

                                ExportablePermId sampleId = new ExportablePermId(ExportableKind.SAMPLE,
                                        sampleValue.getPermId().getPermId());
                                todo.add(sampleId);
                            }
                        }
                    }

                    if (withLevelsAbove) {
                        // Sample / Experiment
                        if (dataSet.getSample() != null) {
                            ExportablePermId sampleId = new ExportablePermId(ExportableKind.SAMPLE, dataSet.getSample().getPermId().getPermId());
                            todo.add(sampleId);
                        } else if (dataSet.getExperiment() != null) {
                            ExportablePermId experimentId = new ExportablePermId(ExportableKind.EXPERIMENT, dataSet.getExperiment().getPermId().getPermId());
                            todo.add(experimentId);
                        }

                        // DataSet Parents (Might be in another space)
                        if (withObjectsAndDataSetsParents) {
                            for (DataSet dataSetParent : dataSet.getParents()) {
                                if (isDataSetInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, enforceDataSetSpaceId, dataSet))
                                    continue;

                                ExportablePermId dataSetId = new ExportablePermId(ExportableKind.DATASET,
                                        dataSetParent.getPermId().getPermId());
                                todo.add(dataSetId);
                            }
                        }
                    }

                    if (withLevelsBelow) {
                        // DataSet Children (Might be in another space)
                        if (withObjectsAndDataSetsChildren) {
                            for (DataSet dataSetChild : dataSet.getChildren()) {
                                if (isDataSetInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, enforceDataSetSpaceId, dataSetChild)) { continue; }

                                ExportablePermId dataSetId = new ExportablePermId(ExportableKind.DATASET,
                                        dataSetChild.getPermId().getPermId());
                                todo.add(dataSetId);
                            }
                        }
                    }
                    break;
            }
        }
    }

    private static boolean isDataSetInOtherSpaceBeingFiltered(boolean withObjectsAndDataSetsOtherSpaces, SpacePermId enforceSpaceId, DataSet dataSet) {
        if (enforceSpaceId != null && withObjectsAndDataSetsOtherSpaces == false) {
            if (dataSet.getSample() != null) {
                Sample datasetSample = dataSet.getSample();
                if (datasetSample != null) {
                    Space sampleSpace = datasetSample.getSpace();
                    if (sampleSpace != null && enforceSpaceId.equals(sampleSpace.getPermId()) == false) {
                        return true;
                    }
                }
            } else if (dataSet.getExperiment() != null) {
                Experiment dataSetExperiment = dataSet.getExperiment();
                if (dataSetExperiment != null) {
                    Space experimentSpace = dataSetExperiment.getProject().getSpace();
                    if (experimentSpace != null && enforceSpaceId.equals(experimentSpace.getPermId()) == false) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isSampleInOtherSpaceBeingFiltered(boolean withObjectsAndDataSetsOtherSpaces, SpacePermId enforceSpaceId, Sample sampleParent) {
        if (enforceSpaceId != null && withObjectsAndDataSetsOtherSpaces == false) {
            Space sampleSpace = sampleParent.getSpace();
            if (sampleSpace != null && sampleSpace.getPermId().equals(enforceSpaceId) == false) {
                return true;
            }
        }
        return false;
    }

    private static <K, V> Map<K, V> safe(Map<K, V> mapOrNull) {
        if (mapOrNull == null) {
            return Map.of();
        } else {
            return mapOrNull;
        }
    }
}
