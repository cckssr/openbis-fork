package ch.openbis.drive.protobuf;

import ch.openbis.drive.DriveAPIServerImpl;
import ch.openbis.drive.model.Event;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.protobuf.converters.ProtobufConversionUtil;
import io.grpc.stub.StreamObserver;

import java.util.List;

public class DriveAPIGrpcImpl extends DriveAPIServiceGrpc.DriveAPIServiceImplBase {
    final DriveAPIServerImpl driveAPIServer;

    public DriveAPIGrpcImpl(DriveAPIServerImpl driveAPIServer) {
        this.driveAPIServer = driveAPIServer;
    }

    @Override
    public void setSettings(DriveApiService.Settings request, StreamObserver<DriveApiService.Empty> responseObserver) {
        try {
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
            Settings settings = driveAPIServer.getSettings();
            responseObserver.onNext(ProtobufConversionUtil.toProtobufSettings(settings));
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getSyncJobs(DriveApiService.Empty request, StreamObserver<DriveApiService.SyncJobs> responseObserver) {
        try {
            List<SyncJob> syncJobs = driveAPIServer.getSyncJobs();
            responseObserver.onNext(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs));
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void addSyncJobs(DriveApiService.SyncJobs request, StreamObserver<DriveApiService.Empty> responseObserver) {
        try {
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
            List<Notification> notifications = driveAPIServer.getNotifications(request.getLimit());
            responseObserver.onNext(ProtobufConversionUtil.toProtobufNotifications(notifications));
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }
}
