/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.server.xls.export.Attribute;
import ch.ethz.sis.openbis.generic.server.xls.export.ExportableKind;
import ch.ethz.sis.openbis.generic.server.xls.export.FieldType;
import ch.ethz.sis.openbis.generic.server.xls.export.XLSExport;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XLSTypeGroupExportHelper extends AbstractXLSExportHelper<IEntityType>
{
    public XLSTypeGroupExportHelper(final Workbook wb)
    {
        super(wb);
    }

    @Override
    public AdditionResult add(IApplicationServerApi api, String sessionToken,
            Workbook wb, List<String> permIds, int rowNumber,
            Map<String, List<Map<String, String>>> entityTypeExportFieldsMap,
            XLSExport.TextFormatting textFormatting, boolean compatibleWithImport)
    {
        final Collection<TypeGroup> typeGroups = getTypeGroups(api, sessionToken, permIds);
        final Collection<String> warnings = new ArrayList<>();
        final Map<String, String> valueFiles = new HashMap<>();

        addRow(rowNumber++, true, ExportableKind.TYPE_GROUP, null, warnings, valueFiles, ExportableKind.TYPE_GROUP.name());

        final Attribute[] possibleAttributes = getAttributes();
        if (entityTypeExportFieldsMap == null || entityTypeExportFieldsMap.isEmpty() ||
                !entityTypeExportFieldsMap.containsKey(ExportableKind.TYPE_GROUP.toString()) ||
                entityTypeExportFieldsMap.get(ExportableKind.TYPE_GROUP.toString()).isEmpty())
        {
            // Export all attributes in any order
            // Headers
            final Attribute[] importableAttributes = Arrays.stream(possibleAttributes).filter(Attribute::isImportable)
                    .toArray(Attribute[]::new);
            final Attribute[] defaultPossibleAttributes = Arrays.stream(possibleAttributes).filter(Attribute::isIncludeInDefaultList)
                    .toArray(Attribute[]::new);
            final Attribute[] attributes = compatibleWithImport ? importableAttributes : defaultPossibleAttributes;
            final String[] attributeHeaders = Arrays.stream(attributes).map(Attribute::getName).toArray(String[]::new);

            addRow(rowNumber++, true, ExportableKind.TYPE_GROUP, null, warnings, valueFiles, attributeHeaders);

            // Values
            for (final TypeGroup typeGroup : typeGroups)
            {
                final String[] values = Arrays.stream(attributes).map(attribute -> getAttributeValue(typeGroup, attribute)).toArray(String[]::new);
                addRow(rowNumber++, false, ExportableKind.TYPE_GROUP, null, warnings, valueFiles, values);
            }
        } else
        {
            // Export selected attributes in predefined order
            // Headers
            final Set<Attribute> possibleAttributeNameSet = Stream.of(possibleAttributes)
                    .filter(attribute -> !compatibleWithImport || attribute.isImportable())
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Attribute.class)));
            final List<Map<String, String>> selectedExportAttributes = entityTypeExportFieldsMap.get(ExportableKind.TYPE_GROUP.toString());

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

            addRow(rowNumber++, true, ExportableKind.TYPE_GROUP, null, warnings, valueFiles, allAttributeNames);

            // Values
            final Set<Map<String, String>> selectedExportFieldSet = new HashSet<>(selectedExportAttributes);
            final List<Map<String, String>> extraExportFields = compatibleWithImport
                    ? Arrays.stream(requiredForImportAttributes)
                    .map(attribute -> Map.of(FIELD_TYPE_KEY, FieldType.ATTRIBUTE.toString(), FIELD_ID_KEY, attribute.toString()))
                    .filter(map -> !selectedExportFieldSet.contains(map))
                    .collect(Collectors.toList())
                    : List.of();
            for (final TypeGroup typeGroup : typeGroups)
            {
                final String[] entityValues = Stream.concat(selectedExportAttributes.stream(), extraExportFields.stream())
                        .filter(field -> isFieldAcceptable(possibleAttributeNameSet, field))
                        .map(field ->
                        {
                            if (FieldType.valueOf(field.get(FIELD_TYPE_KEY)) == FieldType.ATTRIBUTE)
                            {
                                return getAttributeValue(typeGroup, Attribute.valueOf(field.get(FIELD_ID_KEY)));
                            } else
                            {
                                throw new IllegalArgumentException();
                            }
                        }).toArray(String[]::new);

                addRow(rowNumber++, false, ExportableKind.TYPE_GROUP, null, warnings, valueFiles, entityValues);
            }
        }

        return new AdditionResult(rowNumber + 1, warnings, valueFiles, Map.of());
    }


    protected Attribute[] getAttributes()
    {
        return new Attribute[] { Attribute.CODE, Attribute.INTERNAL,
                Attribute.REGISTRATOR, Attribute.REGISTRATION_DATE,
                Attribute.MODIFIER, Attribute.MODIFICATION_DATE };
    }

    protected String getAttributeValue(final TypeGroup typeGroup, final Attribute attribute)
    {
        switch (attribute)
        {
            case CODE:
            {
                return typeGroup.getCode();
            }
            case INTERNAL:
            {
                return typeGroup.isManagedInternally().toString().toUpperCase();
            }
            case REGISTRATOR:
            {
                final Person registrator = typeGroup.getRegistrator();
                return registrator != null ? registrator.getUserId() : null;
            }
            case REGISTRATION_DATE:
            {
                final Date registrationDate = typeGroup.getRegistrationDate();
                return registrationDate != null ? DATE_FORMAT.format(registrationDate) : null;
            }
            case MODIFIER:
            {
                final Person modifier = typeGroup.getModifier();
                return modifier != null ? modifier.getUserId() : null;
            }
            case MODIFICATION_DATE:
            {
                final Date modificationDate = typeGroup.getModificationDate();
                return modificationDate != null ? DATE_FORMAT.format(modificationDate) : null;
            }
            default:
            {
                return null;
            }
        }
    }

    private Collection<TypeGroup> getTypeGroups(final IApplicationServerApi api, final String sessionToken,
            final Collection<String> permIds)
    {
        final List<TypeGroupId> typeGroupPermIds = permIds.stream().map(TypeGroupId::new)
                .collect(Collectors.toList());
        final TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        fetchOptions.withRegistrator();
        fetchOptions.withModifier();
        return api.getTypeGroups(sessionToken, typeGroupPermIds, fetchOptions).values();
    }
}
