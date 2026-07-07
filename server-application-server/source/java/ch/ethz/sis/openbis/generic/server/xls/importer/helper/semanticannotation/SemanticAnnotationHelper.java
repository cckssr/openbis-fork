/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation;


import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.search.SemanticAnnotationSearchCriteria;
import ch.ethz.sis.openbis.generic.server.xls.importer.delay.DelayedExecutionDecorator;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.IAttribute;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

import java.util.*;
import java.util.stream.Collectors;

/***
 *  map type code to first semantic annotation found in xls
 */
public final class SemanticAnnotationHelper
{

    private final Map<IEntityTypeId, SemanticAnnotation> entityTypeToSemanticAnnotationMap;
    private final Map<String, SemanticAnnotation> propertyTypeToSemanticAnnotationMap;
    private final Map<Map.Entry<IEntityTypeId, String>, SemanticAnnotation> propertyAssignmentToSemanticAnnotationMap;

    private final Map<SemanticAnnotationRecord, List<SemanticAnnotation>> recordToAnnotationMap;

    private final DelayedExecutionDecorator delayedExecutor;

    public SemanticAnnotationHelper(DelayedExecutionDecorator delayedExecutor)
    {
        entityTypeToSemanticAnnotationMap = new HashMap<>();
        propertyTypeToSemanticAnnotationMap = new HashMap<>();
        propertyAssignmentToSemanticAnnotationMap = new HashMap<>();
        recordToAnnotationMap = new HashMap<>();
        this.delayedExecutor = delayedExecutor;
    }

    public enum Attribute implements IAttribute
    {
        OntologyId("Ontology Id", false, false),
        OntologyVersion("Ontology Version", false, false),
        OntologyAnnotationId("Ontology Annotation Id", false, false);

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

    public SemanticAnnotation getEntityTypeSemanticAnnotation(List<SemanticAnnotationRecord> records,
            EntityTypePermId entityTypePermId)
    {
        return getSemanticAnnotationFromRecords(records, SemanticAnnotationType.EntityType, entityTypePermId, null);
    }

    public SemanticAnnotation getPropertyTypeSemanticAnnotation(List<SemanticAnnotationRecord> records,
            String propertyTypeCode)
    {
        return getSemanticAnnotationFromRecords(records, SemanticAnnotationType.PropertyType, null, propertyTypeCode);
    }

    public SemanticAnnotation getPropertyAssignmentSemanticAnnotation(List<SemanticAnnotationRecord> records,
            EntityTypePermId entityTypePermId, String propertyTypeCode)
    {
        return getSemanticAnnotationFromRecords(records, SemanticAnnotationType.PropertyAssignment, entityTypePermId, propertyTypeCode);
    }

    private SemanticAnnotation getSemanticAnnotationFromRecords(List<SemanticAnnotationRecord> records,
            SemanticAnnotationType type, EntityTypePermId entityTypePermIdOrNull,
            String propertyTypeCodeOrNull) {
        SemanticAnnotation annotation = null;
        boolean multipleFound = false;
        SemanticAnnotationRecord faultyRecord = null;
        for(SemanticAnnotationRecord record : records)
        {
            List<SemanticAnnotation> annotations = getSemanticAnnotation(record, type, entityTypePermIdOrNull, propertyTypeCodeOrNull);
            if(annotations.size() == 1)
            {
                annotation = annotations.get(0);
                break;
            } else if(annotations.size() > 1){
                multipleFound = true;
                faultyRecord = record;
            }
        }
        if(multipleFound) {
            throw new UserFailureException(String.format("Ambiguous import state: multiple semantic annotations were found for record: %s", faultyRecord));
        }
        if(annotation != null)
        {
            putSemanticAnnotationToCache(annotation, type, entityTypePermIdOrNull, propertyTypeCodeOrNull);
        }
        return annotation;
    }

    public Set<String> getCachedPropertyTypes() {
        Set<String> types = propertyAssignmentToSemanticAnnotationMap.keySet().stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        types.addAll(propertyTypeToSemanticAnnotationMap.keySet());
        return types;
    }

    public SemanticAnnotation getCachedSemanticAnnotation(SemanticAnnotationType type,
            EntityTypePermId permIdOrNull, String propertyCodeOrNull)
    {
        switch (type) {
            case EntityType:
                return entityTypeToSemanticAnnotationMap.get(permIdOrNull);
            case PropertyType:
                return propertyTypeToSemanticAnnotationMap.get(propertyCodeOrNull);
            case PropertyAssignment:
                return propertyAssignmentToSemanticAnnotationMap.get(Map.entry(permIdOrNull, propertyCodeOrNull));
            default:
                throw new UserFailureException("Unsupported semantic annotation type");
        }
    }

    private void putSemanticAnnotationToCache(SemanticAnnotation semanticAnnotation, SemanticAnnotationType type,
            EntityTypePermId permIdOrNull, String propertyCodeOrNull)
    {
        switch (type) {
            case EntityType:
                entityTypeToSemanticAnnotationMap.put(permIdOrNull, semanticAnnotation);
                break;
            case PropertyType:
                propertyTypeToSemanticAnnotationMap.put(propertyCodeOrNull, semanticAnnotation);
                break;
            case PropertyAssignment:
                propertyAssignmentToSemanticAnnotationMap.put(Map.entry(permIdOrNull, propertyCodeOrNull), semanticAnnotation);
                break;
        }
    }

