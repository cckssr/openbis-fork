package ch.eth.sis.rocrate.writer.mapping.types;

import java.util.List;

public class RdfsProperty
{
    List<JsonLdId> domainIncludes;

    List<JsonLdId> rangeIncludes;

    String id;

    public List<JsonLdId> getDomainIncludes()
    {
        return domainIncludes;
    }

    public void setDomainIncludes(List<JsonLdId> domainIncludes)
    {
        this.domainIncludes = domainIncludes;
    }

    public List<JsonLdId> getRangeIncludes()
    {
        return rangeIncludes;
    }

    public void setRangeIncludes(List<JsonLdId> rangeIncludes)
    {
        this.rangeIncludes = rangeIncludes;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }
}
