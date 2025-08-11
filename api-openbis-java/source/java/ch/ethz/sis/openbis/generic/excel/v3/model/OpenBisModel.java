package ch.ethz.sis.openbis.generic.excel.v3.model;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.PluginPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyPermId;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenBisModel
{
    final Map<VocabularyPermId, Vocabulary> vocabularyTypes;

    final Map<EntityTypePermId, IEntityType> entityTypes;

    final Map<SpacePermId, Space> spaces;

    final Map<ProjectIdentifier, Project> projects;

    final Map<ObjectIdentifier, AbstractEntityPropertyHolder> entities;

    final Map<PluginPermId, Plugin> plugins;

    final Map<String, List<Path>> miscellaneous;
            // e.g. "file-service", Path to file -> "/eln-lims/29/be/e4/29bee495-9ec6-492e-995a-554b84a4f13a/1a975eb7-5c73-4553-a295-4d9ce9b43d23.jpeg"

    final Map<String, String> externalToOpenBisIdentifiers;

    public static final String CODE_SPECIAL_CHARACTER_REPLACEMENT = "_";


    public OpenBisModel(Map<VocabularyPermId, Vocabulary> vocabularyTypes,
            Map<EntityTypePermId, IEntityType> entityTypes, Map<SpacePermId, Space> spaces,
            Map<ProjectIdentifier, Project> projects,
            Map<ObjectIdentifier, AbstractEntityPropertyHolder> entities,
            Map<PluginPermId, Plugin> plugins,
            Map<String, List<Path>> miscellaneous, Map<String, String> externalToOpenBisIdentifiers)
    {
        this.vocabularyTypes = vocabularyTypes;
        this.entityTypes = entityTypes;
        this.spaces = spaces;
        this.projects = projects;
        this.entities = entities;
        this.plugins = plugins;
        this.miscellaneous = miscellaneous;
        this.externalToOpenBisIdentifiers = externalToOpenBisIdentifiers;
    }

    public Map<VocabularyPermId, Vocabulary> getVocabularyTypes() {
        return vocabularyTypes;
    }

    public Map<EntityTypePermId, IEntityType> getEntityTypes() {
        return entityTypes;
    }

    public Map<SpacePermId, Space> getSpaces() {
        return spaces;
    }

    public Map<ProjectIdentifier, Project> getProjects() {
        return projects;
    }

    public Map<ObjectIdentifier, AbstractEntityPropertyHolder> getEntities() {
        return entities;
    }

    public Map<PluginPermId, Plugin> getPlugins()
    {
        return plugins;
    }

    public List<SampleType> getSampleTypes()
    {
        return entityTypes.entrySet().stream()
                .filter(x -> x.getKey().getEntityKind() == EntityKind.SAMPLE)
                .map(x -> x.getValue())
                .map(SampleType.class::cast)
                .collect(Collectors.toList());

    }

    public Map<EntityTypePermId, List<Sample>> getSamplesByType()
    {
        return entities.values().stream().filter(x -> x instanceof Sample).
                map(Sample.class::cast)
                .collect(Collectors.groupingBy(x -> new EntityTypePermId(x.getType().getCode(),EntityKind.SAMPLE)));
    }

    public Map<String, String> getExternalToOpenBisIdentifiers()
    {
        return externalToOpenBisIdentifiers;
    }

    public static String makeOpenBisCodeCompliant(String candiate)
    {
        return candiate.replaceAll("\\|", CODE_SPECIAL_CHARACTER_REPLACEMENT)
                .replaceAll("%[0-9A-Fa-f]{2}", CODE_SPECIAL_CHARACTER_REPLACEMENT)
                .replaceAll("\\\\u([0-9A-Fa-f]{2}){6}", CODE_SPECIAL_CHARACTER_REPLACEMENT)
                .replaceAll("\\\\u([0-9A-Fa-f]{2}){3}", CODE_SPECIAL_CHARACTER_REPLACEMENT);
    }

}
