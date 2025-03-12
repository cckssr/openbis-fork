package ch.ethz.sis.openbis.generic.excel.v3.to.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import static ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind.EXPERIMENT_TYPE;
import static ch.ethz.sis.openbis.generic.excel.v3.to.Constants.COLLECTION_TYPE;

public class ExperimentTypeHelper
{
    private enum Attribute { //implements IAttribute {
        Code("Code", true),
        Description("Description", true),
        ValidationScript("Validation script", true);

        private final String headerName;

        private final boolean mandatory;

        Attribute(String headerName, boolean mandatory) {
            this.headerName = headerName;
            this.mandatory = mandatory;
        }

        public String getHeaderName() {
            return headerName;
        }
        public boolean isMandatory() {
            return mandatory;
        }
    }

    private enum AttributeHeader {// implements IAttribute {
        Code("Code", true),
        Mandatory("Mandatory", true),
        ShowInEditViews("Show in edit views", true),
        Section("Section", true),
        PropertyLabel("Property label", true),
        DataType("Data type", true),
        VocabularyCode("Vocabulary code", true),
        Description("Description", true),
        Metadata("Metadata", false),
        DynamicScript("Dynamic script", false);

        private final String headerName;

        private final boolean mandatory;

        AttributeHeader(String headerName, boolean mandatory) {
            this.headerName = headerName;
            this.mandatory = mandatory;
        }

        public String getHeaderName() {
            return headerName;
        }
        public boolean isMandatory() {
            return mandatory;
        }
    }

    public int addExperimentTypeSection(Sheet sheet, int rowNum, CellStyle headerStyle)
    {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(EXPERIMENT_TYPE.name());
        row.getCell(0).setCellStyle(headerStyle);

        Row rowHeaders = sheet.createRow(rowNum++);

        // Populate header row with enum values
        Attribute[] fields = Attribute.values();
        for (int i = 0; i < fields.length; i++) {
            Cell cell = rowHeaders.createCell(i);
            cell.setCellValue(fields[i].getHeaderName());
            cell.setCellStyle(headerStyle);
        }

        Row rowValues = sheet.createRow(rowNum++);

        rowValues.createCell(0).setCellValue(COLLECTION_TYPE);     //Code("Code", true),
        rowValues.createCell(1).setCellValue("");               //Description("Description", true),
        rowValues.createCell(2).setCellValue("");               //ValidationScript("Validation script", true)

        return rowNum;
    }

    private int addNameRow(Sheet sheet, int rowNum)
    {
        Row resRow = sheet.createRow(rowNum++);
        resRow.createCell(0).setCellValue("NAME");  // Code("Code", true),
        resRow.createCell(1).setCellValue(0);  // Mandatory("Mandatory", true),
        resRow.createCell(2).setCellValue(1);  // ShowInEditViews("Show in edit views", true),
        resRow.createCell(3).setCellValue("General info");  // Section("Section", true),
        resRow.createCell(4).setCellValue("Name");  // PropertyLabel("Property label", true),
        resRow.createCell(5).setCellValue(DataType.VARCHAR.name());  // DataType("Data type", true),
        //resRow.createCell(6).setCellValue("");          // VocabularyCode("Vocabulary code", true),
        resRow.createCell(7).setCellValue("Name");      // Description("Description", true),
        //resRow.createCell(1).setCellValue("");               // Metadata("Metadata", false),
        //resRow.createCell(1).setCellValue("");               // DynamicScript("Dynamic script", false);
        return rowNum;
    }

    private int addObjctTypeRow(Sheet sheet, int rowNum)
    {
        Row resRow = sheet.createRow(rowNum++);
        resRow.createCell(0).setCellValue("DEFAULT_OBJECT_TYPE");  // Code("Code", true),
        resRow.createCell(1).setCellValue(0);  // Mandatory("Mandatory", true),
        resRow.createCell(2).setCellValue(1);  // ShowInEditViews("Show in edit views", true),
        resRow.createCell(3).setCellValue("General info");  // Section("Section", true),
        resRow.createCell(4).setCellValue("Default object type");  // PropertyLabel("Property label", true),
        resRow.createCell(5).setCellValue(DataType.VARCHAR.name());  // DataType("Data type", true),
        //resRow.createCell(6).setCellValue("");          // VocabularyCode("Vocabulary code", true),
        resRow.createCell(7).setCellValue("Enter the code of the object type for which the collection is used");      // Description("Description", true),
        //resRow.createCell(1).setCellValue("");               // Metadata("Metadata", false),
        //resRow.createCell(1).setCellValue("");               // DynamicScript("Dynamic script", false);
        return rowNum;
    }

    public int addExperimentSection(Sheet sheet, int rowNum, CellStyle headerStyle)
    {
        Row rowHeaders = sheet.createRow(rowNum++);

        // Populate header row with enum values
        AttributeHeader[] fields = AttributeHeader.values();
        for (int i = 0; i < fields.length; i++) {
            Cell cell = rowHeaders.createCell(i);
            cell.setCellValue(fields[i].getHeaderName());
            cell.setCellStyle(headerStyle);
        }

        rowNum = addNameRow(sheet, rowNum);
        rowNum = addObjctTypeRow(sheet, rowNum);

        return rowNum;
    }
}
