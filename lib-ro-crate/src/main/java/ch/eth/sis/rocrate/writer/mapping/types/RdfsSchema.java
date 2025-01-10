package ch.eth.sis.rocrate.writer.mapping.types;

import java.util.List;

public class RdfsSchema
{
    public RdfsSchema(List<RdfsClass> classes, List<RdfsProperty> properties)
    {
        this.classes = classes;
        this.properties = properties;
    }

    List<RdfsClass> classes;

    List<RdfsProperty> properties;

    public List<RdfsClass> getClasses()
    {
        return classes;
    }

    public List<RdfsProperty> getProperties()
    {
        return properties;
    }
}
