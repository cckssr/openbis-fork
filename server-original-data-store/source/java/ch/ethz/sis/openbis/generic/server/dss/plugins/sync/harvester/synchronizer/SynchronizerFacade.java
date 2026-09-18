/*
 * Copyright ETH 2017 - 2023 Zürich, Scientific IT Services
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import ch.ethz.sis.shared.log.classic.impl.Logger;

import java.util.Collections;
import java.util.function.Consumer;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyAssignmentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.update.PropertyAssignmentListUpdateValue;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.update.PropertyTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.PluginPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.create.ExternalDmsCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.update.ExternalDmsUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.delete.TypeGroupAssignmentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.update.TypeGroupUpdate;
import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.common.ServiceFinderUtils;
import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.util.SummaryUtils;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProvider;
import ch.systemsx.cisd.openbis.generic.shared.ICommonServer;
import ch.systemsx.cisd.openbis.generic.shared.basic.TechId;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.EntityKind;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.EntityType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ExperimentType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.NewETPTAssignment;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.NewVocabulary;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.PropertyType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.SampleType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Script;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Vocabulary;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.VocabularyTerm;

/**
 * @author Ganime Betul Akin
 */

public class SynchronizerFacade implements ISynchronizerFacade
{
    private final String sessionToken;

    private final ICommonServer commonServer;

    private final IApplicationServerApi v3api;
    
    private final Logger operationLog;

    private Map<String, String> propertyTypesToUpdate = new TreeMap<String, String>();

    private Set<String> propertyTypesToAdd = new TreeSet<String>();

    private Map<String, String> validationPluginsToUpdate = new TreeMap<String, String>();

    private Set<String> validationPluginsToAdd = new TreeSet<String>();

    private final boolean dryRun;

    private final boolean verbose;

    private Set<String> vocabulariesToAdd = new TreeSet<String>();

    private Map<String, UpdateSummary> vocabulariesToUpdate = new TreeMap<String, UpdateSummary>();

    // private Map<String, String> vocabularyTermsToUpdate = new TreeMap<String, String>();

    // private Map<String, List<VocabularyTerm>> vocabularyTermsToAdd = new TreeMap<String, List<VocabularyTerm>>();

    private Set<String> sampleTypesToAdd = new TreeSet<String>();

    private Set<String> experimentTypesToAdd = new TreeSet<String>();

    private Set<String> dataSetTypesToAdd = new TreeSet<String>();

    private Set<String> typeGroupsToAdd = new TreeSet<String>();

    private Map<String, UpdateSummary> sampleTypesToUpdate = new TreeMap<>();

    private Map<String, UpdateSummary> experimentTypesToUpdate = new TreeMap<>();

    private Map<String, UpdateSummary> dataSetTypesToUpdate = new TreeMap<>();

    private Map<String, UpdateSummary> typeGroupsToUpdate = new TreeMap<>();

    public SynchronizerFacade(String openBisServerUrl, String harvesterUser, String harvesterPassword, boolean dryRun, boolean verbose,
            Logger operationLog)
    {
        this(ServiceFinderUtils.getCommonServer(openBisServerUrl), ServiceProvider.getV3ApplicationService(),
                harvesterUser, harvesterPassword, dryRun, verbose, operationLog);
    }

    SynchronizerFacade(ICommonServer commonServer, IApplicationServerApi v3api, String harvesterUser,
            String harvesterPassword, boolean dryRun, boolean verbose, Logger operationLog)
    {
        this.commonServer = commonServer;
        this.v3api = v3api;
        this.sessionToken = ServiceFinderUtils.login(commonServer, harvesterUser, harvesterPassword);
        this.dryRun = dryRun;
        this.verbose = verbose || dryRun;
        this.operationLog = operationLog;
    }

