package ch.eth.sis.rocrate.writer.mappinginfo;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;

import java.util.List;
import java.util.Map;

public class MappingInfo
{
    Map<String, List<IEntityType>> rdfsToObjects;

    Map<String, List<IEntityType>> rdfsPropertiesUsedIn;

    public MappingInfo(Map<String, List<IEntityType>> rdfsToObjects,
            Map<String, List<IEntityType>> rdfsPropertiesUsedIn)
    {
        this.rdfsToObjects = rdfsToObjects;
        this.rdfsPropertiesUsedIn = rdfsPropertiesUsedIn;
    }

    public Map<String, List<IEntityType>> getRdfsToObjects()
    {
        return rdfsToObjects;
    }

    public Map<String, List<IEntityType>> getRdfsPropertiesUsedIn()
    {
        return rdfsPropertiesUsedIn;
    }
}
