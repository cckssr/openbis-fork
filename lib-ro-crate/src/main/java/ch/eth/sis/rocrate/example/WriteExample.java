package ch.eth.sis.rocrate.example;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.writer.FolderWriter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class WriteExample
{
    public static void main(String[] args) throws JsonProcessingException
    {
        RoCrate.RoCrateBuilder roCrateBuilder =
                new RoCrate.RoCrateBuilder("name", "description", "2024-12-04T07:53:11Z",
                        "licenseIdentifier");
        roCrateBuilder.addValuePairToContext("schema",
                "https://www.w3.org/TR/rdf-schema");
        roCrateBuilder.addValuePairToContext("owl",
                "https://www.w3.org/TR/owl-ref");
        roCrateBuilder.addValuePairToContext("openBIS", "_:");

        ISchemaFacade schemaFacade = SchemaFacade.of(roCrateBuilder.build());

        {
            Type type = new Type();
            type.setId("TextResource");
            type.setSubClassOf(List.of("https://schema.org/Thing"));
            type.setOntologicalAnnotations(
                    List.of("https://www.dublincore.org/specifications/dublin-core/dcmi-terms/dcmitype/Text/"));
            schemaFacade.addType(type);

            PropertyType property = new PropertyType();
            property.setId("hasDateSubmitted");
            property.setTypes(List.of(LiteralType.DATETIME));
            type.addProperty(property);


            property.setOntologicalAnnotations(
                    List.of("https://www.dublincore.org/specifications/dublin-core/dcmi-terms/terms/dateSubmitted/"));
            schemaFacade.addPropertyType(property);

        }
        {
            IMetadataEntry metadataEntry =
                    new MetadataEntry("TextResource1", Set.of("TextResource"),
                    Map.of("hasDate", "2025-01-21T07:12:20Z"), Map.of());
            schemaFacade.addEntry(metadataEntry);

        }

        String path = args.length >= 1 ? args[0] : "out";



        roCrateBuilder.build();

        FolderWriter folderWriter = new FolderWriter();
        folderWriter.save(roCrateBuilder.build(), path);

    }

}
