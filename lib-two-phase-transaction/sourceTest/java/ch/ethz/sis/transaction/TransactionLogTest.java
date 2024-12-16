package ch.ethz.sis.transaction;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.systemsx.cisd.common.filesystem.FileUtilities;

public class TransactionLogTest
{

    public static final UUID TEST_TRANSACTION_ID = UUID.randomUUID();

    public static final UUID TEST_TRANSACTION_ID_2 = UUID.randomUUID();

    public static final ObjectMapper objectMapper = new ObjectMapper();

    private File testWorkspace;

    @BeforeTest
    protected void beforeTest() throws IOException
    {
        testWorkspace = Files.createTempDirectory(TransactionLogTest.class.getSimpleName()).toFile();
    }

    @AfterTest
    protected void afterTest()
    {
        if (testWorkspace != null && testWorkspace.exists())
        {
            FileUtilities.deleteRecursively(testWorkspace);
        }
    }

    @Test
    public void testCreateWithNonExistentFolder()
    {
        File nonExistentRootLogFolder = new File(testWorkspace, UUID.randomUUID().toString());
        File nonExistentLogFolder = new File(nonExistentRootLogFolder, UUID.randomUUID().toString());

        assertFalse(nonExistentRootLogFolder.exists());
        assertFalse(nonExistentLogFolder.exists());

        new TransactionLog(nonExistentRootLogFolder, nonExistentLogFolder.getName());

        assertTrue(nonExistentRootLogFolder.exists());
        assertTrue(nonExistentRootLogFolder.isDirectory());

        assertTrue(nonExistentLogFolder.exists());
        assertTrue(nonExistentLogFolder.isDirectory());
    }

    @Test
    public void testCreateWithExistingFolderAndStatuses() throws IOException
    {
        File existingRootLogFolder = new File(testWorkspace, UUID.randomUUID().toString());
        Files.createDirectory(existingRootLogFolder.toPath());
        assertTrue(existingRootLogFolder.exists());

        File existingLogFolder = new File(existingRootLogFolder, UUID.randomUUID().toString());
        Files.createDirectory(existingLogFolder.toPath());
        assertTrue(existingLogFolder.exists());

        TransactionLogEntry transaction1LogEntry = new TransactionLogEntry();
        transaction1LogEntry.setTransactionId(TEST_TRANSACTION_ID);
        transaction1LogEntry.setTwoPhaseTransaction(true);
        transaction1LogEntry.setTransactionStatus(TransactionStatus.PREPARE_STARTED);
        createFile(new File(existingLogFolder, TEST_TRANSACTION_ID.toString()), objectMapper.writeValueAsString(transaction1LogEntry));

        TransactionLogEntry transaction2LogEntry = new TransactionLogEntry();
        transaction2LogEntry.setTransactionId(TEST_TRANSACTION_ID_2);
        transaction2LogEntry.setTwoPhaseTransaction(false);
        transaction2LogEntry.setTransactionStatus(TransactionStatus.ROLLBACK_FINISHED);
        createFile(new File(existingLogFolder, TEST_TRANSACTION_ID_2.toString()), objectMapper.writeValueAsString(transaction2LogEntry));

        createFolder(new File(existingLogFolder, "some_folder"));
        createFile(new File(existingLogFolder, "some_file_with_name_which_is_not_status"), "some_content");

        ITransactionLog transactionLog = new TransactionLog(existingRootLogFolder, existingLogFolder.getName());

        Map<UUID, TransactionLogEntry> logEntries = transactionLog.getTransactions();
        assertEquals(logEntries.size(), 2);

        assertEquals(logEntries.get(TEST_TRANSACTION_ID), transaction1LogEntry);
        assertEquals(logEntries.get(TEST_TRANSACTION_ID_2), transaction2LogEntry);
    }

    @Test
    public void testCreateWithNotAFolder() throws IOException
    {
        File existingFile = new File(testWorkspace, UUID.randomUUID().toString());
        Files.createFile(existingFile.toPath());
        assertTrue(existingFile.exists());

        try
        {
            new TransactionLog(existingFile, UUID.randomUUID().toString());
        } catch (Exception e)
        {
            assertEquals(e.getCause().getMessage(), "Folder '" + existingFile.getAbsolutePath() + "' is not a directory");
        }
    }

    @Test
    public void testLogStatus() throws IOException
    {
        File rootLogFolder = new File(testWorkspace, UUID.randomUUID().toString());
        File logFolder = new File(rootLogFolder, UUID.randomUUID().toString());

        ITransactionLog transactionLog = new TransactionLog(rootLogFolder, logFolder.getName());

        assertTransactionFiles(logFolder);

        TransactionLogEntry transaction1ALogEntry = new TransactionLogEntry();
        transaction1ALogEntry.setTransactionId(TEST_TRANSACTION_ID);
        transaction1ALogEntry.setTwoPhaseTransaction(true);
        transaction1ALogEntry.setTransactionStatus(TransactionStatus.BEGIN_STARTED);
        transactionLog.logTransaction(transaction1ALogEntry);

        TransactionLogEntry transaction2LogEntry = new TransactionLogEntry();
        transaction2LogEntry.setTransactionId(TEST_TRANSACTION_ID_2);
        transaction2LogEntry.setTwoPhaseTransaction(false);
        transaction2LogEntry.setTransactionStatus(TransactionStatus.PREPARE_STARTED);
        transactionLog.logTransaction(transaction2LogEntry);

        TransactionLogEntry transaction1BLogEntry = new TransactionLogEntry();
        transaction1BLogEntry.setTransactionId(TEST_TRANSACTION_ID);
        transaction1BLogEntry.setTwoPhaseTransaction(true);
        transaction1BLogEntry.setTransactionStatus(TransactionStatus.BEGIN_FINISHED);
        transactionLog.logTransaction(transaction1BLogEntry);

        assertTransactionFiles(logFolder, transaction1BLogEntry, transaction2LogEntry);
    }

    private void assertTransactionFiles(File logFolder, TransactionLogEntry... expectedLogEntries) throws IOException
    {
        Map<String, TransactionLogEntry> expectedLogEntriesMap = new HashMap<>();
        Map<String, TransactionLogEntry> actualLogEntriesMap = new HashMap<>();

        for (TransactionLogEntry expectedLogEntry : expectedLogEntries)
        {
            expectedLogEntriesMap.put(expectedLogEntry.getTransactionId().toString(), expectedLogEntry);
        }

        for (File actualLogFile : list(logFolder))
        {
            String actualFileContent = FileUtilities.loadToString(actualLogFile);
            actualLogEntriesMap.put(actualLogFile.getName(), objectMapper.readValue(actualFileContent, TransactionLogEntry.class));
        }

        assertEquals(actualLogEntriesMap, expectedLogEntriesMap);
    }

    private static void createFolder(File folder) throws IOException
    {
        Files.createDirectory(folder.toPath());
    }

    private static void createFile(File file, String content)
    {
        FileUtilities.writeToFile(file, content);
    }

    private static List<File> list(File folder) throws IOException
    {
        return Files.list(folder.toPath()).map(Path::toFile).collect(Collectors.toList());
    }

}
