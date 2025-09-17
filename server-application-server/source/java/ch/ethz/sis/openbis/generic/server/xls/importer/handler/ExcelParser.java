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
package ch.ethz.sis.openbis.generic.server.xls.importer.handler;

import static org.springframework.util.StringUtils.trimWhitespace;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExcelParser
{
    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, ExcelParser.class);

    public static List<List<List<String>>> parseExcel(byte[] xls, final Map<String, String> importValues)
    {
        List<List<List<String>>> lines = new ArrayList<>();

        try
        {
            Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(xls));

            for (int sheetIndex = 0; sheetIndex < wb.getNumberOfSheets(); ++sheetIndex)
            {
                Sheet sheet = wb.getSheetAt(sheetIndex);
                List<List<String>> sheetLines = new ArrayList<>();

                for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); ++rowIndex)
                {
                    Row row = sheet.getRow(rowIndex);

                    List<String> columns = new ArrayList<>();

                    if (row != null)
                    {
                        for (int cellIndex = 0; cellIndex < row.getLastCellNum(); ++cellIndex)
                        {
                            Cell cell = row.getCell(cellIndex);
                            if (cell != null)
                            {
                                String value = getFinalValue(importValues, extractCellValue(cell, sheetIndex, rowIndex, cellIndex));
                                if (value != null && value.isBlank()) {
                                    value = null;
                                }
                                columns.add(value);
                            } else
                            {
                                columns.add(null);
                            }
                        }
                    }

                    sheetLines.add(columns);
                }
                lines.add(sheetLines);
            }

        } catch (Exception e)
        {
            throw new UserFailureException(e.getMessage());
        }

        return lines;
    }

    private static String getFinalValue(final Map<String, String> importValues, final String value)
    {
        if (value != null && value.startsWith("__value-") && value.endsWith(".txt__"))
        {
            return trimWhitespace(importValues.get(value.substring(2, value.length() - 2)));
        } else
        {
            return value;
        }
    }

    private static String extractCellValue(Cell cell, int sheet, int row, int column)
    {
        String position = "[ sheet = " + sheet + ", row = " + row + ", column = " + column + "]";
        switch (cell.getCellTypeEnum())
        {
            case BLANK:
                return "";
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case NUMERIC:
                double numeric = cell.getNumericCellValue();
                if (Math.ceil(numeric) == Math.floor(numeric))
                {
                    return Integer.toString((int) numeric);
                } else
                {
                    return Double.toString(numeric);
                }
            case STRING:
                return cell.getStringCellValue().trim();
            case FORMULA:
                throw new ch.systemsx.cisd.common.exceptions.UserFailureException(
                        "Excel formulas are not supported but one was found in cell " + position
                );
            case ERROR:
                throw new ch.systemsx.cisd.common.exceptions.UserFailureException("There is an error in cell " + position);
            default:
                throw new ch.systemsx.cisd.common.exceptions.UserFailureException("Unknown data type of cell " + position);
        }
    }
}
