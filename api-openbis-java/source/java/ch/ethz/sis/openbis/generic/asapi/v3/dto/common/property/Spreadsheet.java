package ch.ethz.sis.openbis.generic.asapi.v3.dto.common.property;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.ObjectToString;
import ch.systemsx.cisd.base.annotation.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.python.antlr.ast.Str;

import java.io.Serializable;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@JsonObject("as.dto.common.property.Spreadsheet")
public final class Spreadsheet implements Serializable
{
    @JsonProperty
    String version = "1";
    @JsonProperty
    String[] headers;
    @JsonProperty
    String[][] data;
    @JsonProperty
    String[][] values;
    @JsonProperty
    Integer[] width;
    @JsonProperty
    Map<String, String> style;
    @JsonProperty
    Map<String, String> meta;

    @JsonIgnore
    static final String DEFAULT_STYLE = "text-align: center;";

    public Spreadsheet()
    {
        this(10, 10);
    }

    @JsonIgnore
    public Spreadsheet(int columnCount, int rowCount)
    {
        if(columnCount < 0 || rowCount < 0)
        {
            throw new IllegalArgumentException("Parameters must not be negative!");
        }

        this.headers = getDefaultHeaders(columnCount);
        this.data = new String[rowCount][columnCount];
        this.values = new String[rowCount][columnCount];
        this.style = new HashMap<>();
        for(int i=0;i<columnCount;i++)
        {
            for(int j=0;j<rowCount;j++)
            {
                this.data[j][i] = "";
                this.values[j][i] = "";
                this.style.put(headers[i] + (j+1), DEFAULT_STYLE);
            }
        }
        this.width = IntStream.generate(() -> 50).limit(columnCount).boxed().toArray(Integer[]::new);
        this.meta = new HashMap<>();
    }

    @JsonIgnore
    public int getColumnCount()
    {
        return this.headers.length;
    }

    @JsonIgnore
    public int getRowCount()
    {
        return this.data.length;
    }

    @JsonIgnore
    public String getVersion()
    {
        return version;
    }

    @JsonIgnore
    public Map<String, String> getMetaData()
    {
        return meta;
    }

    @JsonIgnore
    public void setMetaData(Map<String, String> meta)
    {
        this.meta = meta;
    }

    @JsonIgnore
    public String[][] getFormulas() {
        String[][] formulas = new String[this.data.length][];
        for (int i = 0; i < this.data.length; i++) {
            formulas[i] = new String[this.data[i].length];
            System.arraycopy(this.data[i], 0, formulas[i], 0, this.data[i].length);
        }
        return formulas;
    }

    @JsonIgnore
    public String[] getHeaders()
    {
        String[] headers = new String[this.headers.length];
        System.arraycopy(this.headers, 0, headers, 0, this.headers.length);
        return headers;
    }

    @JsonIgnore
    public String[][] getValues()
    {
        String[][] values = new String[this.values.length][];
        for (int i = 0; i < this.values.length; i++) {
            values[i] = new String[this.values[i].length];
            System.arraycopy(this.values[i], 0, values[i], 0, this.values[i].length);
        }
        return values;
    }

    @JsonIgnore
    public Integer[] getWidth()
    {
        Integer[] width = new Integer[this.width.length];
        System.arraycopy(this.width, 0, width, 0, this.width.length);
        return width;
    }

    @JsonIgnore
    public Map<String, String> getStyle()
    {
        return new HashMap<String,String>(this.style);
    }

    @JsonIgnore
    public CellBuilder cell(String columnHeader, int rowNumber) {
        return new CellBuilder(columnHeader, rowNumber);
    }

    @JsonIgnore
    public CellBuilder cell(int columnNumber, int rowNumber) {
        return new CellBuilder(columnNumber, rowNumber);
    }

    @JsonIgnore
    public ColumnBuilder column(String columnHeader) {
        return new ColumnBuilder(columnHeader);
    }

    @JsonIgnore
    public ColumnBuilder column(int columnNumber) {
        return new ColumnBuilder(columnNumber);
    }

    @JsonIgnore
    public void addRow()
    {
        String[][] newData = new String[this.data.length+1][];
        System.arraycopy(this.data, 0, newData, 0, this.data.length);
        newData[this.data.length] = Stream.generate(() -> "").limit(this.headers.length).toArray(String[]::new);
        this.data = newData;

        String[][] newValues = new String[this.values.length+1][];
        System.arraycopy(this.values, 0, newValues, 0, this.values.length);
        newValues[this.values.length] = Stream.generate(() -> "").limit(this.headers.length).toArray(String[]::new);
        this.values = newValues;

        for(int i=0; i < this.headers.length; i++)
        {
            this.style.put(this.headers[i] + this.data.length, DEFAULT_STYLE);
        }

    }

