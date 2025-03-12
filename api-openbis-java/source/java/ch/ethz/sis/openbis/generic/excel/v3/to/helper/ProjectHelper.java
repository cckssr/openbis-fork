package ch.ethz.sis.openbis.generic.excel.v3.to.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class ProjectHelper
{
    private static final String PROJECT = "PROJECT";

    private enum Attribute { // implements IAttribute {
        Identifier("Identifier", false),
        Code("Code", true),
        Space("Space", true),
        Description("Description", false);

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

    public int addProjectSection(Sheet sheet, int rowNum, CellStyle headerStyle,
            OpenBisModel openBisModel)
    {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(PROJECT);
        row.getCell(0).setCellStyle(headerStyle);

        Row rowHeaders = sheet.createRow(rowNum++);

        // Populate header row with enum values
        Attribute[] fields = Attribute.values();
        for (int i = 0; i < fields.length; i++) {
            Cell cell = rowHeaders.createCell(i);
            cell.setCellValue(fields[i].getHeaderName());
            cell.setCellStyle(headerStyle);
        }

        for (Project project : openBisModel.getProjects().values())
        {
            Row rowValues = sheet.createRow(rowNum++);

            rowValues.createCell(0)
                    .setCellValue(project.getIdentifier()
                            .getIdentifier());      // Identifier("Identifier", false),
            rowValues.createCell(1).setCellValue(project.getCode());      // Code("Code", true),
            rowValues.createCell(2)
                    .setCellValue(project.getSpace().getCode());      // Space("Space", true),
            rowValues.createCell(3).setCellValue("");      // Description("Description", false);
        }
        // add empty row
        sheet.createRow(rowNum++);

        return rowNum;
    }
}
