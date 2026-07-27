/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.xls.importer.helper;

import java.util.*;
import java.util.stream.Collectors;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.delete.TypeGroupAssignmentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.delay.DelayedExecutionDecorator;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.handler.JSONHandler;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationRecord;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationType;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.AttributeValidator;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.IAttribute;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.ImportUtils;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.VersionUtils;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

public class SampleTypeImportHelper extends BasicImportHelper
{
    private enum Attribute implements IAttribute {
        Version("Version", false, false),
        Code("Code", true, true),
        Description("Description", true, false),
        AutoGenerateCodes("Auto generate codes", true, false),
        ValidationScript("Validation script", true, false),
        GeneratedCodePrefix("Generated code prefix", true, false),
        OntologyId("Ontology Id", false, false),
        OntologyVersion("Ontology Version", false, false),
        OntologyAnnotationId("Ontology Annotation Id", false, false),
        TypeGroups("Type Groups", false, false),
        Metadata("Meta Data", false, false),
        Internal("Internal", false, false);

        private final String headerName;

        private final boolean mandatory;

        private final boolean upperCase;

        Attribute(String headerName, boolean mandatory, boolean upperCase)
        {
            this.headerName = headerName;
            this.mandatory = mandatory;
            this.upperCase = upperCase;
        }

        public String getHeaderName()
        {
            return headerName;
        }

        @Override
        public boolean isMandatory()
        {
            return mandatory;
        }

        @Override
        public boolean isUpperCase()
        {
            return upperCase;
        }
    }

    private final DelayedExecutionDecorator delayedExecutor;

    private final Map<String, Integer> versions;

    private final AttributeValidator<Attribute> attributeValidator;


    public SampleTypeImportHelper(DelayedExecutionDecorator delayedExecutor, ImportModes mode, ImportOptions options, Map<String, Integer> versions)
    {
        super(mode, options);
        this.versions = versions;
        this.delayedExecutor = delayedExecutor;
        this.attributeValidator = new AttributeValidator<>(Attribute.class);
    }

    @Override protected ImportTypes getTypeName()
    {
        return ImportTypes.SAMPLE_TYPE;
    }


    @Override protected void updateVersion(Map<String, Integer> header, List<String> values)
    {
        String version = getValueByColumnName(header, values, Attribute.Version);
        String code = getValueByColumnName(header, values, Attribute.Code);

        if (version == null || version.isEmpty()) {
            Integer storedVersion = VersionUtils.getStoredVersion(versions, ImportTypes.SAMPLE_TYPE.getType(), code);
            storedVersion++;
            version = storedVersion.toString();
        }

        VersionUtils.updateVersion(version, versions, ImportTypes.SAMPLE_TYPE.getType(), code);
    }

    @Override
    protected boolean isNewVersion(Map<String, Integer> header, List<String> values)
    {
        String internal = getValueByColumnName(header, values, Attribute.Internal);
        SampleType sampleType = getSampleType(header, values);
        boolean isInternalNamespace = ImportUtils.isTrue(internal) || (sampleType != null && sampleType.isManagedInternally());

        if(isInternalNamespace && !delayedExecutor.isSystem()) {
            //if exists, skip
            return sampleType == null;
        }
        return true;
    }

