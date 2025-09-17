package ch.ethz.sis.messages.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import ch.ethz.sis.shared.log.classic.impl.Logger;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;

public class MessagesDatabase implements IMessagesDatabase
{

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION, MessagesDatabase.class);

    private final DataSource dataSource;

    private Connection connection;

    public MessagesDatabase(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override public void begin()
    {
        if (connection != null)
        {
            throw new IllegalStateException("Cannot start a new transaction as the previous one was never committed or rolled back.");
        }
        try
        {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override public MessagesDAO getMessagesDAO()
    {
        if (connection == null)
        {
            throw new IllegalStateException("A transaction hasn't been started.");
        }
        return new MessagesDAO(connection);
    }

    @Override public LastSeenMessagesDAO getLastSeenMessagesDAO()
    {
        if (connection == null)
        {
            throw new IllegalStateException("A transaction hasn't been started.");
        }
        return new LastSeenMessagesDAO(connection);
    }

    @Override public void rollback()
    {
        if (connection == null)
        {
            return;
        }
        try
        {
            connection.rollback();
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            try
            {
                connection.close();
            } catch (SQLException e)
            {
                operationLog.warn("Could not close database connection", e);
            } finally
            {
                connection = null;
            }
        }
    }

    @Override public void commit()
    {
        if (connection == null)
        {
            throw new IllegalStateException("Cannot commit a transaction as it was never started.");
        }
        try
        {
            connection.commit();
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            try
            {
                connection.close();
            } catch (SQLException e)
            {
                operationLog.warn("Could not close database connection", e);
            } finally
            {
                connection = null;
            }
        }
    }

}
