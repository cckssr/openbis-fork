/*
 * Copyright ETH 2026 Zürich, Scientific IT Services
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.StringReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupCreation;
import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.translator.PrefixBasedNameTranslator;

public class MasterDataParserTypeGroupsTest
{
    @Test
    public void testAbsentTypeGroupsElementYieldsEmptyMap() throws Exception
    {
        MasterDataParser parser = parse("<xmd:masterData/>");

        assertTrue(parser.getTypeGroups().isEmpty());
    }

    @Test
    public void testParsesGroupAttributesAndMetaData() throws Exception
    {
        String typeGroups = "<xmd:typeGroups><xmd:typeGroup code='TG' managedInternally='false' registrator='user'>"
                + "<xmd:metaData><xmd:entry key='a'>b</xmd:entry></xmd:metaData>"
                + "</xmd:typeGroup></xmd:typeGroups>";
        MasterDataParser parser = parse("<xmd:masterData>" + typeGroups + "</xmd:masterData>");

        TypeGroupCreation typeGroup = parser.getTypeGroups().get("SRC_TG");
        assertEquals(typeGroup.getCode(), "SRC_TG");
        assertEquals(typeGroup.isManagedInternally(), false);
        assertEquals(typeGroup.getMetaData(), Map.of("a", "b"));
    }

    @Test
    public void testParsesInternalManagedGroup() throws Exception
    {
        String typeGroups = "<xmd:typeGroups><xmd:typeGroup code='TG_INTERNAL' managedInternally='true' "
                + "registrator='system'/></xmd:typeGroups>";
        MasterDataParser parser = parse("<xmd:masterData>" + typeGroups + "</xmd:masterData>");

        TypeGroupCreation typeGroup = parser.getTypeGroups().get("SRC_TG_INTERNAL");
        assertEquals(typeGroup.getCode(), "SRC_TG_INTERNAL");
        assertEquals(typeGroup.isManagedInternally(), true);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRejectsDuplicateTypeGroupCode() throws Exception
    {
        String typeGroups = "<xmd:typeGroups>"
                + "<xmd:typeGroup code='TG' managedInternally='false' registrator='user'/>"
                + "<xmd:typeGroup code='TG' managedInternally='false' registrator='user'/>"
                + "</xmd:typeGroups>";
        parse("<xmd:masterData>" + typeGroups + "</xmd:masterData>");
    }

    @Test
    public void testGroupWithoutAssignmentsElementHasNoAssignmentsEntry() throws Exception
    {
        String typeGroups = "<xmd:typeGroups><xmd:typeGroup code='TG' managedInternally='false' "
                + "registrator='user'/></xmd:typeGroups>";
        MasterDataParser parser = parse("<xmd:masterData>" + typeGroups + "</xmd:masterData>");

        assertFalse(parser.getTypeGroupAssignments().containsKey("SRC_TG"));
    }

    @Test
    public void testParsesMemberObjectTypeCodes() throws Exception
    {
        String typeGroups = "<xmd:typeGroups><xmd:typeGroup code='TG' managedInternally='false' registrator='user'>"
                + "<xmd:typeGroupAssignments>"
                + "<xmd:typeGroupAssignment objectTypeCode='T1' managedInternally='false'/>"
                + "<xmd:typeGroupAssignment objectTypeCode='T2' managedInternally='true'/>"
                + "</xmd:typeGroupAssignments>"
                + "</xmd:typeGroup></xmd:typeGroups>";
        MasterDataParser parser = parse("<xmd:masterData>" + typeGroups + "</xmd:masterData>");

        List<TypeGroupAssignmentCreation> assignments = parser.getTypeGroupAssignments().get("SRC_TG");
        assertEquals(assignments.size(), 2);

        TypeGroupAssignmentCreation first = assignments.get(0);
        assertEquals(((EntityTypePermId) first.getSampleTypeId()).getPermId(), "SRC_T1");
        assertEquals(first.getTypeGroupId().toString(), "SRC_TG");
        assertEquals(first.isManagedInternally(), false);

        TypeGroupAssignmentCreation second = assignments.get(1);
        assertEquals(((EntityTypePermId) second.getSampleTypeId()).getPermId(), "SRC_T2");
        assertEquals(second.isManagedInternally(), true);
    }

    private MasterDataParser parse(String masterDataElement) throws Exception
    {
        String xml = "<urlset xmlns='http://www.sitemaps.org/schemas/sitemap/0.9' xmlns:xmd='urn:xmd'>"
                + "<url><loc>master</loc>" + masterDataElement + "</url></urlset>";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext()
        {
            public String getNamespaceURI(String prefix)
            {
                return "s".equals(prefix) ? "http://www.sitemaps.org/schemas/sitemap/0.9" : "";
            }

            public String getPrefix(String uri)
            {
                return null;
            }

            public Iterator<String> getPrefixes(String uri)
            {
                return Collections.emptyIterator();
            }
        });
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        MasterDataParser parser = MasterDataParser.create(new PrefixBasedNameTranslator("SRC"));
        parser.parseMasterData(document, xpath, "master");
        return parser;
    }
}
