package ch.ethz.sis.openbis.generic.excel.v3.to.helper.longvals;

import org.apache.poi.ss.usermodel.Cell;

import java.util.Optional;

public class CellWriter
{
    public static int THRESHOLD = 32767;

    public static Optional<RowWriteResult.LongCell> writeCell(Cell cell, String value)
    {

        cell.getColumnIndex();

        if (value.length() < THRESHOLD)
        {
            cell.setCellValue(value);
            return Optional.empty();
        }
        RowWriteResult.LongCell longCell =
                new RowWriteResult.LongCell(cell.getSheet().getSheetName(),
                        cell.getRow().getRowNum(),
                        cell.getColumnIndex(), value);
        cell.setCellValue(longCell.getCellValue());
        return Optional.of(longCell);

    }

}
