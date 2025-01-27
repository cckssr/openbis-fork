package writer.mapping.types;

import ch.eth.sis.rocrate.facade.RdfsClass;
import ch.eth.sis.rocrate.facade.RdfsProperty;

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
