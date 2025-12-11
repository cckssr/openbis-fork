package ch.openbis.drive.protobuf.converters;

import ch.openbis.drive.model.Event;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.protobuf.DriveApiService;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ProtobufConversionUtil {

    public static @NonNull Settings fromProtobufSettings(@NonNull DriveApiService.Settings settingsDto) {
        Settings settings = new Settings();
        settings.setStartAtLogin(settingsDto.getStartAtLogin());
        settings.setLanguage(settingsDto.getLanguage());
        settings.setSyncInterval(settingsDto.getSyncIntervalSeconds());
        settings.setJobs(fromProtobufSyncJobs(settingsDto.getJobs()));

        return settings;
    }


    public static DriveApiService.Settings toProtobufSettings(@NonNull Settings settings) {
        return DriveApiService.Settings.newBuilder()
            .setStartAtLogin(settings.isStartAtLogin())
            .setLanguage(settings.getLanguage())
            .setSyncIntervalSeconds(settings.getSyncInterval())
            .setJobs(toProtobufSyncJobs(settings.getJobs())).build();
    }

    public static @NonNull ArrayList<@NonNull SyncJob> fromProtobufSyncJobs(@NonNull DriveApiService.SyncJobs syncJobsDto) {
        ArrayList<SyncJob> syncJobs = new ArrayList<>();

        for(DriveApiService.SyncJob syncJobDto: syncJobsDto.getSyncJobsList()) {
            SyncJob syncJob = new SyncJob();
            syncJob.setTitle(syncJobDto.getTitle());
            syncJob.setType(fromProtobuftoSyncJobTypeEnum(syncJobDto.getType()));
            syncJob.setEnabled(syncJobDto.getEnabled());
            syncJob.setOpenBisUrl(syncJobDto.getOpenBisUrl());
            syncJob.setEntityPermId(syncJobDto.getEntityPermId());
            syncJob.setOpenBisPersonalAccessToken(syncJobDto.getOpenBisPersonalAccessToken());
            syncJob.setRemoteDirectoryRoot(syncJobDto.getRemoteDirectoryRoot());
            syncJob.setLocalDirectoryRoot(syncJobDto.getLocalDirectoryRoot());
            syncJob.setSkipHiddenFiles(syncJobDto.getSkipHiddenFiles());
            syncJob.setHiddenPathPatterns(new ArrayList<>(syncJobDto.getHiddenPathPatterns().getHiddenPathPatternsList().stream().toList()));
            syncJobs.add(syncJob);
        }

        return syncJobs;
    }

    public static DriveApiService.SyncJobs toProtobufSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        DriveApiService.SyncJobs.Builder builder = DriveApiService.SyncJobs.newBuilder();

        for(SyncJob syncJob : syncJobs) {
            DriveApiService.SyncJob.Builder syncJobBuilder = DriveApiService.SyncJob.newBuilder();
            syncJobBuilder.setTitle(syncJob.getTitle());
            syncJobBuilder.setEnabled(syncJob.isEnabled());
            syncJobBuilder.setType(toProtobufSyncJobTypeEnum(syncJob.getType()));
            syncJobBuilder.setLocalDirectoryRoot(syncJob.getLocalDirectoryRoot());
            syncJobBuilder.setOpenBisUrl(syncJob.getOpenBisUrl());
            syncJobBuilder.setEntityPermId(syncJob.getEntityPermId());
            syncJobBuilder.setOpenBisPersonalAccessToken(syncJob.getOpenBisPersonalAccessToken());
            syncJobBuilder.setRemoteDirectoryRoot(syncJob.getRemoteDirectoryRoot());
            syncJobBuilder.setSkipHiddenFiles(syncJob.isSkipHiddenFiles());
            syncJobBuilder.setHiddenPathPatterns(DriveApiService.HiddenPathPatterns.newBuilder().addAllHiddenPathPatterns(syncJob.getHiddenPathPatterns()).build());
            builder.addSyncJobs(syncJobBuilder.build());
        }

        return builder.build();
    }

    public static @NonNull List<@NonNull Notification> fromProtobufNotifications(@NonNull DriveApiService.Notifications notificationsDto) {
        List<Notification> notifications = new ArrayList<>();

        for(DriveApiService.Notification notificationDto : notificationsDto.getNotificationsList()) {
            Notification notification = Notification.builder()
                    .type(fromProtobufNotificationTypeEnum(notificationDto.getType()))
                    .localDirectory(notificationDto.getLocalDirectory())
                    .localFile(notificationDto.getLocalFile())
                    .remoteFile(notificationDto.getRemoteFile())
                    .timestamp(notificationDto.getTimestamp())
                    .message(notificationDto.getMessage())
                    .build();
            notifications.add(notification);
        }

        return notifications;
    }

    public static DriveApiService.Notifications toProtobufNotifications(@NonNull List<@NonNull Notification> notifications) {
        DriveApiService.Notifications.Builder builder = DriveApiService.Notifications.newBuilder();

        for(Notification notification : notifications) {
            DriveApiService.Notification.Builder notificationBuilder = DriveApiService.Notification.newBuilder();
            notificationBuilder.setType(toProtobufNotificationTypeEnum(notification.getType()));
            notificationBuilder.setLocalDirectory(notification.getLocalDirectory());
            notificationBuilder.setMessage(notification.getMessage());
            notificationBuilder.setTimestamp(notification.getTimestamp());
            if(notification.getLocalFile() != null) {
                notificationBuilder.setLocalFile(notification.getLocalFile());
            }
            if(notification.getRemoteFile() != null) {
                notificationBuilder.setRemoteFile(notification.getRemoteFile());
            }
            builder.addNotifications(notificationBuilder.build());
        }

        return builder.build();
    }

    public static @NonNull List<@NonNull ? extends Event> fromProtobufEvents(@NonNull DriveApiService.Events eventsDto) {
        List<EventClientDto> eventClientDtos = new ArrayList<>();

        for(DriveApiService.Event event : eventsDto.getEventsList()) {
            eventClientDtos.add(new EventClientDto(event));
        }

        return eventClientDtos;
    }

    public static DriveApiService.Events toProtobufEvents(@NonNull List<@NonNull ? extends Event> events) {
        DriveApiService.Events.Builder builder = DriveApiService.Events.newBuilder();

        for(Event event : events) {
            DriveApiService.Event eventDto = DriveApiService.Event.newBuilder()
                .setSyncDirection(toProtobufSyncDirectionEnum(event.getSyncDirection()))
                .setLocalDirectoryRoot(event.getLocalDirectoryRoot())
                .setLocalFile(event.getLocalFile())
                .setRemoteFile(event.getRemoteFile())
                .setDirectory(event.isDirectory())
                .setSourceDeleted(event.isSourceDeleted())
                .setTimestamp(event.getTimestamp()).build();
            builder.addEvents(eventDto);
        }

        return builder.build();
    }

    public static Event.SyncDirection fromProtobufSyncDirectionEnum(@NonNull DriveApiService.Event.SyncDirection syncDirection) {
        return switch (syncDirection) {
            case UP -> Event.SyncDirection.UP;
            case DOWN -> Event.SyncDirection.DOWN;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown sync-direction");
        };
    }

    public static DriveApiService.Event.SyncDirection toProtobufSyncDirectionEnum(@NonNull Event.SyncDirection syncDirection) {
        return switch (syncDirection) {
            case UP -> DriveApiService.Event.SyncDirection.UP;
            case DOWN -> DriveApiService.Event.SyncDirection.DOWN;
        };
    }

    public static Notification.Type fromProtobufNotificationTypeEnum(@NonNull DriveApiService.Notification.Type type) {
        return switch (type) {
            case CONFLICT -> Notification.Type.Conflict;
            case JOB_STOPPED -> Notification.Type.JobStopped;
            case JOB_EXCEPTION -> Notification.Type.JobException;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown notification-type");
        };
    }

    public static DriveApiService.Notification.Type toProtobufNotificationTypeEnum(@NonNull Notification.Type type) {
        return switch (type) {
            case Conflict -> DriveApiService.Notification.Type.CONFLICT;
            case JobStopped -> DriveApiService.Notification.Type.JOB_STOPPED;
            case JobException -> DriveApiService.Notification.Type.JOB_EXCEPTION;
        };
    }

    public static SyncJob.Type fromProtobuftoSyncJobTypeEnum(@NonNull DriveApiService.SyncJob.Type type) {
        return switch (type) {
            case UPLOAD -> SyncJob.Type.Upload;
            case DOWNLOAD -> SyncJob.Type.Download;
            case BIDIRECTIONAL -> SyncJob.Type.Bidirectional;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown sync-job type");
        };
    }

    public static DriveApiService.SyncJob.Type toProtobufSyncJobTypeEnum(@NonNull SyncJob.Type type) {
        return switch (type) {
            case Upload -> DriveApiService.SyncJob.Type.UPLOAD;
            case Download -> DriveApiService.SyncJob.Type.DOWNLOAD;
            case Bidirectional -> DriveApiService.SyncJob.Type.BIDIRECTIONAL;
        };
    }
}
