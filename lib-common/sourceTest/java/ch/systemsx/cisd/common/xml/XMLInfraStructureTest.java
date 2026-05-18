/*
 * Copyright ETH 2009 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.common.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ch.systemsx.cisd.common.xml.XMLInfraStructure;

/**
 * @author Franz-Josef Elmer
 */
public class XMLInfraStructureTest extends AssertJUnit
{
    private static final class MockContentHandler extends DefaultHandler
    {
        private List<String> texts = new ArrayList<String>();

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException
        {
            String text = new String(ch, start, length).trim();
            if (text.length() > 0)
            {
                texts.add(text);
            }
        }

        @Override
        public String toString()
        {
            return texts.toString();
        }
    }

    private static final String EXAMPLE_XSD =
            "<?xml version='1.0'?>\n"
                    + "<xs:schema targetNamespace='http://my.host.org' xmlns:xs='http://www.w3.org/2001/XMLSchema'>\n"
                    + "<xs:element name='note'>\n" + "  <xs:complexType>\n" + "    <xs:sequence>\n"
                    + "      <xs:element name='to' type='xs:string'/>\n"
                    + "      <xs:element name='from' type='xs:string'/>\n"
                    + "      <xs:element name='heading' type='xs:string'/>\n"
                    + "      <xs:element name='body' type='xs:string'/>\n" + "    </xs:sequence>\n"
                    + "  </xs:complexType>\n" + "</xs:element>\n" + "</xs:schema>";

    private static final String VALID_EXAMPLE_XML = "<?xml version='1.0'?>\n" + "<n:note\n"
            + "xmlns:n='http://my.host.org'\n"
            + "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n"
            + "xsi:schemaLocation='http://my.host.org /note.xsd'>\n" + "  <to>Albert</to>\n"
            + "  <from>Isaac</from>\n" + "  <heading>Space and Time</heading>\n"
            + "  <body>New theory on space and time.</body>\n" + "</n:note>";

    private static final String INVALID_EXAMPLE_XML = "<?xml version='1.0'?>\n" + "<n:note\n"
            + "xmlns:n='http://my.host.org'\n"
            + "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n"
            + "xsi:schemaLocation='http://my.host.org /note.xsd'>\n" + "  <to>Albert</to>\n"
            + "  <from>Isaac</from>\n" + "  <heading>Space and Time</heading>\n" + "</n:note>";

    private static final String NOT_WELLFORMED_EXAMPLE_XML = "<?xml version='1.0'?>\n"
            + "<n:note\n" + "xmlns:n='http://my.host.org'\n"
            + "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n"
            + "xsi:schemaLocation='http://my.host.org /note.xsd'>\n" + "  <to>Albert</to>\n";

    private static final String EXTERNAL_RESOURCE = "http://127.0.0.1:9/xxe";

    private ContentHandler contentHandler;

    private EntityResolver entityResolver;

    @BeforeMethod
    public void beforeMethod()
    {
        contentHandler = new MockContentHandler();
        entityResolver = new EntityResolver()
            {
                @Override
                public InputSource resolveEntity(String publicId, String systemId)
                        throws SAXException, IOException
                {
                    assertEquals("file:///note.xsd", systemId);
                    return new InputSource(new StringReader(EXAMPLE_XSD));
                }
            };
    }

    @Test
    public void testCreateSchema()
    {
        XMLInfraStructure.createSchema(new ByteArrayInputStream(EXAMPLE_XSD.getBytes()));
    }

    @Test
    public void testCreateInvalidSchema()
    {
        try
        {
            XMLInfraStructure.createSchema(new ByteArrayInputStream(VALID_EXAMPLE_XML.getBytes()));
            fail("Expection expected");
        } catch (Exception ex)
        {
            // ignored
        }
    }

    @Test
    public void testSecureDocumentBuilderFactoryRejectsDoctype() throws Exception
    {
        String xml = "<!DOCTYPE root [<!ELEMENT root ANY>]><root>text</root>";

        assertDocumentBuilderParsingFails(xml);
    }

