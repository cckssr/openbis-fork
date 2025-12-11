package ch.openbis.drive.protobuf;

import ch.openbis.drive.DriveAPIServerImpl;
import ch.openbis.drive.model.Event;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.protobuf.converters.EventClientDto;
import ch.openbis.drive.protobuf.converters.ProtobufConversionUtil;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class DriveAPIGrpcImplTest {

    final DriveAPIServerImpl driveAPIServer = Mockito.mock(DriveAPIServerImpl.class);
    final DriveAPIGrpcImpl driveAPIGrpc = new DriveAPIGrpcImpl(driveAPIServer);

    @Test
    public void before() {
        Mockito.reset(driveAPIServer);
    }

    @Test
    public void setSettings() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        Settings settings = new Settings(true, "it", 63, new ArrayList<>(List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true)
        )));
        driveAPIGrpc.setSettings(ProtobufConversionUtil.toProtobufSettings(settings), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).setSettings(settings);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(DriveApiService.Empty.newBuilder().build());
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).setSettings(Mockito.any());

        driveAPIGrpc.setSettings(ProtobufConversionUtil.toProtobufSettings(settings), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).setSettings(settings);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getSettings() {
        StreamObserver<DriveApiService.Settings> streamObserver = Mockito.mock(StreamObserver.class);
        Settings settings = new Settings(true, "it", 63, new ArrayList<>(List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true)
        )));
        Mockito.doReturn(settings).when(driveAPIServer).getSettings();
        driveAPIGrpc.getSettings(DriveApiService.Empty.newBuilder().build(), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getSettings();
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(ProtobufConversionUtil.toProtobufSettings(settings));
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).getSettings();

        driveAPIGrpc.getSettings(DriveApiService.Empty.newBuilder().build(), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getSettings();
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getSyncJobs() {
        StreamObserver<DriveApiService.SyncJobs> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );
        Mockito.doReturn(syncJobs).when(driveAPIServer).getSyncJobs();
        driveAPIGrpc.getSyncJobs(DriveApiService.Empty.newBuilder().build(), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getSyncJobs();
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs));
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).getSyncJobs();

        driveAPIGrpc.getSyncJobs(DriveApiService.Empty.newBuilder().build(), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getSyncJobs();
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void addSyncJobs() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );
        driveAPIGrpc.addSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).addSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(DriveApiService.Empty.newBuilder().build());
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).addSyncJobs(Mockito.any());

        driveAPIGrpc.addSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).addSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void removeSyncJobs() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );
        driveAPIGrpc.removeSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).removeSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(DriveApiService.Empty.newBuilder().build());
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).removeSyncJobs(Mockito.any());

        driveAPIGrpc.removeSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).removeSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void startSyncJobs() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );
        driveAPIGrpc.startSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).startSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(DriveApiService.Empty.newBuilder().build());
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).startSyncJobs(Mockito.any());

        driveAPIGrpc.startSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).startSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void stopSyncJobs() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );
        driveAPIGrpc.stopSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).stopSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(DriveApiService.Empty.newBuilder().build());
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).stopSyncJobs(Mockito.any());

        driveAPIGrpc.stopSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).stopSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getEvents() {
        StreamObserver<DriveApiService.Events> streamObserver = Mockito.mock(StreamObserver.class);
        List<? extends Event> events = List.of(
                new EventClientDto(DriveApiService.Event.newBuilder().setSyncDirection(DriveApiService.Event.SyncDirection.UP)
                        .setLocalFile("/loc").setDirectory(true).setSourceDeleted(false).setRemoteFile("/rem").setTimestamp(432523L).build()),
                new EventClientDto(DriveApiService.Event.newBuilder().setSyncDirection(DriveApiService.Event.SyncDirection.DOWN)
                        .setLocalFile("/loc1").setDirectory(true).setSourceDeleted(true).setRemoteFile("/rem1").setTimestamp(75934L).build())
        );
        Mockito.doReturn(events).when(driveAPIServer).getEvents(2);
        driveAPIGrpc.getEvents(DriveApiService.Limit.newBuilder().setLimit(2).build(), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getEvents(2);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(ProtobufConversionUtil.toProtobufEvents(events));
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).getEvents(Mockito.anyInt());

        driveAPIGrpc.getEvents(DriveApiService.Limit.newBuilder().setLimit(2).build(), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getEvents(2);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getNotifications() {
        StreamObserver<DriveApiService.Notifications> streamObserver = Mockito.mock(StreamObserver.class);
        List<Notification> notifications = List.of(
                Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir").localFile("/loc4").remoteFile("/rem").message("mEsSaGe1").timestamp(324234L).build(),
                Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir2").localFile("/loc5").remoteFile("/rem").message("mEsSaGe2").timestamp(67543L).build(),
                Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir6").localFile("/loc7").remoteFile("/rem").message("mEsSaGe3").timestamp(8543L).build()
        );
        Mockito.doReturn(notifications).when(driveAPIServer).getNotifications(3);
        driveAPIGrpc.getNotifications(DriveApiService.Limit.newBuilder().setLimit(3).build(), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getNotifications(3);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(ProtobufConversionUtil.toProtobufNotifications(notifications));
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).getNotifications(Mockito.anyInt());

        driveAPIGrpc.getNotifications(DriveApiService.Limit.newBuilder().setLimit(3).build(), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getNotifications(3);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }
}