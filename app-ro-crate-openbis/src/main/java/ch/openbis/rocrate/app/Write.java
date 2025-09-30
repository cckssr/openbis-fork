package ch.openbis.rocrate.app;

import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.writer.Writer;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Path;

public class Write
{

    private static boolean TEST_MODE = false;


    public static void main(String[] args) throws IOException, ParseException
    {

        if (TEST_MODE)
        {
            Path excelInputFile =
                    Path.of("/home/meiandr/Downloads/metadata.2025-07-23-13-20-25-933edited.xlsx");
            OpenBisModel openBisModel = ExcelReader.convert(ExcelReader.Format.EXCEL, excelInputFile);

            Writer writer = new Writer();
            writer.write(openBisModel, Path.of("ro_out"));
            System.exit(0);
        }

        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        cmd = parser.parse(options, args);

        Path path = Path.of(cmd.getOptionValues('i')[0]);
        OpenBisModel openBisModel = ExcelReader.convert(ExcelReader.Format.EXCEL, path);


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

        return options;

    }

}
