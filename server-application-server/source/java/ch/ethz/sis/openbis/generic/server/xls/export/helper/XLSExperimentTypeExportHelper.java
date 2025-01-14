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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.server.xls.export.Attribute;
import ch.ethz.sis.openbis.generic.server.xls.export.ExportableKind;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import static ch.ethz.sis.openbis.generic.server.xls.export.Attribute.*;

public class XLSExperimentTypeExportHelper extends AbstractXLSEntityTypeExportHelper<ExperimentType>
{

    public XLSExperimentTypeExportHelper(final Workbook wb)
    {
        super(wb);
    }

    @Override
    protected Attribute[] getAttributes(final ExperimentType entityType)
    {
        return new Attribute[] { CODE, INTERNAL, DESCRIPTION, VALIDATION_SCRIPT, MODIFICATION_DATE,
                ONTOLOGY_ID, ONTOLOGY_ANNOTATION_ID, ONTOLOGY_VERSION };
    }

    @Override
    protected String getAttributeValue(IApplicationServerApi api, String sessionToken,
            final ExperimentType experimentType, final Attribute attribute)
    {
        switch (attribute)
        {
            case CODE:
            {
                return experimentType.getCode();
            }
            case INTERNAL:
            {
                return experimentType.isManagedInternally().toString().toUpperCase();
            }
            case DESCRIPTION:
            {
                return experimentType.getDescription();
            }
            case VALIDATION_SCRIPT:
            {
                final Plugin validationPlugin = experimentType.getValidationPlugin();
                return validationPlugin != null
                        ? (validationPlugin.getName() != null ? validationPlugin.getName() + ".py" : "") : "";

            }
            case MODIFICATION_DATE:
            {
                return DATE_FORMAT.format(experimentType.getModificationDate());
            }
            case ONTOLOGY_ID:
            {
                return getSemanticAnnotationSearchResult(api, sessionToken,
                        EntityKind.EXPERIMENT, experimentType.getCode(), null).stream()
                        .map(x -> x.getDescriptorOntologyId()).collect(
                                Collectors.joining("\n"));
            }
            case ONTOLOGY_VERSION:
            {
                return getSemanticAnnotationSearchResult(api, sessionToken,
                        EntityKind.EXPERIMENT, experimentType.getCode(), null).stream()
                        .map(x -> x.getDescriptorOntologyVersion()).collect(
                                Collectors.joining("\n"));
            }
            case ONTOLOGY_ANNOTATION_ID:
            {
                return getSemanticAnnotationSearchResult(api, sessionToken,
                        EntityKind.EXPERIMENT, experimentType.getCode(), null).stream()
                        .map(x -> x.getPredicateAccessionId()).collect(
                                Collectors.joining("\n"));
            }

            default:
            {
                return null;
            }
        }
    }

    @Override
    public ExperimentType getEntityType(final IApplicationServerApi api, final String sessionToken,
            final String permId)
    {
        final ExperimentTypeFetchOptions fetchOptions = new ExperimentTypeFetchOptions();
        configureFetchOptions(fetchOptions);
        final Map<IEntityTypeId, ExperimentType> experimentTypes = api.getExperimentTypes(sessionToken,
                Collections.singletonList(new EntityTypePermId(permId, EntityKind.EXPERIMENT)), fetchOptions);

        assert experimentTypes.size() <= 1;

        final Iterator<ExperimentType> iterator = experimentTypes.values().iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    static void configureFetchOptions(final ExperimentTypeFetchOptions fetchOptions)
    {
        fetchOptions.withValidationPlugin().withScript();
        final PropertyAssignmentFetchOptions propertyAssignmentFetchOptions = fetchOptions.withPropertyAssignments();
        propertyAssignmentFetchOptions.withPropertyType().withVocabulary();
        propertyAssignmentFetchOptions.withPropertyType().withSampleType();
        propertyAssignmentFetchOptions.withPropertyType().withMaterialType();
        propertyAssignmentFetchOptions.withPlugin().withScript();
    }

    @Override
    protected ExportableKind getExportableKind()
    {
        return ExportableKind.EXPERIMENT_TYPE;
    }

    @Override
    protected EntityKind getEntityKind()
    {
        return EntityKind.EXPERIMENT;
    }

}
