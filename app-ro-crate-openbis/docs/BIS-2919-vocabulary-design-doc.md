# Vocabularies in RO-Crate Schema Plus

https://ethsis.atlassian.net/browse/BIS-2919

## RO-Crate, owl:oneOf

Property, the `rangeIncludes` points to an `owlClass` for a vocabulary. This in turn points to the
individual terms.

```json
{
  "@context": {},
  "@graph": [
    {
      "@id": "openBIS:hasTERMINSTRUMENT",
      "@type": "rdf:Property",
      "schema:rangeIncludes": {
        "@id": "openBIS:termINSTRUMENT"
      }
    },
    {
      "@id": "openBIS:termINSTRUMENT",
      "@type": "owl:Class",
      "owl:oneOf": [
        {
          "@id": "openBIS:termINSTRUMENT_CALORIMETER"
        },
        {
          "@id": "openBIS:termINSTRUMENT_GAS_CHROMATOGRAPH"
        }
      ]
    },
    {
      "@id": "openBIS:termINSTRUMENT_CALORIMETER",
      "@type": "owl:NamedIndividual",
      "rdfs:label": "Calorimeter"
    },
    {
      "@id": "openBIS:termINSTRUMENT_GAS_CHROMATOGRAPH",
      "@type": "owl:NamedIndividual",
      "rdfs:label": "Gas chromatograph"
    }
  ]
}
```

## Facade

Add a new type interface: `IVocabularytype` and `IVocabularyTerm` for values.
VocabularyTypes are addressed from the IType.

```java
interface IVocabularyType
{
    String getId();

    String getLabel();

    List<IVocabularyTerm> getTerms();
}

interface IVocabularyTerm
{
    String getId();

    String getLabel();
}


```

`Itype` gets a new method:

```java
interface IPropertyType
{

    void addVocabularyType(IVocabularyType);

}


```