    @Override
    public void updatePropertyTypeAssignment(NewETPTAssignment newETPTAssignment, String diff)
    {
        Map<String, UpdateSummary> summaryMap = getEntityTypeSummaryMap(newETPTAssignment.getEntityKind());
        getEntityTypeSummary(summaryMap, newETPTAssignment.getEntityTypeCode())
                .update(newETPTAssignment.getPropertyTypeCode(), diff);
        if (dryRun == false)
        {
            List<PropertyAssignmentCreation> assignments = new ArrayList<>();
            boolean found = false;
            for (PropertyAssignment existing : getPropertyAssignments(newETPTAssignment.getEntityKind(), newETPTAssignment.getEntityTypeCode()))
            {
                PropertyAssignmentCreation creation = copyAssignment(existing);
                if (existing.getPropertyType().getCode().equals(newETPTAssignment.getPropertyTypeCode()))
                {
                    // Preserve schema fields that older exports do not carry, including uniqueness.
                    applyAssignment(newETPTAssignment, creation);
                    found = true;
                }
                assignments.add(creation);
            }
            if (!found)
            {
                throw new IllegalStateException("Property assignment disappeared during synchronization: "
                        + newETPTAssignment.getEntityTypeCode() + "." + newETPTAssignment.getPropertyTypeCode());
            }
            updateAssignments(newETPTAssignment.getEntityKind(), newETPTAssignment.getEntityTypeCode(),
                    update -> update.set(assignments.toArray(new PropertyAssignmentCreation[0])));
        }
    }

    @Override
    public void assignPropertyType(NewETPTAssignment newETPTAssignment)
    {
        Map<String, UpdateSummary> summaryMap = getEntityTypeSummaryMap(newETPTAssignment.getEntityKind());
        getEntityTypeSummary(summaryMap, newETPTAssignment.getEntityTypeCode())
                .add(newETPTAssignment.getPropertyTypeCode());
        if (dryRun == false)
        {
            PropertyAssignmentCreation creation = new PropertyAssignmentCreation();
            applyAssignment(newETPTAssignment, creation);
            creation.setUnique(newETPTAssignment.isUnique());
            creation.setManagedInternally(newETPTAssignment.isManagedInternally());
            creation.setPattern(newETPTAssignment.getPattern());
            creation.setPatternType(newETPTAssignment.getPatternType());
            updateAssignments(newETPTAssignment.getEntityKind(), newETPTAssignment.getEntityTypeCode(),
                    update -> update.add(creation));
        }
    }

    @Override
    public void unassignPropertyType(EntityKind entityKind, String propertyTypeCode, String entityTypeCode)
    {
        getEntityTypeSummary(getEntityTypeSummaryMap(entityKind), entityTypeCode).remove(propertyTypeCode);
        if (dryRun == false)
        {
            updateAssignments(entityKind, entityTypeCode, update -> update.remove(
                    new PropertyAssignmentPermId(entityTypeId(entityKind, entityTypeCode), new PropertyTypePermId(propertyTypeCode))));
        }
    }

    private static EntityTypePermId entityTypeId(EntityKind kind, String code)
    {
        return new EntityTypePermId(code, ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind.valueOf(kind.name()));
    }

    private void updateAssignments(EntityKind kind, String code, Consumer<PropertyAssignmentListUpdateValue> change)
    {
        EntityTypePermId id = entityTypeId(kind, code);
        switch (kind)
        {
            case SAMPLE:
                SampleTypeUpdate sample = new SampleTypeUpdate();
                sample.setTypeId(id);
                change.accept(sample.getPropertyAssignments());
                v3api.updateSampleTypes(sessionToken, Collections.singletonList(sample));
                break;
            case EXPERIMENT:
                ExperimentTypeUpdate experiment = new ExperimentTypeUpdate();
                experiment.setTypeId(id);
                change.accept(experiment.getPropertyAssignments());
                v3api.updateExperimentTypes(sessionToken, Collections.singletonList(experiment));
                break;
            case DATA_SET:
                DataSetTypeUpdate dataSet = new DataSetTypeUpdate();
                dataSet.setTypeId(id);
                change.accept(dataSet.getPropertyAssignments());
                v3api.updateDataSetTypes(sessionToken, Collections.singletonList(dataSet));
                break;
            default:
                throw new IllegalArgumentException("Unsupported entity kind: " + kind);
        }
    }

