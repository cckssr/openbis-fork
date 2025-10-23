package ch.openbis.drive.db;

import ch.openbis.drive.model.Notification;
import lombok.NonNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface NotificationDAO {
    void createDatabaseIfNotExists() throws SQLException, IOException;
    int insertOrUpdate(@NonNull Notification notification) throws SQLException, IOException;

    List<Notification> selectLast(int limit) throws SQLException, IOException;
    List<Notification> selectByLocalDirectoryAndType(@NonNull String localDirectory, @NonNull Notification.Type type, int limit) throws SQLException, IOException;

    Notification selectByPrimaryKey(@NonNull Notification.Type type, @NonNull String localDirectory, String localFile, String remoteFile) throws SQLException, IOException;
    int removeByPrimaryKey(@NonNull Notification.Type type, @NonNull String localDirectory, String localFile, String remoteFile) throws SQLException, IOException;

    int removeByLocalDirectory(@NonNull String localDirectory) throws SQLException, IOException;
    int removeOldEntriesByLocalDirectoryAndType(@NonNull String localDirectory, @NonNull Notification.Type type, int offset) throws SQLException, IOException;
    int clearAll() throws SQLException, IOException;
}
