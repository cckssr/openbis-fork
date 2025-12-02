package ch.ethz.sis.rdf.main.parser;

import ch.ethz.sis.rdf.main.model.xlsx.SampleObject;
import ch.ethz.sis.rdf.main.model.xlsx.SamplePropertyType;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;
import java.util.Set;

public class ResourceParsingResult
{
    private final List<SampleObject> deletedObjects;

    private final List<SampleObject> unchangedObjects;

    private final List<SampleObject> editedObjects;

    private final Set<String> classesImported;

    private final List<String> propertiesImported;

    private Set<Triple<SampleObject, SamplePropertyType, String>> danglingReferences;

    private List<VocabRepairInfo> vocabRepairThingies;

    public ResourceParsingResult(List<SampleObject> deletedObjects,
            List<SampleObject> unchangedObjects,
            List<SampleObject> editedObjects, Set<String> classesImported,
            List<String> propertiesImported,
            Set<Triple<SampleObject, SamplePropertyType, String>> danglingReferences,
            List<VocabRepairInfo> vocabRepairThingies)
    {
        this.deletedObjects = deletedObjects;
        this.unchangedObjects = unchangedObjects;
        this.editedObjects = editedObjects;
        this.classesImported = classesImported;
        this.propertiesImported = propertiesImported;
        this.danglingReferences = danglingReferences;
        this.vocabRepairThingies = vocabRepairThingies;
    }

    public Set<Triple<SampleObject, SamplePropertyType, String>> getDanglingReferences()
    {
        return danglingReferences;
    }

    public void setDanglingReferences(
            Set<Triple<SampleObject, SamplePropertyType, String>> danglingReferences)
    {
        this.danglingReferences = danglingReferences;
    }

    public List<SampleObject> getDeletedObjects()
    {
        return deletedObjects;
    }

    public List<SampleObject> getEditedObjects()
    {
        return editedObjects;
    }

    public List<SampleObject> getUnchangedObjects()
    {
        return unchangedObjects;
    }

    public Set<String> getClassesImported()
    {
        return classesImported;
    }

    public List<String> getPropertiesImported()
    {
        return propertiesImported;
    }

    public List<VocabRepairInfo> getVocabRepairThingies()
    {
        return vocabRepairThingies;
    }

    public static class VocabRepairInfo
    {
        SampleObject sampleObject;

        SamplePropertyType stuff;

        String value;

        public SampleObject getSampleObject()
        {
            return sampleObject;
        }

        public SamplePropertyType getPropertyType()
        {
            return stuff;
        }

        public String getValue()
        {
            return value;
        }

        public VocabRepairInfo(SampleObject sampleObject, SamplePropertyType stuff, String value)
        {
            this.sampleObject = sampleObject;
            this.stuff = stuff;
            this.value = value;
        }
    }
}
