package ch.openbis.rocrate.app.examples.datatypes;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.writer.Writer;
import ch.openbis.rocrate.app.writer.mapping.Mapper;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.reader.ZipReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.*;

public class DateRoundTripTest
{
    static final String INPUT =
            "src/test/resources/examples/export-with-date-field.zip";

    static final String OUTPUT = "out/test/resources/datecrate.zip";

    @Test
    public void testDateRoundTrip() throws Exception
    {
        OpenBisModel openBisModel = ExcelReader.convert(ExcelReader.Format.EXCEL, Path.of(INPUT));
        TimeZone timeZone = TimeZone.getTimeZone(ZoneId.systemDefault());
        Mapper mapper = new Mapper(timeZone);
        Writer writer = new Writer();
        writer.write(openBisModel, Path.of(OUTPUT));

        RoCrateReader roCrateFolderReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateFolderReader.readCrate(OUTPUT);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (IType type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel
                openBisModel2 =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();

        PropertyAssignment propertyAssignment =
                openBisModel2.getEntityTypes().get(new EntityTypePermId("ITEM", EntityKind.SAMPLE))
                        .getPropertyAssignments().stream()
                        .filter(x -> x.getPropertyType().getCode().equals("ITEM.DATE_PREPARED"))
                        .findFirst().orElseThrow();
        Assert.assertEquals(DataType.TIMESTAMP, propertyAssignment.getPropertyType().getDataType());
        Sample sample = (Sample) openBisModel2.getEntities()
                .get(new SampleIdentifier("/MAXIMILIAN_SEURIG/MATERIALS/ITEM242"));
        Serializable val = sample.getProperties().get("ITEM.DATE_PREPARED");
        Assert.assertEquals("2022-08-29 00:00:00 +0200", val);

    }

}
