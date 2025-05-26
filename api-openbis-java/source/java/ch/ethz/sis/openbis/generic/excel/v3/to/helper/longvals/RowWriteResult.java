package ch.ethz.sis.openbis.generic.excel.v3.to.helper.longvals;

import java.util.List;

public class RowWriteResult
{
    private final int rowNum;

    private final List<LongCell> longCells;

    public RowWriteResult(int rowNum, List<LongCell> longCells)
    {
        this.rowNum = rowNum;
        this.longCells = longCells;
    }

    public int getRowNum()
    {
        return rowNum;
    }

    public List<LongCell> getLongCells()
    {
        return longCells;
    }

    public static class LongCell
    {

        private final String sheet;

        private final long rowNumber;

        private final long column;

        private final String value;

        public long getColumn()
        {
            return column;
        }

        public LongCell(String sheet, long rowNumber, long column, String value)
        {
            this.sheet = sheet;
            this.rowNumber = rowNumber;
            this.column = column;
            this.value = value;
        }

        public String getSheet()
        {
            return sheet;
        }

        public long getRowNumber()
        {
            return rowNumber;
        }

        public String getFileName()
        {
            return "value-" + sheet + "-" + column + "-" + rowNumber + ".txt";
        }

        public String getCellValue()
        {
            return "__" + getFileName() + "__";
        }

        public String getValue()
        {
            return value;
        }
    }
}
