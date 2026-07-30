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

import static ch.ethz.sis.openbis.generic.server.xls.importer.utils.PropertyTypeSearcher.SAMPLE_DATA_TYPE_MANDATORY_TYPE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationRecord;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationType;
import ch.ethz.sis.shared.log.classic.impl.Logger;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.update.PropertyTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyPermId;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.delay.DelayedExecutionDecorator;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.handler.JSONHandler;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.AttributeValidator;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.IAttribute;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.ImportUtils;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.VersionUtils;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;

public class PropertyTypeImportHelper extends BasicImportHelper
{
    private static final Logger operationLog =
            LogFactory.getLogger(LogCategory.OPERATION, PropertyTypeImportHelper.class);

    private enum Attribute implements IAttribute
    {
        Version("Version", false, false),
        Code("Code", true, true),
        Mandatory("Mandatory", false, false),
        DefaultValue("Default Value",
                false, false),  // Ignored, only used by PropertyAssignmentImportHelper
        ShowInEditViews("Show in edit views", false, false),
        Section("Section", false, false),
        PropertyLabel("Property label", true, false),
        DataType("Data type", true, true),
        VocabularyCode("Vocabulary code", true, true),
        Description("Description", true, false),
        Metadata("Metadata", false, false),
        DynamicScript("Dynamic script", false, false),
        OntologyId("Ontology Id", false, false),
        OntologyVersion("Ontology Version", false, false),
        OntologyAnnotationId("Ontology Annotation Id", false, false),
        MultiValued("Multivalued", false, false),
        Unique("Unique", false, false),
        Pattern("Pattern", false, false),
        PatternType("Pattern Type", false, false),
        Internal("Internal", false, false),
        InternalAssignment("Internal Assignment", false, false);


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

    private final Map<String, String> propertyCache;

    private final AttributeValidator<Attribute> attributeValidator;

    private EntityTypePermId typePermId;

    public PropertyTypeImportHelper(DelayedExecutionDecorator delayedExecutor, ImportModes mode,
            ImportOptions options, Map<String, Integer> versions)
    {
        super(mode, options);
        this.versions = versions;
        this.delayedExecutor = delayedExecutor;
        this.propertyCache = new HashMap<>();
        this.attributeValidator = new AttributeValidator<>(Attribute.class);
    }

    @Override
    protected void validateLine(Map<String, Integer> headers, List<String> values)
    {
        // Validate Unambiguous
        String code = getValueByColumnName(headers, values, Attribute.Code);
        String propertyLabel = getValueByColumnName(headers, values, Attribute.PropertyLabel);
        String description = getValueByColumnName(headers, values, Attribute.Description);
        String dataType = getValueByColumnName(headers, values, Attribute.DataType);
        String vocabularyCode = getValueByColumnName(headers, values, Attribute.VocabularyCode);
        String metadata = getValueByColumnName(headers, values, Attribute.Metadata);

        if(code == null || code.isEmpty()) {
            throw new UserFailureException("Mandatory field is missing or empty: Code.");
        }
        String propertyData =
                code + propertyLabel + description + dataType + vocabularyCode + metadata;
        if (this.propertyCache.get(code) == null)
        {
            this.propertyCache.put(code, propertyData);
        }
        if (!propertyData.equals(this.propertyCache.get(code)))
        {
            throw new UserFailureException("Ambiguous property " + code + " found, it has been declared before with different attributes.");
        }

    }

    @Override
    protected boolean isNewVersion(Map<String, Integer> header, List<String> values)
    {
        PropertyType propertyType = getPropertyType(header, values);
        String internal = getValueByColumnName(header, values, Attribute.Internal);
        boolean isInternalNamespace = ImportUtils.isTrue(internal) || (propertyType != null && propertyType.isManagedInternally());

        if(isInternalNamespace && !delayedExecutor.isSystem()) {
           return propertyType == null;
        }
        return true;
    }

    @Override
    protected ImportTypes getTypeName()
    {
        return ImportTypes.PROPERTY_TYPE;
    }


    @Override
    protected void updateVersion(Map<String, Integer> header, List<String> values)
    {
        String version = getValueByColumnName(header, values, Attribute.Version);
        String code = getValueByColumnName(header, values, Attribute.Code);

        if (version == null || version.isEmpty()) {
            Integer storedVersion = VersionUtils.getStoredVersion(versions, ImportTypes.PROPERTY_TYPE.getType(), code);
            storedVersion++;
            version = storedVersion.toString();
        }

        VersionUtils.updateVersion(version, versions, ImportTypes.PROPERTY_TYPE.getType(), code);
    }


