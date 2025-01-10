package ch.eth.sis.rocrate;

import ch.eth.sis.rocrate.parser.ExcelConversionParser;
import ch.eth.sis.rocrate.parser.results.ParseResult;
import ch.eth.sis.rocrate.writer.Writer;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main
{

    private static boolean TEST_MODE = false;

    private static String TEST_FILE =
            "/home/meiandr/Documents/sissource/openbis/lib-ro-crate/reference-openbis-export/metadata.xlsx";

    public static void main(String[] args) throws IOException, ParseException
    {

        if (TEST_MODE)
        {
            Path excelInputFile = Path.of(TEST_FILE);

            InputStream inputStream = Files.newInputStream(excelInputFile);
            String[] files = new String[] { TEST_FILE };
            ExcelConversionParser
                    excelConversionParser =
                    new ExcelConversionParser(
                            files);
            ParseResult parseResult = excelConversionParser.start();

            Writer writer = new Writer();
            writer.write(parseResult, Path.of("ro_out"));
            System.exit(0);
        }

        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        cmd = parser.parse(options, args);

        ExcelConversionParser
                excelConversionParser =
                new ExcelConversionParser(
                        cmd.getOptionValues('i'));
        ParseResult parseResult = excelConversionParser.start();

        Writer writer = new Writer();
        writer.write(parseResult, Path.of(cmd.getOptionValue('o')));

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
