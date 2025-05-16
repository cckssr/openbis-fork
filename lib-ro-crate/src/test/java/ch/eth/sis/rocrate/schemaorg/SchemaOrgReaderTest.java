package ch.eth.sis.rocrate.schemaorg;

import ch.eth.sis.rocrate.facade.IPropertyType;
import ch.eth.sis.rocrate.facade.IType;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.List;

public class SchemaOrgReaderTest extends TestCase
{

    @Test
    public void testReading()
    {
        String path = "/home/meiandr/Downloads/schema.org/schemaorg-all-https.ttl";
        SchemaOrgInformation information = SchemaOrgReader.read(path);

        IType person = information.getIdentifiersToDataTypes().get("schema:Person");
        assertEquals(1, person.getOntologicalAnnotations().size());
        assertTrue(person.getSubClassOf().stream().anyMatch(x -> x.equals("schema:Thing")));
        assertEquals(1, person.getSubClassOf().size());

        IPropertyType firstNameType =
                information.getIdentifiersToPropertyTypes().get("schema:givenName");
        assertTrue(firstNameType.getRange().contains("xsd:string"));
        assertEquals(1, firstNameType.getRange().size());

        assertTrue(firstNameType.getDomain().contains(person));
        assertEquals(1, firstNameType.getDomain().size());

        {
            IPropertyType propertyType =
                    information.getIdentifiersToPropertyTypes().get("schema:isicV4");
            assertTrue(propertyType.getRange().contains("xsd:string"));
            assertEquals(3, propertyType.getDomain().size());

        }

        {
            IPropertyType propertyType =
                    information.getIdentifiersToPropertyTypes().get("schema:makesOffer");
            assertTrue(propertyType.getDomain().contains(person));
            assertTrue(propertyType.getDomain().stream()
                    .anyMatch(x -> x.getId().equals("schema:Organization")));

            assertEquals(1, propertyType.getRange().size());
            assertTrue(propertyType.getRange().stream().anyMatch(x -> x.equals("schema:Offer")));

        }

        {
            IPropertyType propertyType =
                    information.getIdentifiersToPropertyTypes().get("schema:abstract");
            assertTrue(propertyType.getDomain().stream()
                    .anyMatch(x -> x.getId().equals("schema:CreativeWork")));

            assertEquals(1, propertyType.getRange().size());
            assertTrue(propertyType.getRange().stream().anyMatch(x -> x.equals("xsd:string")));

        }

        {
            IPropertyType propertyType =
                    information.getIdentifiersToPropertyTypes().get("schema:comment");
            assertTrue(propertyType.getDomain().stream()
                    .anyMatch(x -> x.getId().equals("schema:CreativeWork")));

            assertEquals(1, propertyType.getRange().size());
            assertTrue(propertyType.getRange().stream().anyMatch(x -> x.equals("schema:Comment")));

        }
        {
            IPropertyType propertyType =
                    information.getIdentifiersToPropertyTypes().get("schema:isAccessibleForFree");
            assertTrue(propertyType.getDomain().stream()
                    .anyMatch(x -> x.getId().equals("schema:CreativeWork")));
            assertTrue(propertyType.getDomain().stream()
                    .anyMatch(x -> x.getId().equals("schema:Event")));
            assertTrue(propertyType.getDomain().stream()
                    .anyMatch(x -> x.getId().equals("schema:Place")));


            assertEquals(1, propertyType.getRange().size());
            assertTrue(propertyType.getRange().stream().anyMatch(x -> x.equals("xsd:boolean")));

        }

        {
            IPropertyType propertyType =
                    information.getIdentifiersToPropertyTypes().get("schema:commentCount");
            assertTrue(propertyType.getDomain().stream()
                    .anyMatch(x -> x.getId().equals("schema:CreativeWork")));

            assertEquals(1, propertyType.getRange().size());
            assertTrue(propertyType.getRange().stream().anyMatch(x -> x.equals("xsd:integer")));

        }
        {
            IPropertyType propertyType =
                    information.getIdentifiersToPropertyTypes().get("schema:url");
            assertTrue(propertyType.getDomain().stream()
                    .anyMatch(x -> x.getId().equals("schema:Thing")));

            assertEquals(1, propertyType.getRange().size());
            assertTrue(propertyType.getRange().stream().anyMatch(x -> x.equals("xsd:anyURI")));

        }

        {
            IType type = information.getIdentifiersToDataTypes().get("schema:MedicalProcedure");
            assertEquals(1, type.getOntologicalAnnotations().size());
            List<IType> superTypes = information.getSuperTypes().get(type);

            assertTrue(
                    type.getSubClassOf().stream().anyMatch(x -> x.equals("schema:MedicalEntity")));

            assertTrue(superTypes.stream().anyMatch(x -> x.getId().equals("schema:Thing")));
            assertTrue(superTypes.stream().anyMatch(x -> x.getId().equals("schema:MedicalEntity")));
            assertEquals(1, type.getSubClassOf().size());

        }


    }

}