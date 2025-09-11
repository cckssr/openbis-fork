package ch.ethz.sis.rdf.main;

import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;
import ch.ethz.sis.rdf.main.mappers.openBis.RdfToOpenBisMapper;
import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import ch.ethz.sis.rdf.main.parser.RDFReader;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

public class RDFCommandLineTest
{
    public final String inputFilePath = "src/test/resources/sphn_test_reader.ttl";
    public final String actualOutputFilePath = "src/test/resources/actual_output.xlsx";

    public final String actualOutputFilePath2 = "src/test/resources/actual_output2.xlsx";

    public final String expectedOutputFilePath = "src/test/resources/expected_output.xlsx";
    public final String inputFormatValue = "TTL";
    private ModelRDF modelRDF;

    @Before
    public void setup() {
        Config.setConfig(false, true, false);
        RDFReader rdfReader = new RDFReader();
        OntModel ontModel = ModelFactory.createOntologyModel();
        modelRDF =
                rdfReader.read(new String[] { inputFilePath }, inputFormatValue, false, ontModel);
    }

    @Test
    public void testReadRDFModel() {
        assertNotNull(modelRDF);
        assertEquals("https://biomedit.ch/rdf/sphn-schema/sphn#", modelRDF.ontNamespace);
    }

    @Test
    @Ignore
    public void testXLSXWriter()
    {
        OpenBisModel model = RdfToOpenBisMapper.convert(modelRDF, "/DEFAULT/SPHN");
        byte[] xlsx = ExcelWriter.convert(ExcelWriter.Format.EXCEL, model);
        try {
            Files.write(Path.of(actualOutputFilePath), xlsx);
        } catch (IOException e) {
            throw new RuntimeException("Error when writing XLSX file to disk: " + e.getMessage());
        }
        File outputFile = new File(actualOutputFilePath);
        assertTrue("The XLSX file should be created!", outputFile.exists());
    }

    @Test
    @Ignore
    public void testCompareExcelFiles() {
        File excelFile1 = new File(actualOutputFilePath);
        File excelFile2 = new File(expectedOutputFilePath);

        try (FileInputStream fis1 = new FileInputStream(excelFile1);
                Workbook workbook1 = new XSSFWorkbook(fis1);
                FileInputStream fis2 = new FileInputStream(excelFile2);
                Workbook workbook2 = new XSSFWorkbook(fis2)) {
            workbook1.write(new FileOutputStream(new File(actualOutputFilePath2)));

            assertSameSheets(workbook1, workbook2);

            int sheetCount = workbook1.getNumberOfSheets();
            for (int sheetIdx = 0; sheetIdx < sheetCount; sheetIdx++) {
                Sheet sheet1 = workbook1.getSheetAt(sheetIdx);
                Sheet sheet2 = workbook2.getSheetAt(sheetIdx);

                assertEquals("Sheets have different row counts", sheet1.getPhysicalNumberOfRows(), sheet2.getPhysicalNumberOfRows());

                Iterator<Row> rowIterator1 = sheet1.rowIterator();
                Iterator<Row> rowIterator2 = sheet2.rowIterator();

                while (rowIterator1.hasNext() && rowIterator2.hasNext()) {
                    Row row1 = rowIterator1.next();
                    Row row2 = rowIterator2.next();

                    assertEquals("Rows have different column counts", row1.getPhysicalNumberOfCells(), row2.getPhysicalNumberOfCells());

                    compareRows(row1, row2);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error while comparing Excel files", e);
        } finally
        {
            excelFile1.delete();
        }
    }

    private void assertSameSheets(Workbook workbook1, Workbook workbook2){

        int sheetsInWorkbook1 = workbook1.getNumberOfSheets();
        int sheetsInWorkbook2 = workbook2.getNumberOfSheets();
        assertEquals("Excel work books have different number of sheets. \n "
                        + "Sheets in work book 1 : " + sheetsInWorkbook1 + "\n "
                        + "Number of sheets in work book 2 : " + sheetsInWorkbook2,
                sheetsInWorkbook1, sheetsInWorkbook2);

        List<String> sheetsNameOfWb1 = new ArrayList<>();
        List<String> sheetsNameOfWb2 = new ArrayList<>();
        for (int i = 0; i < sheetsInWorkbook1; i++) {
            sheetsNameOfWb1.add(workbook1.getSheetName(i));
            sheetsNameOfWb2.add(workbook2.getSheetName(i));
        }
        Collections.sort(sheetsNameOfWb1);
        Collections.sort(sheetsNameOfWb2);
        assertEquals("Provided excel work books have different name of sheets.", sheetsNameOfWb1, sheetsNameOfWb2);
    }

    private void compareRows(Row row1, Row row2) {
        int cellCount = row1.getPhysicalNumberOfCells();

        for (int cellIdx = 0; cellIdx < cellCount; cellIdx++) {
            Cell cell1 = row1.getCell(cellIdx);
            Cell cell2 = row2.getCell(cellIdx);
            compareCells(cell1, cell2);
        }
    }

    private void compareCells(Cell cell1, Cell cell2) {
        if (cell1 == null && cell2 == null) {
            // Both cells are null, they are equal
            return;
        }

        if (cell1 == null || cell2 == null) {
            throw new AssertionError("One of the cells is null while the other is not.");
        }

        assertEquals("Cell types differ", cell1.getCellTypeEnum(), cell2.getCellTypeEnum());


        switch (cell1.getCellTypeEnum()) {
            case FORMULA:
                assertEquals("Formulas differ", cell1.getCellFormula(), cell2.getCellFormula());
                break;
            case NUMERIC:
                assertEquals("Numeric values differ", cell1.getNumericCellValue(), cell2.getNumericCellValue(), 0.1);
                break;
            case STRING:
                assertEquals("String values differ", cell1.getStringCellValue(), cell2.getStringCellValue());
                break;
            case BLANK:
                assertEquals("Blank cells differ", cell2.getCellTypeEnum(), CellType.BLANK);
                break;
            case BOOLEAN:
                assertEquals("Boolean values differ", cell1.getBooleanCellValue(), cell2.getBooleanCellValue());
                break;
            default:
                throw new IllegalArgumentException("Unsupported cell type: " + cell1.getCellTypeEnum());
        }
    }
}