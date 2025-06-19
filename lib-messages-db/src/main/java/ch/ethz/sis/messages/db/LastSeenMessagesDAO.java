package ch.ethz.sis.messages.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LastSeenMessagesDAO implements ILastSeenMessagesDAO
{

    private static final String CREATE_SQL =
            "INSERT INTO LAST_SEEN_MESSAGES (ID, LAST_SEEN_MESSAGE_ID, CONSUMER_ID) VALUES (?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE LAST_SEEN_MESSAGES SET LAST_SEEN_MESSAGE_ID = ? WHERE ID = ?";

    private static final String GET_NEXT_ID_SQL =
            "SELECT NEXTVAL('LAST_SEEN_MESSAGES_ID_SEQ') AS ID";

    private static final String GET_BY_CONSUMER_ID_SQL =
            "SELECT ID, LAST_SEEN_MESSAGE_ID, CONSUMER_ID FROM LAST_SEEN_MESSAGES WHERE CONSUMER_ID = ?";

    private final Connection connection;

    public LastSeenMessagesDAO(Connection connection)
    {
        this.connection = connection;
    }

    @Override public Long create(final LastSeenMessage lastSeenMessage)
    {
        try (PreparedStatement statement = connection.prepareStatement(CREATE_SQL))
        {
            Long id = getNextId();
            statement.setLong(1, id);
            statement.setLong(2, lastSeenMessage.getLastSeenMessageId());
            statement.setString(3, lastSeenMessage.getConsumerId());
            statement.executeUpdate();
            return id;
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override public void update(final LastSeenMessage lastSeenMessage)
    {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL))
        {
            statement.setLong(1, lastSeenMessage.getLastSeenMessageId());
            statement.setLong(2, lastSeenMessage.getId());
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

    @Override public LastSeenMessage getByConsumerId(final String consumerId)
    {
        try (PreparedStatement statement = connection.prepareStatement(GET_BY_CONSUMER_ID_SQL))
        {
            statement.setString(1, consumerId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next())
            {
                LastSeenMessage lastSeenMessage = new LastSeenMessage();
                lastSeenMessage.setId(resultSet.getLong("id"));
                lastSeenMessage.setLastSeenMessageId(resultSet.getLong("last_seen_message_id"));
                lastSeenMessage.setConsumerId(resultSet.getString("consumer_id"));
                return lastSeenMessage;
            } else
            {
                return null;
            }
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

}
