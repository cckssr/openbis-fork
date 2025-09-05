package ch.eth.sis.rocrate.example.doc;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.ISchemaFacade;
import ch.eth.sis.rocrate.facade.LiteralType;
import ch.eth.sis.rocrate.facade.PropertyType;
import ch.eth.sis.rocrate.facade.Type;
import edu.kit.datamanager.ro_crate.writer.FolderWriter;

import java.util.List;
import java.util.Map;

public class QuickStart
{

    private static final String PREFIX = "";

    private static final String SEPARATOR = ":";

    public static void main(String[] args)
    {
        /* Setting up an RO-Crate with the schema facade */
        ISchemaFacade schemaFacade =
                new SchemaFacade("name", "description", "2024-12-04T07:53:11Z", "licenceIdentifier",
                        Map.of());

        Type personType = new Type();

        {
            personType.setId(PREFIX + SEPARATOR + "Person");
            personType.setOntologicalAnnotations(List.of("https://schema.org/Person"));

            {
                PropertyType personId = new PropertyType();
                personId.setId(PREFIX + SEPARATOR + "personid");
                personId.setTypes(List.of(LiteralType.STRING));
                personType.addProperty(personId);
            }
            {
                PropertyType givenName = new PropertyType();
                givenName.setId(PREFIX + SEPARATOR + "givenName");
                givenName.setOntologicalAnnotations(List.of("https://schema.org/givenName"));
                givenName.setTypes(List.of(LiteralType.STRING));
                personType.addProperty(givenName);
            }
            {
                PropertyType givenName = new PropertyType();
                givenName.setId(PREFIX + SEPARATOR + "familyName");
                givenName.setOntologicalAnnotations(List.of("https://schema.org/familyName"));
                givenName.setTypes(List.of(LiteralType.STRING));
                personType.addProperty(givenName);
            }
            {
                PropertyType identifier = new PropertyType();
                identifier.setId(PREFIX + SEPARATOR + "identifier");
                identifier.setOntologicalAnnotations(List.of("https://schema.org/identifier"));
                identifier.setTypes(List.of(LiteralType.STRING));
                personType.addProperty(identifier);
            }
            schemaFacade.addType(personType);

        }


        /* Building our Experiment type */
        {
            Type experimentType = new Type();
            experimentType.setId(PREFIX + SEPARATOR + "Experiment");

            {
                PropertyType experimentId = new PropertyType();
                experimentId.setId(PREFIX + SEPARATOR + "experimentid");
                experimentId.setTypes(List.of(LiteralType.STRING));
                experimentType.addProperty(experimentId);
            }
            {
                PropertyType creator = new PropertyType();
                creator.setId(PREFIX + SEPARATOR + "creator");
                creator.setOntologicalAnnotations(List.of("https://schema.org/creator"));
                creator.addType(personType);
                experimentType.addProperty(creator);
            }
            {
                PropertyType name = new PropertyType();
                name.setId(PREFIX + SEPARATOR + "name");
                name.setTypes(List.of(LiteralType.STRING));
                experimentType.addProperty(name);
            }
            {
                PropertyType date = new PropertyType();
                date.setId(PREFIX + SEPARATOR + "date");
                date.setTypes(List.of(LiteralType.DATETIME));
                experimentType.addProperty(date);
            }
            schemaFacade.addType(experimentType);

        }

        FolderWriter folderWriter = new FolderWriter();
        folderWriter.save(schemaFacade.getCrate(), "/tmp/example-crate");

    }

}
