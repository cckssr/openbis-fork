/*
 *  Copyright ETH 2023 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.openbis.generic.server.xls.export.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.search.SemanticAnnotationSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.server.xls.export.Attribute;
import ch.ethz.sis.openbis.generic.server.xls.export.ExportableKind;
import ch.ethz.sis.openbis.generic.server.xls.export.FieldType;
import ch.ethz.sis.openbis.generic.server.xls.export.XLSExport;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractXLSEntityTypeExportHelper<ENTITY_TYPE extends IEntityType> extends AbstractXLSExportHelper<ENTITY_TYPE>
{

    public AbstractXLSEntityTypeExportHelper(final Workbook wb)
    {
        super(wb);
    }

    @Override
    public AdditionResult add(final IApplicationServerApi api, final String sessionToken, final Workbook wb,
            final List<String> permIds, int rowNumber,
            final Map<String, List<Map<String, String>>> entityTypeExportFieldsMap,
            final XLSExport.TextFormatting textFormatting, final boolean compatibleWithImport)
    {
        if (permIds.size() != 1)
        {
            throw new IllegalArgumentException("For entity type export number of permIds should be equal to 1.");
        }
        final ENTITY_TYPE entityType = getEntityType(api, sessionToken, permIds.get(0));
        final Collection<String> warnings = new ArrayList<>();
        final Map<String, String> valueFiles = new HashMap<>();

        if (entityType != null)
        {
            final String permId = entityType.getPermId().toString();
            final ExportableKind exportableKind = getExportableKind();
            addRow(rowNumber++, true, exportableKind, permId, warnings, valueFiles, exportableKind.name());

            final Attribute[] possibleAttributes = getAttributes(entityType);
            if (entityTypeExportFieldsMap == null || entityTypeExportFieldsMap.isEmpty() ||
                    !entityTypeExportFieldsMap.containsKey(exportableKind.toString()) ||
                    entityTypeExportFieldsMap.get(exportableKind.toString()).isEmpty())
            {
                // Export all attributes in any order
                // Headers
                final Attribute[] importableAttributes = Arrays.stream(possibleAttributes).filter(Attribute::isImportable)
                        .toArray(Attribute[]::new);
                final Attribute[] defaultPossibleAttributes = Arrays.stream(possibleAttributes).filter(Attribute::isIncludeInDefaultList)
                        .toArray(Attribute[]::new);
                final Attribute[] attributes = compatibleWithImport ? importableAttributes : defaultPossibleAttributes;
                final String[] attributeHeaders = Arrays.stream(attributes).map(Attribute::getName).toArray(String[]::new);

                addRow(rowNumber++, true, exportableKind, permId, warnings, valueFiles, attributeHeaders);

                // Values
                // clean ontology cache
                final String[] values = Arrays.stream(attributes)
                        .map(attribute -> getAttributeValue(api, sessionToken,
                                entityType, attribute)).toArray(String[]::new);
                addRow(rowNumber++, false, exportableKind, permId, warnings, valueFiles, values);
            } else
            {
                // Export selected attributes in predefined order
                // Headers
                final Set<Attribute> possibleAttributeNameSet = Stream.of(possibleAttributes)
                        .filter(attribute -> !compatibleWithImport || attribute.isImportable())
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(Attribute.class)));
                final List<Map<String, String>> selectedExportAttributes = entityTypeExportFieldsMap.get(exportableKind.toString());

                final String[] selectedAttributeHeaders = selectedExportAttributes.stream()
                        .filter(attribute -> AbstractXLSExportHelper.isFieldAcceptable(possibleAttributeNameSet, attribute))
                        .map(attribute ->
                        {
                            if (FieldType.valueOf(attribute.get(FIELD_TYPE_KEY)) == FieldType.ATTRIBUTE)
                            {
                                return Attribute.valueOf(attribute.get(FIELD_ID_KEY)).getName();
                            } else
                            {
                                throw new IllegalArgumentException();
                            }
                        }).toArray(String[]::new);
                final Attribute[] requiredForImportAttributes = Arrays.stream(possibleAttributes)
                        .filter(Attribute::isRequiredForImport)
                        .toArray(Attribute[]::new);
                final Set<Attribute> selectedAttributes = selectedExportAttributes.stream()
                        .filter(map -> map.get(FIELD_TYPE_KEY).equals(FieldType.ATTRIBUTE.toString()))
                        .map(map -> Attribute.valueOf(map.get(FIELD_ID_KEY)))
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(Attribute.class)));
                final Stream<String> requiredForImportAttributeNameStream = compatibleWithImport
                        ? Arrays.stream(requiredForImportAttributes)
                        .filter(attribute -> !selectedAttributes.contains(attribute))
                        .map(Attribute::getName)
                        : Stream.empty();
                final String[] allAttributeNames = Stream.concat(Arrays.stream(selectedAttributeHeaders), requiredForImportAttributeNameStream)
                        .toArray(String[]::new);

                addRow(rowNumber++, true, exportableKind, permId, warnings, valueFiles, allAttributeNames);

                // Values
                final Set<Map<String, String>> selectedExportFieldSet = new HashSet<>(selectedExportAttributes);
                final List<Map<String, String>> extraExportFields = compatibleWithImport
                        ? Arrays.stream(requiredForImportAttributes)
                        .map(attribute -> Map.of(FIELD_TYPE_KEY, FieldType.ATTRIBUTE.toString(), FIELD_ID_KEY, attribute.toString()))
                        .filter(map -> !selectedExportFieldSet.contains(map))
                        .collect(Collectors.toList())
                        : List.of();

                final String[] entityValues = Stream.concat(selectedExportAttributes.stream(), extraExportFields.stream())
                        .filter(field -> isFieldAcceptable(possibleAttributeNameSet, field))
                        .map(field ->
                        {
                            if (FieldType.valueOf(field.get(FIELD_TYPE_KEY)) == FieldType.ATTRIBUTE)
                            {
                                return getAttributeValue(api, sessionToken, entityType,
                                        Attribute.valueOf(field.get(FIELD_ID_KEY)));
                            } else
                            {
                                throw new IllegalArgumentException();
                            }
                        }).toArray(String[]::new);

                addRow(rowNumber++, false, exportableKind, permId, warnings, valueFiles, entityValues);
            }

            final AdditionResult additionResult =
                    addEntityTypePropertyAssignments(api, sessionToken,
                            rowNumber, entityType.getPropertyAssignments(),
                            exportableKind, entityType.getCode(), permId, compatibleWithImport);
            warnings.addAll(additionResult.getWarnings());
            rowNumber = additionResult.getRowNumber();

            return new AdditionResult(rowNumber + 1, warnings, valueFiles, Map.of());
        } else
        {
            return new AdditionResult(rowNumber, warnings, valueFiles, Map.of());
        }
    }

    protected AdditionResult addEntityTypePropertyAssignments(IApplicationServerApi api,
            String sessionToken, int rowNumber,
            final Collection<PropertyAssignment> propertyAssignments,
            final ExportableKind exportableKind, String code,
            final String permId, final boolean compatibleWithImport)
    {
        final Collection<String> warnings = new ArrayList<>();
        final Map<String, String> valueFiles = new HashMap<>();
        addRow(rowNumber++, true, exportableKind, permId, warnings, valueFiles, ENTITY_ASSIGNMENT_COLUMNS);
        for (final PropertyAssignment propertyAssignment : propertyAssignments)
        {
            final PropertyType propertyType = propertyAssignment.getPropertyType();
            final Plugin plugin = propertyAssignment.getPlugin();
            final Vocabulary vocabulary = propertyType.getVocabulary();
            List<SemanticAnnotation>
                    semanticAnnotations =
                    getSemanticAnnotationSearchResult(api, sessionToken, getEntityKind(), code,
                            propertyType.getCode());

            final String[] values = {
                    propertyType.getCode(),
                    String.valueOf(propertyType.isManagedInternally()).toUpperCase(),
                    String.valueOf(propertyAssignment.isMandatory()).toUpperCase(),
                    String.valueOf(propertyAssignment.isShowInEditView()).toUpperCase(),
                    propertyAssignment.getSection(),
                    propertyType.getLabel(),
                    getFullDataTypeString(propertyType),
                    String.valueOf(vocabulary != null ? vocabulary.getCode() : ""),
                    propertyType.getDescription(),
                    mapToJSON(propertyType.getMetaData()),
                    plugin != null ? (plugin.getName() != null ? plugin.getName() + ".py" : "") : "",
                    String.valueOf(propertyType.isMultiValue() != null && propertyType.isMultiValue()).toUpperCase(),
                    String.valueOf(propertyAssignment.isUnique() != null && propertyAssignment.isUnique()).toUpperCase(),
                    String.valueOf(propertyAssignment.getPattern() != null ? propertyAssignment.getPattern() : ""),
                    String.valueOf(propertyAssignment.getPatternType() != null ? propertyAssignment.getPatternType() : ""),
                    String.valueOf(
                            propertyAssignment.isManagedInternally() != null && propertyAssignment.isManagedInternally()).toUpperCase(),
                    semanticAnnotations.stream()
                            .map(SemanticAnnotation::getPredicateOntologyId).collect(
                            Collectors.joining("\n")),
                    semanticAnnotations.stream()
                            .map(SemanticAnnotation::getPredicateAccessionId).collect(
                            Collectors.joining("\n")),
                    semanticAnnotations.stream().map(
                            SemanticAnnotation::getPredicateOntologyVersion).collect(
                            Collectors.joining("\n")),
            };
            addRow(rowNumber++, false, exportableKind, permId, warnings, valueFiles, values);
        }
        return new AdditionResult(rowNumber, warnings, valueFiles, Map.of());
    }



    private String getFullDataTypeString(final PropertyType propertyType)
    {
        final String dataTypeString = String.valueOf(propertyType.getDataType());
        switch (propertyType.getDataType())
        {
            case SAMPLE:
            {
                return dataTypeString +
                        ((propertyType.getSampleType() != null) ? ':' + propertyType.getSampleType().getCode() : "");
            }
            case MATERIAL:
            {
                return dataTypeString +
                        ((propertyType.getMaterialType() != null)
                                ? ':' + propertyType.getMaterialType().getCode() : "");
            }
            default:
            {
                return dataTypeString;
            }
        }
    }

    protected abstract Attribute[] getAttributes(final ENTITY_TYPE entityType);

    protected abstract String getAttributeValue(IApplicationServerApi api, String sessionToken,
            final ENTITY_TYPE entityType, final Attribute attribute);

    protected abstract ExportableKind getExportableKind();

    protected Map<SemanticAnnotationKey, List<SemanticAnnotation>>
            semanticAnnotationsCache = new HashMap<>();

    private static class SemanticAnnotationKey
    {
        private final String entityTypeCode;

        private final String propertyTypeCode;

        private final EntityKind entityKind;

        public SemanticAnnotationKey(EntityKind entityKind,
                String entityTypeCode, String propertyTypeCode)
        {
            this.entityTypeCode = entityTypeCode;
            this.propertyTypeCode = propertyTypeCode;
            this.entityKind = entityKind;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == null || getClass() != o.getClass())
                return false;
            SemanticAnnotationKey key = (SemanticAnnotationKey) o;
            return Objects.equals(entityTypeCode, key.entityTypeCode) && Objects.equals(
                    propertyTypeCode, key.propertyTypeCode) && entityKind == key.entityKind;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(entityTypeCode, propertyTypeCode, entityKind);
        }

        @Override
        public String toString()
        {
            return "SemanticAnnotationKey{" +
                    "entityTypeCode='" + entityTypeCode + '\'' +
                    ", propertyTypeCode='" + propertyTypeCode + '\'' +
                    ", entityKind=" + entityKind +
                    '}';
        }
    }

    protected List<SemanticAnnotation> getSemanticAnnotationSearchResult(
            IApplicationServerApi api, String sessionToken, EntityKind entityKind,
            String entityTypeCode, @Nullable String propertyTypeCode)
    {
        SemanticAnnotationKey key =
                new SemanticAnnotationKey(entityKind, entityTypeCode, propertyTypeCode);

        if (!semanticAnnotationsCache.containsKey(key))
        {
            SemanticAnnotationSearchCriteria semanticCriteria =
                    new SemanticAnnotationSearchCriteria();


            if (propertyTypeCode != null)
            {
                semanticCriteria.withPropertyAssignment().withPropertyType().withCode()
                        .thatEquals(propertyTypeCode);
                semanticCriteria.withPropertyAssignment().withEntityType().withKind()
                        .thatEquals(entityKind);

            } else
            {
                semanticCriteria.withEntityType().withCode().thatEquals(entityTypeCode);
                semanticCriteria.withEntityType().withKind().thatEquals(entityKind);
            }

            SemanticAnnotationFetchOptions fetchOptions = new SemanticAnnotationFetchOptions();
            fetchOptions.withPropertyType();
            fetchOptions.withEntityType();
            PropertyAssignmentFetchOptions propertyAssignmentFetchOptions =
                    fetchOptions.withPropertyAssignment();
            propertyAssignmentFetchOptions.withEntityType();
            propertyAssignmentFetchOptions.withPropertyType();
            propertyAssignmentFetchOptions.withEntityType();

            SearchResult<SemanticAnnotation> result =
                    api.searchSemanticAnnotations(sessionToken, semanticCriteria, fetchOptions);

            if (propertyTypeCode != null)
            {

                result.getObjects().stream().collect(
                                Collectors.groupingBy(x -> x.getPropertyAssignment().getEntityType()))
                        .forEach((k, v) -> semanticAnnotationsCache.put(
                                new SemanticAnnotationKey(entityKind, k.getCode(),
                                        propertyTypeCode), v));

                if (result.getTotalCount() == 0)
                {
                    semanticAnnotationsCache.put(key, List.of());

                }

            } else
            {

                semanticAnnotationsCache.put(key,
                        result.getObjects());
            }
        }

        return semanticAnnotationsCache.get(key);
    }

    abstract protected EntityKind getEntityKind();


}
