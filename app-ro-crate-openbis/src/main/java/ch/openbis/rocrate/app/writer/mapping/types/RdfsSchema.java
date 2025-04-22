package ch.openbis.rocrate.app.writer.mapping.types;

import ch.eth.sis.rocrate.facade.IType;
import ch.eth.sis.rocrate.facade.PropertyType;

import java.util.List;

public class RdfsSchema
{
    public RdfsSchema(List<IType> classes, List<PropertyType> properties)
    {
        this.classes = classes;
        this.properties = properties;
    }

    List<IType> classes;

    List<PropertyType> properties;

    public List<IType> getClasses()
    {
        return classes;
    }

    public List<PropertyType> getProperties()
    {
        return properties;
    }
}
