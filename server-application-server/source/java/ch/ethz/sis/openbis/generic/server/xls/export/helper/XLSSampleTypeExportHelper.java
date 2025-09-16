/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.xls.export.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.server.xls.export.Attribute;
import ch.ethz.sis.openbis.generic.server.xls.export.ExportableKind;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import static ch.ethz.sis.openbis.generic.server.xls.export.Attribute.*;

public class XLSSampleTypeExportHelper extends AbstractXLSEntityTypeExportHelper<SampleType>
{

    public XLSSampleTypeExportHelper(final Workbook wb)
    {
        super(wb);
    }

    @Override
    public SampleType getEntityType(final IApplicationServerApi api, final String sessionToken, final String permId)
    {
        final SampleTypeFetchOptions fetchOptions = new SampleTypeFetchOptions();
        configureFetchOptions(fetchOptions);
        final Map<IEntityTypeId, SampleType> sampleTypes = api.getSampleTypes(sessionToken,
                Collections.singletonList(new EntityTypePermId(permId, EntityKind.SAMPLE)), fetchOptions);

        assert sampleTypes.size() <= 1;

        final Iterator<SampleType> iterator = sampleTypes.values().iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    static void configureFetchOptions(final SampleTypeFetchOptions fetchOptions)
    {
        fetchOptions.withValidationPlugin().withScript();
        final TypeGroupAssignmentFetchOptions typeGroupAssignmentFetchOptions = fetchOptions.withTypeGroupAssignments();
        typeGroupAssignmentFetchOptions.withTypeGroup();
        final PropertyAssignmentFetchOptions propertyAssignmentFetchOptions = fetchOptions.withPropertyAssignments();
        final PropertyTypeFetchOptions propertyTypeFetchOptions = propertyAssignmentFetchOptions.withPropertyType();
        propertyTypeFetchOptions.withVocabulary();
        propertyTypeFetchOptions.withSampleType();
        propertyTypeFetchOptions.withMaterialType();
        propertyAssignmentFetchOptions.withPlugin().withScript();
    }

    @Override
    protected Attribute[] getAttributes(final SampleType sampleType)
    {
        return new Attribute[] { CODE, INTERNAL, DESCRIPTION, AUTO_GENERATE_CODES, VALIDATION_SCRIPT,
                GENERATED_CODE_PREFIX, UNIQUE_SUBCODES, MODIFICATION_DATE, ONTOLOGY_ID,
                ONTOLOGY_ANNOTATION_ID, ONTOLOGY_VERSION, TYPE_GROUP };
    }

    @Override
    protected String getAttributeValue(IApplicationServerApi api, String sessionToken,
            final SampleType sampleType, final Attribute attribute)
    {
        switch (attribute)
        {
            case CODE:
            {
                return sampleType.getCode();
            }
            case INTERNAL:
            {
                return sampleType.isManagedInternally().toString().toUpperCase();
            }
            case DESCRIPTION:
            {
                return sampleType.getDescription();
            }
            case VALIDATION_SCRIPT:
            {
                final Plugin validationPlugin = sampleType.getValidationPlugin();
                return validationPlugin != null
                        ? (validationPlugin.getName() != null ? validationPlugin.getName() + ".py" : "") : "";

            }
            case GENERATED_CODE_PREFIX:
            {
                return sampleType.getGeneratedCodePrefix();
            }
            case AUTO_GENERATE_CODES:
            {
                return sampleType.isAutoGeneratedCode().toString().toUpperCase();
            }
            case UNIQUE_SUBCODES:
            {
                return sampleType.isSubcodeUnique().toString().toUpperCase();
            }
            case MODIFICATION_DATE:
            {
                return DATE_FORMAT.format(sampleType.getModificationDate());
            }
            case ONTOLOGY_ID:
            {
                return getSemanticAnnotationSearchResult(api, sessionToken, EntityKind.SAMPLE,
                        sampleType.getCode(), null).stream()
                        .map(SemanticAnnotation::getPredicateOntologyId).collect(
                                Collectors.joining("\n"));
            }
            case ONTOLOGY_VERSION:
            {
                return getSemanticAnnotationSearchResult(api, sessionToken, EntityKind.SAMPLE,
                        sampleType.getCode(), null).stream()
                        .map(SemanticAnnotation::getPredicateOntologyVersion).collect(
                                Collectors.joining("\n"));
            }
            case ONTOLOGY_ANNOTATION_ID:
            {
                return getSemanticAnnotationSearchResult(api, sessionToken, EntityKind.SAMPLE,
                        sampleType.getCode(), null).stream()
                        .map(SemanticAnnotation::getPredicateAccessionId).collect(
                                Collectors.joining("\n"));
            }
            case TYPE_GROUP:
            {
                return sampleType.getTypeGroupAssignments().stream()
                        .map(x -> x.getTypeGroup().getCode())
                        .collect(Collectors.joining("\n"));
            }
            default:
            {
                return null;
            }
        }
    }

    @Override
    protected ExportableKind getExportableKind()
    {
        return ExportableKind.SAMPLE_TYPE;
    }

    @Override
    protected EntityKind getEntityKind()
    {
        return EntityKind.SAMPLE;
    }

}
