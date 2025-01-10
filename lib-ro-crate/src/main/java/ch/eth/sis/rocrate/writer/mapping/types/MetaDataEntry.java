package ch.eth.sis.rocrate.writer.mapping.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MetaDataEntry
{
    String id;

    String type;

    Map<String, Object> props;

    List<String> childrenIdentifiers = new ArrayList<>();

    List<String> parentIdentifiers = new ArrayList<>();

    public MetaDataEntry()
    {
    }

    public MetaDataEntry(String id, String type, Map<String, Object> props)
    {
        this.id = id;
        this.type = type;
        this.props = props;
    }

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

    public Map<String, Object> getProps()
    {
        return props;
    }

    public void setProps(Map<String, Object> props)
    {
        this.props = props;
    }

    public void addChildIdentifier(String a)
    {
        childrenIdentifiers.add(a);
    }

    public void addParentIdentifier(String a)
    {
        parentIdentifiers.add(a);
    }

    public List<String> getChildrenIdentifiers()
    {
        return childrenIdentifiers;
    }

    public List<String> getParentIdentifiers()
    {
        return parentIdentifiers;
    }
}
