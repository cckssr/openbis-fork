package ch.eth.sis.rocrate;

import ch.eth.sis.rocrate.parser.ExcelParser;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntity;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main
{

    private static boolean TEST_MODE  = true;
    private static String TEST_FILE = "/home/meiandr/Documents/sissource/openbis/lib-ro-crate/reference-openbis-export/metadata.xlsx";

    public static void main(String[] args) throws IOException
    {
        if (TEST_MODE){
            Path excelInputFile = Path.of(TEST_FILE);

            Map<EntityTypePermId, IEntityType> schema = new HashMap<>();
            Map<ObjectIdentifier, AbstractEntity> metadata = new HashMap<>();
            InputStream inputStream = Files.newInputStream(excelInputFile);
            new Main().readFile(excelInputFile);

            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);



            System.out.println("Loaded");


        }



        System.out.println("lol");
    }


    public List<List<List<String>>> readFile(Path excelInputFile) throws IOException
    {
        Map<String, String> textFileNameToTextFileContents = new HashMap<>();
        byte[] bytes = Files.readAllBytes(excelInputFile);
        List<List<List<String>>> document = ExcelParser.parseExcel(bytes, textFileNameToTextFileContents);
        return document;
    }


    public SampleType readSampleType(int pages, int line, List<List<List<String>>> input) {
        return  new SampleType();

    }

}
