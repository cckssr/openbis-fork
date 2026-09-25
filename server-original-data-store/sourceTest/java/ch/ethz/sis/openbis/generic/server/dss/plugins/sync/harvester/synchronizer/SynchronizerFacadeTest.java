/*
 * Copyright ETH 2026, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IMetaDataUpdateHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.update.PropertyAssignmentListUpdateValue;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.PluginPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyAssignmentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.update.PropertyTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.delete.TypeGroupAssignmentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.update.TypeGroupUpdate;
import ch.systemsx.cisd.openbis.generic.shared.ICommonServer;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataTypeCode;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.EntityKind;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ExperimentType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.NewETPTAssignment;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.PropertyType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.SampleType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Script;
import ch.systemsx.cisd.openbis.generic.shared.dto.SessionContextDTO;

public class SynchronizerFacadeTest
{
    private Mockery context;
    private ICommonServer common;
    private IApplicationServerApi api;
    private SynchronizerFacade facade;
    private final List<Object> writes = new ArrayList<>();

    @BeforeMethod
    public void setUp()
    {
        writes.clear();
        context = new Mockery();
        common = context.mock(ICommonServer.class);
        api = context.mock(IApplicationServerApi.class);
        SessionContextDTO session = new SessionContextDTO();
        session.setSessionToken("session");
        context.checking(new Expectations() {{
            allowing(common).tryAuthenticate("user", "password"); will(returnValue(session));
        }});
        facade = newFacade(false);
    }

    private SynchronizerFacade newFacade(boolean dryRun)
    {
        return new SynchronizerFacade(common, api, "user", "password", dryRun, false, null);
    }

    private CustomAction capture()
    {
        return new CustomAction("capture V3 write")
        {
            @Override
            public Object invoke(Invocation invocation)
            {
                writes.addAll((List<?>) invocation.getParameter(1));
                return invocation.getInvokedMethod().getReturnType() == void.class ? null : Collections.emptyList();
            }
        };
    }

    @DataProvider
    public Object[][] metadata()
    {
        return new Object[][] { { null }, { Collections.emptyMap() }, { Map.of("key", "value") } };
    }

    @Test(dataProvider = "metadata")
    public void propertyTypesUseV3WithOrWithoutMetadata(Map<String, String> metadata)
    {
        context.checking(new Expectations() {{
            one(api).createPropertyTypes(with("session"), with(any(List.class))); will(capture());
            one(api).updatePropertyTypes(with("session"), with(any(List.class))); will(capture());
        }});
        PropertyType type = propertyType();
        facade.registerPropertyType(type, metadata);
        facade.updatePropertyType(type, metadata, "changed");
        PropertyTypeCreation creation = (PropertyTypeCreation) writes.get(0);
        assertEquals(creation.getCode(), "P");
        assertEquals(creation.getDescription(), "Description");
        assertTrue(creation.isMultiValue());
        assertEquals(creation.getMetaData(), metadata);
        assertEquals(creation.getSchema(), "schema");
        PropertyTypeUpdate update = (PropertyTypeUpdate) writes.get(1);
        assertEquals(update.getTypeId(), new PropertyTypePermId("P"));
        assertEquals(update.getLabel().getValue(), "Property");
        assertEquals(update.getTransformation().getValue(), "transformation");
        assertMetadata(update, metadata);
        context.assertIsSatisfied();
    }

    @Test(dataProvider = "metadata")
    public void entityTypesUseV3WithOrWithoutMetadata(Map<String, String> metadata)
    {
        context.checking(new Expectations() {{
            one(api).createSampleTypes(with("session"), with(any(List.class))); will(capture());
            one(api).updateSampleTypes(with("session"), with(any(List.class))); will(capture());
            one(api).createExperimentTypes(with("session"), with(any(List.class))); will(capture());
            one(api).updateExperimentTypes(with("session"), with(any(List.class))); will(capture());
            one(api).createDataSetTypes(with("session"), with(any(List.class))); will(capture());
            one(api).updateDataSetTypes(with("session"), with(any(List.class))); will(capture());
        }});
        Script plugin = new Script();
        plugin.setName("validator");
        SampleType sample = new SampleType();
        sample.setCode("S");
        sample.setValidationScript(plugin);
        sample.setMetaData(metadata);
        sample.setGeneratedCodePrefix("OBJ");
        sample.setAutoGeneratedCode(true);
        sample.setShowParents(true);
        facade.registerSampleType(sample);
        facade.updateSampleType(sample, "changed");
        SampleTypeCreation sampleCreation = (SampleTypeCreation) writes.get(0);
        assertEquals(sampleCreation.getMetaData(), metadata);
        assertEquals(sampleCreation.getValidationPluginId(), new PluginPermId("validator"));
        assertEquals(sampleCreation.getGeneratedCodePrefix(), "OBJ");
        assertTrue(sampleCreation.isAutoGeneratedCode());
        assertTrue(((SampleTypeUpdate) writes.get(1)).isShowParents().getValue());
        assertMetadata((SampleTypeUpdate) writes.get(1), metadata);

        ExperimentType experiment = new ExperimentType();
        experiment.setCode("E");
        experiment.setMetaData(metadata);
        facade.registerExperimentType(experiment);
        facade.updateExperimentType(experiment, "changed");
        assertEquals(((ExperimentTypeCreation) writes.get(2)).getMetaData(), metadata);
        assertMetadata((ExperimentTypeUpdate) writes.get(3), metadata);

        DataSetType dataSet = new DataSetType();
        dataSet.setCode("D");
        dataSet.setMetaData(metadata);
        dataSet.setMainDataSetPath("main");
        dataSet.setMainDataSetPattern(".*");
        dataSet.setDeletionDisallow(true);
        facade.registerDataSetType(dataSet);
        facade.updateDataSetType(dataSet, "changed");
        DataSetTypeCreation dataSetCreation = (DataSetTypeCreation) writes.get(4);
        assertEquals(dataSetCreation.getMetaData(), metadata);
        assertEquals(dataSetCreation.getMainDataSetPath(), "main");
        assertTrue(dataSetCreation.isDisallowDeletion());
        assertMetadata((DataSetTypeUpdate) writes.get(5), metadata);
        context.assertIsSatisfied();
    }

    @Test(dataProvider = "metadata")
    public void typeGroupsUseV3WithOrWithoutMetadata(Map<String, String> metadata)
    {
        context.checking(new Expectations() {{
            one(api).createTypeGroups(with("session"), with(any(List.class)));
            will(capture());

            one(api).updateTypeGroups(with("session"), with(any(List.class)));
            will(capture());

        }});
        TypeGroupCreation typeGroup = typeGroup(metadata);
        facade.registerTypeGroup(typeGroup);
        facade.updateTypeGroup(typeGroup, "changed");
        TypeGroupCreation creation = (TypeGroupCreation) writes.get(0);
        assertEquals(creation.getCode(), "TG");
        assertTrue(creation.isManagedInternally());
        assertEquals(creation.getMetaData(), metadata);
        TypeGroupUpdate update = (TypeGroupUpdate) writes.get(1);
        assertEquals(update.getTypeGroupId(), new TypeGroupId("TG"));
        assertMetadata(update, metadata);
        context.assertIsSatisfied();
    }

    @Test
    public void typeGroupAssignmentsUseV3()
    {
        TypeGroupAssignmentCreation assignment = new TypeGroupAssignmentCreation();
        assignment.setTypeGroupId(new TypeGroupId("TG"));
        assignment.setSampleTypeId(new EntityTypePermId("T",
                ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind.SAMPLE));
        List<TypeGroupAssignmentCreation> assignments = Collections.singletonList(assignment);

        context.checking(new Expectations() {{
            one(api).createTypeGroupAssignments(with("session"),
                    with(assignments));

            one(api).deleteTypeGroupAssignments(with("session"),
                    with(any(List.class)),
                    with(any(TypeGroupAssignmentDeletionOptions.class)));

            will(capture());
        }});
        facade.assignObjectTypesToTypeGroup(assignments);
        facade.unassignObjectTypeFromTypeGroup("TG", "T");

        TypeGroupAssignmentId deleted = (TypeGroupAssignmentId) writes.get(0);
        assertEquals(deleted, new TypeGroupAssignmentId(
                new EntityTypePermId("T", ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind.SAMPLE),
                new TypeGroupId("TG")));
        context.assertIsSatisfied();
    }

    private TypeGroupCreation typeGroup(Map<String, String> metadata)
    {
        TypeGroupCreation typeGroup = new TypeGroupCreation();
        typeGroup.setCode("TG");
        typeGroup.setManagedInternally(true);
        typeGroup.setMetaData(metadata);
        return typeGroup;
    }

    private void assertMetadata(IMetaDataUpdateHolder update, Map<String, String> metadata)
    {
        assertEquals(update.getMetaData().getSet(), metadata == null ? Collections.emptyList() : Collections.singletonList(metadata));
    }

    @DataProvider
    public Object[][] kinds()
    {
        return new Object[][] { { EntityKind.SAMPLE }, { EntityKind.EXPERIMENT }, { EntityKind.DATA_SET } };
    }

    @Test(dataProvider = "kinds")
    public void assignmentUpdatePreservesUnexportedFieldsAndOtherAssignments(EntityKind kind)
    {
        PropertyAssignment target = existingAssignment("P", 1);
        PropertyAssignment other = existingAssignment("OTHER", 2);
        List<PropertyAssignment> assignments = Arrays.asList(target, other);
        EntityTypePermId id = new EntityTypePermId("T", ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind.valueOf(kind.name()));
        context.checking(new Expectations() {{
            switch (kind)
            {
                case SAMPLE:
                    var sample = new ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType();
                    SampleTypeFetchOptions sampleFetch = new SampleTypeFetchOptions();
                    sampleFetch.withPropertyAssignments();
                    sample.setFetchOptions(sampleFetch);
                    sample.setPropertyAssignments(assignments);
                    one(api).getSampleTypes(with("session"), with(Collections.singletonList(id)), with(any(SampleTypeFetchOptions.class)));
                    will(returnValue(Collections.singletonMap(id, sample)));
                    break;
                case EXPERIMENT:
                    var experiment = new ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType();
                    ExperimentTypeFetchOptions experimentFetch = new ExperimentTypeFetchOptions();
                    experimentFetch.withPropertyAssignments();
                    experiment.setFetchOptions(experimentFetch);
                    experiment.setPropertyAssignments(assignments);
                    one(api).getExperimentTypes(with("session"), with(Collections.singletonList(id)), with(any(ExperimentTypeFetchOptions.class)));
                    will(returnValue(Collections.singletonMap(id, experiment)));
                    break;
                case DATA_SET:
                    var dataSet = new ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType();
                    DataSetTypeFetchOptions dataSetFetch = new DataSetTypeFetchOptions();
                    dataSetFetch.withPropertyAssignments();
                    dataSet.setFetchOptions(dataSetFetch);
                    dataSet.setPropertyAssignments(assignments);
                    one(api).getDataSetTypes(with("session"), with(Collections.singletonList(id)), with(any(DataSetTypeFetchOptions.class)));
                    will(returnValue(Collections.singletonMap(id, dataSet)));
                    break;
                default: throw new AssertionError(kind);
            }
        }});
        expectAssignmentWrite(kind);
        NewETPTAssignment incoming = incomingAssignment(kind);
        incoming.setSection("new section");
        facade.updatePropertyTypeAssignment(incoming, "changed");
        List<PropertyAssignmentCreation> set = new ArrayList<>(assignmentUpdate().getSet());
        assertEquals(set.size(), 2);
        assertEquals(set.get(0).getOrdinal(), Integer.valueOf(1));
        assertEquals(set.get(0).getSection(), "new section");
        assertTrue(set.get(0).isUnique());
        assertTrue(set.get(0).isManagedInternally());
        assertEquals(set.get(0).getPattern(), "[A-Z]+");
        assertEquals(set.get(1).getPropertyTypeId(), new PropertyTypePermId("OTHER"));
        assertEquals(set.get(1).getOrdinal(), Integer.valueOf(2));
        assertEquals(set.get(1).getPluginId(), new PluginPermId("plugin"));
        assertTrue(set.get(1).isUnique());
        assertTrue(set.get(1).isShowRawValueInForms());
        context.assertIsSatisfied();
    }

    @Test(dataProvider = "kinds")
    public void assignmentAddAndRemoveUseV3(EntityKind kind)
    {
        expectAssignmentWrite(kind);
        NewETPTAssignment incoming = incomingAssignment(kind);
        incoming.setUnique(true);
        facade.assignPropertyType(incoming);
        PropertyAssignmentCreation added = assignmentUpdate().getAdded().iterator().next();
        assertTrue(added.isUnique());
        assertEquals(added.getOrdinal(), Integer.valueOf(1));
        assertEquals(added.getPropertyTypeId(), new PropertyTypePermId("P"));
        writes.clear();
        expectAssignmentWrite(kind);
        facade.unassignPropertyType(kind, "P", "T");
        assertEquals(assignmentUpdate().getRemoved().iterator().next(), new PropertyAssignmentPermId(
                new EntityTypePermId("T", ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind.valueOf(kind.name())),
                new PropertyTypePermId("P")));
        assertFalse(assignmentUpdate().isForceRemovingAssignments());
        context.assertIsSatisfied();
    }

    @DataProvider
    public Object[][] emptyDescriptionAndMetadata()
    {
        List<Object[]> result = new ArrayList<>();
        for (String description : Arrays.asList(null, ""))
        {
            for (Object[] metadata : metadata())
            {
                result.add(new Object[] { description, metadata[0] });
            }
        }
        return result.toArray(new Object[0][]);
    }

    @Test(dataProvider = "emptyDescriptionAndMetadata")
    public void propertyTypesWithEmptyDescriptionUseLegacyApiAndV3OnlyForMetadata(String description,
            Map<String, String> metadata)
    {
        PropertyType type = propertyType();
        type.setDescription(description);
        boolean hasMetadata = metadata != null && metadata.isEmpty() == false;
        context.checking(new Expectations() {{
            one(common).registerPropertyType("session", type);
            one(common).updatePropertyType("session", type);
            exactly(hasMetadata ? 2 : 0).of(api).updatePropertyTypes(with("session"), with(any(List.class)));
            will(capture());
        }});
        facade.registerPropertyType(type, metadata);
        facade.updatePropertyType(type, metadata, "changed");
        for (Object write : writes)
        {
            PropertyTypeUpdate update = (PropertyTypeUpdate) write;
            assertEquals(update.getTypeId(), new PropertyTypePermId("P"));
            assertFalse(update.getDescription().isModified());
            assertFalse(update.getLabel().isModified());
            assertMetadata(update, metadata);
        }
        context.assertIsSatisfied();
    }

    @Test
    public void dryRunDoesNotReadOrWriteV3()
    {
        facade = newFacade(true);
        facade.registerPropertyType(propertyType(), Map.of("key", "value"));
        facade.updatePropertyType(propertyType(), Collections.emptyMap(), "changed");
        PropertyType withoutDescription = propertyType();
        withoutDescription.setDescription("");
        facade.registerPropertyType(withoutDescription, Map.of("key", "value"));
        facade.updatePropertyType(withoutDescription, Map.of("key", "value"), "changed");
        SampleType sample = new SampleType();
        sample.setCode("S");
        facade.registerSampleType(sample);
        facade.updateSampleType(sample, "changed");
        ExperimentType experiment = new ExperimentType();
        experiment.setCode("E");
        facade.registerExperimentType(experiment);
        facade.updateExperimentType(experiment, "changed");
        DataSetType dataSet = new DataSetType();
        dataSet.setCode("D");
        facade.registerDataSetType(dataSet);
        facade.updateDataSetType(dataSet, "changed");
        facade.registerTypeGroup(typeGroup(Collections.emptyMap()));
        facade.updateTypeGroup(typeGroup(Collections.emptyMap()), "changed");
        facade.assignObjectTypesToTypeGroup(Collections.emptyList());
        facade.unassignObjectTypeFromTypeGroup("TG", "T");
        for (EntityKind kind : new EntityKind[] { EntityKind.SAMPLE, EntityKind.EXPERIMENT, EntityKind.DATA_SET })
        {
            facade.assignPropertyType(incomingAssignment(kind));
            facade.updatePropertyTypeAssignment(incomingAssignment(kind), "changed");
            facade.unassignPropertyType(kind, "P", "T");
        }
        context.assertIsSatisfied();
    }

    private void expectAssignmentWrite(EntityKind kind)
    {
        context.checking(new Expectations() {{
            switch (kind)
            {
                case SAMPLE: one(api).updateSampleTypes(with("session"), with(any(List.class))); break;
                case EXPERIMENT: one(api).updateExperimentTypes(with("session"), with(any(List.class))); break;
                case DATA_SET: one(api).updateDataSetTypes(with("session"), with(any(List.class))); break;
                default: throw new AssertionError(kind);
            }
            will(capture());
        }});
    }

    private PropertyAssignmentListUpdateValue assignmentUpdate()
    {
        Object update = writes.get(0);
        if (update instanceof SampleTypeUpdate) return ((SampleTypeUpdate) update).getPropertyAssignments();
        if (update instanceof ExperimentTypeUpdate) return ((ExperimentTypeUpdate) update).getPropertyAssignments();
        return ((DataSetTypeUpdate) update).getPropertyAssignments();
    }

    private PropertyAssignment existingAssignment(String code, int ordinal)
    {
        PropertyAssignment assignment = new PropertyAssignment();
        PropertyAssignmentFetchOptions fetch = new PropertyAssignmentFetchOptions();
        fetch.withPropertyType();
        fetch.withPlugin();
        assignment.setFetchOptions(fetch);
        var property = new ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType();
        property.setCode(code);
        assignment.setPropertyType(property);
        Plugin plugin = new Plugin();
        plugin.setPermId(new PluginPermId("plugin"));
        assignment.setPlugin(plugin);
        assignment.setOrdinal(ordinal);
        assignment.setUnique(true);
        assignment.setManagedInternally(true);
        assignment.setShowRawValueInForms(true);
        assignment.setPattern("[A-Z]+");
        assignment.setPatternType("REGEX");
        return assignment;
    }

    private NewETPTAssignment incomingAssignment(EntityKind kind)
    {
        NewETPTAssignment assignment = new NewETPTAssignment();
        assignment.setEntityKind(kind);
        assignment.setEntityTypeCode("T");
        assignment.setPropertyTypeCode("P");
        assignment.setOrdinal(0L);
        return assignment;
    }

    private PropertyType propertyType()
    {
        PropertyType type = new PropertyType();
        type.setCode("P");
        type.setLabel("Property");
        type.setDescription("Description");
        DataType dataType = new DataType();
        dataType.setCode(DataTypeCode.VARCHAR);
        type.setDataType(dataType);
        type.setMultiValue(true);
        type.setSchema("schema");
        type.setTransformation("transformation");
        return type;
    }
}