    private List<PropertyAssignment> getPropertyAssignments(EntityKind kind, String code)
    {
        EntityTypePermId id = entityTypeId(kind, code);
        switch (kind)
        {
            case SAMPLE:
                SampleTypeFetchOptions samples = new SampleTypeFetchOptions();
                samples.withPropertyAssignments().withPropertyType();
                samples.withPropertyAssignments().withPlugin();
                return v3api.getSampleTypes(sessionToken, Collections.singletonList(id), samples).get(id).getPropertyAssignments();
            case EXPERIMENT:
                ExperimentTypeFetchOptions experiments = new ExperimentTypeFetchOptions();
                experiments.withPropertyAssignments().withPropertyType();
                experiments.withPropertyAssignments().withPlugin();
                return v3api.getExperimentTypes(sessionToken, Collections.singletonList(id), experiments).get(id).getPropertyAssignments();
            case DATA_SET:
                DataSetTypeFetchOptions dataSets = new DataSetTypeFetchOptions();
                dataSets.withPropertyAssignments().withPropertyType();
                dataSets.withPropertyAssignments().withPlugin();
                return v3api.getDataSetTypes(sessionToken, Collections.singletonList(id), dataSets).get(id).getPropertyAssignments();
            default:
                throw new IllegalArgumentException("Unsupported entity kind: " + kind);
        }
    }

    private static PropertyAssignmentCreation copyAssignment(PropertyAssignment assignment)
    {
        PropertyAssignmentCreation creation = new PropertyAssignmentCreation();
        creation.setPropertyTypeId(new PropertyTypePermId(assignment.getPropertyType().getCode()));
        creation.setPluginId(assignment.getPlugin() == null ? null : assignment.getPlugin().getPermId());
        creation.setOrdinal(assignment.getOrdinal());
        creation.setSection(assignment.getSection());
        creation.setMandatory(Boolean.TRUE.equals(assignment.isMandatory()));
        creation.setShowInEditView(Boolean.TRUE.equals(assignment.isShowInEditView()));
        creation.setShowRawValueInForms(Boolean.TRUE.equals(assignment.isShowRawValueInForms()));
        creation.setUnique(Boolean.TRUE.equals(assignment.isUnique()));
        creation.setManagedInternally(Boolean.TRUE.equals(assignment.isManagedInternally()));
        creation.setPattern(assignment.getPattern());
        creation.setPatternType(assignment.getPatternType());
        return creation;
    }

    private static void applyAssignment(NewETPTAssignment assignment, PropertyAssignmentCreation creation)
    {
        creation.setPropertyTypeId(new PropertyTypePermId(assignment.getPropertyTypeCode()));
        creation.setPluginId(assignment.getScriptName() == null ? null : new PluginPermId(assignment.getScriptName()));
        // The parser stores the legacy BO ordinal; the V3 API expects a one-based ordinal.
        creation.setOrdinal(assignment.getOrdinal() == null ? null : Math.toIntExact(assignment.getOrdinal() + 1));
        creation.setSection(assignment.getSection());
        creation.setMandatory(assignment.isMandatory());
        creation.setInitialValueForExistingEntities(assignment.getDefaultValue());
        creation.setShowInEditView(!assignment.isDynamic() && assignment.isShownInEditView());
        creation.setShowRawValueInForms(assignment.getShowRawValue());
    }

