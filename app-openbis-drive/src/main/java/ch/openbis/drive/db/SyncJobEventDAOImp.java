package ch.openbis.drive.db;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.model.SyncJobEvent;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class SyncJobEventDAOImp implements SyncJobEventDAO {
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static final String DB_FILE_NAME = "openbis-drive-events.db";
    private final Configuration configuration;

    public SyncJobEventDAOImp(Configuration configuration) throws Exception {
        this.configuration = configuration;
        createDatabaseIfNotExists();
    }

    private Connection getConnection() throws SQLException, IOException {
        String databaseFile = "jdbc:sqlite:" + configuration.getLocalAppDirectory().toAbsolutePath() + "/" + DB_FILE_NAME;
        return DriverManager.getConnection(databaseFile);
    }

    String CREATE_DATABASE = """
                        CREATE TABLE IF NOT EXISTS SyncJobEvents (
                          syncDirection       TINYINT         NOT NULL CHECK (syncDirection BETWEEN 0 AND 1),
                          localFile           VARCHAR(255)    NOT NULL,
                          remoteFile          VARCHAR(255)    NOT NULL,
        
                          entityPermId        VARCHAR(255)    NOT NULL,
                          localDirectoryRoot  VARCHAR(255)    NOT NULL,
        
                          sourceTimestamp     TIMESTAMP       NOT NULL,
                          destinationTimestamp TIMESTAMP,
        
                          directory            TINYINT         NOT NULL CHECK (directory BETWEEN 0 AND 1),
                          sourceDeleted        TINYINT         NOT NULL CHECK (sourceDeleted BETWEEN 0 AND 1),
                          
                          timestamp     TIMESTAMP       NOT NULL,
                          
                          PRIMARY KEY (syncDirection, localFile, remoteFile)
                      );
        """;
    String CREATE_JOB_ID_INDEX = "CREATE INDEX IF NOT EXISTS jobId ON SyncJobEvents(entityPermId, localDirectoryRoot);";
    String CREATE_TIMESTAMP_INDEX = "CREATE INDEX IF NOT EXISTS timestamp ON SyncJobEvents(timestamp);";

    @Override
    public void createDatabaseIfNotExists() throws SQLException, IOException {
        try (Connection connection = getConnection();
                PreparedStatement createTableStatement = connection.prepareStatement(CREATE_DATABASE)) {
            createTableStatement.executeUpdate();
        }
        try (Connection connection = getConnection();
                PreparedStatement createJobIdIndexStatement = connection.prepareStatement(CREATE_JOB_ID_INDEX);
                PreparedStatement createTimestampIndexStatement = connection.prepareStatement(CREATE_TIMESTAMP_INDEX)) {
            createJobIdIndexStatement.executeUpdate();
            createTimestampIndexStatement.executeUpdate();
        }
    }

    String INSERT_OR_UPDATE = "INSERT OR REPLACE INTO SyncJobEvents (syncDirection, localFile, remoteFile, entityPermId, localDirectoryRoot, sourceTimestamp, destinationTimestamp, directory, sourceDeleted, timestamp) VALUES (?,?,?,?,?,?,?,?,?,?);";

    @Override
    public int insertOrUpdate(SyncJobEvent syncJobEvent) throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_OR_UPDATE)) {

            int syncDirectionAsInt = getSyncDirectionAsInt(syncJobEvent.getSyncDirection());
            statement.setObject(1, syncDirectionAsInt);
            statement.setObject(2, syncJobEvent.getLocalFile());
            statement.setObject(3, syncJobEvent.getRemoteFile());
            statement.setObject(4, syncJobEvent.getEntityPermId());
            statement.setObject(5, syncJobEvent.getLocalDirectoryRoot());
            statement.setObject(6, syncJobEvent.getSourceTimestamp());
            statement.setObject(7, syncJobEvent.getDestinationTimestamp());
            statement.setObject(8, syncJobEvent.isDirectory());
            statement.setObject(9, syncJobEvent.isSourceDeleted());
            statement.setObject(10, syncJobEvent.getTimestamp());

            return statement.executeUpdate();
        }
    }

    String SELECT = "SELECT * FROM SyncJobEvents WHERE syncDirection = ? AND localFile = ? AND remoteFile = ? ;";

    @Override
    public SyncJobEvent selectByPrimaryKey(@NonNull SyncJobEvent.SyncDirection syncDirection, @NonNull String localFile, @NonNull String remoteFile) throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT)) {

            int syncDirectionAsInt = getSyncDirectionAsInt(syncDirection);
            statement.setObject(1, syncDirectionAsInt);
            statement.setObject(2, localFile);
            statement.setObject(3, remoteFile);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return SyncJobEvent.builder()
                        .syncDirection(syncDirection)
                        .localFile(localFile)
                        .remoteFile(remoteFile)
                        .localDirectoryRoot(resultSet.getObject("localDirectoryRoot", String.class))
                        .entityPermId(resultSet.getObject("entityPermId", String.class))
                        .sourceTimestamp(resultSet.getObject("sourceTimestamp", Long.class))
                        .destinationTimestamp((Long) resultSet.getObject("destinationTimestamp"))
                        .directory(resultSet.getObject("directory", Boolean.class))
                        .sourceDeleted(resultSet.getObject("sourceDeleted", Boolean.class))
                        .timestamp(resultSet.getObject("timestamp", Long.class))
                        .build();
            } else {
                return null;
            }
        }
    }

    String SELECT_MOST_RECENT = "SELECT * FROM SyncJobEvents WHERE destinationTimestamp IS NOT NULL OR sourceDeleted = 1 ORDER BY timestamp DESC LIMIT ? ;";

    @Override
    public List<SyncJobEvent> selectMostRecent(int limit) throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_MOST_RECENT)) {

            statement.setObject(1, limit);
            ResultSet resultSet = statement.executeQuery();
            List<SyncJobEvent> events = new ArrayList<>();
            while (resultSet.next()) {
                events.add(SyncJobEvent.builder()
                        .syncDirection(getSyncDirectionFromInt(((Number)resultSet.getObject("syncDirection")).intValue()))
                        .localFile(resultSet.getObject("localFile", String.class))
                        .remoteFile(resultSet.getObject("remoteFile", String.class))
                        .localDirectoryRoot(resultSet.getObject("localDirectoryRoot", String.class))
                        .entityPermId(resultSet.getObject("entityPermId", String.class))
                        .sourceTimestamp(resultSet.getObject("sourceTimestamp", Long.class))
                        .destinationTimestamp((Long) resultSet.getObject("destinationTimestamp"))
                        .directory(resultSet.getObject("directory", Boolean.class))
                        .sourceDeleted(resultSet.getObject("sourceDeleted", Boolean.class))
                        .timestamp(resultSet.getObject("timestamp", Long.class))
                        .build());
            }
            return events;
        }
    }

    String SELECT_BY_LOCAL_DIRECTORY = "SELECT * FROM SyncJobEvents WHERE localDirectoryRoot = ? ORDER BY timestamp DESC;";
    String DELETE_BY_PRIMARY_KEY = "DELETE FROM SyncJobEvents WHERE syncDirection = ? AND localFile = ? AND remoteFile = ? ;";

    @Override
    public void pruneOldDeletedByLocalDirectoryRoot(@NonNull String localDirectoryRoot, int offset) throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_LOCAL_DIRECTORY)) {

            statement.setObject(1, localDirectoryRoot);
            ResultSet resultSet = statement.executeQuery();

            int index = 0;
            while (resultSet.next()) {
                if (index >= offset) {
                    SyncJobEvent syncJobEvent = SyncJobEvent.builder()
                            .syncDirection(getSyncDirectionFromInt(((Number)resultSet.getObject("syncDirection")).intValue()))
                            .localFile(resultSet.getObject("localFile", String.class))
                            .remoteFile(resultSet.getObject("remoteFile", String.class))
                            .localDirectoryRoot(resultSet.getObject("localDirectoryRoot", String.class))
                            .entityPermId(resultSet.getObject("entityPermId", String.class))
                            .sourceTimestamp(resultSet.getObject("sourceTimestamp", Long.class))
                            .destinationTimestamp((Long) resultSet.getObject("destinationTimestamp"))
                            .directory(resultSet.getObject("directory", Boolean.class))
                            .sourceDeleted(resultSet.getObject("sourceDeleted", Boolean.class))
                            .timestamp(resultSet.getObject("timestamp", Long.class))
                            .build();

                    try {
                        if (!Files.exists(Path.of(syncJobEvent.getLocalFile()))) {
                            try (PreparedStatement removeStatement = connection.prepareStatement(DELETE_BY_PRIMARY_KEY)) {
                                int syncDirectionAsInt = getSyncDirectionAsInt(syncJobEvent.getSyncDirection());
                                removeStatement.setObject(1, syncDirectionAsInt);
                                removeStatement.setObject(2, syncJobEvent.getLocalFile());
                                removeStatement.setObject(3, syncJobEvent.getRemoteFile());
                                removeStatement.executeUpdate();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                index++;
            }
        }
    }

    String DELETE = "DELETE FROM SyncJobEvents WHERE localDirectoryRoot = ? ;";

    @Override
    public int removeByLocalDirectoryRoot(@NonNull String localDirectoryRoot) throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {

            statement.setObject(1, localDirectoryRoot);
            return statement.executeUpdate();
        }
    }

    String DELETE_ALL = "DELETE FROM SyncJobEvents ;";

    @Override
    public int clearAll() throws SQLException, IOException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_ALL)) {

            return statement.executeUpdate();
        }
    }


    private static int getSyncDirectionAsInt(@NonNull SyncJobEvent.SyncDirection syncDirection) {
        return switch (syncDirection) {
            case UP -> 0;
            case DOWN -> 1;
        };
    }

    private static @NonNull SyncJobEvent.SyncDirection getSyncDirectionFromInt(int typeAsInt) {
        return switch (typeAsInt) {
            case 0 -> SyncJobEvent.SyncDirection.UP;
            case 1 -> SyncJobEvent.SyncDirection.DOWN;
            default -> throw new IllegalArgumentException(String.format("Illegal integer value %s for SyncJobEvent.SyncDirection", typeAsInt));
        };
    }
}
