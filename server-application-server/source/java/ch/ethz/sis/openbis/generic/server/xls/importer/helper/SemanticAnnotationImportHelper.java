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

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.search.PropertyAssignmentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.create.SemanticAnnotationCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.search.SemanticAnnotationSearchCriteria;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.delay.DelayedExecutionDecorator;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationRecord;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationType;
import ch.ethz.sis.openbis.generic.server.xls.importer.semantic.ApplicationServerSemanticAPIExtensions;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.IAttribute;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.ImportUtils;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SemanticAnnotationImportHelper extends BasicImportHelper
{

    enum Attribute implements IAttribute {
        Code("Code", true, true),
        OntologyId("Ontology Id", false, false),
        OntologyVersion("Ontology Version", false, false),
        OntologyAnnotationId("Ontology Annotation Id", false, false),
        Internal("Internal", false, false);

        private final String headerName;

        private final boolean mandatory;

        private final boolean upperCase;

        Attribute(String headerName, boolean mandatory, boolean upperCase)
        {
            this.headerName = headerName;
            this.mandatory = mandatory;
            this.upperCase = upperCase;
        }

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

    private final DelayedExecutionDecorator delayedExecutor;

    private SemanticAnnotationType type;

    private EntityTypePermId permIdOrNull;

    private final SemanticAnnotationHelper annotationCache;

    public SemanticAnnotationImportHelper(DelayedExecutionDecorator delayedExecutor, ImportModes mode, ImportOptions options, SemanticAnnotationHelper annotationCache)
    {
        super(mode, options);
        this.delayedExecutor = delayedExecutor;
        this.annotationCache = annotationCache;
    }

    @Override protected ImportTypes getTypeName()
    {
        return ImportTypes.SEMANTIC_ANNOTATION;
    }

    @Override protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        boolean insertSemanticAnnotation = false; // Initially we don't need to insert a semantic annotation

        String spreadsheetCode = getValueByColumnName(header, values, Attribute.Code);

        switch (type) {
            case EntityType:
                EntityTypePermId id = new EntityTypePermId(spreadsheetCode, permIdOrNull.getEntityKind());
                SemanticAnnotation annotation = annotationCache.getCachedSemanticAnnotation(type, id, spreadsheetCode);
                if(annotation != null) {
                    spreadsheetCode = annotation.getEntityType().getCode();
                }
                break;
            case PropertyType:
                SemanticAnnotation propertyAnnotation = annotationCache.getCachedSemanticAnnotation(type, null, spreadsheetCode);
                if(propertyAnnotation != null) {
                    spreadsheetCode = propertyAnnotation.getPropertyType().getCode();
                }
                break;
            case PropertyAssignment:
                SemanticAnnotation assignmentAnnotation = annotationCache.getCachedSemanticAnnotation(type, permIdOrNull, spreadsheetCode);
                if(assignmentAnnotation != null) {
                    spreadsheetCode = assignmentAnnotation.getPropertyAssignment().getPropertyType().getCode();
                }
                break;
        }

        final String code = spreadsheetCode;

        String ontologyId[] =
                Optional.ofNullable(getValueByColumnName(header, values, Attribute.OntologyId))
                        .filter(StringUtils::isNotBlank)
                        .map(x -> x.split("\n"))
                        .orElse(new String[] {});
        String ontologyVersion[] =
                Optional.ofNullable(getValueByColumnName(header, values, Attribute.OntologyVersion))
                        .filter(StringUtils::isNotBlank)
                        .map(x -> x.split("\n"))
                        .orElse(new String[] {});
        String ontologyAnnotationId[] = Optional.ofNullable(
                        getValueByColumnName(header, values, Attribute.OntologyAnnotationId))
                .filter(StringUtils::isNotBlank)
                .map(x -> x.split("\n"))
                .orElse(new String[] {});

        if (ontologyId.length != ontologyVersion.length || ontologyVersion.length != ontologyAnnotationId.length)
        {
            throw new UserFailureException("Different number of entries for ontologies!");

        }

        List<SemanticAnnotationSearchCriteria> searchCriteria =
                IntStream.range(0, ontologyAnnotationId.length)
                        .mapToObj(i -> {
                            SemanticAnnotationSearchCriteria criteria =
                                    new SemanticAnnotationSearchCriteria();
                            if (type == SemanticAnnotationType.EntityType)
                            {
                                criteria.withEntityType().withKind()
                                        .thatEquals(this.permIdOrNull.getEntityKind());
                                criteria.withEntityType().withCode()
                                        .thatEquals(this.permIdOrNull.getPermId());
                            }
                            else if (type == SemanticAnnotationType.PropertyType)
                            {
                                criteria.withPropertyType().withCode().thatEquals(code);
                            }
                            else {
                                PropertyAssignmentSearchCriteria p = criteria.withPropertyAssignment();
                                p.withEntityType().withKind()
                                        .thatEquals(this.permIdOrNull.getEntityKind());
                                p.withEntityType().withCode()
                                        .thatEquals(this.permIdOrNull.getPermId());
                                p.withPropertyType().withCode().thatEquals(code);
                            }
                            criteria.withPredicateOntologyId().thatEquals(ontologyId[i]);
                            criteria.withPredicateOntologyVersion().thatEquals(ontologyVersion[i]);
                            criteria.withPredicateAccessionId().thatEquals(ontologyAnnotationId[i]);
                            return criteria;

                        }).collect(Collectors.toList());

        for (SemanticAnnotationSearchCriteria criteria : searchCriteria)
        {
            SemanticAnnotation semanticAnnotation = delayedExecutor.getSemanticAnnotation(criteria, new SemanticAnnotationFetchOptions());
            insertSemanticAnnotation = (semanticAnnotation == null); // We insert a semantic annotation
            if (insertSemanticAnnotation)
            {
                return false;
            }

        }

        return true;
    }

    @Override protected void createObject(Map<String, Integer> headers, List<String> values, int page, int line)
    {
        String spreadsheetCode = getValueByColumnName(headers, values, Attribute.Code);
        if(permIdOrNull != null) {
            SemanticAnnotation annotation = annotationCache.getCachedSemanticAnnotation(SemanticAnnotationType.PropertyAssignment, this.permIdOrNull, spreadsheetCode);
            if(annotation != null) {
                spreadsheetCode = annotation.getPropertyAssignment().getPropertyType().getCode();
            }
        }
        final String code = spreadsheetCode;


        String ontologyId[] =
                Optional.ofNullable(getValueByColumnName(headers, values, Attribute.OntologyId))
                        .map(x -> x.split("\n"))
                        .orElse(new String[] {});
        String ontologyVersion[] =
                Optional.ofNullable(
                                getValueByColumnName(headers, values, Attribute.OntologyVersion))
                        .map(x -> x.split("\n"))
                        .orElse(new String[] {});
        String ontologyAnnotationId[] = Optional.ofNullable(
                        getValueByColumnName(headers, values, Attribute.OntologyAnnotationId))
                .map(x -> x.split("\n"))
                .orElse(new String[] {});

        Map<SemanticAnnotationSearchCriteria, SemanticAnnotationRecord> criteriaToAnnotationInfo =
                new HashMap<>();

        List<SemanticAnnotationSearchCriteria> searchCriteria =
                IntStream.range(0, ontologyAnnotationId.length)
                        .mapToObj(i -> {
                            SemanticAnnotationSearchCriteria criteria =
                                    new SemanticAnnotationSearchCriteria();
                            if (type == SemanticAnnotationType.PropertyAssignment || type == SemanticAnnotationType.EntityType)
                            {
                                criteria.withEntityType().withKind()
                                        .thatEquals(this.permIdOrNull.getEntityKind());
                                criteria.withEntityType().withCode()
                                        .thatEquals(this.permIdOrNull.getPermId());
                            }
                            if (type == SemanticAnnotationType.PropertyAssignment || type == SemanticAnnotationType.PropertyType)
                            {
                                criteria.withPropertyType().withCode().thatEquals(code);
                            }
                            criteria.withPredicateOntologyId().thatEquals(ontologyId[i]);
                            criteria.withPredicateOntologyVersion().thatEquals(ontologyVersion[i]);
                            criteria.withPredicateAccessionId().thatEquals(ontologyAnnotationId[i]);
                            criteriaToAnnotationInfo.put(criteria,
                                    new SemanticAnnotationRecord(ontologyId[i], ontologyVersion[i],
                                            ontologyAnnotationId[i]));

                            return criteria;

                        }).collect(Collectors.toList());
        List<SemanticAnnotationCreation> creations = new ArrayList<>();

        for (SemanticAnnotationSearchCriteria criteria : searchCriteria)
        {
            SemanticAnnotationCreation creation = new SemanticAnnotationCreation();

            SemanticAnnotation semanticAnnotation = delayedExecutor.getSemanticAnnotation(criteria,
                    new SemanticAnnotationFetchOptions());
            if (semanticAnnotation != null)
            {
                continue;
            }
            SemanticAnnotationRecord record = criteriaToAnnotationInfo.get(criteria);

            switch (type)
            {
                case EntityType:
                    creation = ApplicationServerSemanticAPIExtensions.getSemanticSubjectCreation(
                            this.permIdOrNull.getEntityKind(),
                            this.permIdOrNull.getPermId(),// == code
                            record.getSemanticAnnotationId(),
                            record.getSemanticAnnotationVersionId(),
                            record.getSemanticAnnotationAccessionId());
                    break;
                case PropertyType:
                    creation = ApplicationServerSemanticAPIExtensions.getSemanticPredicateCreation(
                            code, // Property Code
                            record.getSemanticAnnotationId(),
                            record.getSemanticAnnotationVersionId(),
                            record.getSemanticAnnotationAccessionId());
                    break;
                case PropertyAssignment:
                    creation =
                            ApplicationServerSemanticAPIExtensions.getSemanticPredicateWithSubjectCreation(
                                    this.permIdOrNull.getEntityKind(),
                                    this.permIdOrNull.getPermId(),
                                    code, // Property Code
                                    record.getSemanticAnnotationId(),
                                    record.getSemanticAnnotationVersionId(),
                                    record.getSemanticAnnotationAccessionId());
                    break;
            }
            creations.add(creation);

        }

        creations.forEach(
                creation -> delayedExecutor.createSemanticAnnotation(creation, page, line));
    }

    @Override protected void updateObject(Map<String, Integer> header, List<String> values, int page, int line)
    {
        // do only create
    }

    @Override protected void validateHeader(Map<String, Integer> headers)
    {
        // not validated here
    }

    public void importBlockForEntityType(List<List<String>> page, int pageIndex, int start, int end, ImportTypes importTypes)
    {
        type = SemanticAnnotationType.EntityType;
        Map<String, Integer> header = parseHeader(page.get(start), false);
        String code = getValueByColumnName(header, page.get(start + 1), Attribute.Code);

        switch (importTypes)
        {
            case EXPERIMENT_TYPE:
                this.permIdOrNull = new EntityTypePermId(code, EntityKind.EXPERIMENT);
                break;
            case SAMPLE_TYPE:
                this.permIdOrNull = new EntityTypePermId(code, EntityKind.SAMPLE);
                break;
            case DATASET_TYPE:
                this.permIdOrNull = new EntityTypePermId(code, EntityKind.DATA_SET);
                break;
            default:
                throw new RuntimeException("Should never happen!");
        }

        SemanticAnnotation annotation = annotationCache.getCachedSemanticAnnotation(SemanticAnnotationType.EntityType, this.permIdOrNull, null);
        if(annotation != null) {
            code = annotation.getEntityType().getCode();
            permIdOrNull = new EntityTypePermId(code, this.permIdOrNull.getEntityKind());
        }

        super.importBlock(page, pageIndex, start, end);
    }

    public void importBlockForPropertyAssignment(List<List<String>> page, int pageIndex, int start, int end, ImportTypes importTypes)
    {
        type = SemanticAnnotationType.PropertyAssignment;
        Map<String, Integer> header = parseHeader(page.get(start), false);
        String code = getValueByColumnName(header, page.get(start + 1), Attribute.Code);
        String internal = getValueByColumnName(header, page.get(start + 1), Attribute.Internal);

        switch (importTypes)
        {
            case EXPERIMENT_TYPE:
                this.permIdOrNull = new EntityTypePermId(code, EntityKind.EXPERIMENT);
                break;
            case SAMPLE_TYPE:
                this.permIdOrNull = new EntityTypePermId(code, EntityKind.SAMPLE);
                break;
            case DATASET_TYPE:
                this.permIdOrNull = new EntityTypePermId(code, EntityKind.DATA_SET);
                break;
            default:
                throw new RuntimeException("Should never happen!");
        }

        SemanticAnnotation annotation = annotationCache.getCachedSemanticAnnotation(SemanticAnnotationType.EntityType, this.permIdOrNull, null);
        if(annotation != null) {
            code = annotation.getEntityType().getCode();
            permIdOrNull = new EntityTypePermId(code, this.permIdOrNull.getEntityKind());
        }

        boolean isInternalNamespace = ImportUtils.isTrue(internal);
        boolean canUpdate = (isInternalNamespace == false) || delayedExecutor.isSystem();

        if(canUpdate) {
            super.importBlock(page, pageIndex, start + 2, end);
        }
    }

    public void importBlockForPropertyType(List<List<String>> page, int pageIndex, int start, int end)
    {
        type = SemanticAnnotationType.PropertyType;
        this.permIdOrNull = null;

        Map<String, Integer> header = parseHeader(page.get(start), false);
        String internal = getValueByColumnName(header, page.get(start + 1), Attribute.Internal);

        boolean isInternalNamespace = internal != null && !internal.trim().isEmpty() && Boolean.parseBoolean(internal);
        boolean canUpdate = (isInternalNamespace == false) || delayedExecutor.isSystem();

        if(canUpdate) {
            super.importBlock(page, pageIndex, start, end);
        }
    }

    @Override public void importBlock(List<List<String>> page, int pageIndex, int start, int end)
    {
        throw new IllegalStateException("This method should have never been called.");
    }

}