    private Map<String, UpdateSummary> getEntityTypeSummaryMap(EntityKind entityKind)
    {
        switch (entityKind)
        {
            case SAMPLE:
                return sampleTypesToUpdate;
            case EXPERIMENT:
                return experimentTypesToUpdate;
            case DATA_SET:
                return dataSetTypesToUpdate;
            default:
                throw new RuntimeException("Unknown entity kind: " + entityKind);
        }
    }

    @Override
    public void updatePropertyType(PropertyType type, Map<String, String> metaData, String diff)
    {
        propertyTypesToUpdate.put(type.getCode(), diff);
        PropertyTypeUpdate update = new PropertyTypeUpdate();
        update.setTypeId(new PropertyTypePermId(type.getCode()));
        update.setLabel(type.getLabel());
        update.setDescription(type.getDescription());
        update.setManagedInternally(type.isManagedInternally());
        update.setSchema(type.getSchema());
        update.setTransformation(type.getTransformation());
        if (metaData != null)
        {
            update.getMetaData().set(metaData);
        }
        if (!dryRun)
        {
            v3api.updatePropertyTypes(sessionToken, Collections.singletonList(update));
        }
    }

    @Override
    public void registerPropertyType(PropertyType type, Map<String, String> metaData)
    {
        propertyTypesToAdd.add(type.getCode());
        PropertyTypeCreation creation = new PropertyTypeCreation();
        creation.setCode(type.getCode());
        creation.setLabel(type.getLabel());
        creation.setDescription(type.getDescription());
        creation.setManagedInternally(type.isManagedInternally());
        creation.setDataType(ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType.valueOf(type.getDataType().getCode().name()));
        creation.setMultiValue(type.isMultiValue());
        creation.setMetaData(metaData);
        creation.setSchema(type.getSchema());
        creation.setTransformation(type.getTransformation());
        if (type.getVocabulary() != null)
        {
            creation.setVocabularyId(new VocabularyPermId(type.getVocabulary().getCode()));
        }
        if (type.getSampleType() != null)
        {
            creation.setSampleTypeId(new EntityTypePermId(type.getSampleType().getCode(),
                    ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind.SAMPLE));
        }
        if (!dryRun)
        {
            v3api.createPropertyTypes(sessionToken, Collections.singletonList(creation));
        }
    }

    @Override
    public void registerTypeGroup(TypeGroupCreation typeGroup)
    {
        typeGroupsToAdd.add(typeGroup.getCode());
        if (dryRun == false)
        {
            v3api.createTypeGroups(sessionToken, Collections.singletonList(typeGroup));
        }
    }

    @Override
    public void updateTypeGroup(TypeGroupCreation typeGroup, String diff)
    {
        getEntityTypeSummary(typeGroupsToUpdate, typeGroup.getCode()).update(diff);
        TypeGroupUpdate update = new TypeGroupUpdate();
        update.setTypeGroupId(new TypeGroupId(typeGroup.getCode()));
        if (typeGroup.getMetaData() != null)
        {
            update.getMetaData().set(typeGroup.getMetaData());
        }
        if (!dryRun)
        {
            v3api.updateTypeGroups(sessionToken, Collections.singletonList(update));
        }
    }

    @Override
    public void assignObjectTypesToTypeGroup(List<TypeGroupAssignmentCreation> assignments)
    {
        if (dryRun == false)
        {
            v3api.createTypeGroupAssignments(sessionToken, assignments);
        }
    }

    @Override
    public void unassignObjectTypeFromTypeGroup(String typeGroupCode, String sampleTypeCode)
    {
        if (dryRun == false)
        {
            TypeGroupAssignmentId id = new TypeGroupAssignmentId(
                    new EntityTypePermId(sampleTypeCode,
                            ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind.SAMPLE),
                    new TypeGroupId(typeGroupCode));
            TypeGroupAssignmentDeletionOptions options = new TypeGroupAssignmentDeletionOptions();
            options.setReason("sync type group assignment removal");
            v3api.deleteTypeGroupAssignments(sessionToken, Collections.singletonList(id), options);
        }
    }

