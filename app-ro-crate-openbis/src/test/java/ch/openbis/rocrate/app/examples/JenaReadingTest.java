package ch.openbis.rocrate.app.examples;

import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.writer.Writer;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.lang.LangJSONLD11;
import org.apache.jena.sparql.util.Context;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JenaReadingTest
{

    static final String INPUT =
            "src/test/resources/reference-from-interoperability-0.2-export.xlsx";

    static final String OUTPUT = "/tmp/jena-reading-crate";

    Map<String, String> pinnedContexts = Map.of(
            "https://schema.org/",
            "/contexts/schemaorg.jsonld"
    );

    DocumentLoader loader = new DocumentLoader()
    {
        @Override
        public Document loadDocument(URI uri, DocumentLoaderOptions options)
        {
            String resource = pinnedContexts.get(uri.toString());
            if (resource == null)
            {
                throw new IllegalArgumentException("Unexpected context: " + uri);
            }

            InputStream in = getClass().getResourceAsStream(resource);
            try
            {
                return JsonDocument.of(in);
            } catch (JsonLdError e)
            {
                throw new RuntimeException(e);
            }
        }
    };


    @Test
    public void conversionTest() throws Exception
    {
        Path path = Paths.get(INPUT);
        OpenBisModel excelModel = ExcelReader.convert(ExcelReader.Format.EXCEL, path);

        Writer writer = new Writer();
        writer.write(excelModel, Path.of(OUTPUT));

        Model model = ModelFactory.createDefaultModel();
        String manifest = OUTPUT + "/ro-crate-metadata.json";

        System.out.println("Directory exists:" + Files.exists(Path.of(OUTPUT)));
        boolean manifestExists = Files.exists(Path.of(manifest));
        System.out.println("Manifest exists exists:" + manifestExists);
        if (manifestExists)
        {
            String x = Files.readString(Path.of(manifest));
            System.out.println(x);
            InputStream stream = new ByteArrayInputStream(x.getBytes(StandardCharsets.UTF_8));
            // I could not find an assertion for do not throw but the test fails when there's an exception
            JsonLdOptions opts = new JsonLdOptions();
            opts.setDocumentLoader(loader);

            Context context = new Context();
            context.set(LangJSONLD11.JSONLD_OPTIONS, opts);

            RDFParser.create()
                    .source("src/test/resources/jena/ro-crate-context.jsonld")
                    .forceLang(Lang.JSONLD)
                    .context(context)
                    .parse(model);

        } else
        {
            assertEquals(false, true);

        }

    }

}
