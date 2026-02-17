package ch.openbis.drive.protobuf;

import ch.openbis.drive.DriveAPIServerImpl;
import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.model.*;
import ch.openbis.drive.protobuf.converters.ProtobufConversionUtil;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;

import java.util.Arrays;
import java.util.List;

public class DriveAPIGrpcImpl extends DriveAPIServiceGrpc.DriveAPIServiceImplBase {
    final DriveAPIServerImpl driveAPIServer;
    final Configuration configuration;

    public DriveAPIGrpcImpl(DriveAPIServerImpl driveAPIServer, Configuration configuration) {
        this.driveAPIServer = driveAPIServer;
        this.configuration = configuration;
    }

    @Override
    public void setSettings(DriveApiService.Settings request, StreamObserver<DriveApiService.Empty> responseObserver) {
        try {
            checkClientSecret(new DriveApiServiceMessageWithClientSecret(request));

            driveAPIServer.setSettings(ProtobufConversionUtil.fromProtobufSettings(request));
            responseObserver.onNext(DriveApiService.Empty.newBuilder().build());
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getSettings(DriveApiService.Empty request, StreamObserver<DriveApiService.Settings> responseObserver) {
        try {
            checkClientSecret(new DriveApiServiceMessageWithClientSecret(request));

            Settings settings = driveAPIServer.getSettings();
            responseObserver.onNext(ProtobufConversionUtil.toProtobufSettings(settings, null));
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getSyncJobs(DriveApiService.Empty request, StreamObserver<DriveApiService.SyncJobs> responseObserver) {
        try {
            checkClientSecret(new DriveApiServiceMessageWithClientSecret(request));

            List<SyncJob> syncJobs = driveAPIServer.getSyncJobs();
            responseObserver.onNext(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, null));
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void addSyncJobs(DriveApiService.SyncJobs request, StreamObserver<DriveApiService.Empty> responseObserver) {
        try {
            checkClientSecret(new DriveApiServiceMessageWithClientSecret(request));

            driveAPIServer.addSyncJobs(ProtobufConversionUtil.fromProtobufSyncJobs(request));
            responseObserver.onNext(DriveApiService.Empty.newBuilder().build());
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void removeSyncJobs(DriveApiService.SyncJobs request, StreamObserver<DriveApiService.Empty> responseObserver) {
        try {
            checkClientSecret(new DriveApiServiceMessageWithClientSecret(request));

            driveAPIServer.removeSyncJobs(ProtobufConversionUtil.fromProtobufSyncJobs(request));
            responseObserver.onNext(DriveApiService.Empty.newBuilder().build());
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void startSyncJobs(DriveApiService.SyncJobs request, StreamObserver<DriveApiService.Empty> responseObserver) {
        try {
            checkClientSecret(new DriveApiServiceMessageWithClientSecret(request));

            driveAPIServer.startSyncJobs(ProtobufConversionUtil.fromProtobufSyncJobs(request));
            responseObserver.onNext(DriveApiService.Empty.newBuilder().build());
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void stopSyncJobs(DriveApiService.SyncJobs request, StreamObserver<DriveApiService.Empty> responseObserver) {
        try {
            checkClientSecret(new DriveApiServiceMessageWithClientSecret(request));

            driveAPIServer.stopSyncJobs(ProtobufConversionUtil.fromProtobufSyncJobs(request));
            responseObserver.onNext(DriveApiService.Empty.newBuilder().build());
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getEvents(DriveApiService.Limit request, StreamObserver<DriveApiService.Events> responseObserver) {
        try {
            checkClientSecret(new DriveApiServiceMessageWithClientSecret(request));

            List<? extends Event> events = driveAPIServer.getEvents(request.getLimit());
            responseObserver.onNext(ProtobufConversionUtil.toProtobufEvents(events));
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getNotifications(DriveApiService.Limit request, StreamObserver<DriveApiService.Notifications> responseObserver) {
        try {
            checkClientSecret(new DriveApiServiceMessageWithClientSecret(request));

            List<Notification> notifications = driveAPIServer.getNotifications(request.getLimit());
            responseObserver.onNext(ProtobufConversionUtil.toProtobufNotifications(notifications));
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getSyncJobsLive(DriveApiService.Empty request, StreamObserver<DriveApiService.SyncJobsLive> responseObserver) {
        try {
            checkClientSecret(new DriveApiServiceMessageWithClientSecret(request));

            List<SyncJobLive> syncJobsLive = driveAPIServer.getSyncJobsLive();
            responseObserver.onNext(ProtobufConversionUtil.toProtobufSyncJobsLive(syncJobsLive));
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }

    static class DriveApiServiceMessageWithClientSecret {
        private final byte[] clientSecret;

        public DriveApiServiceMessageWithClientSecret(DriveApiService.Empty emptyMessage) {
            this.clientSecret = emptyMessage.hasClientSecret() ? emptyMessage.getClientSecret().toByteArray() : null;
        }

        public DriveApiServiceMessageWithClientSecret(DriveApiService.Limit limitMessage) {
            this.clientSecret = limitMessage.hasClientSecret() ? limitMessage.getClientSecret().toByteArray() : null;
        }

        public DriveApiServiceMessageWithClientSecret(DriveApiService.Settings settingsMessage) {
            this.clientSecret = settingsMessage.hasClientSecret() ? settingsMessage.getClientSecret().toByteArray() : null;
        }

        public DriveApiServiceMessageWithClientSecret(DriveApiService.SyncJobs syncJobsMessage) {
            this.clientSecret = syncJobsMessage.hasClientSecret() ? syncJobsMessage.getClientSecret().toByteArray() : null;
        }

        @SuppressWarnings("lombok")
        public byte[] getClientSecret() {
            return clientSecret;
        }
    }

    void checkClientSecret(@NonNull DriveApiServiceMessageWithClientSecret message) throws Exception {
        byte[] clientSecret = message.getClientSecret();

        if ( clientSecret == null ) {
            throw new DriveAPIUnauthorizedException();
        } else {
            if ( this.configuration.getClientSecret() == null ) {
                this.configuration.readOpenbisDriveClientSecret();
            }

            byte[] expectedClientSecret = this.configuration.getClientSecret();
            if ( !Arrays.equals(expectedClientSecret, clientSecret) ) {
                throw new DriveAPIUnauthorizedException();
            }
        }
    }

    public static class DriveAPIUnauthorizedException extends Exception {}
}
