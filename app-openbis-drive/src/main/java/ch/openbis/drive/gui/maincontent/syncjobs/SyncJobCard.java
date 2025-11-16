package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.maincontent.ResizablePanel;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.model.SyncJob;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.RadioButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.NonNull;

import java.util.List;

public class SyncJobCard extends ResizablePanel implements AutoCloseable {
    public static final int SYNC_JOB_ATTRIBUTE_WIDTH = 165;
    private final SyncJob syncJob;
    private final HBox hBoxContainer;
    private final RadioButton radioButton;
    private final VBox syncJobCoordinates;
    private final VBox syncJobAttributes;

    private final List<SyncJobCardLabel> syncJobCardLabels;

    public SyncJobCard(@NonNull SyncJob syncJob, @NonNull Pane parent) {
        super(parent);
        this.syncJob = syncJob;
        this.radioButton = new RadioButton();

        I18n i18n = SharedContext.getContext().getI18n();

        this.getStyleClass().add(DisplaySettings.SYNC_JOB_CARD_CLASS);
        this.setMaxSize(2 * DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH, DisplaySettings.SYNC_TASK_PANEL_JOB_CARD_HEIGHT);
        this.setMinWidth(DisplaySettings.DEFAULT_INITIAL_WINDOW_WIDTH - DisplaySettings.SIDE_MENU_WIDTH - 100);

        hBoxContainer = new HBox();
        hBoxContainer.setMaxWidth(this.getMaxWidth());
        hBoxContainer.setPrefHeight(this.getPrefHeight());
        hBoxContainer.setSpacing(40);
        hBoxContainer.setAlignment(Pos.CENTER_LEFT);
        hBoxContainer.getChildren().add(radioButton);

        syncJobCoordinates = new VBox();
        syncJobCoordinates.setMaxHeight(Double.MAX_VALUE);
        syncJobCoordinates.setPrefHeight(DisplaySettings.SYNC_TASK_PANEL_JOB_CARD_HEIGHT);
        syncJobCoordinates.setAlignment(Pos.CENTER);
        ObservableValue<Double> desiredSyncJobCoordinatesWidth = parent.widthProperty().map( (parentWidth) -> Math.min(parentWidth.doubleValue(), this.getMaxWidth()) - SYNC_JOB_ATTRIBUTE_WIDTH - 180);
        SyncJobCardLabel entityPermIdLabel = new SyncJobCardLabel(i18n.get("main_panel.sync_tasks.sync_job_card.entity_perm_id"), syncJob.getEntityPermId(), SyncJobCardLabel.DEFAULT_BIG_LABEL_SIZE, desiredSyncJobCoordinatesWidth);
        SyncJobCardLabel serverDirectoryLabel = new SyncJobCardLabel(i18n.get("main_panel.sync_tasks.sync_job_card.server_directory"), syncJob.getRemoteDirectoryRoot(), SyncJobCardLabel.DEFAULT_SMALL_LABEL_SIZE, desiredSyncJobCoordinatesWidth);
        SyncJobCardLabel localDirectoryLabel = new SyncJobCardLabel(i18n.get("main_panel.sync_tasks.sync_job_card.local_directory"), syncJob.getLocalDirectoryRoot(), SyncJobCardLabel.DEFAULT_SMALL_LABEL_SIZE, desiredSyncJobCoordinatesWidth);
        SyncJobCardLabel openBisServerUrlLabel = new SyncJobCardLabel(i18n.get("main_panel.sync_tasks.sync_job_card.open_bis_url"), syncJob.getOpenBisUrl(), SyncJobCardLabel.DEFAULT_SMALL_LABEL_SIZE, desiredSyncJobCoordinatesWidth);
        syncJobCoordinates.getChildren().addAll(entityPermIdLabel, serverDirectoryLabel, localDirectoryLabel, openBisServerUrlLabel);
        hBoxContainer.getChildren().add(syncJobCoordinates);

        syncJobAttributes = new VBox();
        syncJobAttributes.setMinWidth(SYNC_JOB_ATTRIBUTE_WIDTH);
        syncJobAttributes.setMaxWidth(SYNC_JOB_ATTRIBUTE_WIDTH);
        syncJobAttributes.setPrefWidth(SYNC_JOB_ATTRIBUTE_WIDTH);
        syncJobAttributes.setMaxHeight(Double.MAX_VALUE);
        syncJobAttributes.setPrefHeight(DisplaySettings.SYNC_TASK_PANEL_JOB_CARD_HEIGHT);
        syncJobAttributes.setAlignment(Pos.CENTER);
        SyncJobCardLabel modeLabel = new SyncJobCardLabel(i18n.get("main_panel.sync_tasks.sync_job_card.mode"),
                i18n.get( switch (syncJob.getType()) {
                    case Bidirectional -> "main_panel.sync_tasks.sync_job_card.mode.bidirectional";
                    case Upload -> "main_panel.sync_tasks.sync_job_card.mode.upload";
                    case Download -> "main_panel.sync_tasks.sync_job_card.mode.download";
                }),
                SyncJobCardLabel.DEFAULT_SMALL_LABEL_SIZE, syncJobAttributes.widthProperty().map(Number::doubleValue), 55);
        SyncJobCardLabel stateLabel = new SyncJobCardLabel(i18n.get("main_panel.sync_tasks.sync_job_card.state"),
                i18n.get(syncJob.isEnabled() ? "main_panel.sync_tasks.sync_job_card.state.enabled" : "main_panel.sync_tasks.sync_job_card.state.disabled"),
                SyncJobCardLabel.DEFAULT_SMALL_LABEL_SIZE,  syncJobAttributes.widthProperty().map(Number::doubleValue),55);
        syncJobAttributes.getChildren().addAll(modeLabel, stateLabel);

        AnchorPane.setLeftAnchor(hBoxContainer, 30.0);
        this.getChildren().add(hBoxContainer);
        AnchorPane.setRightAnchor(syncJobAttributes, 30.0);
        this.getChildren().add(syncJobAttributes);

        this.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                radioButton.selectedProperty().setValue(!radioButton.selectedProperty().getValue());
            }
        });

        syncJobCardLabels = List.of(serverDirectoryLabel, entityPermIdLabel, localDirectoryLabel, openBisServerUrlLabel, modeLabel, stateLabel);
        resize();
    }

    public BooleanProperty getSelectedProperty() {
        return radioButton.selectedProperty();
    }

    @SuppressWarnings("")
    public SyncJob getSyncJob() {
        return syncJob;
    }

    @Override
    protected void resize() {
        this.setPrefWidth( parent.getWidth() > 0 ? parent.getWidth() : parent.getPrefWidth() );
        this.hBoxContainer.setPrefWidth(Math.min(parent.getWidth(), this.getMaxWidth()) - SYNC_JOB_ATTRIBUTE_WIDTH);
        this.hBoxContainer.setMinWidth(Math.min(parent.getWidth(), this.getMaxWidth()) - SYNC_JOB_ATTRIBUTE_WIDTH);
        this.syncJobCoordinates.setPrefWidth(Math.min(parent.getWidth(), this.getMaxWidth()) - SYNC_JOB_ATTRIBUTE_WIDTH - 150);
        this.syncJobCoordinates.setMaxWidth(Math.min(parent.getWidth(), this.getMaxWidth()) - SYNC_JOB_ATTRIBUTE_WIDTH - 150);
    }

    @Override
    public void close() throws Exception {
        super.close();
        for (SyncJobCardLabel syncJobCardLabel : syncJobCardLabels) {
            syncJobCardLabel.close();
        }
    }
}
