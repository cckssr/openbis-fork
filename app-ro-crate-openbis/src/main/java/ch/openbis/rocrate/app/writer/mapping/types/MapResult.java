package ch.openbis.rocrate.app.writer.mapping.types;

import ch.eth.sis.rocrate.facade.MetadataEntry;
import ch.openbis.rocrate.app.writer.mappinginfo.MappingInfo;

import java.nio.file.Path;
import java.util.List;

public class MapResult
{
    private final RdfsSchema schema;

    private final MappingInfo mappingInfo;

    private final List<MetadataEntry> metaDataEntries;

    public List<RoCrateFile> files;

    public MapResult(RdfsSchema schema, MappingInfo mappingInfo,
            List<MetadataEntry> metaDataEntries, List<RoCrateFile> files)
    {
        this.schema = schema;
        this.mappingInfo = mappingInfo;
        this.metaDataEntries = metaDataEntries;
        this.files = files;
    }

    public RdfsSchema getSchema()
    {
        return schema;
    }

    public MappingInfo getMappingInfo()
    {
        return mappingInfo;
    }

    public List<MetadataEntry> getMetaDataEntries()
    {
        return metaDataEntries;
    }

    public List<RoCrateFile> getFiles()
    {
        return files;
    }

    public record RoCrateFile(Path path, String id)
    {
    }


}
