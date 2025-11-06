package ch.openbis.drive.gui;

import ch.openbis.drive.gui.util.DisplaySettings;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

public class Preloader extends javafx.application.Preloader {
    private ProgressBar progressBar;
    private Stage stage;

    private Scene createPreloaderScene() {
        BorderPane borderPane = new BorderPane();
        borderPane.setBackground(Background.fill(Color.WHITE));
        borderPane.setMinSize(DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH, DisplaySettings.DEFAULT_INITIAL_WINDOW_HEIGHT);
        borderPane.setPrefSize(DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH, DisplaySettings.DEFAULT_INITIAL_WINDOW_HEIGHT);
        borderPane.setMaxSize(DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH, DisplaySettings.DEFAULT_INITIAL_WINDOW_HEIGHT);

        ImageView image = null;
        try {
            image = new ImageView(new Image(
                    Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("images/openbis-drive-logo-medium.png"))
            ));
            image.setSmooth(true);
            image.setFitWidth(DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH * 0.9);
            image.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Error loading initial logo");
        }

        if (image != null) {
            borderPane.setCenter(image);
        } else {
            Label logoText = new Label(DisplaySettings.LOGO_TEXT);
            logoText.setFont(Font.font("Sans-Serif", 100));
            borderPane.setCenter(logoText);
        }

        return new Scene(borderPane, DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH, DisplaySettings.DEFAULT_INITIAL_WINDOW_HEIGHT);
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(createPreloaderScene());
        stage.show();
        DisplaySettings.centerStageInScreen(stage);
    }

    @Override
    public void handleProgressNotification(ProgressNotification pn) {
        //
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification evt) {
        if (evt.getType() == StateChangeNotification.Type.BEFORE_START) {
            stage.hide();
            stage.close();
        }
    }
}
