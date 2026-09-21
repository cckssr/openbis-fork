/*
 * Copyright ETH 2020 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer;

import static org.testng.Assert.*;

import java.io.StringReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.translator.PrefixBasedNameTranslator;

public class MasterDataParserSchemaFeaturesTest
{
    @DataProvider
    public Object[][] values()
    {
        return new Object[][] { { true }, { false } };
    }

    @Test(dataProvider = "values")
    public void testExplicitFields(boolean value) throws Exception
    {
        String metadata = "<xmd:metaData><xmd:entry key='a&amp;b'>Grüezi &lt;xml&gt;\nline</xmd:entry>"
                + "<xmd:entry key='empty'></xmd:entry></xmd:metaData>";
        MasterDataParser parser = parse("multiValue='" + value + "'", "unique='" + value + "'", metadata);
        Map<String, String> expected = Map.of("a&b", "Grüezi <xml>\nline", "empty", "");
        assertEquals(parser.getPropertyTypes().get("SRC_P").isMultiValue(), value);
        assertEquals(parser.getEntityPropertyAssignments().get("SAMPLE", "SRC_T").get(0).isUnique(), value);
        assertEquals(parser.getSampleTypes().get("SRC_T").getMetaData(), expected);
        assertEquals(parser.getExperimentTypes().get("SRC_T").getMetaData(), expected);
        assertEquals(parser.getDataSetTypes().get("SRC_T").getMetaData(), expected);
        MasterDataSchemaFeatures features = parser.getSchemaFeatures();
        assertEquals(features.getPropertyTypeMetaData().get("SRC_P"), expected);
        assertEquals(features.getMultiValue().get("SRC_P"), Boolean.valueOf(value));
        for (String kind : new String[] { "SAMPLE", "EXPERIMENT", "DATA_SET" })
        {
            assertEquals(features.getEntityTypeMetaData().get(kind, "SRC_T"), expected);
            assertEquals(features.getUnique().get(kind, "SRC_T", "SRC_P"), Boolean.valueOf(value));
        }
    }

    @Test
    public void testOlderXmlOmitsFields() throws Exception
    {
        MasterDataParser parser = parse("", "", "");
        MasterDataSchemaFeatures features = parser.getSchemaFeatures();
        assertTrue(features.getPropertyTypeMetaData().isEmpty());
        assertTrue(features.getEntityTypeMetaData().isEmpty());
        assertTrue(features.getMultiValue().isEmpty());
        assertTrue(features.getUnique().isEmpty());
        assertFalse(parser.getPropertyTypes().get("SRC_P").isMultiValue());
        assertFalse(parser.getEntityPropertyAssignments().get("SAMPLE", "SRC_T").get(0).isUnique());
    }

    @Test
    public void testExplicitEmptyMetadata() throws Exception
    {
        MasterDataParser parser = parse("", "", "<xmd:metaData/>");
        assertEquals(parser.getSchemaFeatures().getPropertyTypeMetaData().get("SRC_P"), Collections.emptyMap());
        for (String kind : new String[] { "SAMPLE", "EXPERIMENT", "DATA_SET" })
        {
            assertEquals(parser.getSchemaFeatures().getEntityTypeMetaData().get(kind, "SRC_T"), Collections.emptyMap());
        }
    }

    @DataProvider
    public Object[][] invalidFields()
    {
        return new Object[][] {
                { "multiValue='invalid'", "", "" },
                { "", "unique='invalid'", "" },
                { "", "", "<xmd:metaData/><xmd:metaData/>" },
                { "", "", "<xmd:metaData><xmd:entry/></xmd:metaData>" },
                { "", "", "<xmd:metaData><xmd:entry key='a'/><xmd:entry key='a'/></xmd:metaData>" }
        };
    }

    @Test(dataProvider = "invalidFields", expectedExceptions = XPathExpressionException.class)
    public void testRejectsMalformedFields(String multiValue, String unique, String metadata) throws Exception
    {
        parse(multiValue, unique, metadata);
    }

    @Test
    public void testInternalNamespacePropertyTypeCodeIsNotPrefixed() throws Exception
    {
        MasterDataParser parser = parse("", "", "", "$STORAGE_POSITION");
        assertTrue(parser.getPropertyTypes().containsKey("$STORAGE_POSITION"));
        assertFalse(parser.getPropertyTypes().containsKey("SRC_STORAGE_POSITION"));
    }

    private MasterDataParser parse(String multiValue, String unique, String metadata) throws Exception
    {
        return parse(multiValue, unique, metadata, "P");
    }

    private MasterDataParser parse(String multiValue, String unique, String metadata, String propertyTypeCode) throws Exception
    {
        String assignment = "<xmd:propertyAssignments><xmd:propertyAssignment propertyTypeCode='" + propertyTypeCode + "' ordinal='1' "
                + unique + "/></xmd:propertyAssignments>";
        String xml = "<urlset xmlns='http://www.sitemaps.org/schemas/sitemap/0.9' xmlns:xmd='urn:xmd'>"
                + "<url><loc>master</loc><xmd:masterData><xmd:propertyTypes>"
                + "<xmd:propertyType code='" + propertyTypeCode + "' dataType='VARCHAR' managedInternally='false' registrator='user' "
                + multiValue + ">" + metadata + "</xmd:propertyType></xmd:propertyTypes>"
                + "<xmd:objectTypes><xmd:objectType code='T'>" + metadata + assignment
                + "</xmd:objectType></xmd:objectTypes>"
                + "<xmd:collectionTypes><xmd:collectionType code='T'>" + metadata + assignment
                + "</xmd:collectionType></xmd:collectionTypes>"
                + "<xmd:dataSetTypes><xmd:dataSetType code='T'>" + metadata + assignment
                + "</xmd:dataSetType></xmd:dataSetTypes></xmd:masterData></url></urlset>";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext()
        {
            public String getNamespaceURI(String prefix)
            {
                return "s".equals(prefix) ? "http://www.sitemaps.org/schemas/sitemap/0.9" : "";
            }
            public String getPrefix(String uri) { return null; }
            public Iterator<String> getPrefixes(String uri) { return Collections.emptyIterator(); }
        });
        MasterDataParser parser = MasterDataParser.create(new PrefixBasedNameTranslator("SRC"));
        parser.parseMasterData(factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml))), xpath, "master");
        return parser;
    }
}
