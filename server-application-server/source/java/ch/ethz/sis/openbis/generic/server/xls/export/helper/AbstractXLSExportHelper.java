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
package ch.ethz.sis.openbis.generic.server.xls.export.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.server.xls.export.Attribute;
import ch.ethz.sis.openbis.generic.server.xls.export.ExportableKind;
import ch.ethz.sis.openbis.generic.server.xls.export.FieldType;
import ch.ethz.sis.openbis.generic.server.xls.export.XLSExport;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.FileServerUtils;
import ch.systemsx.cisd.openbis.generic.shared.basic.BasicConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractXLSExportHelper<ENTITY_TYPE extends IEntityType> implements IXLSExportHelper<ENTITY_TYPE>
{

    protected static final String[] ENTITY_ASSIGNMENT_COLUMNS = new String[] { "Code", "Internal", "Mandatory",
            "Show in edit views", "Section", "Property label", "Data type", "Vocabulary code", "Description",
            "Metadata", "Dynamic script", "Multivalued", "Unique", "Pattern", "Pattern Type",
            "Internal Assignment", "Ontology Id", "Ontology Annotation Id", "Ontology Version" };

    protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(BasicConstant.DATE_HOURS_MINUTES_SECONDS_PATTERN);

    public static final String FIELD_TYPE_KEY = "type";

    public static final String FIELD_ID_KEY = "id";

    private final Workbook wb;
    
    private final CellStyle normalCellStyle;
    
    private final CellStyle boldCellStyle;

    private final CellStyle errorCellStyle;

    public AbstractXLSExportHelper(final Workbook wb)
    {
        this.wb = wb;
        
        normalCellStyle = wb.createCellStyle();
        boldCellStyle = wb.createCellStyle();
        errorCellStyle = wb.createCellStyle();
        
        final Font boldFont = wb.createFont();
        boldFont.setBold(true);
        boldCellStyle.setFont(boldFont);
        
        final Font normalFont = wb.createFont();
        normalFont.setBold(false);
        normalCellStyle.setFont(normalFont);
        
        errorCellStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.RED.getIndex());
        errorCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    protected static boolean isFieldAcceptable(final Set<Attribute> attributeSet, final Map<String, String> field)
    {
        return FieldType.valueOf(field.get(FIELD_TYPE_KEY)) != FieldType.ATTRIBUTE ||
                attributeSet.contains(Attribute.valueOf(field.get(FIELD_ID_KEY)));
    }

    protected String mapToJSON(final Map<?, ?> map)
    {
        if (map == null || map.isEmpty())
        {
            return "";
        } else
        {
            try
            {
                return new ObjectMapper().writeValueAsString(map);
            } catch (final JsonProcessingException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    protected AddRowResult addRow(final int rowNumber, final boolean bold,
            final ExportableKind exportableKind, final String idForWarningsOrErrors, final String... values)
    {
        final Collection<String> warnings = new ArrayList<>();
        final Map<String, String> valueFiles = new HashMap<>();

        final Row row = wb.getSheetAt(0).createRow(rowNumber);
        for (int j = 0; j < values.length; j++)
        {
            final Cell cell = row.createCell(j);
            final String value = values[j] != null ? values[j] : "";

            if (value.length() <= Short.MAX_VALUE)
            {
                cell.setCellStyle(bold ? boldCellStyle : normalCellStyle);
                cell.setCellValue(value);
            } else
            {
                final String fileName = String.format("value-%s.txt", convertNumericToAlphanumeric(rowNumber, j));
                cell.setCellValue(String.format("__%s__", fileName));
                valueFiles.put(fileName, value);
            }
        }

        return new AddRowResult(warnings, valueFiles);
    }

    public static String convertNumericToAlphanumeric(final int row, final int col)
    {
        final int aCharCode = 'A';
        final int ord0 = col % 26;
        final int ord1 = col / 26;
        final char char0 = (char) (aCharCode + ord0);
        final char char1 = (char) (aCharCode + ord1 - 1);
        return String.valueOf(ord1 > 0 ? char1 : "") + char0 + (row + 1);
    }

    protected void addRow(int rowNumber, boolean bold, final ExportableKind exportableKind, final String idForWarningsOrErrors,
            final Collection<String> warnings, final Map<String, String> valueFiles, final String... values)
    {
        final AddRowResult addRowResult = addRow(rowNumber, bold, exportableKind, idForWarningsOrErrors, values);
        warnings.addAll(addRowResult.getWarnings());
        valueFiles.putAll(addRowResult.getValueFiles());
    }

    @Override
    public ENTITY_TYPE getEntityType(final IApplicationServerApi api, final String sessionToken, final String permId)
    {
        return null;
    }

    protected static Function<PropertyType, PropertyValue> getPropertiesMappingFunction(
            final XLSExport.TextFormatting textFormatting, final Map<String, Serializable> properties, final Collection<String> warnings)
    {
        return textFormatting == XLSExport.TextFormatting.PLAIN
                ? propertyType ->
                        propertyType.getDataType() == DataType.MULTILINE_VARCHAR
                                ? getPlainMultilineVarcharProperty(properties, propertyType)
                                : getProperty(properties, propertyType)
                : propertyType ->
                        propertyType.getDataType() == DataType.MULTILINE_VARCHAR
                                ? getRichMultilineVarcharProperty(properties, propertyType, warnings)
                                : getProperty(properties, propertyType);
    }

    private static PropertyValue getPlainMultilineVarcharProperty(final Map<String, Serializable> properties, final PropertyType propertyType)
    {
        return getProperty(properties, propertyType) != null
                ? new PropertyValue(((String) properties.get(propertyType.getCode())).replaceAll("<[^>]+>", ""), Map.of())
                : null;
    }

    private static PropertyValue getRichMultilineVarcharProperty(final Map<String, Serializable> properties, final PropertyType propertyType,
            final Collection<String> warnings)
    {
        if (propertyType.getDataType() == DataType.MULTILINE_VARCHAR && propertyType.getMetaData() != null &&
                Objects.equals(propertyType.getMetaData().get("custom_widget"), "Word Processor"))
        {
            final String value = (String) properties.get(propertyType.getCode());
            final Map<String, byte[]> imageFiles = findImageFiles(value, warnings);
            return new PropertyValue(value, imageFiles);
        } else
        {
            return getProperty(properties, propertyType);
        }
    }

    public static Map<String, byte[]> findImageFiles(final String input, final Collection<String> warnings)
    {
        if (input == null)
        {
            return null;
        }

        // Regular expression to match <img src='/openbis/openbis/file-service/...' or <img src="/openbis/openbis/file-service/..."
        final String regex = "<img\\s+src=[\"'](http)?.*?(/openbis/openbis/file-service)(/[^\"']*?)[\"']";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(input);
        final Map<String, byte[]> imageFiles = new HashMap<>();

        while (matcher.find())
        {
            final String filePath = matcher.group(3);
            try
            {
                imageFiles.put(filePath, FileServerUtils.readAllBytes(filePath));
            } catch (final IOException e)
            {
                warnings.add(String.format("Could not read the file at path '%s'.", filePath));
            }
        }

        return imageFiles;
    }

    private static PropertyValue getProperty(final Map<String, Serializable> properties, final PropertyType propertyType)
    {
        Serializable propertyValue = properties.get(propertyType.getCode());
        if (propertyValue == null)
        {
            return null;
        }
        if (propertyValue.getClass().isArray())
        {
            StringBuilder sb = new StringBuilder();
            Serializable[] values = (Serializable[]) propertyValue;
            for (Serializable value : values)
            {
                if (sb.length() > 0)
                {
                    sb.append(", ");
                }
                sb.append(getPropertyValueAsString(propertyType, value));
            }
            return new PropertyValue(sb.toString(), Map.of());
        } else
        {
            return new PropertyValue(getPropertyValueAsString(propertyType, propertyValue), Map.of());
        }
    }

    private static String getPropertyValueAsString(final PropertyType propertyType, Serializable value)
    {
        String writableValue = null;
        if (value instanceof Sample) {
            writableValue = ((Sample) value).getIdentifier().getIdentifier();
        } else if (propertyType.getDataType() == DataType.CONTROLLEDVOCABULARY){
            for (VocabularyTerm term: propertyType.getVocabulary().getTerms()) {
                if (term.getCode().equals(value)) {
                    if (term.getLabel() != null && !term.getLabel().isBlank()) {
                        writableValue = term.getLabel();
                    } else {
                        writableValue = term.getCode();
                    }
                    break;
                }
            }
        } else {
            writableValue = value.toString();
        }
        return writableValue;
    }


    protected static class AddRowResult
    {

        private final Collection<String> warnings;

        private final Map<String, String> valueFiles;

        protected AddRowResult(final Collection<String> warnings, final Map<String, String> valueFiles)
        {
            this.warnings = warnings;
            this.valueFiles = valueFiles;
        }

        public Collection<String> getWarnings()
        {
            return warnings;
        }

        public Map<String, String> getValueFiles()
        {
            return valueFiles;
        }

    }

    /**
     * Property value, which may contain not only String but also files for MULTILINE_VARCHAR properties with HTML image references in them.
     */
    public static class PropertyValue
    {
        private final String value;

        /** File name to content map. */
        private final Map<String, byte[]> miscellaneousFiles;

        protected PropertyValue(final String value, final Map<String, byte[]> miscellaneousFiles)
        {
            this.value = value;
            this.miscellaneousFiles = miscellaneousFiles;
        }

        public String getValue()
        {
            return value;
        }

        public Map<String, byte[]> getMiscellaneousFiles()
        {
            return miscellaneousFiles;
        }
    }

}
