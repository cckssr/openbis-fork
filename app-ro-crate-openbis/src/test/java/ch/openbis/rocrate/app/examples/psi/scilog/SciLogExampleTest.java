package ch.openbis.rocrate.app.examples.psi.scilog;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.reader.ZipReader;
import org.junit.Test;
import org.testng.Assert;

import java.io.IOException;
import java.util.*;

public class SciLogExampleTest
{

    static final String INPUT = "src/test/resources/psi/scilog/test.eln";

    @Test
    public void testReading() throws IOException
    {
        RoCrateReader roCrateFolderReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateFolderReader.readCrate(INPUT);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);
        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();

        entryList.addAll(schemaFacade.getAllEntries());

        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();

        Assert.assertEquals(openBisModel.getEntities().size(), 5);
        List<Sample> samples =
                openBisModel.getEntities().values().stream().filter(x -> x instanceof Sample)
                        .map(Sample.class::cast)
                        .toList();

        Assert.assertTrue(openBisModel.getEntityTypes()
                .containsKey(new EntityTypePermId("SCHEMA_PERSON", EntityKind.SAMPLE)));

        Assert.assertTrue(openBisModel.getEntityTypes()
                .containsKey(new EntityTypePermId("BOOK_DATASET", EntityKind.SAMPLE)));

        Assert.assertTrue(samples.stream().anyMatch(
                x -> x.getCode().toLowerCase(Locale.ROOT).contains("andreas") && x.getCode()
                        .toLowerCase(
                                Locale.ROOT).contains("meier")));
    }

}