    @Override
    public void updateValidationPlugin(Script script, String diff)
    {
        validationPluginsToUpdate.put(script.getName(), diff);
        if (dryRun == false)
        {
            commonServer.updateScript(sessionToken, script);
        }
    }

    @Override
    public void registerValidationPlugin(Script script)
    {
        validationPluginsToAdd.add(script.getName());
        if (dryRun == false)
        {
            commonServer.registerScript(sessionToken, script);
        }
    }

    @Override
    public void registerVocabulary(NewVocabulary vocab)
    {
        String vocabCode = vocab.getCode();
        vocabulariesToAdd.add(vocabCode);
        if (dryRun == false)
        {
            commonServer.registerVocabulary(sessionToken, vocab);
        }
    }

    @Override
    public void updateVocabulary(Vocabulary vocab, String diff)
    {
        String vocabCode = vocab.getCode();
        getVocabularySummary(vocabCode).update(diff);
        if (dryRun == false)
        {
            commonServer.updateVocabulary(sessionToken, vocab);
        }
    }

    @Override
    public void updateVocabularyTerm(String vocabularyCode, VocabularyTerm term, String diff)
    {
        getVocabularySummary(vocabularyCode).update(term.getCode(), diff);
        if (dryRun == false)
        {
            commonServer.updateVocabularyTerm(sessionToken, term);
        }
    }

    @Override
    public void registerSampleType(SampleType sampleType)
    {
        sampleTypesToAdd.add(sampleType.getCode());
        if (dryRun == false)
        {
            SampleTypeCreation creation = new SampleTypeCreation();
            creation.setCode(sampleType.getCode());
            creation.setDescription(sampleType.getDescription());
            creation.setValidationPluginId(validationPluginId(sampleType));
            creation.setAutoGeneratedCode(sampleType.isAutoGeneratedCode());
            creation.setGeneratedCodePrefix(sampleType.getGeneratedCodePrefix());
            creation.setSubcodeUnique(sampleType.isSubcodeUnique());
            creation.setListable(sampleType.isListable());
            creation.setShowContainer(sampleType.isShowContainer());
            creation.setShowParents(sampleType.isShowParents());
            creation.setShowParentMetadata(sampleType.isShowParentMetadata());
            creation.setMetaData(sampleType.getMetaData());
            v3api.createSampleTypes(sessionToken, Collections.singletonList(creation));
        }
    }

    @Override
    public void registerDataSetType(DataSetType dataSetType)
    {
        dataSetTypesToAdd.add(dataSetType.getCode());
        if (dryRun == false)
        {
            DataSetTypeCreation creation = new DataSetTypeCreation();
            creation.setCode(dataSetType.getCode());
            creation.setDescription(dataSetType.getDescription());
            creation.setValidationPluginId(validationPluginId(dataSetType));
            creation.setDisallowDeletion(dataSetType.isDeletionDisallow());
            creation.setMainDataSetPath(dataSetType.getMainDataSetPath());
            creation.setMainDataSetPattern(dataSetType.getMainDataSetPattern());
            creation.setMetaData(dataSetType.getMetaData());
            v3api.createDataSetTypes(sessionToken, Collections.singletonList(creation));
        }
    }

    @Override
    public void registerExperimentType(ExperimentType experimentType)
    {
        experimentTypesToAdd.add(experimentType.getCode());
        if (dryRun == false)
        {
            ExperimentTypeCreation creation = new ExperimentTypeCreation();
            creation.setCode(experimentType.getCode());
            creation.setDescription(experimentType.getDescription());
            creation.setValidationPluginId(validationPluginId(experimentType));
            creation.setMetaData(experimentType.getMetaData());
            v3api.createExperimentTypes(sessionToken, Collections.singletonList(creation));
        }
    }

    private static PluginPermId validationPluginId(EntityType type)
    {
        return type.getValidationScript() == null ? null : new PluginPermId(type.getValidationScript().getName());
    }

