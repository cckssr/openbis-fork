package ch.openbis.drive.gui.maincontent;

import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.maincontent.syncjobs.SyncJobCard;
import ch.openbis.drive.gui.maincontent.syncjobs.SyncJobDeleteDialog;
import ch.openbis.drive.gui.maincontent.syncjobs.SyncJobDialog;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.ErrorLabel;
import ch.openbis.drive.gui.util.ServiceCallHandler;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.model.SyncJobLive;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import lombok.NonNull;

import java.util.*;

import static ch.openbis.drive.gui.util.DisplaySettings.SYNC_JOB_CARD_SPACING;

public class SynchronizationTasksPanel extends ResizablePanel {
    private Logger logger = LogManager.getLogger(this.getClass());
    private final VBox mainVBox;
    private final Button addButton;
    private final HBox editButtonGroup;
    private final Button enableDisableButton;
    private final Button editButton;
    private final Button deleteButton;
    private final VBox syncTaskListContainer;

    private final Callback<Void, Void> refreshAll;

    private final ObjectProperty<List<SyncJob>> syncJobs = new SimpleObjectProperty<>(Collections.emptyList());
    private final ObjectProperty<List<SyncJobCard>> syncJobCards = new SimpleObjectProperty<>(Collections.emptyList());
    private final BooleanProperty isSomeSelected = new SimpleBooleanProperty(false);
    private final ScrollPane syncListScrollPane;

    private final Timeline timeline;

