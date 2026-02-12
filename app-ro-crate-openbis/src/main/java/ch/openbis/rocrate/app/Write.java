package ch.openbis.rocrate.app;

import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.writer.Writer;
import org.apache.commons.cli.*;

import java.nio.file.Path;

public class Write
{

    public static void main(String[] args) throws Exception
    {



        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        cmd = parser.parse(options, args);
        boolean dummy = cmd.hasOption("dummy");
        ExcelReader.FileMode fileMode =
                dummy ? ExcelReader.FileMode.DUMMY : ExcelReader.FileMode.FULL;


        Path path = Path.of(cmd.getOptionValues('i')[0]);
        OpenBisModel openBisModel =
                ExcelReader.convert(ExcelReader.Format.EXCEL, path, fileMode);


        Writer writer = new Writer();
        writer.write(openBisModel, Path.of(cmd.getOptionValue('o')));

    }

    private static Options createOptions()
    {
        Options options = new Options();

        {
            Option option = Option.builder("i")
                    .longOpt("input")
                    .hasArgs()
                    .required()
                    .desc("Provide an input file in form of an openBIS excel sheet")
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

        {
            Option dummy = new Option("d", "dummy", false,
                    "Set files to 1 B dummy files for testint puropses");
            options.addOption(dummy);
        }

        return options;

    }

}
