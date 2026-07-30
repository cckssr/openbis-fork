package ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SemanticAnnotationCache
{

    List<SemanticAnnotation> getSemanticAnnotations(SemanticAnnotationType type,
            EntityTypePermId permIdOrNull, String propertyCodeOrNull);

    Set<String> getCachedPropertyTypes();

    Map<SemanticAnnotationRecord, SemanticAnnotation> findPropertyAssignmentSemanticAnnotationsFromRecords(
            List<SemanticAnnotationRecord> records,
            EntityTypePermId entityTypePermId, String propertyTypeCode);

    Map<SemanticAnnotationRecord, SemanticAnnotation> findPropertyTypeSemanticAnnotationsFromRecords(List<SemanticAnnotationRecord> records,
            String propertyTypeCode);

    Map<SemanticAnnotationRecord, SemanticAnnotation> findEntityTypeSemanticAnnotationsFromRecords(List<SemanticAnnotationRecord> records,
            EntityTypePermId entityTypePermId);
}