    @JsonIgnore
    public void deleteRow(int rowNumber)
    {
        if(rowNumber < 1 || rowNumber > this.data.length)
        {
            throw new IllegalArgumentException("Wrong row number!");
        }
        for(int i=rowNumber;i<this.data.length;i++)
        {
            for(int j=0;j<this.headers.length;j++)
            {
                this.style.put(this.headers[j] + i, this.style.get(this.headers[j] + (i+1)));
            }
        }
        for(int j=0;j<this.headers.length;j++)
        {
            this.style.remove(this.headers[j] + this.data.length);
        }

        String[][] newData = new String[this.data.length-1][];
        String[][] newValues = new String[this.data.length-1][];
        if(rowNumber-1 > 0)
        {
            System.arraycopy(this.data, 0, newData, 0, rowNumber-1);
            System.arraycopy(this.values, 0, newValues, 0, rowNumber-1);
        }
        if(rowNumber < this.data.length)
        {
            System.arraycopy(this.data, rowNumber, newData, rowNumber-1, this.data.length - rowNumber);
            System.arraycopy(this.values, rowNumber, newValues, rowNumber-1, this.data.length - rowNumber);
        }
        this.data = newData;
        this.values = newValues;
    }

    @JsonIgnore
    public void addColumn()
    {
        addColumn(getDefaultHeaders(this.headers.length + 1)[this.headers.length]);
    }

    @JsonIgnore
    public void addColumn(String label)
    {
        int oldHeadersLength = this.headers.length;
        String[] newHeaders = new String[oldHeadersLength+1];
        System.arraycopy(this.headers, 0, newHeaders, 0, oldHeadersLength);
        newHeaders[oldHeadersLength] = label;
        this.headers = newHeaders;

        for(int i=0;i<this.data.length;i++)
        {
            String[] temp = new String[oldHeadersLength+1];
            System.arraycopy(this.data[i], 0, temp, 0, oldHeadersLength);
            temp[oldHeadersLength] = "";
            this.data[i] = temp;

            temp = new String[oldHeadersLength+1];
            System.arraycopy(this.values[i], 0, temp, 0, oldHeadersLength);
            temp[oldHeadersLength] = "";
            this.values[i] = temp;

            this.style.put(label + (i+1), "text-align: center;");
        }

        Integer[] newWidth = new Integer[oldHeadersLength+1];
        System.arraycopy(this.width, 0, newWidth, 0, oldHeadersLength);
        newWidth[oldHeadersLength] = 50;
        this.width = newWidth;
    }

    @JsonIgnore
    public void deleteColumn(String label)
    {
        boolean foundLabel = false;
        for(int i=0;i<this.headers.length;i++)
        {
            if(this.headers[i].equals(label))
            {
                foundLabel = true;
                deleteColumn(i+1);
                break;
            }
        }
        if(!foundLabel)
        {
            throw new IllegalArgumentException(String.format("Could not find column '%s' to delete!", label));
        }
    }

    @JsonIgnore
    public void deleteColumn(int columnNumber)
    {
        if(columnNumber < 1 || columnNumber > this.headers.length) {
            throw new IllegalArgumentException("Wrong column number!");
        }
        int oldHeadersLength = this.headers.length;
        String[] newHeaders = new String[oldHeadersLength-1];
        Integer[] newWidth = new Integer[oldHeadersLength-1];
        String headerToDelete = this.headers[columnNumber-1];

        if(columnNumber - 1 > 0)
        {
            System.arraycopy(this.headers, 0, newHeaders, 0, columnNumber-1);
            System.arraycopy(this.width, 0, newWidth, 0, columnNumber-1);
        }
        if(columnNumber < oldHeadersLength)
        {
            System.arraycopy(this.headers, columnNumber, newHeaders, columnNumber-1, oldHeadersLength - columnNumber);
            System.arraycopy(this.width, columnNumber, newWidth, columnNumber-1, oldHeadersLength - columnNumber);
        }
        this.headers = newHeaders;
        this.width = newWidth;

        for(int i=0;i<this.data.length;i++)
        {
            String[] tempData = new String[this.data[i].length-1];
            String[] tempValues = new String[this.data[i].length-1];
            if(columnNumber - 1 > 0)
            {
                System.arraycopy(this.data[i], 0, tempData, 0, columnNumber-1);
                System.arraycopy(this.values[i], 0, tempValues, 0, columnNumber-1);
            }
            if(columnNumber < oldHeadersLength)
            {
                System.arraycopy(this.data[i], columnNumber, tempData, columnNumber-1, oldHeadersLength - columnNumber);
                System.arraycopy(this.values[i], columnNumber, tempValues, columnNumber-1, oldHeadersLength - columnNumber);
            }
            this.data[i] = tempData;
            this.values[i] = tempValues;
            this.style.remove(headerToDelete + (i+1));
        }
    }