    @Override
    public void updateSampleType(EntityType type, String diff)
    {
        EntityTypePermId id = new EntityTypePermId(type.getCode(),
                ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind.valueOf(type.getEntityKind().name()));
        getEntityTypeSummary(sampleTypesToUpdate, type.getCode()).update(diff);
        SampleType sample = (SampleType) type;
        SampleTypeUpdate sampleUpdate = new SampleTypeUpdate();
        sampleUpdate.setTypeId(id);
        sampleUpdate.setDescription(type.getDescription());
        sampleUpdate.setValidationPluginId(validationPluginId(type));
        sampleUpdate.setAutoGeneratedCode(sample.isAutoGeneratedCode());
        sampleUpdate.setGeneratedCodePrefix(sample.getGeneratedCodePrefix());
        sampleUpdate.setSubcodeUnique(sample.isSubcodeUnique());
        sampleUpdate.setListable(sample.isListable());
        sampleUpdate.setShowContainer(sample.isShowContainer());
        sampleUpdate.setShowParents(sample.isShowParents());
        sampleUpdate.setShowParentMetadata(sample.isShowParentMetadata());
        if (sample.getMetaData() != null)
        {
            sampleUpdate.getMetaData().set(sample.getMetaData());
        }
        if (!dryRun)
        {
            v3api.updateSampleTypes(sessionToken, Collections.singletonList(sampleUpdate));
        }
    }

    @Override
    public void updateDataSetType(EntityType type, String diff)
    {
        EntityTypePermId id = new EntityTypePermId(type.getCode(),
                ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind.valueOf(type.getEntityKind().name()));

        getEntityTypeSummary(dataSetTypesToUpdate, type.getCode()).update(diff);
        DataSetType dataSet = (DataSetType) type;
        DataSetTypeUpdate dataSetUpdate = new DataSetTypeUpdate();
        dataSetUpdate.setTypeId(id);
        dataSetUpdate.setDescription(type.getDescription());
        dataSetUpdate.setValidationPluginId(validationPluginId(type));
        dataSetUpdate.setDisallowDeletion(dataSet.isDeletionDisallow());
        dataSetUpdate.setMainDataSetPath(dataSet.getMainDataSetPath());
        dataSetUpdate.setMainDataSetPattern(dataSet.getMainDataSetPattern());
        if (dataSet.getMetaData() != null)
        {
            dataSetUpdate.getMetaData().set(dataSet.getMetaData());
        }
        if (!dryRun)
        {
            v3api.updateDataSetTypes(sessionToken, Collections.singletonList(dataSetUpdate));
        }
    }

    @Override
    public void updateExperimentType(EntityType type, String diff)
    {
        EntityTypePermId id = new EntityTypePermId(type.getCode(),
                ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind.valueOf(type.getEntityKind().name()));
        getEntityTypeSummary(experimentTypesToUpdate, type.getCode()).update(diff);
        ExperimentTypeUpdate experimentUpdate = new ExperimentTypeUpdate();
        experimentUpdate.setTypeId(id);
        experimentUpdate.setDescription(type.getDescription());
        experimentUpdate.setValidationPluginId(validationPluginId(type));
        Map<String, String> metaData = ((ExperimentType) type).getMetaData();
        if (metaData != null)
        {
            experimentUpdate.getMetaData().set(metaData);
        }
        if (!dryRun)
        {
            v3api.updateExperimentTypes(sessionToken, Collections.singletonList(experimentUpdate));
        }
    }


    @Override
    public void addVocabularyTerms(String vocabularyCode, TechId techId, List<VocabularyTerm> termsToBeAdded)
    {
        for (VocabularyTerm vocabularyTerm : termsToBeAdded)
        {
            getVocabularySummary(vocabularyCode).add(vocabularyTerm.getCode());
        }
        if (dryRun == false)
        {
            commonServer.addVocabularyTerms(sessionToken, techId, termsToBeAdded, null);
        }
    }

