package ch.ethz.sis.messages.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessagesDAO implements IMessagesDAO
{

    private static final String CREATE_SQL =
            "INSERT INTO MESSAGES (ID, TYPE, DESCRIPTION, META_DATA, PROCESS_ID) VALUES (?, ?, ?, ?::JSONB, ?)";

    private static final String UPDATE_SQL =
            "UPDATE MESSAGES SET CONSUMPTION_TIMESTAMP = ? WHERE ID = ?";

    private static final String GET_NEXT_ID_SQL =
            "SELECT NEXTVAL('MESSAGES_ID_SEQ') AS ID";

    private static final String GET_NEWEST_BY_TYPES_SQL =
            "SELECT ID, TYPE, DESCRIPTION, META_DATA, PROCESS_ID, CREATION_TIMESTAMP, CONSUMPTION_TIMESTAMP FROM MESSAGES WHERE TYPE IN (SELECT UNNEST(?)) ORDER BY ID DESC LIMIT 1";

    private static final String LIST_BY_TYPES_AND_ID_RANGE_SQL =
            "SELECT ID, TYPE, DESCRIPTION, META_DATA, PROCESS_ID, CREATION_TIMESTAMP, CONSUMPTION_TIMESTAMP FROM MESSAGES WHERE TYPE IN (SELECT UNNEST(?)) AND ID > ? AND ID <= ? ORDER BY ID ASC LIMIT ?";

    private static final String LIST_BY_TYPES_NOT_CONSUMED_SQL =
            "SELECT ID, TYPE, DESCRIPTION, META_DATA, PROCESS_ID, CREATION_TIMESTAMP, CONSUMPTION_TIMESTAMP FROM MESSAGES WHERE TYPE IN (SELECT UNNEST(?)) AND CONSUMPTION_TIMESTAMP IS NULL ORDER BY ID ASC;";

    private final Connection connection;

    public MessagesDAO(Connection connection)
    {
        this.connection = connection;
    }

    @Override public Long create(final Message message)
    {
        try (PreparedStatement createStatement = connection.prepareStatement(CREATE_SQL))
        {
            Long id = getNextId();
            createStatement.setLong(1, id);
            createStatement.setString(2, message.getType());
            createStatement.setString(3, message.getDescription());
            createStatement.setObject(4, message.getMetaData());
            createStatement.setString(5, message.getProcessId());
            createStatement.executeUpdate();
            return id;
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override public void update(final Message message)
    {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL))
        {
            statement.setTimestamp(1,
                    message.getConsumptionTimestamp() != null ? new java.sql.Timestamp(message.getConsumptionTimestamp().getTime()) : null);
            statement.setLong(2, message.getId());
            statement.executeUpdate();
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Long getNextId()
    {
        try (PreparedStatement statement = connection.prepareStatement(GET_NEXT_ID_SQL))
        {
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getLong("id");
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override public Message getNewestByTypes(final List<String> messageTypes)
    {
        try (PreparedStatement statement = connection.prepareStatement(GET_NEWEST_BY_TYPES_SQL))
        {
            statement.setObject(1, messageTypes.toArray(new String[0]));

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next())
            {
                return getMessage(resultSet);
            } else
            {
                return null;
            }
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override public List<Message> listByTypesAndIdRange(List<String> messageTypes, Long minMessageId, Long maxMessageId, int messageBatchSize)
    {
        try (PreparedStatement statement = connection.prepareStatement(LIST_BY_TYPES_AND_ID_RANGE_SQL))
        {
            statement.setObject(1, messageTypes.toArray(new String[0]));
            statement.setLong(2, minMessageId != null ? minMessageId : 0);
            statement.setLong(3, maxMessageId != null ? maxMessageId : 0);
            statement.setInt(4, messageBatchSize);

            List<Message> result = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next())
            {
                result.add(getMessage(resultSet));
            }

            return result;
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override public List<Message> listByTypesNotConsumed(List<String> messageTypes)
    {
        try (PreparedStatement statement = connection.prepareStatement(LIST_BY_TYPES_NOT_CONSUMED_SQL))
        {
            statement.setObject(1, messageTypes.toArray(new String[0]));

            List<Message> result = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next())
            {
                result.add(getMessage(resultSet));
            }

            return result;
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Message getMessage(ResultSet resultSet) throws SQLException
    {
        Message message = new Message();
        message.setId(resultSet.getLong("id"));
        message.setType(resultSet.getString("type"));
        message.setDescription(resultSet.getString("description"));
        message.setMetaData(resultSet.getString("meta_data"));
        message.setProcessId(resultSet.getString("process_id"));
        message.setCreationTimestamp(resultSet.getTimestamp("creation_timestamp"));
        message.setConsumptionTimestamp(resultSet.getTimestamp("consumption_timestamp"));
        return message;
    }

}