    public SynchronizationTasksPanel(@NonNull Pane parent, @NonNull Callback<Void, Void> refreshAll) {
        super(parent);
        this.refreshAll = refreshAll;

        I18n i18n = SharedContext.getContext().getI18n();

        mainVBox = new VBox();
        mainVBox.getStyleClass().add(DisplaySettings.MAIN_CONTENT_PADDED_FRAME_CLASS);
        mainVBox.setSpacing(30);
        this.getChildren().add(mainVBox);

        HBox topButtonRow = new HBox();

        addButton = getAddButton(i18n);

        editButtonGroup = new HBox();
        editButtonGroup.setSpacing(10);
        enableDisableButton = getEnableDisableButton(i18n);
        editButton = getEditButton(i18n);
        deleteButton = getDeleteButton(i18n);
        editButtonGroup.getChildren().addAll(enableDisableButton, editButton, deleteButton);
        editButtonGroup.setAlignment(Pos.CENTER_RIGHT);

        topButtonRow.getChildren().addAll(addButton, editButtonGroup);

        mainVBox.getChildren().add(topButtonRow);

        syncListScrollPane = new ScrollPane();
        syncListScrollPane.setStyle("-fx-background-color: transparent");
        syncTaskListContainer = new VBox();
        syncTaskListContainer.setSpacing(SYNC_JOB_CARD_SPACING);

        ServiceCallHandler.ServiceCallResult<List<SyncJob>> syncJobsResult = SharedContext.getContext().getServiceCallHandler(parent).getSyncJobs();

        if (syncJobsResult.isOk()) {
            syncJobs.setValue(syncJobsResult.getOk());

            syncJobCards.setValue(syncJobs.getValue().stream().sorted(
                    Comparator.comparing(SyncJob::getTitle)
            ).map(syncJob -> new SyncJobCard(syncJob, syncTaskListContainer)).toList());
            syncTaskListContainer.getChildren().addAll(syncJobCards.getValue());

            syncJobCards.getValue().forEach( syncJobCard -> {
                syncJobCard.getSelectedProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
                        if(newValue) {
                            isSomeSelected.setValue(true);
                            for(SyncJobCard other : syncJobCards.getValue()) {
                                if (other != syncJobCard) {
                                    other.getSelectedProperty().setValue(false);
                                }
                            }
                        } else {
                            isSomeSelected.setValue(syncJobCards.getValue().stream().anyMatch( it -> it.getSelectedProperty().getValue()));
                        }
                    }
                });
            });
            syncListScrollPane.setContent(syncTaskListContainer);
            mainVBox.getChildren().add(syncListScrollPane);
        } else {
            ErrorLabel errorLabel = new ErrorLabel();
            mainVBox.getChildren().add(errorLabel);
        }

        timeline = getSyncJobsLiveTimeline();

        resize();
    }

    private Button getDeleteButton(I18n i18n) {
        final Button deleteButton;
        deleteButton = new Button();
        //deleteButton.getStyleClass().add("red-button");
        deleteButton.textProperty().bind(i18n.createStringBinding("main_panel.sync_tasks.delete_button"));
        deleteButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        deleteButton.setMinSize(DisplaySettings.SYNC_TASK_PANEL_TOP_BUTTON_WIDTH, DisplaySettings.SYNC_TASK_PANEL_TOP_BUTTON_HEIGHT);
        deleteButton.setDisable(true);
        isSomeSelected.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
                deleteButton.setDisable(!newValue);
            }
        });
        deleteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                doRemoveSelectedSyncJobAction();
            }
        });
        return deleteButton;
    }

    private Button getEditButton(I18n i18n) {
        final Button editButton;
        editButton = new Button();
        editButton.textProperty().bind(i18n.createStringBinding("main_panel.sync_tasks.edit_button"));
        editButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        editButton.setMinSize(DisplaySettings.SYNC_TASK_PANEL_TOP_BUTTON_WIDTH, DisplaySettings.SYNC_TASK_PANEL_TOP_BUTTON_HEIGHT);
        editButton.setDisable(true);
        isSomeSelected.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
                editButton.setDisable(!newValue);
            }
        });
        editButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                openEditDialogForSelectedSyncJob();
            }
        });
        return editButton;
    }

    private Button getEnableDisableButton(I18n i18n) {
        final Button enableDisableButton;
        enableDisableButton = new Button();
        enableDisableButton.textProperty().bind(i18n.createStringBinding("main_panel.sync_tasks.enable_disable_button"));
        enableDisableButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        enableDisableButton.setMinSize(DisplaySettings.SYNC_TASK_PANEL_TOP_BUTTON_WIDTH, DisplaySettings.SYNC_TASK_PANEL_TOP_BUTTON_HEIGHT);
        enableDisableButton.setDisable(true);
        isSomeSelected.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
                enableDisableButton.setDisable(!newValue);
            }
        });
        enableDisableButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                enableOrDisableSelectedSyncJob();
            }
        });
        return enableDisableButton;
    }

    private Button getAddButton(I18n i18n) {
        final Button addButton;
        addButton = new Button();
        addButton.setDefaultButton(true);
        addButton.textProperty().bind(i18n.createStringBinding("main_panel.sync_tasks.add_button"));
        addButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        addButton.setMinSize(DisplaySettings.SYNC_TASK_PANEL_TOP_BUTTON_WIDTH, 40);
        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                openCreationDialogForNewSyncJob();
            }
        });
        return addButton;
    }

    private Optional<SyncJob> getSelectedSyncJob() {
        return syncJobCards.getValue().stream()
                .filter( syncJobCard -> Boolean.TRUE.equals(syncJobCard.getSelectedProperty().getValue()))
                .map(SyncJobCard::getSyncJob)
                .findFirst();
    }

    private void enableOrDisableSelectedSyncJob() {
        Optional<SyncJob> selectedSyncJob = getSelectedSyncJob();
        if (selectedSyncJob.isPresent()) {
            SyncJob syncJob = selectedSyncJob.get();
            if (!syncJob.isEnabled()) {
                SharedContext.getContext().getServiceCallHandler(parent).enableSyncJob(syncJob);
            } else {
                SharedContext.getContext().getServiceCallHandler(parent).disableSyncJob(syncJob);
            }
        }
        refreshAll();
    }

    private void openEditDialogForSelectedSyncJob() {
        Optional<SyncJob> selectedSyncJob = getSelectedSyncJob();
        if (selectedSyncJob.isPresent()) {
            SyncJob syncJob = selectedSyncJob.get();
            SyncJobDialog syncJobDialog = new SyncJobDialog(syncJob, (Stage) this.getScene().getWindow(), syncJobs.getValue());
            syncJobDialog.setResizable(true);
            Optional<SyncJob> newSyncJob = syncJobDialog.showAndWait();
            if (newSyncJob.isPresent()) {
                ServiceCallHandler serviceCallHandler = SharedContext.getContext().getServiceCallHandler(parent);
                ServiceCallHandler.ServiceCallResult<Settings> settings = serviceCallHandler.getSettings();
                if (settings.isOk()) {
                    Settings toBeUpdated = settings.getOk();
                    if (toBeUpdated.getJobs().remove(syncJob)) {
                        toBeUpdated.getJobs().add(newSyncJob.get());
                        serviceCallHandler.setSettings(toBeUpdated);
                    }
                }
            }
        }
        refreshAll();
    }

    private void openCreationDialogForNewSyncJob() {
        SyncJobDialog syncJobDialog = new SyncJobDialog(null, (Stage) this.getScene().getWindow(), syncJobs.getValue());
        syncJobDialog.setResizable(true);
        Optional<SyncJob> newSyncJob = syncJobDialog.showAndWait();
        if (newSyncJob.isPresent()) {
            ServiceCallHandler serviceCallHandler = SharedContext.getContext().getServiceCallHandler(parent);
            serviceCallHandler.addSyncJob(newSyncJob.get());
        }
        refreshAll();
    }

    private void doRemoveSelectedSyncJobAction() {
        Optional<SyncJob> selectedSyncJob = getSelectedSyncJob();
        if (selectedSyncJob.isPresent()) {
            SyncJob syncJob = selectedSyncJob.get();
            SyncJobDeleteDialog syncJobDeleteDialog = new SyncJobDeleteDialog((Stage) this.getScene().getWindow());
            Optional<Boolean> deletionConfirmation = syncJobDeleteDialog.showAndWait();
            if(deletionConfirmation.orElse(false)) {
                SharedContext.getContext().getServiceCallHandler(parent).removeSyncJob(syncJob);
            }
        }
        refreshAll();
    }

    private void refreshAll() {
        this.refreshAll.call(null);
    }

    @Override
    protected synchronized void resize() {
        mainVBox.setMinSize(parent.getWidth(), parent.getHeight());
        mainVBox.setMaxSize(parent.getWidth(), parent.getHeight());
        mainVBox.setPrefSize(parent.getWidth(), parent.getHeight());

        editButtonGroup.setMinWidth(DisplaySettings.SYNC_TASK_PANEL_TOP_BUTTON_WIDTH * 3 + 80);
        editButtonGroup.setMaxWidth(parent.getWidth() - mainVBox.getPadding().getLeft() - addButton.getWidth());
        editButtonGroup.setPrefWidth(parent.getWidth() - mainVBox.getPadding().getLeft() - addButton.getWidth());

        syncTaskListContainer.setMaxSize(parent.getWidth() - 82, (syncJobCards.getValue().size()) * (DisplaySettings.SYNC_TASK_PANEL_JOB_CARD_HEIGHT + SYNC_JOB_CARD_SPACING));
        syncTaskListContainer.setPrefSize(parent.getWidth() - 82, (syncJobCards.getValue().size()) * (DisplaySettings.SYNC_TASK_PANEL_JOB_CARD_HEIGHT + SYNC_JOB_CARD_SPACING));
        syncListScrollPane.setPrefHeight(parent.getHeight());
    }

    private Timeline getSyncJobsLiveTimeline() {
        final Timeline timeline;
        Node node = this;
        timeline = new Timeline(new KeyFrame(Duration.seconds(1.5), (e) -> {
            ServiceCallHandler.ServiceCallResult<List<SyncJobLive>> syncJobsLiveResult = SharedContext.getContext().getServiceCallHandler(node).getSyncJobsLive();
            if ( syncJobsLiveResult.isOk() ) {
                Map<String, SyncJobLive> jobsLive = new HashMap<>();
                syncJobsLiveResult.getOk().forEach( syncJobLive -> jobsLive.put(syncJobLive.getLocalDirectory(), syncJobLive));

                for (SyncJobCard syncJobCard : syncJobCards.getValue()) {
                    SyncJobLive syncJobLive = jobsLive.get(syncJobCard.getSyncJob().getLocalDirectoryRoot());
                    if (syncJobLive != null) {
                        if (syncJobLive.isUploading()) {
                            if (syncJobLive.getTotalUpload() == 0) {
                                syncJobCard.setScanning(false);
                            } else {
                                syncJobCard.setUploading(syncJobLive.getTotalUpload(), syncJobLive.getCurrentUpload());
                            }
                        } else if (syncJobLive.isDownloading()) {
                            if (syncJobLive.getTotalDownload() == 0) {
                                syncJobCard.setScanning(true);
                            } else {
                                syncJobCard.setDownloading(syncJobLive.getTotalDownload(), syncJobLive.getCurrentDownload());
                            }
                        } else {
                            syncJobCard.setNotRunning();
                        }
                    } else {
                        syncJobCard.setNotRunning();
                    }
                }
            } else {
                logger.catching(syncJobsLiveResult.getErr());
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        return timeline;
    }

    @Override
    public void close() throws Exception {
        this.timeline.stop();
        super.close();
        List<SyncJobCard> syncJobCardList = syncJobCards.getValue();
        if(syncJobCardList != null) {
            syncJobCardList.forEach(syncJobCard -> {
                try {
                    syncJobCard.close();
                } catch (Exception e) {
                    logger.catching(e);
                }
            });
        }
    }
}