    @Override
    public void createExternalDataManagementSystems(List<ExternalDmsCreation> creations)
    {
        if (dryRun == false)
        {
            v3api.createExternalDataManagementSystems(sessionToken, creations);
        }
    }

    @Override
    public void updateExternalDataManagementSystems(List<ExternalDmsUpdate> updates)
    {
        if (dryRun == false)
        {
            v3api.updateExternalDataManagementSystems(sessionToken, updates);
        }
    }

    @Override
    public void printSummary()
    {
        if (verbose)
        {
            SummaryUtils.printAddedSummary(operationLog, validationPluginsToAdd, "validation plugins");
            printUpdatedSummary(validationPluginsToUpdate, "validation plugins");

            SummaryUtils.printAddedSummary(operationLog, vocabulariesToAdd, "vocabularies");
            printUpdateSummary(vocabulariesToAdd, vocabulariesToUpdate, "vocabularies");

            SummaryUtils.printAddedSummary(operationLog, propertyTypesToAdd, "property types");
            printUpdatedSummary(propertyTypesToUpdate, "property types");

            SummaryUtils.printAddedSummary(operationLog, experimentTypesToAdd, "experiment types");
            printUpdateSummary(experimentTypesToAdd, experimentTypesToUpdate, "experiment types");
            SummaryUtils.printAddedSummary(operationLog, sampleTypesToAdd, "sample types");
            printUpdateSummary(sampleTypesToAdd, sampleTypesToUpdate, "sample types");
            SummaryUtils.printAddedSummary(operationLog, dataSetTypesToAdd, "data set types");
            printUpdateSummary(dataSetTypesToAdd, dataSetTypesToUpdate, "data set types");
            SummaryUtils.printAddedSummary(operationLog, typeGroupsToAdd, "type groups");
            printUpdateSummary(typeGroupsToAdd, typeGroupsToUpdate, "type groups");
        }
        SummaryUtils.printShortSummaryHeader(operationLog);
        SummaryUtils.printShortAddedSummary(operationLog, validationPluginsToAdd.size(), "validation plugins");
        SummaryUtils.printShortUpdatedSummary(operationLog, validationPluginsToUpdate.size(), "validation plugins");
        SummaryUtils.printShortAddedSummary(operationLog, vocabulariesToAdd.size(), "vocabularies");
        printShortSummary(vocabulariesToAdd, vocabulariesToUpdate, "vocabularies", "terms");
        SummaryUtils.printShortAddedSummary(operationLog, propertyTypesToAdd.size(), "property types");
        SummaryUtils.printShortUpdatedSummary(operationLog, propertyTypesToUpdate.size(), "property types");
        SummaryUtils.printShortAddedSummary(operationLog, experimentTypesToAdd.size(), "experiment types");
        printShortSummary(experimentTypesToAdd, experimentTypesToUpdate, "experiment types", "property assignments");
        SummaryUtils.printShortAddedSummary(operationLog, sampleTypesToAdd.size(), "sample types");
        printShortSummary(sampleTypesToAdd, sampleTypesToUpdate, "sample types", "property assignments");
        SummaryUtils.printShortAddedSummary(operationLog, dataSetTypesToAdd.size(), "data set types");
        printShortSummary(dataSetTypesToAdd, dataSetTypesToUpdate, "data set types", "property assignments");
        SummaryUtils.printShortAddedSummary(operationLog, typeGroupsToAdd.size(), "type groups");
        printShortSummary(typeGroupsToAdd, typeGroupsToUpdate, "type groups", "object type assignments");
        SummaryUtils.printShortSummaryFooter(operationLog);
    }

