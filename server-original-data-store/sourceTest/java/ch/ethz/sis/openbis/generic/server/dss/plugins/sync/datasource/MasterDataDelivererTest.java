/*
 * Copyright ETH 2026 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.datasource;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.ContentCopy;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.LinkedData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.IDataSetId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.ExternalDms;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.fetchoptions.ExternalDmsFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.id.ExternalDmsPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.id.IExternalDmsId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.PluginType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.fetchoptions.PluginFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.IPluginId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.PluginPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.IPropertyTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.IVocabularyId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyPermId;

/**
 * @author Tufail Tak
 */
public class MasterDataDelivererTest
{
    private static final String SESSION_TOKEN = "test-token";

    private Mockery mockery;

    private IApplicationServerApi api;

    private MasterDataDeliverer deliverer;

    @BeforeMethod
    public void beforeMethod()
    {
        mockery = new Mockery();
        api = mockery.mock(IApplicationServerApi.class);

        DeliveryContext context = new DeliveryContext();
        context.setServerUrl("http://test");
        context.setV3api(api);
        deliverer = new MasterDataDeliverer(context);
    }

    @AfterMethod
    public void afterMethod()
    {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testDeliverEntitiesWithEmptyPermIdsByKindWritesNothing() throws Exception
    {
        mockery.checking(new Expectations()
        {
            {
                allowing(api).getSampleTypes(with(SESSION_TOKEN), with(Collections.<IEntityTypeId> emptyList()),
                        with(any(SampleTypeFetchOptions.class)));
                will(returnValue(Collections.emptyMap()));

                allowing(api).getExperimentTypes(with(SESSION_TOKEN), with(Collections.<IEntityTypeId> emptyList()),
                        with(any(ExperimentTypeFetchOptions.class)));
                will(returnValue(Collections.emptyMap()));

                allowing(api).getDataSetTypes(with(SESSION_TOKEN), with(Collections.<IEntityTypeId> emptyList()),
                        with(any(DataSetTypeFetchOptions.class)));
                will(returnValue(Collections.emptyMap()));

                allowing(api).getVocabularies(with(SESSION_TOKEN), with(Collections.<IVocabularyId> emptyList()),
                        with(any(VocabularyFetchOptions.class)));
                will(returnValue(Collections.emptyMap()));

                allowing(api).getPlugins(with(SESSION_TOKEN), with(Collections.<IPluginId> emptyList()), with(any(PluginFetchOptions.class)));
                will(returnValue(Collections.emptyMap()));

                allowing(api).getPropertyTypes(with(SESSION_TOKEN), with(Collections.<IPropertyTypeId> emptyList()),
                        with(any(PropertyTypeFetchOptions.class)));
                will(returnValue(Collections.emptyMap()));

                allowing(api).getExternalDataManagementSystems(with(SESSION_TOKEN), with(Collections.<IExternalDmsId> emptyList()),
                        with(any(ExternalDmsFetchOptions.class)));
                will(returnValue(Collections.emptyMap()));
            }
        });

        String xml = deliverEntities(new EnumMap<>(ExportableKind.class));

        assertFalse(xml.contains("xmd:objectTypes"), xml);
        assertFalse(xml.contains("xmd:collectionTypes"), xml);
        assertFalse(xml.contains("xmd:dataSetTypes"), xml);
        assertFalse(xml.contains("xmd:controlledVocabularies"), xml);
        assertFalse(xml.contains("xmd:validationPlugins"), xml);
        assertFalse(xml.contains("xmd:propertyTypes"), xml);
        assertFalse(xml.contains("xmd:externalDataManagementSystems"), xml);
    }

    @Test
    public void testDeliverEntitiesFiltersByPermIdsAndDerivedReferences() throws Exception
    {
        Map<ExportableKind, List<String>> permIdsByKind = new EnumMap<>(ExportableKind.class);
        permIdsByKind.put(ExportableKind.SAMPLE_TYPE, List.of("SAMPLE_TYPE_A"));
        permIdsByKind.put(ExportableKind.EXPERIMENT_TYPE, List.of("EXPERIMENT_TYPE_A"));
        permIdsByKind.put(ExportableKind.DATASET_TYPE, List.of("DATASET_TYPE_A"));
        permIdsByKind.put(ExportableKind.VOCABULARY_TYPE, List.of("VOC_A"));
        permIdsByKind.put(ExportableKind.DATASET, List.of("DATASET-1"));

        EntityTypePermId sampleTypeId = new EntityTypePermId("SAMPLE_TYPE_A", EntityKind.SAMPLE);
        EntityTypePermId experimentTypeId = new EntityTypePermId("EXPERIMENT_TYPE_A", EntityKind.EXPERIMENT);
        EntityTypePermId dataSetTypeId = new EntityTypePermId("DATASET_TYPE_A", EntityKind.DATA_SET);
        VocabularyPermId vocabularyId = new VocabularyPermId("VOC_A");
        DataSetPermId dataSetId = new DataSetPermId("DATASET-1");
        PluginPermId pluginAId = new PluginPermId("PLUGIN_A");
        PluginPermId pluginBId = new PluginPermId("PLUGIN_B");
        PropertyTypePermId propertyTypeId = new PropertyTypePermId("PROP_A");
        ExternalDmsPermId externalDmsId = new ExternalDmsPermId("EDMS_A");

        Plugin pluginA = plugin(pluginAId, PluginType.DYNAMIC_PROPERTY);
        Plugin pluginB = plugin(pluginBId, PluginType.ENTITY_VALIDATION);

        SampleType sampleType = sampleType("SAMPLE_TYPE_A", propertyAssignment(propertyTypeId, pluginA));
        ExperimentType experimentType = experimentType("EXPERIMENT_TYPE_A", pluginB);
        DataSetType dataSetType = dataSetType("DATASET_TYPE_A");
        Vocabulary vocabulary = vocabulary("VOC_A");
        DataSet dataSet = dataSetWithExternalDms("DATASET-1", externalDmsId);
        PropertyType propertyType = propertyType("PROP_A");
        ExternalDms externalDms = externalDms(externalDmsId, "EDMS_A");

        mockery.checking(new Expectations()
        {
            {
                allowing(api).getSampleTypes(with(SESSION_TOKEN), with(List.<IEntityTypeId> of(sampleTypeId)),
                        with(any(SampleTypeFetchOptions.class)));
                will(returnValue(Map.of(sampleTypeId, sampleType)));

                allowing(api).getExperimentTypes(with(SESSION_TOKEN), with(List.<IEntityTypeId> of(experimentTypeId)),
                        with(any(ExperimentTypeFetchOptions.class)));
                will(returnValue(Map.of(experimentTypeId, experimentType)));

                allowing(api).getDataSetTypes(with(SESSION_TOKEN), with(List.<IEntityTypeId> of(dataSetTypeId)),
                        with(any(DataSetTypeFetchOptions.class)));
                will(returnValue(Map.of(dataSetTypeId, dataSetType)));

                allowing(api).getVocabularies(with(SESSION_TOKEN), with(List.<IVocabularyId> of(vocabularyId)),
                        with(any(VocabularyFetchOptions.class)));
                will(returnValue(Map.of(vocabularyId, vocabulary)));

                allowing(api).getDataSets(with(SESSION_TOKEN), with(List.<IDataSetId> of(dataSetId)), with(any(DataSetFetchOptions.class)));
                will(returnValue(Map.of(dataSetId, dataSet)));

                allowing(api).getPlugins(with(SESSION_TOKEN), with(List.<IPluginId> of(pluginAId, pluginBId)),
                        with(any(PluginFetchOptions.class)));
                will(returnValue(Map.of(pluginAId, pluginA, pluginBId, pluginB)));

                allowing(api).getPropertyTypes(with(SESSION_TOKEN), with(List.<IPropertyTypeId> of(propertyTypeId)),
                        with(any(PropertyTypeFetchOptions.class)));
                will(returnValue(Map.of(propertyTypeId, propertyType)));

                allowing(api).getExternalDataManagementSystems(with(SESSION_TOKEN), with(List.<IExternalDmsId> of(externalDmsId)),
                        with(any(ExternalDmsFetchOptions.class)));
                will(returnValue(Map.of(externalDmsId, externalDms)));
            }
        });

        String xml = deliverEntities(permIdsByKind);

        assertTrue(xml.contains("SAMPLE_TYPE_A"), xml);
        assertTrue(xml.contains("EXPERIMENT_TYPE_A"), xml);
        assertTrue(xml.contains("DATASET_TYPE_A"), xml);
        assertTrue(xml.contains("VOC_A"), xml);
        assertTrue(xml.contains("PLUGIN_A"), xml);
        assertTrue(xml.contains("PLUGIN_B"), xml);
        assertTrue(xml.contains("PROP_A"), xml);
        assertTrue(xml.contains("EDMS_A"), xml);
    }

    private String deliverEntities(Map<ExportableKind, List<String>> permIdsByKind) throws Exception
    {
        StringWriter stringWriter = new StringWriter();
        XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(stringWriter);

        DeliveryExecutionContext executionContext = new DeliveryExecutionContext();
        executionContext.setWriter(writer);
        executionContext.setSessionToken(SESSION_TOKEN);
        executionContext.setRequestTimestamp(new Date());
        executionContext.setFileServicePaths(new HashSet<>());
        executionContext.setPermIdsByKind(permIdsByKind);

        writer.writeStartDocument();
        deliverer.deliverEntities(executionContext);
        writer.writeEndDocument();
        writer.flush();

        return stringWriter.toString();
    }

    private static SampleType sampleType(String code, PropertyAssignment... propertyAssignments)
    {
        SampleTypeFetchOptions fetchOptions = new SampleTypeFetchOptions();
        fetchOptions.withPropertyAssignments();
        fetchOptions.withValidationPlugin();

        SampleType type = new SampleType();
        type.setFetchOptions(fetchOptions);
        type.setCode(code);
        type.setPropertyAssignments(List.of(propertyAssignments));
        return type;
    }

    private static ExperimentType experimentType(String code, Plugin validationPlugin)
    {
        ExperimentTypeFetchOptions fetchOptions = new ExperimentTypeFetchOptions();
        fetchOptions.withPropertyAssignments();
        fetchOptions.withValidationPlugin();

        ExperimentType type = new ExperimentType();
        type.setFetchOptions(fetchOptions);
        type.setCode(code);
        type.setPropertyAssignments(Collections.emptyList());
        type.setValidationPlugin(validationPlugin);
        return type;
    }

    private static DataSetType dataSetType(String code)
    {
        DataSetTypeFetchOptions fetchOptions = new DataSetTypeFetchOptions();
        fetchOptions.withPropertyAssignments();
        fetchOptions.withValidationPlugin();

        DataSetType type = new DataSetType();
        type.setFetchOptions(fetchOptions);
        type.setCode(code);
        type.setPropertyAssignments(Collections.emptyList());
        return type;
    }

    private static Vocabulary vocabulary(String code)
    {
        VocabularyFetchOptions fetchOptions = new VocabularyFetchOptions();
        fetchOptions.withTerms();
        fetchOptions.withRegistrator();

        Person registrator = new Person();
        registrator.setUserId("test-user");

        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setFetchOptions(fetchOptions);
        vocabulary.setCode(code);
        vocabulary.setTerms(Collections.emptyList());
        vocabulary.setRegistrator(registrator);
        return vocabulary;
    }

    private static PropertyAssignment propertyAssignment(PropertyTypePermId propertyTypeId, Plugin plugin)
    {
        PropertyAssignmentFetchOptions fetchOptions = new PropertyAssignmentFetchOptions();
        fetchOptions.withPlugin();
        fetchOptions.withPropertyType();

        PropertyType propertyType = new PropertyType();
        propertyType.setPermId(propertyTypeId);
        propertyType.setCode(propertyTypeId.getPermId());

        PropertyAssignment assignment = new PropertyAssignment();
        assignment.setFetchOptions(fetchOptions);
        assignment.setPropertyType(propertyType);
        assignment.setPlugin(plugin);
        assignment.setMandatory(false);
        assignment.setShowInEditView(true);
        assignment.setShowRawValueInForms(true);
        return assignment;
    }

    private static Plugin plugin(PluginPermId permId, PluginType pluginType)
    {
        PluginFetchOptions fetchOptions = new PluginFetchOptions();
        fetchOptions.withScript();

        Plugin plugin = new Plugin();
        plugin.setFetchOptions(fetchOptions);
        plugin.setPermId(permId);
        plugin.setName(permId.getPermId());
        plugin.setPluginType(pluginType);
        return plugin;
    }

    private static PropertyType propertyType(String code)
    {
        PropertyTypeFetchOptions fetchOptions = new PropertyTypeFetchOptions();
        fetchOptions.withRegistrator();

        Person registrator = new Person();
        registrator.setUserId("test-user");

        PropertyType propertyType = new PropertyType();
        propertyType.setFetchOptions(fetchOptions);
        propertyType.setCode(code);
        propertyType.setDataType(DataType.VARCHAR);
        propertyType.setManagedInternally(false);
        propertyType.setRegistrator(registrator);
        return propertyType;
    }

    private static ExternalDms externalDms(ExternalDmsPermId permId, String code)
    {
        ExternalDms externalDms = new ExternalDms();
        externalDms.setPermId(permId);
        externalDms.setCode(code);
        return externalDms;
    }

    private static DataSet dataSetWithExternalDms(String permId, ExternalDmsPermId externalDmsId)
    {
        ExternalDms externalDms = new ExternalDms();
        externalDms.setPermId(externalDmsId);
        externalDms.setCode(externalDmsId.getPermId());

        ContentCopy contentCopy = new ContentCopy();
        contentCopy.setExternalDms(externalDms);

        LinkedData linkedData = new LinkedData();
        linkedData.setContentCopies(List.of(contentCopy));

        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withLinkedData();

        DataSet dataSet = new DataSet();
        dataSet.setFetchOptions(fetchOptions);
        dataSet.setPermId(new DataSetPermId(permId));
        dataSet.setLinkedData(linkedData);
        return dataSet;
    }
}