    @Test
    public void testSecureDocumentBuilderFactoryDoesNotResolveExternalGeneralEntity()
            throws Exception
    {
        String xml = "<!DOCTYPE root [<!ENTITY xxe SYSTEM '" + EXTERNAL_RESOURCE
                + "/general'>]><root>&xxe;</root>";

        assertDocumentBuilderParsingFails(xml);
    }

    @Test
    public void testSecureSaxParserFactoryDoesNotResolveExternalParameterEntity()
            throws Exception
    {
        String xml = "<!DOCTYPE root [<!ENTITY % xxe SYSTEM '" + EXTERNAL_RESOURCE
                + "/parameter'>%xxe;]><root>text</root>";

        assertSaxParsingFails(xml);
    }

    @Test
    public void testSecureSaxParserFactoryDoesNotLoadExternalDtd() throws Exception
    {
        String xml = "<!DOCTYPE root SYSTEM '" + EXTERNAL_RESOURCE
                + "/external.dtd'><root>text</root>";

        assertSaxParsingFails(xml);
    }

    @Test
    public void testSecureSchemaFactoryDoesNotLoadExternalSchema()
    {
        String schema = "<?xml version='1.0'?>"
                + "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>"
                + "<xs:include schemaLocation='" + EXTERNAL_RESOURCE + "/external.xsd'/>"
                + "</xs:schema>";

        try
        {
            XMLInfraStructure.createSchema(new ByteArrayInputStream(schema.getBytes()));
            fail("External schema must not be loaded.");
        } catch (Exception ex)
        {
            assertTrue(ex.toString(), ex.toString().contains("accessExternalSchema")
                    || ex.toString().contains("External Schema")
                    || ex.toString().contains("schema_reference"));
        }
    }

    @Test
    public void testSecureTransformerFactoryDoesNotLoadExternalStylesheet()
    {
        String xslt = "<xsl:stylesheet version='1.0' "
                + "xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
                + "<xsl:import href='" + EXTERNAL_RESOURCE + "/external.xsl'/>"
                + "<xsl:template match='/'><out>text</out></xsl:template>"
                + "</xsl:stylesheet>";

        try
        {
            TransformerFactory factory = XMLInfraStructure.createSecureTransformerFactory();
            factory.newTransformer(new StreamSource(new StringReader(xslt)));
            fail("External stylesheet must not be loaded.");
        } catch (Exception ex)
        {
            assertTrue(ex.toString(), ex.toString().contains("accessExternalStylesheet")
                    || ex.toString().contains("External Stylesheet")
                    || ex.toString().contains("Could not read stylesheet target"));
        }
    }

    @Test
    public void testSecureTransformerFactoryDoesNotResolveDocumentFunction()
            throws Exception
    {
        String xslt = "<xsl:stylesheet version='1.0' "
                + "xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
                + "<xsl:template match='/'>"
                + "<out><xsl:value-of select=\"document('" + EXTERNAL_RESOURCE
                + "/document.xml')\"/></out>"
                + "</xsl:template></xsl:stylesheet>";

        TransformerFactory factory = XMLInfraStructure.createSecureTransformerFactory();
        Transformer transformer = factory.newTransformer(new StreamSource(new StringReader(xslt)));
        try
        {
            transformer.transform(new StreamSource(new StringReader("<root/>")),
                    new StreamResult(new StringWriter()));
            fail("XSLT document() must not resolve external resources.");
        } catch (Exception ex)
        {
            assertTrue(ex.toString(), ex.toString().contains("accessExternalStylesheet")
                    || ex.toString().contains("External Stylesheet")
                    || ex.toString().contains("Could not read stylesheet target"));
        }
    }

    @Test
    public void testLegacyParserRejectsDoctypeWithoutValidation()
    {
        String xml = "<!DOCTYPE root [<!ELEMENT root ANY>]><root>text</root>";

        assertLegacyParsingFails(false, xml, "DOCTYPE");
    }

    @Test
    public void testLegacyParserRejectsDoctypeWithValidation()
    {
        String xml = "<!DOCTYPE root [<!ELEMENT root ANY>]><root>text</root>";

        assertLegacyParsingFails(true, xml, "DOCTYPE");
    }

