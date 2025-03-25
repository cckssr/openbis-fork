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

package ch.ethz.sis.openbis.generic.asapi.v3.dto.common.property;

import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.*;

public class SpreadsheetTest
{
    @Test
    public void newSpreadsheetDeleteColumns()
    {
        Spreadsheet spreadsheet = new Spreadsheet(10, 10);

        assertEquals(spreadsheet.headers.length, 10);
        assertEquals(spreadsheet.width.length, 10);
        assertEquals(spreadsheet.style.size(), 100);

        spreadsheet.data[0] = new String[] {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

        spreadsheet.deleteColumn(1); //A
        assertEquals(spreadsheet.headers.length, 9);
        assertEquals(spreadsheet.width.length, 9);
        assertEquals(spreadsheet.style.size(), 90);
        assertEquals(spreadsheet.data[0], new String[] {"B", "C", "D", "E", "F", "G", "H", "I", "J"});

        spreadsheet.deleteColumn(9); //J
        assertEquals(spreadsheet.headers.length, 8);
        assertEquals(spreadsheet.width.length, 8);
        assertEquals(spreadsheet.style.size(), 80);
        assertEquals(spreadsheet.data[0], new String[] {"B", "C", "D", "E", "F", "G", "H", "I"});

        spreadsheet.deleteColumn("C"); //C
        assertEquals(spreadsheet.headers.length, 7);
        assertEquals(spreadsheet.width.length, 7);
        assertEquals(spreadsheet.style.size(), 70);
        assertEquals(spreadsheet.data[0], new String[] {"B", "D", "E", "F", "G", "H", "I"});
    }

    @Test
    public void newSpreadsheetAddColumns()
    {
        Spreadsheet spreadsheet = new Spreadsheet(5, 5);

        assertEquals(spreadsheet.headers.length, 5);
        assertEquals(spreadsheet.width.length, 5);
        assertEquals(spreadsheet.style.size(), 25);
        spreadsheet.data[0] = new String[] {"A", "B", "C", "D", "E"};

        spreadsheet.addColumn(); //F
        assertEquals(spreadsheet.headers.length, 6);
        assertEquals(spreadsheet.width.length, 6);
        assertEquals(spreadsheet.style.size(), 30);
        assertEquals(spreadsheet.headers, new String[] {"A", "B", "C", "D", "E", "F"});
        assertEquals(spreadsheet.data[0], new String[] {"A", "B", "C", "D", "E", ""});

        spreadsheet.addColumn("OPENBIS");
        assertEquals(spreadsheet.headers.length, 7);
        assertEquals(spreadsheet.width.length, 7);
        assertEquals(spreadsheet.style.size(), 35);
        assertEquals(spreadsheet.headers, new String[] {"A", "B", "C", "D", "E", "F", "OPENBIS"});
        assertEquals(spreadsheet.data[0], new String[] {"A", "B", "C", "D", "E", "", ""});

        spreadsheet.addColumn(); //H
        assertEquals(spreadsheet.headers.length, 8);
        assertEquals(spreadsheet.width.length, 8);
        assertEquals(spreadsheet.style.size(), 40);
        assertEquals(spreadsheet.headers, new String[] {"A", "B", "C", "D", "E", "F", "OPENBIS", "H"});
        assertEquals(spreadsheet.data[0], new String[] {"A", "B", "C", "D", "E", "", "", ""});
    }

    @Test
    public void newSpreadsheetAddColumnAfterZ()
    {
        Spreadsheet spreadsheet = new Spreadsheet(26, 5);

        assertEquals(spreadsheet.headers.length, 26);
        assertEquals(spreadsheet.width.length, 26);
        assertEquals(spreadsheet.style.size(), 130); //26*5=130

        spreadsheet.addColumn(); //AA
        assertEquals(spreadsheet.headers.length, 27);
        assertEquals(spreadsheet.width.length, 27);
        assertEquals(spreadsheet.style.size(), 135);
        assertEquals(spreadsheet.headers[spreadsheet.headers.length-1], "AA");

        spreadsheet.addColumn("OPENBIS");
        assertEquals(spreadsheet.headers.length, 28);
        assertEquals(spreadsheet.width.length, 28);
        assertEquals(spreadsheet.style.size(), 140);
        assertEquals(spreadsheet.headers[spreadsheet.headers.length-2], "AA");
        assertEquals(spreadsheet.headers[spreadsheet.headers.length-1], "OPENBIS");

        spreadsheet.addColumn(); //AC
        assertEquals(spreadsheet.headers.length, 29);
        assertEquals(spreadsheet.width.length, 29);
        assertEquals(spreadsheet.style.size(), 145);
        assertEquals(spreadsheet.headers[spreadsheet.headers.length-3], "AA");
        assertEquals(spreadsheet.headers[spreadsheet.headers.length-2], "OPENBIS");
        assertEquals(spreadsheet.headers[spreadsheet.headers.length-1], "AC");
    }

    @Test
    public void newSpreadsheetDeleteRow()
    {
        Spreadsheet spreadsheet = new Spreadsheet(5, 5);

        assertEquals(spreadsheet.headers.length, 5);
        assertEquals(spreadsheet.width.length, 5);
        assertEquals(spreadsheet.style.size(), 25);
        for(int i=0;i<5;i++)
        {
            spreadsheet.data[i][0] = "" + (i+1);
        }

        spreadsheet.style.put("A2", "some_custom_style");
        spreadsheet.deleteRow(1);
        assertEquals(spreadsheet.style.size(), 20);
        assertEquals(spreadsheet.data.length, 4);
        assertEquals(spreadsheet.data[0][0], "2");
        assertEquals(spreadsheet.style.get("A1"), "some_custom_style");


        spreadsheet.deleteRow(4);
        assertEquals(spreadsheet.style.size(), 15);
        assertEquals(spreadsheet.data.length, 3);
        assertEquals(spreadsheet.data[spreadsheet.data.length-1][0], "4");

        spreadsheet.deleteRow(2); //C
        assertEquals(spreadsheet.style.size(), 10);
        assertEquals(spreadsheet.data.length, 2);
        assertEquals(spreadsheet.data[0][0], "2");
        assertEquals(spreadsheet.data[1][0], "4");
    }

    @Test
    public void newSpreadsheetAddRow()
    {
        Spreadsheet spreadsheet = new Spreadsheet(5, 5);

        assertEquals(spreadsheet.headers.length, 5);
        for(int i=0;i<5;i++)
        {
            spreadsheet.data[i][0] = "" + (i+1);
        }

        spreadsheet.addRow();
        assertEquals(spreadsheet.data.length, 6);
        for(int i=0;i<5;i++)
        {
            assertEquals(spreadsheet.data[i][0], "" + (i+1));
            assertEquals(spreadsheet.style.get(spreadsheet.headers[i] + spreadsheet.data.length), Spreadsheet.DEFAULT_STYLE);
        }
        assertEquals(spreadsheet.data[spreadsheet.data.length-1][0], "");
    }

    @Test
    public void spreadsheetGetFormulas() {
        Spreadsheet spreadsheet = new Spreadsheet(5, 5);
        for(int i=0;i<5;i++)
        {
            spreadsheet.data[i][0] = "" + (i+1);
        }

        String[][] formulas = spreadsheet.getFormulas();
        assertNotSame(formulas, spreadsheet.data);
        assertEquals(formulas.length, spreadsheet.data.length);
        assertEquals(formulas[0].length, spreadsheet.data[0].length);

        for(int i=0;i<5;i++)
        {
            for(int j=0;j<5;j++) {
                assertEquals(formulas[i][j], spreadsheet.data[i][j]);
            }
        }

        formulas[1][1] = "SOME_VALUE";
        assertNotEquals(formulas[1][1], spreadsheet.data[1][1]);

    }

    @Test
    public void spreadsheetGetValues() {
        Spreadsheet spreadsheet = new Spreadsheet(5, 5);
        for(int i=0;i<5;i++)
        {
            spreadsheet.values[i][0] = "" + (i+1);
        }

        String[][] values = spreadsheet.getValues();
        assertNotSame(values, spreadsheet.values);
        assertEquals(values.length, spreadsheet.values.length);
        assertEquals(values[0].length, spreadsheet.values[0].length);

        for(int i=0;i<5;i++)
        {
            for(int j=0;j<5;j++) {
                assertEquals(values[i][j], spreadsheet.values[i][j]);
            }
        }

        values[1][1] = "SOME_VALUE";
        assertNotEquals(values[1][1], spreadsheet.values[1][1]);
    }

    @Test
    public void spreadsheetGetHeaders() {
        Spreadsheet spreadsheet = new Spreadsheet(5, 5);
        for(int i=0;i<5;i++)
        {
            spreadsheet.headers[i] = "" + (i+1);
        }

        String[] headers = spreadsheet.getHeaders();
        assertNotSame(headers, spreadsheet.headers);
        assertEquals(headers.length, spreadsheet.headers.length);

        for(int i=0;i<5;i++)
        {
            assertEquals(headers[i], spreadsheet.headers[i]);
        }

        headers[1] = "SOME_VALUE";
        assertNotEquals(headers[1], spreadsheet.headers[1]);
    }

    @Test
    public void spreadsheetGetWidth() {
        Spreadsheet spreadsheet = new Spreadsheet(5, 5);
        for(int i=0;i<5;i++)
        {
            spreadsheet.width[i] = (i+1);
        }

        Integer[] width = spreadsheet.getWidth();
        assertNotSame(width, spreadsheet.width);
        assertEquals(width.length, spreadsheet.width.length);

        for(int i=0;i<5;i++)
        {
            assertEquals(width[i], spreadsheet.width[i]);
        }

        width[1] = 150;
        assertNotEquals(width[1], spreadsheet.width[1]);
    }

    @Test
    public void spreadsheetGetStyle() {
        Spreadsheet spreadsheet = new Spreadsheet(5, 5);
        for(int i=0;i<5;i++)
        {
            spreadsheet.style.put(""+(i+1), "" + (i+1)) ;
        }

        Map<String, String> style = spreadsheet.getStyle();
        assertNotSame(style, spreadsheet.style);
        assertEquals(style.size(), spreadsheet.style.size());

        for(Map.Entry<String, String> entry : style.entrySet())
        {
            assertTrue(spreadsheet.style.containsKey(entry.getKey()));
            assertEquals(entry.getValue(), spreadsheet.style.get(entry.getKey()));
        }

        style.put("test", "");
        assertFalse(spreadsheet.style.containsKey("test"));
        assertEquals(style.size(), spreadsheet.style.size() + 1);
    }
}
