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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.event.EntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.IPropertyTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.search.SemanticAnnotationSearchCriteria;
import ch.ethz.sis.openbis.generic.server.xls.importer.delay.DelayedExecutionDecorator;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    public SemanticAnnotation getSemanticAnnotation(SemanticAnnotationRecord[] records, EntityTypePermId permIdOrNull, String propertyCodeOrNull)
    {
        SemanticAnnotationType type = null;
        if(permIdOrNull != null) {
            if(propertyCodeOrNull != null) {
                type = SemanticAnnotationType.PropertyAssignment;
            } else {
                type = SemanticAnnotationType.EntityType;
            }
        } else if(propertyCodeOrNull != null) {
            type = SemanticAnnotationType.PropertyType;
        } else {
            throw new UserFailureException("At least one type for semantic annotation matching needs to be defined!");
        }

        SemanticAnnotation annotation = null;
        boolean multipleFound = false;
        for(SemanticAnnotationRecord record : records)
        {
            List<SemanticAnnotation> annotations = getSemanticAnnotation(record, type, permIdOrNull, propertyCodeOrNull);
            if(annotations.size() == 1)
            {
                annotation = annotations.get(0);
                break;
            } else if(annotations.size() > 1){
                multipleFound = true;
            }
        }
        if(annotation == null && multipleFound) {
            throw new UserFailureException("Ambiguous import state: multiple semantic annotations were found matching the criteria!");
        }
        if(annotation != null)
        {
            putSemanticAnnotationToCache(annotation, type, permIdOrNull, propertyCodeOrNull);
        }
        return annotation;
    }

    public Set<String> getCachedPropertyTypes() {
        return propertyTypeToSemanticAnnotationMap.keySet();
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
                if(entityType != null && entityType.getPermId() instanceof EntityTypePermId) {
                    EntityTypePermId entityTypePermId = (EntityTypePermId) entityType.getPermId();
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