    private List<SemanticAnnotation> getSemanticAnnotation(SemanticAnnotationRecord record,
            SemanticAnnotationType type, EntityTypePermId permIdOrNull, String codeOrNull)
    {
        List<SemanticAnnotation> result = new ArrayList<>();
        List<SemanticAnnotationRecord> annotationsToGet = new ArrayList<>();
        List<SemanticAnnotation> annotations = recordToAnnotationMap.get(record);

        if(annotations != null)
        {
            if (!annotations.isEmpty())
            {
                boolean annotationNotFound = true;
                for (SemanticAnnotation annotation : annotations)
                {
                    SemanticAnnotation matchedAnnotation =
                            matchSemanticAnnotation(type, permIdOrNull, codeOrNull, annotation, false);
                    if (matchedAnnotation != null)
                    {
                        result.add(matchedAnnotation);
                        annotationNotFound = false;
                    }
                }
                if (annotationNotFound)
                {
                    // no matching was found for the record
                    annotationsToGet.add(record);
                }
            }
        } else {
            //no annotation was found for the record
            annotationsToGet.add(record);
        }


        if(!annotationsToGet.isEmpty()) {
            for(SemanticAnnotationRecord annotationRecord : annotationsToGet)
            {
                List<SemanticAnnotation> annotationList = searchSemanticAnnotations(annotationRecord);
                if(annotationList == null) {
                    recordToAnnotationMap.put(annotationRecord, List.of());
                } else {
                    recordToAnnotationMap.put(annotationRecord, annotationList);
                    for(SemanticAnnotation annotation : annotationList) {
                        SemanticAnnotation matchedAnnotation = matchSemanticAnnotation(type, permIdOrNull, codeOrNull, annotation, false);
                        if(matchedAnnotation != null) {
                            result.add(matchedAnnotation);
                        }
                    }
                }
            }
        }

        if(result.size() > 1) {
            for(SemanticAnnotation annotation : result) {
                SemanticAnnotation matchedAnnotation = matchSemanticAnnotation(type, permIdOrNull, codeOrNull, annotation, true);
                if(matchedAnnotation != null) {
                    return List.of(matchedAnnotation);
                }
            }
        }
        return result;
    }


    private SemanticAnnotation matchSemanticAnnotation(SemanticAnnotationType type,
            EntityTypePermId permIdOrNull, String propertyCodeOrNull, SemanticAnnotation annotation, boolean exactMatching)
    {
        SemanticAnnotation result = null;
        switch (type) {
            case EntityType:
                IEntityType entityType = annotation.getEntityType();
                if(entityType != null && entityType.getPermId() instanceof EntityTypePermId entityTypePermId) {
                    if(entityTypePermId.equals(permIdOrNull)) {
                        result = annotation;
                    } else if(!exactMatching && entityTypePermId.getEntityKind().equals(permIdOrNull.getEntityKind())) {
                        result = annotation;
                    }

                }
                break;
            case PropertyType:
                PropertyType propertyType = annotation.getPropertyType();
                if(propertyType != null) {
                    if(exactMatching && propertyType.getCode().equals(propertyCodeOrNull)) {
                        result = annotation;
                    } else {
                        result = annotation;
                    }
                }
                break;
            case PropertyAssignment:
                PropertyAssignment assignment = annotation.getPropertyAssignment();
                if(assignment != null) {
                    IEntityType assignmentEntityType = assignment.getEntityType();
                    PropertyType assignmentPropertyType = assignment.getPropertyType();
                    if(assignmentEntityType.getPermId() instanceof EntityTypePermId) {
                        if(permIdOrNull.equals(assignmentEntityType.getPermId())) {
                            if(!exactMatching || assignmentPropertyType.getCode().equals(propertyCodeOrNull) ){
                                result = annotation;
                            }
                        }
                    }
                }
                break;
        }
        return result;
    }

    private List<SemanticAnnotation> searchSemanticAnnotations(SemanticAnnotationRecord record)
    {
        SemanticAnnotationSearchCriteria criteria =
                new SemanticAnnotationSearchCriteria();

        criteria.withPredicateOntologyId().thatEquals(record.getSemanticAnnotationId());
        criteria.withPredicateOntologyVersion().thatEquals(record.getSemanticAnnotationVersionId());
        criteria.withPredicateAccessionId().thatEquals(record.getSemanticAnnotationAccessionId());

        SemanticAnnotationFetchOptions fetchOptions = new SemanticAnnotationFetchOptions();
        fetchOptions.withEntityType();
        fetchOptions.withPropertyType();
        fetchOptions.withPropertyAssignment().withEntityType();
        fetchOptions.withPropertyAssignment().withPropertyType();

        return delayedExecutor.searchSemanticAnnotations(criteria,
                fetchOptions);
    }


}
