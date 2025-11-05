package ch.openbis.drive.db;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.model.Notification;
import lombok.NonNull;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAOImpl implements NotificationDAO {
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static final String DB_FILE_NAME = "openbis-drive-notifications.db";
    private final Configuration configuration;

    public NotificationDAOImpl(Configuration configuration) throws Exception {
        this.configuration = configuration;
        createDatabaseIfNotExists();
    }

    private Connection getConnection() throws SQLException, IOException {
        String databaseFile = "jdbc:sqlite:" + configuration.getLocalAppStateDirectory().toAbsolutePath() + "/" + DB_FILE_NAME;
        return DriverManager.getConnection(databaseFile);
    }

    String CREATE_DATABASE = """
                        CREATE TABLE IF NOT EXISTS Notifications (
                          type       TINYINT         NOT NULL CHECK (type BETWEEN 0 AND 2),
                          
                          localDirectory           VARCHAR(255)    NOT NULL,
                          
                          localFile           VARCHAR(255),
                          remoteFile          VARCHAR(255),
        
                          message        VARCHAR(255)    NOT NULL,
        
                          timestamp     TIMESTAMP       NOT NULL,
                          
                          PRIMARY KEY (type, localDirectory, localFile, remoteFile)
                      );
        """;
    String CREATE_TIMESTAMP_INDEX = "CREATE INDEX IF NOT EXISTS timestamp ON Notifications(timestamp);";
    String CREATE_LOCAL_DIRECTORY_INDEX = "CREATE INDEX IF NOT EXISTS localDirectory ON Notifications(localDirectory);";

    public void createDatabaseIfNotExists() throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement createTableStatement = connection.prepareStatement(CREATE_DATABASE)) {
            createTableStatement.executeUpdate();
        }
        try (Connection connection = getConnection();
             PreparedStatement createTimestampIndexStatement = connection.prepareStatement(CREATE_TIMESTAMP_INDEX);
             PreparedStatement createLocalDirectoryIndexStatement = connection.prepareStatement(CREATE_LOCAL_DIRECTORY_INDEX)) {
            createTimestampIndexStatement.executeUpdate();
            createLocalDirectoryIndexStatement.executeUpdate();
        }
    }

    String INSERT_OR_UPDATE = "INSERT OR REPLACE INTO Notifications (type, localDirectory, localFile, remoteFile, message, timestamp) VALUES (?,?,?,?,?,?);";

    public int insertOrUpdate(@NonNull Notification notification) throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_OR_UPDATE)) {

            int typeAsInt = getTypeAsInt(notification.getType());
            statement.setObject(1, typeAsInt);
            statement.setObject(2, notification.getLocalDirectory());
            statement.setObject(3, notification.getLocalFile());
            statement.setObject(4, notification.getRemoteFile());
            statement.setObject(5, notification.getMessage());
            statement.setObject(6, notification.getTimestamp());

            return statement.executeUpdate();
        }
    }

    String SELECT_LAST = "SELECT * FROM Notifications ORDER BY timestamp DESC LIMIT ?;";

    public List<Notification> selectLast(int limit) throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LAST)) {

            statement.setObject(1, limit);

            ResultSet resultSet = statement.executeQuery();

            List<Notification> notifications = new ArrayList<>();
            while (resultSet.next()) {
                notifications.add(Notification.builder()
                        .type(getTypeFromInt(((Number) resultSet.getObject("type")).intValue()))
                        .localDirectory((String) resultSet.getObject("localDirectory"))
                        .localFile((String) resultSet.getObject("localFile"))
                        .remoteFile((String) resultSet.getObject("remoteFile"))
                        .message((String) resultSet.getObject("message"))
                        .timestamp((Long) resultSet.getObject("timestamp"))
                        .build());
            }
            return notifications;
        }
    }

    String SELECT_BY_LOCAL_DIR_AND_TYPE = "SELECT * FROM Notifications WHERE localDirectory = ? AND type = ? LIMIT ?;";

    public List<Notification> selectByLocalDirectoryAndType(@NonNull String localDirectory, @NonNull Notification.Type type, int limit) throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_LOCAL_DIR_AND_TYPE)) {

            statement.setObject(1, localDirectory);
            statement.setObject(2, getTypeAsInt(type));
            statement.setObject(3, limit);

            ResultSet resultSet = statement.executeQuery();

            List<Notification> notifications = new ArrayList<>();
            while (resultSet.next()) {
                notifications.add(Notification.builder()
                        .type(getTypeFromInt(((Number) resultSet.getObject("type")).intValue()))
                        .localDirectory((String) resultSet.getObject("localDirectory"))
                        .localFile((String) resultSet.getObject("localFile"))
                        .remoteFile((String) resultSet.getObject("remoteFile"))
                        .message((String) resultSet.getObject("message"))
                        .timestamp((Long) resultSet.getObject("timestamp"))
                        .build());
            }
            return notifications;
        }
    }

    String SELECT_SPECIFIC_CONFLICT_ENTRY = "SELECT * FROM Notifications WHERE type = ? AND localDirectory = ? AND localFile IS ? AND remoteFile IS ?;";

    public Notification selectByPrimaryKey(@NonNull Notification.Type type, @NonNull String localDirectory, String localFile, String remoteFile) throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_SPECIFIC_CONFLICT_ENTRY)) {

            statement.setObject(1, getTypeAsInt(type));
            statement.setObject(2, localDirectory);
            statement.setObject(3, localFile);
            statement.setObject(4, remoteFile);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return Notification.builder()
                        .type(type)
                        .localDirectory(localDirectory)
                        .localFile(localFile)
                        .remoteFile(remoteFile)
                        .message((String) resultSet.getObject("message"))
                        .timestamp((Long) resultSet.getObject("timestamp"))
                        .build();
            } else {
                return null;
            }
        }
    }

    String DELETE = "DELETE FROM Notifications WHERE type = ? AND localDirectory = ? AND localFile IS ? AND remoteFile IS ?;";

    public int removeByPrimaryKey(@NonNull Notification.Type type, @NonNull String localDirectory, String localFile, String remoteFile) throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {

            statement.setObject(1, getTypeAsInt(type));
            statement.setObject(2, localDirectory);
            statement.setObject(3, localFile);
            statement.setObject(4, remoteFile);
            return statement.executeUpdate();
        }
    }

    String DELETE_BY_LOCAL_DIR = "DELETE FROM Notifications WHERE localDirectory = ?;";

    @Override
    public int removeByLocalDirectory(@NonNull String localDirectory) throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_BY_LOCAL_DIR)) {

            statement.setObject(1, localDirectory);
            return statement.executeUpdate();
        }
    }

    String DELETE_OLD_ENTRIES_BY_TYPE = "DELETE FROM Notifications WHERE localDirectory = ? AND type = ? AND timestamp NOT IN (SELECT timestamp FROM Notifications WHERE localDirectory = ? AND type = ? ORDER BY timestamp DESC LIMIT ?);";

    @Override
    public int removeOldEntriesByLocalDirectoryAndType(@NonNull String localDirectory, @NonNull Notification.Type type, int offset) throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_OLD_ENTRIES_BY_TYPE)) {

            statement.setObject(1, localDirectory);
            statement.setObject(2, getTypeAsInt(type));
            statement.setObject(3, localDirectory);
            statement.setObject(4, getTypeAsInt(type));
            statement.setObject(5, offset);

            return statement.executeUpdate();
        }
    }

    String DELETE_ALL = "DELETE FROM Notifications;";

    @Override
    public int clearAll() throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_ALL)) {
            return statement.executeUpdate();
        }
    }

    private static int getTypeAsInt(@NonNull Notification.Type type) {
        return switch (type) {
            case Conflict -> 0;
            case JobStopped -> 1;
            case JobException -> 2;
        };
    }

    private static @NonNull Notification.Type getTypeFromInt(int typeAsInt) {
        return switch (typeAsInt) {
            case 0 -> Notification.Type.Conflict;
            case 1 -> Notification.Type.JobStopped;
            case 2 -> Notification.Type.JobException;
            default -> throw new IllegalArgumentException(String.format("Illegal integer value %s for Notification.Type", typeAsInt));
        };
    }
}
