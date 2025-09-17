# Semantic Annotations

- Introduction
    - Classic Nomenclature
    - Modern Nomenclature 
    - openBIS Nomenclature 
- openBIS Implementation : Java Examples
    - Use Case 1 : Annotating a Semantic Class corresponds to Annotating an openBIS Type
    - Use Case 2 : Annotating a Semantic Class Property corresponds to Annotating an openBIS Property Assignment
    - Use Case 3 : Annotating a Semantic Property corresponds to Annotating an openBIS Property
    - Search Based on Semantic Annotations
    - Helper Class - Semantic API Extensions

## Introduction
Semantic annotations were created as a means of standardisation to provide interoperability between systems.
Different systems can then name the same kinds of data differently, but providing the same semantic annotations systems could theoretically interoperate.
One of the biggest hurdles to overcome when trying to understand semantic annotations is the nomenclature. There is a plethora of information often using different terms.
We will now introduce the main three nomenclatures to help the reader to familiarise with them.

Classic Nomenclature
This is typically found in literature.

![Semantic Annotations Classic Nomenclature](img/semantic-annotations-classic-nomenclature.png "Semantic Annotations Classic Nomenclature")

For reference: http://www.linkeddatatools.com/semantic-web-basics

Modern Nomenclature 
This is typically found in standards.

![Semantic Annotations Modern Nomenclature](img/semantic-annotations-modern-nomenclature.png "Semantic Annotations Modern Nomenclature")

For reference: https://www.biomedit.ch/rdf/sphn-ontology/sphn/2022/2

openBIS Nomenclature 
This is typically found in systems or programming languages.

![Semantic Annotations openBIS Nomenclature](img/semantic-annotations-openbis-nomenclature.png "Semantic Annotations openBIS Nomenclature")

openBIS Implementation : Java Examples

## Use Case 1 : Annotating a Semantic Class corresponds to Annotating an openBIS Type
openBIS allows you to annotate Types using semantic annotation URIs through the use of its API using SemanticAnnotationCreation. 
In the Example below:
The openBIS Sample Type ADMINISTRATIVE_GENDER is annotated with the semantic class AdministrativeGender.
package ch.ethz.sis.pat;
 
```java
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.create.SemanticAnnotationCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.id.SemanticAnnotationPermId;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
 
import java.util.List;
import java.util.Map;
 
public class MainSemantic {
 
    private static final String URL = "https://openbis-sis-ci-sprint.ethz.ch/openbis/openbis" + IApplicationServerApi.SERVICE_URL;
    private static final int TIMEOUT = 10000;
 
    private static final String USER = "admin";
    private static final String PASSWORD = "changeit";
 
    public static void main(String[] args) {
        IApplicationServerApi v3 = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class, URL, TIMEOUT);
        String sessionToken = v3.login(USER, PASSWORD);
 
        System.out.println("sessionToken: " + sessionToken);
 
        // Creating semantic annotations using helper methods
        SemanticAnnotationCreation administrative_gender = ApplicationServerSemanticAPIExtensions.getSemanticSubjectCreation(
                EntityKind.SAMPLE,
                "ADMINISTRATIVE_GENDER",
                "https://biomedit.ch/rdf/sphn-ontology/sphn",
                "https://biomedit.ch/rdf/sphn-ontology/sphn/2022/2",
                "https://biomedit.ch/rdf/sphn-ontology/sphn#AdministrativeGender");
 
        List<SemanticAnnotationPermId> annotations = v3.createSemanticAnnotations(sessionToken, List.of(administrative_gender));
        System.out.println("created annotations: " + annotations);
 
        v3.logout(sessionToken);
    }
}
```

## Use Case 2 : Annotating a Semantic Class Property corresponds to Annotating an openBIS Property Assignment
openBIS allows you to annotate Property Assignments using semantic annotation URIs through the use of its API using SemanticAnnotationCreation. 
In the Example below:
The openBIS Property Type IDENTIFIER of the Type ADMINISTRATIVE_GENDER is annotated with the semantic property hasIdentifier.
package ch.ethz.sis.pat;
 
