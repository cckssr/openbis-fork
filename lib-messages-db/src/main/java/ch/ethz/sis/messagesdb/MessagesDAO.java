package ch.ethz.sis.messagesdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

public class MessagesDAO implements IMessagesDAO
{

    private static final String CREATE_SQL =
            "INSERT INTO MESSAGES (ID, TYPE, DESCRIPTION, META_DATA, PROCESS_ID) VALUES (NEXTVAL('MESSAGES_ID_SEQ'), ?, ?, ?, ?)";

    private static final String LIST_BY_TYPE_PREFIX_AND_LAST_SEEN_ID_SQL =
            "SELECT ID, TYPE, DESCRIPTION, META_DATA, PROCESS_ID, CREATION_TIMESTAMP FROM MESSAGES WHERE TYPE LIKE ? AND LAST_SEEN_ID > ?";

    private final DataSource dataSource;

    public MessagesDAO(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override public void create(final Message message)
    {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_SQL);)
        {
            statement.setString(1, message.getType());
            statement.setString(2, message.getDescription());
            statement.setString(3, message.getMetaData());
            statement.setString(4, message.getProcessId());
            statement.executeUpdate();
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override public List<Message> listByTypePrefixAndLastSeenId(final String typePrefix, final Long lastSeenId)
    {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(LIST_BY_TYPE_PREFIX_AND_LAST_SEEN_ID_SQL);)
        {
            statement.setString(1, typePrefix);
            statement.setLong(2, lastSeenId);

            List<Message> result = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next())
            {
                Message message = new Message();
                message.setId(resultSet.getLong("id"));
                message.setType(resultSet.getString("type"));
                message.setDescription(resultSet.getString("description"));
                message.setMetaData(resultSet.getString("meta_data"));
                message.setProcessId(resultSet.getString("process_id"));
                message.setCreationTimestamp(resultSet.getTimestamp("creation_timestamp"));
                result.add(message);
            }

            return result;
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

}
