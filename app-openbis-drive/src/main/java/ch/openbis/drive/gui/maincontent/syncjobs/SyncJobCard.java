package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.maincontent.ResizablePanel;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.gui.util.UsageUtil;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.util.OpenBISQueryUtil;
import ch.openbis.drive.util.ParallelExecutionUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import lombok.NonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;


public class SyncJobCard extends ResizablePanel implements AutoCloseable {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm z");
    private final long warningMillisBeforExpiringSession;
    public static final int SYNC_JOB_ATTRIBUTE_WIDTH = 165;
    private final I18n i18n;
    private final SyncJob syncJob;
    private final HBox hBoxContainer;
    private final RadioButton radioButton;
    private final VBox syncJobCoordinates;
    private final VBox syncJobAttributes;

    private final List<SyncJobCardLabel> syncJobCardLabels;
    private final SyncJobCardLabel sessionValidUntilLabel;

    private final AnchorPane progressBarWithFraction;
    private final Label liveStatus;
    private final AnimatedProgressBar animatedProgressBar;
    private final Label progressFraction;

    private volatile OpenBISQueryUtil.PATCheckResult patCheckResult = null;

    public SyncJobCard(
            @NonNull SyncJob syncJob,
            @NonNull Pane parent,
            long warningMillisBeforExpiringSession
    ) {
        super(parent);

        this.warningMillisBeforExpiringSession = warningMillisBeforExpiringSession;

        SyncJobCard thisRef = this;
        ParallelExecutionUtil.EXECUTOR_SERVICE.submit(new Task<>() {
            @Override
            protected Void call() throws Exception {
                patCheckResult = OpenBISQueryUtil.getInstance()
                        .checkPAT(syncJob.getOpenBisUrl(), syncJob.getOpenBisPersonalAccessToken());
                Platform.runLater(
                    () -> {
                        if (patCheckResult.result() == OpenBISQueryUtil.PATCheckResultEnum.OK) {
                            setSessionValidUntilLabel();
                        } else {
                            thisRef.getStyleClass().removeIf(DisplaySettings.SYNC_JOB_CARD_CLASS::equals);
                            thisRef.getStyleClass().add(DisplaySettings.SYNC_JOB_CARD_ERROR_CLASS);
                            setErrorMessage(patCheckResult);
                        }
                    }
                );
                return null;
            }
        });

        this.syncJob = syncJob;
        this.radioButton = new RadioButton();

        i18n = SharedContext.getContext().getI18n();
        EventHandler<MouseEvent> mouseClickEvent = new EventHandler<>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                radioButton.selectedProperty().setValue(!radioButton.selectedProperty().getValue());
            }
        };

        this.getStyleClass().add(DisplaySettings.SYNC_JOB_CARD_CLASS);
        this.setMaxSize(10 * DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH, DisplaySettings.SYNC_TASK_PANEL_JOB_CARD_HEIGHT);
        this.setMinWidth(DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH - DisplaySettings.SIDE_MENU_WIDTH - 100);

        hBoxContainer = new HBox();
        hBoxContainer.setMaxWidth(this.getMaxWidth());
        hBoxContainer.setPrefHeight(this.getPrefHeight());
        hBoxContainer.setSpacing(40);
        hBoxContainer.setAlignment(Pos.CENTER_LEFT);
        hBoxContainer.getChildren().add(radioButton);

        VBox dataBox = new VBox();
        TextField titleLabel = new TextField(syncJob.getTitle());
        titleLabel.setPrefWidth(this.getMaxWidth());
        titleLabel.setStyle(String.format("-fx-font-weight: bold; -fx-font-size: %spt; -fx-background-color: transparent", 14));
        titleLabel.setPadding(new Insets(15, 0, 10, -8));
        titleLabel.setEditable(false);
        titleLabel.setOnMouseClicked(mouseClickEvent);
        dataBox.getChildren().add(titleLabel);
        hBoxContainer.getChildren().add(dataBox);

        AnchorPane labelPane = new AnchorPane();
        syncJobCoordinates = new VBox();
        syncJobCoordinates.setMaxHeight(Double.MAX_VALUE);
        syncJobCoordinates.setPrefHeight(DisplaySettings.SYNC_TASK_PANEL_JOB_CARD_HEIGHT);
        ObservableValue<Double> desiredSyncJobCoordinatesWidth = parent.widthProperty().map( (parentWidth) -> Math.min(parentWidth.doubleValue(), this.getMaxWidth()) - SYNC_JOB_ATTRIBUTE_WIDTH - 180);

        SyncJobCardLabel entityPermIdLabel = new SyncJobCardLabel(i18n.get("main_panel.sync_tasks.sync_job_card.entity_perm_id"), syncJob.getEntityPermId(), SyncJobCardLabel.DEFAULT_SMALL_LABEL_SIZE, desiredSyncJobCoordinatesWidth, mouseClickEvent);
        SyncJobCardLabel serverDirectoryLabel = new SyncJobCardLabel(i18n.get("main_panel.sync_tasks.sync_job_card.server_directory"), syncJob.getRemoteDirectoryRoot(), SyncJobCardLabel.DEFAULT_SMALL_LABEL_SIZE, desiredSyncJobCoordinatesWidth, mouseClickEvent);
        SyncJobCardLabel localDirectoryLabel = new SyncJobCardLabel(i18n.get("main_panel.sync_tasks.sync_job_card.local_directory"), syncJob.getLocalDirectoryRoot(), SyncJobCardLabel.DEFAULT_SMALL_LABEL_SIZE, desiredSyncJobCoordinatesWidth, mouseClickEvent);
        SyncJobCardLabel openBisServerUrlLabel = new SyncJobCardLabel(i18n.get("main_panel.sync_tasks.sync_job_card.open_bis_url"), syncJob.getOpenBisUrl(), SyncJobCardLabel.DEFAULT_SMALL_LABEL_SIZE, desiredSyncJobCoordinatesWidth, mouseClickEvent);
        sessionValidUntilLabel = new SyncJobCardLabel("Session valid until", "-", SyncJobCardLabel.DEFAULT_SMALL_LABEL_SIZE, desiredSyncJobCoordinatesWidth, mouseClickEvent);
        syncJobCoordinates.getChildren().addAll(entityPermIdLabel, openBisServerUrlLabel, serverDirectoryLabel, localDirectoryLabel, sessionValidUntilLabel);
        labelPane.getChildren().add(syncJobCoordinates);

        syncJobAttributes = new VBox();
        syncJobAttributes.setMinWidth(SYNC_JOB_ATTRIBUTE_WIDTH);
        syncJobAttributes.setMaxWidth(SYNC_JOB_ATTRIBUTE_WIDTH);
        syncJobAttributes.setPrefWidth(SYNC_JOB_ATTRIBUTE_WIDTH);
        syncJobAttributes.setMaxHeight(Double.MAX_VALUE);
        syncJobAttributes.setAlignment(Pos.CENTER);
        SyncJobCardLabel modeLabel = new SyncJobCardLabel(i18n.get("main_panel.sync_tasks.sync_job_card.mode"),
                i18n.get( switch (syncJob.getType()) {
                    case Bidirectional -> "main_panel.sync_tasks.sync_job_card.mode.bidirectional";
                    case Upload -> "main_panel.sync_tasks.sync_job_card.mode.upload";
                    case Download -> "main_panel.sync_tasks.sync_job_card.mode.download";
                }),
                SyncJobCardLabel.DEFAULT_SMALL_LABEL_SIZE, syncJobAttributes.widthProperty().map(Number::doubleValue), 55, mouseClickEvent, null);
        SyncJobCardLabel stateLabel = new SyncJobCardLabel(i18n.get("main_panel.sync_tasks.sync_job_card.state"),
                i18n.get(syncJob.isEnabled() ? "main_panel.sync_tasks.sync_job_card.state.enabled" : "main_panel.sync_tasks.sync_job_card.state.disabled"),
                SyncJobCardLabel.DEFAULT_SMALL_LABEL_SIZE,  syncJobAttributes.widthProperty().map(Number::doubleValue),55, mouseClickEvent,
                syncJob.isEnabled() ? Color.GREEN : Color.RED);

        VBox liveStatusBox = new VBox();
        liveStatusBox.setSpacing(10);
        this.progressFraction = new Label();
        this.animatedProgressBar = new AnimatedProgressBar(150);
        ProgressBar progressBar = this.animatedProgressBar.getProgressBar();
        this.liveStatus = new Label();
        this.progressBarWithFraction = new AnchorPane();
        AnchorPane.setTopAnchor(progressBar,0.0);
        AnchorPane.setLeftAnchor(progressBar,0.0);
        AnchorPane.setTopAnchor(this.progressFraction,0.0);
        AnchorPane.setLeftAnchor(this.progressFraction,10.0);
        progressBarWithFraction.getChildren().addAll(progressBar, this.progressFraction);
        liveStatusBox.getChildren().addAll(this.liveStatus, progressBarWithFraction);

        syncJobAttributes.getChildren().addAll(modeLabel, stateLabel);

        AnchorPane.setRightAnchor(syncJobAttributes, 0.0);
        AnchorPane.setTopAnchor(syncJobAttributes, 0.0);
        labelPane.getChildren().add(syncJobAttributes);

        AnchorPane.setBottomAnchor(liveStatusBox, 18.0);
        AnchorPane.setRightAnchor(liveStatusBox, 22.0);
        labelPane.getChildren().add(liveStatusBox);

        dataBox.getChildren().add(labelPane);

        AnchorPane.setLeftAnchor(hBoxContainer, 30.0);
        this.getChildren().add(hBoxContainer);

        this.setOnMouseClicked(mouseClickEvent);

        syncJobCardLabels = List.of(serverDirectoryLabel, entityPermIdLabel, localDirectoryLabel, openBisServerUrlLabel, modeLabel, stateLabel);

        setNotRunning();
        resize();
    }

    public BooleanProperty getSelectedProperty() {
        return radioButton.selectedProperty();
    }

    @SuppressWarnings("lombok")
    public SyncJob getSyncJob() {
        return syncJob;
    }

    @Override
    protected void resize() {
        this.setPrefWidth( parent.getWidth() > 0 ? parent.getWidth() : parent.getPrefWidth() );
        this.hBoxContainer.setPrefWidth(Math.min(parent.getWidth(), this.getMaxWidth()) - 50);
        this.hBoxContainer.setMinWidth(Math.min(parent.getWidth(), this.getMaxWidth()) - 50);
        this.syncJobCoordinates.setPrefWidth(Math.min(parent.getWidth(), this.getMaxWidth()) - SYNC_JOB_ATTRIBUTE_WIDTH - 150);
        this.syncJobCoordinates.setMaxWidth(Math.min(parent.getWidth(), this.getMaxWidth()) - SYNC_JOB_ATTRIBUTE_WIDTH - 150);
    }

    public void setScanning(boolean remote) {
        Platform.runLater( () -> {
            if (isErrorState()) { return; }
            removeLabelErrorStyle(liveStatus);
            this.liveStatus.setText(
                remote ?
                    i18n.get("main_panel.sync_tasks.sync_job_card.live_state.scanning_remote") :
                    i18n.get("main_panel.sync_tasks.sync_job_card.live_state.scanning_local")
            );
            this.progressBarWithFraction.getStyleClass().removeIf( cls -> cls.equals(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS) );
            this.animatedProgressBar.getProgressBar().setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            this.progressFraction.setText("");
        });
    }

    public void setUploading(long total, long current) {
        double progress;
        if (total != 0) {
            progress = Double.max(0, Double.min(1, (double) current / total));
        } else {
            progress = 1;
        }
        Platform.runLater( () -> {
            if (isErrorState()) { return; }
            removeLabelErrorStyle(liveStatus);
            this.liveStatus.setText(i18n.get("main_panel.sync_tasks.sync_job_card.live_state.upload"));
            this.progressBarWithFraction.getStyleClass().removeIf( cls -> cls.equals(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS) );
            this.animatedProgressBar.getProgressBar().setProgress(progress);
            this.progressFraction.setText(String.format("%s / %s",
                    UsageUtil.getFileSizeWithUnitOfMeasurement(current),
                    UsageUtil.getFileSizeWithUnitOfMeasurement(total)));
        });
    }

    public void setDownloading(long total, long current) {
        double progress;
        if (total != 0) {
            progress = Double.max(0, Double.min(1, (double) current / total));
        } else {
            progress = 1;
        }
        Platform.runLater( () -> {
            if (isErrorState()) { return; }
            removeLabelErrorStyle(liveStatus);
            this.liveStatus.setText(i18n.get("main_panel.sync_tasks.sync_job_card.live_state.download"));
            this.progressBarWithFraction.getStyleClass().removeIf( cls -> cls.equals(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS) );
            this.animatedProgressBar.getProgressBar().setProgress(progress);
            this.progressFraction.setText(String.format("%s / %s",
                    UsageUtil.getFileSizeWithUnitOfMeasurement(current),
                    UsageUtil.getFileSizeWithUnitOfMeasurement(total)));
        });
    }

    public void setNotRunning() {
        Platform.runLater( () -> {
            if (isErrorState()) { return; }
            if ( !this.progressBarWithFraction.getStyleClass().contains(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS) ) {
                this.progressBarWithFraction.getStyleClass().add(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS);
            }
            if (!isWarningExpiringSessionState()) {
                removeLabelErrorStyle(liveStatus);
                if (syncJob.isEnabled()) {
                    this.liveStatus.setText(i18n.get("main_panel.sync_tasks.sync_job_card.live_state.not_running"));
                } else {
                    this.liveStatus.setText("");
                }
            } else {
                addLabelErrorStyle(liveStatus);
                this.liveStatus.setText(i18n.get("main_panel.sync_tasks.sync_job_card.warning_state.session_expires_in") + " " + Optional.ofNullable(patCheckResult).map(OpenBISQueryUtil.PATCheckResult::validUntil)
                        .map(SyncJobCard::formatExpiresInText).orElse("-"));
            }
        });
    }

    public void setSessionValidUntilLabel() {
        Optional<ZonedDateTime> sessionValidityEndDate = Optional.ofNullable(patCheckResult).map(OpenBISQueryUtil.PATCheckResult::validUntil)
                .map( date -> date.toInstant().atZone(ZoneId.systemDefault()));
        if (isWarningExpiringSessionState()) {
            this.getStyleClass().removeIf(DisplaySettings.SYNC_JOB_CARD_CLASS::equals);
            this.getStyleClass().add(DisplaySettings.SYNC_JOB_CARD_WARNING_CLASS);
        }
        sessionValidUntilLabel.setValue(sessionValidityEndDate.map(DATE_TIME_FORMATTER::format).orElse("-"));
    }

    boolean isErrorState() {
        return patCheckResult != null && patCheckResult.result() != OpenBISQueryUtil.PATCheckResultEnum.OK;
    }

    boolean isWarningExpiringSessionState() {
        return patCheckResult != null && patCheckResult.result() == OpenBISQueryUtil.PATCheckResultEnum.OK &&
                patCheckResult.validUntil() != null && patCheckResult.validUntil().toInstant().isBefore(
                    Instant.now().plus(warningMillisBeforExpiringSession, ChronoUnit.MILLIS)
                );
    }

    public void setErrorMessage(@NonNull OpenBISQueryUtil.PATCheckResult patCheckResult) {
        Platform.runLater( () -> {
            if ( !this.progressBarWithFraction.getStyleClass().contains(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS) ) {
                this.progressBarWithFraction.getStyleClass().add(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS);
            }
            addLabelErrorStyle(liveStatus);
            switch (patCheckResult.result()) {
                case INVALID_SESSION -> this.liveStatus.setText(i18n.get("main_panel.sync_tasks.sync_job_card.error_state.invalid_session"));
                case ERROR_REACHING_SERVER -> this.liveStatus.setText(i18n.get("main_panel.sync_tasks.sync_job_card.error_state.server_unreachable"));
                default -> this.liveStatus.setText(i18n.get("main_panel.sync_tasks.sync_job_card.error_state.unknown_error_checking_server_session"));
            }
        });
    }

    void addLabelErrorStyle(@NonNull Label label) {
        if (!label.getStyleClass().contains(DisplaySettings.SYNC_JOB_CARD_LIVE_STATUS_ERROR)) {
            label.getStyleClass().add(DisplaySettings.SYNC_JOB_CARD_LIVE_STATUS_ERROR);
        }
    }

    void removeLabelErrorStyle(@NonNull Label label) {
        label.getStyleClass().removeIf(DisplaySettings.SYNC_JOB_CARD_LIVE_STATUS_ERROR::equals);
    }

    static class AnimatedProgressBar {
        final SimpleObjectProperty<Node> node = new SimpleObjectProperty<>();

        // Wrapped and not simply extended, for difficulties with animated style otherwise
        final ProgressBar progressBar;

        final Timeline timeline;

        public AnimatedProgressBar(int minWidth) {
            this.progressBar = new ProgressBar(0) {
                @Override
                protected void layoutChildren() {
                    super.layoutChildren();
                    if (node.get() == null) {
                        Node n = lookup(".bar");
                        node.set(n);

                        progressProperty().addListener((obs, old, val) -> {
                            if (old.doubleValue() <= 0) {
                                timeline.playFromStart();
                            }
                        });
                    }
                }
            };
            this.progressBar.setMinWidth(minWidth);

            int stripWidth = 15;
            IntegerProperty x = new SimpleIntegerProperty(0);
            IntegerProperty y = new SimpleIntegerProperty(stripWidth);
            this.timeline = new Timeline(new KeyFrame(Duration.millis(150), e -> {
                Node n = node.get();
                if (n != null) {
                    x.set(x.get() + 1);
                    y.set(y.get() + 1);
                    String style = "-fx-background-color: linear-gradient(from " + x.get() + "px " + x.get() + "px to " + y.get() + "px " + y.get() + "px, repeat, derive(-fx-accent, 75%) 10%, derive(-fx-accent, 90%) 90%);";
                    n.setStyle(style);
                    if (x.get() >= stripWidth * 2) {
                        x.set(0);
                        y.set(stripWidth);
                    }
                }
            }));

            this.timeline.setCycleCount(Animation.INDEFINITE);
        }

        @SuppressWarnings("lombok")
        public ProgressBar getProgressBar() {
            return progressBar;
        }

        public void close() {
            timeline.stop();
        }
    }

    @Override
    public void close() throws Exception {
        this.animatedProgressBar.close();
        super.close();
        for (SyncJobCardLabel syncJobCardLabel : syncJobCardLabels) {
            syncJobCardLabel.close();
        }
    }

    public static String formatExpiresInText(@NonNull Date date) {
        long numberOfHours = Long.max(date.getTime() - System.currentTimeMillis(), 0L) / 3_600_000L;
        long numberOfDays = numberOfHours / 24;
        if (numberOfDays > 0) {
            return numberOfDays + " " + SharedContext.getContext().getI18n().get("generic_messages.time_durations.expires_in.days");
        } else {
            return numberOfHours + " " + SharedContext.getContext().getI18n().get("generic_messages.time_durations.expires_in.hours");
        }
    }
}
