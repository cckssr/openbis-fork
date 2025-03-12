package ch.ethz.sis.openbis.generic.excel.v3.to.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.stream.Collectors;

public class PropertyTypeHelper
{
    private enum Attribute {// implements IAttribute {
        Code("Code", true),
        Mandatory("Mandatory", true),
        //DefaultValue("Default Value", false),  // Ignored, only used by PropertyAssignmentImportHelper
        ShowInEditViews("Show in edit views", true),
        Section("Section", true),
        PropertyLabel("Property label", true),
        DataType("Data type", true),
        VocabularyCode("Vocabulary code", true),
        Description("Description", true),
        Metadata("Metadata", false),
        DynamicScript("Dynamic script", false),
        OntologyId("Ontology Id", false),
        OntologyVersion("Ontology Version", false),
        OntologyAnnotationId("Ontology Annotation Id", false),
        MultiValued("Multivalued", false),
        Unique("Unique", false),
        Pattern("Pattern", false),
        PatternType("Pattern Type", false);

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

    private int addDefaultNameRow(Sheet sheet, int rowNum){
        Row resRow = sheet.createRow(rowNum++);
        resRow.createCell(0).setCellValue("NAME");  // Code("Code", true),
        resRow.createCell(1).setCellValue(0);  // Mandatory("Mandatory", true),
        resRow.createCell(2).setCellValue(1);  // ShowInEditViews("Show in edit views", true),
        //resRow.createCell(3).setCellValue("No");  // Section("Section", true),
        resRow.createCell(4).setCellValue("Name");  // PropertyLabel("Property label", true),
        resRow.createCell(5).setCellValue("VARCHAR");  // DataType("Data type", true),
        //resRow.createCell(6).setCellValue("No");  // VocabularyCode("Vocabulary code", true),
        resRow.createCell(7).setCellValue("Name");  // Description("Description", true),
        resRow.createCell(13).setCellValue(false);  // MultiValued("Multivalued", false),
        return rowNum;
    }

    public int addObjectProperties(Sheet sheet, int rowNum, CellStyle headerStyle,
            SampleType sampleType)
    {
        Row propTypeRowHeaders = sheet.createRow(rowNum++);

        // Populate header row with enum values
        Attribute[] fields = Attribute.values();
        for (int i = 0; i < fields.length; i++) {
            Cell cell = propTypeRowHeaders.createCell(i);
            cell.setCellValue(fields[i].getHeaderName());
            cell.setCellStyle(headerStyle);
        }

        for (var assingment : sampleType.getPropertyAssignments())
            createRow(sheet, rowNum++, assingment);

        rowNum = addDefaultNameRow(sheet, rowNum);

        // add empty row
        sheet.createRow(rowNum++);

        return rowNum;
    }

    // Method to create a row in the sheet
    private void createRow(Sheet sheet, int rowNum, PropertyAssignment propertyAssignment)
    {
        PropertyType propertyType = propertyAssignment.getPropertyType();
        int mandatoryVal = propertyAssignment.isMandatory() ? 1 : 0;

        Row resRow = sheet.createRow(rowNum);
        resRow.createCell(0).setCellValue(
                propertyAssignment.getPropertyType().getCode());  // Code("Code", true),
        resRow.createCell(1)
                .setCellValue(mandatoryVal);  // Mandatory("Mandatory", true),
        resRow.createCell(2).setCellValue(1);  // ShowInEditViews("Show in edit views", true),
        //resRow.createCell(3).setCellValue("No");  // Section("Section", true),
        resRow.createCell(4)
                .setCellValue(propertyType.getLabel());  // PropertyLabel("Property label", true),
        resRow.createCell(5).setCellValue(propertyAssignment.getPropertyType().getDataType()
                .toString());  // DataType("Data type", true),
        if (propertyAssignment.getPropertyType().getVocabulary() != null)
        {
            resRow.createCell(6).setCellValue(propertyAssignment.getPropertyType().getVocabulary()
                    .getCode()); // VocabularyCode("Vocabulary code", true),
        }
        resRow.createCell(7).setCellValue(
                propertyType.getLabel() + (propertyType.getDescription() != null ?
                        ": " + propertyType.getDescription() :
                        ""));  // Description("Description", true),
        //resRow.createCell(8).setCellValue(StringUtils.join(propertyType.metadata));  // Metadata("Metadata", false),
        //resRow.createCell(9).setCellValue("No");  // DynamicScript("Dynamic script", false),
        resRow.createCell(10).setCellValue(propertyAssignment.getSemanticAnnotations().stream().map(
                SemanticAnnotation::getPredicateOntologyId).collect(
                Collectors.joining("\n")));  // //OntologyId("Ontology Id", false),
        resRow.createCell(11).setCellValue(propertyAssignment.getSemanticAnnotations().stream().map(
                SemanticAnnotation::getPredicateOntologyId).collect(
                Collectors.joining("\n")));  // //OntologyVersion("Ontology Version", false),
        resRow.createCell(12).setCellValue(propertyAssignment.getSemanticAnnotations().stream().map(
                SemanticAnnotation::getPredicateOntologyId).collect(
                Collectors.joining(
                        "\n")));  // //OntologyAnnotationId("Ontology Annotation Id", false),
        resRow.createCell(13).setCellValue(propertyAssignment.getPropertyType()
                .isMultiValue());  // MultiValued("Multivalued", false),
        //resRow.createCell(14).setCellValue("No");  // Unique("Unique", false),
        //resRow.createCell(15).setCellValue("No");  // Pattern("Pattern", false),
        //resRow.createCell(16).setCellValue("No");  // PatternType("Pattern Type", false);
    }
}
