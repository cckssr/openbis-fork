package ch.openbis.drive.model;

import lombok.NonNull;

public interface Event {
    enum SyncDirection { UP, DOWN }

    @NonNull SyncDirection getSyncDirection();

    @NonNull String getLocalFile();

    @NonNull String getRemoteFile();

    boolean isDirectory();

    boolean isSourceDeleted();

    @NonNull Long getTimestamp();

    @NonNull String getLocalDirectoryRoot();
}
