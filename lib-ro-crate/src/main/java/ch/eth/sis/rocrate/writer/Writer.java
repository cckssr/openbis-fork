package ch.eth.sis.rocrate.writer;

import ch.eth.sis.rocrate.parser.results.ParseResult;
import ch.eth.sis.rocrate.writer.mapping.Mapper;
import ch.eth.sis.rocrate.writer.mapping.types.MapResult;
import ch.eth.sis.rocrate.writer.mapping.types.MetaDataEntry;
import ch.eth.sis.rocrate.writer.mapping.types.RdfsClass;
import ch.eth.sis.rocrate.writer.mapping.types.RdfsProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import edu.kit.datamanager.ro_crate.writer.FolderWriter;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;

public class Writer
{
    public static final String PREFIX_SYSTEM = "openbis-system";

    public static final String PREFIX_SCHEMA = "openbis-schema";

    public static final String PREFIX_METADATA = "openbis-metadata";

    private static final String TYPE = "@type";

    public static final String NAMESPACE_SEPARATOR = ":";

    public static final String SYSTEM_SPACE = PREFIX_SYSTEM + NAMESPACE_SEPARATOR + "Space";

    public static final String SYSTEM_OBJECT = PREFIX_SYSTEM + NAMESPACE_SEPARATOR + "Object";

    public static final String SYSTEM_COLLECTION =
            PREFIX_SYSTEM + NAMESPACE_SEPARATOR + "Collection";

    public static final String SYSTEM_PROJECT = PREFIX_SYSTEM + NAMESPACE_SEPARATOR + "Project";

    public static final String SYSTEM_DATASET = PREFIX_SYSTEM + NAMESPACE_SEPARATOR + "Dataset";

    public static final String SYSTEM_VOCABULARY =
            PREFIX_SYSTEM + NAMESPACE_SEPARATOR + "Vocabulary";

    private final static String RDFS_CLASS = "rdfs:Class";

    private final static String RDFS_PROPERTY = "rdfs:Property";

    public void write(ParseResult parseResult, Path outPath) throws JsonProcessingException
    {
        RoCrate.RoCrateBuilder builder =
                new RoCrate.RoCrateBuilder("name", "description", "2024-12-04T07:53:11Z",
                        "licenseIdentifier");
        addSystemSchema(builder);
        Mapper mapper = new Mapper();
        var rdfsRepresentation =
                mapper.transform(parseResult);
        RoCrate roCrate = builder.build();
        addSchema(builder, rdfsRepresentation);
        addMetaData(builder, rdfsRepresentation);
        FolderWriter folderRoCrateWriter = new FolderWriter();
        folderRoCrateWriter.save(roCrate, "ro_out");



    }

    private void addSystemSchema(RoCrate.RoCrateBuilder crateBuilder)
    {

        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.setId(SYSTEM_SPACE);
            builder.addProperty(TYPE, RDFS_CLASS);
            crateBuilder.addDataEntity(builder.build());
        }
        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.setId(SYSTEM_OBJECT);
            builder.addProperty(TYPE, RDFS_CLASS);
            crateBuilder.addDataEntity(builder.build());
        }
        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.setId(SYSTEM_COLLECTION);
            builder.addProperty(TYPE, RDFS_CLASS);
            crateBuilder.addDataEntity(builder.build());
        }
        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.setId(SYSTEM_PROJECT);
            builder.addProperty(TYPE, RDFS_CLASS);
            crateBuilder.addDataEntity(builder.build());
        }
        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.setId(SYSTEM_DATASET);
            builder.addProperty(TYPE, RDFS_CLASS);
            crateBuilder.addDataEntity(builder.build());
        }
        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.setId(SYSTEM_VOCABULARY);
            builder.addProperty(TYPE, RDFS_CLASS);
            crateBuilder.addDataEntity(builder.build());
        }

    }

    private void addSchema(RoCrate.RoCrateBuilder crateBuilder, MapResult mapResult)
    {
        for (RdfsClass rdfsClass : mapResult.getSchema().getClasses())
        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.addProperty("@id", rdfsClass.getId());
            builder.addProperty("@type", RDFS_CLASS);
            if (rdfsClass.getSubClassOf() != null)
            {
                builder.addIdProperty("rdfs:subClassOf", rdfsClass.getSubClassOf().getId());
            }

            crateBuilder.addDataEntity(builder.build());

        }
        for (RdfsProperty rdfsProperty : mapResult.getSchema().getProperties())
        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            boolean asasd = StringUtils.isBlank(rdfsProperty.getId());

            builder.setId(rdfsProperty.getId());
            builder.addProperty("@type", RDFS_PROPERTY);

            rdfsProperty.getRangeIncludes().stream().distinct()
                    .forEach(x -> builder.addIdProperty("schema:rangeIncludes", x.getId()));
            rdfsProperty.getDomainIncludes().stream().distinct()
                    .forEach(x -> builder.addIdProperty("schema:domainIncludes", x.getId()));
            crateBuilder.addDataEntity(builder.build());
        }

    }

    private void addMetaData(RoCrate.RoCrateBuilder crateBuilder, MapResult mapResult)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        for (MetaDataEntry metaDataEntry : mapResult.getMetaDataEntries())
        {
            DataEntity.DataEntityBuilder builder = new DataEntity.DataEntityBuilder();
            builder.setId(metaDataEntry.getId());
            builder.addProperty("@type", metaDataEntry.getType());

            metaDataEntry.getProps().forEach((s, o) -> {
                if (o instanceof Double)
                {
                    builder.addProperty(s, (Double) o);
                } else if (o instanceof Integer)
                {
                    builder.addProperty(s, (Integer) o);
                } else if (o instanceof Boolean)
                {
                    builder.addProperty(s, (Boolean) o);
                } else if (o instanceof String)
                {
                    builder.addProperty(s, o.toString());
                } else if (o == null)
                {
                    builder.addProperty(s, objectMapper.nullNode());
                }
            });
            metaDataEntry.getChildrenIdentifiers()
                    .forEach(x -> builder.addIdProperty("children", x));
            metaDataEntry.getParentIdentifiers().forEach(x -> builder.addIdProperty("parents", x));
            crateBuilder.addDataEntity(builder.build());



        }

    }

}
