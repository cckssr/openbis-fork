package ch.openbis.rocrate.app.examples.openbis.nameproperty;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.reader.ZipReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NamePropertyTest
{

    @Test
    public void testName() throws IOException
    {
        String location =
                "src/test/resources/examples/openbis.name/fancy_name.result-crate.2026-07-02-15-30-15-892.zip";
        RoCrateReader roCrateFolderReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateFolderReader.readCrate(location);
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
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();

        Sample sample = (Sample) openBisModel.getEntities()
                .get(new SampleIdentifier("/DEFAULT/DEFAULT/FANCY_ENTRY1"));

        Assert.assertEquals("SOME_FANCY_ENTRY", sample.getProperties().get("NAME"));
        Assert.assertEquals("<html>\n" +
                " <head></head>\n" +
                " <body>\n" +
                "  <h2>SOME_FANCY_ENTRY</h2>\n" +
                "  <p>new content</p>\n" +
                " </body>\n" +
                "</html>", sample.getProperties().get("DOCUMENT"));

    }

}
