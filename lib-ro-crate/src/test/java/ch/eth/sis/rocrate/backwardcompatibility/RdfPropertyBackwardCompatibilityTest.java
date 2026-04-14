package ch.eth.sis.rocrate.backwardcompatibility;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IPropertyType;
import ch.eth.sis.rocrate.facade.IType;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import junit.framework.TestCase;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RdfPropertyBackwardCompatibilityTest extends TestCase
{

    public static final String INPUT_NEW =
            "src/test/resources/ch/eth/sis/rocrate/property/backwardcompatibility/rdfProperty";

    public static final String INPUT_OLD =
            "src/test/resources/ch/eth/sis/rocrate/property/backwardcompatibility/rdfProperty";

    public void testBackwardCompatibility() throws JsonProcessingException
    {
        RoCrateReader roCrateReader = new RoCrateReader(new FolderReader());
        RoCrate crateNew = roCrateReader.readCrate(INPUT_NEW);
        RoCrate crateOld = roCrateReader.readCrate(INPUT_OLD);
        SchemaFacade schemaFacadeOld = SchemaFacade.of(crateOld);
        SchemaFacade schemaFacadeNew = SchemaFacade.of(crateNew);

        List<IType> typesOld = schemaFacadeOld.getTypes();
        List<IType> typesNew = schemaFacadeNew.getTypes();

        System.out.println("lol");

        Map<String, IPropertyType> oldIdTopropertyType = schemaFacadeOld.getPropertyTypes().stream()
                .collect(Collectors.toMap(x -> x.getId(), Function.identity()));

        for (IPropertyType propertyTypeNew : schemaFacadeNew.getPropertyTypes())
        {
            assertTrue(oldIdTopropertyType.containsKey(propertyTypeNew.getId()));

        }

    }

}
