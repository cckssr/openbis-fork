package ch.openbis.drive.gui;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.gui.util.Style;
import ch.openbis.drive.protobuf.client.DriveAPIClientProtobufImpl;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.Locale;

public class OpenDriveApplication extends Application {
    @Override
    @SneakyThrows
    public void start(Stage stage) throws IOException {
        //Initialize shared context: with drive-api-protobuf-client and localization utility
        I18n i18n = new I18n(Locale.getDefault().getLanguage());
        DriveAPIClientProtobufImpl driveAPIClientProtobuf = new DriveAPIClientProtobufImpl(new Configuration());
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
        try {
            SharedContext.closeSharedContext();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