    @Test
    public void testLegacyParserDoesNotResolveExternalSchemaWithValidation()
    {
        String xml = "<?xml version='1.0'?>"
                + "<root xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
                + "xsi:noNamespaceSchemaLocation='" + EXTERNAL_RESOURCE + "/external.xsd'>"
                + "text</root>";

        assertLegacyParsingFails(true, xml, "accessExternalSchema", "External Schema",
                "schema_reference");
    }

    @Test
    public void testNoValidation()
    {
        XMLInfraStructure xmlInfraStructure = new XMLInfraStructure(false);
        xmlInfraStructure.setEntityResolver(entityResolver);

        xmlInfraStructure.parse(new StringReader(INVALID_EXAMPLE_XML), contentHandler);
        assertEquals("[Albert, Isaac, Space and Time]", contentHandler.toString());
    }

    @Test
    public void testValidXML()
    {
        XMLInfraStructure xmlInfraStructure = new XMLInfraStructure(true);
        xmlInfraStructure.setEntityResolver(entityResolver);

        xmlInfraStructure.parse(new StringReader(VALID_EXAMPLE_XML), contentHandler);
        assertEquals("[Albert, Isaac, Space and Time, New theory on space and time.]",
                contentHandler.toString());
    }

    @Test
    public void testInvalidXML()
    {
        XMLInfraStructure xmlInfraStructure = new XMLInfraStructure(true);
        xmlInfraStructure.setEntityResolver(entityResolver);

        try
        {
            xmlInfraStructure.parse(new StringReader(INVALID_EXAMPLE_XML), contentHandler);
            fail("Exception expected");
        } catch (Exception ex)
        {
            assertTrue(ex.toString(), ex.toString().indexOf(
                    "org.xml.sax.SAXException: XML validation errors:\n"
                            + "Error in line 9 column 10:cvc-complex-type.2.4.b: "
                            + "The content of element 'n:note' is not complete. ") >= 0);
        }
    }

    @Test
    public void testNotWellFormedXML()
    {
        XMLInfraStructure xmlInfraStructure = new XMLInfraStructure(true);
        xmlInfraStructure.setEntityResolver(entityResolver);

        try
        {
            xmlInfraStructure.parse(new StringReader(NOT_WELLFORMED_EXAMPLE_XML), contentHandler);
            fail("Exception expected");
        } catch (Exception ex)
        {
            assertTrue(ex.toString().startsWith("org.xml.sax.SAXParseException"));
            assertTrue(ex.toString().contains(
                    "XML document structures must start and end within the same entity."));
        }
    }

    private void assertDocumentBuilderParsingFails(String xml) throws Exception
    {
        DocumentBuilderFactory factory = XMLInfraStructure.createSecureDocumentBuilderFactory();
        try
        {
            factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            fail("XML with external entity/DOCTYPE must not be parsed.");
        } catch (Exception ex)
        {
            assertTrue(ex.toString(), ex.toString().contains("DOCTYPE")
                    || ex.toString().contains("External Entity")
                    || ex.toString().contains("external entity"));
        }
    }

    private void assertLegacyParsingFails(boolean validating, String xml, String... expectedMessages)
    {
        XMLInfraStructure xmlInfraStructure = new XMLInfraStructure(validating);
        try
        {
            xmlInfraStructure.parse(new StringReader(xml), new MockContentHandler());
            fail("Legacy XMLInfraStructure parser must reject unsafe XML.");
        } catch (Exception ex)
        {
            for (String expectedMessage : expectedMessages)
            {
                if (ex.toString().contains(expectedMessage))
                {
                    return;
                }
            }
            fail("Unexpected exception message: " + ex);
        }
    }

    private void assertSaxParsingFails(String xml) throws Exception
    {
        SAXParserFactory factory = XMLInfraStructure.createSecureSAXParserFactory();
        try
        {
            factory.newSAXParser().parse(new InputSource(new StringReader(xml)), new DefaultHandler());
            fail("XML with external entity/DOCTYPE must not be parsed.");
        } catch (Exception ex)
        {
            assertTrue(ex.toString(), ex.toString().contains("DOCTYPE")
                    || ex.toString().contains("External Entity")
                    || ex.toString().contains("external entity"));
        }
    }
}
