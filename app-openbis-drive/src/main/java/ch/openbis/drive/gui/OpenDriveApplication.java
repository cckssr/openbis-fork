package ch.openbis.drive.gui;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.gui.util.Style;
import ch.openbis.drive.protobuf.client.DriveAPIClientProtobufImpl;
import ch.openbis.drive.util.OpenBISDriveUtil;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class OpenDriveApplication extends Application {
    volatile ServerSocket serverSocket;

    @Override
    @SneakyThrows
    public void start(Stage stage) throws IOException {
        //Initialize shared context: with drive-api-protobuf-client and localization utility
        I18n i18n = new I18n(Locale.getDefault().getLanguage());
        Configuration configuration = new Configuration();

        startGuiWakingServerSocket(configuration, stage);

        configuration.readOpenbisDriveProperties();
        DriveAPIClientProtobufImpl driveAPIClientProtobuf = new DriveAPIClientProtobufImpl(configuration);
        SharedContext.initializeSharedContext(driveAPIClientProtobuf, i18n, getHostServices());
        SharedContext.getContext().getServiceCallHandler(stage).getSettings();

        FXMLLoader fxmlLoader = new FXMLLoader(OpenDriveApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH, DisplaySettings.DEFAULT_INITIAL_WINDOW_HEIGHT);
        Style.applyStyle(scene);
        stage.titleProperty().bind(i18n.createStringBinding("main_title"));
        stage.setScene(scene);
        stage.show();
        stage.setWidth(DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH);
        stage.setHeight(DisplaySettings.DEFAULT_INITIAL_WINDOW_HEIGHT);
        DisplaySettings.centerStageInScreen(stage);

        activateSpecificSectionAccordingToLaunchArguments(getParameters().getRaw());
    }

    @Override
    public void init() throws Exception {
        super.init();
        Thread.sleep(2000);
        float systemFontSize = Toolkit.getToolkit().getFontLoader().getSystemFontSize();
        Font.loadFont(Launcher.class.getClassLoader().getResourceAsStream("font/OpenSans.ttf"), systemFontSize);
        Font.loadFont(Launcher.class.getClassLoader().getResourceAsStream("font/OpenSans-Bold.ttf"), systemFontSize);
        Font.loadFont(Launcher.class.getClassLoader().getResourceAsStream("font/OpenSans-BoldItalic.ttf"), systemFontSize);
        Font.loadFont(Launcher.class.getClassLoader().getResourceAsStream("font/OpenSans-Italic.ttf"), systemFontSize);
        Font.loadFont(Launcher.class.getClassLoader().getResourceAsStream("font/FontAwesome-7-Free-Solid-900.otf"), systemFontSize);
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        if ( serverSocket != null ) {
            try {
                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            SharedContext.closeSharedContext();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    void startGuiWakingServerSocket(@NonNull Configuration configuration, @NonNull Stage stage) {
        Thread thread = new Thread( () -> {
            try {
                serverSocket = new ServerSocket(configuration.getOpenbisDriveGuiPort());

                while ( !serverSocket.isClosed() ) {
                    try {
                        Socket socket = serverSocket.accept();

                        String message = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                        Platform.runLater( () -> {
                            stage.show();
                            stage.toFront();
                            activateSpecificSectionAccordingToLaunchArguments(Arrays.stream(message.split(" ")).toList());
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    void activateNotificationsPanel() {
        MainViewController.MAIN_INSTANCE.get().activateNotificationsPanel();
    }

    void activateSynchronizationTasksPanel() {
        MainViewController.MAIN_INSTANCE.get().activateSynchronizationTasksPanel();
    }

    void activateSettingssPanel() {
        MainViewController.MAIN_INSTANCE.get().activateSettingsPanel();
    }

    void activateLogsPanel() {
        MainViewController.MAIN_INSTANCE.get().activateLogsPanel();
    }

    void activateSpecificSectionAccordingToLaunchArguments(@NonNull List<String> parameters) {
        if (parameters.contains(OpenBISDriveUtil.GUISection.SYNC_TASKS.toLabel())) {
            activateSynchronizationTasksPanel();
        } else if (parameters.contains(OpenBISDriveUtil.GUISection.SETTINGS.toLabel())) {
            activateSettingssPanel();
        } else if (parameters.contains(OpenBISDriveUtil.GUISection.EVENTS.toLabel())) {
            activateLogsPanel();
        } else if (parameters.contains(OpenBISDriveUtil.GUISection.NOTIFICATIONS.toLabel())) {
            activateNotificationsPanel();
        }
    }
}
