package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afssftp.filesystemview.impl.standard.StandardPathTranslator;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import junit.framework.TestCase;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ch.ethz.sis.afssftp.helpers.TestHelper.createRandomNode;
import static ch.ethz.sis.afssftp.helpers.TestHelper.createRandomNodeOfType;

public class SftpNodeChainTest extends TestCase {

    public void testGetLast() {
        for (int i=0; i<5; i++) {
            List<SftpNode> nodes = new ArrayList<>();
            for(int j=0; j<i; j++) {
                nodes.add(createRandomNode());
            }
            SftpNodeChain chain = new SftpNodeChain(nodes);
            assertEquals(i > 0 ? Optional.of(nodes.getLast()) : Optional.empty(), chain.getLast());
        }
    }

    public void testSize() {
        for (int i=0; i<5; i++) {
            List<SftpNode> nodes = new ArrayList<>();
            for(int j=0; j<i; j++) {
                nodes.add(createRandomNode());
            }
            SftpNodeChain chain = new SftpNodeChain(nodes);
            assertEquals(i, chain.size());
        }
    }

    public void testGet() {
        for (int i=0; i<5; i++) {
            List<SftpNode> nodes = new ArrayList<>();
            for(int j=0; j<i; j++) {
                nodes.add(createRandomNode());
            }
            SftpNodeChain chain = new SftpNodeChain(nodes);
            for(int j=0; j<i; j++) {
                assertEquals(nodes.get(j), chain.get(j));
            }
        }
    }

    public void testConcat() {
        for (int i=0; i<5; i++) {
            List<SftpNode> nodes = new ArrayList<>();
            for(int j=0; j<i; j++) {
                nodes.add(createRandomNode());
            }
            SftpNodeChain chain = new SftpNodeChain(nodes);

            SftpNode nodeToBeChained = createRandomNode();
            assertEquals(
                    Stream.concat(nodes.stream(), Stream.of(nodeToBeChained)).toList(),
                    SftpNodeChain.concat(chain, nodeToBeChained).nodes()
            );

            SftpNodeChain chainToBeChained = new SftpNodeChain(
                    IntStream.range(0, new Random().nextInt(5))
                            .mapToObj(n -> createRandomNode()).toList());

            assertEquals(
                    Stream.concat(nodes.stream(), chainToBeChained.nodes().stream()).toList(),
                    SftpNodeChain.concat(chain, chainToBeChained).nodes()
            );
        }
    }

    public void testCreateRootNode() {
        assertEquals(
                SftpNode.builder().type(SftpNode.Type.ROOT).build(),
                SftpNodeChain.createRootNode()
        );
    }

    public void testCreateRoot() {
        assertEquals(
                List.of(SftpNode.builder().type(SftpNode.Type.ROOT).build()),
                SftpNodeChain.createRoot().nodes()
        );
    }

    public void testFromSpace() {
        Space space = new Space();
        space.setCode("space-1");
        assertEquals(
            SftpNode.builder()
                .type(SftpNode.Type.SPACE)
                .identifier(Optional.of("space-1")).build(),
            SftpNodeChain.fromSpace(space)
        );
    }

    public void testFromProject() {
        Project project = new Project();
        project.setCode("project-1");
        assertEquals(
                SftpNode.builder()
                        .type(SftpNode.Type.PROJECT)
                        .identifier(Optional.of("project-1")).build(),
                SftpNodeChain.fromProject(project)
        );
    }

    public void testFromSample() {
        Sample sample = new Sample();
        sample.setPermId(new SamplePermId("sample-1"));
        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();
        fetchOptions.withProperties();
        sample.setFetchOptions(fetchOptions);
        SampleType sampleType = new SampleType();
        sampleType.setCode("NONFOLDER");
        sample.setType(sampleType);
        sample.setStringProperty("NAME", "sample name");

        assertEquals(
                SftpNode.builder()
                        .type(SftpNode.Type.SAMPLE)
                        .identifier(Optional.of("sample name(SAMPLE-1)")).build(),
                SftpNodeChain.fromSample(sample)
        );

        Sample folder = new Sample();
        folder.setPermId(new SamplePermId("folder-1"));
        folder.setFetchOptions(fetchOptions);
        SampleType folderType = new SampleType();
        folderType.setCode("FOLDER");
        folder.setType(folderType);
        folder.setStringProperty("NAME", "folder name");

        assertEquals(
                SftpNode.builder()
                        .type(SftpNode.Type.FOLDER)
                        .identifier(Optional.of("folder name(FOLDER-1)")).build(),
                SftpNodeChain.fromSample(folder)
        );
    }

