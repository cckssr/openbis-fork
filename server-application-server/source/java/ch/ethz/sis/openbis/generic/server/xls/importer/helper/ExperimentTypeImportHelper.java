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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.delay.DelayedExecutionDecorator;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.handler.JSONHandler;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationRecord;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationType;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.AttributeValidator;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.IAttribute;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.ImportUtils;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.VersionUtils;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

public class ExperimentTypeImportHelper extends BasicImportHelper
{
    private enum Attribute implements IAttribute {
        Version("Version", false, false),
        Code("Code", true, true),
        Description("Description", true, false),
        ValidationScript("Validation script", true, false),
        OntologyId("Ontology Id", false, false),
        OntologyVersion("Ontology Version", false, false),
        OntologyAnnotationId("Ontology Annotation Id", false, false),
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

        @Override
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

    public ExperimentTypeImportHelper(DelayedExecutionDecorator delayedExecutor, ImportModes mode, ImportOptions options, Map<String, Integer> versions, SemanticAnnotationHelper annotationCache)
    {
        super(mode, options);
        this.versions = versions;
        this.delayedExecutor = delayedExecutor;
        this.attributeValidator = new AttributeValidator<>(Attribute.class);
        this.annotationCache = annotationCache;
    }

    @Override protected ImportTypes getTypeName()
    {
        return ImportTypes.EXPERIMENT_TYPE;
    }


    @Override protected void updateVersion(Map<String, Integer> header, List<String> values)
    {
        String version = getValueByColumnName(header, values, Attribute.Version);
        String code = getValueByColumnName(header, values, Attribute.Code);

        if (version == null || version.isEmpty()) {
            Integer storedVersion = VersionUtils.getStoredVersion(versions, ImportTypes.EXPERIMENT_TYPE.getType(), code);
            storedVersion++;
            version = storedVersion.toString();
        }

        VersionUtils.updateVersion(version, versions, ImportTypes.EXPERIMENT_TYPE.getType(), code);
    }

    @Override
    protected boolean isNewVersion(Map<String, Integer> header, List<String> values)
    {
        String internal = getValueByColumnName(header, values, Attribute.Internal);
        ExperimentType experimentType = getExperimentType(header, values);
        boolean isInternalNamespace = ImportUtils.isTrue(internal) || (experimentType != null && experimentType.isManagedInternally());

        if(isInternalNamespace && !delayedExecutor.isSystem()) {
            //if exists, skip
            return experimentType == null;
        }
        return true;
    }

    private ExperimentType getExperimentType(Map<String, Integer> header, List<String> values)
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
            EntityTypePermId permId = new EntityTypePermId(code, EntityKind.EXPERIMENT);
            SemanticAnnotation annotation = annotationCache.getSemanticAnnotation(records.toArray(new SemanticAnnotationRecord[0]), permId, null);
            if(annotation != null) {
                // if there is semantic annotation, then there is an associated type
                return (ExperimentType) annotation.getEntityType();
            }
        } else {
            if (code == null)
            {
                throw new UserFailureException("Mandatory field is missing or empty: " + Attribute.Code);
            }
        }

        EntityTypePermId id = new EntityTypePermId(code, EntityKind.EXPERIMENT);
        return delayedExecutor.getExperimentType(id, new ExperimentTypeFetchOptions());
    }

    @Override protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        return getExperimentType(header, values) != null;
    }

    @Override protected void createObject(Map<String, Integer> header, List<String> values, int page, int line)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        String description = getValueByColumnName(header, values, Attribute.Description);
        String validationScript = getValueByColumnName(header, values, Attribute.ValidationScript);
        String internal = getValueByColumnName(header, values, Attribute.Internal);
        String metaData = getValueByColumnName(header, values, Attribute.Metadata);

        ExperimentTypeCreation creation = new ExperimentTypeCreation();

        creation.setCode(code);
        creation.setDescription(description);
        creation.setValidationPluginId(ImportUtils.getScriptId(code, validationScript, null));
        if(delayedExecutor.isSystem())
        {
            creation.setManagedInternally(ImportUtils.isTrue(internal));
        }
        if (metaData != null && !metaData.isEmpty()) {
            creation.setMetaData(JSONHandler.parseMetaData(metaData));
        }

        delayedExecutor.createExperimentType(creation, page, line);
    }

    @Override protected void updateObject(Map<String, Integer> header, List<String> values, int page, int line)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        String description = getValueByColumnName(header, values, Attribute.Description);
        String validationScript = getValueByColumnName(header, values, Attribute.ValidationScript);
        String metaData = getValueByColumnName(header, values, Attribute.Metadata);
        String internal = getValueByColumnName(header, values, Attribute.Internal);

        ExperimentTypeUpdate update = new ExperimentTypeUpdate();
        EntityTypePermId permId = new EntityTypePermId(code, EntityKind.EXPERIMENT);

        SemanticAnnotation annotation = annotationCache.getCachedSemanticAnnotation(
                SemanticAnnotationType.EntityType, permId, null);

        if(annotation != null) {
            code = annotation.getEntityType().getCode();
            permId = new EntityTypePermId(code, EntityKind.EXPERIMENT);
        }

        update.setTypeId(permId);

        if(delayedExecutor.isSystem() && internal != null && !internal.isEmpty()) {
            update.setManagedInternally(ImportUtils.isTrue(internal));
        }

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

        ExperimentTypeFetchOptions experimentTypeFetchOptions = new ExperimentTypeFetchOptions();
        experimentTypeFetchOptions.withValidationPlugin();
        ExperimentType experimentType = delayedExecutor.getExperimentType(new EntityTypePermId(code, EntityKind.EXPERIMENT), experimentTypeFetchOptions);
        update.setValidationPluginId(ImportUtils.getScriptId(code, validationScript, experimentType.getValidationPlugin()));
        if (metaData != null && !metaData.isEmpty())
        {
            update.getMetaData().add(JSONHandler.parseMetaData(metaData));
        }
        delayedExecutor.updateExperimentType(update, page, line);
    }

    @Override protected void validateHeader(Map<String, Integer> header)
    {
        attributeValidator.validateHeaders(Attribute.values(), header);
    }
}
