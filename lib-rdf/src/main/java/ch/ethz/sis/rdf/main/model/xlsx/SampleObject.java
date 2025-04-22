package ch.ethz.sis.rdf.main.model.xlsx;

import java.util.ArrayList;
import java.util.List;

public class SampleObject
{
    public String identifier;
    public String code;
    public String pace;
    public String project;
    public String experiment;
    public String parents;
    public String children;
    public String name;

    public List<SampleObjectProperty> getProperties()
    {
        return properties;
    }

    public List<SampleObjectProperty> properties;
    public String typeURI;
    public String type;

    public SampleObject(String code, String typeURI, String type)
    {
        this.code = code;
        this.name = code;
        this.typeURI = typeURI;
        this.type = type;
        this.properties = new ArrayList<>();
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void addProperty(SampleObjectProperty sampleObjectProperty){
        this.properties.add(sampleObjectProperty);
    }

    @Override
    public String toString()
    {
        return "SampleObject{" +
                "identifier='" + identifier + '\'' +
                ", code='" + code + '\'' +
                ", pace='" + pace + '\'' +
                ", project='" + project + '\'' +
                ", experiment='" + experiment + '\'' +
                ", parents='" + parents + '\'' +
                ", children='" + children + '\'' +
                ", name='" + name + '\'' +
                ", properties=" + properties +
                ", typeURI='" + typeURI + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
