package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afssftp.filesystemview.impl.standard.StandardPathTranslator;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import junit.framework.TestCase;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ch.ethz.sis.afssftp.helpers.TestHelper.createRandomNode;
import static ch.ethz.sis.afssftp.helpers.TestHelper.createRandomNodeOfType;

public class OpenBISSftpNodeChainTest extends TestCase {

    public void testGetLast() {
        for (int i=0; i<5; i++) {
            List<OpenBISSftpNode> nodes = new ArrayList<>();
            for(int j=0; j<i; j++) {
                nodes.add(createRandomNode());
            }
            OpenBISSftpNodeChain chain = new OpenBISSftpNodeChain(nodes);
            assertEquals(i > 0 ? Optional.of(nodes.getLast()) : Optional.empty(), chain.getLast());
        }
    }

    public void testSize() {
        for (int i=0; i<5; i++) {
            List<OpenBISSftpNode> nodes = new ArrayList<>();
            for(int j=0; j<i; j++) {
                nodes.add(createRandomNode());
            }
            OpenBISSftpNodeChain chain = new OpenBISSftpNodeChain(nodes);
            assertEquals(i, chain.size());
        }
    }

    public void testGet() {
        for (int i=0; i<5; i++) {
            List<OpenBISSftpNode> nodes = new ArrayList<>();
            for(int j=0; j<i; j++) {
                nodes.add(createRandomNode());
            }
            OpenBISSftpNodeChain chain = new OpenBISSftpNodeChain(nodes);
            for(int j=0; j<i; j++) {
                assertEquals(nodes.get(j), chain.get(j));
            }
        }
    }

    public void testConcat() {
        for (int i=0; i<5; i++) {
            List<OpenBISSftpNode> nodes = new ArrayList<>();
            for(int j=0; j<i; j++) {
                nodes.add(createRandomNode());
            }
            OpenBISSftpNodeChain chain = new OpenBISSftpNodeChain(nodes);

            OpenBISSftpNode nodeToBeChained = createRandomNode();
            assertEquals(
                    Stream.concat(nodes.stream(), Stream.of(nodeToBeChained)).toList(),
                    OpenBISSftpNodeChain.concat(chain, nodeToBeChained).nodes()
            );

            OpenBISSftpNodeChain chainToBeChained = new OpenBISSftpNodeChain(
                    IntStream.range(0, new Random().nextInt(5))
                            .mapToObj(n -> createRandomNode()).toList());

            assertEquals(
                    Stream.concat(nodes.stream(), chainToBeChained.nodes().stream()).toList(),
                    OpenBISSftpNodeChain.concat(chain, chainToBeChained).nodes()
            );
        }
    }

    public void testCreateRootNode() {
        assertEquals(
                OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build(),
                OpenBISSftpNodeChain.createRootNode()
        );
    }

