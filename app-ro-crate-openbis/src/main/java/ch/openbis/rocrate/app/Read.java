package ch.openbis.rocrate.app;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.openbis.rocrate.app.reader.RdfToModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;

import java.util.ArrayList;
import java.util.List;

public class Read
{

    private final static String TEST_DIR =
            "ro_out";

    public static void main(String[] args) throws JsonProcessingException
    {
        String path = args.length >= 1 ? args[0] : TEST_DIR;
        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateFolderReader.readCrate(path);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);
        schemaFacade.getTypes().forEach(
                x -> System.out.println("RDFS Class " + x.getId())
        );
        schemaFacade.getPropertyTypes().forEach(
                x -> System.out.println("RDFS Property " + x.getId())
        );
        schemaFacade.getEntries(schemaFacade.getTypes().get(0).getId()).forEach(
                x -> System.out.println("Metadata entry " + x.getId())
        );
        var types = schemaFacade.getTypes();

        List<IMetadataEntry> entryList = new ArrayList<>();
        for (var type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        RdfToModel.convert(types, schemaFacade.getPropertyTypes(), entryList);


    }
}
