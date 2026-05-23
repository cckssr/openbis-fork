package ch.openbis.drive;


import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntity;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.fetchoptions.AbstractEntityFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.ICodeHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IIdentifierHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.AbstractEntitySearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.SearchDataSetsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.SearchDataSetsOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.SearchExperimentsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.SearchExperimentsOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.SynchronousOperationExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.SynchronousOperationExecutionResults;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SearchSamplesOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SearchSamplesOperationResult;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

//TODO Scratchpad to be removed
public class SearchScratchpad {

    public static String getDisplayName(AbstractEntity entity) {
        String identifier =  null;
        if (entity instanceof IIdentifierHolder) {
            identifier = ((IIdentifierHolder) entity).getIdentifier().getIdentifier();
        } else if (entity instanceof ICodeHolder) {
            identifier = ((ICodeHolder) entity).getCode();
        }
        String name = entity.getStringProperty("NAME");
        return identifier + ((name != null)? " (" + name + ")":"");
    }

    public static <E extends AbstractEntitySearchCriteria> E setEntityCriteria(E criteria, String searchText) {
        criteria.withCode().thatContains(searchText);
        criteria.withOrOperator();
        criteria.withStringProperty("NAME").thatContains(searchText);
        return criteria;
    }
    public static <E extends AbstractEntityFetchOptions> E setEntityFetchOptions(E options) {
        options.withProperties(); // We are going to need the Name property to populate the search results
        options.count(10); // Maximum 10 results are returned
        return options;
    }

    public static boolean isEntityDataMutable(Sample sample) {
        return !sample.isImmutableData();
    }
    public static boolean isEntityDataMutable(Experiment experiment) {
        return !experiment.isImmutableData();
    }
    public static boolean isEntityDataMutable(DataSet dataSet) {
        return false;
    }

    public static void searchExample() {
        OpenBIS openbis = new OpenBIS("https://openbis-sis-ci-sprint.ethz.ch");
//        openbis.setSessionToken();
        String sessionToken = openbis.login("admin", "changeit");
        System.out.println("sessionToken: " + sessionToken);

        //

        // Sample Search
        String searchText = "GEN";
        SearchResult<Sample> sampleSearchResult = openbis.searchSamples(setEntityCriteria(new SampleSearchCriteria(), searchText), setEntityFetchOptions(new SampleFetchOptions()));
        for (Sample sample:sampleSearchResult.getObjects()) {
            System.out.println("Sample Result: " + sample.getPermId().getPermId() + " " + isEntityDataMutable(sample) + " " + getDisplayName(sample));
        }

        //Experiment Search
        searchText = "Protocols";
        SearchResult<Experiment> experimentSearchResult = openbis.searchExperiments(setEntityCriteria(new ExperimentSearchCriteria(), searchText), setEntityFetchOptions(new ExperimentFetchOptions()));

        for (Experiment experiment:experimentSearchResult.getObjects()) {
            System.out.println("Experiment Result: " + experiment.getPermId().getPermId() + " " + isEntityDataMutable(experiment) + " " + getDisplayName(experiment));
        }

        //DataSet Search
        searchText = "Example";
        SearchResult<DataSet> dataSetSearchResult = openbis.searchDataSets(setEntityCriteria(new DataSetSearchCriteria(), searchText), setEntityFetchOptions(new DataSetFetchOptions()));

        for (DataSet dataset:dataSetSearchResult.getObjects()) {
            System.out.println("DataSet Result: " + dataset.getPermId().getPermId() + " " + " " + isEntityDataMutable(dataset) + " " + getDisplayName(dataset));
        }
        //

        openbis.logout();
    }

