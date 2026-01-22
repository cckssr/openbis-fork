package ch.openbis.drive.util;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntity;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.fetchoptions.AbstractEntityFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.ICodeHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IIdentifierHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.operation.IOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.AbstractEntitySearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchObjectsOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.SearchDataSetsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.SearchDataSetsOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.SearchExperimentsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.SearchExperimentsOperationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.SynchronousOperationExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.SynchronousOperationExecutionResults;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SearchSamplesOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SearchSamplesOperationResult;
import com.google.common.collect.Streams;
import lombok.NonNull;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class OpenBISQueryUtil {
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

    public static String getEntityPermId(AbstractEntity entity) {
        if (entity instanceof Sample) {
            return ((Sample) entity).getPermId().getPermId();
        } else if (entity instanceof Experiment) {
            return ((Experiment) entity).getPermId().getPermId();
        } else if (entity instanceof DataSet) {
            return ((DataSet) entity).getPermId().getPermId();
        } else {
            return "";
        }
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

    public static boolean isEntityDataMutable(AbstractEntity abstractEntity) {
        if (abstractEntity instanceof Sample) {
            return isEntityDataMutable((Sample) abstractEntity);
        } else if (abstractEntity instanceof Experiment) {
            return isEntityDataMutable((Experiment) abstractEntity);
        } else if (abstractEntity instanceof DataSet) {
            return isEntityDataMutable((DataSet) abstractEntity);
        } else {
            return false;
        }
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


    public static List<AbstractEntity> searchSynchronizableOpenBISEntities(@NonNull String openBISUrl, @NonNull String personalAccessToken, @NonNull String searchText) throws Exception {
        OpenBIS openbis;
        if ("true".equalsIgnoreCase(System.getenv("OPENBIS_DRIVE_LOCAL_DEVELOPMENT_TESTS"))) {
            //TODO remove after local tests !!!   //////////////////////////////////////////////////
            openBISUrl = openBISUrl.replaceAll("localhost:8085", "localhost:8888");
            //TODO /////////////////////////////////////////////////////////////////////////////////

            openbis = new OpenBIS(openBISUrl);

            //TODO remove after local tests !!!   //////////////////////////////////////////////////
            if ( openBISUrl.contains("localhost:8888") ) {
                personalAccessToken = openbis.login("admin", "...");
            }
            //TODO /////////////////////////////////////////////////////////////////////////////////
        } else {
            openbis = new OpenBIS(openBISUrl);
            openbis.setSessionToken(personalAccessToken);
        }

        // Unified server call
        SearchSamplesOperation searchSamplesOperation = new SearchSamplesOperation(setEntityCriteria(new SampleSearchCriteria(), searchText), setEntityFetchOptions(new SampleFetchOptions()));
        SearchExperimentsOperation searchExperimentsOperation = new SearchExperimentsOperation(setEntityCriteria(new ExperimentSearchCriteria(), searchText), setEntityFetchOptions(new ExperimentFetchOptions()));
        SearchDataSetsOperation searchDataSetsOperation = new SearchDataSetsOperation(setEntityCriteria(new DataSetSearchCriteria(), searchText), setEntityFetchOptions(new DataSetFetchOptions()));

        // If this takes more than 30 seconds a standard http proxy will just cut the connection, for long calls please use Async operations
        Optional<List<? extends IOperationResult>> results = Optional.ofNullable(
                (SynchronousOperationExecutionResults) openbis.executeOperations(
                        List.of(searchSamplesOperation, searchExperimentsOperation, searchDataSetsOperation),
                        new SynchronousOperationExecutionOptions())
                ).map(SynchronousOperationExecutionResults::getResults);

        if (results.isPresent()) {
            // Sample results
            Stream<Sample> searchSamplesOperationResult = Optional.ofNullable((SearchSamplesOperationResult) results.get().get(0))
                    .map(SearchObjectsOperationResult::getSearchResult)
                    .map(SearchResult::getObjects).stream().flatMap(Collection::stream);

            //Experiment results
            Stream<Experiment> searchExperimentsOperationResult = Optional.ofNullable((SearchExperimentsOperationResult) results.get().get(1))
                    .map(SearchObjectsOperationResult::getSearchResult)
                    .map(SearchResult::getObjects).stream().flatMap(Collection::stream);

            //DataSet results
            Stream<DataSet> searchDataSetsOperationResult = Optional.ofNullable((SearchDataSetsOperationResult) results.get().get(2))
                    .map(SearchObjectsOperationResult::getSearchResult)
                    .map(SearchResult::getObjects).stream().flatMap(Collection::stream);
            //

            return Streams.concat(searchSamplesOperationResult, searchExperimentsOperationResult, searchDataSetsOperationResult)
                    .limit(15).map( it -> (AbstractEntity) it).toList();
        } else {
            return Collections.emptyList();
        }
    }

    public static class SearchUnit implements AutoCloseable {
        private String openBISUrl = null;
        private String personalAccessToken = null;

        private Timer timer = null;
        private String searchText;
        private Long searchStartingMoment = null;
        private boolean searching;

        private final BiFunction<List<AbstractEntity>, Exception, Void> resultListener;
        private final Consumer<Boolean> searchingStateChangeListener;

        public SearchUnit(
                @NonNull BiFunction<List<AbstractEntity>, Exception, Void> resultListener,
                @NonNull Consumer<Boolean> searchingStateChangeListener) {
            this.resultListener = resultListener;
            this.searchingStateChangeListener = searchingStateChangeListener;

            this.timer = new Timer();
        }

        synchronized public void setOpenBISUrl(String openBISUrl) {
            this.openBISUrl = openBISUrl;
        }

        synchronized public void setPersonalAccessToken(String personalAccessToken) {
            this.personalAccessToken = personalAccessToken;
        }

        synchronized public String getOpenBISUrl() {
            return this.openBISUrl;
        }

        synchronized public String getPersonalAccessToken() {
            return this.personalAccessToken;
        }

        synchronized public boolean isSearching() {
            return this.searching;
        }

        synchronized void setTimer() {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timerTaskJob();
                }
            }, 2000);
        }

        synchronized void timerTaskJob() {
            if ( searchStartingMoment != null ) {
                if ( searchStartingMoment < System.currentTimeMillis() ) {

                    if (this.openBISUrl != null && this.personalAccessToken != null) {
                        this.searching = true;
                        searchingStateChangeListener.accept(true);
                        this.searchStartingMoment = null;

                        try {
                            List<AbstractEntity> result =
                                    searchSynchronizableOpenBISEntities(this.openBISUrl, this.personalAccessToken, this.searchText);
                            resultListener.apply(result, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                            resultListener.apply(null, e);
                        } finally {
                            this.searching = false;
                            searchingStateChangeListener.accept(false);
                        }
                    }

                } else {
                    setTimer();
                }
            }
        }

        synchronized public void inputSearchText(@NonNull String searchText) {
            this.searchText = searchText;
            if (getOpenBISUrl() != null && getPersonalAccessToken() != null && !searching) {
                this.searchStartingMoment = System.currentTimeMillis() + 1500;
                setTimer();
            }
        }

        synchronized private void innerThreadCycle() throws InterruptedException {
            while (true) {

                Thread.sleep(1000);
            }
        }

        @Override
        public void close() throws Exception {
            this.timer.cancel();
        }
    }
}
