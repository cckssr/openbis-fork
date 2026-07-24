package ch.openbis.rocrate.app.writer.mapping;

import ch.eth.sis.rocrate.facade.MetadataEntry;
import ch.eth.sis.rocrate.facade.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.excel.v3.model.IFileInfo;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.writer.mapping.types.MapResult;
import junit.framework.TestCase;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import java.util.*;

public class MapperTest extends TestCase
{
    @Test
    public void testEmpty() throws Exception
    {

        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new HashMap<>();
        Map<ProjectIdentifier, Project> projects = new HashMap<>();
        Map<SpacePermId, Space> spaces = new HashMap<>();

        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of(),
                        Map.of(), Map.of(), Map.of());
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(openBisModel);
        assertEquals(3, result.getSchema().getClasses().size());
        assertEquals(result.getSchema().getProperties().size(), 3);
        assertTrue(result.getMetaDataEntries().isEmpty());
        assertTrue(result.getMappingInfo().getRdfsToObjects().isEmpty());
        assertTrue(result.getMappingInfo().getRdfsPropertiesUsedIn().isEmpty());
    }

    @Test
    public void testEmptyObject() throws Exception
    {

        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();
        Mockery context = new Mockery();

        EntityTypePermId entityTypePermId = new EntityTypePermId("ENTRY", EntityKind.SAMPLE);

                /*context.checking(new Expectations()
                {
                    {
                        one(permId).();
                        will(returnValue(false));
                    }
                });
*/
        IEntityType entryType = context.mock(IEntityType.class);
        context.checking(new Expectations()
        {
            {
                atLeast(1).of(entryType).getCode();
                will(returnValue("ENTRY1"));
            }

            {
                allowing(entryType).getPropertyAssignments();
                will(returnValue(new ArrayList<>()));

            }
        });
        schema.put(entityTypePermId, entryType);

        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new HashMap<>();
        Map<ProjectIdentifier, Project> projects = new HashMap<>();
        Map<SpacePermId, Space> spaces = new HashMap<>();


        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of(),
                        Map.of(), Map.of(), Map.of());
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(openBisModel);

        assertEquals(4, result.getSchema().getClasses().size());
        assertEquals(3, result.getSchema().getProperties().size());
        assertTrue(result.getMetaDataEntries().isEmpty());

    }

    @Test
    public void testObjectWithProperties() throws Exception
    {

        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();
        Mockery context = new Mockery();

        EntityTypePermId entityTypePermId = new EntityTypePermId("ENTRY", EntityKind.SAMPLE);

        PropertyAssignment propertyAssignment = new PropertyAssignment();

        IEntityType entryType = context.mock(IEntityType.class);
        propertyAssignment.setEntityType(entryType);
        PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
        fetchOptions.withPropertyType();
        fetchOptions.withSemanticAnnotations();
        ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType
                propertyType = new ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType();
        propertyType.setCode("NAME");
        propertyAssignment.setFetchOptions(fetchOptions);
        propertyAssignment.setPropertyType(propertyType);
        propertyType.setDataType(DataType.VARCHAR);
        PropertyTypeFetchOptions propertyTypeFetchOptions = new PropertyTypeFetchOptions();
        propertyTypeFetchOptions.withSemanticAnnotations();
        propertyType.setFetchOptions(propertyTypeFetchOptions);

        context.checking(new Expectations()
        {
            {
                atLeast(1).of(entryType).getCode();
                will(returnValue("ENTRY1"));

                allowing(entryType).getPropertyAssignments();
                will(returnValue(List.of(propertyAssignment)));
            }

        });

        schema.put(entityTypePermId, entryType);

        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new HashMap<>();
        Map<ProjectIdentifier, Project> projects = new HashMap<>();
        Map<SpacePermId, Space> spaces = new HashMap<>();

        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of(),
                        Map.of(), Map.of(), Map.of());
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(openBisModel);

        assertEquals(4, result.getSchema().getClasses().size());
        PropertyType res1 = result.getSchema().getProperties().get(0);
        assertEquals("openBIS:hasNAME", res1.getId());
        assertTrue(result.getMetaDataEntries().isEmpty());

    }

    @Test
    public void testObjectWithPropertiesAndMetaData() throws Exception
    {

        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();
        Mockery context = new Mockery();

        EntityTypePermId entityTypePermId = new EntityTypePermId("ENTRY", EntityKind.SAMPLE);

        PropertyAssignment propertyAssignment = new PropertyAssignment();

        PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
        fetchOptions.withPropertyType();
        fetchOptions.withSemanticAnnotations();
        ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType
                propertyType = new ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType();
        propertyType.setCode("NAME");
        propertyAssignment.setFetchOptions(fetchOptions);
        propertyAssignment.setPropertyType(propertyType);
        propertyType.setDataType(DataType.VARCHAR);
        PropertyTypeFetchOptions propertyTypeFetchOptions = new PropertyTypeFetchOptions();
        propertyTypeFetchOptions.withSemanticAnnotations();
        propertyType.setFetchOptions(propertyTypeFetchOptions);




        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new HashMap<>();
        SampleIdentifier objectIdentifier = new SampleIdentifier("JOHN", "JOHN", "ENTRY1");
        Space space = new Space();
        space.setCode("JOHN");
        Project project = new Project();
        project.setCode("JOHN");
        project.setSpace(space);
        project.setIdentifier(new ProjectIdentifier("JOHN", "JOHN"));
        Experiment experiment = new Experiment();
        experiment.setIdentifier(new ExperimentIdentifier("JOHN", "JOHN", "JOHN"));

        Sample object = new Sample();
        object.setSpace(space);
        object.setProject(project);
        object.setExperiment(experiment);
        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withType();
        sampleFetchOptions.withProperties();
        sampleFetchOptions.withChildren();
        sampleFetchOptions.withParents();
        sampleFetchOptions.withSpace();
        sampleFetchOptions.withProject();
        sampleFetchOptions.withExperiment();
        object.setFetchOptions(sampleFetchOptions);
        object.setChildren(new ArrayList<>());
        object.setParents(new ArrayList<>());
        SampleType sampleType = new SampleType();

        {
            SampleTypeFetchOptions fetchOptions1 = new SampleTypeFetchOptions();
            fetchOptions1.withPropertyAssignments();
            fetchOptions1.withSemanticAnnotations();
            sampleType.setSemanticAnnotations(new ArrayList<>());
            sampleType.setPropertyAssignments(List.of(propertyAssignment));

            sampleType.setFetchOptions(fetchOptions1);

        }

        sampleType.setCode("ENTRY1");
        object.setType(sampleType);
        object.setIdentifier(objectIdentifier);
        object.setProperties(Map.of("NAME", "AAAAAAA"));
        metadata.put(objectIdentifier, object);
        propertyAssignment.setEntityType(sampleType);
        schema.put(entityTypePermId, sampleType);

        Map<ProjectIdentifier, Project> projects = new HashMap<>();
        Map<SpacePermId, Space> spaces = new HashMap<>();

        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of(),
                        Map.of(), Map.of(), Map.of());
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(openBisModel);

        assertEquals(4, result.getSchema().getClasses().size());
        assertEquals("openBIS:hasNAME", result.getSchema().getProperties().get(0).getId());
        MetadataEntry metaDataEntry = result.getMetaDataEntries().get(0);
        assertTrue(metaDataEntry.getValues().containsKey("openBIS:hasNAME"));
        assertTrue(result.getMappingInfo().getRdfsToObjects().get("ENTRY1")
                .contains(sampleType));

    }

    @Test
    public void testSpace() throws Exception
    {
        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();

        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new HashMap<>();

        Space space = new Space();
        space.setCode("SPACE");
        Map<ProjectIdentifier, Project> projects = Map.of();

        Map<SpacePermId, Space> spaces = Map.of(new SpacePermId("SPACE"), space);


        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of(),
                        Map.of(), Map.of(), Map.of());
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(openBisModel);
        MetadataEntry entry = result.getMetaDataEntries().get(0);
        assertEquals("SPACE", entry.getId());

    }

    @Test
    public void testProject() throws Exception
    {

        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();

        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new HashMap<>();

        Project project = new Project();
        Space space = new Space();
        space.setCode("SPACE");
        project.setCode("PROJECT");
        project.setSpace(space);

        Map<ProjectIdentifier, Project> projects =
                Map.of(new ProjectIdentifier("SPACE", "PROJECT"), project);
        Map<SpacePermId, Space> spaces = new HashMap<>();

        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of(),
                        Map.of(), Map.of(), Map.of());
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(openBisModel);
        MetadataEntry entry = result.getMetaDataEntries().get(0);
        assertEquals("/SPACE/PROJECT", entry.getId());

    }

    @Test
    public void testSampleAndCollectionWithSameIdentifier() throws Exception
    {

        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();
        Mockery context = new Mockery();

        EntityTypePermId sampleTypePermId = new EntityTypePermId("DEFAULT", EntityKind.SAMPLE);
        EntityTypePermId collectionTypePermId = new EntityTypePermId("DEFAULT", EntityKind.EXPERIMENT);


        Project project = new Project();
        Space space = new Space();
        SpacePermId  spacePermId = new SpacePermId("DEFAULT");
        space.setPermId(spacePermId);
        space.setCode("DEFAULT");
        Map<SpacePermId, Space> spaces = Map.of(spacePermId, space);

        project.setCode("DEFAULT");
        ProjectIdentifier projectId = new ProjectIdentifier("DEFAULT", "DEFAULT");
        project.setIdentifier(projectId);
        project.setSpace(space);
        Map<ProjectIdentifier, Project> projects =
                Map.of(projectId, project);

        ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
        experimentFetchOptions.withProperties();
        experimentFetchOptions.withType();

        Experiment experiment = new Experiment();
        experiment.setCode("DEFAULT");
        experiment.setProject(project);
        experiment.setFetchOptions(experimentFetchOptions);

        ExperimentType experimentType = new ExperimentType();
        String experimenttype = "EXPERIMENTTYPE";
        experimentType.setCode(experimenttype);
        experimentType.setPropertyAssignments(new ArrayList<>());
        ExperimentTypeFetchOptions experimentTypeFetchOptions = new ExperimentTypeFetchOptions();
        experimentTypeFetchOptions.withPropertyAssignments();
        experimentType.setFetchOptions(experimentTypeFetchOptions);

        experiment.setType(experimentType);
        experimentType.setPermId(new EntityTypePermId(experimenttype, EntityKind.EXPERIMENT));

        ExperimentIdentifier experimentIdentifier = new ExperimentIdentifier("DEFAULT", "DEFAULT", "DEFAULT");
        experiment.setIdentifier(experimentIdentifier);
        experiment.setProperties(new HashMap<>());


        SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
        sampleTypeFetchOptions.withPropertyAssignments();
        sampleTypeFetchOptions.withSemanticAnnotations();

        SampleType sampleType = new SampleType();
        sampleType.setCode("DEFAULT");
        sampleType.setFetchOptions(sampleTypeFetchOptions);
        sampleType.setPropertyAssignments(List.of());
        sampleType.setSemanticAnnotations(List.of());


        Sample sample = new Sample();
        sample.setCode("DEFAULT");
        SampleIdentifier sampleIdentifier = new SampleIdentifier("/DEFAULT/DEFAULT/DEFAULT");
        sample.setIdentifier(sampleIdentifier);
        sample.setExperiment(experiment);
        sample.setProject(project);
        sample.setSpace(space);
        sample.setType(sampleType);
        sample.setParents(List.of());
        sample.setChildren(List.of());
        sample.setProperties(new HashMap<>());

        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withSpace();
        sampleFetchOptions.withExperiment();
        sampleFetchOptions.withProject();
        sampleFetchOptions.withProperties();
        sampleFetchOptions.withParents();
        sampleFetchOptions.withChildren();
        sampleFetchOptions.withType().withPropertyAssignments();
        sample.setFetchOptions(sampleFetchOptions);

        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new HashMap<>();
        metadata.put(sampleIdentifier, sample);
        metadata.put(experimentIdentifier, experiment);


        Map<ObjectIdentifier, List<IFileInfo>> files = new LinkedHashMap<>();

        byte[] contents = new byte[]{ 0 };

        OpenBisModel.FileInfoContents file = new OpenBisModel.FileInfoContents(
                sampleIdentifier.getIdentifier(),
                "hierarchy/DEFAULT/DEFAULT/DEFAULT/file.txt",
                contents,
                "hierarchy/DEFAULT/DEFAULT/DEFAULT/data/file.txt"
                );

        files.put(sampleIdentifier, List.of(file));

        Map<ObjectIdentifier, List<IFileInfo>> imageFiles =
                new LinkedHashMap<>();

        IEntityType entryType = context.mock(IEntityType.class);
        context.checking(new Expectations()
        {
            {
                atLeast(1).of(entryType).getCode();
                will(returnValue("DEFAULT"));
            }

            {
                allowing(entryType).getPropertyAssignments();
                will(returnValue(new ArrayList<>()));
            }
        });
        schema.put(sampleTypePermId, sampleType);
        schema.put(collectionTypePermId, entryType);
        schema.put(experimentType.getPermId(), experimentType);

        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of(),
                        Map.of(), files, imageFiles);
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(openBisModel);

        assertEquals(5, result.getSchema().getClasses().size());
        assertEquals(3, result.getSchema().getProperties().size());
        assertEquals(4, result.getMetaDataEntries().size());
        assertEquals(1, result.getFiles().size());
    }

}