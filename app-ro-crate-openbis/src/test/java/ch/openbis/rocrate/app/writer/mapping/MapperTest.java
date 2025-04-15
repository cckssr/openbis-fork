package ch.openbis.rocrate.app.writer.mapping;

import ch.eth.sis.rocrate.facade.MetadataEntry;
import ch.eth.sis.rocrate.facade.TypeProperty;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.writer.mapping.types.MapResult;
import junit.framework.TestCase;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapperTest extends TestCase
{
    @Test
    public void testEmpty()
    {

        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();
        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new HashMap<>();
        Map<ProjectIdentifier, Project> projects = new HashMap<>();
        Map<SpacePermId, Space> spaces = new HashMap<>();

        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of());
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(openBisModel);
        assertTrue(result.getSchema().getClasses().isEmpty());
        assertEquals(result.getSchema().getProperties().size(), 2);
        assertTrue(result.getMetaDataEntries().isEmpty());
        assertTrue(result.getMappingInfo().getRdfsToObjects().isEmpty());
        assertTrue(result.getMappingInfo().getRdfsPropertiesUsedIn().isEmpty());
    }

    @Test
    public void testEmptyObject()
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
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of());
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(openBisModel);

        assertEquals(1, result.getSchema().getClasses().size());
        assertEquals(2, result.getSchema().getProperties().size());
        assertTrue(result.getMetaDataEntries().isEmpty());

    }

    @Test
    public void testObjectWithProperties()
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
        PropertyType propertyType = new PropertyType();
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
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of());
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(openBisModel);

        assertEquals(result.getSchema().getClasses().size(), 1);
        TypeProperty res1 = result.getSchema().getProperties().get(0);
        assertEquals("hasNAME", res1.getId());
        assertTrue(result.getMetaDataEntries().isEmpty());

    }

    @Test
    public void testObjectWithPropertiesAndMetaData()
    {

        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();
        Mockery context = new Mockery();

        EntityTypePermId entityTypePermId = new EntityTypePermId("ENTRY", EntityKind.SAMPLE);

        PropertyAssignment propertyAssignment = new PropertyAssignment();

        PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
        fetchOptions.withPropertyType();
        fetchOptions.withSemanticAnnotations();
        PropertyType propertyType = new PropertyType();
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
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of());
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(openBisModel);

        assertEquals(result.getSchema().getClasses().size(), 1);
        assertEquals("hasNAME", result.getSchema().getProperties().get(0).getId());
        MetadataEntry metaDataEntry = result.getMetaDataEntries().get(0);
        assertTrue(metaDataEntry.getValues().containsKey("hasNAME"));
        assertTrue(result.getMappingInfo().getRdfsToObjects().get("ENTRY1")
                .contains(sampleType));

    }

    @Test
    public void testSpace()
    {
        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();

        Map<ObjectIdentifier, AbstractEntityPropertyHolder> metadata = new HashMap<>();

        Space space = new Space();
        space.setCode("SPACE");
        Map<ProjectIdentifier, Project> projects = Map.of();

        Map<SpacePermId, Space> spaces = Map.of(new SpacePermId("SPACE"), space);


        OpenBisModel openBisModel =
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of());
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(openBisModel);
        MetadataEntry entry = result.getMetaDataEntries().get(0);
        assertEquals("SPACE", entry.getId());

    }

    @Test
    public void testProject()
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
                new OpenBisModel(Map.of(), schema, spaces, projects, metadata, Map.of(), Map.of());
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(openBisModel);
        MetadataEntry entry = result.getMetaDataEntries().get(0);
        assertEquals("/SPACE/PROJECT", entry.getId());

    }

}