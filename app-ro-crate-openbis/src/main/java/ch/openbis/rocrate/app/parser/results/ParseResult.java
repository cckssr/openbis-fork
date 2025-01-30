package ch.openbis.rocrate.app.parser.results;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntity;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.openbis.rocrate.app.parser.helper.SemanticAnnotationHelper;

import java.util.Map;

public class ParseResult
{
    final Map<EntityTypePermId, IEntityType> schema;

    final Map<ObjectIdentifier, AbstractEntity> metadata;

    final Map<ProjectIdentifier, Project> projects;

    public Map<String, Space> getSpaceResult()
    {
        return spaceResult;
    }

    public Map<ProjectIdentifier, Project> getProjects()
    {
        return projects;
    }

    public Map<ObjectIdentifier, AbstractEntity> getMetadata()
    {
        return metadata;
    }

    public Map<EntityTypePermId, IEntityType> getSchema()
    {
        return schema;
    }

    final Map<String, Space> spaceResult;

    public final SemanticAnnotationHelper.SemanticAnnotationByKind semanticAnnotationByKind;

    public SemanticAnnotationHelper.SemanticAnnotationByKind getSemanticAnnotationByKind()
    {
        return semanticAnnotationByKind;
    }

    public ParseResult(Map<EntityTypePermId, IEntityType> schema,
            Map<ObjectIdentifier, AbstractEntity> metadata,
            Map<ProjectIdentifier, Project> projects,
            Map<String, Space> spaceResult,
            SemanticAnnotationHelper.SemanticAnnotationByKind semanticAnnotationByKind)
    {
        this.schema = schema;
        this.metadata = metadata;
        this.projects = projects;
        this.spaceResult = spaceResult;
        this.semanticAnnotationByKind = semanticAnnotationByKind;
    }
}
