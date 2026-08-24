package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.openbis.drive.gui.MainViewController;
import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.util.OpenBISQueryUtil;
import ch.openbis.drive.util.ParallelExecutionUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;


public class SyncJobSessionChoiceDialogStep implements DialogStep<SyncJobDialogContext, SyncJob> {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ConcurrentHashMap<RadioButton, OpenBISQueryUtil.AvailableSession> availableSessionMap =
            new ConcurrentHashMap<>();

    private volatile Future<?> loadingAvailableSessionsFuture;
    private volatile Node content;
    private volatile CompletableFuture<DialogStepResult<SyncJobDialogContext, SyncJob>> result;
    private volatile EventHandler<ActionEvent> applyButtonHandler;

    private volatile long acceptedValidityMillisLeftForPATs = Settings.DEFAULT_EXPIRING_SESSION_WARNING_DAYS * 24 * 60 * 60 * 1000;

    @Override
    public void initialize(
            @NonNull SyncJobDialogContext context,
            @NonNull Dialog<SyncJob> parentDialog,
            @NonNull BooleanProperty applyDisableProperty,
            @lombok.NonNull CompletableFuture<DialogStepResult<SyncJobDialogContext, SyncJob>> resultFuture
    ) {
        this.result = resultFuture;
        this.acceptedValidityMillisLeftForPATs = context.acceptedValidityMillisLeftForPATs();

        I18n i18n = SharedContext.getContext().getI18n();

        VBox progressIndicatorBox = new VBox();
        progressIndicatorBox.setSpacing(20);
        progressIndicatorBox.setAlignment(Pos.CENTER);
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(true);
        progressIndicatorBox.getChildren().add(progressIndicator);

        VBox baseVerticalBox = new VBox();
        baseVerticalBox.setPadding(new Insets(50, 0, 0, 0));
        baseVerticalBox.setSpacing(50);
        baseVerticalBox.setAlignment(Pos.TOP_CENTER);
        this.content = baseVerticalBox;

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
        scrollPane.widthProperty().addListener((obs, oldValue, newValue) -> {
            progressIndicatorBox.setPrefWidth(newValue.doubleValue() - 5);
        });

        baseVerticalBox.getChildren().addAll(
                MainViewController.getOpenBisDriveBigRectangleLogo(500),
                gridPane
        );

        ToggleGroup sessionChoices = new ToggleGroup();

        RadioButton newLoginChoice = new RadioButton(i18n.get("sync_tasks.session_choice.new_login"));

        newLoginChoice.setToggleGroup(sessionChoices);
        sessionChoices.selectToggle(newLoginChoice);

        VBox vBox = new VBox();
        vBox.setSpacing(20);
        vBox.getChildren().add(newLoginChoice);

        loadingAvailableSessionsFuture = ParallelExecutionUtil.EXECUTOR_SERVICE.submit(
            new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Set<OpenBISQueryUtil.AvailableSession> availableSessionSet =
                            OpenBISQueryUtil.getInstance().getAvailableSessions(
                                    context.currentSyncJobs(),
                                    acceptedValidityMillisLeftForPATs
                            );

                    if (availableSessionSet.isEmpty()) {
                        result.complete(
                                new DialogStepResult<>(
                                    DialogStepResultEnum.NEXT,
                                    new SyncJobDialogContext(
                                            context.toBeModified(),
                                            context.currentSyncJobs(),
                                            new SyncJobSessionChoiceResult(true, null),
                                            acceptedValidityMillisLeftForPATs
                                    ),
                                    null
                                )
                        );
                    } else {
                        Platform.runLater(
                                () -> {
                                    for (OpenBISQueryUtil.AvailableSession availableSession : availableSessionSet) {
                                        RadioButton sessionChoice = new RadioButton(availableSession.username() + " @ " + availableSession.openBISUrl());
                                        Label intervalOfTimeBeforeExpiry = new Label(SyncJobCard.formatExpiresInText(Date.from(availableSession.validUntil().toInstant())));
                                        intervalOfTimeBeforeExpiry.getStyleClass().add(DisplaySettings.BOLD_TEXT);
                                        List<Label> sessionChoiceLabels = List.of(
                                                new Label(" (" + i18n.get("sync_tasks.modal_panel.sync_task_modal.expires_in")),
                                                intervalOfTimeBeforeExpiry,
                                                new Label(i18n.get("sync_tasks.modal_panel.sync_task_modal.on_date") + " " + DATE_TIME_FORMATTER.format(
                                                        availableSession.validUntil().toInstant().atZone(ZoneId.systemDefault())
                                                ) + ")"));
                                        HBox sessionChoiceBox = new HBox();
                                        sessionChoiceBox.setSpacing(4);
                                        sessionChoiceBox.getChildren().add(sessionChoice);
                                        sessionChoiceBox.getChildren().addAll(sessionChoiceLabels);
                                        availableSessionMap.put(sessionChoice, availableSession);
                                        sessionChoice.setToggleGroup(sessionChoices);
                                        vBox.getChildren().add(sessionChoiceBox);
                                    }
                                    scrollPane.setContent(vBox);
                                }
                        );
                    }
                    return null;
                }
            }
        );

        applyButtonHandler = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                result.complete(new DialogStepResult<>(
                                DialogStepResultEnum.NEXT,
                                new SyncJobDialogContext(
                                        context.toBeModified(),
                                        context.currentSyncJobs(),
                                        new SyncJobSessionChoiceResult(true, availableSessionMap.get((RadioButton) sessionChoices.getSelectedToggle())),
                                        acceptedValidityMillisLeftForPATs
                                ),
                                null
                        )
                );
            }
        };
    }

    @Override
    public @NonNull Node getContent() {
        return content;
    }

    @Override
    public @lombok.NonNull EventHandler<ActionEvent> getApplyHandler() {
        return applyButtonHandler;
    }

    @Override
    public CompletableFuture<DialogStepResult<SyncJobDialogContext, SyncJob>> getResult() {
        return result;
    }

    @Override
    public void close() throws Exception {
        if (loadingAvailableSessionsFuture != null) {
            loadingAvailableSessionsFuture.cancel(true);
        }
    }
}
