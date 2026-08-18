package ch.openbis.drive.util;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClient;
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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.PersonalAccessToken;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.create.PersonalAccessTokenCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.fetchoptions.PersonalAccessTokenFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.id.PersonalAccessTokenPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.search.PersonalAccessTokenSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.PersonPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SearchSamplesOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SearchSamplesOperationResult;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.tasks.SyncOperation;
import ch.systemsx.cisd.common.exceptions.InvalidSessionException;
import com.google.common.collect.Streams;
import lombok.NonNull;
import org.springframework.remoting.RemoteAccessException;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class OpenBISQueryUtil {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private static final OpenBISQueryUtil INSTANCE = new OpenBISQueryUtil();

    static final int AFS_MAX_READ_SIZE_BYTES = 10485760;
    static final int AFS_CLIENT_TIMEOUT = 30000;

    public static final String DRIVE_PAT_SESSION_NAME = "OPENBIS_DRIVE_GENERATED_SESSION";
    static final long PAT_MINIMUM_LEFT_VALIDITY_MILLIS = 1000L * 60 * 60 * 24 * 7;
    static final long GENERATED_PAT_DURATION_MILLIS = 1000L * 60 * 60 * 24 * 365;

    public static @NonNull OpenBISQueryUtil getInstance() {
        return INSTANCE;
    }

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

    public enum PATCheckResultEnum {
        OK,
        INVALID_SESSION,
        ERROR_REACHING_SERVER,
        UNKNOWN_ERROR
    }
    public record PATCheckResult(
        @NonNull PATCheckResultEnum result,
        String user,
        Date validUntil
    ) {}
    public @NonNull PATCheckResult checkPAT(@NonNull String openBISUrl, @NonNull String personalAccessToken) {
        PersonalAccessTokenPermId patPermId = new PersonalAccessTokenPermId(personalAccessToken);
        try {
            return CompletableFuture.supplyAsync(
                    () -> {
                        PersonalAccessToken pat;
                        try {
                            OpenBIS openbis = getOpenbisClient(openBISUrl);
                            openbis.setSessionToken(personalAccessToken);

                            PersonalAccessTokenFetchOptions patFetchOptions = new PersonalAccessTokenFetchOptions();
                            patFetchOptions.withOwner();
                            pat = openbis.getPersonalAccessTokens(Collections.singletonList(patPermId), patFetchOptions)
                                    .get(patPermId);
                        } catch (InvalidSessionException invalidSessionException) {
                            return new PATCheckResult(PATCheckResultEnum.INVALID_SESSION, null, null);
                        } catch (Exception e) {
                            logger.catching(e);
                            if (e instanceof RemoteAccessException ||
                                    e instanceof ConnectException ||
                                    e instanceof SocketException ||
                                    e instanceof SocketTimeoutException
                            ) {
                                return new PATCheckResult(PATCheckResultEnum.ERROR_REACHING_SERVER, null, null);
                            } else {
                                return new PATCheckResult(PATCheckResultEnum.UNKNOWN_ERROR, null, null);
                            }
                        }
                        if (pat != null) {
                            return new PATCheckResult(PATCheckResultEnum.OK, pat.getOwner().getUserId(), pat.getValidToDate());
                        } else {
                            return new PATCheckResult(PATCheckResultEnum.INVALID_SESSION, null, null);
                        }
                    }
            ).get(5000, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException e) {
            logger.catching(e);
            return new PATCheckResult(PATCheckResultEnum.UNKNOWN_ERROR, null, null);
        } catch (TimeoutException e) {
            logger.catching(e);
            return new PATCheckResult(PATCheckResultEnum.ERROR_REACHING_SERVER, null, null);
        }
    }

    public record AvailableSession(
            @NonNull String username,
            @NonNull String openBISUrl,
            @NonNull String personalAccessToken,
            ZonedDateTime validUntil
    ) {}
    public @NonNull Set<AvailableSession> getAvailableSessions(@NonNull List<SyncJob> syncJobs) {
        ConcurrentHashMap<List<String>, AvailableSession> availableSessions = new ConcurrentHashMap<>();

        try {
            CompletableFuture.allOf(syncJobs.stream().map(
                    (syncJob) -> CompletableFuture.supplyAsync(
                            () -> {
                                PATCheckResult patCheckResult = checkPAT(syncJob.getOpenBisUrl(), syncJob.getOpenBisPersonalAccessToken());
                                if (patCheckResult.result() == PATCheckResultEnum.OK) {
                                    availableSessions.compute(List.of(patCheckResult.user(), syncJob.getOpenBisUrl()),
                                        (sessionKey, currentRelatedSession) -> {
                                            if (
                                                    currentRelatedSession == null ||
                                                    currentRelatedSession.validUntil() == null ||
                                                    (
                                                        patCheckResult.validUntil() != null &&
                                                        currentRelatedSession.validUntil().toInstant()
                                                                .isBefore(patCheckResult.validUntil().toInstant())
                                                    )
                                            ) {
                                                return new AvailableSession(
                                                        patCheckResult.user(),
                                                        syncJob.getOpenBisUrl(),
                                                        syncJob.getOpenBisPersonalAccessToken(),
                                                        Optional.ofNullable(patCheckResult.validUntil())
                                                                .map(Date::toInstant)
                                                                .map(instant -> instant.atZone(ZoneId.systemDefault()))
                                                                .orElse(null)
                                                );
                                            } else {
                                                return currentRelatedSession;
                                            }
                                        }
                                    );
                                }
                                return patCheckResult;
                            },
                            ParallelExecutionUtil.EXECUTOR_SERVICE
                    )
            ).toArray(CompletableFuture[]::new)).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.catching(e);
        }

        return new HashSet<>(availableSessions.values());
    }

    public enum NewSessionResultEnum {
        OK,
        BAD_CREDENTIALS,
        ERROR_REACHING_SERVER,
        UNKNOWN_ERROR
    }
    public record NewSessionResult (
            @NonNull NewSessionResultEnum result,
            AvailableSession availableSession
    ){}
    public NewSessionResult getNewSession(
            @NonNull String openBISUrl,
            @NonNull String username,
            @NonNull String password
    ) {
        try {
            OpenBIS openbis = getOpenbisClient(openBISUrl);
            String sessionToken = openbis.login(username, password);
            if (sessionToken == null) {
                return new NewSessionResult(NewSessionResultEnum.BAD_CREDENTIALS, null);
            }

            PersonalAccessTokenFetchOptions patFetchOptions = new PersonalAccessTokenFetchOptions();
            PersonalAccessTokenSearchCriteria personalAccessTokenSearchCriteria = new PersonalAccessTokenSearchCriteria();
            personalAccessTokenSearchCriteria.withOwner().withUserId().thatEquals(username);
            personalAccessTokenSearchCriteria.withSessionName().thatEquals(DRIVE_PAT_SESSION_NAME);
            SearchResult<PersonalAccessToken> pats = openbis.searchPersonalAccessTokens(personalAccessTokenSearchCriteria, patFetchOptions);

            PersonalAccessToken alreadyAvailableSession = pats.getObjects().stream().filter(
                pat -> pat.getValidFromDate().before(new Date()) &&
                    pat.getValidToDate().after(
                        new Date(System.currentTimeMillis() + PAT_MINIMUM_LEFT_VALIDITY_MILLIS)
                    )
            ).findFirst().orElse(null);

            if (alreadyAvailableSession != null) {
                return new NewSessionResult(
                        NewSessionResultEnum.OK,
                        new AvailableSession(
                            username,
                            openBISUrl,
                            alreadyAvailableSession.getPermId().getPermId(),
                            Optional.ofNullable(alreadyAvailableSession.getValidToDate())
                                    .map(Date::toInstant)
                                    .map(instant -> instant.atZone(ZoneId.systemDefault()))
                                    .orElse(null)
                        )
                );
            } else {
                PersonalAccessTokenCreation personalAccessTokenCreation = new PersonalAccessTokenCreation();
                personalAccessTokenCreation.setSessionName(DRIVE_PAT_SESSION_NAME);
                personalAccessTokenCreation.setOwnerId(new PersonPermId(username));
                personalAccessTokenCreation.setValidFromDate(new Date());
                Date validToDate = new Date(System.currentTimeMillis() + GENERATED_PAT_DURATION_MILLIS);
                personalAccessTokenCreation.setValidToDate(validToDate);

                PersonalAccessTokenPermId generatedPat = openbis.createPersonalAccessTokens(Collections.singletonList(personalAccessTokenCreation)).getFirst();

                return new NewSessionResult(
                        NewSessionResultEnum.OK,
                        new AvailableSession(
                                username,
                                openBISUrl,
                                generatedPat.getPermId(),
                                Optional.of(validToDate)
                                        .map(Date::toInstant)
                                        .map(instant -> instant.atZone(ZoneId.systemDefault()))
                                        .orElse(null)
                        )
                );
            }
        } catch (Exception e) {
            logger.catching(e);
            if (e instanceof RemoteAccessException ||
                    e instanceof ConnectException ||
                    e instanceof SocketException ||
                    e instanceof SocketTimeoutException
            ) {
                return new NewSessionResult(NewSessionResultEnum.ERROR_REACHING_SERVER, null);
            } else {
                return new NewSessionResult(NewSessionResultEnum.UNKNOWN_ERROR, null);
            }
        }
    }

    @NonNull OpenBIS getOpenbisClient(@NonNull String openBISUrl) {
        OpenBIS openbis;
        if (isOpenbisDriveLocalDevelopmentAsAndAfsUrl(openBISUrl)
        ) {
            openbis = getOpenBISForLocalDevelopment(openBISUrl);
        } else {
            openbis = new OpenBIS(openBISUrl);
        }
        return openbis;
    }

    public List<AbstractEntity> searchSynchronizableOpenBISEntities(@NonNull String openBISUrl, @NonNull String personalAccessToken, @NonNull String searchText) throws Exception {
        OpenBIS openbis = getOpenbisClient(openBISUrl);
        openbis.setSessionToken(personalAccessToken);

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

    private static boolean isOpenbisDriveLocalDevelopmentAsAndAfsUrl(@NonNull String openBISUrl) {
        return "true".equalsIgnoreCase(System.getenv("OPENBIS_DRIVE_LOCAL_DEVELOPMENT_AS_AND_AFS_URL"))
                && openBISUrl.contains("localhost:8085");
    }

    private static @NonNull OpenBIS getOpenBISForLocalDevelopment(@NonNull String openBISUrl) {
        openBISUrl = openBISUrl.replaceAll("localhost:8085", "localhost:8888");
        OpenBIS openbis = new OpenBIS(openBISUrl);
        if ( openBISUrl.contains("localhost:8888") ) {
            openbis.login("admin", "...");
        }
        return openbis;
    }

    public static List<String> searchOpenBISEntityAfsDirectories(@NonNull String openBISUrl,
                                                                         @NonNull String personalAccessToken,
                                                                         @NonNull String entityId,
                                                                         @NonNull String searchText) throws Exception {
        AfsClient afsClient = new AfsClient(URI.create(openBISUrl + SyncOperation.AFS_SERVER_PATH), AFS_MAX_READ_SIZE_BYTES, AFS_CLIENT_TIMEOUT);
        afsClient.setSessionToken(personalAccessToken);

        File[] files = afsClient.list(entityId, "/", true);

        return Streams.concat(
            Stream.of("/"),
            Arrays.stream(files).filter(Objects::nonNull)
                .filter( fileDto -> Boolean.TRUE.equals(fileDto.getDirectory())
                ).map( File::getPath )
        ).filter(path -> path != null && path.contains(searchText) ).toList();
    }

    public static class SearchUnit implements AutoCloseable {
        private final Logger logger = LogManager.getLogger(this.getClass());
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
            }, 1000);
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
                                OpenBISQueryUtil.getInstance()
                                    .searchSynchronizableOpenBISEntities(
                                            this.openBISUrl, this.personalAccessToken, this.searchText
                                    );
                            resultListener.apply(result, null);
                        } catch (Exception e) {
                            logger.catching(e);
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
            if (!searchText.isEmpty() && getOpenBISUrl() != null && getPersonalAccessToken() != null && !searching) {
                this.searchStartingMoment = System.currentTimeMillis() + 500;
                setTimer();
            }
        }

        @Override
        public void close() throws Exception {
            this.timer.cancel();
        }
    }

    public static class AfsSearchUnit implements AutoCloseable {
        private Logger logger = LogManager.getLogger(this.getClass());
        private String openBISUrl = null;
        private String personalAccessToken = null;
        private String entityId = null;

        private Timer timer = null;
        private String searchText;
        private Long searchStartingMoment = null;
        private boolean searching;

        private final BiFunction<List<String>, Exception, Void> resultListener;
        private final Consumer<Boolean> searchingStateChangeListener;

        public AfsSearchUnit(
                @NonNull BiFunction<List<String>, Exception, Void> resultListener,
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

        synchronized public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        synchronized public String getOpenBISUrl() {
            return this.openBISUrl;
        }

        synchronized public String getPersonalAccessToken() {
            return this.personalAccessToken;
        }

        synchronized public String getEntityId() {
            return this.entityId;
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
            }, 1000);
        }

        synchronized void timerTaskJob() {
            if ( searchStartingMoment != null ) {
                if ( searchStartingMoment < System.currentTimeMillis() ) {

                    if (this.openBISUrl != null && this.personalAccessToken != null && this.entityId != null) {
                        this.searching = true;
                        searchingStateChangeListener.accept(true);
                        this.searchStartingMoment = null;

                        try {
                            List<String> result =
                                    searchOpenBISEntityAfsDirectories(this.openBISUrl, this.personalAccessToken, this.entityId, this.searchText);
                            resultListener.apply(result, null);
                        } catch (Exception e) {
                            logger.catching(e);
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
            if (getOpenBISUrl() != null &&
                    getPersonalAccessToken() != null &&
                    getEntityId() != null &&
                    !searching) {

                this.searchStartingMoment = System.currentTimeMillis() + 500;
                setTimer();
            }
        }

        @Override
        public void close() throws Exception {
            this.timer.cancel();
        }
    }
}
