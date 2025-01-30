package ch.openbis.rocrate.app.writer.mapping.types;

import ch.eth.sis.rocrate.facade.RdfsClass;
import ch.eth.sis.rocrate.facade.TypeProperty;

import java.util.List;

public class RdfsSchema
{
    public RdfsSchema(List<RdfsClass> classes, List<TypeProperty> properties)
    {
        this.classes = classes;
        this.properties = properties;
    }

    List<RdfsClass> classes;

    List<TypeProperty> properties;

    public List<RdfsClass> getClasses()
    {
        return classes;
    }

    public List<TypeProperty> getProperties()
    {
        return properties;
    }
}
