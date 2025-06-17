package ch.ethz.sis.openbis.systemtest.asapi.v3;

import static ch.ethz.sis.transaction.TransactionTestUtil.TestTransaction;
import static ch.ethz.sis.transaction.TransactionTestUtil.assertTransactions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.ProjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.search.ProjectSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.create.SpaceCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.ISpaceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
import ch.ethz.sis.transaction.TransactionStatus;
import ch.ethz.sis.transaction.api.TransactionOperationException;
import ch.systemsx.cisd.common.concurrent.MessageChannel;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.test.AssertionUtil;

public class Transaction1PCTest extends AbstractTransactionTest
{

    private TestTransactionParticipantApi participant;

    @BeforeMethod
    private void beforeMethod()
    {
        FileUtilities.deleteRecursively(new File(TRANSACTION_LOG_ROOT_FOLDER, TRANSACTION_LOG_PARTICIPANT_1_FOLDER));
        participant = createParticipant(createConfiguration(true, 60, 10), TEST_PARTICIPANT_1_ID, TRANSACTION_LOG_PARTICIPANT_1_FOLDER);
    }

    @AfterMethod
    private void afterMethod() throws Exception
    {
        if (participant.getTransactionConfiguration().isEnabled())
        {
            participant.close();
        }

        rollbackPreparedDatabaseTransactions();
        deleteCreatedSpacesAndProjects();
    }

    @Test
    public void testTransactionsDisabled()
    {
        participant = createParticipant(createConfiguration(false, 60, 10), TEST_PARTICIPANT_1_ID, TRANSACTION_LOG_PARTICIPANT_1_FOLDER);

        assertTransactionsDisabled(() -> participant.recoverTransactionsFromTransactionLog());
        assertTransactionsDisabled(() -> participant.finishFailedOrAbandonedTransactions());
        assertTransactionsDisabled(() -> participant.getTransactionMap());
        assertTransactionsDisabled(() -> participant.beginTransaction(null, null, null, null));
        assertTransactionsDisabled(() -> participant.executeOperation(null, null, null, null, null));
        assertTransactionsDisabled(() -> participant.prepareTransaction(null, null, null, null));
        assertTransactionsDisabled(() -> participant.commitTransaction(null, null, null));
        assertTransactionsDisabled(() -> participant.commitRecoveredTransaction(null, null, null));
        assertTransactionsDisabled(() -> participant.rollbackTransaction(null, null, null));
        assertTransactionsDisabled(() -> participant.rollbackRecoveredTransaction(null, null, null));
    }

    @Test
    public void testBegin()
    {
        UUID transactionId = UUID.randomUUID();
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        participant.beginTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, TEST_COORDINATOR_KEY);

