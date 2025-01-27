package ch.eth.sis.rocrate.facade;

import java.util.ArrayList;
import java.util.List;

public class RdfsClass implements IRdfsClass
{
    String id;

    String type;

    List<String> subClassOf;

    List<String> ontologicalAnnotations;

    List<RdfsProperty> rdfsProperties;

    public RdfsClass()
    {
        this.subClassOf = new ArrayList<>();
        this.ontologicalAnnotations = new ArrayList<>();
        this.rdfsProperties = new ArrayList<>();

    }

    public String getId()
    {
        return id;
    }

    @Override
    public List<String> getSuperClasses()
    {
        return subClassOf;
    }

    @Override
    public List<String> getOntologicalAnnotations()
    {
        return ontologicalAnnotations;
    }

    /**
     * This is a convenience method for adding a property to a class.
     *
     */
    @Override
    public void addProperty(RdfsProperty rdfsProperty)
    {
        List<String> domainIncludes = rdfsProperty.getDomainIncludes();
        if (domainIncludes == null)
        {
            domainIncludes = new ArrayList<>();
            rdfsProperty.setDomainIncludes(domainIncludes);
        }
        if (id == null)
        {
            throw new IllegalArgumentException("Class id is null");
        }
        domainIncludes.add(id);
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getType()
    {
        return "rdfs:Class";
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setSubClassOf(List<String> subClassOf)
    {
        this.subClassOf = subClassOf;
    }

    public void setOntologicalAnnotations(List<String> ontologicalAnnotations)
    {
        this.ontologicalAnnotations = ontologicalAnnotations;
    }
}
