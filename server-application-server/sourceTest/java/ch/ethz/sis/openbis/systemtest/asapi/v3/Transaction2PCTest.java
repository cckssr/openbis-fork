package ch.ethz.sis.openbis.systemtest.asapi.v3;

import static ch.ethz.sis.transaction.TransactionTestUtil.TestTransaction;
import static ch.ethz.sis.transaction.TransactionTestUtil.assertTransactions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import ch.ethz.sis.transaction.api.ITransactionParticipant;
import ch.ethz.sis.transaction.api.TransactionOperationException;
import ch.systemsx.cisd.common.concurrent.MessageChannel;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.test.AssertionUtil;

public class Transaction2PCTest extends AbstractTransactionTest
{

    private TestTransactionCoordinatorApi coordinator;

    private TestTransactionParticipantApi participant1;

    private TestTransactionParticipantApi participant2;

    private UUID coordinatorTrId;

    private UUID coordinatorTr2Id;

    private UUID coordinatorTr3Id;

    private UUID participant1TrId;

    @BeforeMethod
    private void beforeMethod()
    {
        FileUtilities.deleteRecursively(new File(TRANSACTION_LOG_ROOT_FOLDER, TRANSACTION_LOG_COORDINATOR_FOLDER));
        FileUtilities.deleteRecursively(new File(TRANSACTION_LOG_ROOT_FOLDER, TRANSACTION_LOG_PARTICIPANT_1_FOLDER));
        FileUtilities.deleteRecursively(new File(TRANSACTION_LOG_ROOT_FOLDER, TRANSACTION_LOG_PARTICIPANT_2_FOLDER));

        coordinatorTrId = UUID.randomUUID();
        coordinatorTr2Id = UUID.randomUUID();
        coordinatorTr3Id = UUID.randomUUID();

        participant1TrId = UUID.randomUUID();
        final UUID participant1Tr2Id = UUID.randomUUID();
        final UUID participant1Tr3Id = UUID.randomUUID();

        final UUID participant2TrId = UUID.randomUUID();
        final UUID participant2Tr2Id = UUID.randomUUID();
        final UUID participant2Tr3Id = UUID.randomUUID();

        participant1 = createParticipant(createConfiguration(true, 60, 10), TEST_PARTICIPANT_1_ID, TRANSACTION_LOG_PARTICIPANT_1_FOLDER);
        participant1.setTestTransactionMapping(
                Map.of(coordinatorTrId, participant1TrId, coordinatorTr2Id, participant1Tr2Id, coordinatorTr3Id, participant1Tr3Id));

        participant2 = createParticipant(createConfiguration(true, 60, 10), TEST_PARTICIPANT_2_ID, TRANSACTION_LOG_PARTICIPANT_2_FOLDER);
        participant2.setTestTransactionMapping(
                Map.of(coordinatorTrId, participant2TrId, coordinatorTr2Id, participant2Tr2Id, coordinatorTr3Id, participant2Tr3Id));

        coordinator =
                createCoordinator(createConfiguration(true, 60, 10), Arrays.asList(participant1, participant2), TRANSACTION_LOG_COORDINATOR_FOLDER);
    }

    @AfterMethod
    private void afterMethod() throws Exception
    {
        if (coordinator.getTransactionConfiguration().isEnabled())
        {
            coordinator.close();
            participant1.close();
            participant2.close();
        }

        rollbackPreparedDatabaseTransactions();
        deleteCreatedSpacesAndProjects();
    }

    @Test
    public void testTransactionsDisabled()
    {
        coordinator =
                createCoordinator(createConfiguration(false, 60, 10), Arrays.asList(participant1, participant2), TRANSACTION_LOG_COORDINATOR_FOLDER);

        assertTransactionsDisabled(() -> coordinator.recoverTransactionsFromTransactionLog());
        assertTransactionsDisabled(() -> coordinator.finishFailedOrAbandonedTransactions());
        assertTransactionsDisabled(() -> coordinator.getTransactionMap());
        assertTransactionsDisabled(() -> coordinator.beginTransaction(null, null, null));
        assertTransactionsDisabled(() -> coordinator.executeOperation(null, null, null, null, null, null));
        assertTransactionsDisabled(() -> coordinator.commitTransaction(null, null, null));
        assertTransactionsDisabled(() -> coordinator.rollbackTransaction(null, null, null));
    }

