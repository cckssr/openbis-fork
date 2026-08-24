package ch.openbis.drive.protobuf;

import ch.openbis.drive.DriveAPIServerImpl;
import ch.openbis.drive.DriveTestCase;
import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.model.*;
import ch.openbis.drive.protobuf.converters.EventClientDto;
import ch.openbis.drive.protobuf.converters.ProtobufConversionUtil;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class DriveAPIGrpcImplTest extends DriveTestCase {

    final DriveAPIServerImpl driveAPIServer = Mockito.mock(DriveAPIServerImpl.class);
    final DriveAPIGrpcImpl driveAPIGrpc = new DriveAPIGrpcImpl(driveAPIServer, new Configuration(Path.of("/fake-local-app-directory"), "seCReT".getBytes(StandardCharsets.UTF_8)));

    @Test
    public void before() {
        Mockito.reset(driveAPIServer);
    }

    @Test
    public void setSettings() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        Settings settings = new Settings(true, "it", 63, new ArrayList<>(List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true)
        )), new ArrayList<>(List.of("aaa", "bbb")), Settings.DEFAULT_EXPIRING_SESSION_WARNING_DAYS);
        driveAPIGrpc.setSettings(ProtobufConversionUtil.toProtobufSettings(settings, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).setSettings(settings);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(DriveApiService.Empty.newBuilder().build());
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).setSettings(Mockito.any());

        driveAPIGrpc.setSettings(ProtobufConversionUtil.toProtobufSettings(settings, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).setSettings(settings);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void setSettings_wrong_secret() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        Settings settings = new Settings(true, "it", 63, new ArrayList<>(List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true)
        )), new ArrayList<>(List.of("aaa", "bbb")), Settings.DEFAULT_EXPIRING_SESSION_WARNING_DAYS);

        driveAPIGrpc.setSettings(ProtobufConversionUtil.toProtobufSettings(settings, "wrong_seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).setSettings(settings);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void setSettings_missing_secret() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        Settings settings = new Settings(true, "it", 63, new ArrayList<>(List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true)
        )), new ArrayList<>(List.of("aaa", "bbb")), Settings.DEFAULT_EXPIRING_SESSION_WARNING_DAYS);

        driveAPIGrpc.setSettings(ProtobufConversionUtil.toProtobufSettings(settings, null), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).setSettings(settings);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getSettings() {
        StreamObserver<DriveApiService.Settings> streamObserver = Mockito.mock(StreamObserver.class);
        Settings settings = new Settings(true, "it", 63, new ArrayList<>(List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true)
        )), new ArrayList<>(List.of("aaa", "bbb")), Settings.DEFAULT_EXPIRING_SESSION_WARNING_DAYS);
        Mockito.doReturn(settings).when(driveAPIServer).getSettings();
        driveAPIGrpc.getSettings(ProtobufConversionUtil.toProtobufEmpty("seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getSettings();
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(ProtobufConversionUtil.toProtobufSettings(settings, null));
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).getSettings();

        driveAPIGrpc.getSettings(ProtobufConversionUtil.toProtobufEmpty("seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getSettings();
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getSettings_wrong_secret() {
        StreamObserver<DriveApiService.Settings> streamObserver = Mockito.mock(StreamObserver.class);
        Settings settings = new Settings(true, "it", 63, new ArrayList<>(List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true)
        )), new ArrayList<>(List.of("aaa", "bbb")), Settings.DEFAULT_EXPIRING_SESSION_WARNING_DAYS);
        Mockito.doReturn(settings).when(driveAPIServer).getSettings();

        driveAPIGrpc.getSettings(ProtobufConversionUtil.toProtobufEmpty("wrong_seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).getSettings();
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getSettings_missing_secret() {
        StreamObserver<DriveApiService.Settings> streamObserver = Mockito.mock(StreamObserver.class);
        Settings settings = new Settings(true, "it", 63, new ArrayList<>(List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true)
        )), new ArrayList<>(List.of("aaa", "bbb")), Settings.DEFAULT_EXPIRING_SESSION_WARNING_DAYS);
        Mockito.doReturn(settings).when(driveAPIServer).getSettings();

        driveAPIGrpc.getSettings(ProtobufConversionUtil.toProtobufEmpty(null), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).getSettings();
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
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
        driveAPIGrpc.getSyncJobs(ProtobufConversionUtil.toProtobufEmpty("seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getSyncJobs();
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, null));
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).getSyncJobs();

        driveAPIGrpc.getSyncJobs(ProtobufConversionUtil.toProtobufEmpty("seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getSyncJobs();
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getSyncJobs_wrong_secret() {
        StreamObserver<DriveApiService.SyncJobs> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );
        Mockito.doReturn(syncJobs).when(driveAPIServer).getSyncJobs();
        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).getSyncJobs();

        driveAPIGrpc.getSyncJobs(ProtobufConversionUtil.toProtobufEmpty("wrong_seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).getSyncJobs();
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getSyncJobs_missing_secret() {
        StreamObserver<DriveApiService.SyncJobs> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );
        Mockito.doReturn(syncJobs).when(driveAPIServer).getSyncJobs();
        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).getSyncJobs();

        driveAPIGrpc.getSyncJobs(ProtobufConversionUtil.toProtobufEmpty(null), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).getSyncJobs();
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void addSyncJobs() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );
        driveAPIGrpc.addSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).addSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(DriveApiService.Empty.newBuilder().build());
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).addSyncJobs(Mockito.any());

        driveAPIGrpc.addSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).addSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void addSyncJobs_wrong_secret() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );

        driveAPIGrpc.addSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, "wrong_seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).addSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void addSyncJobs_missing_secret() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );

        driveAPIGrpc.addSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, null), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).addSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void removeSyncJobs() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );
        driveAPIGrpc.removeSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).removeSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(DriveApiService.Empty.newBuilder().build());
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).removeSyncJobs(Mockito.any());

        driveAPIGrpc.removeSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).removeSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void removeSyncJobs_wrong_secret() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );

        driveAPIGrpc.removeSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, "wrong_seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).removeSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void removeSyncJobs_missing_secret() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );

        driveAPIGrpc.removeSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, null), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).removeSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void startSyncJobs() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );
        driveAPIGrpc.startSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).startSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(DriveApiService.Empty.newBuilder().build());
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).startSyncJobs(Mockito.any());

        driveAPIGrpc.startSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).startSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void startSyncJobs_wrong_secret() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );

        driveAPIGrpc.startSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, "wrong_seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).startSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void startSyncJobs_missing_secret() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );

        driveAPIGrpc.startSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, null), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).startSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void stopSyncJobs() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );
        driveAPIGrpc.stopSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).stopSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(DriveApiService.Empty.newBuilder().build());
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).stopSyncJobs(Mockito.any());

        driveAPIGrpc.stopSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).stopSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void stopSyncJobs_wrong_secret() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );

        driveAPIGrpc.stopSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, "wrong_seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).stopSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void stopSyncJobs_missing_secret() {
        StreamObserver<DriveApiService.Empty> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJob> syncJobs = List.of(
                new SyncJob(SyncJob.Type.Upload, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir", true),
                new SyncJob(SyncJob.Type.Bidirectional, "http://loc", "tkntkn", "1234-abcd", "title", "/remDIR", "/LOCdir2", true)
        );

        driveAPIGrpc.stopSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs, null), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).stopSyncJobs(syncJobs);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
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
        driveAPIGrpc.getEvents(ProtobufConversionUtil.toProtobufLimit(2, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getEvents(2);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(ProtobufConversionUtil.toProtobufEvents(events));
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).getEvents(Mockito.anyInt());

        driveAPIGrpc.getEvents(ProtobufConversionUtil.toProtobufLimit(2, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getEvents(2);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getEvents_wrong_secret() {
        StreamObserver<DriveApiService.Events> streamObserver = Mockito.mock(StreamObserver.class);
        List<? extends Event> events = List.of(
                new EventClientDto(DriveApiService.Event.newBuilder().setSyncDirection(DriveApiService.Event.SyncDirection.UP)
                        .setLocalFile("/loc").setDirectory(true).setSourceDeleted(false).setRemoteFile("/rem").setTimestamp(432523L).build()),
                new EventClientDto(DriveApiService.Event.newBuilder().setSyncDirection(DriveApiService.Event.SyncDirection.DOWN)
                        .setLocalFile("/loc1").setDirectory(true).setSourceDeleted(true).setRemoteFile("/rem1").setTimestamp(75934L).build())
        );
        Mockito.doReturn(events).when(driveAPIServer).getEvents(2);

        driveAPIGrpc.getEvents(ProtobufConversionUtil.toProtobufLimit(2, "wrong_seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).getEvents(2);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getEvents_missing_secret() {
        StreamObserver<DriveApiService.Events> streamObserver = Mockito.mock(StreamObserver.class);
        List<? extends Event> events = List.of(
                new EventClientDto(DriveApiService.Event.newBuilder().setSyncDirection(DriveApiService.Event.SyncDirection.UP)
                        .setLocalFile("/loc").setDirectory(true).setSourceDeleted(false).setRemoteFile("/rem").setTimestamp(432523L).build()),
                new EventClientDto(DriveApiService.Event.newBuilder().setSyncDirection(DriveApiService.Event.SyncDirection.DOWN)
                        .setLocalFile("/loc1").setDirectory(true).setSourceDeleted(true).setRemoteFile("/rem1").setTimestamp(75934L).build())
        );
        Mockito.doReturn(events).when(driveAPIServer).getEvents(2);

        driveAPIGrpc.getEvents(ProtobufConversionUtil.toProtobufLimit(2, null), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).getEvents(2);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
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
        driveAPIGrpc.getNotifications(ProtobufConversionUtil.toProtobufLimit(3, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getNotifications(3);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(ProtobufConversionUtil.toProtobufNotifications(notifications));
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).getNotifications(Mockito.anyInt());

        driveAPIGrpc.getNotifications(ProtobufConversionUtil.toProtobufLimit(3, "seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getNotifications(3);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getNotifications_wrong_secret() {
        StreamObserver<DriveApiService.Notifications> streamObserver = Mockito.mock(StreamObserver.class);
        List<Notification> notifications = List.of(
                Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir").localFile("/loc4").remoteFile("/rem").message("mEsSaGe1").timestamp(324234L).build(),
                Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir2").localFile("/loc5").remoteFile("/rem").message("mEsSaGe2").timestamp(67543L).build(),
                Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir6").localFile("/loc7").remoteFile("/rem").message("mEsSaGe3").timestamp(8543L).build()
        );
        Mockito.doReturn(notifications).when(driveAPIServer).getNotifications(3);

        driveAPIGrpc.getNotifications(ProtobufConversionUtil.toProtobufLimit(3, "wrong_seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).getNotifications(3);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getNotifications_missing_secret() {
        StreamObserver<DriveApiService.Notifications> streamObserver = Mockito.mock(StreamObserver.class);
        List<Notification> notifications = List.of(
                Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir").localFile("/loc4").remoteFile("/rem").message("mEsSaGe1").timestamp(324234L).build(),
                Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir2").localFile("/loc5").remoteFile("/rem").message("mEsSaGe2").timestamp(67543L).build(),
                Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir6").localFile("/loc7").remoteFile("/rem").message("mEsSaGe3").timestamp(8543L).build()
        );
        Mockito.doReturn(notifications).when(driveAPIServer).getNotifications(3);

        driveAPIGrpc.getNotifications(ProtobufConversionUtil.toProtobufLimit(3, null), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).getNotifications(3);
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getSyncJobsLive() {
        StreamObserver<DriveApiService.SyncJobsLive> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJobLive> syncJobsLive = List.of(
                new SyncJobLive("http://loc", false,true,3,4,5, 6),
                new SyncJobLive("http://loc2", true,false,6,7,8, 9)
        );
        Mockito.doReturn(syncJobsLive).when(driveAPIServer).getSyncJobsLive();
        driveAPIGrpc.getSyncJobsLive(ProtobufConversionUtil.toProtobufEmpty("seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getSyncJobsLive();
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(ProtobufConversionUtil.toProtobufSyncJobsLive(syncJobsLive));
        Mockito.verify(streamObserver, Mockito.times(0)).onError(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();

        Mockito.reset(driveAPIServer);
        Mockito.reset(streamObserver);

        Mockito.doThrow(new RuntimeException()).when(driveAPIServer).getSyncJobsLive();

        driveAPIGrpc.getSyncJobsLive(ProtobufConversionUtil.toProtobufEmpty("seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(1)).getSyncJobsLive();
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(RuntimeException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }

    @Test
    public void getSyncJobsLive_wrong_secret() {
        StreamObserver<DriveApiService.SyncJobsLive> streamObserver = Mockito.mock(StreamObserver.class);
        List<SyncJobLive> syncJobsLive = List.of(
                new SyncJobLive("http://loc", false,true,3,4,5, 6),
                new SyncJobLive("http://loc2", true,false,6,7,8, 9)
        );
        Mockito.doReturn(syncJobsLive).when(driveAPIServer).getSyncJobsLive();

        driveAPIGrpc.getSyncJobsLive(ProtobufConversionUtil.toProtobufEmpty("wrong_seCReT".getBytes(StandardCharsets.UTF_8)), streamObserver);
        Mockito.verify(driveAPIServer, Mockito.times(0)).getSyncJobsLive();
        Mockito.verify(streamObserver, Mockito.times(0)).onNext(Mockito.any());
        Mockito.verify(streamObserver, Mockito.times(1)).onError(Mockito.any(DriveAPIGrpcImpl.DriveAPIUnauthorizedException.class));
        Mockito.verify(streamObserver, Mockito.times(1)).onCompleted();
    }
}