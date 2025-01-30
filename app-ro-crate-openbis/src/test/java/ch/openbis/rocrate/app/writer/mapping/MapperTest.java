package ch.openbis.rocrate.app.writer.mapping;

import ch.eth.sis.rocrate.facade.MetadataEntry;
import ch.eth.sis.rocrate.facade.TypeProperty;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntity;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.openbis.rocrate.app.parser.helper.SemanticAnnotationHelper;
import junit.framework.TestCase;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import ch.openbis.rocrate.app.parser.results.ParseResult;
import ch.openbis.rocrate.app.writer.mapping.types.MapResult;

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
        Map<ObjectIdentifier, AbstractEntity> metadata = new HashMap<>();
        Map<ProjectIdentifier, Project> projects = new HashMap<>();
        Map<String, Space> spaces = new HashMap<>();

        SemanticAnnotationHelper.SemanticAnnotationByKind semanticAnnotationByKind = new SemanticAnnotationHelper.SemanticAnnotationByKind(Map.of(), Map.of(), Map.of());
        ParseResult parseResult = new ParseResult(schema, metadata, projects, spaces, semanticAnnotationByKind);
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(parseResult);
        assertTrue(result.getSchema().getClasses().isEmpty());
        assertTrue(result.getSchema().getProperties().isEmpty());
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

        Map<ObjectIdentifier, AbstractEntity> metadata = new HashMap<>();
        Map<ProjectIdentifier, Project> projects = new HashMap<>();
        Map<String, Space> spaces = new HashMap<>();


        SemanticAnnotationHelper.SemanticAnnotationByKind semanticAnnotationByKind = new SemanticAnnotationHelper.SemanticAnnotationByKind(Map.of(), Map.of(), Map.of());

        ParseResult parseResult = new ParseResult(schema, metadata, projects, spaces, semanticAnnotationByKind);
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(parseResult);

        assertEquals(1, result.getSchema().getClasses().size());
        assertEquals(0, result.getSchema().getProperties().size());
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
        PropertyType propertyType = new PropertyType();
        propertyType.setCode("NAME");
        propertyAssignment.setFetchOptions(fetchOptions);
        propertyAssignment.setPropertyType(propertyType);
        propertyType.setDataType(DataType.VARCHAR);

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

        Map<ObjectIdentifier, AbstractEntity> metadata = new HashMap<>();
        Map<ProjectIdentifier, Project> projects = new HashMap<>();
        Map<String, Space> spaces = new HashMap<>();
        SemanticAnnotationHelper.SemanticAnnotationByKind semanticAnnotationByKind = new SemanticAnnotationHelper.SemanticAnnotationByKind(Map.of(), Map.of(), Map.of());

        ParseResult parseResult = new ParseResult(schema, metadata, projects, spaces, semanticAnnotationByKind);
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(parseResult);

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

        IEntityType entryType = context.mock(IEntityType.class);
        propertyAssignment.setEntityType(entryType);
        PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
        fetchOptions.withPropertyType();
        PropertyType propertyType = new PropertyType();
        propertyType.setCode("NAME");
        propertyAssignment.setFetchOptions(fetchOptions);
        propertyAssignment.setPropertyType(propertyType);
        propertyType.setDataType(DataType.VARCHAR);

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

        Map<ObjectIdentifier, AbstractEntity> metadata = new HashMap<>();
        SampleIdentifier objectIdentifier = new SampleIdentifier("JOHN", "JOHN", "ENTRY1");
        Sample object = new Sample();
        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withType();
        sampleFetchOptions.withProperties();
        sampleFetchOptions.withChildren();
        sampleFetchOptions.withParents();
        object.setFetchOptions(sampleFetchOptions);
        object.setChildren(new ArrayList<>());
        object.setParents(new ArrayList<>());
        SampleType sampleType = new SampleType();
        sampleType.setCode("ENTRY1");
        object.setType(sampleType);
        object.setIdentifier(objectIdentifier);
        object.setProperties(Map.of("NAME", "AAAAAAA"));
        metadata.put(objectIdentifier, object);

        Map<ProjectIdentifier, Project> projects = new HashMap<>();
        Map<String, Space> spaces = new HashMap<>();
        SemanticAnnotationHelper.SemanticAnnotationByKind semanticAnnotationByKind = new SemanticAnnotationHelper.SemanticAnnotationByKind(Map.of(), Map.of(), Map.of());

        ParseResult parseResult = new ParseResult(schema, metadata, projects, spaces, semanticAnnotationByKind);
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(parseResult);

        assertEquals(result.getSchema().getClasses().size(), 1);
        assertEquals("hasNAME", result.getSchema().getProperties().get(0).getId());
        MetadataEntry metaDataEntry = result.getMetaDataEntries().get(0);
        assertTrue(metaDataEntry.getValues().containsKey("hasNAME"));
        assertTrue(result.getMappingInfo().getRdfsToObjects().get("ENTRY1")
                .contains(entryType));

    }

    @Test
    public void testSpace()
    {
        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();

        Map<ObjectIdentifier, AbstractEntity> metadata = new HashMap<>();

        Space space = new Space();
        space.setCode("SPACE");
        Map<ProjectIdentifier, Project> projects = Map.of();

        Map<String, Space> spaces = Map.of("SPACE", space);

        SemanticAnnotationHelper.SemanticAnnotationByKind semanticAnnotationByKind = new SemanticAnnotationHelper.SemanticAnnotationByKind(Map.of(), Map.of(), Map.of());

        ParseResult parseResult = new ParseResult(schema, metadata, projects, spaces, semanticAnnotationByKind);
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(parseResult);
        MetadataEntry entry = result.getMetaDataEntries().get(0);
        assertEquals("SPACE", entry.getId());

    }

    @Test
    public void testProject()
    {

        Map<EntityTypePermId, IEntityType> schema = new HashMap<>();

        Map<ObjectIdentifier, AbstractEntity> metadata = new HashMap<>();

        Project project = new Project();
        Space space = new Space();
        space.setCode("SPACE");
        project.setCode("PROJECT");
        project.setSpace(space);

        Map<ProjectIdentifier, Project> projects =
                Map.of(new ProjectIdentifier("SPACE", "PROJECT"), project);
        Map<String, Space> spaces = new HashMap<>();
        SemanticAnnotationHelper.SemanticAnnotationByKind semanticAnnotationByKind = new SemanticAnnotationHelper.SemanticAnnotationByKind(Map.of(), Map.of(), Map.of());

        ParseResult parseResult = new ParseResult(schema, metadata, projects, spaces, semanticAnnotationByKind);
        Mapper mapper = new Mapper();
        MapResult result = mapper.transform(parseResult);
        MetadataEntry entry = result.getMetaDataEntries().get(0);
        assertEquals("/SPACE/PROJECT", entry.getId());

    }

}