    @Test
    public void testExecuteOperationFailsAtBegin()
    {
        // "begin" should fail
        RuntimeException exception = new RuntimeException("Test begin exception");
        participant1.getDatabaseTransactionProvider().setBeginAction(() ->
        {
            throw exception;
        });

        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        coordinator.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        try
        {
            SpaceCreation spaceCreation = new SpaceCreation();
            spaceCreation.setCode(CODE_PREFIX + UUID.randomUUID());

            coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                    OPERATION_CREATE_SPACES, new Object[] { sessionToken, Collections.singletonList(spaceCreation) });
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "Transaction '" + coordinatorTrId + "' execute operation '" + OPERATION_CREATE_SPACES + "' for participant '"
                    + participant1.getParticipantId() + "' failed.");
            assertEquals(e.getCause().getMessage(),
                    "Begin transaction '" + coordinatorTrId + "' failed for participant '" + participant1.getParticipantId() + "'.");
            assertEquals(e.getCause().getCause().getCause(), exception);
        }

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());
    }

    @Test
    public void testExecuteOperationFails()
    {
        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        coordinator.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        SpaceCreation spaceCreation1 = new SpaceCreation();
        spaceCreation1.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation1) });

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap());

        try
        {
            coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant2.getParticipantId(),
                    OPERATION_CREATE_SPACES,
                    new Object[] { sessionToken, Collections.singletonList(new SpaceCreation()) });
            fail();
        } catch (TransactionOperationException e)
        {
            AssertionUtil.assertContains("Code cannot be empty", e.getCause().getMessage());
        }

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));

        Map<ISpaceId, Space> createdSpaces = v3api.getSpaces(sessionToken,
                Collections.singletonList(new SpacePermId(spaceCreation1.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 0);
    }

    @Test
    public void testExecuteOperationTimesOut() throws Exception
    {
        coordinator =
                createCoordinator(createConfiguration(true, 1, 10), Arrays.asList(participant1, participant2), TRANSACTION_LOG_COORDINATOR_FOLDER);

        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        coordinator.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation) });

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap());

        Thread.sleep(500);

        coordinator.finishFailedOrAbandonedTransactions();

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap());

        Thread.sleep(500);

        coordinator.finishFailedOrAbandonedTransactions();

        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        Map<ISpaceId, Space> createdSpaces = v3api.getSpaces(sessionToken,
                Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 0);
    }

    @Test
    public void testPrepareTransactionFails()
    {
        // "prepare" should fail
        RuntimeException exception = new RuntimeException("Test prepare exception");
        participant2.getDatabaseTransactionProvider().setPrepareAction(() ->
        {
            throw exception;
        });

        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        coordinator.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        SpaceCreation spaceCreation1 = new SpaceCreation();
        spaceCreation1.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation1) });

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap());

        SpaceCreation spaceCreation2 = new SpaceCreation();
        spaceCreation2.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant2.getParticipantId(),
                OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation2) });

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));

        try
        {
            coordinator.commitTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "Commit transaction '" + coordinatorTrId + "' failed.");
            assertEquals(e.getCause().getMessage(),
                    "Prepare transaction '" + coordinatorTrId + "' failed for participant '" + participant2.getParticipantId()
                            + "'. The transaction was rolled back.");
            assertEquals(e.getCause().getCause().getCause(), exception);
        }

        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        Map<ISpaceId, Space> createdSpaces = v3api.getSpaces(sessionToken,
                Arrays.asList(new SpacePermId(spaceCreation1.getCode()), new SpacePermId(spaceCreation2.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 0);
    }

    @Test
    public void testCommitTransactionFailsAndRecovers()
    {
        // "commit" should fail
        RuntimeException exception = new RuntimeException("Test commit exception");
        participant2.getDatabaseTransactionProvider().setCommitAction(() ->
        {
            throw exception;
        });

        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        coordinator.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        SpaceCreation spaceCreation1 = new SpaceCreation();
        spaceCreation1.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation1) });

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap());

        SpaceCreation spaceCreation2 = new SpaceCreation();
        spaceCreation2.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant2.getParticipantId(),
                OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation2) });

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));

        coordinator.commitTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.COMMIT_STARTED));
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.COMMIT_STARTED));

        // "commit" should succeed
        participant2.getDatabaseTransactionProvider().setCommitAction(null);

        coordinator.finishFailedOrAbandonedTransactions();

        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        Map<ISpaceId, Space> createdSpaces = v3api.getSpaces(sessionToken,
                Arrays.asList(new SpacePermId(spaceCreation1.getCode()), new SpacePermId(spaceCreation2.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 2);
    }

    @Test
    public void testRollbackTransactionFailsAndRecovers()
    {
        // "rollback" should fail
        RuntimeException exception = new RuntimeException("Test rollback exception");
        participant2.getDatabaseTransactionProvider().setRollbackAction(() ->
        {
            throw exception;
        });

        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        coordinator.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        SpaceCreation spaceCreation1 = new SpaceCreation();
        spaceCreation1.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation1) });

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap());

        SpaceCreation spaceCreation2 = new SpaceCreation();
        spaceCreation2.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant2.getParticipantId(),
                OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation2) });

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));

        coordinator.rollbackTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.ROLLBACK_STARTED));
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.ROLLBACK_STARTED));

        // "rollback" should succeed
        participant2.getDatabaseTransactionProvider().setRollbackAction(null);

        coordinator.finishFailedOrAbandonedTransactions();

        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        Map<ISpaceId, Space> createdSpaces = v3api.getSpaces(sessionToken,
                Arrays.asList(new SpacePermId(spaceCreation1.getCode()), new SpacePermId(spaceCreation2.getCode())), new SpaceFetchOptions());
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
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        coordinator.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        // participants create new spaces
        SearchResult<Space> participant1SpacesBeforeCreation =
                coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions() });

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap());

        SearchResult<Space> participant2SpacesBeforeCreation =
                coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant2.getParticipantId(),
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions() });

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));

        SearchResult<Space> noTrSpacesBeforeCreation = v3api.searchSpaces(sessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions());

        SpaceCreation spaceCreation1 = new SpaceCreation();
        spaceCreation1.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_CREATE_SPACES, new Object[] { sessionToken, Collections.singletonList(spaceCreation1) });

        SpaceCreation spaceCreation2 = new SpaceCreation();
        spaceCreation2.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant2.getParticipantId(),
                OPERATION_CREATE_SPACES, new Object[] { sessionToken, Collections.singletonList(spaceCreation2) });

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));

        SearchResult<Space> participant1SpacesAfterCreation =
                coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> participant2SpacesAfterCreation =
                coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant2.getParticipantId(),
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> noTrSpacesAfterCreation = v3api.searchSpaces(sessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions());

        // participants see their own new spaces, outside the transaction they are not visible
        assertEquals(Collections.singleton(spaceCreation1.getCode().toUpperCase()),
                difference(codes(participant1SpacesAfterCreation.getObjects()), codes(participant1SpacesBeforeCreation.getObjects())));
        assertEquals(Collections.singleton(spaceCreation2.getCode().toUpperCase()),
                difference(codes(participant2SpacesAfterCreation.getObjects()), codes(participant2SpacesBeforeCreation.getObjects())));
        assertEquals(Collections.emptySet(), difference(codes(noTrSpacesAfterCreation.getObjects()), codes(noTrSpacesBeforeCreation.getObjects())));

        // participants create projects in their new spaces
        SearchResult<Project> participant1ProjectsBeforeCreation =
                coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                        OPERATION_SEARCH_PROJECTS, new Object[] { sessionToken, new ProjectSearchCriteria(), new ProjectFetchOptions() });
        SearchResult<Project> participant2ProjectsBeforeCreation =
                coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant2.getParticipantId(),
                        OPERATION_SEARCH_PROJECTS, new Object[] { sessionToken, new ProjectSearchCriteria(), new ProjectFetchOptions() });
        SearchResult<Project> noTrProjectsBeforeCreation =
                v3api.searchProjects(sessionToken, new ProjectSearchCriteria(), new ProjectFetchOptions());

        ProjectCreation projectCreation1 = new ProjectCreation();
        projectCreation1.setSpaceId(new SpacePermId(spaceCreation1.getCode()));
        projectCreation1.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_CREATE_PROJECTS, new Object[] { sessionToken, Collections.singletonList(projectCreation1) });

        ProjectCreation projectCreation2 = new ProjectCreation();
        projectCreation2.setSpaceId(new SpacePermId(spaceCreation2.getCode()));
        projectCreation2.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant2.getParticipantId(),
                OPERATION_CREATE_PROJECTS, new Object[] { sessionToken, Collections.singletonList(projectCreation2) });

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));

        SearchResult<Project> participant1ProjectsAfterCreation =
                coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                        OPERATION_SEARCH_PROJECTS, new Object[] { sessionToken, new ProjectSearchCriteria(), new ProjectFetchOptions() });
        SearchResult<Project> participant2ProjectsAfterCreation =
                coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant2.getParticipantId(),
                        OPERATION_SEARCH_PROJECTS, new Object[] { sessionToken, new ProjectSearchCriteria(), new ProjectFetchOptions() });
        SearchResult<Project> noTrProjectsAfterCreation =
                v3api.searchProjects(sessionToken, new ProjectSearchCriteria(), new ProjectFetchOptions());

        // participants see their own new projects, outside the transaction they are not visible
        assertEquals(Collections.singleton("/" + spaceCreation1.getCode().toUpperCase() + "/" + projectCreation1.getCode().toUpperCase()),
                difference(identifiers(participant1ProjectsAfterCreation.getObjects()),
                        identifiers(participant1ProjectsBeforeCreation.getObjects())));
        assertEquals(Collections.singleton("/" + spaceCreation2.getCode().toUpperCase() + "/" + projectCreation2.getCode().toUpperCase()),
                difference(identifiers(participant2ProjectsAfterCreation.getObjects()),
                        identifiers(participant2ProjectsBeforeCreation.getObjects())));
        assertEquals(Collections.emptySet(),
                difference(identifiers(noTrProjectsAfterCreation.getObjects()), identifiers(noTrProjectsBeforeCreation.getObjects())));

        if (rollback)
        {
            coordinator.rollbackTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);
        } else
        {
            coordinator.commitTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);
        }

        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

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
            // both the spaces and projects are committed and are visible outside the transaction
            assertEquals(Set.of(spaceCreation1.getCode().toUpperCase(), spaceCreation2.getCode().toUpperCase()),
                    difference(codes(noTrSpacesAfterCommit.getObjects()), codes(noTrSpacesBeforeCreation.getObjects())));
            assertEquals(Set.of("/" + spaceCreation1.getCode().toUpperCase() + "/" + projectCreation1.getCode().toUpperCase(),
                            "/" + spaceCreation2.getCode().toUpperCase() + "/" + projectCreation2.getCode().toUpperCase()),
                    difference(identifiers(noTrProjectsAfterCommit.getObjects()), identifiers(noTrProjectsBeforeCreation.getObjects())));
        }
    }

    @Test
    public void testMultipleTransactions()
    {
        String sessionToken1 = v3api.login(TEST_USER, PASSWORD);
        String sessionToken2 = v3api.login(TEST_USER, PASSWORD);

        // begin tr1 and tr2
        coordinator.beginTransaction(coordinatorTrId, sessionToken1, TEST_INTERACTIVE_SESSION_KEY);
        coordinator.beginTransaction(coordinatorTr2Id, sessionToken2, TEST_INTERACTIVE_SESSION_KEY);

        SearchResult<Space> tr1SpacesBeforeCreations =
                coordinator.executeOperation(coordinatorTrId, sessionToken1, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> tr2SpacesBeforeCreations =
                coordinator.executeOperation(coordinatorTr2Id, sessionToken2, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken2, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> noTrSpacesBeforeCreations = v3api.searchSpaces(sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions());

        // create space1 in tr1
        SpaceCreation tr1Creation = new SpaceCreation();
        tr1Creation.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken1, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_CREATE_SPACES, new Object[] { sessionToken1, Collections.singletonList(tr1Creation) });

        // create space2 in tr2
        SpaceCreation tr2Creation = new SpaceCreation();
        tr2Creation.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTr2Id, sessionToken2, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_CREATE_SPACES, new Object[] { sessionToken2, Collections.singletonList(tr2Creation) });

        // create space3 in noTr
        SpaceCreation noTrCreation = new SpaceCreation();
        noTrCreation.setCode(CODE_PREFIX + UUID.randomUUID());
        v3api.createSpaces(sessionToken1, Collections.singletonList(noTrCreation));

        SearchResult<Space> tr1SpacesAfterCreations =
                coordinator.executeOperation(coordinatorTrId, sessionToken1, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> tr2SpacesAfterCreations =
                coordinator.executeOperation(coordinatorTr2Id, sessionToken2, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken2, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> noTrSpacesAfterCreations = v3api.searchSpaces(sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions());

        // check that tr1 sees only space1, tr2 sees only space2, noTr sees space3
        assertEquals(Collections.singleton(tr1Creation.getCode().toUpperCase()),
                difference(codes(tr1SpacesAfterCreations.getObjects()), codes(tr1SpacesBeforeCreations.getObjects())));
        assertEquals(Collections.singleton(tr2Creation.getCode().toUpperCase()),
                difference(codes(tr2SpacesAfterCreations.getObjects()), codes(tr2SpacesBeforeCreations.getObjects())));
        assertEquals(Collections.singleton(noTrCreation.getCode().toUpperCase()),
                difference(codes(noTrSpacesAfterCreations.getObjects()), codes(noTrSpacesBeforeCreations.getObjects())));

        coordinator.commitTransaction(coordinatorTrId, sessionToken1, TEST_INTERACTIVE_SESSION_KEY);

        try
        {
            coordinator.executeOperation(coordinatorTrId, sessionToken1, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                    OPERATION_SEARCH_SPACES, new Object[] { sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions() });
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "Transaction '" + coordinatorTrId + "' does not exist.");
        }

        // after tr1 commit, tr2 sees space1 and space2, noTr sees space1 and space3
        SearchResult<Space> tr2SpacesAfterTr1Commit =
                coordinator.executeOperation(coordinatorTr2Id, sessionToken2, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                        OPERATION_SEARCH_SPACES, new Object[] { sessionToken2, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        SearchResult<Space> noTrSpacesAfterTr1Commit = v3api.searchSpaces(sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions());

        assertEquals(Collections.singleton(tr1Creation.getCode().toUpperCase()),
                difference(codes(tr2SpacesAfterTr1Commit.getObjects()), codes(tr2SpacesAfterCreations.getObjects())));
        assertEquals(Collections.singleton(tr1Creation.getCode().toUpperCase()),
                difference(codes(noTrSpacesAfterTr1Commit.getObjects()), codes(noTrSpacesAfterCreations.getObjects())));

        coordinator.rollbackTransaction(coordinatorTr2Id, sessionToken2, TEST_INTERACTIVE_SESSION_KEY);

        try
        {
            coordinator.executeOperation(coordinatorTr2Id, sessionToken2, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                    OPERATION_SEARCH_SPACES, new Object[] { sessionToken2, new SpaceSearchCriteria(), new SpaceFetchOptions() });
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "Transaction '" + coordinatorTr2Id + "' does not exist.");
        }

        // after tr1 commit and tr2 rollback, noTr sees space1 and space3
        noTrSpacesAfterTr1Commit = v3api.searchSpaces(sessionToken1, new SpaceSearchCriteria(), new SpaceFetchOptions());
        assertEquals(Collections.singleton(tr1Creation.getCode().toUpperCase()),
                difference(codes(noTrSpacesAfterTr1Commit.getObjects()), codes(noTrSpacesAfterCreations.getObjects())));
    }

    @Test
    public void testMultipleConcurrentCallsToOneTransaction() throws InterruptedException
    {
        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        coordinator.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        SpaceCreation spaceCreation1 = new SpaceCreation();
        spaceCreation1.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_CREATE_SPACES,
                new Object[] { sessionToken, Collections.singletonList(spaceCreation1) });

        MessageChannel messageChannel1 = new MessageChannel(1000);
        MessageChannel messageChannel2 = new MessageChannel(1000);

        participant1.getDatabaseTransactionProvider().setCommitAction(() ->
        {
            messageChannel1.send("committing");
            messageChannel2.assertNextMessage("executed");
        });

        Thread committingThread = new Thread(() -> coordinator.commitTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY));
        committingThread.start();

        SpaceCreation spaceCreation2 = new SpaceCreation();
        spaceCreation2.setCode(CODE_PREFIX + UUID.randomUUID());

        messageChannel1.assertNextMessage("committing");

        try
        {
            // try to execute an operation while the commit is in progress
            coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant2.getParticipantId(),
                    OPERATION_CREATE_SPACES,
                    new Object[] { sessionToken, Collections.singletonList(spaceCreation2) });
            fail();
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Cannot execute a new action on transaction '" + coordinatorTrId + "' as it is still busy executing a previous action.");
        }

        messageChannel2.send("executed");

        committingThread.join();
    }

    @Test
    public void testTooManyTransactions()
    {
        coordinator =
                createCoordinator(createConfiguration(true, 60, 2), Arrays.asList(participant1, participant2), TRANSACTION_LOG_COORDINATOR_FOLDER);

        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        String sessionToken2 = v3api.login(TEST_USER, PASSWORD);
        String sessionToken3 = v3api.login(TEST_USER, PASSWORD);

        coordinator.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);
        coordinator.beginTransaction(coordinatorTr2Id, sessionToken2, TEST_INTERACTIVE_SESSION_KEY);

        try
        {
            coordinator.beginTransaction(coordinatorTr3Id, sessionToken3, TEST_INTERACTIVE_SESSION_KEY);
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Cannot create transaction '" + coordinatorTr3Id
                            + "' because the transaction count limit has been reached. Number of existing transactions: 2");
        }
    }

    @Test
    public void testOneTransactionPerSessionToken()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        String sessionToken2 = v3api.login(TEST_USER, PASSWORD);

        coordinator.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        try
        {
            coordinator.beginTransaction(coordinatorTr2Id, sessionToken, TEST_INTERACTIVE_SESSION_KEY);
        } catch (Exception e)
        {
            assertEquals(e.getMessage(),
                    "Cannot create more than one transaction for the same session token. Transaction that could not be created: '" + coordinatorTr2Id
                            + "'. The already existing and still active transaction: '" + coordinatorTrId + "'.");
        }

        coordinator.beginTransaction(coordinatorTr2Id, sessionToken2, TEST_INTERACTIVE_SESSION_KEY);

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED),
                new TestTransaction(coordinatorTr2Id, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());
    }

    @Test
    public void testTransactionAccess()
    {
        String regularUserSessionToken = v3api.login(TEST_SPACE_USER, PASSWORD);
        String regularUserSessionToken2 = v3api.login(TEST_POWER_USER_CISD, PASSWORD);
        String instanceAdminSessionToken = v3api.login(TEST_USER, PASSWORD);
        String systemSessionToken = v3api.loginAsSystem();

        coordinator.beginTransaction(coordinatorTrId, regularUserSessionToken, TEST_INTERACTIVE_SESSION_KEY);
        coordinator.beginTransaction(coordinatorTr2Id, regularUserSessionToken2, TEST_INTERACTIVE_SESSION_KEY);

        coordinator.executeOperation(coordinatorTrId, regularUserSessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_SEARCH_SPACES, new Object[] { regularUserSessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions() });

        try
        {
            // a different but regular user does not have access
            coordinator.executeOperation(coordinatorTrId, regularUserSessionToken2, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                    OPERATION_SEARCH_SPACES, new Object[] { regularUserSessionToken2, new SpaceSearchCriteria(), new SpaceFetchOptions() });
        } catch (Exception e)
        {
            assertEquals(e.getMessage(), "Access denied to transaction '" + coordinatorTrId + "'");
        }

        // instance admin user has access to any transaction
        coordinator.executeOperation(coordinatorTrId, instanceAdminSessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_SEARCH_SPACES, new Object[] { instanceAdminSessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions() });

        // system user has access to any transaction
        coordinator.executeOperation(coordinatorTrId, systemSessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_SEARCH_SPACES, new Object[] { systemSessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions() });
    }

    @Test
    public void testRecoveryOfCoordinatorWithTransactionToRollback()
    {
        TestTransactionCoordinatorApi coordinatorBeforeCrash =
                createCoordinator(createConfiguration(true, 60, 10), Arrays.asList(participant1, participant2), TRANSACTION_LOG_COORDINATOR_FOLDER);

        assertTransactions(coordinatorBeforeCrash.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        coordinatorBeforeCrash.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinatorBeforeCrash.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_CREATE_SPACES, new Object[] { sessionToken, Collections.singletonList(spaceCreation) });

        // new coordinator
        TestTransactionCoordinatorApi coordinatorAfterCrash =
                createCoordinator(createConfiguration(true, 60, 10), Arrays.asList(participant1, participant2), TRANSACTION_LOG_COORDINATOR_FOLDER);

        assertTransactions(coordinatorAfterCrash.getTransactionMap());
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap());

        coordinatorAfterCrash.recoverTransactionsFromTransactionLog();

        assertTransactions(coordinatorAfterCrash.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap());

        coordinatorAfterCrash.finishFailedOrAbandonedTransactions();

        assertTransactions(coordinatorAfterCrash.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        Map<ISpaceId, Space> createdSpaces = v3api.getSpaces(sessionToken,
                Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 0);
    }

    @Test
    public void testRecoveryOfCoordinatorWithTransactionToCommit()
    {
        // "commit" for both participants should fail
        RuntimeException exception = new RuntimeException("Test prepare exception");

        participant1.getDatabaseTransactionProvider().setCommitAction(() ->
        {
            throw exception;
        });
        participant2.getDatabaseTransactionProvider().setCommitAction(() ->
        {
            throw exception;
        });

        TestTransactionCoordinatorApi coordinatorBeforeCrash =
                createCoordinator(createConfiguration(true, 60, 10), Arrays.asList(participant1, participant2), TRANSACTION_LOG_COORDINATOR_FOLDER);

        assertTransactions(coordinatorBeforeCrash.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        coordinatorBeforeCrash.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinatorBeforeCrash.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_CREATE_SPACES, new Object[] { sessionToken, Collections.singletonList(spaceCreation) });

        coordinatorBeforeCrash.commitTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        assertTransactions(coordinatorBeforeCrash.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.COMMIT_STARTED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.COMMIT_STARTED));
        assertTransactions(participant2.getTransactionMap());

        // new coordinator
        TestTransactionCoordinatorApi coordinatorAfterCrash =
                createCoordinator(createConfiguration(true, 60, 10), Arrays.asList(participant1, participant2), TRANSACTION_LOG_COORDINATOR_FOLDER);

        // "commit" for both participants should succeed
        participant1.getDatabaseTransactionProvider().setCommitAction(null);
        participant2.getDatabaseTransactionProvider().setCommitAction(null);

        assertTransactions(coordinatorAfterCrash.getTransactionMap());
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.COMMIT_STARTED));
        assertTransactions(participant2.getTransactionMap());

        Map<ISpaceId, Space> createdSpaces = v3api.getSpaces(sessionToken,
                Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 0);

        coordinatorAfterCrash.recoverTransactionsFromTransactionLog();

        assertTransactions(coordinatorAfterCrash.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.COMMIT_STARTED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.COMMIT_STARTED));
        assertTransactions(participant2.getTransactionMap());

        coordinatorAfterCrash.finishFailedOrAbandonedTransactions();

        assertTransactions(coordinatorAfterCrash.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        createdSpaces = v3api.getSpaces(sessionToken,
                Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 1);
    }

    @Test
    public void testRecoveryOfParticipantWithTransactionToRollback()
    {
        List<ITransactionParticipant> participants = new ArrayList<>();
        participants.add(participant1);
        participants.add(participant2);

        TestTransactionCoordinatorApi coordinator =
                createCoordinator(createConfiguration(true, 60, 10), participants, TRANSACTION_LOG_COORDINATOR_FOLDER);

        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        coordinator.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

        SpaceCreation spaceCreation = new SpaceCreation();
        spaceCreation.setCode(CODE_PREFIX + UUID.randomUUID());

        coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                OPERATION_CREATE_SPACES, new Object[] { sessionToken, Collections.singletonList(spaceCreation) });

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap());

        // replace original participant with a new instance
        TestTransactionParticipantApi participant1AfterCrash =
                createParticipant(createConfiguration(true, 60, 10), TEST_PARTICIPANT_1_ID, TRANSACTION_LOG_PARTICIPANT_1_FOLDER);
        participant1AfterCrash.setTestTransactionMapping(participant1.getTestTransactionMapping());
        participants.set(0, participant1AfterCrash);

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1AfterCrash.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        Map<ISpaceId, Space> createdSpaces = v3api.getSpaces(sessionToken,
                Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 0);

        participant1AfterCrash.recoverTransactionsFromTransactionLog();

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1AfterCrash.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant2.getTransactionMap());

        participant1AfterCrash.finishFailedOrAbandonedTransactions();

        createdSpaces = v3api.getSpaces(sessionToken,
                Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 0);

        assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.BEGIN_FINISHED));
        assertTransactions(participant1AfterCrash.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        try
        {
            coordinator.commitTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);
        } catch (Exception e)
        {
            // thrown by coordinator
            assertEquals(e.getMessage(), "Commit transaction '" + coordinatorTrId + "' failed.");
            assertEquals(e.getCause().getMessage(),
                    "Prepare transaction '" + coordinatorTrId + "' failed for participant '" + participant1.getParticipantId()
                            + "'. The transaction was rolled back.");
            // thrown by participant
            assertEquals(e.getCause().getCause().getMessage(), "Transaction '" + participant1TrId + "' does not exist.");
        }

        assertTransactions(coordinator.getTransactionMap());
        assertTransactions(participant1AfterCrash.getTransactionMap());
        assertTransactions(participant2.getTransactionMap());

        createdSpaces = v3api.getSpaces(sessionToken,
                Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
        assertEquals(createdSpaces.size(), 0);

        participant1AfterCrash.close();
    }

    @Test
    public void testRecoveryOfParticipantWithTransactionToCommit()
    {
        TestTransactionParticipantApi participant1AfterCrash = null;

        try
        {
            List<ITransactionParticipant> participants = new ArrayList<>();
            participants.add(participant1);
            participants.add(participant2);

            coordinator = createCoordinator(createConfiguration(true, 60, 10), participants, TRANSACTION_LOG_COORDINATOR_FOLDER);

            assertTransactions(coordinator.getTransactionMap());
            assertTransactions(participant1.getTransactionMap());
            assertTransactions(participant2.getTransactionMap());

            String sessionToken = v3api.login(TEST_USER, PASSWORD);

            coordinator.beginTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

            SpaceCreation spaceCreation = new SpaceCreation();
            spaceCreation.setCode(CODE_PREFIX + UUID.randomUUID());

            coordinator.executeOperation(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY, participant1.getParticipantId(),
                    OPERATION_CREATE_SPACES, new Object[] { sessionToken, Collections.singletonList(spaceCreation) });

            // "prepare" should fail
            RuntimeException exception = new RuntimeException("Test commit exception");
            participant1.getDatabaseTransactionProvider().setCommitAction(() ->
            {
                throw exception;
            });

            coordinator.commitTransaction(coordinatorTrId, sessionToken, TEST_INTERACTIVE_SESSION_KEY);

            assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.COMMIT_STARTED));
            assertTransactions(participant1.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.COMMIT_STARTED));
            assertTransactions(participant2.getTransactionMap());

            participant1AfterCrash =
                    createParticipant(createConfiguration(true, 60, 10), TEST_PARTICIPANT_1_ID, TRANSACTION_LOG_PARTICIPANT_1_FOLDER);
            participant1AfterCrash.setTestTransactionMapping(Map.of(coordinatorTrId, participant1TrId));
            // replace original participant with a new instance
            participants.set(0, participant1AfterCrash);

            assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.COMMIT_STARTED));
            assertTransactions(participant1AfterCrash.getTransactionMap());
            assertTransactions(participant2.getTransactionMap());

            Map<ISpaceId, Space> createdSpaces = v3api.getSpaces(sessionToken,
                    Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
            assertEquals(createdSpaces.size(), 0);

            participant1AfterCrash.recoverTransactionsFromTransactionLog();

            assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.COMMIT_STARTED));
            assertTransactions(participant1AfterCrash.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.COMMIT_STARTED));
            assertTransactions(participant2.getTransactionMap());

            participant1AfterCrash.finishFailedOrAbandonedTransactions();

            createdSpaces = v3api.getSpaces(sessionToken,
                    Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
            assertEquals(createdSpaces.size(), 1);

            assertTransactions(coordinator.getTransactionMap(), new TestTransaction(coordinatorTrId, TransactionStatus.COMMIT_STARTED));
            assertTransactions(participant1AfterCrash.getTransactionMap());
            assertTransactions(participant2.getTransactionMap());

            coordinator.finishFailedOrAbandonedTransactions();

            assertTransactions(coordinator.getTransactionMap());
            assertTransactions(participant1AfterCrash.getTransactionMap());
            assertTransactions(participant2.getTransactionMap());

            createdSpaces = v3api.getSpaces(sessionToken,
                    Collections.singletonList(new SpacePermId(spaceCreation.getCode())), new SpaceFetchOptions());
            assertEquals(createdSpaces.size(), 1);

            participant1AfterCrash.close();
        } finally
        {
            if (participant1AfterCrash != null)
            {
                participant1AfterCrash.close();
            }
        }
    }

}
