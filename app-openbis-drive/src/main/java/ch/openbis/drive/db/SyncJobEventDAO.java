package ch.openbis.drive.db;

import ch.openbis.drive.model.SyncJobEvent;
import lombok.NonNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface SyncJobEventDAO {
    void createDatabaseIfNotExists() throws SQLException, IOException;
    int insertOrUpdate(SyncJobEvent syncJobEvent) throws SQLException, IOException;
    SyncJobEvent selectByPrimaryKey(@NonNull SyncJobEvent.SyncDirection syncDirection, @NonNull String localFile, @NonNull String remoteFile) throws SQLException, IOException;
    List<SyncJobEvent> selectMostRecent(int limit) throws SQLException, IOException;
    void pruneOldDeletedByLocalDirectoryRoot(@NonNull String localDirectoryRoot, int offset) throws SQLException, IOException;
    int removeByLocalDirectoryRoot(@NonNull String localDirectoryRoot) throws SQLException, IOException;
    int clearAll() throws SQLException, IOException;
}