    public final class CellBuilder {

        private String columnHeader;
        private int columnIndex;
        private int rowIndex;

        private CellBuilder(String columnHeader, int rowNumber)
        {
            if(rowNumber < 1 || rowNumber > data.length)
            {
                throw new IllegalArgumentException("Wrong row index!");
            }
            boolean shouldBreak = true;
            for(int i=0;i< headers.length; i++)
            {
                if(headers[i].equals(columnHeader))
                {
                    this.columnHeader = columnHeader;
                    this.columnIndex = i;
                    this.rowIndex = rowNumber-1;
                    shouldBreak = false;
                    break;
                }
            }
            if(shouldBreak)
            {
                throw new IllegalArgumentException(String.format("Could not find cell with indices: '%s' '%d'", columnHeader, rowNumber));
            }
        }

        private CellBuilder(int columnNumber, int rowNumber)
        {
            if(headers.length < columnNumber || columnNumber < 1)
            {
                throw new IllegalArgumentException("Wrong column index!");
            }
            if(rowNumber < 1 || rowNumber > data.length)
            {
                throw new IllegalArgumentException("Wrong row index!");
            }
            this.columnHeader = headers[columnNumber-1];
            this.columnIndex = columnNumber-1;
            this.rowIndex = rowNumber-1;
        }


        public String getFormula()
        {
            return data[rowIndex][columnIndex];
        }

        public void setFormula(String formula)
        {
            data[rowIndex][columnIndex] = formula;
        }

        public String getValue()
        {
            return values[rowIndex][columnIndex];
        }

        public void setValue(String value)
        {
            values[rowIndex][columnIndex] = value;
            data[rowIndex][columnIndex] = value;
        }

        public String getStyle()
        {
            return style.get(columnHeader + (rowIndex+1)) ;
        }

        public void setStyle(String newStyle)
        {
            style.put(columnHeader + (rowIndex+1), newStyle);
        }

        public String getColumnHeader()
        {
            return columnHeader;
        }

        public int getColumnNumber()
        {
            return columnIndex+1;
        }


        public int getRowNumber()
        {
            return rowIndex+1;
        }

    }

    public class ColumnBuilder {
        private int index;

        private ColumnBuilder(String columnHeader)
        {
            boolean shouldBreak = true;
            for(int i=0;i<headers.length;i++)
            {
                if(headers[i].equals(columnHeader))
                {
                    this.index = i;
                    shouldBreak = false;
                    break;
                }
            }
            if(shouldBreak)
            {
                throw new IllegalArgumentException("Could not find header!");
            }
        }

        private ColumnBuilder(int columnNumber)
        {
            this.index = columnNumber-1;
        }

        public String getHeader()
        {
            return headers[index];
        }

        public void setHeader(String header)
        {
            headers[index] = header;
        }

        public int getWidth()
        {
            return width[index];
        }

        public void setWidth(int columnWidth)
        {
            width[index] = columnWidth;
        }

        public int getIndex()
        {
            return index;
        }

    }

    @JsonIgnore
    private static String[] getDefaultHeaders(int count)
    {
        if(count < 1) {
            throw new IllegalArgumentException("Count must not be less than 1");
        }
        int alphabetMax = 26;
        List<String> headers = new ArrayList<>();
        for(char minChar = 'A'; minChar < 'A' + Math.min(alphabetMax, count); minChar++)
        {
            headers.add("" + minChar);
        }
        if(count > alphabetMax)
        {
            for(int x = 0; x < count / alphabetMax; x++)
            {
                String ch = "" + (char)('A' + x);

                for(int y = 0; y < Math.min(alphabetMax, count-alphabetMax*(x+1)) ; y++)
                {
                    headers.add(ch + (char)('A'+y));
                }
            }
        }
        return headers.toArray(String[]::new);
    }


    @Override
    public String toString()
    {
        return new ObjectToString(this).append("version", version).toString();
    }
}