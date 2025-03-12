package ch.ethz.sis.openbis.generic.excel.v3.from.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.id.SemanticAnnotationPermId;
import ch.ethz.sis.openbis.generic.excel.v3.from.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.excel.v3.from.utils.IAttribute;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.IntStream;

public class SemanticAnnotationHelper extends BasicImportHelper
{
    Map<String, List<SemanticAnnotation>> entityTypeAnnotations = new HashMap<>();

    Map<String, List<SemanticAnnotation>> propertyTypeAnnotations = new HashMap<>();

    Map<String, List<SemanticAnnotation>> entityPropertyTypeAnnotations = new HashMap<>();

    Set<String> ontologies = new HashSet<>();

    Set<String> ontologyVersions = new HashSet<>();

    public enum SemanticAnnotationType
    {EntityType, PropertyType, EntityTypeProperty}

    private enum Attribute implements IAttribute
    {
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

    private SemanticAnnotationType type;

    private EntityTypePermId permIdOrNull;

    @Override
    protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        return false;
    }

    @Override
    protected void createObject(Map<String, Integer> headers, List<String> values, int page,
            int line)
    {
        String code = getValueByColumnName(headers, values, Attribute.Code);

        String ontologyId = getValueByColumnName(headers, values, Attribute.OntologyId);
        String[] ontologyIds =
                StringUtils.isNoneBlank(ontologyId) ? ontologyId.split("\n") : new String[] {};
        String ontologyVersion = getValueByColumnName(headers, values, Attribute.OntologyVersion);
        String[] ontologyVersions = StringUtils.isNotBlank(ontologyVersion) ?
                ontologyVersion.split("\n") :
                new String[] {};
        String ontologyAnnotationId =
                getValueByColumnName(headers, values, Attribute.OntologyAnnotationId);
        String[] accessionIds = StringUtils.isNotBlank(ontologyAnnotationId) ?
                ontologyAnnotationId.split("\n") :
                new String[] {};
        EntityKind entityKind;

        List<SemanticAnnotation> annotations =
                IntStream.range(0, ontologyIds.length).mapToObj(i -> {
                    SemanticAnnotation annotation = new SemanticAnnotation();
                    annotation.setDescriptorOntologyId(ontologyIds[i]);
                    annotation.setDescriptorOntologyVersion(ontologyVersions[i]);
                    annotation.setDescriptorAccessionId(accessionIds[i]);
                    annotation.setPredicateOntologyId(ontologyIds[i]);
                    annotation.setPredicateOntologyVersion(ontologyVersions[i]);
                    annotation.setPredicateAccessionId(accessionIds[i]);
                    return annotation;

                }).toList();

        SemanticAnnotation annotation = null;

        switch (type)
        {
            case EntityType:
                List<SemanticAnnotation> entityAnnotations = entityTypeAnnotations.get(code);
                if (entityAnnotations == null)
                {
                    entityAnnotations = new ArrayList<>();
                    entityTypeAnnotations.put(code, entityAnnotations);
                }
                entityAnnotations.addAll(annotations);


                break;
            case PropertyType:
                annotation = new SemanticAnnotation();
                annotation.setPredicateOntologyVersion(ontologyVersion);
                annotation.setPredicateAccessionId(ontologyAnnotationId);
                annotation.setPermId(new SemanticAnnotationPermId(this.permIdOrNull.getPermId()));
                List<SemanticAnnotation> propertyAnnotations = propertyTypeAnnotations.get(code);
                if (propertyAnnotations == null)
                {
                    propertyAnnotations = new ArrayList<>();
                    propertyTypeAnnotations.put(code, propertyAnnotations);
                }
                propertyAnnotations.addAll(annotations);
                break;
            case EntityTypeProperty:
                for (String currentOntologyId : ontologyIds)
                {

                    annotation = new SemanticAnnotation();
                    annotation.setPredicateOntologyId(ontologyId);
                    annotation.setPredicateOntologyVersion(ontologyVersion);
                    annotation.setPredicateAccessionId(ontologyAnnotationId);
                    annotation.setPermId(
                            new SemanticAnnotationPermId(this.permIdOrNull.getPermId()));
                    annotation.setDescriptorOntologyId(ontologyId);
                    annotation.setDescriptorOntologyVersion(ontologyVersion);
                    annotation.setDescriptorAccessionId(ontologyAnnotationId);
                    List<SemanticAnnotation> entityPropertyAnnotations =
                            entityPropertyTypeAnnotations.get(code);
                    if (entityPropertyAnnotations == null)
                    {
                        entityPropertyAnnotations = new ArrayList<>();
                        entityPropertyTypeAnnotations.put(code, entityPropertyAnnotations);
                    }
                    entityPropertyAnnotations.addAll(annotations);
                    break;
                }
        }
    }

    @Override
    protected void updateObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        // do only create
    }

    @Override
    protected void validateHeader(Map<String, Integer> headers)
    {
        // not validated here
    }

    public void importBlockForEntityType(List<List<String>> page, int pageIndex, int start, int end,
            ImportTypes importTypes)
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

        super.importBlock(page, pageIndex, start, end);
    }

    public void importBlockForEntityTypeProperty(List<List<String>> page, int pageIndex, int start,
            int end, ImportTypes importTypes)
    {
        type = SemanticAnnotationType.EntityTypeProperty;
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

        super.importBlock(page, pageIndex, start + 2, end);
    }

    public void importBlockForPropertyType(List<List<String>> page, int pageIndex, int start,
            int end)
    {
        type = SemanticAnnotationType.PropertyType;
        this.permIdOrNull = null;

        Map<String, Integer> header = parseHeader(page.get(start), false);
        String internal = getValueByColumnName(header, page.get(start + 1), Attribute.Internal);

        boolean canUpdate = true;

        super.importBlock(page, pageIndex, start, end);
    }

    @Override
    public void importBlock(List<List<String>> page, int pageIndex, int start, int end)
    {
        throw new IllegalStateException("This method should have never been called.");
    }

    public SemanticAnnotationByKind getResult()
    {
        return new SemanticAnnotationByKind(entityTypeAnnotations, propertyTypeAnnotations,
                entityPropertyTypeAnnotations);
    }


}
