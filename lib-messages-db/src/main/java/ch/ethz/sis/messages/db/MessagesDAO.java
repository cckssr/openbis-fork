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
            "INSERT INTO MESSAGES (ID, TYPE, DESCRIPTION, META_DATA, PROCESS_ID) VALUES (NEXTVAL('MESSAGES_ID_SEQ'), ?, ?, ?::JSONB, ?)";

    private static final String UPDATE_SQL =
            "UPDATE MESSAGES SET CONSUMPTION_TIMESTAMP = ? WHERE ID = ?";

    private static final String GET_NEWEST_BY_TYPES =
            "SELECT ID, TYPE, DESCRIPTION, META_DATA, PROCESS_ID, CREATION_TIMESTAMP, CONSUMPTION_TIMESTAMP FROM MESSAGES WHERE TYPE IN (SELECT UNNEST(?)) ORDER BY ID DESC LIMIT 1";

    private static final String LIST_BY_TYPES_AND_ID_RANGE =
            "SELECT ID, TYPE, DESCRIPTION, META_DATA, PROCESS_ID, CREATION_TIMESTAMP, CONSUMPTION_TIMESTAMP FROM MESSAGES WHERE TYPE IN (SELECT UNNEST(?)) AND ID > ? AND ID <= ? ORDER BY ID ASC LIMIT ?";

    private final Connection connection;

    public MessagesDAO(Connection connection)
    {
        this.connection = connection;
    }

    @Override public void create(final Message message)
    {
        try (PreparedStatement statement = connection.prepareStatement(CREATE_SQL);)
        {
            statement.setString(1, message.getType());
            statement.setString(2, message.getDescription());
            statement.setObject(3, message.getMetaData());
            statement.setString(4, message.getProcessId());
            statement.executeUpdate();
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

    @Override public Message getNewestByTypes(final List<String> messageTypes)
    {
        try (PreparedStatement statement = connection.prepareStatement(GET_NEWEST_BY_TYPES))
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
        try (PreparedStatement statement = connection.prepareStatement(LIST_BY_TYPES_AND_ID_RANGE))
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
