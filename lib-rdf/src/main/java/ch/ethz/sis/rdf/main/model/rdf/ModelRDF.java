package ch.ethz.sis.rdf.main.model.rdf;

import ch.ethz.sis.rdf.main.model.xlsx.SampleObject;
import ch.ethz.sis.rdf.main.model.xlsx.SampleType;
import ch.ethz.sis.rdf.main.model.xlsx.VocabularyType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModelRDF
{
    public String ontNamespace;
    public String ontVersion;
    public Map<String, String> ontMetadata;
    public Map<String, String> nsPrefixes;
    public Map<String, OntClassExtension> stringOntClassExtensionMap;
    public Map<String, List<String>> RDFtoOpenBISDataType;
    public Map<String, List<String>> objectPropertyMap;
    public Map<String, List<ResourceRDF>> resourcesGroupedByType;
    public Map<String, List<SampleObject>> sampleObjectsGroupedByTypeMap;
    public List<VocabularyType> vocabularyTypeList;
    public Map<String, List<VocabularyType>> vocabularyTypeListGroupedByType;
    public Map<String, List<String>> subClassChanisMap;
    public List<SampleType> sampleTypeList;

    private Set<String> multiValueProperties = new LinkedHashSet<>();

    public Set<String> getMultiValueProperties()
    {
        return multiValueProperties;
    }

    public void setMultiValueProperties(Set<String> multiValueProperties)
    {
        this.multiValueProperties = multiValueProperties;
    }

    public boolean isSubClass(String uri)
    {
        return subClassChanisMap.containsKey(uri);
    }

    public boolean isASampleObject(String uri)
    {
        return sampleObjectsGroupedByTypeMap.containsKey(uri);
    }

    public boolean isPresentInVocType(String uri)
    {
        return vocabularyTypeListGroupedByType.containsKey(uri);
    }
}
