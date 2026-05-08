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
        Deque<ExportablePermId> todo = new LinkedList<>();
        todo.add(root);
        SpacePermId initialSpacePermId = null;

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
                    Space space = null;
                    SpacePermId spacePermId = null;

                    //
                    // Space Fetch
                    //
                    SpaceFetchOptions spaceFetchOptions = new SpaceFetchOptions();
                    spaceFetchOptions.withProjects();
                    Map<ISpaceId, Space> spaces = api.getSpaces(sessionToken,
                            List.of(new SpacePermId(current.getPermId())),
                            spaceFetchOptions);
                    space = spaces.values().iterator().next();
                    spacePermId = space.getPermId();
                    initialSpacePermId = setInitialSpacePermId(root, initialSpacePermId, current, spacePermId);

                    if (withLevelsBelow && isInitialEntityUpstreamCurrent(root.getExportableKind(), initialSpacePermId, current.getExportableKind(), spacePermId)) { // BIS-2255: only exporting downstream if was initially selected
                        // Projects
                        for (Project project: space.getProjects()) {
                            ExportablePermId projectId = new ExportablePermId(ExportableKind.PROJECT,
                                    project.getPermId().getPermId());
                            todo.add(projectId);
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
                    Project project = null;
                    SpacePermId projectSpacePermId = null;

                    //
                    // Project Fetch
                    //
                    ProjectFetchOptions projectFetchOptions = new ProjectFetchOptions();
                    projectFetchOptions.withSpace();
                    if (withLevelsBelow) {
                        projectFetchOptions.withExperiments();
                    }
                    Map<IProjectId, Project> projects = api.getProjects(sessionToken,
                            List.of(new ProjectPermId(current.getPermId())),
                            projectFetchOptions);
                    project = projects.values().iterator().next();
                    projectSpacePermId = project.getSpace().getPermId();
                    initialSpacePermId = setInitialSpacePermId(root, initialSpacePermId, current, projectSpacePermId);

                    if (withLevelsAbove) {
                        // Space
                        ExportablePermId spaceId = new ExportablePermId(ExportableKind.SPACE, projectSpacePermId.getPermId());
                        todo.add(spaceId);
                    }

                    if (withLevelsBelow && isInitialEntityUpstreamCurrent(root.getExportableKind(), initialSpacePermId, current.getExportableKind(), projectSpacePermId)) { // BIS-2255: only exporting downstream if was initially selected
                        // Experiments
                        List<Experiment> experiments = project.getExperiments();
                        for (Experiment experiment:experiments) {
                            ExportablePermId experimentId = new ExportablePermId(ExportableKind.EXPERIMENT, experiment.getPermId().getPermId());
                            todo.add(experimentId);
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
                    Experiment experiment = null;
                    SpacePermId experimentSpacePermId = null; // Only used if withObjectsAndDataSetsOtherSpaces == false

                    //
                    // Experiment Fetch
                    //
                    ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
                    experimentFetchOptions.withProject().withSpace();
                    if (withObjectsAndDataSetsOtherSpaces == false) {
                        experimentFetchOptions.withSampleProperties().withSpace();
                    } else {
                        experimentFetchOptions.withSampleProperties();
                    }
                    Map<IExperimentId, Experiment> experiments = api.getExperiments(sessionToken,
                            List.of(new ExperimentPermId(current.getPermId())),
                            experimentFetchOptions);
                    experiment = experiments.values().iterator().next();
                    experimentSpacePermId = experiment.getProject().getSpace().getPermId();
                    initialSpacePermId = setInitialSpacePermId(root, initialSpacePermId, current, experimentSpacePermId);

                    // Sample Properties (Might be in another space)
                    for (Sample[] sampleValues : safe(experiment.getSampleProperties()).values()) {
                        for (Sample sampleValue : sampleValues) {
                            if (isSampleInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, initialSpacePermId, sampleValue)) {
                                continue;
                            }

                            ExportablePermId sampleId = new ExportablePermId(ExportableKind.SAMPLE,
                                    sampleValue.getPermId().getPermId());
                            todo.add(sampleId);
                        }
                    }

                    if (withLevelsAbove) {
                        // Project
                        Project experimentProject = experiment.getProject();
                        ExportablePermId projectId = new ExportablePermId(ExportableKind.PROJECT,
                                experimentProject.getPermId().getPermId());
                        todo.add(projectId);
                    }

                    if (withLevelsBelow && isInitialEntityUpstreamCurrent(root.getExportableKind(), initialSpacePermId, current.getExportableKind(), experimentSpacePermId)) { // BIS-2255: only exporting downstream if was initially selected
                        // Experiment Samples (implicitly always on same space as experiment)
                        SampleSearchCriteria sampleSearchCriteria = new SampleSearchCriteria();
                        sampleSearchCriteria.withExperiment().withPermId().thatEquals(current.getPermId());
                        SearchResult<Sample> sampleSearchResult = api.searchSamples(sessionToken, sampleSearchCriteria, new SampleFetchOptions());
                        for (Sample sample : sampleSearchResult.getObjects()) {
                            ExportablePermId sampleId = new ExportablePermId(ExportableKind.SAMPLE,
                                    sample.getPermId().getPermId());
                            todo.add(sampleId);
                        }

                        // Experiment DataSets NOT belonging to a Sample (implicitly always on same space as experiment)
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
                    Sample sample = null;
                    SpacePermId sampleSpacePermId = null; // Only used if withObjectsAndDataSetsOtherSpaces == false

                    //
                    // Sample Fetch
                    //
                    SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
                    sampleFetchOptions.withSpace();

                    // Sample Properties (Might be in another space)
                    if (withObjectsAndDataSetsOtherSpaces == false) {
                        sampleFetchOptions.withSampleProperties().withSpace();
                    } else {
                        sampleFetchOptions.withSampleProperties();
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

                    Map<ISampleId, Sample> samples = api.getSamples(sessionToken, List.of(new SamplePermId(current.getPermId())), sampleFetchOptions);
                    sample = samples.values().iterator().next();
                    sampleSpacePermId = sample.getSpace().getPermId();
                    initialSpacePermId = setInitialSpacePermId(root, initialSpacePermId, current, sampleSpacePermId);

                    //
                    // Iterate over results (filter other spaces if needed)
                    //

                    // Sample Properties (Might be in another space)
                    for (Sample[] sampleValues:safe(sample.getSampleProperties()).values()) {
                        for (Sample sampleValue : sampleValues) {
                            if (isSampleInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, initialSpacePermId, sampleValue)) { continue; }
                            ExportablePermId sampleId = new ExportablePermId(ExportableKind.SAMPLE,
                                    sampleValue.getPermId().getPermId());
                            todo.add(sampleId);
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
                                if (isSampleInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, initialSpacePermId, sampleParent)) {
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
                                if (isSampleInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, initialSpacePermId, sampleChild)) {
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
                    DataSet dataSet = null;
                    SpacePermId datasetSpacePermId = null;

                    //
                    // DataSet Fetch
                    //
                    DataSetFetchOptions dataSetFetchOptions = new DataSetFetchOptions();
                    dataSetFetchOptions.withSample().withSpace();
                    dataSetFetchOptions.withExperiment().withProject().withSpace();

                    // Sample Properties (Might be in another space)
                    if (withObjectsAndDataSetsOtherSpaces == false) {
                        dataSetFetchOptions.withSampleProperties().withSpace();
                    } else {
                        dataSetFetchOptions.withSampleProperties();
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

                    Map<IDataSetId, DataSet> datasets = api.getDataSets(sessionToken, List.of(new DataSetPermId(current.getPermId())), dataSetFetchOptions);
                    dataSet = datasets.values().iterator().next();
                    if (dataSet.getSample() != null) {
                        datasetSpacePermId = dataSet.getSample().getSpace().getPermId();
                    }
                    if (dataSet.getExperiment() != null) {
                        datasetSpacePermId = dataSet.getExperiment().getProject().getSpace().getPermId();
                    }
                    initialSpacePermId = setInitialSpacePermId(root, initialSpacePermId, current, datasetSpacePermId);

                    //
                    // Iterate over results (filter other spaces if needed)
                    //

                    // Sample Properties (Might be in another space)
                    for (Sample[] sampleValues : safe(dataSet.getSampleProperties()).values()) {
                        for (Sample sampleValue : sampleValues) {
                            if (isSampleInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, initialSpacePermId, sampleValue)) {
                                continue;
                            }

                            ExportablePermId sampleId = new ExportablePermId(ExportableKind.SAMPLE,
                                    sampleValue.getPermId().getPermId());
                            todo.add(sampleId);
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
                                if (isDataSetInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, initialSpacePermId, dataSet)) {
                                    continue;
                                }

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
                                if (isDataSetInOtherSpaceBeingFiltered(withObjectsAndDataSetsOtherSpaces, initialSpacePermId, dataSetChild)) {
                                    continue;
                                }

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

    private static boolean isInitialEntityUpstreamCurrent(ExportableKind initialKind, SpacePermId initialSpace, ExportableKind currentKind, SpacePermId currentSpace) {
        boolean isInitialSpace = currentSpace.equals(initialSpace);
        boolean isIncludingCurrent = false;
        switch (initialKind) {
            case SPACE ->
            {
                isIncludingCurrent = currentKind == ExportableKind.SPACE || currentKind == ExportableKind.PROJECT || currentKind == ExportableKind.EXPERIMENT;
            }
            case PROJECT ->
            {
                isIncludingCurrent = currentKind == ExportableKind.PROJECT || currentKind == ExportableKind.EXPERIMENT;
            }
            case EXPERIMENT ->
            {
                isIncludingCurrent = currentKind == ExportableKind.EXPERIMENT;
            }
        }
        return isInitialSpace && isIncludingCurrent;
    }

    private static SpacePermId setInitialSpacePermId(ExportablePermId root, SpacePermId initialSpacePermId, ExportablePermId currentPermId, SpacePermId currentPermIdSpacePermId) {
        if (initialSpacePermId == null &&
                root.getExportableKind().equals(currentPermId.getExportableKind()) &&
                root.getPermId().equals(currentPermId.getPermId())) {
            return currentPermIdSpacePermId;
        }
        return initialSpacePermId;
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
