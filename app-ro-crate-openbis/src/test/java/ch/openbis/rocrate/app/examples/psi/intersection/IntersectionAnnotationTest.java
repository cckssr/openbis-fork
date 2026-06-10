package ch.openbis.rocrate.app.examples.psi.intersection;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import org.junit.Test;
import org.testng.Assert;

import java.io.IOException;
import java.util.*;

public class IntersectionAnnotationTest
{
    static final String INPUT = "src/test/resources/psi";

    @Test
    public void testIntersetionAnnotations() throws IOException
    {

        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateFolderReader.readCrate(INPUT);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (

                IType type : types)

        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();

        SampleType intersectionType = (SampleType) openBisModel.getEntityTypes()
                .get(new EntityTypePermId("SCHEMA_CREATIVEWORK_SCICAT_PUBLISHEDDATA",
                        EntityKind.SAMPLE));
        SampleType sciCatType = (SampleType) openBisModel.getEntityTypes()
                .get(new EntityTypePermId("SCICAT_PUBLISHEDDATA",
                        EntityKind.SAMPLE));

        Assert.assertEquals(intersectionType.getSemanticAnnotations().size(), 1);
        Assert.assertTrue(intersectionType.getSemanticAnnotations().get(0).getPredicateAccessionId()
                .contains("CreativeWork"));
        Assert.assertTrue(Optional.ofNullable(sciCatType.getSemanticAnnotations())
                .map(List::isEmpty)
                .orElse(true));

    }

}
