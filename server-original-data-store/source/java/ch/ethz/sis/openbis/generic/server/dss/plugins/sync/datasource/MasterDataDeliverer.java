/*
 * Copyright ETH 2019 - 2023 Zürich, Scientific IT Services
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

import static ch.systemsx.cisd.openbis.generic.shared.basic.BasicConstant.INTERNAL_NAMESPACE_PREFIX;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.ICodeHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IDescriptionHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IPropertyAssignmentsHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.ContentCopy;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.LinkedData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.ExternalDms;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.fetchoptions.ExternalDmsFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.id.ExternalDmsPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.fetchoptions.PluginFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.PluginPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyPermId;
import ch.systemsx.cisd.common.shared.basic.string.CommaSeparatedListBuilder;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProvider;
import ch.systemsx.cisd.openbis.generic.server.jython.api.v1.DataType;
import ch.systemsx.cisd.openbis.generic.shared.basic.BasicConstant;
import ch.systemsx.cisd.openbis.generic.shared.basic.CodeConverter;

/**
 * @author Franz-Josef Elmer
 */
public class MasterDataDeliverer extends AbstractEntityDeliverer<Object>
{
    MasterDataDeliverer(DeliveryContext context)
    {
        super(context, "master data");
    }

    @Override
    public void deliverEntities(DeliveryExecutionContext context) throws XMLStreamException
    {
        XMLStreamWriter writer = context.getWriter();
        startUrlElement(writer);
        addLocation(writer, "MASTER_DATA", "MASTER_DATA");
        addLastModificationDate(writer, context.getRequestTimestamp());
        writer.writeStartElement("xmd:masterData");
        String sessionToken = context.getSessionToken();

        List<SampleType> sampleTypes = fetchSampleTypes(context, sessionToken);
        List<ExperimentType> experimentTypes = fetchExperimentTypes(context, sessionToken);
        List<DataSetType> dataSetTypes = fetchDataSetTypes(context, sessionToken);
        List<DataSet> dataSets = fetchDataSetsForExternalDms(context, sessionToken);

        addValidationPlugins(context, writer, sessionToken, collectPluginIds(sampleTypes, experimentTypes, dataSetTypes));
        addVocabularies(context, writer, sessionToken);
        addPropertyTypes(context, writer, sessionToken, collectPropertyTypeIds(sampleTypes, experimentTypes, dataSetTypes));
        writeSampleTypes(context, writer, sampleTypes);
        writeExperimentTypes(context, writer, experimentTypes);
        writeDataSetTypes(context, writer, dataSetTypes);
        addExternalDataManagementSystems(context, writer, sessionToken, collectExternalDmsIds(dataSets));
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private Set<PluginPermId> collectPluginIds(List<SampleType> sampleTypes, List<ExperimentType> experimentTypes,
            List<DataSetType> dataSetTypes)
    {
        Set<PluginPermId> ids = new LinkedHashSet<>();
        for (SampleType type : sampleTypes)
        {
            addPluginId(ids, type.getValidationPlugin());
            for (PropertyAssignment assignment : type.getPropertyAssignments())
            {
                addPluginId(ids, assignment.getPlugin());
            }
        }
        for (ExperimentType type : experimentTypes)
        {
            addPluginId(ids, type.getValidationPlugin());
            for (PropertyAssignment assignment : type.getPropertyAssignments())
            {
                addPluginId(ids, assignment.getPlugin());
            }
        }
        for (DataSetType type : dataSetTypes)
        {
            addPluginId(ids, type.getValidationPlugin());
            for (PropertyAssignment assignment : type.getPropertyAssignments())
            {
                addPluginId(ids, assignment.getPlugin());
            }
        }
        return ids;
    }

    private void addPluginId(Set<PluginPermId> ids, Plugin plugin)
    {
        if (plugin != null)
        {
            ids.add(plugin.getPermId());
        }
    }

    private Set<PropertyTypePermId> collectPropertyTypeIds(List<SampleType> sampleTypes, List<ExperimentType> experimentTypes,
            List<DataSetType> dataSetTypes)
    {
        Set<PropertyTypePermId> ids = new LinkedHashSet<>();
        for (SampleType type : sampleTypes)
        {
            for (PropertyAssignment assignment : type.getPropertyAssignments())
            {
                ids.add(assignment.getPropertyType().getPermId());
            }
        }
        for (ExperimentType type : experimentTypes)
        {
            for (PropertyAssignment assignment : type.getPropertyAssignments())
            {
                ids.add(assignment.getPropertyType().getPermId());
            }
        }
        for (DataSetType type : dataSetTypes)
        {
            for (PropertyAssignment assignment : type.getPropertyAssignments())
            {
                ids.add(assignment.getPropertyType().getPermId());
            }
        }
        return ids;
    }

    private Set<ExternalDmsPermId> collectExternalDmsIds(List<DataSet> dataSets)
    {
        Set<ExternalDmsPermId> ids = new LinkedHashSet<>();
        for (DataSet dataSet : dataSets)
        {
            LinkedData linkedData = dataSet.getLinkedData();
            if (linkedData != null && linkedData.getContentCopies() != null)
            {
                for (ContentCopy contentCopy : linkedData.getContentCopies())
                {
                    if (contentCopy.getExternalDms() != null)
                    {
                        ids.add(contentCopy.getExternalDms().getPermId());
                    }
                }
            }
        }
        return ids;
    }

    private List<DataSet> fetchDataSetsForExternalDms(DeliveryExecutionContext executionContext, String sessionToken)
    {
        List<String> permIds = executionContext.getPermIds(ExportableKind.DATASET);
        if (permIds.isEmpty())
        {
            return new ArrayList<>();
        }
        List<DataSetPermId> ids = permIds.stream().map(DataSetPermId::new).collect(Collectors.toList());
        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withLinkedData().withExternalDms();
        return new ArrayList<>(context.getV3api().getDataSets(sessionToken, ids, fetchOptions).values());
    }

    private void addValidationPlugins(DeliveryExecutionContext executionContext, XMLStreamWriter writer,
            String sessionToken, Set<PluginPermId> pluginIds) throws XMLStreamException
    {
        PluginFetchOptions fetchOptions = new PluginFetchOptions();
        fetchOptions.withScript();
        List<Plugin> plugins = new ArrayList<>(context.getV3api()
                .getPlugins(sessionToken, new ArrayList<>(pluginIds), fetchOptions).values());
        if (plugins.isEmpty())
        {
            return;
        }
        writer.writeStartElement("xmd:validationPlugins");
        for (Plugin plugin : plugins)
        {
            writer.writeStartElement("xmd:validationPlugin");
            addAttributeAndExtractFilePaths(executionContext, writer, "description", plugin.getDescription());
            addAttribute(writer, "entityKind", getEntityKind(plugin));
            addAttribute(writer, "isAvailable", String.valueOf(plugin.isAvailable()));
            addAttribute(writer, "name", plugin.getName());
            addAttribute(writer, "type", plugin.getPluginType(), t -> t.toString());
            addAttribute(writer, "registration-timestamp", plugin.getRegistrationDate(), h -> DataSourceUtils.convertToW3CDate(h));
            addAttribute(writer, "modification-timestamp", plugin.getRegistrationDate(), h -> DataSourceUtils.convertToW3CDate(h));
            if (plugin.getScript() != null)
            {
                writer.writeCData(plugin.getScript());
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private String getEntityKind(Plugin plugin)
    {
        String entityKind = "All";
        Set<EntityKind> entityKinds = plugin.getEntityKinds();
        if (entityKinds != null)
        {
            CommaSeparatedListBuilder builder = new CommaSeparatedListBuilder();
            for (EntityKind kind : entityKinds)
            {
                builder.append(kind.toString());
            }
            entityKind = builder.toString();
        }
        return entityKind;
    }

    private void addVocabularies(DeliveryExecutionContext executionContext, XMLStreamWriter writer, 
            String sessionToken) throws XMLStreamException
    {
        VocabularyFetchOptions fetchOptions = new VocabularyFetchOptions();
        fetchOptions.withTerms().withRegistrator();
        fetchOptions.withRegistrator();
        List<VocabularyPermId> ids = executionContext.getPermIds(ExportableKind.VOCABULARY_TYPE).stream()
                .map(VocabularyPermId::new).collect(Collectors.toList());
        List<Vocabulary> vocabularies = new ArrayList<>(context.getV3api().getVocabularies(sessionToken, ids, fetchOptions).values());
        if (vocabularies.isEmpty())
        {
            return;
        }
        writer.writeStartElement("xmd:controlledVocabularies");
        for (Vocabulary vocabulary : vocabularies)
        {
            writer.writeStartElement("xmd:controlledVocabulary");
            String code = vocabulary.isManagedInternally()
                    && vocabulary.getCode().startsWith(INTERNAL_NAMESPACE_PREFIX) ? CodeConverter.tryToDatabase(vocabulary.getCode())
                            : vocabulary.getCode();
            addAttribute(writer, "chosenFromList", String.valueOf(vocabulary.isChosenFromList()));
            addAttribute(writer, "code", code);
            addAttribute(writer, "description", vocabulary.getDescription());
            addAttribute(writer, "managedInternally", String.valueOf(vocabulary.isManagedInternally()));
            addAttribute(writer, "urlTemplate", vocabulary.getUrlTemplate());
            addAttribute(writer, "registration-timestamp", vocabulary.getRegistrationDate(), h -> DataSourceUtils.convertToW3CDate(h));
            addAttribute(writer, "registrator", vocabulary.getRegistrator().getUserId());
            addAttribute(writer, "modification-timestamp", vocabulary.getModificationDate(), h -> DataSourceUtils.convertToW3CDate(h));

            for (VocabularyTerm term : vocabulary.getTerms())
            {
                writer.writeStartElement("xmd:term");
                addAttribute(writer, "code", term.getCode());
                addAttributeAndExtractFilePaths(executionContext, writer, "description", term.getDescription());
                addAttribute(writer, "label", term.getLabel());
                addAttribute(writer, "ordinal", String.valueOf(term.getOrdinal()));
                addAttribute(writer, "registration-timestamp", term.getRegistrationDate(), h -> DataSourceUtils.convertToW3CDate(h));
                addAttribute(writer, "registrator", term.getRegistrator().getUserId());
                addAttribute(writer, "url", vocabulary.getUrlTemplate(),
                        t -> t.replaceAll(BasicConstant.DEPRECATED_VOCABULARY_URL_TEMPLATE_TERM_PATTERN, code)
                                .replaceAll(BasicConstant.VOCABULARY_URL_TEMPLATE_TERM_PATTERN, code));
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void addPropertyTypes(DeliveryExecutionContext executionContext, XMLStreamWriter writer,
            String sessionToken, Set<PropertyTypePermId> propertyTypeIds) throws XMLStreamException
    {
        PropertyTypeFetchOptions fetchOptions = new PropertyTypeFetchOptions();
        fetchOptions.withVocabulary();
        fetchOptions.withRegistrator();
        List<PropertyType> propertyTypes = new ArrayList<>(context.getV3api()
                .getPropertyTypes(sessionToken, new ArrayList<>(propertyTypeIds), fetchOptions).values());
        if (propertyTypes.isEmpty())
        {
            return;
        }
        writer.writeStartElement("xmd:propertyTypes");

        for (PropertyType propertyType : propertyTypes)
        {
            Boolean managedInternally = propertyType.isManagedInternally();
            String code =
                    (managedInternally && propertyType.getCode().startsWith(INTERNAL_NAMESPACE_PREFIX))
                            ? CodeConverter.tryToDatabase(propertyType.getCode())
                            : propertyType.getCode();
            writer.writeStartElement("xmd:propertyType");
            addAttribute(writer, "code", code);
            addAttribute(writer, "dataType", propertyType.getDataType(), t -> t.name());
            addAttributeAndExtractFilePaths(executionContext, writer, "description", propertyType.getDescription());
            addAttribute(writer, "label", propertyType.getLabel());
            addAttribute(writer, "managedInternally", managedInternally);
            addAttribute(writer, "registration-timestamp", propertyType.getRegistrationDate(), h -> DataSourceUtils.convertToW3CDate(h));
            addAttribute(writer, "registrator", propertyType.getRegistrator().getUserId());
            if (propertyType.getDataType().name().equals(DataType.CONTROLLEDVOCABULARY.name()))
            {
                addAttribute(writer, "vocabulary", propertyType.getVocabulary(), v -> v.getCode());
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private List<SampleType> fetchSampleTypes(DeliveryExecutionContext executionContext, String sessionToken)
    {
        SampleTypeFetchOptions fetchOptions = new SampleTypeFetchOptions();
        fetchOptions.withPropertyAssignments().withPropertyType();
        fetchOptions.withPropertyAssignments().withPlugin();
        fetchOptions.withValidationPlugin();
        List<EntityTypePermId> ids = executionContext.getPermIds(ExportableKind.SAMPLE_TYPE).stream()
                .map(permId -> new EntityTypePermId(permId, EntityKind.SAMPLE)).collect(Collectors.toList());
        return new ArrayList<>(context.getV3api().getSampleTypes(sessionToken, ids, fetchOptions).values());
    }

    private void writeSampleTypes(DeliveryExecutionContext executionContext, XMLStreamWriter writer,
            List<SampleType> types) throws XMLStreamException
    {
        if (types.isEmpty())
        {
            return;
        }
        writer.writeStartElement("xmd:objectTypes");
        for (SampleType type : types)
        {
            writeTypeElement(executionContext, writer, "xmd:objectType", type);
            addAttribute(writer, "autoGeneratedCode", type.isAutoGeneratedCode());
            addAttribute(writer, "generatedCodePrefix", type.getGeneratedCodePrefix());
            addAttribute(writer, "listable", type.isListable());
            addAttribute(writer, "showContainer", type.isShowContainer());
            addAttribute(writer, "showParentMetadata", type.isShowParentMetadata());
            addAttribute(writer, "showParents", type.isShowParents());
            addAttribute(writer, "subcodeUnique", type.isSubcodeUnique());
            addAttribute(writer, "validationPlugin", type.getValidationPlugin(), p -> p.getName());
            addAttribute(writer, "modification-timestamp", type.getModificationDate(), h -> DataSourceUtils.convertToW3CDate(h));
            addPropertyAssignments(writer, type.getPropertyAssignments());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private List<ExperimentType> fetchExperimentTypes(DeliveryExecutionContext executionContext, String sessionToken)
    {
        ExperimentTypeFetchOptions fetchOptions = new ExperimentTypeFetchOptions();
        fetchOptions.withPropertyAssignments().withPropertyType();
        fetchOptions.withPropertyAssignments().withPlugin();
        fetchOptions.withValidationPlugin();
        List<EntityTypePermId> ids = executionContext.getPermIds(ExportableKind.EXPERIMENT_TYPE).stream()
                .map(permId -> new EntityTypePermId(permId, EntityKind.EXPERIMENT)).collect(Collectors.toList());
        return new ArrayList<>(context.getV3api().getExperimentTypes(sessionToken, ids, fetchOptions).values());
    }

    private void writeExperimentTypes(DeliveryExecutionContext executionContext, XMLStreamWriter writer,
            List<ExperimentType> types) throws XMLStreamException
    {
        if (types.isEmpty())
        {
            return;
        }
        writer.writeStartElement("xmd:collectionTypes");
        for (ExperimentType type : types)
        {
            writeTypeElement(executionContext, writer, "xmd:collectionType", type);
            addAttribute(writer, "validationPlugin", type.getValidationPlugin(), p -> p.getName());
            addAttribute(writer, "modification-timestamp", type.getModificationDate(), h -> DataSourceUtils.convertToW3CDate(h));
            addPropertyAssignments(writer, type.getPropertyAssignments());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private List<DataSetType> fetchDataSetTypes(DeliveryExecutionContext executionContext, String sessionToken)
    {
        DataSetTypeFetchOptions fetchOptions = new DataSetTypeFetchOptions();
        fetchOptions.withPropertyAssignments().withPropertyType();
        fetchOptions.withPropertyAssignments().withPlugin();
        fetchOptions.withValidationPlugin();
        List<EntityTypePermId> ids = executionContext.getPermIds(ExportableKind.DATASET_TYPE).stream()
                .map(permId -> new EntityTypePermId(permId, EntityKind.DATA_SET)).collect(Collectors.toList());
        return new ArrayList<>(context.getV3api().getDataSetTypes(sessionToken, ids, fetchOptions).values());
    }

    private void writeDataSetTypes(DeliveryExecutionContext executionContext, XMLStreamWriter writer,
            List<DataSetType> types) throws XMLStreamException
    {
        if (types.isEmpty())
        {
            return;
        }
        writer.writeStartElement("xmd:dataSetTypes");
        for (DataSetType type : types)
        {
            writeTypeElement(executionContext, writer, "xmd:dataSetType", type);
            addAttribute(writer, "deletionDisallowed", type.isDisallowDeletion());
            addAttribute(writer, "mainDataSetPath", type.getMainDataSetPath());
            addAttribute(writer, "mainDataSetPattern", type.getMainDataSetPattern());
            addAttribute(writer, "validationPlugin", type.getValidationPlugin(), p -> p.getName());
            addAttribute(writer, "modification-timestamp", type.getModificationDate(), h -> DataSourceUtils.convertToW3CDate(h));
            addPropertyAssignments(writer, type.getPropertyAssignments());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void addExternalDataManagementSystems(DeliveryExecutionContext executionContext, XMLStreamWriter writer,
            String sessionToken, Set<ExternalDmsPermId> externalDmsIds) throws XMLStreamException
    {
        ExternalDmsFetchOptions fetchOptions = new ExternalDmsFetchOptions();
        List<ExternalDms> externalDataManagementSystems = new ArrayList<>(context.getV3api()
                .getExternalDataManagementSystems(sessionToken, new ArrayList<>(externalDmsIds), fetchOptions).values());
        if (externalDataManagementSystems.isEmpty() == false)
        {
            writer.writeStartElement("xmd:externalDataManagementSystems");
            for (ExternalDms externalDms : externalDataManagementSystems)
            {
                writer.writeStartElement("xmd:externalDataManagementSystem");
                addAttribute(writer, "address", externalDms.getAddress());
                addAttribute(writer, "addressType", externalDms.getAddressType(), t -> t.toString());
                addAttribute(writer, "code", externalDms.getCode());
                addAttribute(writer, "label", externalDms.getLabel());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private <T extends ICodeHolder & IDescriptionHolder & IPropertyAssignmentsHolder> void writeTypeElement(
            DeliveryExecutionContext executionContext, XMLStreamWriter writer, String elementType, T type) throws XMLStreamException
    {
        writer.writeStartElement(elementType);
        addAttribute(writer, "code", type.getCode());
        addAttributeAndExtractFilePaths(executionContext, writer, "description", type.getDescription());
    }

    private void addPropertyAssignments(XMLStreamWriter writer, List<PropertyAssignment> propertyAssignments) throws XMLStreamException
    {
        writer.writeStartElement("xmd:propertyAssignments");
        for (PropertyAssignment propertyAssignment : propertyAssignments)
        {
            writer.writeStartElement("xmd:propertyAssignment");
            addAttribute(writer, "mandatory", propertyAssignment.isMandatory());
            addAttribute(writer, "ordinal", propertyAssignment.getOrdinal(), i -> String.valueOf(i));
            addAttribute(writer, "plugin", propertyAssignment.getPlugin(), p -> p.getPermId().getPermId());
            addAttribute(writer, "pluginType", propertyAssignment.getPlugin(), p -> p.getPluginType().toString());
            addAttribute(writer, "propertyTypeCode", propertyAssignment.getPropertyType(), t -> t.getCode());
            addAttribute(writer, "section", propertyAssignment.getSection());
            addAttribute(writer, "showInEdit", propertyAssignment.isShowInEditView());
            addAttribute(writer, "showRawValueInForms", propertyAssignment.isShowRawValueInForms());
            addAttribute(writer, "registration-timestamp", propertyAssignment.getRegistrationDate(), h -> DataSourceUtils.convertToW3CDate(h));
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

}
