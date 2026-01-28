package ch.openbis.rocrate.app;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;
import ch.openbis.rocrate.app.reader.RdfToModel;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.reader.ZipReader;
import org.apache.commons.cli.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Read
{

    private final static String TEST_DIR =
            "/home/meiandr/Documents/sissource/openbis/build/ro_out_3";

    public static void main(String[] args) throws IOException, ParseException
    {

        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        cmd = parser.parse(options, args);

        String path = cmd.getOptionValue('i');
        RoCrateReader roCrateReader;
        if (path.endsWith(".zip"))
        {
            roCrateReader = new RoCrateReader(new ZipReader());
        } else
        {
            roCrateReader = new RoCrateReader(new FolderReader());
        }

        RoCrate crate = roCrateReader.readCrate(path);
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
        Set<DataEntity> allDataEntities = crate.getAllDataEntities();


        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(), entryList, "DEFAULT",
                        "DEFAULT", schemaFacade);
        byte[] writtenStuff = ExcelWriter.convert(ExcelWriter.Format.ZIP_EXPORT, openBisModel);
        String outPath = cmd.getOptionValue('o');
        try (FileOutputStream byteArrayOutputStream = new FileOutputStream(
                outPath))
        {
            byteArrayOutputStream.write(writtenStuff);
        }

    }

    private static Options createOptions()
    {
        Options options = new Options();

        {
            Option option = Option.builder("i")
                    .longOpt("input")
                    .hasArgs()
                    .required()
                    .desc("Provide an input file in form of an RO-Crate")
                    .build();
            options.addOption(option);

        }
        {
            Option option = Option.builder("o")
                    .longOpt("output")
                    .numberOfArgs(1)
                    .required()
                    .desc("Provide output path")
                    .build();
            options.addOption(option);

        }

        return options;

    }
}
