package ch.eth.sis.rocrate.writer.mapping.types;

import ch.eth.sis.rocrate.writer.mappinginfo.MappingInfo;

import java.util.List;

public class MapResult
{
    private final RdfsSchema schema;

    private final MappingInfo mappingInfo;

    private final List<MetaDataEntry> metaDataEntries;

    public MapResult(RdfsSchema schema, MappingInfo mappingInfo,
            List<MetaDataEntry> metaDataEntries)
    {
        this.schema = schema;
        this.mappingInfo = mappingInfo;
        this.metaDataEntries = metaDataEntries;
    }

    public RdfsSchema getSchema()
    {
        return schema;
    }

    public MappingInfo getMappingInfo()
    {
        return mappingInfo;
    }

    public List<MetaDataEntry> getMetaDataEntries()
    {
        return metaDataEntries;
    }
}
