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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.datasource;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;

public class TypeGroupDelivererTest
{
    private static final String SESSION_TOKEN = "test-token";

    private Mockery mockery;

    private IApplicationServerApi api;

    private TypeGroupDeliverer deliverer;

    @BeforeMethod
    public void beforeMethod()
    {
        mockery = new Mockery();
        api = mockery.mock(IApplicationServerApi.class);

        DeliveryContext context = new DeliveryContext();
        context.setServerUrl("http://test");
        context.setV3api(api);
        deliverer = new TypeGroupDeliverer(context);
    }

    @AfterMethod
    public void afterMethod()
    {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testWriteTypeGroupsWithEmptyPermIdsWritesNothingAndMakesNoApiCall() throws Exception
    {
        // no expectations set on `api` at all: a call to getTypeGroups would fail the test.
        String xml = writeTypeGroups(new EnumMap<>(ExportableKind.class));

        assertFalse(xml.contains("xmd:typeGroups"), xml);
    }

    @Test
    public void testWriteTypeGroupsWritesAttributesAndMetaData() throws Exception
    {
        Map<ExportableKind, List<String>> permIdsByKind = new EnumMap<>(ExportableKind.class);
        permIdsByKind.put(ExportableKind.TYPE_GROUP, List.of("TG_A"));

        TypeGroupId typeGroupId = new TypeGroupId("TG_A");

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("z", "");
        metadata.put("a<&\"", "Grüezi <xml> & \"quoted\"\nsecond line");

        TypeGroup typeGroup = typeGroup("TG_A", false, metadata);

        mockery.checking(new Expectations()
        {
            {
                allowing(api).getTypeGroups(with(SESSION_TOKEN), with(List.<ITypeGroupId> of(typeGroupId)),
                        with(any(TypeGroupFetchOptions.class)));
                will(returnValue(Map.of(typeGroupId, typeGroup)));
            }
        });

        String xml = writeTypeGroups(permIdsByKind);

        assertTrue(xml.contains("TG_A"), xml);

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
        Element element = (Element) document.getElementsByTagName("xmd:typeGroup").item(0);
        assertEquals(element.getAttribute("code"), "TG_A");
        assertEquals(element.getAttribute("managedInternally"), "false");
        assertEquals(element.getAttribute("registrator"), "test-user");

        Element metaDataElement = (Element) element.getElementsByTagName("xmd:metaData").item(0);
        Map<String, String> actual = new LinkedHashMap<>();
        var entries = metaDataElement.getElementsByTagName("xmd:entry");
        for (int i = 0; i < entries.getLength(); i++)
        {
            Element entry = (Element) entries.item(i);
            actual.put(entry.getAttribute("key"), entry.getTextContent());
        }
        assertEquals(actual, metadata);
    }

    @Test
    public void testWriteTypeGroupsWritesOnlyInScopeAssignments() throws Exception
    {
        Map<ExportableKind, List<String>> permIdsByKind = new EnumMap<>(ExportableKind.class);
        permIdsByKind.put(ExportableKind.TYPE_GROUP, List.of("TG_A"));
        permIdsByKind.put(ExportableKind.SAMPLE_TYPE, List.of("IN_SCOPE"));

        TypeGroupId typeGroupId = new TypeGroupId("TG_A");
        List<TypeGroupAssignment> assignments =
                List.of(assignment("IN_SCOPE", false),
                        assignment("OUT_OF_SCOPE", false));
        TypeGroup typeGroup = typeGroup("TG_A", false, Collections.emptyMap(),
                assignments);

        mockery.checking(new Expectations()
        {
            {
                allowing(api).getTypeGroups(with(SESSION_TOKEN), with(List.<ITypeGroupId> of(typeGroupId)),
                        with(any(TypeGroupFetchOptions.class)));
                will(returnValue(Map.of(typeGroupId, typeGroup)));
            }
        });

        String xml = writeTypeGroups(permIdsByKind);

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
        var assignmentElements = document.getElementsByTagName("xmd:typeGroupAssignment");
        assertEquals(assignmentElements.getLength(), 1);
        assertEquals(((Element) assignmentElements.item(0)).getAttribute("objectTypeCode"), "IN_SCOPE");
    }

    @Test
    public void testWriteTypeGroupsOmitsAssignmentsElementWhenNoneInScope() throws Exception
    {
        Map<ExportableKind, List<String>> permIdsByKind = new EnumMap<>(ExportableKind.class);
        permIdsByKind.put(ExportableKind.TYPE_GROUP, List.of("TG_A"));
        permIdsByKind.put(ExportableKind.SAMPLE_TYPE, List.of("SOME_OTHER_TYPE"));

        TypeGroupId typeGroupId = new TypeGroupId("TG_A");
        TypeGroup typeGroup = typeGroup("TG_A", false, Collections.emptyMap(),
                List.of(assignment("OUT_OF_SCOPE", false)));

        mockery.checking(new Expectations()
        {
            {
                allowing(api).getTypeGroups(with(SESSION_TOKEN), with(List.<ITypeGroupId> of(typeGroupId)),
                        with(any(TypeGroupFetchOptions.class)));
                will(returnValue(Map.of(typeGroupId, typeGroup)));
            }
        });

        String xml = writeTypeGroups(permIdsByKind);

        assertFalse(xml.contains("xmd:typeGroupAssignments"), xml);
    }

    @Test
    public void testWriteTypeGroupsTranslatesInternalCode() throws Exception
    {
        Map<ExportableKind, List<String>> permIdsByKind = new EnumMap<>(ExportableKind.class);
        permIdsByKind.put(ExportableKind.TYPE_GROUP, List.of("$TG_INTERNAL"));

        TypeGroupId typeGroupId = new TypeGroupId("$TG_INTERNAL");
        TypeGroup typeGroup = typeGroup("$TG_INTERNAL", true, Collections.emptyMap());

        mockery.checking(new Expectations()
        {
            {
                allowing(api).getTypeGroups(with(SESSION_TOKEN), with(List.<ITypeGroupId> of(typeGroupId)),
                        with(any(TypeGroupFetchOptions.class)));
                will(returnValue(Map.of(typeGroupId, typeGroup)));
            }
        });

        String xml = writeTypeGroups(permIdsByKind);

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
        Element element = (Element) document.getElementsByTagName("xmd:typeGroup").item(0);
        assertEquals(element.getAttribute("code"), "TG_INTERNAL");
        assertEquals(element.getAttribute("managedInternally"), "true");
    }

    private String writeTypeGroups(Map<ExportableKind, List<String>> permIdsByKind) throws Exception
    {
        StringWriter stringWriter = new StringWriter();
        XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(stringWriter);

        DeliveryExecutionContext executionContext = new DeliveryExecutionContext();
        executionContext.setWriter(writer);
        executionContext.setSessionToken(SESSION_TOKEN);
        executionContext.setRequestTimestamp(new Date());
        executionContext.setFileServicePaths(new HashSet<>());
        executionContext.setPermIdsByKind(permIdsByKind);

        writer.writeStartDocument();
        deliverer.writeTypeGroups(executionContext, writer);
        writer.writeEndDocument();
        writer.flush();

        return stringWriter.toString();
    }

    private static TypeGroup typeGroup(String code, boolean managedInternally, Map<String, String> metaData)
    {
        return typeGroup(code, managedInternally, metaData, Collections.emptyList());
    }

    private static TypeGroup typeGroup(String code, boolean managedInternally, Map<String, String> metaData,
            List<TypeGroupAssignment> assignments)
    {
        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        fetchOptions.withRegistrator();
        fetchOptions.withTypeGroupAssignments().withSampleType();

        Person registrator = new Person();
        registrator.setUserId("test-user");

        TypeGroup typeGroup = new TypeGroup();
        typeGroup.setFetchOptions(fetchOptions);
        typeGroup.setCode(code);
        typeGroup.setManagedInternally(managedInternally);
        typeGroup.setRegistrator(registrator);
        typeGroup.setRegistrationDate(new Date());
        typeGroup.setModificationDate(new Date());
        typeGroup.setMetaData(metaData);
        typeGroup.setTypeGroupAssignments(assignments);
        for (TypeGroupAssignment assignment : assignments)
        {
            assignment.setTypeGroup(typeGroup);
        }
        return typeGroup;
    }

    private static TypeGroupAssignment assignment(String sampleTypeCode, boolean managedInternally)
    {
        SampleType sampleType = new SampleType();
        sampleType.setCode(sampleTypeCode);

        TypeGroupAssignment assignment = new TypeGroupAssignment();
        assignment.setSampleType(sampleType);
        assignment.setManagedInternally(managedInternally);
        return assignment;
    }
}
