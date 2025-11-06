package ch.openbis.drive.gui.util;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.protobuf.client.DriveAPIClientProtobufImpl;
import javafx.application.HostServices;
import javafx.scene.Node;
import javafx.stage.Stage;
import lombok.NonNull;

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
}

