package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.openbis.drive.gui.MainViewController;
import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.gui.util.Style;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.util.OpenBISQueryUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;


public class SyncJobSessionChoiceDialog extends Dialog<SyncJobSessionChoiceResult> {
    public static final int EXTENDED_HEIGHT = SyncJobDialog.EXTENDED_HEIGHT - 100;
    private final ConcurrentHashMap<RadioButton, OpenBISQueryUtil.AvailableSession> availableSessionMap =
            new ConcurrentHashMap<>();

    private final Future<?> loadingAvailableSessionsFuture;

    public SyncJobSessionChoiceDialog(Stage mainStage, List<SyncJob> currentSyncJobs) {
        super();
        I18n i18n = SharedContext.getContext().getI18n();

        VBox progressIndicatorBox = new VBox();
        progressIndicatorBox.setSpacing(20);
        progressIndicatorBox.setPadding(new Insets(0, 0, 0, 150));
        progressIndicatorBox.setAlignment(Pos.CENTER);
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(true);
        progressIndicatorBox.getChildren().add(progressIndicator);

        VBox baseVerticalBox = new VBox();
        baseVerticalBox.setPadding(new Insets(50, 0, 0, 0));
        baseVerticalBox.setSpacing(50);
        baseVerticalBox.setAlignment(Pos.TOP_CENTER);

        GridPane gridPane = new GridPane();
        int columnsCount = 6;
        for (int i = 0; i<columnsCount; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(16.6);
            gridPane.getColumnConstraints().add(column);
        }

        Label explanation = new Label(i18n.get("sync_tasks.session_choice.explanation"));
        explanation.setPadding(new Insets(0, 0, 50, 0));
        gridPane.add(explanation, 1, 0, 4, 1);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setPrefHeight(1000);
        scrollPane.setStyle("-fx-background-color: transparent");
        gridPane.add(scrollPane, 1, 1, 4, 1);

        scrollPane.setContent(progressIndicatorBox);

        baseVerticalBox.getChildren().addAll(
                MainViewController.getOpenBisDriveBigRectangleLogo(500),
                gridPane
        );

        getDialogPane().setContent(baseVerticalBox);

        initStyle(StageStyle.DECORATED);

        final Window window = getDialogPane().getScene().getWindow();
        window.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Platform.runLater(() -> DisplaySettings.centerStageOnMainStage((Stage) window, mainStage));
            }
        });
        Style.applyStyle(getDialogPane().getScene());

        ToggleGroup sessionChoices = new ToggleGroup();

        RadioButton newLoginChoice = new RadioButton(i18n.get("sync_tasks.session_choice.new_login"));

        newLoginChoice.setToggleGroup(sessionChoices);
        sessionChoices.selectToggle(newLoginChoice);

        VBox vBox = new VBox();
        vBox.setSpacing(20);
        vBox.getChildren().add(newLoginChoice);

        loadingAvailableSessionsFuture = OpenBISQueryUtil.EXECUTOR_SERVICE.submit(
            new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Set<OpenBISQueryUtil.AvailableSession> availableSessionSet =
                            OpenBISQueryUtil.getInstance().getAvailableSessions(currentSyncJobs);

                    if (availableSessionSet.isEmpty()) {
                        Platform.runLater( () -> {
                            setResult(new SyncJobSessionChoiceResult(true, null));
                            close();
                        });
                    } else {
                        Platform.runLater(
                                () -> {
                                    for (OpenBISQueryUtil.AvailableSession availableSession : availableSessionSet) {
                                        RadioButton sessionChoice = new RadioButton(
                                                availableSession.username() + " @ " + availableSession.openBISUrl() +
                                                        " (" + i18n.get("sync_tasks.session_choice.expires") + ": " +
                                                        availableSession.validUntil() + ")"
                                        );
                                        availableSessionMap.put(sessionChoice, availableSession);
                                        sessionChoice.setToggleGroup(sessionChoices);
                                        vBox.getChildren().add(sessionChoice);
                                    }

                                    scrollPane.setContent(vBox);
                                }
                        );
                    }
                    return null;
                }
            }
        );

        getDialogPane().getButtonTypes().add(ButtonType.APPLY);
        Button applyButton = (Button) getDialogPane().lookupButton(ButtonType.APPLY);
        applyButton.textProperty().bind(i18n.createStringBinding("generic_buttons.next"));

        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        ((Button) getDialogPane().lookupButton(ButtonType.CANCEL)).textProperty().bind(i18n.createStringBinding("generic_buttons.cancel"));

        Platform.runLater( () -> {
            getDialogPane().getScene().getWindow().setWidth(SyncJobDialog.EXTENDED_WIDTH);
            getDialogPane().getScene().getWindow().setHeight(EXTENDED_HEIGHT);
        } );

        setResultConverter((dialogButton) -> {
            loadingAvailableSessionsFuture.cancel(true);
            if (dialogButton.getButtonData().getTypeCode().equals(ButtonType.APPLY.getButtonData().getTypeCode())) {
                return new SyncJobSessionChoiceResult(true, availableSessionMap.get((RadioButton) sessionChoices.getSelectedToggle()));
            } else {
                return new SyncJobSessionChoiceResult(false, null);
            }
        });
    }
}