    public void testCreateRoot() {
        assertEquals(
                List.of(OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.ROOT).build()),
                OpenBISSftpNodeChain.createRoot().nodes()
        );
    }

    public void testFromSpace() {
        Space space = new Space();
        space.setCode("space-1");
        assertEquals(
            OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.SPACE)
                .identifier(Optional.of("space-1")).build(),
            OpenBISSftpNodeChain.fromSpace(space)
        );
    }

    public void testFromProject() {
        Project project = new Project();
        project.setCode("project-1");
        assertEquals(
                OpenBISSftpNode.builder()
                        .type(OpenBISSftpNode.Type.PROJECT)
                        .identifier(Optional.of("project-1")).build(),
                OpenBISSftpNodeChain.fromProject(project)
        );
    }

    public void testFromSample() {
        Sample sample = new Sample();
        sample.setCode("sample-1");
        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();
        sample.setFetchOptions(fetchOptions);
        SampleType sampleType = new SampleType();
        sampleType.setCode("NONFOLDER");
        sample.setType(sampleType);

        assertEquals(
                OpenBISSftpNode.builder()
                        .type(OpenBISSftpNode.Type.SAMPLE)
                        .identifier(Optional.of("sample-1")).build(),
                OpenBISSftpNodeChain.fromSample(sample)
        );

        Sample folder = new Sample();
        folder.setCode("folder-1");
        folder.setFetchOptions(fetchOptions);
        SampleType folderType = new SampleType();
        folderType.setCode("FOLDER");
        folder.setType(folderType);

        assertEquals(
                OpenBISSftpNode.builder()
                        .type(OpenBISSftpNode.Type.FOLDER)
                        .identifier(Optional.of("folder-1")).build(),
                OpenBISSftpNodeChain.fromSample(folder)
        );
    }

    public void testFromExperiment() {
        Experiment experiment = new Experiment();
        experiment.setCode("experiment-1");
        assertEquals(
                OpenBISSftpNode.builder()
                        .type(OpenBISSftpNode.Type.EXPERIMENT)
                        .identifier(Optional.of("experiment-1")).build(),
                OpenBISSftpNodeChain.fromExperiment(experiment)
        );
    }

    public void testFromDataSet() {
        DataSet dataset = new DataSet();
        dataset.setCode("dataset-1");
        assertEquals(
                OpenBISSftpNode.builder()
                        .type(OpenBISSftpNode.Type.DATA_SET)
                        .identifier(Optional.of("dataset-1")).build(),
                OpenBISSftpNodeChain.fromDataSet(dataset)
        );
    }

    public void testFromAfsFilePath() {
        List<String> afsPathSegments = List.of("abc", "cde", "efgh");
        assertEquals(
            OpenBISSftpNode.builder().type(OpenBISSftpNode.Type.AFS_FILE)
                .afsFilePath(afsPathSegments).build(),
            OpenBISSftpNodeChain.fromAfsFilePath(afsPathSegments)
        );
    }

    public void testCreateSublevelNode() {
        String sublevelLabel = "somelabel";
        assertEquals(
                OpenBISSftpNode.builder()
                        .type(OpenBISSftpNode.Type.SUBLEVEL)
                        .identifier(Optional.of(sublevelLabel)).build(),
                OpenBISSftpNodeChain.createSublevelNode(sublevelLabel)
        );
    }

    public void testLookUpSpaceCode() {
        OpenBISSftpNode spaceNode = createRandomNodeOfType(OpenBISSftpNode.Type.SPACE);

        OpenBISSftpNodeChain chainWithSpace = new OpenBISSftpNodeChain(
                List.of(
                        createRandomNodeOfType(OpenBISSftpNode.Type.ROOT),
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SPACE_TYPE_LABEL),
                        spaceNode,
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.PROJECT_TYPE_LABEL),
                        createRandomNodeOfType(OpenBISSftpNode.Type.PROJECT)
                )
        );
        assertEquals(spaceNode.getIdentifier().get(), chainWithSpace.lookUpSpaceCode());

        OpenBISSftpNodeChain chainWithoutSpace = new OpenBISSftpNodeChain(
                List.of(
                        createRandomNodeOfType(OpenBISSftpNode.Type.ROOT),
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.PROJECT_TYPE_LABEL),
                        createRandomNodeOfType(OpenBISSftpNode.Type.PROJECT),
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.EXPERIMENT_TYPE_LABEL),
                        createRandomNodeOfType(OpenBISSftpNode.Type.EXPERIMENT)
                )
        );
        assertNull(chainWithoutSpace.lookUpSpaceCode());
    }

    public void testLookUpProjectCode() {
        OpenBISSftpNode projectNode = createRandomNodeOfType(OpenBISSftpNode.Type.PROJECT);

        OpenBISSftpNodeChain chainWithProject = new OpenBISSftpNodeChain(
                List.of(
                        createRandomNodeOfType(OpenBISSftpNode.Type.ROOT),
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SPACE_TYPE_LABEL),
                        createRandomNodeOfType(OpenBISSftpNode.Type.SPACE),
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.PROJECT_TYPE_LABEL),
                        projectNode
                )
        );
        assertEquals(projectNode.getIdentifier().get(), chainWithProject.lookUpProjectCode());

        OpenBISSftpNodeChain chainWithoutProject = new OpenBISSftpNodeChain(
                List.of(
                        createRandomNodeOfType(OpenBISSftpNode.Type.ROOT),
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SPACE_TYPE_LABEL),
                        createRandomNodeOfType(OpenBISSftpNode.Type.SPACE),
                        OpenBISSftpNodeChain.createSublevelNode(StandardPathTranslator.SAMPLE_TYPE_LABEL),
                        createRandomNodeOfType(OpenBISSftpNode.Type.SAMPLE)
                )
        );
        assertNull(chainWithoutProject.lookUpProjectCode());
    }
}