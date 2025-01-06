package ch.eth.sis.rocrate;

import ch.eth.sis.rocrate.parser.ExcelConversionParser;
import ch.eth.sis.rocrate.parser.results.ParseResult;
import ch.eth.sis.rocrate.writer.Writer;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main
{

    private static boolean TEST_MODE = true;

    private static String TEST_FILE =
            "/home/meiandr/Documents/sissource/openbis/lib-ro-crate/reference-openbis-export/metadata.xlsx";

    public static void main(String[] args) throws IOException
    {

        if (TEST_MODE)
        {
            Path excelInputFile = Path.of(TEST_FILE);

            InputStream inputStream = Files.newInputStream(excelInputFile);
            String[] files = new String[] { TEST_FILE };
            ExcelConversionParser
                    excelConversionParser =
                    new ExcelConversionParser(
                            files);
            ParseResult parseResult = excelConversionParser.start();

            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            System.out.println("Loaded");
            Writer writer = new Writer();
            writer.write(parseResult, null);

        }
    }

}
