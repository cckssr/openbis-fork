package ch.eth.sis.rocrate.writer.mapping.types;

public class RdfsClass
{
    String id;

    String type;

    JsonLdId subClassOf;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public JsonLdId getSubClassOf()
    {
        return subClassOf;
    }

    public void setSubClassOf(JsonLdId subClassOf)
    {
        this.subClassOf = subClassOf;
    }
}
