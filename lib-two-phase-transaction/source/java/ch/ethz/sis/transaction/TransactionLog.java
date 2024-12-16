package ch.ethz.sis.transaction;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;

public class TransactionLog implements ITransactionLog
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, TransactionLog.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final File logFolder;

    private final Map<UUID, TransactionLogEntry> transactionsMap;

    public TransactionLog(File rootLogFolder, String subFolderName)
    {
        if (rootLogFolder == null)
        {
            throw new IllegalArgumentException("Transactions log folder cannot be null");
        }

        if (subFolderName == null)
        {
            throw new IllegalArgumentException("Transaction log subfolder cannot be null");
        }

        try
        {
            createOrCheckFolder(rootLogFolder);
        } catch (Exception e)
        {
            throw new RuntimeException("Could not prepare transactions log folder '" + rootLogFolder + "'.", e);
        }

        File logFolder = new File(rootLogFolder, subFolderName);

        try
        {
            createOrCheckFolder(logFolder);
        } catch (Exception e)
        {
            throw new RuntimeException("Could not prepare transactions log folder '" + logFolder + "'.", e);
        }

        this.logFolder = logFolder;
        this.transactionsMap = loadTransactions(logFolder);
    }

    @Override public void logTransaction(final TransactionLogEntry transaction)
    {
        File transactionLogFile = new File(logFolder, transaction.getTransactionId().toString());

        try
        {
            String fileContent = objectMapper.writeValueAsString(transaction);
            createOrUpdateFile(transactionLogFile, fileContent);
        } catch (Exception e)
        {
            throw new RuntimeException(
                    "Could not log transaction '" + transaction.getTransactionId() + " with status '" + transaction.getTransactionStatus()
                            + "' into file '" + transactionLogFile + "'.",
                    e);
        }

        transactionsMap.put(transaction.getTransactionId(), transaction);

        operationLog.info("Logged transaction '" + transaction.getTransactionId() + "' with status '" + transaction.getTransactionStatus() + "'.");
    }

    @Override public void deleteTransaction(final UUID transactionId)
    {
        File transactionLogFile = new File(logFolder, transactionId.toString());

        boolean deleted = FileUtilities.delete(transactionLogFile);

        if (deleted)
        {
            transactionsMap.remove(transactionId);
            operationLog.info("Deleted transaction '" + transactionId + "' log stored in '" + transactionLogFile + "' file.");
        } else
        {
            throw new RuntimeException("Could not delete transaction '" + transactionId + "' log stored in '" + transactionLogFile + "' file.");
        }
    }

    @Override public Map<UUID, TransactionLogEntry> getTransactions()
    {
        return new HashMap<>(transactionsMap);
    }

    private static Map<UUID, TransactionLogEntry> loadTransactions(File logFolder)
    {
        operationLog.info("Loading transactions from folder '" + logFolder + "'.");

        if (!logFolder.exists() || !logFolder.isDirectory())
        {
            throw new RuntimeException("Transactions log folder '" + logFolder + "' does not exist or is not a directory.");
        }

        File[] transactionFiles = logFolder.listFiles();

        if (transactionFiles == null)
        {
            throw new RuntimeException("Could not load the contents of the transaction log folder '" + logFolder + "'.");
        }

        Map<UUID, TransactionLogEntry> transactionsMap = new ConcurrentHashMap<>();

        for (File transactionFile : transactionFiles)
        {
            if (transactionFile.isFile())
            {
                try
                {
                    UUID.fromString(transactionFile.getName());
                } catch (IllegalArgumentException e)
                {
                    operationLog.info("Ignoring file '" + transactionFile + "'. File name in UUID format is expected for a transaction file.");
                    continue;
                }

                try
                {
                    String fileContent = FileUtilities.loadToString(transactionFile);
                    TransactionLogEntry logEntry = objectMapper.readValue(fileContent, TransactionLogEntry.class);
                    transactionsMap.put(logEntry.getTransactionId(), logEntry);
                } catch (Exception e)
                {
                    throw new RuntimeException("Could not load transaction from file '" + transactionFile + "'.");
                }
            } else
            {
                operationLog.info(
                        "Ignoring directory '" + transactionFile + "'. Only files that represent transactions are expected to be found in '"
                                + logFolder + "'.");
            }
        }

        return transactionsMap;
    }

    private static void createOrCheckFolder(File folder)
    {
        if (folder.exists() && !folder.isDirectory())
        {
            throw new IllegalArgumentException("Folder '" + folder.getAbsolutePath() + "' is not a directory");
        }

        if (!folder.exists())
        {
            boolean created = false;
            Exception exception = null;

            try
            {
                created = folder.mkdir();
            } catch (Exception e)
            {
                exception = e;
            }

            if (!created)
            {
                throw new RuntimeException("Could not create folder '" + folder.getAbsolutePath() + "'.", exception);
            }
        }

        if (!folder.canWrite())
        {
            throw new IllegalArgumentException("Cannot write to folder '" + folder.getAbsolutePath() + "'.");
        }
    }

    private static void createOrUpdateFile(File file, String content)
    {
        boolean success = false;
        Exception exception = null;

        try
        {
            if (file.exists() && !file.isFile())
            {
                throw new RuntimeException("File '" + file + "' is not a regular file.");
            }

            FileUtilities.writeToFile(file, content);
            success = file.setLastModified(System.currentTimeMillis());
        } catch (Exception e)
        {
            exception = e;
        }

        if (!success)
        {
            throw new RuntimeException("Could not create or update file '" + file + "'.", exception);
        }
    }

}
