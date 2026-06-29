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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.exporter.ExportEntityTypeCollector;
import ch.ethz.sis.openbis.generic.server.xls.export.Attribute;
import ch.ethz.sis.openbis.generic.server.xls.export.ExportableKind;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;
import java.util.stream.Collectors;

import static ch.ethz.sis.openbis.generic.server.xls.export.Attribute.*;

public class XLSDataSetTypeExportHelper extends AbstractXLSEntityTypeExportHelper<DataSetType>
{


    public XLSDataSetTypeExportHelper(final Workbook wb)
    {
        super(wb);
    }


    @Override
    public DataSetType getEntityType(final IApplicationServerApi api, final String sessionToken,
            final String permId)
    {
        return ExportEntityTypeCollector.fetchDataSetType(api, sessionToken, permId);
    }

    @Override
    protected Attribute[] getAttributes(final DataSetType entityType)
    {
        return new Attribute[] { CODE, INTERNAL, DESCRIPTION, VALIDATION_SCRIPT, MAIN_DATA_SET_PATTERN, MAIN_DATA_SET_PATH, DISALLOW_DELETION,
                MODIFICATION_DATE, ONTOLOGY_ID, ONTOLOGY_ANNOTATION_ID, ONTOLOGY_VERSION, META_DATA };
    }

    @Override
    protected String getAttributeValue(IApplicationServerApi api, String sessionToken,
            final DataSetType dataSetType, final Attribute attribute)
    {
        switch (attribute)
        {
            case CODE:
            {
                return dataSetType.getCode();
            }
            case INTERNAL:
            {
                return dataSetType.isManagedInternally().toString().toUpperCase();
            }
            case DESCRIPTION:
            {
                return dataSetType.getDescription();
            }
            case VALIDATION_SCRIPT:
            {
                final Plugin validationPlugin = dataSetType.getValidationPlugin();
                return validationPlugin != null ? (validationPlugin.getName() != null ? validationPlugin.getName() + ".py" : "") : "";

            }
            case MAIN_DATA_SET_PATTERN:
            {
                return dataSetType.getMainDataSetPattern();
            }
            case MAIN_DATA_SET_PATH:
            {
                return dataSetType.getMainDataSetPath();
            }
            case DISALLOW_DELETION:
            {
                return dataSetType.isDisallowDeletion().toString().toUpperCase();
            }
            case MODIFICATION_DATE:
            {
                return DATE_FORMAT.format(dataSetType.getModificationDate());
            }
            case ONTOLOGY_ID:
            {
                // create a helper method, give kind as argument
                List<SemanticAnnotation>
                        searchResult =
                        getSemanticAnnotationSearchResult(api, sessionToken, EntityKind.DATA_SET,
                                dataSetType.getCode(), null);

                return searchResult.stream().map(
                                SemanticAnnotation::getPredicateOntologyId)
                        .collect(Collectors.joining("\n"));

            }
            case ONTOLOGY_VERSION:
            {
                List<SemanticAnnotation>
                        searchResult =
                        getSemanticAnnotationSearchResult(api, sessionToken, EntityKind.DATA_SET,
                                dataSetType.getCode(), null);

                return searchResult.stream().map(
                                SemanticAnnotation::getPredicateOntologyVersion)
                        .collect(Collectors.joining("\n"));

            }
            case ONTOLOGY_ANNOTATION_ID:
            {
                List<SemanticAnnotation>
                        searchResult =
                        getSemanticAnnotationSearchResult(api, sessionToken, EntityKind.DATA_SET,
                                dataSetType.getCode(), null);

                return searchResult.stream().map(
                                SemanticAnnotation::getPredicateAccessionId)
                        .collect(Collectors.joining("\n"));

            }
            case META_DATA:
            {
                return mapToJSON(dataSetType.getMetaData());
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
        return ExportableKind.DATASET_TYPE;
    }

    @Override
    protected EntityKind getEntityKind()
    {
        return EntityKind.DATA_SET;
    }

}