    public void importBlock(List<List<String>> page, int pageIndex, int start, int end, ImportTypes importTypes)
    {
        Map<String, Integer> header = parseHeader(page.get(start-2), false);
        String entityTypeCode = getValueByColumnName(header, page.get(start - 1), Attribute.Code);

        EntityTypePermId permId = null;

        switch (importTypes) {
            case SAMPLE_TYPE:
                permId = new EntityTypePermId(entityTypeCode, EntityKind.SAMPLE);
                break;
            case DATASET_TYPE:
                permId = new EntityTypePermId(entityTypeCode, EntityKind.DATA_SET);
                break;
            case EXPERIMENT_TYPE:
                permId = new EntityTypePermId(entityTypeCode, EntityKind.EXPERIMENT);
                break;
        }
        if(permId != null)
        {
            List<SemanticAnnotation> annotations =
                    delayedExecutor.getSemanticAnnotations(SemanticAnnotationType.EntityType,
                            permId, null);
            if(annotations != null && !annotations.isEmpty())
            {
                permId = new EntityTypePermId(annotations.get(0).getEntityType().getCode(),
                        permId.getEntityKind());
            }
        }
        typePermId = permId;

        super.importBlock(page, pageIndex, start, end);
    }

    private PropertyType getPropertyType(Map<String, Integer> header, List<String> values)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        if(hasSemanticAnnotations(header, values)) {

            List<SemanticAnnotationRecord> records = getSemanticAnnotationRecords(header, values);
            Map<SemanticAnnotationRecord, SemanticAnnotation> annotationMap = delayedExecutor.findPropertyTypeSemanticAnnotationsFromRecords(records, code);
            if(annotationMap != null && !annotationMap.isEmpty()) {
                // there is a type for semantic annotation
                return annotationMap.entrySet().iterator().next().getValue().getPropertyType();
            }
        }

        PropertyTypePermId propertyTypePermId = new PropertyTypePermId(code);
        return delayedExecutor.getPropertyType(propertyTypePermId);
    }

