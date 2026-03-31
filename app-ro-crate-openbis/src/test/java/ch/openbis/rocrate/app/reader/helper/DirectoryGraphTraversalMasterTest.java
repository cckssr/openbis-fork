package ch.openbis.rocrate.app.reader.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.AbstractEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DirectoryGraphTraversalMasterTest extends TestCase
{

    public static final String PART = "PART";

    @Test
    public void testEmpty()
    {
        DirectoryTraversal directoryTraversal =
                new DirectoryTraversal();

        RoCrate.RoCrateBuilder builder = new RoCrate.RoCrateBuilder();
        AbstractEntity entity;

        ContextualEntity build;
        {
            ContextualEntity.ContextualEntityBuilder contextual =
                    new ContextualEntity.ContextualEntityBuilder();

            contextual.addType("openBIS:test");

            build = contextual.build();
            entity = build;
            builder.addContextualEntity(build);

        }

        Sample sample = getEmptySample();

        DirectoryTraversal.TraversalResult result =
                directoryTraversal.findAllFiles(entity, builder.build(),
                        sample);
        Assert.assertEquals(0, result.files().size());
        Assert.assertTrue(result.missingEntitites().isEmpty());
    }

    private static Sample getEmptySample()
    {
        Sample sample = new Sample();
        SampleFetchOptions sampleFetchOption = new SampleFetchOptions();
        {
            sampleFetchOption.withProperties();
            sampleFetchOption.withType();
            sample.setFetchOptions(sampleFetchOption);

        }
        SampleType sampleType = new SampleType();
        sample.setType(sampleType);

        {
            SampleTypeFetchOptions fetchOptions = new SampleTypeFetchOptions();
            fetchOptions.withPropertyAssignments();
            sampleType.setFetchOptions(fetchOptions);
            sampleType.setPropertyAssignments(new ArrayList<>());

        }


        return sample;
    }

    @Test
    public void testOneFile()
    {
        DirectoryTraversal directoryTraversal =
                new DirectoryTraversal();

        RoCrate.RoCrateBuilder builder = new RoCrate.RoCrateBuilder();
        AbstractEntity entity;

        ContextualEntity build;
        String filerooni = "filerooni";
        {
            ContextualEntity.ContextualEntityBuilder contextual =
                    new ContextualEntity.ContextualEntityBuilder();

            contextual.setId("testerooni");
            contextual.addIdProperty("hasPart", filerooni);
            contextual.addType("openBIS:test");

            build = contextual.build();
            entity = build;
            builder.addContextualEntity(build);

        }

        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId(filerooni);
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());

        }

        DirectoryTraversal.TraversalResult result =
                directoryTraversal.findAllFiles(entity, builder.build(),
                        getEmptySample());
        Assert.assertEquals(1, result.files().size());
        Assert.assertTrue(result.missingEntitites().isEmpty());


    }

    @Test
    public void testOneMissingFile()
    {
        DirectoryTraversal directoryTraversal =
                new DirectoryTraversal();

        RoCrate.RoCrateBuilder builder = new RoCrate.RoCrateBuilder();
        AbstractEntity entity;

        ContextualEntity build;
        String filerooni = "filerooni";
        {
            ContextualEntity.ContextualEntityBuilder contextual =
                    new ContextualEntity.ContextualEntityBuilder();

            contextual.setId("testerooni");
            contextual.addIdProperty("hasPart", filerooni);
            contextual.addType("openBIS:test");

            build = contextual.build();
            entity = build;
            builder.addContextualEntity(build);

        }

        DirectoryTraversal.TraversalResult result =
                directoryTraversal.findAllFiles(entity, builder.build(),
                        getEmptySample());
        Assert.assertEquals(0, result.files().size());
        Assert.assertEquals(1, result.missingEntitites().size());

    }

    @Test
    public void testOneDirectoryWithTwoFiles()
    {
        DirectoryTraversal directoryTraversal =
                new DirectoryTraversal();

        RoCrate.RoCrateBuilder builder = new RoCrate.RoCrateBuilder();
        AbstractEntity entity;

        ContextualEntity build;
        {
            ContextualEntity.ContextualEntityBuilder contextual =
                    new ContextualEntity.ContextualEntityBuilder();

            contextual.addType("openBIS:test");
            contextual.addIdProperty("hasPart", "dataset1");

            build = contextual.build();
            entity = build;
            builder.addContextualEntity(build);

        }

        {
            ContextualEntity.ContextualEntityBuilder contextualEntityBuilder =
                    new ContextualEntity.ContextualEntityBuilder();
            contextualEntityBuilder.addType("Dataset");
            contextualEntityBuilder.setId("dataset1");
            contextualEntityBuilder.addIdProperty("hasPart", "file1");
            contextualEntityBuilder.addIdProperty("hasPart", "file2");

            builder.addContextualEntity(contextualEntityBuilder.build());
        }
        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file1");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }
        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file2");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }

        DirectoryTraversal.TraversalResult result =
                directoryTraversal.findAllFiles(entity, builder.build(),
                        getEmptySample());
        Assert.assertEquals(2, result.files().size());
        Assert.assertTrue(result.missingEntitites().isEmpty());

    }

    @Test
    public void testATree()
    {
        DirectoryTraversal directoryTraversal =
                new DirectoryTraversal();

        RoCrate.RoCrateBuilder builder = new RoCrate.RoCrateBuilder();
        AbstractEntity entity;

        ContextualEntity build;
        {
            ContextualEntity.ContextualEntityBuilder contextual =
                    new ContextualEntity.ContextualEntityBuilder();

            contextual.addType("openBIS:test");
            contextual.addIdProperty("hasPart", "dataset1");

            build = contextual.build();
            entity = build;
            builder.addContextualEntity(build);

        }

        {
            ContextualEntity.ContextualEntityBuilder contextualEntityBuilder =
                    new ContextualEntity.ContextualEntityBuilder();
            contextualEntityBuilder.addType("Dataset");
            contextualEntityBuilder.setId("dataset1");
            contextualEntityBuilder.addIdProperty("hasPart", "dataset2");
            contextualEntityBuilder.addIdProperty("hasPart", "dataset3");

            builder.addContextualEntity(contextualEntityBuilder.build());
        }
        {
            ContextualEntity.ContextualEntityBuilder contextualEntityBuilder =
                    new ContextualEntity.ContextualEntityBuilder();
            contextualEntityBuilder.addType("Dataset");
            contextualEntityBuilder.setId("dataset2");
            contextualEntityBuilder.addIdProperty("hasPart", "file1");
            contextualEntityBuilder.addIdProperty("hasPart", "file2");

            builder.addContextualEntity(contextualEntityBuilder.build());
        }
        {
            ContextualEntity.ContextualEntityBuilder contextualEntityBuilder =
                    new ContextualEntity.ContextualEntityBuilder();
            contextualEntityBuilder.addType("Dataset");
            contextualEntityBuilder.setId("dataset3");
            contextualEntityBuilder.addIdProperty("hasPart", "file3");

            builder.addContextualEntity(contextualEntityBuilder.build());
        }

        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file1");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }
        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file2");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }
        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file3");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }

        DirectoryTraversal.TraversalResult result =
                directoryTraversal.findAllFiles(entity, builder.build(),
                        getEmptySample());
        Assert.assertEquals(3, result.files().size());
        Assert.assertTrue(result.missingEntitites().isEmpty());

    }

    @Test
    public void testEmptyAnnotatedProperty()
    {
        DirectoryTraversal directoryTraversal =
                new DirectoryTraversal();

        RoCrate.RoCrateBuilder builder = new RoCrate.RoCrateBuilder();
        AbstractEntity entity;

        ContextualEntity build;
        {
            ContextualEntity.ContextualEntityBuilder contextual =
                    new ContextualEntity.ContextualEntityBuilder();

            contextual.addType("openBIS:test");
            contextual.addIdProperty("hasPart", "dataset1");

            build = contextual.build();
            entity = build;
            builder.addContextualEntity(build);

        }

        {
            ContextualEntity.ContextualEntityBuilder contextualEntityBuilder =
                    new ContextualEntity.ContextualEntityBuilder();
            contextualEntityBuilder.addType("Dataset");
            contextualEntityBuilder.setId("dataset1");
            contextualEntityBuilder.addIdProperty("hasPart", "dataset2");
            contextualEntityBuilder.addIdProperty("hasPart", "dataset3");

            builder.addContextualEntity(contextualEntityBuilder.build());
        }
        {
            ContextualEntity.ContextualEntityBuilder contextualEntityBuilder =
                    new ContextualEntity.ContextualEntityBuilder();
            contextualEntityBuilder.addType("Dataset");
            contextualEntityBuilder.setId("dataset2");
            contextualEntityBuilder.addIdProperty("hasPart", "file1");
            contextualEntityBuilder.addIdProperty("hasPart", "file2");

            builder.addContextualEntity(contextualEntityBuilder.build());
        }
        {
            ContextualEntity.ContextualEntityBuilder contextualEntityBuilder =
                    new ContextualEntity.ContextualEntityBuilder();
            contextualEntityBuilder.addType("Dataset");
            contextualEntityBuilder.setId("dataset3");
            contextualEntityBuilder.addIdProperty("hasPart", "file3");

            builder.addContextualEntity(contextualEntityBuilder.build());
        }

        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file1");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }
        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file2");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }
        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file3");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }

        Sample sample = getEmptySample();
        PropertyAssignment propertyAssignment = new PropertyAssignment();
        {
            PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
            fetchOptions.withPropertyType();
            fetchOptions.withSemanticAnnotations();
            propertyAssignment.setFetchOptions(fetchOptions);
            propertyAssignment.setSemanticAnnotations(List.of());

        }
        {
            PropertyType propertyType = new PropertyType();

            PropertyTypeFetchOptions fetchOptions = new PropertyTypeFetchOptions();
            fetchOptions.withSemanticAnnotations();

            propertyType.setFetchOptions(fetchOptions);

            propertyType.setCode(PART);

            SemanticAnnotation e1 = new SemanticAnnotation();
            e1.setPredicateOntologyVersion("https://schema.org/hasPart");
            e1.setPredicateAccessionId("https://schema.org/hasPart");
            e1.setPredicateOntologyVersion("https://schema.org/hasPart");

            propertyType.setSemanticAnnotations(List.of(e1));

            propertyAssignment.setPropertyType(propertyType);
        }
        sample.getType().setPropertyAssignments(List.of(propertyAssignment));
        DirectoryTraversal.TraversalResult result =
                directoryTraversal.findAllFiles(entity, builder.build(),
                        sample);
        Assert.assertEquals(3, result.files().size());
        Assert.assertTrue(result.missingEntitites().isEmpty());

    }

    @Test
    public void testAnnotatedProperty()
    {
        DirectoryTraversal directoryTraversal =
                new DirectoryTraversal();

        RoCrate.RoCrateBuilder builder = new RoCrate.RoCrateBuilder();
        AbstractEntity entity;

        ContextualEntity build;
        {
            ContextualEntity.ContextualEntityBuilder contextual =
                    new ContextualEntity.ContextualEntityBuilder();

            contextual.addType("openBIS:test");
            contextual.addIdProperty("hasPart", "dataset1");
            contextual.addIdProperty(PART, "file4");

            build = contextual.build();
            entity = build;
            builder.addContextualEntity(build);

        }

        {
            ContextualEntity.ContextualEntityBuilder contextualEntityBuilder =
                    new ContextualEntity.ContextualEntityBuilder();
            contextualEntityBuilder.addType("Dataset");
            contextualEntityBuilder.setId("dataset1");
            contextualEntityBuilder.addIdProperty("hasPart", "dataset2");
            contextualEntityBuilder.addIdProperty("hasPart", "dataset3");

            builder.addContextualEntity(contextualEntityBuilder.build());
        }
        {
            ContextualEntity.ContextualEntityBuilder contextualEntityBuilder =
                    new ContextualEntity.ContextualEntityBuilder();
            contextualEntityBuilder.addType("Dataset");
            contextualEntityBuilder.setId("dataset2");
            contextualEntityBuilder.addIdProperty("hasPart", "file1");
            contextualEntityBuilder.addIdProperty("hasPart", "file2");

            builder.addContextualEntity(contextualEntityBuilder.build());
        }
        {
            ContextualEntity.ContextualEntityBuilder contextualEntityBuilder =
                    new ContextualEntity.ContextualEntityBuilder();
            contextualEntityBuilder.addType("Dataset");
            contextualEntityBuilder.setId("dataset3");
            contextualEntityBuilder.addIdProperty("hasPart", "file3");

            builder.addContextualEntity(contextualEntityBuilder.build());
        }

        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file1");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }
        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file2");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }
        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file3");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }
        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file4");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }

        Sample sample = getEmptySample();
        PropertyAssignment propertyAssignment = new PropertyAssignment();
        {
            PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
            fetchOptions.withPropertyType();
            fetchOptions.withSemanticAnnotations();
            propertyAssignment.setFetchOptions(fetchOptions);
            propertyAssignment.setSemanticAnnotations(List.of());

        }
        {
            PropertyType propertyType = new PropertyType();

            PropertyTypeFetchOptions fetchOptions = new PropertyTypeFetchOptions();
            fetchOptions.withSemanticAnnotations();

            propertyType.setFetchOptions(fetchOptions);

            propertyType.setCode(PART);
            propertyType.setLabel(PART);

            SemanticAnnotation e1 = new SemanticAnnotation();
            e1.setPredicateOntologyVersion("https://schema.org/hasPart");
            e1.setPredicateAccessionId("https://schema.org/hasPart");
            e1.setPredicateOntologyVersion("https://schema.org/hasPart");

            propertyType.setSemanticAnnotations(List.of(e1));

            propertyAssignment.setPropertyType(propertyType);
        }
        sample.getType().setPropertyAssignments(List.of(propertyAssignment));
        sample.setProperties(Map.of(PART, "{\"@id\": \"file4\"}"));
        List<AbstractEntity> result =
                directoryTraversal.findAllFiles(entity, builder.build(),
                        sample).files();
        Assert.assertEquals(4, result.size());

    }

    @Test
    public void testAnnotatedPropertyWithMissingStuff()
    {
        DirectoryTraversal directoryTraversal =
                new DirectoryTraversal();

        RoCrate.RoCrateBuilder builder = new RoCrate.RoCrateBuilder();
        AbstractEntity entity;

        ContextualEntity build;
        {
            ContextualEntity.ContextualEntityBuilder contextual =
                    new ContextualEntity.ContextualEntityBuilder();

            contextual.addType("openBIS:test");
            contextual.addIdProperty("hasPart", "dataset1");
            contextual.addIdProperty(PART, "file4");

            build = contextual.build();
            entity = build;
            builder.addContextualEntity(build);

        }

        {
            ContextualEntity.ContextualEntityBuilder contextualEntityBuilder =
                    new ContextualEntity.ContextualEntityBuilder();
            contextualEntityBuilder.addType("Dataset");
            contextualEntityBuilder.setId("dataset1");
            contextualEntityBuilder.addIdProperty("hasPart", "dataset2");
            contextualEntityBuilder.addIdProperty("hasPart", "dataset3");

            builder.addContextualEntity(contextualEntityBuilder.build());
        }
        {
            ContextualEntity.ContextualEntityBuilder contextualEntityBuilder =
                    new ContextualEntity.ContextualEntityBuilder();
            contextualEntityBuilder.addType("Dataset");
            contextualEntityBuilder.setId("dataset2");
            contextualEntityBuilder.addIdProperty("hasPart", "file1");
            contextualEntityBuilder.addIdProperty("hasPart", "file2");

            builder.addContextualEntity(contextualEntityBuilder.build());
        }
        {
            ContextualEntity.ContextualEntityBuilder contextualEntityBuilder =
                    new ContextualEntity.ContextualEntityBuilder();
            contextualEntityBuilder.addType("Dataset");
            contextualEntityBuilder.setId("dataset3");
            contextualEntityBuilder.addIdProperty("hasPart", "file3");

            builder.addContextualEntity(contextualEntityBuilder.build());
        }

        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file1");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }
        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file2");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }
        {
            DataEntity.DataEntityBuilder dataEntityBuilder = new DataEntity.DataEntityBuilder();
            dataEntityBuilder.setId("file3");
            dataEntityBuilder.addType("File");
            builder.addDataEntity(dataEntityBuilder.build());
        }

        Sample sample = getEmptySample();
        PropertyAssignment propertyAssignment = new PropertyAssignment();
        {
            PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
            fetchOptions.withPropertyType();
            fetchOptions.withSemanticAnnotations();
            propertyAssignment.setFetchOptions(fetchOptions);
            propertyAssignment.setSemanticAnnotations(List.of());

        }
        {
            PropertyType propertyType = new PropertyType();

            PropertyTypeFetchOptions fetchOptions = new PropertyTypeFetchOptions();
            fetchOptions.withSemanticAnnotations();

            propertyType.setFetchOptions(fetchOptions);

            propertyType.setCode(PART);
            propertyType.setLabel(PART);

            SemanticAnnotation e1 = new SemanticAnnotation();
            e1.setPredicateOntologyVersion("https://schema.org/hasPart");
            e1.setPredicateAccessionId("https://schema.org/hasPart");
            e1.setPredicateOntologyVersion("https://schema.org/hasPart");

            propertyType.setSemanticAnnotations(List.of(e1));

            propertyAssignment.setPropertyType(propertyType);
        }
        sample.getType().setPropertyAssignments(List.of(propertyAssignment));
        sample.setProperties(Map.of(PART, "{\"@id\": \"file4\"}"));
        DirectoryTraversal.TraversalResult result =
                directoryTraversal.findAllFiles(entity, builder.build(),
                        sample);
        Assert.assertEquals(3, result.files().size());
        Assert.assertEquals(1, result.missingEntitites().size());

    }

}