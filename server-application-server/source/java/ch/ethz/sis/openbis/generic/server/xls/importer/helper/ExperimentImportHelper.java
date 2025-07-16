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
package ch.ethz.sis.openbis.generic.server.xls.importer.helper;

import static ch.ethz.sis.openbis.generic.server.xls.importer.utils.PropertyTypeSearcher.getPropertyValue;

import java.util.List;
import java.util.Map;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.delay.DelayedExecutionDecorator;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationType;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.AttributeValidator;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.IAttribute;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.PropertyTypeSearcher;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

public class ExperimentImportHelper extends BasicImportHelper
{
    private static final String EXPERIMENT_TYPE_FIELD = "Experiment type";

    private enum Attribute implements IAttribute {
        Identifier("Identifier", false, true),
        Code("Code", true, true),
        Project("Project", true, true);

        private final String headerName;

        private final boolean mandatory;

        private final boolean upperCase;

        Attribute(String headerName, boolean mandatory, boolean upperCase)
        {
            this.headerName = headerName;
            this.mandatory = mandatory;
            this.upperCase = upperCase;
        }

        @Override
        public String getHeaderName()
        {
            return headerName;
        }

        @Override
        public boolean isMandatory()
        {
            return mandatory;
        }

        @Override
        public boolean isUpperCase()
        {
            return upperCase;
        }
    }

    private EntityTypePermId experimentType;

    private final DelayedExecutionDecorator delayedExecutor;

    private PropertyTypeSearcher propertyTypeSearcher;

    private final AttributeValidator<Attribute> attributeValidator;

    private final SemanticAnnotationHelper annotationCache;

    public ExperimentImportHelper(DelayedExecutionDecorator delayedExecutor, ImportModes mode, ImportOptions options, SemanticAnnotationHelper annotationCache)
    {
        super(mode, options);
        this.delayedExecutor = delayedExecutor;
        this.attributeValidator = new AttributeValidator<>(Attribute.class);
        this.annotationCache = annotationCache;
    }

    @Override public void importBlock(List<List<String>> page, int pageIndex, int start, int end)
    {
        int lineIndex = start;

        try
        {
            Map<String, Integer> header = parseHeader(page.get(lineIndex), false);
            AttributeValidator.validateHeader(EXPERIMENT_TYPE_FIELD, header);
            lineIndex++;

            String typeCode = getValueByColumnName(header, page.get(lineIndex), EXPERIMENT_TYPE_FIELD);
            experimentType = new EntityTypePermId(typeCode, EntityKind.EXPERIMENT);
            SemanticAnnotation
                    annotation = annotationCache.getCachedSemanticAnnotation(SemanticAnnotationType.EntityType, experimentType, null);
            if(annotation != null)
            {
                typeCode = annotation.getEntityType().getCode();
                experimentType = new EntityTypePermId(typeCode, EntityKind.EXPERIMENT);
            }
            if(experimentType.getPermId() == null || experimentType.getPermId().isEmpty()) {
                throw new UserFailureException("Mandatory field is missing or empty: " + EXPERIMENT_TYPE_FIELD);
            }

            // first check that experiment type exist.
            ExperimentTypeFetchOptions fetchTypeOptions = new ExperimentTypeFetchOptions();
            fetchTypeOptions.withPropertyAssignments().withPropertyType().withVocabulary().withTerms();
            ExperimentType type = delayedExecutor.getExperimentType(experimentType, fetchTypeOptions);
            if (type == null)
            {
                throw new UserFailureException("Experiment type " + experimentType + " not found.");
            }
            this.propertyTypeSearcher = new PropertyTypeSearcher(type.getPropertyAssignments(), annotationCache);

            lineIndex++;
        } catch (Exception e)
        {
            throw new UserFailureException("sheet: " + (pageIndex + 1) + " line: " + (lineIndex + 1) + " message: " + e.getMessage());
        }

        // and then import experiments
        super.importBlock(page, pageIndex, lineIndex, end);
    }

    @Override protected ImportTypes getTypeName()
    {
        return ImportTypes.EXPERIMENT;
    }

    private ExperimentIdentifier getIdentifier(Map<String, Integer> header, List<String> values)
    {
        String identifier = getValueByColumnName(header, values, Attribute.Identifier);

        String code = getValueByColumnName(header, values, Attribute.Code);
        String project = getValueByColumnName(header, values, Attribute.Project);

        ExperimentIdentifier experimentIdentifier = null;
        if (identifier != null && !identifier.isEmpty()) {
            experimentIdentifier = new ExperimentIdentifier(identifier);
        } else {
            experimentIdentifier = new ExperimentIdentifier(project + "/" + code);
        }
        return experimentIdentifier;
    }

    @Override protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
        fetchOptions.withType();

        return delayedExecutor.getExperiment(getIdentifier(header, values), fetchOptions) != null;
    }

    @Override protected void createObject(Map<String, Integer> header, List<String> values, int page, int line)
    {
        ExperimentCreation creation = new ExperimentCreation();

        String code = getValueByColumnName(header, values, Attribute.Code);
        String project = getValueByColumnName(header, values, Attribute.Project);

        creation.setTypeId(experimentType);
        creation.setCode(code);
        creation.setProjectId(new ProjectIdentifier(project));

        for (String key : header.keySet())
        {
            if (!attributeValidator.isHeader(key))
            {
                String value = getValueByColumnName(header, values, key);
                PropertyType propertyType = propertyTypeSearcher.findPropertyType(key);
                creation.setProperty(propertyType.getCode(), getPropertyValue(propertyType, value));
            }
        }

        delayedExecutor.createExperiment(creation, page, line);
    }

    @Override protected void updateObject(Map<String, Integer> header, List<String> values, int page, int line)
    {
        String identifier = getValueByColumnName(header, values, Attribute.Identifier);

        if (identifier == null || identifier.isEmpty())
        {
            throw new UserFailureException("'Identifier' is missing, is mandatory since is needed to select a experiment to update.");
        }

        ExperimentUpdate update = new ExperimentUpdate();
        IExperimentId experimentId = new ExperimentIdentifier(identifier);
        update.setExperimentId(experimentId);

        String project = getValueByColumnName(header, values, Attribute.Project);

        // Project is used to "MOVE", only set if present since can't be null
        if (project != null && !project.isEmpty())
        {
            update.setProjectId(new ProjectIdentifier(project));
        }

        for (String key : header.keySet())
        {
            if (!attributeValidator.isHeader(key))
            {
                String value = getValueByColumnName(header, values, key);
                if (value == null || value.isEmpty()) { // Skip empty values to avoid deleting by mistake
                    continue;
                } else if (value.equals("--DELETE--") || value.equals("__DELETE__")) // Do explicit delete
                {
                    value = null;
                } else { // Normal behaviour, set value
                }
                PropertyType propertyType = propertyTypeSearcher.findPropertyType(key);
                update.setProperty(propertyType.getCode(), getPropertyValue(propertyType, value));
            }
        }

        delayedExecutor.updateExperiment(update, page, line);
    }

    @Override protected void validateHeader(Map<String, Integer> header)
    {
        attributeValidator.validateHeaders(Attribute.values(), propertyTypeSearcher, header);
    }
}