    @Override
    protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        return getPropertyType(header, values) != null;
    }

    @Override
    protected void createObject(Map<String, Integer> header, List<String> values, int page,
                                int line)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        String propertyLabel = getValueByColumnName(header, values, Attribute.PropertyLabel);
        String description = getValueByColumnName(header, values, Attribute.Description);
        String dataTypeXLS = getValueByColumnName(header, values, Attribute.DataType);

        DataType dataType = null;
        String dataTypeObjectType = null;
        if (dataTypeXLS.contains(SAMPLE_DATA_TYPE_MANDATORY_TYPE))
        {
            dataType = DataType.valueOf(dataTypeXLS.split(SAMPLE_DATA_TYPE_MANDATORY_TYPE)[0]);
            dataTypeObjectType = dataTypeXLS.split(SAMPLE_DATA_TYPE_MANDATORY_TYPE)[1];
        } else {
            dataType = DataType.valueOf(dataTypeXLS);
        }

        String vocabularyCode = getValueByColumnName(header, values, Attribute.VocabularyCode);
        String metadata = getValueByColumnName(header, values, Attribute.Metadata);
        String multiValued = getValueByColumnName(header, values, Attribute.MultiValued);
        String internal = getValueByColumnName(header, values, Attribute.Internal);

        PropertyTypeCreation creation = new PropertyTypeCreation();
        creation.setCode(code);
        creation.setLabel(propertyLabel);
        creation.setDescription(description);
        creation.setDataType(dataType);
        if (dataType == DataType.SAMPLE && dataTypeObjectType != null)
        {
            creation.setSampleTypeId(new EntityTypePermId(dataTypeObjectType, EntityKind.SAMPLE));
        }

        if(delayedExecutor.isSystem())
        {
            creation.setManagedInternally(ImportUtils.isTrue(internal));
        }
        creation.setMultiValue(ImportUtils.isTrue(multiValued));

        if (dataType == DataType.CONTROLLEDVOCABULARY && vocabularyCode != null && !vocabularyCode.isEmpty())
        {
            creation.setVocabularyId(new VocabularyPermId(vocabularyCode));
        } else if(dataType != DataType.CONTROLLEDVOCABULARY && vocabularyCode != null && !vocabularyCode.isEmpty()) {
            throw new UserFailureException("Ambiguous Property type declaration, the dataType is not CONTROLLEDVOCABULARY but it has a controlled vocabulary set.");
        }
        if (metadata != null && !metadata.trim().isEmpty())
        {
            creation.setMetaData(JSONHandler.parseMetaData(metadata));
        }


        delayedExecutor.createPropertyType(creation, page, line);
    }

    @Override
    protected void updateObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        String propertyLabel = getValueByColumnName(header, values, Attribute.PropertyLabel);
        String description = getValueByColumnName(header, values, Attribute.Description);
        String dataType = getValueByColumnName(header, values, Attribute.DataType);
        String vocabularyCode = getValueByColumnName(header, values, Attribute.VocabularyCode);
        String metadata = getValueByColumnName(header, values, Attribute.Metadata);
        String internal = getValueByColumnName(header, values, Attribute.Internal);

        List<SemanticAnnotation> annotations = delayedExecutor.getSemanticAnnotations(SemanticAnnotationType.PropertyType, null, code);
        if(annotations != null && !annotations.isEmpty()) {
            code = annotations.get(0).getPropertyType().getCode();
        } else if(typePermId != null) {
            annotations = delayedExecutor.getSemanticAnnotations(SemanticAnnotationType.PropertyAssignment, typePermId, code);
            if(annotations != null && !annotations.isEmpty())
            {
                code = annotations.get(0).getPropertyAssignment().getPropertyType().getCode();
            }
        }

        PropertyTypePermId propertyTypePermId = new PropertyTypePermId(code);

        PropertyTypeUpdate update = new PropertyTypeUpdate();
        update.setTypeId(propertyTypePermId);
        if (propertyLabel != null)
        {
            if (propertyLabel.equals("--DELETE--") || propertyLabel.equals("__DELETE__"))
            {
                update.setLabel("");
            } else if (!propertyLabel.isEmpty())
            {
                update.setLabel(propertyLabel);
            }
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

        if(delayedExecutor.isSystem() && internal != null && !internal.isEmpty()) {
            update.setManagedInternally(ImportUtils.isTrue(internal));
        }

        PropertyType propertyType = delayedExecutor.getPropertyType(propertyTypePermId);
        if (vocabularyCode != null && !vocabularyCode.isEmpty())
        {
            if (propertyType.getVocabulary() != null  && vocabularyCode.equals(propertyType.getVocabulary().getCode()) == false)
            {
                operationLog.warn(
                        "PROPERTY TYPE [" + code + "] : Vocabulary types can't be updated. Ignoring the update.");
                operationLog.warn(
                        "PROPERTY TYPE [" + code + "] : Current: [" + propertyType.getVocabulary().getCode() + "] New: [" + vocabularyCode + "]");
                throw new UserFailureException("Vocabulary types can't be updated.");
            } else if (propertyType.getVocabulary() == null) {
                operationLog.warn(
                        "PROPERTY TYPE [" + code + "] : Types that are not vocabulary cannot become one. Ignoring the update.");
                throw new UserFailureException("Types that are not of type Vocabulary can't be updated to Vocabulary.");
            }
        }
        if (dataType != null && !dataType.isEmpty())
        {
            String currentDataType = propertyType.getDataType().name();
            if (propertyType.getDataType() == DataType.SAMPLE && propertyType.getSampleType() != null)
            {
                currentDataType += ":" + propertyType.getSampleType().getCode();
            }
            if (dataType.equals(currentDataType) == false)
            {
                operationLog.warn(
                        "PROPERTY TYPE [" + code + "] : Data Types can't be converted with Master Data XLS. Ignoring the update.");
                // update.convertToDataType(DataType.valueOf(dataType));
            }
        }
        if (metadata != null && !metadata.isEmpty())
        {
            update.getMetaData().add(JSONHandler.parseMetaData(metadata));
        }

        delayedExecutor.updatePropertyType(update, page, line);
    }

    @Override
    protected void validateHeader(Map<String, Integer> headers)
    {
        attributeValidator.validateHeaders(Attribute.values(), headers);
    }
}
