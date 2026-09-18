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

import static ch.systemsx.cisd.openbis.generic.shared.basic.BasicConstant.INTERNAL_NAMESPACE_PREFIX;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.systemsx.cisd.openbis.generic.shared.basic.CodeConverter;

/**
 * Delivers Object Type Groups (an openBIS 7 feature, absent from 20.10) as part of the master-data XML written by
 * {@link MasterDataDeliverer}.
 */
class TypeGroupDeliverer extends AbstractEntityDeliverer<Object>
{
    TypeGroupDeliverer(DeliveryContext context)
    {
        super(context, "type group");
    }

    void writeTypeGroups(DeliveryExecutionContext executionContext, XMLStreamWriter writer) throws XMLStreamException
    {
        List<TypeGroup> typeGroups = fetchTypeGroups(executionContext);
        if (typeGroups.isEmpty())
        {
            return;
        }
        Set<String> sampleTypeCodesInScope = new HashSet<>(executionContext.getPermIds(ExportableKind.SAMPLE_TYPE));
        writer.writeStartElement("xmd:typeGroups");
        for (TypeGroup typeGroup : typeGroups)
        {
            writeTypeGroup(writer, typeGroup, sampleTypeCodesInScope);
        }
        writer.writeEndElement();
    }

    private List<TypeGroup> fetchTypeGroups(DeliveryExecutionContext executionContext)
    {
        List<String> permIds = executionContext.getPermIds(ExportableKind.TYPE_GROUP);
        if (permIds.isEmpty())
        {
            return new ArrayList<>();
        }
        List<TypeGroupId> ids = permIds.stream().map(TypeGroupId::new).collect(Collectors.toList());
        TypeGroupFetchOptions fetchOptions = new TypeGroupFetchOptions();
        fetchOptions.withRegistrator();
        fetchOptions.withTypeGroupAssignments().withSampleType();
        return new ArrayList<>(getV3Api().getTypeGroups(executionContext.getSessionToken(), ids, fetchOptions).values());
    }

    private void writeTypeGroup(XMLStreamWriter writer, TypeGroup typeGroup, Set<String> sampleTypeCodesInScope) throws XMLStreamException
    {
        writer.writeStartElement("xmd:typeGroup");
        boolean managedInternally = Boolean.TRUE.equals(typeGroup.isManagedInternally());
        String code = managedInternally && typeGroup.getCode().startsWith(INTERNAL_NAMESPACE_PREFIX)
                ? CodeConverter.tryToDatabase(typeGroup.getCode())
                : typeGroup.getCode();
        addAttribute(writer, "code", code);
        addAttribute(writer, "managedInternally", managedInternally);
        addRegistrator(writer, typeGroup);
        addRegistrationDate(writer, typeGroup);
        addAttribute(writer, "modification-timestamp", typeGroup.getModificationDate(), h -> DataSourceUtils.convertToW3CDate(h));
        DataSourceUtils.addMetaData(writer, typeGroup.getMetaData());
        writeTypeGroupAssignments(writer, typeGroup, sampleTypeCodesInScope);
        writer.writeEndElement();
    }

    private void writeTypeGroupAssignments(XMLStreamWriter writer, TypeGroup typeGroup, Set<String> sampleTypeCodesInScope)
            throws XMLStreamException
    {
        List<TypeGroupAssignment> assignmentsInScope = typeGroup.getTypeGroupAssignments().stream()
                .filter(assignment -> sampleTypeCodesInScope.contains(assignment.getSampleType().getCode()))
                .collect(Collectors.toList());
        if (assignmentsInScope.isEmpty())
        {
            return;
        }
        writer.writeStartElement("xmd:typeGroupAssignments");
        for (TypeGroupAssignment assignment : assignmentsInScope)
        {
            writer.writeStartElement("xmd:typeGroupAssignment");
            addAttribute(writer, "objectTypeCode", assignment.getSampleType().getCode());
            addAttribute(writer, "managedInternally", assignment.isManagedInternally());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}
