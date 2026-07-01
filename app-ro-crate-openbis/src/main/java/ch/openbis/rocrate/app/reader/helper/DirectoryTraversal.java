package ch.openbis.rocrate.app.reader.helper;

import ch.eth.sis.rocrate.util.RoCrateValueUtil;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.openbis.rocrate.app.reader.RdfToModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.AbstractEntity;

import java.util.*;

import static ch.openbis.rocrate.app.Constants.FILE_TYPES;

public class DirectoryTraversal
{

    Set<String> hasPartAnnotations =
            Set.of("schema:hasPart", "https://schema.org/hasPart", "http://schema.org/hasPart");

    Set<String> hasPartPropertiesName = Set.of("schema:hasPart", "hasPart");

    public TraversalResult findAllFiles(String id, RoCrate crate, Sample sample)
    {
        AbstractEntity entityById = crate.getEntityById(id);
        return findAllFiles(entityById, crate, sample);

    }

    public record TraversalResult(
            List<RdfToModel.FileProblem> missingEntitites,
            List<AbstractEntity> files
    )
    {

    }

    public TraversalResult findAllFiles(AbstractEntity abstractEntity, RoCrate crate,
            Sample sample)
    {

        List<PropertyAssignment> hasPartProperties =
                sample.getType().getPropertyAssignments().stream().filter(this::isPartProperty)
                        .toList(); // we only do this once. If the result is not a Dataset or a File, it's considered a metadata entity
        List<RdfToModel.FileProblem> missingEntityIdentifiers = new ArrayList<>();

        List<AbstractEntity> result = new ArrayList<>();

        abstractEntity.getLinkedTo();
        Deque<AbstractEntity> open = new LinkedList<>();
        Set<AbstractEntity> closed = new LinkedHashSet<>();

        List<String> entitiesToAdd = hasPartProperties.stream()
                .map(x -> abstractEntity.getProperty(x.getPropertyType().getLabel()))
                .filter(Objects::nonNull)
                .map(RoCrateValueUtil::parseMultiValued)
                .flatMap(Collection::stream)
                .toList();

        for (String a : entitiesToAdd)
        {

            AbstractEntity entityById = crate.getEntityById(a);
            if (entityById == null)
            {
                missingEntityIdentifiers.add(new RdfToModel.FileProblem("File", a));
            } else
            {
                open.add(entityById);

            }

        }


        open.add(abstractEntity);

        while (!open.isEmpty())
        {

            AbstractEntity cur = open.poll();

            Set<String> foundFileTypes = new LinkedHashSet<>(parseTypes(cur));
            foundFileTypes.retainAll(FILE_TYPES);

            if (!foundFileTypes.isEmpty())
            {
                result.add(cur);
            }

            if (closed.contains(cur))
            {
                continue;
            }
            List<String> idsToCheck = new ArrayList<>();

            for (String curPropertyName : hasPartPropertiesName)
            {
                idsToCheck.addAll(RoCrateValueUtil.parseMultiValued(
                        cur.getProperty(curPropertyName)));
            }

            for (String linkedId : idsToCheck)
            {
                AbstractEntity curEntity = crate.getEntityById(linkedId);
                if (curEntity == null)
                {
                    missingEntityIdentifiers.add(new RdfToModel.FileProblem("File", linkedId));
                    continue;
                }

                Set<String> types = parseTypes(curEntity);
                if (types.contains("Dataset"))
                {
                    open.add(curEntity);
                }

                foundFileTypes = new LinkedHashSet<>(parseTypes(curEntity));
                foundFileTypes.retainAll(FILE_TYPES);

                if (!foundFileTypes.isEmpty())
                {
                    result.add(curEntity);
                }

            }
            closed.add(cur);
        }
        return new TraversalResult(missingEntityIdentifiers, result);
    }

    private Set<String> parseTypes(AbstractEntity entity)
    {
        JsonNode typeResult = entity.getProperty("@type");
        if (typeResult.isTextual())
        {
            return Set.of(typeResult.textValue());
        }
        if (typeResult.isArray())
        {
            ArrayNode arrayNode = (ArrayNode) typeResult;
            Set<String> typeroos = new LinkedHashSet<>();
            arrayNode.forEach(x -> typeroos.add(x.textValue()));
            return typeroos;

        }
        throw new RuntimeException("Unknown node type for @type");

    }

    boolean isPartProperty(PropertyAssignment propertyAssignment)
    {

        for (SemanticAnnotation semanticAnnotation : propertyAssignment.getSemanticAnnotations())
        {
            if (hasPartAnnotations.contains(semanticAnnotation.getPredicateAccessionId()))
            {
                return true;
            }

        }
        for (SemanticAnnotation semanticAnnotation : propertyAssignment.getPropertyType()
                .getSemanticAnnotations())
        {
            if (hasPartAnnotations.contains(semanticAnnotation.getPredicateAccessionId()))
            {
                return true;
            }
        }
        return false;
    }


}
