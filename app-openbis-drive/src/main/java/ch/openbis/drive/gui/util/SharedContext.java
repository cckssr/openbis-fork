package ch.openbis.drive.gui.util;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.protobuf.client.DriveAPIClientProtobufImpl;
import javafx.application.HostServices;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.Data;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class SharedContext {
    private final DriveAPIClientProtobufImpl driveAPIClient;
    private final I18n i18n;
    private final HostServices hostServices;

    private static volatile SharedContext staticInstance;
    private static final AtomicBoolean closed = new AtomicBoolean(false);

    synchronized public static void initializeSharedContext(@NonNull DriveAPIClientProtobufImpl driveAPIClient,
                                                            @NonNull I18n i18n,
                                                            @NonNull HostServices hostServices) {
        if (staticInstance == null) {
            staticInstance = new SharedContext(driveAPIClient, i18n, hostServices);
        } else {
            throw new IllegalStateException("SharedContext already initialized");
        }
    }

    synchronized public static void closeSharedContext() {
        if (staticInstance != null) {
           try {
               staticInstance.driveAPIClient.close();
           } catch (Exception e) {
               e.printStackTrace();
           }
            closed.set(true);
        } else {
            throw new IllegalStateException("SharedContext not initialized yet");
        }
    }

    SharedContext(DriveAPIClientProtobufImpl driveAPIClient, I18n i18n, HostServices hostServices) {
        this.driveAPIClient = driveAPIClient;
        this.i18n = i18n;
        this.hostServices = hostServices;
    }

    public static SharedContext getContext() {
        if (staticInstance != null) {
            if(!closed.get()) {
                return staticInstance;
            } else {
                throw new IllegalStateException("SharedContext closed");
            }
        } else {
            throw new IllegalStateException("SharedContext not initialized yet");
        }
    }

    public ServiceCallHandler getServiceCallHandler(Stage callingStage) {
        return new ServiceCallHandler(driveAPIClient, i18n, callingStage);
    }

    public ServiceCallHandler getServiceCallHandler(Node callingNode) {
        return new ServiceCallHandler(driveAPIClient, i18n, DisplaySettings.getStageFromNode(callingNode));
    }

    public I18n getI18n() {
        return i18n;
    }

    public HostServices getHostServices() {
        return hostServices;
    }

    //// Log and notification table state
    public enum LogTableColumn { LOCAL_FILE, REMOTE_FILE, FILE_TYPE, DATE_TIME, EVENT }
    public enum NotificationTableColumn { TYPE, NOTIFICATION, LOCAL_DIRECTORY, FILE, DATE_TIME }

    @Data
    public static class LogTableState {
        private @NonNull HashMap<@NonNull LogTableColumn, @NonNull Integer> columnSizes = new HashMap<>();
        private @Nullable Pair<@NonNull LogTableColumn, TableColumn.SortType> sorting;
    }
    @Data
    public static class NotificationTableState {
        private @NonNull HashMap<@NonNull NotificationTableColumn, @NonNull Integer> columnSizes = new HashMap<>();
        private @Nullable Pair<@NonNull NotificationTableColumn, TableColumn.SortType> sorting;
    }

    private final LogTableState logTableState = new LogTableState();
    private final NotificationTableState notificationTableState = new NotificationTableState();

    public synchronized Integer getColumSize(@NonNull LogTableColumn logTableColumn) {
        return logTableState.getColumnSizes().get(logTableColumn);
    }
    public synchronized Integer getColumSize(@NonNull NotificationTableColumn notificationTableColumn) {
        return notificationTableState.getColumnSizes().get(notificationTableColumn);
    }
    public synchronized void setColumSize(@NonNull LogTableColumn logTableColumn, @Nullable Integer size) {
        if (size != null) {
            logTableState.getColumnSizes().put(logTableColumn, size);
        } else {
            logTableState.getColumnSizes().remove(logTableColumn);
        }
    }
    public synchronized void setColumSize(@NonNull NotificationTableColumn notificationTableColumn, @Nullable Integer size) {
        if (size != null) {
            notificationTableState.getColumnSizes().put(notificationTableColumn, size);
        } else {
            notificationTableState.getColumnSizes().remove(notificationTableColumn);
        }
    }
    public synchronized @Nullable Pair<LogTableColumn, TableColumn.SortType> getLogTableSorting() {
        return logTableState.getSorting();
    }
    public synchronized @Nullable Pair<NotificationTableColumn, TableColumn.SortType> getNotificationTableSorting() {
        return notificationTableState.getSorting();
    }
    public synchronized void setSortedColumn(@Nullable LogTableColumn logTableColumn, @Nullable TableColumn.SortType sorting) {
        if (logTableColumn != null && sorting != null) {
            logTableState.setSorting(new Pair<>(logTableColumn, sorting));
        } else {
            logTableState.setSorting(null);
        }
    }
    public synchronized void setSortedColumn(@Nullable NotificationTableColumn notificationTableColumn, @Nullable TableColumn.SortType sorting) {
        if (notificationTableColumn != null && sorting != null) {
            notificationTableState.setSorting(new Pair<>(notificationTableColumn, sorting));
        } else {
            notificationTableState.setSorting(null);
        }
    }
    public synchronized void setLogTableUnsorted() {
        logTableState.setSorting(null);
    }
    public synchronized void setNotificationTableUnsorted() {
        notificationTableState.setSorting(null);
    }
    ////
}

