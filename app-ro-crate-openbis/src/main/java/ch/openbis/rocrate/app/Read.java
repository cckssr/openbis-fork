package ch.openbis.rocrate.app;

import ch.eth.sis.rocrate.SchemaFacade;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;

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
        System.out.println();

    }
}