    public void testFromExperiment() {
        Experiment experiment = new Experiment();
        ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
        fetchOptions.withProperties();
        experiment.setFetchOptions(fetchOptions);
        experiment.setPermId(new ExperimentPermId("experiment-1"));
        experiment.setStringProperty("NAME", "Exp name");
        assertEquals(
                SftpNode.builder()
                        .type(SftpNode.Type.EXPERIMENT)
                        .identifier(Optional.of("Exp name(EXPERIMENT-1)")).build(),
                SftpNodeChain.fromExperiment(experiment)
        );
    }

    public void testFromDataSet() {
        DataSet dataset = new DataSet();
        dataset.setPermId(new DataSetPermId("dataset-1"));
        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withProperties();
        dataset.setFetchOptions(fetchOptions);
        dataset.setStringProperty("NAME", "Dataset name");
        assertEquals(
                SftpNode.builder()
                        .type(SftpNode.Type.DATA_SET)
                        .identifier(Optional.of("Dataset name(DATASET-1)")).build(),
                SftpNodeChain.fromDataSet(dataset)
        );
    }

    public void testFromAfsFilePath() {
        List<String> afsPathSegments = List.of("abc", "cde", "efgh");
        assertEquals(
            SftpNode.builder().type(SftpNode.Type.AFS_FILE)
                .afsFilePath(afsPathSegments).build(),
            SftpNodeChain.fromAfsFilePath(afsPathSegments)
        );
    }

    public void testCreateSublevelNode() {
        String sublevelLabel = "somelabel";
        assertEquals(
                SftpNode.builder()
                        .type(SftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(sublevelLabel)).build(),
                SftpNodeChain.createSublevelNode(sublevelLabel)
        );
    }

    public void testLookUpSpaceCode() {
        SftpNode spaceNode = createRandomNodeOfType(SftpNode.Type.SPACE);

        SftpNodeChain chainWithSpace = new SftpNodeChain(
                List.of(
                        createRandomNodeOfType(SftpNode.Type.ROOT),
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.SPACE_TYPE_LABEL),
                        spaceNode,
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.PROJECT_TYPE_LABEL),
                        createRandomNodeOfType(SftpNode.Type.PROJECT)
                )
        );
        assertEquals(spaceNode.getIdentifier().get(), chainWithSpace.lookUpSpaceCode());

        SftpNodeChain chainWithoutSpace = new SftpNodeChain(
                List.of(
                        createRandomNodeOfType(SftpNode.Type.ROOT),
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.PROJECT_TYPE_LABEL),
                        createRandomNodeOfType(SftpNode.Type.PROJECT),
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.EXPERIMENT_TYPE_LABEL),
                        createRandomNodeOfType(SftpNode.Type.EXPERIMENT)
                )
        );
        assertNull(chainWithoutSpace.lookUpSpaceCode());
    }

    public void testLookUpProjectCode() {
        SftpNode projectNode = createRandomNodeOfType(SftpNode.Type.PROJECT);

        SftpNodeChain chainWithProject = new SftpNodeChain(
                List.of(
                        createRandomNodeOfType(SftpNode.Type.ROOT),
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.SPACE_TYPE_LABEL),
                        createRandomNodeOfType(SftpNode.Type.SPACE),
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.PROJECT_TYPE_LABEL),
                        projectNode
                )
        );
        assertEquals(projectNode.getIdentifier().get(), chainWithProject.lookUpProjectCode());

        SftpNodeChain chainWithoutProject = new SftpNodeChain(
                List.of(
                        createRandomNodeOfType(SftpNode.Type.ROOT),
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.SPACE_TYPE_LABEL),
                        createRandomNodeOfType(SftpNode.Type.SPACE),
                        SftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL),
                        createRandomNodeOfType(SftpNode.Type.SAMPLE)
                )
        );
        assertNull(chainWithoutProject.lookUpProjectCode());
    }
}