```java
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.create.SemanticAnnotationCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.id.SemanticAnnotationPermId;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
 
import java.util.List;
import java.util.Map;
 
public class MainSemantic {
 
    private static final String URL = "https://openbis-sis-ci-sprint.ethz.ch/openbis/openbis" + IApplicationServerApi.SERVICE_URL;
    private static final int TIMEOUT = 10000;
 
    private static final String USER = "admin";
    private static final String PASSWORD = "changeit";
 
    public static void main(String[] args) {
        IApplicationServerApi v3 = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class, URL, TIMEOUT);
        String sessionToken = v3.login(USER, PASSWORD);
 
        System.out.println("sessionToken: " + sessionToken);         
        SemanticAnnotationCreation  identifier = ApplicationServerSemanticAPIExtensions.getSemanticPredicateWithSubjectCreation(
                EntityKind.SAMPLE,
                "ADMINISTRATIVE_GENDER",
                "identifier",
                "https://biomedit.ch/rdf/sphn-ontology/sphn",
                "https://biomedit.ch/rdf/sphn-ontology/sphn/2022/2",
                "https://biomedit.ch/rdf/sphn-ontology/sphn#hasIdentifier");         
        List<SemanticAnnotationPermId> annotations = v3.createSemanticAnnotations(sessionToken, List.of(identifier));
        System.out.println("created annotations: " + annotations);
 
        v3.logout(sessionToken);
    }
}
```

## Use Case 3 : Annotating a Semantic Property corresponds to Annotating an openBIS Property
Even if less common, sometimes we can be under the need of annotating a predicate without a subject.
openBIS allows you to annotate Property Types without Types using semantic annotation URIs through the use of its API using SemanticAnnotationCreation. 
In the Example below:
The openBIS Property Type IDENTIFIER is annotated with the semantic property hasIdentifier.
package ch.ethz.sis.pat;

```java
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.create.SemanticAnnotationCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.id.SemanticAnnotationPermId;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
 
import java.util.List;
import java.util.Map;
 
public class MainSemantic {
 
    private static final String URL = "https://openbis-sis-ci-sprint.ethz.ch/openbis/openbis" + IApplicationServerApi.SERVICE_URL;
    private static final int TIMEOUT = 10000;
 
    private static final String USER = "admin";
    private static final String PASSWORD = "changeit";
 
    public static void main(String[] args) {
        IApplicationServerApi v3 = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class, URL, TIMEOUT);
        String sessionToken = v3.login(USER, PASSWORD);
 
        System.out.println("sessionToken: " + sessionToken);         
        SemanticAnnotationCreation  identifierOnly = ApplicationServerSemanticAPIExtensions.getSemanticPredicateCreation(
                "identifier",
                "https://biomedit.ch/rdf/sphn-ontology/sphn",
                "https://biomedit.ch/rdf/sphn-ontology/sphn/2022/2",
                "https://biomedit.ch/rdf/sphn-ontology/sphn#hasIdentifier");      
        List<SemanticAnnotationPermId> annotations = v3.createSemanticAnnotations(sessionToken, List.of(identifierOnly));
        System.out.println("created annotations: " + annotations);
 
        v3.logout(sessionToken);
    }
}
```

## Search Based on Semantic Annotations
openBIS search doesn't directly allow a search based on semantic annotations. This is currently a two steps process.
On the next example we reduce this two step process to a single call for what we believe is the most common use case: Search for subject and predicate of a well known ontology. 
We build this example on top of the previous example. We would like now to make a search based on the previously created subject (AdministrativeGender) and predicate (hasIdentifier) to finally obtain Samples having the identifier "12345678".
The reader will appreciate that this search can be done without having previous knowledge of how this semantic annotations map to openBIS types and property types. See example below.
package ch.ethz.sis.pat;

