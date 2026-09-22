package ch.openbis.rocrate.app.examples.spacetypecollision;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.writer.Writer;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.reader.ZipReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SpaceTypeCollisionTest
{

    static final String INPUT =
            "src/test/resources/examples/small-example-collision-storage-space-entity-type.zip";

    static final String OUTPUT = "/tmp/ro-crate-collision.zip";

    /**
     * The important thing here is that there are no exceptions related to restrictions.
     *
     * @throws IOException
     */
    @Test
    public void testNoCollision() throws Exception
    {
        Path path = Paths.get(INPUT);
        OpenBisModel excelModel = ExcelReader.convert(ExcelReader.Format.EXCEL, path);
        Writer writer = new Writer();
        writer.write(excelModel, Path.of(OUTPUT));

        RoCrateReader roCrateReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateReader.readCrate(OUTPUT);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (IType type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();
        Assert.assertTrue(openBisModel.getEntityTypes()
                .containsKey(new EntityTypePermId("STORAGE", EntityKind.SAMPLE)));
        Assert.assertTrue(openBisModel.getSpaces().containsKey(new SpacePermId("STORAGE")));

    }

}
