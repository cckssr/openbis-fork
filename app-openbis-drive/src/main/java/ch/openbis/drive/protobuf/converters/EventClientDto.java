package ch.openbis.drive.protobuf.converters;

import ch.openbis.drive.model.Event;
import ch.openbis.drive.protobuf.DriveApiService;
import lombok.NonNull;

public class EventClientDto implements Event {
    private final DriveApiService.Event event;

    public EventClientDto(DriveApiService.Event event) {
        this.event = event;
    }

    @Override
    public @NonNull SyncDirection getSyncDirection() {
        return ProtobufConversionUtil.fromProtobufSyncDirectionEnum(this.event.getSyncDirection());
    }

    @Override
    public @NonNull String getLocalFile() {
        return this.event.getLocalFile();
    }

    @Override
    public @NonNull String getRemoteFile() {
        return this.event.getRemoteFile();
    }

    @Override
    public boolean isDirectory() {
        return this.event.getDirectory();
    }

    @Override
    public boolean isSourceDeleted() {
        return this.event.getSourceDeleted();
    }

    @Override
    public @NonNull Long getTimestamp() {
        return this.event.getTimestamp();
    }

    @Override
    public @NonNull String getLocalDirectoryRoot() {
        return this.event.getLocalDirectoryRoot();
    }
}
