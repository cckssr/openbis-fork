package ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;

import java.util.List;
import java.util.Set;

public interface SemanticAnnotationCache
{

    SemanticAnnotation getCachedSemanticAnnotation(SemanticAnnotationType type,
            EntityTypePermId permIdOrNull, String propertyCodeOrNull);

    Set<String> getCachedPropertyTypes();

    SemanticAnnotation getPropertyAssignmentSemanticAnnotation(
            List<SemanticAnnotationRecord> records,
            EntityTypePermId entityTypePermId, String propertyTypeCode);

    SemanticAnnotation getPropertyTypeSemanticAnnotation(List<SemanticAnnotationRecord> records,
            String propertyTypeCode);

    SemanticAnnotation getEntityTypeSemanticAnnotation(List<SemanticAnnotationRecord> records,
            EntityTypePermId entityTypePermId);
}
