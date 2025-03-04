package ch.openbis.rocrate.app.examples;

import ch.openbis.rocrate.app.parser.ExcelConversionParser;
import ch.openbis.rocrate.app.parser.results.ParseResult;
import ch.openbis.rocrate.app.writer.Writer;
import ch.openbis.rocrate.app.writer.mapping.Mapper;
import ch.openbis.rocrate.app.writer.mapping.types.MapResult;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

public class Excel2JSONTest {

    static final String INPUT = "src/test/resources/interoperability-ro-crate-0.1-publication.xlsx";
    static final String OUTPUT = "out/test/resources/";

    @Test
    public void conversionTest() throws IOException
    {
        // These classes are to create the Excel Model that uses openBIS V3 classes
        String[] paths = {INPUT};
        ExcelConversionParser excelConversionParser =
                new ExcelConversionParser(paths);
        ParseResult excelModel = excelConversionParser.start(); // <- <- Our model using only classes from openBIS V3 Official Java library

        // https://sissource.ethz.ch/sispub/ro-crate/-/tree/main/interoperability/0.1.x/lib?ref_type=heads
        Mapper mapper = new Mapper();
        MapResult rocrateModel = mapper.transform(excelModel); // <- Our model using only classes by Ro-Crate Profile Official Java library

        // Using official Ro-Crate library to generate the final Ro-Crate JSON
        Writer writer = new Writer();
        writer.write(excelModel, Path.of(OUTPUT));
    }
}