```java
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.create.SemanticAnnotationCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.id.SemanticAnnotationPermId;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
 
import java.util.List;
import java.util.Map;
 
public class MainSemantic {
 
    private static final String URL = "https://openbis-sis-ci-sprint.ethz.ch/openbis/openbis" + IApplicationServerApi.SERVICE_URL;
    private static final int TIMEOUT = 10000;
 
    private static final String USER = "admin";
    private static final String PASSWORD = "changeit";
 
    public static void main(String[] args) {
        IApplicationServerApi v3 = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class, URL, TIMEOUT);
        String sessionToken = v3.login(USER, PASSWORD);
 
        System.out.println("sessionToken: " + sessionToken);
 
        // Searching semantic annotations using helper methods
        SearchResult searchResult = ApplicationServerSemanticAPIExtensions.searchEntityWithSemanticAnnotations(v3, sessionToken,
                EntityKind.SAMPLE,
                "https://biomedit.ch/rdf/sphn-ontology/sphn#AdministrativeGender",
                Map.of("https://biomedit.ch/rdf/sphn-ontology/sphn#hasIdentifier", "12345678"),
                0,
                Integer.MAX_VALUE
        );
 
        System.out.println("Found Entities: " + searchResult.getTotalCount());
        v3.logout(sessionToken);
    }
}
```

## Helper Class - Semantic API Extensions
To facilitate previous straightforward examples we have created the next utility class:
package ch.ethz.sis.pat;
 
