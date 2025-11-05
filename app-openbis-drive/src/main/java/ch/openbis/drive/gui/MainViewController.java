package ch.openbis.drive.gui;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.maincontent.LogsPanel;
import ch.openbis.drive.gui.maincontent.NotificationsPanel;
import ch.openbis.drive.gui.maincontent.SettingsPanel;
import ch.openbis.drive.gui.maincontent.SynchronizationTasksPanel;
import ch.openbis.drive.gui.util.AboutDialog;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.SharedContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static ch.openbis.drive.gui.util.NodeNavigationUtil.closeAndClearChildNodes;


public class MainViewController {
    public static final String SIDE_MENU_ID = "side-menu";
    public static final String MAIN_CONTENT_PANEL_ID = "main-content";
    public static final String SYNC_TASKS_BUTTON_ID = "menu-sync-tasks";
    public static final String LOGS_ID = "menu-logs-id";
    public static final String NOTIFICATIONS_ID = "menu-notifications-id";
    public static final String SETTINGS_ID = "menu-settings-id";

    public static final String ACTIVE_TAB_BUTTON_STYLE_CLASS = "active-tab-button";

    @FXML()
    AnchorPane root;

    final SimpleStringProperty activeSectionId = new SimpleStringProperty();

    @FXML()
    public void initialize() {
        I18n i18n = SharedContext.getContext().getI18n();

        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setPrefSize(DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH, DisplaySettings.DEFAULT_INITIAL_WINDOW_HEIGHT);

        //Side menu definition
        AnchorPane sideMenu = new AnchorPane();
        sideMenu.setId(SIDE_MENU_ID);
        sideMenu.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        sideMenu.setPrefSize(DisplaySettings.SIDE_MENU_WIDTH, DisplaySettings.DEFAULT_INITIAL_WINDOW_HEIGHT);
        sideMenu.getStyleClass().add(SIDE_MENU_ID);

        VBox sideMenuButtonList = new VBox();
        sideMenu.getChildren().add(sideMenuButtonList);

        Pane logo = getOpenBisDriveTopLeftLogo();
        sideMenuButtonList.getChildren().add(logo);

        Button syncTasksButton = new Button(); syncTasksButton.setId(SYNC_TASKS_BUTTON_ID);
        Button logsButton = new Button(); logsButton.setId(LOGS_ID);
        Button notificationsButton = new Button(); notificationsButton.setId(NOTIFICATIONS_ID);
        Button settingsButton = new Button(); settingsButton.setId(SETTINGS_ID);

        syncTasksButton.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> activeSectionId.setValue(SYNC_TASKS_BUTTON_ID));
        logsButton.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> activeSectionId.setValue(LOGS_ID));
        notificationsButton.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> activeSectionId.setValue(NOTIFICATIONS_ID));
        settingsButton.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> activeSectionId.setValue(SETTINGS_ID));

        activeSectionId.addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                switch (newValue) {
                    case SYNC_TASKS_BUTTON_ID -> activateSynchronizationTasksPanel();
                    case LOGS_ID -> activateLogsPanel();
                    case NOTIFICATIONS_ID -> activateNotificationsPanel();
                    case SETTINGS_ID -> activateSettingsPanel();
                }
            }
        });

        List<Button> sideMenuButtons = List.of(syncTasksButton, logsButton, notificationsButton, settingsButton);

        sideMenuButtons.forEach( (button) -> {
            button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            button.setPrefSize(DisplaySettings.SIDE_MENU_WIDTH, DisplaySettings.SIDE_MENU_BUTTON_HEIGHT);
        });

        syncTasksButton.textProperty().bind(i18n.createStringBinding("main_navigation.sync_tasks"));
        logsButton.textProperty().bind(i18n.createStringBinding("main_navigation.logs"));
        notificationsButton.textProperty().bind(i18n.createStringBinding("main_navigation.notifications"));
        settingsButton.textProperty().bind(i18n.createStringBinding("main_navigation.settings"));

        sideMenuButtonList.getChildren().addAll(sideMenuButtons);

        Label aboutButton = getAboutButton(i18n);
        AnchorPane.setLeftAnchor(aboutButton, 10.0);
        AnchorPane.setBottomAnchor(aboutButton, 45.0);
        sideMenu.getChildren().add(0,aboutButton);
        //end side menu definition

        //Main content panel definition
        AnchorPane mainContentPanel = new AnchorPane();
        mainContentPanel.setId(MAIN_CONTENT_PANEL_ID);
        mainContentPanel.setTranslateX(DisplaySettings.SIDE_MENU_WIDTH);
        mainContentPanel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        //end main content panel definition

        root.getChildren().add(sideMenu);
        root.getChildren().add(mainContentPanel);

        resize();
        root.widthProperty().addListener((size)->{
            resize();
        });
        root.heightProperty().addListener((size)->{
            resize();
        });

        activateSynchronizationTasksPanel();
    }

    private Pane getOpenBisDriveTopLeftLogo() {
        BorderPane logo = new BorderPane();

        Node logoImage;
        ImageView image = null;
        try {
            image = new ImageView(new Image(
                    Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("images/openbis-drive-logo-small.png"))
            ));
            image.setSmooth(true);
            image.setPreserveRatio(true);
            image.setFitHeight(DisplaySettings.SIDE_MENU_BUTTON_HEIGHT);
            image.setTranslateY(DisplaySettings.SIDE_MENU_BUTTON_HEIGHT * 0.08);
        } catch (Exception e) {
            System.err.println("Error loading top-left logo image");
        }
        if (image != null) {
            logoImage = image;
        } else {
            Label logoText = new Label(DisplaySettings.LOGO_TEXT);
            logoText.setFont(Font.font("Sans-Serif", 20));
            logoImage = logoText;
        }

        logo.setBackground(Background.fill(Color.WHITE));
        logo.setCenter(logoImage);
        logo.setMaxSize(DisplaySettings.SIDE_MENU_WIDTH, DisplaySettings.SIDE_MENU_BUTTON_HEIGHT);
        logo.setPrefSize(DisplaySettings.SIDE_MENU_WIDTH, DisplaySettings.SIDE_MENU_BUTTON_HEIGHT);
        return logo;
    }

    synchronized private void resize() {
        Window stage = Optional.ofNullable(root.getScene()).map(Scene::getWindow).filter(window -> window instanceof Stage).orElse(null);

        double windowWidth = stage != null ? stage.getWidth() : DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH;
        double windowHeight = stage != null ? stage.getHeight() : DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH;

        root.getChildren().stream().filter(it -> it instanceof AnchorPane).map(it -> (AnchorPane) it).forEach( child -> {
            if (SIDE_MENU_ID.equals(child.getId())) {
                child.setPrefHeight(windowHeight);
            }
            if (MAIN_CONTENT_PANEL_ID.equals(child.getId())) {
                child.setMaxSize(windowWidth - DisplaySettings.SIDE_MENU_WIDTH, windowHeight - 30);
                child.setMinSize(windowWidth - DisplaySettings.SIDE_MENU_WIDTH, windowHeight - 30);
                child.setPrefSize(windowWidth - DisplaySettings.SIDE_MENU_WIDTH, windowHeight - 30);
            }
        });
    }

    synchronized void activateSynchronizationTasksPanel() {
        AnchorPane mainContentPanel = (AnchorPane) this.root.getChildren().filtered(it -> it instanceof AnchorPane && MAIN_CONTENT_PANEL_ID.equals(it.getId()))
                .stream().findFirst().orElse(null);

        closeAndClearChildNodes(mainContentPanel);
        mainContentPanel.getChildren().add(new SynchronizationTasksPanel(mainContentPanel, (voidArg) -> {activateSynchronizationTasksPanel(); return null;}));
        markButtonAsActiveById(SYNC_TASKS_BUTTON_ID);
    }

    synchronized void activateLogsPanel() {
        AnchorPane mainContentPanel = (AnchorPane) this.root.getChildren().filtered(it -> it instanceof AnchorPane && MAIN_CONTENT_PANEL_ID.equals(it.getId()))
                .stream().findFirst().orElse(null);

        closeAndClearChildNodes(mainContentPanel);
        mainContentPanel.getChildren().add(new LogsPanel(mainContentPanel));
        markButtonAsActiveById(LOGS_ID);
    }

    synchronized void activateNotificationsPanel() {
        AnchorPane mainContentPanel = (AnchorPane) this.root.getChildren().filtered(it -> it instanceof AnchorPane && MAIN_CONTENT_PANEL_ID.equals(it.getId()))
                .stream().findFirst().orElse(null);

        closeAndClearChildNodes(mainContentPanel);
        mainContentPanel.getChildren().add(new NotificationsPanel(mainContentPanel));
        markButtonAsActiveById(NOTIFICATIONS_ID);
    }

    synchronized void activateSettingsPanel() {
        AnchorPane mainContentPanel = (AnchorPane) this.root.getChildren().filtered(it -> it instanceof AnchorPane && MAIN_CONTENT_PANEL_ID.equals(it.getId()))
                .stream().findFirst().orElse(null);

        closeAndClearChildNodes(mainContentPanel);
        mainContentPanel.getChildren().add(new SettingsPanel(mainContentPanel));
        markButtonAsActiveById(SETTINGS_ID);
    }

    synchronized List<Button> getSideMenuButtons() {
        AnchorPane sideMenuAnchorPane = (AnchorPane) this.root.getChildren().filtered(it -> it instanceof AnchorPane && SIDE_MENU_ID.equals(it.getId()))
                .stream().findFirst().orElse(null);

        VBox sideMenuButtonWrapper = (VBox) Optional.ofNullable(sideMenuAnchorPane)
                .flatMap(pane -> pane.getChildren().filtered(it -> it instanceof VBox)
                .stream().findFirst()).orElse(null);

        return Optional.ofNullable(sideMenuButtonWrapper)
                .map(vBox ->
                        vBox.getChildren().stream().filter(it -> it instanceof Button)
                                .map(it -> (Button) it).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private Label getAboutButton(I18n i18n) {
        Label aboutButton = new Label();
        aboutButton.getStyleClass().add(DisplaySettings.FONT_AWESOME_CLASS);
        aboutButton.setStyle("-fx-font-size: 18");
        aboutButton.setText(DisplaySettings.FONT_AWESOME_7_FREE_SOLID_CIRCLE_QUESTION_MARK);
        aboutButton.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> {
            new AboutDialog(i18n, (Stage) root.getScene().getWindow()).showAndWait();
        });
        return aboutButton;
    }

    synchronized void markButtonAsActiveById(@NonNull String id) {
        getSideMenuButtons().forEach(
                button -> {
                    if (id.equals(button.getId())) {
                        button.getStyleClass().add(ACTIVE_TAB_BUTTON_STYLE_CLASS);
                    } else {
                        button.getStyleClass().removeIf( styleClass -> styleClass.equals(ACTIVE_TAB_BUTTON_STYLE_CLASS));
                    }
                }
        );
    }
}
