package ch.openbis.rocrate.app.examples.psi.molecules;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.reader.ZipReader;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class MoleculeRoCrateTest
{

    static final String INPUT = "src/test/resources/psi/molecules/ro-crate-out.zip";

    public static final String IDENTIFIER_MOLECULE = "/MATERIALS/MOLECULES/MOLE151";

    @Test
    public void testMoleculeRoCrate() throws JsonProcessingException
    {
        RoCrateReader roCrateFolderReader = new RoCrateReader(new ZipReader());
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

        assertEquals(2, openBisModel.getEntityTypes().size());
        assertTrue(openBisModel.getEntities()
                .containsKey(new SampleIdentifier(IDENTIFIER_MOLECULE)));
        AbstractEntityPropertyHolder abstractEntityPropertyHolder =
                openBisModel.getEntities().get(new SampleIdentifier(IDENTIFIER_MOLECULE));
        Sample sample = (Sample) abstractEntityPropertyHolder;
        assertEquals("MATERIALS", sample.getSpace().getCode());
        assertEquals("/MATERIALS/MOLECULES", sample.getProject().getIdentifier().getIdentifier());
        assertNotNull(sample.getExperiment());
        assertEquals("MOLECULE_COLLECTION", sample.getExperiment().getCode());


    }
}
