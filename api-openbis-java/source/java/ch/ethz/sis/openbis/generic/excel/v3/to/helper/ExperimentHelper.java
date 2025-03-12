package ch.ethz.sis.openbis.generic.excel.v3.to.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.Locale;

import static ch.ethz.sis.openbis.generic.excel.v3.to.Constants.COLLECTION_TYPE;

public class ExperimentHelper
{
    private static final String EXPERIMENT_TYPE_FIELD = "Experiment type";
    private static final String EXPERIMENT = "EXPERIMENT";

    private enum Attribute { //implements IAttribute {
        Identifier("Identifier", false),
        Code("Code", true),
        Project("Project", true),
        Name("Name", true),
        DefaultObjectType("Default object type", true);

        private final String headerName;

        private final boolean mandatory;

        Attribute(String headerName, boolean mandatory) {
            this.headerName = headerName;
            this.mandatory = mandatory;
        }

        public String getHeaderName() {
            return headerName;
        }

    }

    public int addExperimentSection(Sheet sheet, int rowNum, CellStyle headerStyle,
            OpenBisModel openBisModel)
    {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(EXPERIMENT);
        row.getCell(0).setCellStyle(headerStyle);

        Row rowType = sheet.createRow(rowNum++);
        rowType.createCell(0).setCellValue(EXPERIMENT_TYPE_FIELD);
        rowType.getCell(0).setCellStyle(headerStyle);

        Row rowTypeValues = sheet.createRow(rowNum++);

        rowTypeValues.createCell(0).setCellValue(COLLECTION_TYPE);

        Row rowHeaders = sheet.createRow(rowNum++);

        // Populate header row with enum values
        Attribute[] fields = Attribute.values();
        for (int i = 0; i < fields.length; i++)
        {
            Cell cell = rowHeaders.createCell(i);
            cell.setCellValue(fields[i].getHeaderName());
            cell.setCellStyle(headerStyle);
        }

        for (var project : openBisModel.getProjects().values())
        {
            for (SampleType sampleType : openBisModel.getSampleTypes())
            {

                Row rowValues = sheet.createRow(rowNum++);

                String objectType = sampleType.getCode() != null ?
                        sampleType.getCode().toUpperCase(Locale.ROOT) :
                        "";
                String code = objectType + "_" + COLLECTION_TYPE;
                String name = sampleType.getDescription() != null ?
                        sampleType.getDescription() + " Collection" :
                        "Unknown Collection";

                rowValues.createCell(0).setCellValue(
                        project.getIdentifier()
                                .toString() + "/" + code);   // Identifier("Identifier", false),
                rowValues.createCell(1)
                        .setCellValue(code);                     // Code("Code", true),
                rowValues.createCell(2)
                        .setCellValue(project.getIdentifier()
                                .toString());                // Project("Project", true),
                rowValues.createCell(3)
                        .setCellValue(name);                     // Name("Name", true),
                rowValues.createCell(4).setCellValue(
                        objectType);               // DefaultObjectType("Default object type", true);
            }
        }
        autosizeColumns(sheet, fields.length);

        // add empty row
        sheet.createRow(rowNum++);

        return rowNum;

    }

    public static String extractLabel(String uri)
    {
        int hashIndex = uri.indexOf('#');
        if (hashIndex != -1)
        {
            return uri.substring(hashIndex + 1);
        } else
        {
            return uri;
        }
    }

    public static void autosizeColumns(Sheet sheet, int numCols)
    {
        for (int i = 0; i < numCols; i++)
        {
            sheet.autoSizeColumn(i);
        }
    }

}