        assertTransactions(participant.getTransactionMap(),
                new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));
    }

    @Test
    public void testExecuteOperationFails()
    {
        assertTransactions(participant.getTransactionMap());

        UUID transactionId = UUID.randomUUID();
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        participant.beginTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, null);

        assertTransactions(participant.getTransactionMap(),
                new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode(CODE_PREFIX + UUID.randomUUID());

        participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation) });

        try
        {
            participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, OPERATION_CREATE_SPACES,
                    new Object[] { sessionToken, Collections.singletonList(new SpaceCreation()) });
            fail();
        } catch (TransactionOperationException e)
        {
            AssertionUtil.assertContains("Code cannot be empty", e.getCause().getMessage());
        }

        assertTransactions(participant.getTransactionMap(),
                new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        Map<ISpaceId, Space> createdSpaces = v3api.getSpaces(sessionToken,
                Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 0);
    }

    @Test
    public void testExecuteOperationTimesOut() throws Exception
    {
        participant = createParticipant(createConfiguration(true, 1, 10), TEST_PARTICIPANT_1_ID, TRANSACTION_LOG_PARTICIPANT_1_FOLDER);

        assertTransactions(participant.getTransactionMap());

        UUID transactionId = UUID.randomUUID();
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        participant.beginTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, null);

        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode(CODE_PREFIX + UUID.randomUUID());

        participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation) });

        assertTransactions(participant.getTransactionMap(),
                new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        Thread.sleep(500);

        participant.finishFailedOrAbandonedTransactions();

        assertTransactions(participant.getTransactionMap(),
                new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        Thread.sleep(500);

        participant.finishFailedOrAbandonedTransactions();

        assertTransactions(participant.getTransactionMap());

        Map<ISpaceId, Space> createdSpaces = v3api.getSpaces(sessionToken,
                Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 0);
    }

    @Test
    public void testPrepareTransactionFails()
    {
        assertTransactions(participant.getTransactionMap());

        UUID transactionId = UUID.randomUUID();
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        participant.beginTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, null);

        assertTransactions(participant.getTransactionMap(), new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode(CODE_PREFIX + UUID.randomUUID());

        participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation) });

        assertTransactions(participant.getTransactionMap(),
                new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        try
        {
            participant.prepareTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, TEST_COORDINATOR_KEY);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Transaction '" + transactionId + "' was started without transaction coordinator key, therefore calling prepare is not allowed.");
        }

        assertTransactions(participant.getTransactionMap(),
                new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));
    }

    @Test
    public void testCommitTransactionFails()
    {
        // "commit" should fail
        RuntimeException exception = new RuntimeException("Test commit exception");
        participant.getDatabaseTransactionProvider().setCommitAction(() ->
        {
            throw exception;
        });

        assertTransactions(participant.getTransactionMap());

        UUID transactionId = UUID.randomUUID();
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        participant.beginTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, null);

        assertTransactions(participant.getTransactionMap(), new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode(CODE_PREFIX + UUID.randomUUID());

        participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation) });

        assertTransactions(participant.getTransactionMap(), new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        try
        {
            participant.commitTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "Commit transaction '" + transactionId + "' failed.");
            assertEquals(e.getCause(), exception);
        }

        // transaction is rolled back
        assertTransactions(participant.getTransactionMap());

        Map<ISpaceId, Space> createdSpaces = v3api.getSpaces(sessionToken,
                Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 0);
    }

    @Test
    public void testRollbackTransactionFails()
    {
        // "rollback" should fail
        RuntimeException exception = new RuntimeException("Test rollback exception");
        participant.getDatabaseTransactionProvider().setRollbackAction(() ->
        {
            throw exception;
        });

        assertTransactions(participant.getTransactionMap());

        UUID transactionId = UUID.randomUUID();
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        participant.beginTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, null);

        assertTransactions(participant.getTransactionMap(), new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode(CODE_PREFIX + UUID.randomUUID());

        participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation) });

        assertTransactions(participant.getTransactionMap(), new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        try
        {
            participant.rollbackTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "Rollback transaction '" + transactionId + "' failed.");
            assertEquals(e.getCause(), exception);
        }

        assertTransactions(participant.getTransactionMap(), new TestTransaction(transactionId, TransactionStatus.ROLLBACK_STARTED));

        // "rollback" should succeed
        participant.getDatabaseTransactionProvider().setRollbackAction(null);

        participant.finishFailedOrAbandonedTransactions();

        assertTransactions(participant.getTransactionMap());

        Map<ISpaceId, Space> createdSpaces = v3api.getSpaces(sessionToken,
                Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 0);
    }

    @Test
    public void testTransactionWithCommit()
    {
        testTransaction(false);
    }

    @Test
    public void testTransactionWithRollback()
    {
        testTransaction(true);
    }

    private void testTransaction(boolean rollback)
    {
        UUID transactionId = UUID.randomUUID();
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        // begin
        participant.beginTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, null);

        assertTransactions(participant.getTransactionMap(),
                new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        // get spaces before
        SearchResult<Space> trSpacesBeforeCreation =
                participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY,
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> noTrSpacesBeforeCreation = v3api.searchSpaces(sessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions());

        assertTransactions(participant.getTransactionMap(),
                new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        // create a new space
        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode(CODE_PREFIX + UUID.randomUUID());

        participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY,
                OPERATION_CREATE_SPACES, new Object[] { sessionToken, Collections.singletonList(spaceCreation) });

        assertTransactions(participant.getTransactionMap(),
                new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        // get spaces after
        SearchResult<Space> trSpacesAfterCreation =
                participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY,
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> noTrSpacesAfterCreation = v3api.searchSpaces(sessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions());

        // participant sees its own new space, outside the transaction it is not visible
        assertEquals(Collections.singleton(spaceCreation.getCode().toUpperCase()),
                difference(codes(trSpacesAfterCreation.getObjects()), codes(trSpacesBeforeCreation.getObjects())));

        // participant creates a project in its new space
        SearchResult<Project> trProjectsBeforeCreation =
                participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY,
                        OPERATION_SEARCH_PROJECTS, new Object[] { sessionToken, new ProjectSearchCriteria(), new ProjectFetchOptions() });
        SearchResult<Project> noTrProjectsBeforeCreation =
                v3api.searchProjects(sessionToken, new ProjectSearchCriteria(), new ProjectFetchOptions());

        ProjectCreation projectCreation = new ProjectCreation();
        projectCreation.setSpaceId(new SpacePermId(spaceCreation.getCode()));
        projectCreation.setCode(CODE_PREFIX + UUID.randomUUID());

        participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY,
                OPERATION_CREATE_PROJECTS, new Object[] { sessionToken, Collections.singletonList(projectCreation) });

        assertTransactions(participant.getTransactionMap(),
                new TestTransaction(transactionId, TransactionStatus.BEGIN_FINISHED));

        // participant sees its own new project, outside the transaction it is not visible
        SearchResult<Project> trProjectsAfterCreation =
                participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY,
                        OPERATION_SEARCH_PROJECTS, new Object[] { sessionToken, new ProjectSearchCriteria(), new ProjectFetchOptions() });
        SearchResult<Project> noTrProjectsAfterCreation =
                v3api.searchProjects(sessionToken, new ProjectSearchCriteria(), new ProjectFetchOptions());

        assertEquals(Collections.singleton("/" + spaceCreation.getCode().toUpperCase() + "/" + projectCreation.getCode().toUpperCase()),
                difference(identifiers(trProjectsAfterCreation.getObjects()),
                        identifiers(trProjectsBeforeCreation.getObjects())));
        assertEquals(Collections.emptySet(),
                difference(identifiers(noTrProjectsAfterCreation.getObjects()), identifiers(noTrProjectsBeforeCreation.getObjects())));

        if (rollback)
        {
            participant.rollbackTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);
        } else
        {
            participant.commitTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);
        }

        assertTransactions(participant.getTransactionMap());

        SearchResult<Space> noTrSpacesAfterCommit = v3api.searchSpaces(sessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions());
        SearchResult<Project> noTrProjectsAfterCommit =
                v3api.searchProjects(sessionToken, new ProjectSearchCriteria(), new ProjectFetchOptions());

        if (rollback)
        {
            // both the spaces and projects are rolled back and are not visible outside the transaction
            assertEquals(codes(noTrSpacesAfterCommit.getObjects()), codes(noTrSpacesBeforeCreation.getObjects()));
            assertEquals(identifiers(noTrProjectsAfterCommit.getObjects()), identifiers(noTrProjectsBeforeCreation.getObjects()));
        } else
        {
            // both the space and project are committed and are visible outside the transaction
            assertEquals(Set.of(spaceCreation.getCode().toUpperCase()),
                    difference(codes(noTrSpacesAfterCommit.getObjects()), codes(noTrSpacesBeforeCreation.getObjects())));
            assertEquals(Set.of("/" + spaceCreation.getCode().toUpperCase() + "/" + projectCreation.getCode().toUpperCase()),
                    difference(identifiers(noTrProjectsAfterCommit.getObjects()), identifiers(noTrProjectsBeforeCreation.getObjects())));
        }
    }

    @Test
    public void testMultipleTransactions()
    {
        UUID transactionId1 = UUID.randomUUID();
        UUID transactionId2 = UUID.randomUUID();

        String sessionToken1 = v3api.login(TEST_USER, PASSWORD);
        String sessionToken2 = v3api.login(TEST_USER, PASSWORD);

        // begin tr1 and tr2
        participant.beginTransaction(transactionId1, sessionToken1, TEST_INTERACTIVE_SESSION_KEY, null);
        participant.beginTransaction(transactionId2, sessionToken2, TEST_INTERACTIVE_SESSION_KEY, null);

        SearchResult<Space> tr1SpacesBeforeCreations =
                participant.executeOperation(transactionId1, sessionToken1, TEST_INTERACTIVE_SESSION_KEY, OPERATION_SEARCH_SPACES,
                        new Object[] { sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> tr2SpacesBeforeCreations =
                participant.executeOperation(transactionId2, sessionToken2, TEST_INTERACTIVE_SESSION_KEY, OPERATION_SEARCH_SPACES,
                        new Object[] { sessionToken2, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> noTrSpacesBeforeCreations = v3api.searchSpaces(sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions());

        // create space1 in tr1
        SpaceCreation tr1Creation = new SpaceCreation();
        tr1Creation.setCode(CODE_PREFIX + UUID.randomUUID());

        participant.executeOperation(transactionId1, sessionToken1, TEST_INTERACTIVE_SESSION_KEY,
                OPERATION_CREATE_SPACES, new Object[] { sessionToken1, Collections.singletonList(tr1Creation) });

        // create space2 in tr2
        SpaceCreation tr2Creation = new SpaceCreation();
        tr2Creation.setCode(CODE_PREFIX + UUID.randomUUID());

        participant.executeOperation(transactionId2, sessionToken2, TEST_INTERACTIVE_SESSION_KEY,
                OPERATION_CREATE_SPACES, new Object[] { sessionToken2, Collections.singletonList(tr2Creation) });

        // create space3 in noTr
        SpaceCreation noTrCreation = new SpaceCreation();
        noTrCreation.setCode(CODE_PREFIX + UUID.randomUUID());
        v3api.createSpaces(sessionToken1, Collections.singletonList(noTrCreation));

        SearchResult<Space> tr1SpacesAfterCreations =
                participant.executeOperation(transactionId1, sessionToken1, TEST_INTERACTIVE_SESSION_KEY,
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> tr2SpacesAfterCreations =
                participant.executeOperation(transactionId2, sessionToken2, TEST_INTERACTIVE_SESSION_KEY,
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken2, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> noTrSpacesAfterCreations = v3api.searchSpaces(sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions());

        // check that tr1 sees only space1, tr2 sees only space2, noTr sees space3
        assertEquals(Collections.singleton(tr1Creation.getCode().toUpperCase()),
                difference(codes(tr1SpacesAfterCreations.getObjects()), codes(tr1SpacesBeforeCreations.getObjects())));
        assertEquals(Collections.singleton(tr2Creation.getCode().toUpperCase()),
                difference(codes(tr2SpacesAfterCreations.getObjects()), codes(tr2SpacesBeforeCreations.getObjects())));
        assertEquals(Collections.singleton(noTrCreation.getCode().toUpperCase()),
                difference(codes(noTrSpacesAfterCreations.getObjects()), codes(noTrSpacesBeforeCreations.getObjects())));

        participant.commitTransaction(transactionId1, sessionToken1, TEST_INTERACTIVE_SESSION_KEY);

        try
        {
            participant.executeOperation(transactionId1, sessionToken1, TEST_INTERACTIVE_SESSION_KEY,
                    OPERATION_SEARCH_SPACES, new Object[] { sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions() });
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "Transaction '" + transactionId1 + "' does not exist.");
        }

        // after tr1 commit, tr2 sees space1 and space2, noTr sees space1 and space3
        SearchResult<Space> tr2SpacesAfterTr1Commit =
                participant.executeOperation(transactionId2, sessionToken2, TEST_INTERACTIVE_SESSION_KEY,
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken2, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> noTrSpacesAfterTr1Commit = v3api.searchSpaces(sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions());

        assertEquals(Collections.singleton(tr1Creation.getCode().toUpperCase()),
                difference(codes(tr2SpacesAfterTr1Commit.getObjects()), codes(tr2SpacesAfterCreations.getObjects())));
        assertEquals(Collections.singleton(tr1Creation.getCode().toUpperCase()),
                difference(codes(noTrSpacesAfterTr1Commit.getObjects()), codes(noTrSpacesAfterCreations.getObjects())));

        participant.rollbackTransaction(transactionId2, sessionToken2, TEST_INTERACTIVE_SESSION_KEY);

        try
        {
            participant.executeOperation(transactionId2, sessionToken2, TEST_INTERACTIVE_SESSION_KEY,
                    OPERATION_SEARCH_SPACES, new Object[] { sessionToken2, new SpaceSearchCriteria(), new SpaceFetchOptions() });
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "Transaction '" + transactionId2 + "' does not exist.");
        }

        // after tr1 commit and tr2 rollback, noTr sees space1 and space3
        noTrSpacesAfterTr1Commit = v3api.searchSpaces(sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions());
        assertEquals(Collections.singleton(tr1Creation.getCode().toUpperCase()),
                difference(codes(noTrSpacesAfterTr1Commit.getObjects()), codes(noTrSpacesAfterCreations.getObjects())));
    }

    @Test
    public void testMultipleConcurrentCallsToOneTransaction() throws InterruptedException
    {
        assertTransactions(participant.getTransactionMap());

        UUID transactionId = UUID.randomUUID();
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        participant.beginTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, null);

        SpaceCreation spaceCreation1 = new SpaceCreation();
        spaceCreation1.setCode(CODE_PREFIX + UUID.randomUUID());

        participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation1) });

        MessageChannel messageChannel1 = new MessageChannel(1000);
        MessageChannel messageChannel2 = new MessageChannel(1000);

        participant.getDatabaseTransactionProvider().setCommitAction(() ->
        {
            messageChannel1.send("committing");
            messageChannel2.assertNextMessage("executed");
        });

        Thread committingThread = new Thread(() -> participant.commitTransaction(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY));
        committingThread.start();

        SpaceCreation spaceCreation2 = new SpaceCreation();
        spaceCreation2.setCode(CODE_PREFIX + UUID.randomUUID());

        messageChannel1.assertNextMessage("committing");

        try
        {
            // try to execute an operation while the commit is in progress
            participant.executeOperation(transactionId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, OPERATION_CREATE_SPACES,
                    new Object[] { sessionToken, Collections.singletonList(spaceCreation2) });
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Cannot execute a new action on transaction '" + transactionId + "' as it is still busy executing a previous action.");
        }

        messageChannel2.send("executed");

        committingThread.join();
    }

    @Test
    public void testTooManyTransactions()
    {
        participant = createParticipant(createConfiguration(true, 60, 2), TEST_PARTICIPANT_1_ID, TRANSACTION_LOG_PARTICIPANT_1_FOLDER);

        assertTransactions(participant.getTransactionMap());

        UUID transactionId1 = UUID.randomUUID();
        UUID transactionId2 = UUID.randomUUID();
        UUID transactionId3 = UUID.randomUUID();

        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        String sessionToken2 = v3api.login(TEST_USER, PASSWORD);
        String sessionToken3 = v3api.login(TEST_USER, PASSWORD);

        participant.beginTransaction(transactionId1, sessionToken, TEST_INTERACTIVE_SESSION_KEY, null);
        participant.beginTransaction(transactionId2, sessionToken2, TEST_INTERACTIVE_SESSION_KEY, null);

        try
        {
            participant.beginTransaction(transactionId3, sessionToken3, TEST_INTERACTIVE_SESSION_KEY, null);
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Cannot create transaction '" + transactionId3
                            + "' because the transaction count limit has been reached. Number of existing transactions: 2");
        }
    }

    @Test
    public void testOneTransactionPerSessionToken()
    {
        UUID transactionId1 = UUID.randomUUID();
        UUID transactionId2 = UUID.randomUUID();

        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        String sessionToken2 = v3api.login(TEST_USER, PASSWORD);

        participant.beginTransaction(transactionId1, sessionToken, TEST_INTERACTIVE_SESSION_KEY, null);

        try
        {
            participant.beginTransaction(transactionId2, sessionToken, TEST_INTERACTIVE_SESSION_KEY, null);
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Cannot create more than one transaction for the same session token. Transaction that could not be created: '" + transactionId2
                            + "'. The already existing and still active transaction: '" + transactionId1 + "'.");
        }

        assertTransactions(participant.getTransactionMap(), new TestTransaction(transactionId1, TransactionStatus.BEGIN_FINISHED));

        participant.beginTransaction(transactionId2, sessionToken2, TEST_INTERACTIVE_SESSION_KEY, null);

        assertTransactions(participant.getTransactionMap(), new TestTransaction(transactionId1, TransactionStatus.BEGIN_FINISHED),
                new TestTransaction(transactionId2, TransactionStatus.BEGIN_FINISHED));
    }

    @Test
    public void testTransactionAccess()
    {
        UUID transactionId1 = UUID.randomUUID();
        UUID transactionId2 = UUID.randomUUID();

        String regularUserSessionToken = v3api.login(TEST_SPACE_USER, PASSWORD);
        String regularUserSessionToken2 = v3api.login(TEST_POWER_USER_CISD, PASSWORD);
        String instanceAdminSessionToken = v3api.login(TEST_USER, PASSWORD);
        String systemSessionToken = v3api.loginAsSystem();

        participant.beginTransaction(transactionId1, regularUserSessionToken, TEST_INTERACTIVE_SESSION_KEY, null);
        participant.beginTransaction(transactionId2, regularUserSessionToken2, TEST_INTERACTIVE_SESSION_KEY, null);

        participant.executeOperation(transactionId1, regularUserSessionToken, TEST_INTERACTIVE_SESSION_KEY,
                OPERATION_SEARCH_SPACES, new Object[] { regularUserSessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions() });

        try
        {
            // a different but regular user does not have access
            participant.executeOperation(transactionId1, regularUserSessionToken2, TEST_INTERACTIVE_SESSION_KEY,
                    OPERATION_SEARCH_SPACES, new Object[] { regularUserSessionToken2, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "Access denied to transaction '" + transactionId1 + "'");
        }

        // instance admin user has access to any transaction
        participant.executeOperation(transactionId1, instanceAdminSessionToken, TEST_INTERACTIVE_SESSION_KEY,
                OPERATION_SEARCH_SPACES, new Object[] { instanceAdminSessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions() });

        // system user has access to any transaction
        participant.executeOperation(transactionId1, systemSessionToken, TEST_INTERACTIVE_SESSION_KEY,
                OPERATION_SEARCH_SPACES, new Object[] { systemSessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions() });
    }

    @Test
    public void testRecovery()
    {
        TestTransactionParticipantApi participantBeforeCrash = null;
        TestTransactionParticipantApi participantAfterCrash = null;

        try
        {
            participantBeforeCrash =
                    createParticipant(createConfiguration(true, 60, 10), TEST_PARTICIPANT_1_ID, TRANSACTION_LOG_PARTICIPANT_1_FOLDER);

            // "commit" and "rollback" should fail
            RuntimeException commitException = new RuntimeException("Test commit exception");
            RuntimeException rollbackException = new RuntimeException("Test rollback exception");

            participantBeforeCrash.getDatabaseTransactionProvider().setCommitAction(() ->
            {
                throw commitException;
            });
            participantBeforeCrash.getDatabaseTransactionProvider().setRollbackAction(() ->
            {
                throw rollbackException;
            });

            assertTransactions(participantBeforeCrash.getTransactionMap());

            UUID transactionId1 = UUID.randomUUID();
            UUID transactionId2 = UUID.randomUUID();

            String sessionToken1 = v3api.login(TEST_USER, PASSWORD);
            String sessionToken2 = v3api.login(TEST_USER, PASSWORD);

            // begin both transactions
            participantBeforeCrash.beginTransaction(transactionId1, sessionToken1, TEST_INTERACTIVE_SESSION_KEY, null);
            participantBeforeCrash.beginTransaction(transactionId2, sessionToken2, TEST_INTERACTIVE_SESSION_KEY, null);

            // create a space in tr1
            SpaceCreation spaceCreation1 = new SpaceCreation();
            spaceCreation1.setCode(CODE_PREFIX + UUID.randomUUID());

            participantBeforeCrash.executeOperation(transactionId1, sessionToken1, TEST_INTERACTIVE_SESSION_KEY,
                    OPERATION_CREATE_SPACES, new Object[] { sessionToken2, Collections.singletonList(spaceCreation1) });

            // create a space in tr2
            SpaceCreation spaceCreation2 = new SpaceCreation();
            spaceCreation2.setCode(CODE_PREFIX + UUID.randomUUID());

            participantBeforeCrash.executeOperation(transactionId2, sessionToken2, TEST_INTERACTIVE_SESSION_KEY,
                    OPERATION_CREATE_SPACES, new Object[] { sessionToken2, Collections.singletonList(spaceCreation2) });

            // failed commit of tr1
            try
            {
                participantBeforeCrash.commitTransaction(transactionId1, sessionToken1, TEST_INTERACTIVE_SESSION_KEY);
                fail();
            } catch (Exception e)
            {
                assertEquals(e.getMessage(), "Commit transaction '" + transactionId1 + "' failed.");
                assertEquals(e.getCause(), commitException);
            }

            assertTransactions(participantBeforeCrash.getTransactionMap(), new TestTransaction(transactionId1, TransactionStatus.ROLLBACK_STARTED),
                    new TestTransaction(transactionId2, TransactionStatus.BEGIN_FINISHED));

            // new participant
            participantAfterCrash =
                    createParticipant(createConfiguration(true, 60, 10), TEST_PARTICIPANT_1_ID, TRANSACTION_LOG_PARTICIPANT_1_FOLDER);

            assertTransactions(participantAfterCrash.getTransactionMap());

            // only 2PC transactions are recovered
            participantAfterCrash.recoverTransactionsFromTransactionLog();

            assertTransactions(participantAfterCrash.getTransactionMap());

            Map<ISpaceId, Space> createdSpaces1 = v3api.getSpaces(sessionToken1,
                    Collections.singletonList(new SpacePermId(spaceCreation2.getCode())), new SpaceFetchOptions());
            Map<ISpaceId, Space> createdSpaces2 = v3api.getSpaces(sessionToken2,
                    Collections.singletonList(new SpacePermId(spaceCreation2.getCode())), new SpaceFetchOptions());
            assertEquals(createdSpaces1.size(), 0);
            assertEquals(createdSpaces2.size(), 0);
        } finally
        {
            if (participantBeforeCrash != null)
            {
                participantBeforeCrash.close();
            }
            if (participantAfterCrash != null)
            {
                participantAfterCrash.close();
            }
        }
    }

}