    private void printShortSummary(Set<String> added, Map<String, UpdateSummary> updates, String type, String subType)
    {
        int numberOfUpdates = 0;
        int numberOfAdds = 0;
        int numberOfRemoves = 0;
        int numberOfUpdatedItems = 0;
        for (UpdateSummary updateSummary : updates.values())
        {
            if (added.contains(updateSummary.getItem()))
            {
                continue;
            }
            numberOfUpdatedItems++;
            numberOfUpdates += updateSummary.getNumberOfUpdates();
            numberOfAdds += updateSummary.getNumberOfAdds();
            numberOfRemoves += updateSummary.getNumberOfRemoves();
        }
        SummaryUtils.printShortUpdatedSummary(operationLog, numberOfUpdatedItems, type);
        SummaryUtils.printShortAddedSummaryDetail(operationLog, numberOfAdds, subType);
        SummaryUtils.printShortUpdatedSummaryDetail(operationLog, numberOfUpdates, subType);
        SummaryUtils.printShortRemovedSummaryDetail(operationLog, numberOfRemoves, subType);
    }

    private void printUpdateSummary(Set<String> addedEntityTypes, Map<String, UpdateSummary> summaries, String itemType)
    {
        List<String> details = new LinkedList<>();
        for (Entry<String, UpdateSummary> entry : summaries.entrySet())
        {
            String entityType = entry.getKey();
            if (addedEntityTypes.contains(entityType))
            {
                continue;
            }
            UpdateSummary summary = entry.getValue();
            String diff = summary.getDiff();
            details.add(entityType + " " + (diff == null ? "no basic changes" : diff));
            Map<String, String> assignmentChanges = summary.getChanges();
            for (Entry<String, String> entry2 : assignmentChanges.entrySet())
            {
                details.add("    " + entry2.getKey() + ": " + entry2.getValue());
            }
        }
        SummaryUtils.printUpdatedSummary(operationLog, details, itemType);
    }

    private void printUpdatedSummary(Map<String, String> map, String type)
    {
        List<String> details = new LinkedList<>();
        for (String key : map.keySet())
        {
            details.add(key + " - " + map.get(key));
        }
        SummaryUtils.printUpdatedSummary(operationLog, details, type);
    }

    private UpdateSummary getEntityTypeSummary(Map<String, UpdateSummary> summariesByType, String entityTypeCode)
    {
        UpdateSummary summary = summariesByType.get(entityTypeCode);
        if (summary == null)
        {
            summary = new UpdateSummary(entityTypeCode);
            summariesByType.put(entityTypeCode, summary);
        }
        return summary;
    }

    private UpdateSummary getVocabularySummary(String vocabularyCode)
    {
        UpdateSummary vocabularySummary = vocabulariesToUpdate.get(vocabularyCode);
        if (vocabularySummary == null)
        {
            vocabularySummary = new UpdateSummary(vocabularyCode);
            vocabulariesToUpdate.put(vocabularyCode, vocabularySummary);
        }
        return vocabularySummary;
    }

    private static final class UpdateSummary
    {
        private final String item;

        private String diff;

        private Map<String, String> changes = new TreeMap<>();

        private int numberOfUpdates;

        private int numberOfAdds;

        private int numberOfRemoves;

        public UpdateSummary(String item)
        {
            this.item = item;
        }

        void update(String diff)
        {
            this.diff = diff;
        }

        void update(String item, String diff)
        {
            changes.put(item, diff);
            numberOfUpdates++;
        }

        void add(String item)
        {
            changes.put(item, "ADDED");
            numberOfAdds++;
        }

        void remove(String item)
        {
            changes.put(item, "REMOVED");
            numberOfRemoves++;
        }

        public String getItem()
        {
            return item;
        }

        public String getDiff()
        {
            return diff;
        }

        public Map<String, String> getChanges()
        {
            return changes;
        }

        public int getNumberOfUpdates()
        {
            return numberOfUpdates;
        }

        public int getNumberOfAdds()
        {
            return numberOfAdds;
        }

        public int getNumberOfRemoves()
        {
            return numberOfRemoves;
        }
    }

}
