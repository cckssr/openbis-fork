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
import java.util.stream.IntStream;

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
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationHelper;
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
        TypeGroup("Type Group", false, false),
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

    private final SemanticAnnotationHelper annotationCache;

    public SampleTypeImportHelper(DelayedExecutionDecorator delayedExecutor, ImportModes mode, ImportOptions options, Map<String, Integer> versions, SemanticAnnotationHelper annotationCache)
    {
        super(mode, options);
        this.versions = versions;
        this.delayedExecutor = delayedExecutor;
        this.attributeValidator = new AttributeValidator<>(Attribute.class);
        this.annotationCache = annotationCache;
    }

    @Override protected ImportTypes getTypeName()
    {
        return ImportTypes.SAMPLE_TYPE;
    }

    @Override
    protected void validateLine(Map<String, Integer> header, List<String> values) {
        String code = getValueByColumnName(header, values, Attribute.Code);
        String internal = getValueByColumnName(header, values, Attribute.Internal);

        if(!delayedExecutor.isSystem() && ImportUtils.isTrue(internal))
        {
            SampleType
                    st = delayedExecutor.getSampleType(new EntityTypePermId(code), new SampleTypeFetchOptions());
            if(st == null) {
                throw new UserFailureException("Non-system user can not create new internal entity types!");
            }
        }
    }

    @Override protected boolean isNewVersion(Map<String, Integer> header, List<String> values)
    {
        return isNewVersionWithInternalNamespace(header, values, versions,
                delayedExecutor.isSystem(),
                getTypeName().getType(),
                Attribute.Version, Attribute.Code, Attribute.Internal);
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

    @Override protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);

        String[] ontologyId = getMultiValueByColumnName(header, values, SemanticAnnotationImportHelper.Attribute.OntologyId, "\n");
        if(ontologyId != null) {
            String[] ontologyVersion = getMultiValueByColumnName(header, values, SemanticAnnotationImportHelper.Attribute.OntologyVersion, "\n");
            String[] ontologyAnnotationId =  getMultiValueByColumnName(header, values, SemanticAnnotationImportHelper.Attribute.OntologyAnnotationId, "\n");
            if(ontologyVersion == null) {
                throw new UserFailureException("Mandatory field is missing or empty: " + Attribute.OntologyVersion);
            }
            if(ontologyAnnotationId == null) {
                throw new UserFailureException("Mandatory field is missing or empty: " + Attribute.OntologyAnnotationId);
            }
            if(ontologyId.length != ontologyVersion.length || ontologyId.length != ontologyAnnotationId.length) {
                throw new UserFailureException("Number of ontology triplets does not match!");
            }

            List<SemanticAnnotationRecord> records =
                IntStream.range(0, ontologyId.length)
                        .mapToObj(i -> new SemanticAnnotationRecord(ontologyId[i], ontologyVersion[i], ontologyAnnotationId[i]))
                        .collect(Collectors.toList());
            EntityTypePermId permId = new EntityTypePermId(code, EntityKind.SAMPLE);
            SemanticAnnotation annotation = annotationCache.getSemanticAnnotation(records.toArray(new SemanticAnnotationRecord[0]), permId, null);
            if(annotation != null) {
                // if there is semantic annotation, then there is an associated type
                return true;
            }
        } else {
            if (code == null)
            {
                throw new UserFailureException("Mandatory field is missing or empty: " + Attribute.Code);
            }
        }

        EntityTypePermId id = new EntityTypePermId(code, EntityKind.SAMPLE);
        return delayedExecutor.getSampleType(id, new SampleTypeFetchOptions()) != null;
    }

    @Override protected void createObject(Map<String, Integer> header, List<String> values, int page, int line)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        String description = getValueByColumnName(header, values, Attribute.Description);
        String validationScript = getValueByColumnName(header, values, Attribute.ValidationScript);
        String autoGenerateCodes = getValueByColumnName(header, values, Attribute.AutoGenerateCodes);
        String generatedCodePrefix = getValueByColumnName(header, values, Attribute.GeneratedCodePrefix);
        String internal = getValueByColumnName(header, values, Attribute.Internal);

        SampleTypeCreation creation = new SampleTypeCreation();

        creation.setCode(code);
        creation.setDescription(description);
        creation.setAutoGeneratedCode(Boolean.parseBoolean(autoGenerateCodes));
        creation.setValidationPluginId(ImportUtils.getScriptId(validationScript, null));
        if (generatedCodePrefix != null && !generatedCodePrefix.isEmpty())
        {
            creation.setGeneratedCodePrefix(generatedCodePrefix);
        }
        creation.setManagedInternally(ImportUtils.isTrue(internal));

        delayedExecutor.createSampleType(creation, page, line);

        String[] typeGroups = getMultiValueByColumnName(header, values, Attribute.TypeGroup, "\n");
        if(typeGroups != null) {
            List<TypeGroupAssignmentCreation> typeGroupAssignmentCreations = new ArrayList<>();
            for(String typeGroup : typeGroups)
            {
                TypeGroupAssignmentCreation assignmentCreation = new TypeGroupAssignmentCreation();
                assignmentCreation.setSampleTypeId(new EntityTypePermId(code, EntityKind.SAMPLE));
                assignmentCreation.setTypeGroupId(new TypeGroupId(typeGroup));
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

        SampleTypeUpdate update = new SampleTypeUpdate();
        EntityTypePermId permId = new EntityTypePermId(code, EntityKind.SAMPLE);

        SemanticAnnotation annotation = annotationCache.getCachedSemanticAnnotation(SemanticAnnotationType.EntityType, permId, null);

        if(annotation != null) {
            code = annotation.getEntityType().getCode();
            permId = new EntityTypePermId(code, EntityKind.SAMPLE);
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

        update.setValidationPluginId(ImportUtils.getScriptId(validationScript, sampleType.getValidationPlugin()));
        if (generatedCodePrefix != null && !generatedCodePrefix.isEmpty())
        {
            update.setGeneratedCodePrefix(generatedCodePrefix);
        }

        delayedExecutor.updateSampleType(update, page, line);

        String[] typeGroups = getMultiValueByColumnName(header, values, Attribute.TypeGroup, "\n");
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
