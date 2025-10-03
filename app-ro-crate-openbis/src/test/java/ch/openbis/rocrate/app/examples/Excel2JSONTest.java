package ch.openbis.rocrate.app.examples;

import ch.eth.sis.rocrate.facade.MetadataEntry;
import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.writer.Writer;
import ch.openbis.rocrate.app.writer.mapping.Mapper;
import ch.openbis.rocrate.app.writer.mapping.types.MapResult;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.stream.Collectors;

public class Excel2JSONTest {

    static final String INPUT =
            "src/test/resources/reference-from-interoperability-0.2-export.xlsx";
    static final String OUTPUT = "out/test/resources/";

    @Test
    public void conversionTest() throws IOException
    {
        // These classes are to create the Excel Model that uses openBIS V3 classes
        Path path = Paths.get(INPUT);
        OpenBisModel excelModel = ExcelReader.convert(ExcelReader.Format.EXCEL, path);
        // https://sissource.ethz.ch/sispub/ro-crate/-/tree/main/interoperability/0.1.x/lib?ref_type=heads
        Mapper mapper = new Mapper();
        MapResult rocrateModel = mapper.transform(excelModel); // <- Our model using only classes by Ro-Crate Profile Official Java library

        MetadataEntry metadataEntry = rocrateModel.getMetaDataEntries().stream()
                .filter(x -> x.getId().equals("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB25")).collect(
                        Collectors.toList()).stream().findFirst().orElseThrow();
        Serializable[] timeStamp =
                (Serializable[]) metadataEntry.getValues()
                        .get("openBIS:hasPUBLICATION.PUBLICATION_YEAR");

        Timestamp ts = Timestamp.from(Instant.parse(timeStamp[0].toString()));

        // Using official Ro-Crate library to generate the final Ro-Crate JSON
        Writer writer = new Writer();
        writer.write(excelModel, Path.of(OUTPUT));
    }
}