```java
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.fetchoptions.FetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.AbstractEntitySearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyAssignmentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.create.SemanticAnnotationCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.search.SemanticAnnotationSearchCriteria;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
 
import java.util.Map;
 
public abstract class ApplicationServerSemanticAPIExtensions {
 
    /**
     * This utility method provides a simplified API to create subject semantic annotations
     *
     */
    public static SemanticAnnotationCreation getSemanticSubjectCreation(    EntityKind subjectEntityKind,
                                                                            String subjectClass,
                                                                             String subjectClassOntologyId,
                                                                             String subjectClassOntologyVersion,
                                                                             String subjectClassId) {
        SemanticAnnotationCreation semanticAnnotationCreation = new SemanticAnnotationCreation();
        // Subject: Type matching an ontology class
        semanticAnnotationCreation.setEntityTypeId(new EntityTypePermId(subjectClass, subjectEntityKind));
        // Ontology URL
        semanticAnnotationCreation.setPredicateOntologyId(subjectClassOntologyId);
        // Ontology Version URL
        semanticAnnotationCreation.setPredicateOntologyVersion(subjectClassOntologyVersion);
        // Ontology Class URL
        semanticAnnotationCreation.setPredicateAccessionId(subjectClassId);
        return semanticAnnotationCreation;
    }
 
    /**
     * This utility method provides a simplified API to create predicate semantic annotations
     *
     */
    public static SemanticAnnotationCreation getSemanticPredicateWithSubjectCreation( EntityKind subjectEntityKind,
                                                                            String subjectClass,
                                                                            String predicateProperty,
                                                                            String predicatePropertyOntologyId,
                                                                            String predicatePropertyOntologyVersion,
                                                                            String predicatePropertyId) {
        SemanticAnnotationCreation semanticAnnotationCreation = new SemanticAnnotationCreation();
        // Subject: Type matching an ontology class
        // Predicate: Property matching an ontology class property
        semanticAnnotationCreation.setPropertyAssignmentId(new PropertyAssignmentPermId(
                new EntityTypePermId(subjectClass, subjectEntityKind),
                new PropertyTypePermId(predicateProperty)));
        // Ontology URL
        semanticAnnotationCreation.setPredicateOntologyId(predicatePropertyOntologyId);
        // Ontology Version URL
        semanticAnnotationCreation.setPredicateOntologyVersion(predicatePropertyOntologyVersion);
        // Ontology Property URL
        semanticAnnotationCreation.setPredicateAccessionId(predicatePropertyId);
        return semanticAnnotationCreation;
    }
 
    /**
     * This utility method provides a simplified API to create predicate semantic annotations
     *
     */
    public static SemanticAnnotationCreation getSemanticPredicateCreation( String predicateProperty,
                                                                                      String predicatePropertyOntologyId,
                                                                                      String predicatePropertyOntologyVersion,
                                                                                      String predicatePropertyId) {
        SemanticAnnotationCreation semanticAnnotationCreation = new SemanticAnnotationCreation();
        // Predicate: Property matching an ontology class property
        semanticAnnotationCreation.setPropertyTypeId(new PropertyTypePermId(predicateProperty));
        // Ontology URL
        semanticAnnotationCreation.setPredicateOntologyId(predicatePropertyOntologyId);
        // Ontology Version URL
        semanticAnnotationCreation.setPredicateOntologyVersion(predicatePropertyOntologyVersion);
        // Ontology Property URL
        semanticAnnotationCreation.setPredicateAccessionId(predicatePropertyId);
        return semanticAnnotationCreation;
    }
 
    /**
     * This utility method provides a simplified API to search based on semantic subjects and predicates
     *
     * @throws UserFailureException in case of any problems
     */
    public static SearchResult searchEntityWithSemanticAnnotations(IApplicationServerApi v3,
                                                                           String sessionToken,
                                                                           EntityKind entityKind,
                                                                           String subjectClassIDOrNull,
                                                                           Map<String, String> predicatePropertyIDsOrNull,
                                                                           Integer fromOrNull,
                                                                           Integer countOrNull) {
        if (entityKind == null) {
            throw new UserFailureException("entityKind cannot be null");
        }
        if (entityKind == EntityKind.DATA_SET) {
            throw new UserFailureException("EntityKind.DATA_SET is not supported");
        }
 
        //
        // Part 1 : Translate semantic classes and properties into openBIS types and property types
        //
 
        SemanticAnnotationSearchCriteria semanticCriteria = new SemanticAnnotationSearchCriteria();
        semanticCriteria.withOrOperator();
 
        SemanticAnnotationFetchOptions semanticFetchOptions = new SemanticAnnotationFetchOptions();
 
        // Request and collect subjects
        if (subjectClassIDOrNull != null) {
            semanticCriteria.withPredicateAccessionId().thatEquals(subjectClassIDOrNull);
        }
        semanticFetchOptions.withEntityType();
 
        // Request and collect predicates
        if (predicatePropertyIDsOrNull != null) {
            for (String predicate : predicatePropertyIDsOrNull.keySet()) {
                semanticCriteria.withPredicateAccessionId().thatEquals(predicate);
            }
        }
        PropertyAssignmentFetchOptions propertyAssignmentFetchOptions = semanticFetchOptions.withPropertyAssignment();
        propertyAssignmentFetchOptions.withPropertyType();
        propertyAssignmentFetchOptions.withEntityType();
 
        SearchResult<SemanticAnnotation> semanticAnnotationSearchResult = v3.searchSemanticAnnotations(sessionToken, new SemanticAnnotationSearchCriteria(), semanticFetchOptions);
 
        //
        // Part 2 : Create openBIS search matching semantic results
        //
 
        AbstractEntitySearchCriteria criteria = getEntitySearchCriteria(entityKind);
        criteria.withAndOperator();
 
        // Set Subject
        String entityTypeCode = null;
        for (SemanticAnnotation semanticAnnotation:semanticAnnotationSearchResult.getObjects()) {
            if (semanticAnnotation.getEntityType() != null) {
                EntityTypePermId permId = (EntityTypePermId) semanticAnnotation.getEntityType().getPermId();
                if (permId.getEntityKind() == entityKind) {
                    entityTypeCode = semanticAnnotation.getEntityType().getCode();
                    setWithTypeThatEquals(entityKind, criteria, entityTypeCode);
                }
            }
        }
 
        if (entityTypeCode == null) {
            throw new UserFailureException("Entity Type matching Subject not found.");
        }
 
        // Set Predicates matching the Subject
        if (predicatePropertyIDsOrNull != null) {
            int predicatesFound = 0;
            for (SemanticAnnotation semanticAnnotation : semanticAnnotationSearchResult.getObjects()) {
                if (semanticAnnotation.getPropertyAssignment() != null &&
                        semanticAnnotation.getPropertyAssignment().getEntityType().getCode().equals(entityTypeCode)) {
                    EntityTypePermId permId = (EntityTypePermId) semanticAnnotation.getPropertyAssignment().getEntityType().getPermId();
                    if (permId.getEntityKind() == entityKind) {
                        String value = predicatePropertyIDsOrNull.get(semanticAnnotation.getPredicateAccessionId());
                        criteria.withProperty(semanticAnnotation.getPropertyAssignment().getPropertyType().getCode()).thatEquals(value);
                        predicatesFound++;
                    }
                }
            }
 
            if (predicatesFound != predicatePropertyIDsOrNull.size()) {
                throw new UserFailureException("Property Types matching Predicates not found.");
            }
        }
 
        FetchOptions fetchOptions = getEntityFetchOptions(entityKind);
        if (fromOrNull != null) {
            fetchOptions.from(fromOrNull);
        }
        if (countOrNull != null) {
            fetchOptions.count(countOrNull);
        }
 
        SearchResult searchResult = getSearchResult(v3, sessionToken, entityKind, criteria, fetchOptions);
        return searchResult;
    }
 
    private static void setWithTypeThatEquals(EntityKind entityKind, AbstractEntitySearchCriteria criteria, String entityTypeCode) {
        switch (entityKind) {
            case EXPERIMENT:
                ((ExperimentSearchCriteria) criteria).withType().withCode().thatEquals(entityTypeCode);
                break;
            case SAMPLE:
                ((SampleSearchCriteria) criteria).withType().withCode().thatEquals(entityTypeCode);
                break;
            case DATA_SET:
                ((DataSetSearchCriteria) criteria).withType().withCode().thatEquals(entityTypeCode);
                break;
        }
    }
 
    private static AbstractEntitySearchCriteria getEntitySearchCriteria(EntityKind entityKind) {
        AbstractEntitySearchCriteria criteria = null;
        switch (entityKind) {
            case EXPERIMENT:
                criteria = new ExperimentSearchCriteria();
                break;
            case SAMPLE:
                criteria = new SampleSearchCriteria();
                break;
            case DATA_SET:
                criteria = new DataSetSearchCriteria();
                break;
        }
        return criteria;
    }
 
    private static FetchOptions getEntityFetchOptions(EntityKind entityKind) {
        FetchOptions fetchOptions = null;
        switch (entityKind) {
            case EXPERIMENT:
                fetchOptions = new ExperimentFetchOptions();
                break;
            case SAMPLE:
                fetchOptions = new SampleFetchOptions();
                break;
            case DATA_SET:
                fetchOptions = new DataSetFetchOptions();
                break;
        }
        return fetchOptions;
    }
 
    private static SearchResult getSearchResult(IApplicationServerApi v3, String sessionToken, EntityKind entityKind, AbstractEntitySearchCriteria criteria, FetchOptions fetchOptions) {
        SearchResult searchResult = null;
        switch (entityKind) {
            case EXPERIMENT:
                searchResult = v3.searchExperiments(sessionToken, (ExperimentSearchCriteria) criteria, (ExperimentFetchOptions) fetchOptions);
                break;
            case SAMPLE:
                searchResult = v3.searchSamples(sessionToken, (SampleSearchCriteria) criteria, (SampleFetchOptions) fetchOptions);
                break;
            case DATA_SET:
                searchResult = v3.searchDataSets(sessionToken, (DataSetSearchCriteria) criteria, (DataSetFetchOptions) fetchOptions);
                break;
        }
        return searchResult;
    }
}
```