package ch.openbis.drive.gui.util;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.model.Event;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.protobuf.client.DriveAPIClientProtobufImpl;
import ch.openbis.drive.util.OpenBISDriveUtil;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServiceCallHandler {
    private final @NonNull DriveAPIClientProtobufImpl driveAPIClientProtobuf;
    private final @NonNull I18n i18n;
    private final Stage callingStage;

    private static final AtomicBoolean shutdown = new AtomicBoolean(false);

    public ServiceCallHandler(@NonNull DriveAPIClientProtobufImpl driveAPIClientProtobuf,
                              @NonNull I18n i18n,
                              Stage callingStage) {
        this.driveAPIClientProtobuf = driveAPIClientProtobuf;
        this.i18n = i18n;
        this.callingStage = callingStage;
    }

    public @NonNull ServiceCallResult<Settings> getSettings() {
        return doCall( () -> {
            Settings settingsResponse = driveAPIClientProtobuf.getSettings();
            if (settingsResponse.getLanguage() != null) {
               i18n.setLanguage(settingsResponse.getLanguage());
            }
            return settingsResponse;
        });
    }

    public ServiceCallResult<Boolean> setSettings(@NonNull Settings settings) {
        return doCall(() -> {
            driveAPIClientProtobuf.setSettings(settings);
            if (settings.getLanguage() != null) {
                i18n.setLanguage(settings.getLanguage());
            }
            return true;
        });
    }

    public ServiceCallResult<Boolean> enableSyncJob(@NonNull SyncJob syncJob) {
        return doCall(() -> {
            driveAPIClientProtobuf.startSyncJobs(Collections.singletonList(syncJob));
            return true;
        });
    }

    public ServiceCallResult<Boolean> disableSyncJob(@NonNull SyncJob syncJob) {
        return doCall(() -> {
            driveAPIClientProtobuf.stopSyncJobs(Collections.singletonList(syncJob));
            return true;
        });
    }

    public ServiceCallResult<Boolean> addSyncJob(@NonNull SyncJob syncJob) {
        return doCall(() -> {
            driveAPIClientProtobuf.addSyncJobs(Collections.singletonList(syncJob));
            return true;
        });
    }

    public ServiceCallResult<Boolean> removeSyncJob(@NonNull SyncJob syncJob) {
        return doCall(() -> {
            driveAPIClientProtobuf.removeSyncJobs(Collections.singletonList(syncJob));
            return true;
        });
    }

    public ServiceCallResult<List<? extends Event>> getEvents(int limit) {
        return doCall( () ->
            driveAPIClientProtobuf.getEvents(limit)
        );
    }

    public ServiceCallResult<List<Notification>> getNotifications(int limit) {
        return doCall( () ->
            driveAPIClientProtobuf.getNotifications(limit)
        );
    }

    public @NonNull ServiceCallResult<List<SyncJob>> getSyncJobs() {
        return doCall(driveAPIClientProtobuf::getSyncJobs);
    }

    interface ServiceCall<T> {
        T call() throws Exception;
    }

    @SneakyThrows
    <T> ServiceCallResult<T> doCall(ServiceCall<T> callable) {
        int attempt = 0;
        while (true) {
            if (attempt < 3) {
                try {
                    return new ServiceCallResult<>(callable.call(), null);
                } catch (Exception e) {
                    ServiceExceptionAction serviceExceptionAction = handleServiceException(e);
                    if (serviceExceptionAction == ServiceExceptionAction.RETRY) {
                        Thread.sleep(3000);
                    } else if (serviceExceptionAction == ServiceExceptionAction.SHUTDOWN){
                        Platform.exit();
                        return null;
                    } else {
                        return new ServiceCallResult<>(null, e);
                    }
                }
            } else {
                return new ServiceCallResult<>(null, new RuntimeException("Too many attempts calling service"));
            }
            attempt++;
        }
    }

    @Value
    public static class ServiceCallResult<T> {
        @Nullable T ok;
        @Nullable Exception err;

        public ServiceCallResult(T ok, Exception err) {
            this.ok = ok;
            this.err = err;
        }

        public boolean isOk() {
            return this.err == null && this.ok != null;
        }
    }
    public enum ServiceExceptionAction {
        RETRY, SHUTDOWN, RAISE_EXCEPTION
    }
    @SneakyThrows
    public final ServiceExceptionAction handleServiceException(Exception e) {
        synchronized (shutdown) {
            if (e instanceof StatusRuntimeException && ((StatusRuntimeException) e).getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
                if (!shutdown.get()) {

                    if (Platform.isFxApplicationThread()) {
                        return promptForBackroundServiceStart();
                    } else {
                        Platform.runLater( () -> promptForBackroundServiceStart() );
                        return ServiceExceptionAction.RETRY;
                    }

                } else {
                    shutdown.set(true);
                    return ServiceExceptionAction.SHUTDOWN;
                }
            } else {
                e.printStackTrace();

                if (Platform.isFxApplicationThread()) {
                    showBackgroundServiceCallErrorDialog();
                } else {
                    Platform.runLater(this::showBackgroundServiceCallErrorDialog);
                }
                return ServiceExceptionAction.RAISE_EXCEPTION;
            }
        }
    }

    private void showBackgroundServiceCallErrorDialog() {
        synchronized (shutdown) {
            ServiceResponseErrorDialog serviceResponseErrorDialog = new ServiceResponseErrorDialog(i18n, callingStage);
            serviceResponseErrorDialog.showAndWait();
        }
    }

    @SneakyThrows
    private ServiceExceptionAction promptForBackroundServiceStart() {
        synchronized (shutdown) {
            ServiceNotRunningDialog dialog = new ServiceNotRunningDialog(i18n, callingStage);
            Optional<Boolean> tryToStartService = dialog.showAndWait();
            if (tryToStartService.isPresent() && tryToStartService.get()) {
                OpenBISDriveUtil.startServiceBackgroundProcess();
                return ServiceExceptionAction.RETRY;
            } else {
                shutdown.set(true);
                return ServiceExceptionAction.SHUTDOWN;
            }
        }
    }

    public void close() throws Exception {
        driveAPIClientProtobuf.close();
    }
}
