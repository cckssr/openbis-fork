package ch.ethz.sis.openbis.generic.excel.v3.to.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.Optional;
import java.util.stream.Collectors;

public class SampleTypeHelper
{

    private enum Attribute {// implements IAttribute {
        Code("Code", true),
        Description("Description", true),
        AutoGenerateCodes("Auto generate codes", true),
        ValidationScript("Validation script", true),
        GeneratedCodePrefix("Generated code prefix", true),
        OntologyId("Ontology Id", false),
        OntologyVersion("Ontology Version", false),
        OntologyAnnotationId("Ontology Annotation Id", false);

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

    public int addSampleTypeSection(Sheet sheet, int rowNum, CellStyle headerStyle,
            SampleType sampleType)
    {
        Row sampleTypeRow = sheet.createRow(rowNum++);
        sampleTypeRow.createCell(0).setCellValue("SAMPLE_TYPE");
        sampleTypeRow.getCell(0).setCellStyle(headerStyle);

        Row sampleTypeRowHeaders = sheet.createRow(rowNum++);

        // Populate header row with enum values
        Attribute[] fields = Attribute.values();
        for (int i = 0; i < fields.length; i++) {
            Cell cell = sampleTypeRowHeaders.createCell(i);
            cell.setCellValue(fields[i].getHeaderName());
            cell.setCellStyle(headerStyle);
        }

        String
                accessionId = Optional.ofNullable(sampleType.getSemanticAnnotations())
                .map(x -> x.stream().map(y -> y.getDescriptorAccessionId()).collect(
                        Collectors.joining("\n"))).orElse(null);
        String
                ontologyVersions = Optional.ofNullable(sampleType.getSemanticAnnotations())
                .map(x -> x.stream().map(y -> y.getDescriptorOntologyVersion()).collect(
                        Collectors.joining("\n"))).orElse(null);
        String
                ontologies = Optional.ofNullable(sampleType.getSemanticAnnotations())
                .map(x -> x.stream().map(y -> y.getDescriptorOntologyId()).collect(
                        Collectors.joining("\n"))).orElse(null);



        Row sampleTypeRowValues = sheet.createRow(rowNum++);

        sampleTypeRowValues.createCell(0)
                .setCellValue(sampleType.getCode());                   //Code("Code", true),
        sampleTypeRowValues.createCell(1).setCellValue(
                sampleType.getDescription());            //Description("Description", true),
        sampleTypeRowValues.createCell(2).setCellValue(1);                      //AutoGenerateCodes("Auto generate codes", true),
        //sampleTypeRowValues.createCell(3).setCellValue("");                       //ValidationScript("Validation script", true),
        sampleTypeRowValues.createCell(4).setCellValue(false);                  //GeneratedCodePrefix("Generated code prefix", true);
        sampleTypeRowValues.createCell(5)
                .setCellValue(ontologies);           //OntologyId("Ontology Id", false),
        sampleTypeRowValues.createCell(6).setCellValue(
                ontologyVersions);             //OntologyVersion("Ontology Version", false),
        sampleTypeRowValues.createCell(7).setCellValue(
                accessionId);            //OntologyAnnotationId("Ontology Annotation Id", false),

        return rowNum;
    }
}
