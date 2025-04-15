package ch.openbis.rocrate.app.examples;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;
import ch.openbis.rocrate.app.reader.RdfToModel;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class JSON2ExcelTest {

    static final String INPUT = "src/test/resources/json2excel";

    static final String REFERENCE_EXCEL = "src/test/resources/json2excel/ro_out.xlsx";

    public static final String TMP_OPENBIS_TEST_RO_OUT_XLSX = "/tmp/openbis_test_ro_out.xlsx";

    public static final EntityTypePermId
            PUBLICATION_TYPE_PERMID = new EntityTypePermId("PUBLICATION", EntityKind.SAMPLE);

    @Test
    public void conversionTest() throws IOException
    {

        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateFolderReader.readCrate(INPUT);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (var type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }


        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT");

        // assertions on model because the Excel files might have different orders of stuff
        // ordering comes from the RO-Crate library and is outside our control

        Optional<IEntityType>
                maybePublicatioNType =
                openBisModel.getEntityTypes().values().stream().filter(x -> x.getPermId().equals(
                        PUBLICATION_TYPE_PERMID)).findFirst();
        List<String> publicationCodes = List.of("PUBLICATION.PUBLISHER",
                "PUBLICATION.PUBLICATION_YEAR",
                "XMLCOMMENTS");


        assertEquals(8, openBisModel.getEntities().size());
        assertTrue(maybePublicatioNType.isPresent());
        assertEquals(2, openBisModel.getSpaces().size());
        assertEquals(2, openBisModel.getProjects().size());
        for (String propertyCode : publicationCodes)
        {
            assertTrue(maybePublicatioNType.filter(x -> x.getPropertyAssignments().stream()
                            .anyMatch(y -> y.getPropertyType().getCode().equals(propertyCode)))
                    .isPresent());
        }

            assertNotNull(openBisModel.getEntities()
                    .get(new SampleIdentifier("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUBCREA27")));
            assertNotNull(openBisModel.getEntities()
                    .get(new SampleIdentifier("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUBCREA22")));
            assertNotNull(openBisModel.getEntities()
                    .get(new SampleIdentifier("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUBCREA23")));
            assertNotNull(openBisModel.getEntities()
                    .get(new SampleIdentifier("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB30")));
            assertNotNull(openBisModel.getEntities()
                    .get(new SampleIdentifier("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB29")));
            assertNotNull(openBisModel.getEntities()
                    .get(new SampleIdentifier("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB25")));
            assertNotNull(openBisModel.getEntities()
                    .get(new SampleIdentifier("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUBPUB24")));
            assertNotNull(openBisModel.getEntities()
                    .get(new SampleIdentifier("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUBPUB26")));
        {
            Sample sample = (Sample) openBisModel.getEntities()
                    .get(new SampleIdentifier("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB30"));

            assertEquals("Quantum computational advantage using photons",
                    sample.getProperties().get("NAME").toString());
            assertEquals("Registered", sample.getProperties().get("PUBLICATION.STATUS").toString());
            assertEquals("DataPaper", sample.getProperties().get("PUBLICATION.TYPE").toString());
            assertEquals("https://www.science.org/doi/10.1126/science.abe8770",
                    sample.getProperties().get("PUBLICATION.URL").toString());
            assertEquals("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUBCREA27",
                    sample.getProperties().get("PUBLICATION.CREATOR").toString());
            assertEquals("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUBPUB26",
                    sample.getProperties().get("PUBLICATION.PUBLISHER").toString());

            Sample creatorSample = (Sample) openBisModel.getEntities()
                    .get(new SampleIdentifier("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUBCREA27"));
            assertEquals("https://orcid.org/0000-0001-9844-8214",
                    creatorSample.getProperties().get("PUBLICATION_CREATOR.IDENTIFIER").toString());
            assertEquals("https://orcid.org",
                    creatorSample.getProperties().get("PUBLICATION_CREATOR.IDENTIFIER_SCHEME")
                            .toString());

            Sample creatorPublisher = (Sample) openBisModel.getEntities()
                    .get(new SampleIdentifier("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUBPUB26"));
            assertEquals("Science",
                    creatorPublisher.getProperties().get("PUBLICATION_PUBLISHER.IDENTIFIER")
                            .toString());


        }



        byte[] writtenStuff = ExcelWriter.convert(ExcelWriter.Format.EXCEL, openBisModel);
        try (FileOutputStream byteArrayOutputStream = new FileOutputStream(
                TMP_OPENBIS_TEST_RO_OUT_XLSX))
        {
            byteArrayOutputStream.write(writtenStuff);
        }

        try (FileInputStream fis1 = new FileInputStream(TMP_OPENBIS_TEST_RO_OUT_XLSX);
                Workbook workbook1 = new XSSFWorkbook(fis1);
                FileInputStream fis2 = new FileInputStream(REFERENCE_EXCEL);
                Workbook workbook2 = new XSSFWorkbook(fis2)
        )
        {
            //assertSameSheets(workbook1, workbook2);

        }
    }

    private void assertSameSheets(Workbook workbook1, Workbook workbook2)
    {

        int sheetsInWorkbook1 = workbook1.getNumberOfSheets();
        int sheetsInWorkbook2 = workbook2.getNumberOfSheets();
        assertEquals("Excel work books have different number of sheets. \n "
                        + "Sheets in work book 1 : " + sheetsInWorkbook1 + "\n "
                        + "Number of sheets in work book 2 : " + sheetsInWorkbook2,
                sheetsInWorkbook1, sheetsInWorkbook2);

        List<String> sheetsNameOfWb1 = new ArrayList<>();
        List<String> sheetsNameOfWb2 = new ArrayList<>();
        for (int i = 0; i < sheetsInWorkbook1; i++)
        {
            sheetsNameOfWb1.add(workbook1.getSheetName(i));
            sheetsNameOfWb2.add(workbook2.getSheetName(i));
        }
        Collections.sort(sheetsNameOfWb1);
        Collections.sort(sheetsNameOfWb2);
        assertEquals("Provided excel work books have different name of sheets.", sheetsNameOfWb1,
                sheetsNameOfWb2);
        int sheetCount = workbook1.getNumberOfSheets();
        for (int sheetIdx = 0; sheetIdx < sheetCount; sheetIdx++)
        {
            Sheet sheet1 = workbook1.getSheetAt(sheetIdx);
            Sheet sheet2 = workbook2.getSheetAt(sheetIdx);

            assertEquals("Sheets have different row counts", sheet1.getPhysicalNumberOfRows(),
                    sheet2.getPhysicalNumberOfRows());

            Iterator<Row> rowIterator1 = sheet1.rowIterator();
            Iterator<Row> rowIterator2 = sheet2.rowIterator();

            while (rowIterator1.hasNext() && rowIterator2.hasNext())
            {
                Row row1 = rowIterator1.next();
                Row row2 = rowIterator2.next();

                assertEquals("Rows have different column counts", row1.getPhysicalNumberOfCells(),
                        row2.getPhysicalNumberOfCells());

                compareRows(row1, row2);
            }
        }

    }

    private void compareRows(Row row1, Row row2)
    {
        int cellCount = row1.getPhysicalNumberOfCells();

        for (int cellIdx = 0; cellIdx < cellCount; cellIdx++)
        {
            Cell cell1 = row1.getCell(cellIdx);
            Cell cell2 = row2.getCell(cellIdx);
            compareCells(cell1, cell2);
        }
    }



    private void compareCells(Cell cell1, Cell cell2)
    {
        if (cell1 == null && cell2 == null)
        {
            // Both cells are null, they are equal
            return;
        }

        if (cell1 == null || cell2 == null)
        {
            throw new AssertionError("One of the cells is null while the other is not.");
        }

        assertEquals("Cell types differ", cell1.getCellTypeEnum(), cell2.getCellTypeEnum());

        switch (cell1.getCellTypeEnum())
        {
            case FORMULA:
                assertEquals("Formulas differ", cell1.getCellFormula(), cell2.getCellFormula());
                break;
            case NUMERIC:
                assertEquals("Numeric values differ", cell1.getNumericCellValue(),
                        cell2.getNumericCellValue(), 0.1);
                break;
            case STRING:
                assertEquals("String values differ", cell1.getStringCellValue(),
                        cell2.getStringCellValue());
                break;
            case BLANK:
                assertEquals("Blank cells differ", cell2.getCellTypeEnum(), CellType.BLANK);
                break;
            case BOOLEAN:
                assertEquals("Boolean values differ", cell1.getBooleanCellValue(),
                        cell2.getBooleanCellValue());
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported cell type: " + cell1.getCellTypeEnum());
        }
    }

}
