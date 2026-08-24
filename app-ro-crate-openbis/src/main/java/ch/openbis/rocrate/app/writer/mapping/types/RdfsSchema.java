package ch.openbis.rocrate.app.writer.mapping.types;

import ch.eth.sis.rocrate.facade.IType;
import ch.eth.sis.rocrate.facade.IVocabularyType;
import ch.eth.sis.rocrate.facade.PropertyType;

import java.util.List;

public class RdfsSchema
{

    public RdfsSchema(List<IType> classes, List<PropertyType> properties,
            List<IVocabularyType> vocabularyTypes)
    {
        this.classes = classes;
        this.properties = properties;
        this.vocabularyTypes = vocabularyTypes;
    }

    List<IType> classes;

    List<PropertyType> properties;

    List<IVocabularyType> vocabularyTypes;

    public List<IType> getClasses()
    {
        return classes;
    }

    public List<PropertyType> getProperties()
    {
        return properties;
    }

    public List<IVocabularyType> getVocabularyTypes()
    {
        return vocabularyTypes;
    }
}