    private SampleType getSampleType(Map<String, Integer> header, List<String> values)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);

        if(hasSemanticAnnotations(header, values)) {
            List<SemanticAnnotationRecord> records = getSemanticAnnotationRecords(header, values);
            EntityTypePermId permId = new EntityTypePermId(code, EntityKind.SAMPLE);
            SemanticAnnotation annotation = delayedExecutor.getEntityTypeSemanticAnnotation(records, permId);
            if(annotation != null) {
                // if there is semantic annotation, then there is an associated type
                return (SampleType) annotation.getEntityType();
            }
        } else {
            if (code == null)
            {
                throw new UserFailureException("Mandatory field is missing or empty: " + Attribute.Code);
            }
        }

        EntityTypePermId id = new EntityTypePermId(code, EntityKind.SAMPLE);
        return delayedExecutor.getSampleType(id, new SampleTypeFetchOptions());
    }

    @Override protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        return getSampleType(header, values) != null;
    }

    @Override protected void createObject(Map<String, Integer> header, List<String> values, int page, int line)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        String description = getValueByColumnName(header, values, Attribute.Description);
        String validationScript = getValueByColumnName(header, values, Attribute.ValidationScript);
        String autoGenerateCodes = getValueByColumnName(header, values, Attribute.AutoGenerateCodes);
        String generatedCodePrefix = getValueByColumnName(header, values, Attribute.GeneratedCodePrefix);
        String internal = getValueByColumnName(header, values, Attribute.Internal);
        String metaData = getValueByColumnName(header, values, Attribute.Metadata);

        SampleTypeCreation creation = new SampleTypeCreation();

        creation.setCode(code);
        creation.setDescription(description);
        creation.setAutoGeneratedCode(Boolean.parseBoolean(autoGenerateCodes));
        creation.setValidationPluginId(ImportUtils.getScriptId(code, validationScript, null));
        if (generatedCodePrefix != null && !generatedCodePrefix.isEmpty())
        {
            creation.setGeneratedCodePrefix(generatedCodePrefix);
        }
        if(delayedExecutor.isSystem())
        {
            creation.setManagedInternally(ImportUtils.isTrue(internal));
        }
        if (metaData != null && !metaData.isEmpty()) {
            creation.setMetaData(JSONHandler.parseMetaData(metaData));
        }

        delayedExecutor.createSampleType(creation, page, line);

        String[] typeGroups = getMultiValueByColumnName(header, values, Attribute.TypeGroups, "\n");
        if(typeGroups != null) {
            List<TypeGroupAssignmentCreation> typeGroupAssignmentCreations = new ArrayList<>();
            for(String typeGroup : typeGroups)
            {
                TypeGroupAssignmentCreation assignmentCreation = new TypeGroupAssignmentCreation();
                assignmentCreation.setSampleTypeId(new EntityTypePermId(code, EntityKind.SAMPLE));
                assignmentCreation.setTypeGroupId(new TypeGroupId(typeGroup));
                assignmentCreation.setManagedInternally(false);
                typeGroupAssignmentCreations.add(assignmentCreation);
            }
            delayedExecutor.createTypeGroupAssignments(typeGroupAssignmentCreations, page, line);
        }
    }

    @Override protected void updateObject(Map<String, Integer> header, List<String> values, int page, int line)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        String description = getValueByColumnName(header, values, Attribute.Description);
        String validationScript = getValueByColumnName(header, values, Attribute.ValidationScript);
        String autoGenerateCodes = getValueByColumnName(header, values, Attribute.AutoGenerateCodes);
        String generatedCodePrefix = getValueByColumnName(header, values, Attribute.GeneratedCodePrefix);
        String metaData = getValueByColumnName(header, values, Attribute.Metadata);
        String internal = getValueByColumnName(header, values, Attribute.Internal);

        SampleTypeUpdate update = new SampleTypeUpdate();
        EntityTypePermId permId = new EntityTypePermId(code, EntityKind.SAMPLE);

        SemanticAnnotation annotation = delayedExecutor.getCachedSemanticAnnotation(SemanticAnnotationType.EntityType, permId, null);

        if(annotation != null) {
            code = annotation.getEntityType().getCode();
            permId = new EntityTypePermId(code, EntityKind.SAMPLE);
        }

        if(delayedExecutor.isSystem() && internal != null && !internal.isEmpty()) {
            update.setManagedInternally(ImportUtils.isTrue(internal));
        }

        update.setTypeId(permId);
        if (description != null)
        {
            if (description.equals("--DELETE--") || description.equals("__DELETE__"))
            {
                update.setDescription("");
            } else if (!description.isEmpty())
            {
                update.setDescription(description);
            }
        }
        update.setAutoGeneratedCode(Boolean.parseBoolean(autoGenerateCodes));

        SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
        sampleTypeFetchOptions.withValidationPlugin();
        SampleType sampleType = delayedExecutor.getSampleType(new EntityTypePermId(code, EntityKind.SAMPLE), sampleTypeFetchOptions);

        update.setValidationPluginId(ImportUtils.getScriptId(code, validationScript, sampleType.getValidationPlugin()));
        if (generatedCodePrefix != null && !generatedCodePrefix.isEmpty())
        {
            update.setGeneratedCodePrefix(generatedCodePrefix);
        }
        if (metaData != null && !metaData.isEmpty())
        {
            update.getMetaData().add(JSONHandler.parseMetaData(metaData));
        }

        delayedExecutor.updateSampleType(update, page, line);

        String[] typeGroups = getMultiValueByColumnName(header, values, Attribute.TypeGroups, "\n");
        if(typeGroups != null) {
            TypeGroupAssignmentFetchOptions fetchOptions = new TypeGroupAssignmentFetchOptions();
            fetchOptions.withTypeGroup();
            List<TypeGroupAssignment> assignments = delayedExecutor.getTypeGroupAssignmentsForSampleType(code, fetchOptions);
            Set<String> existingGroups = assignments.stream().map(x -> x.getTypeGroup().getCode()).collect(
                    Collectors.toSet());
            Set<String> newGroups = new HashSet<>(List.of(typeGroups));
            List<ITypeGroupAssignmentId> typeGroupAssignmentIds = new ArrayList<>();
            TypeGroupAssignmentDeletionOptions deletionOptions = new TypeGroupAssignmentDeletionOptions();
            for(TypeGroupAssignment assignment : assignments)
            {
                if(!newGroups.contains(assignment.getTypeGroup().getCode()))
                {
                    // delete assignments
                    TypeGroupAssignmentId id = new TypeGroupAssignmentId(permId, new TypeGroupId(assignment.getTypeGroup().getCode()));
                    typeGroupAssignmentIds.add(id);
                }
            }
            if(!typeGroupAssignmentIds.isEmpty())
            {
                delayedExecutor.deleteTypeGroupAssignments(typeGroupAssignmentIds, deletionOptions);
            }

            List<TypeGroupAssignmentCreation> typeGroupAssignmentCreations = new ArrayList<>();
            for(String typeGroup : typeGroups)
            {
                if(!existingGroups.contains(typeGroup))
                {
                    //create assignment
                    TypeGroupAssignmentCreation assignmentCreation = new TypeGroupAssignmentCreation();
                    assignmentCreation.setSampleTypeId(new EntityTypePermId(code, EntityKind.SAMPLE));
                    assignmentCreation.setTypeGroupId(new TypeGroupId(typeGroup));
                    assignmentCreation.setManagedInternally(false);
                    typeGroupAssignmentCreations.add(assignmentCreation);
                }
            }
            if(!typeGroupAssignmentCreations.isEmpty())
            {
                delayedExecutor.createTypeGroupAssignments(typeGroupAssignmentCreations, page, line);
            }
        }

    }

    @Override protected void validateHeader(Map<String, Integer> header)
    {
        attributeValidator.validateHeaders(Attribute.values(), header);
    }
}