    public static void searchExampleBatchOperations() {
        OpenBIS openbis = new OpenBIS("https://openbis-sis-ci-sprint.ethz.ch");
//        openbis.setSessionToken();
        String sessionToken = openbis.login("admin", "changeit");
        System.out.println("sessionToken: " + sessionToken);

        // Unified server call

        String searchText = "GEN";
        SearchSamplesOperation searchSamplesOperation = new SearchSamplesOperation(setEntityCriteria(new SampleSearchCriteria(), searchText), setEntityFetchOptions(new SampleFetchOptions()));
        searchText = "Protocols";
        SearchExperimentsOperation searchExperimentsOperation = new SearchExperimentsOperation(setEntityCriteria(new ExperimentSearchCriteria(), searchText), setEntityFetchOptions(new ExperimentFetchOptions()));
        searchText = "Example";
        SearchDataSetsOperation searchDataSetsOperation = new SearchDataSetsOperation(setEntityCriteria(new DataSetSearchCriteria(), searchText), setEntityFetchOptions(new DataSetFetchOptions()));

        // If this takes more than 30 seconds a standard http proxy will just cut the connection, for long calls please use Async operations
        SynchronousOperationExecutionResults iOperationExecutionResults = (SynchronousOperationExecutionResults) openbis.executeOperations(List.of(searchSamplesOperation, searchExperimentsOperation, searchDataSetsOperation), new SynchronousOperationExecutionOptions());

        // Sample Search
        SearchSamplesOperationResult searchSamplesOperationResult = (SearchSamplesOperationResult) iOperationExecutionResults.getResults().get(0);
        for (Sample sample:(searchSamplesOperationResult.getSearchResult()).getObjects()) {
            System.out.println("Sample Result: " + sample.getPermId().getPermId() + " " + isEntityDataMutable(sample) + " " + getDisplayName(sample));
        }
        //Experiment Search
        SearchExperimentsOperationResult searchExperimentsOperationResult = (SearchExperimentsOperationResult) iOperationExecutionResults.getResults().get(1);
        for (Experiment experiment:searchExperimentsOperationResult.getSearchResult().getObjects()) {
            System.out.println("Experiment Result: " + experiment.getPermId().getPermId() + " " + isEntityDataMutable(experiment) + " " + getDisplayName(experiment));
        }
        //DataSet Search
        SearchDataSetsOperationResult searchDataSetsOperationResult = (SearchDataSetsOperationResult) iOperationExecutionResults.getResults().get(2);
        for (DataSet dataset:searchDataSetsOperationResult.getSearchResult().getObjects()) {
            System.out.println("DataSet Result: " + dataset.getPermId().getPermId() + " " + isEntityDataMutable(dataset) + " " + getDisplayName(dataset));
        }
        //

        openbis.logout();
    }

    public static void main(String[] args) throws Exception {
        searchExampleBatchOperations();
    }

    public static List<AbstractEntity> fakeAbstractEntities() {
        Experiment experiment = new Experiment();
        experiment.setIdentifier(new ExperimentIdentifier("exp-identifier"));
        experiment.setPermId(new ExperimentPermId("exp-perm-id"));
        experiment.setCode("exp-CODE");
        ExperimentFetchOptions experimentFetchOptions = new ExperimentFetchOptions();
        experimentFetchOptions.withProperties();
        experiment.setFetchOptions(experimentFetchOptions);
        experiment.setProperties(new HashMap<>());
        experiment.setStringProperty("NAME", "EXP");

        Experiment experiment2 = new Experiment();
        experiment2.setIdentifier(new ExperimentIdentifier("exp-identifier-2"));
        experiment2.setPermId(new ExperimentPermId("exp-perm-id-2"));
        experiment2.setCode("exp-CODE-TWO");
        experiment2.setImmutableDataDate(new Date());
        ExperimentFetchOptions experimentFetchOptions2 = new ExperimentFetchOptions();
        experimentFetchOptions2.withProperties();
        experiment2.setFetchOptions(experimentFetchOptions2);
        experiment2.setProperties(new HashMap<>());
        experiment2.setStringProperty("NAME", "EXP2");

        DataSet dataSet = new DataSet();
        dataSet.setPermId(new DataSetPermId("data-set-perm-id"));
        dataSet.setCode("exp-CODE");
        DataSetFetchOptions dataSetFetchOptions = new DataSetFetchOptions();
        dataSetFetchOptions.withProperties();
        dataSet.setFetchOptions(dataSetFetchOptions);
        dataSet.setProperties(new HashMap<>());
        dataSet.setStringProperty("NAME", "DATASET!!!");

        Sample sample = new Sample();
        sample.setIdentifier(new SampleIdentifier("sample-identifier"));
        sample.setPermId(new SamplePermId("sample-perm-id"));
        sample.setCode("sample-CODE");
        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withProperties();
        sample.setFetchOptions(sampleFetchOptions);
        sample.setProperties(new HashMap<>());
        sample.setStringProperty("NAME", "SAMPLEXXX");

        return List.of(experiment, experiment2, dataSet, sample);
    }
}
