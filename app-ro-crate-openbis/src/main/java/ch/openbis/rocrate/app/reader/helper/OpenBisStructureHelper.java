package ch.openbis.rocrate.app.reader.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.openbis.rocrate.app.reader.RdfToModel;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class OpenBisStructureHelper
{

    public static Structure findStructure(Map<SpacePermId, Space> spaces,
            Map<ProjectIdentifier, Project> projects,
            Map<ExperimentIdentifier, Experiment> idsToCollections,
            Pair<Sample, RdfToModel.ReferencesToResolve> sampleToResolve,
            String fallbacbProjectCode, String fallbackSpaceCode)
    {
        if (sampleToResolve.getRight().getSpaceCode() == null)
        {
            Space space = spaces.get(new SpacePermId(fallbackSpaceCode));

            Project project =
                    projects.get(new ProjectIdentifier(space.getCode(), fallbacbProjectCode));

            String collectionCode =
                    Optional.ofNullable(sampleToResolve.getRight().getCollectionCode())
                            .orElse(sampleToResolve.getLeft().getType().getCode() + "_COLLECTION");

            ExperimentIdentifier experimentIdentifier =
                    new ExperimentIdentifier(space.getCode(), project.getCode(), collectionCode
                    );
            if (!idsToCollections.containsKey(experimentIdentifier))
            {


                Experiment experiment = new Experiment();
                experiment.setIdentifier(experimentIdentifier);
                experiment.setCode(collectionCode);
                experiment.setProject(projects.get(
                        new ProjectIdentifier(fallbackSpaceCode, fallbacbProjectCode)));
                {
                    Map<String, Serializable> properties = new LinkedHashMap<>();
                    properties.put("NAME", collectionCode);

                }
                idsToCollections.put(experimentIdentifier, experiment);
                ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
                experimentFetchOptions.withType();
                experimentFetchOptions.withProject();
                experimentFetchOptions.withProperties();

                experiment.setType(getDefaultExperimentType());
                experiment.setFetchOptions(experimentFetchOptions);
            }

            Experiment experiment = idsToCollections.get(experimentIdentifier);

            return new Structure(
                    new SampleIdentifier("/" + space.getCode() + "/" + project.getCode() + "/" +
                            sampleToResolve.getLeft().getCode()), space, project, experiment);

        }
        Space space = spaces.get(new SpacePermId(sampleToResolve.getRight().getSpaceCode()));

        if (sampleToResolve.getRight().getProjectCode() == null)
        {
            return new Structure(new SampleIdentifier(space.getCode(), null,
                    sampleToResolve.getLeft().getCode()), space, null, null);

        }
        Project project = projects.get(new ProjectIdentifier(space.getCode(),
                sampleToResolve.getRight().getProjectCode()));

        if (sampleToResolve.getRight().getCollectionCode() == null)
        {
            return new Structure(new SampleIdentifier(space.getCode(), null,
                    sampleToResolve.getLeft().getCode()), space, project, null);

        }

        Experiment experiment = idsToCollections.get(
                new ExperimentIdentifier(sampleToResolve.getRight().getSpaceCode(),
                        sampleToResolve.getRight().getProjectCode(),
                        sampleToResolve.getRight().getCollectionCode()));

        for (Experiment experiment1 : idsToCollections.values())
        {
            if (experiment1.getType() == null)
            {
                experiment1.setType(getDefaultExperimentType());
            }
        }

        return new Structure(
                new SampleIdentifier("/" + space.getCode() + "/" + project.getCode() + "/"
                        + sampleToResolve.getLeft().getCode()), space, project, experiment);

    }

    public record Structure(SampleIdentifier sampleIdentifier, Space space, Project project,
                            Experiment experiment)
    {
    }

    public static ExperimentType getDefaultExperimentType()
    {
        ExperimentTypeFetchOptions experimentTypeFetchOptions =
                new ExperimentTypeFetchOptions();
        experimentTypeFetchOptions.withPropertyAssignments();
        ExperimentType experimentType = new ExperimentType();
        experimentType.setCode("COLLECTION");
        experimentType.setPropertyAssignments(new ArrayList<>());
        experimentType.setFetchOptions(experimentTypeFetchOptions);

        return experimentType;
    }

}
