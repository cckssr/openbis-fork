package ch.ethz.sis.openbis.generic.excel.v3.from.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;

import java.util.List;
import java.util.Map;

public class SemanticAnnotationByKind
{

    private final Map<String, List<SemanticAnnotation>> entityTypeAnnotations;

    private final Map<String, List<SemanticAnnotation>> propertyTypeAnnotations;

    private final Map<String, List<SemanticAnnotation>> entityPropertyTypeAnnotations;

    public SemanticAnnotationByKind(Map<String, List<SemanticAnnotation>> entityTypeAnnotations,
            Map<String, List<SemanticAnnotation>> propertyTypeAnnotations,
            Map<String, List<SemanticAnnotation>> entityPropertyTypeAnnotations)
    {
        this.entityTypeAnnotations = entityTypeAnnotations;
        this.propertyTypeAnnotations = propertyTypeAnnotations;
        this.entityPropertyTypeAnnotations = entityPropertyTypeAnnotations;
    }

    public Map<String, List<SemanticAnnotation>> getEntityTypeAnnotations()
    {
        return entityTypeAnnotations;
    }

    public Map<String, List<SemanticAnnotation>> getPropertyTypeAnnotations()
    {
        return propertyTypeAnnotations;
    }

    public Map<String, List<SemanticAnnotation>> getEntityPropertyTypeAnnotations()
    {
        return entityPropertyTypeAnnotations;
    }

}
