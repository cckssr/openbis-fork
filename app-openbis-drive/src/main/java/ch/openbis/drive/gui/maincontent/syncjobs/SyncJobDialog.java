package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntity;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.*;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.util.GlobUtil;
import ch.openbis.drive.util.OpenBISQueryUtil;
import impl.org.controlsfx.skin.AutoCompletePopup;
import impl.org.controlsfx.skin.AutoCompletePopupSkin;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.stage.*;
import javafx.util.Callback;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import org.controlsfx.control.textfield.CustomTextField;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ch.ethz.sis.afsclient.client.AfsClientUploadHelper.toServerPathString;

public class SyncJobDialog extends Dialog<SyncJob> {
    final int MAX_TEXT_INPUT_LENGTH = 300;
    Pattern HTTP_URL_PATTERN = Pattern.compile("^(http|https)://[^\\s/$.?#][^/]*$");
    final static String SUGGESTED_REMOTE_DIRECTORY = "/";
    final static String LINE_SEPARATOR = System.lineSeparator();
    final OpenBISQueryUtil.SearchUnit searchUnit;

    final SyncJob editedSyncJob;
    final List<SyncJob> currentSyncJobs;

    final TextField openbisServerUrlValue;
    final TextField titleValue;
    final TextField openbisEntityIdValue;
    final ProgressIndicator openbisEntityIdValueProgressIndicator = new ProgressIndicator();
    final AutoCompletePopup<EntitySuggestion> openbisEntityIdAutocompletion;
    final TextField openbisServerDirectoryValue;
    final TextField personalAccessTokenValue;
    final TextField localDirectoryValue;
    final ObjectProperty<SyncJob.Type> selectedSyncJobType = new SimpleObjectProperty<>(SyncJob.Type.Download);
    final CheckBox enabledCheckBox;
    final ObjectProperty<SyncJob.IgnoredFilesMode> selectedIgnoredFilesMode = new SimpleObjectProperty<>(SyncJob.IgnoredFilesMode.GlobalDefault);
    final TextArea ignoredPathPatterns;
    final TextArea globalDefaultPathPatternsTextArea;

    final BooleanProperty openbisUrlPropertyError = new SimpleBooleanProperty(false);
    final BooleanProperty titlePropertyError = new SimpleBooleanProperty(false);
    final BooleanProperty entityIdPropertyError = new SimpleBooleanProperty(false);
    final BooleanProperty remoteDirectoryPropertyError = new SimpleBooleanProperty(false);
    final BooleanProperty personalAccessTokenPropertyError = new SimpleBooleanProperty(false);
    final BooleanProperty localDirectoryPropertyError = new SimpleBooleanProperty(false);
    final BooleanProperty ignoredPathPatternPropertyError = new SimpleBooleanProperty(false);
    final List<BooleanProperty> validationErrors = List.of(
            openbisUrlPropertyError, titlePropertyError, entityIdPropertyError, remoteDirectoryPropertyError, personalAccessTokenPropertyError, localDirectoryPropertyError, ignoredPathPatternPropertyError);
    final BooleanBinding allValid = Bindings.createBooleanBinding(
            () -> validationErrors.stream().noneMatch(BooleanProperty::getValue), validationErrors.toArray(BooleanProperty[]::new));

    final ObjectProperty<ChosenEntity> entityChosen = new SimpleObjectProperty<>(null);
    final static int EXTENDED_HEIGHT = 750;
    final static int EXTENDED_WIDTH = 900;

    public SyncJobDialog(@Nullable SyncJob toBeModified, Stage mainStage, List<SyncJob> currentSyncJobs) {
        super();
        this.editedSyncJob = toBeModified;
        this.currentSyncJobs = currentSyncJobs;
        if (this.editedSyncJob != null) {
            entityChosen.setValue(new ChosenEntity(
                    this.editedSyncJob.getEntityPermId(),
                    this.editedSyncJob.getEntityType(),
                    this.editedSyncJob.isEntityImmutable()
            ));
        }

        I18n i18n = SharedContext.getContext().getI18n();
        initStyle(StageStyle.DECORATED);

        final Window window = getDialogPane().getScene().getWindow();
        window.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                DisplaySettings.centerStageOnMainStage((Stage) window, mainStage);
            }
        });
        Style.applyStyle(getDialogPane().getScene());

        VBox content = new VBox();
        content.getStyleClass().add(DisplaySettings.MAIN_CONTENT_PADDED_FRAME_CLASS);
        content.setSpacing(30);

        Label description = new Label();
        description.textProperty().bind(i18n.createStringBinding(
                isEditDialog() ? "sync_tasks.modal_panel.edit_sync_task_description" :
                        "sync_tasks.modal_panel.add_new_sync_task_description"
        ));

        VBox headerBox = new VBox();
        HBox textParametersBox = new HBox();
        textParametersBox.setSpacing(80);
        VBox leftTextParametersBox = new VBox();
        VBox rightTextParametersBox = new VBox();

        headerBox.getChildren().add(description);

        Label titleLabel = new Label();
        titleLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.title"));
        titleValue = getTitleTextField();

        Label openbisServerUrlLabel = new Label();
        openbisServerUrlLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.openbis_server_url"));
        openbisServerUrlLabel.setPadding(new Insets(30, 0, 0, 0));
        openbisServerUrlValue = getOpenbisServerUrlTextField();

        Label personalAccessTokenLabel = new Label();
        personalAccessTokenLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.personal_access_token"));
        personalAccessTokenLabel.setPadding(new Insets(30, 0, 0, 0));
        personalAccessTokenValue = getPersonalAccessTokenTextField();

        HBox openbisEntityIdLabelBox = new HBox();
        openbisEntityIdLabelBox.setSpacing(6);
        openbisEntityIdLabelBox.setAlignment(Pos.CENTER_LEFT);
        Label openbisEntityIdLabel = new Label();
        openbisEntityIdLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.openbis_entity_id"));
        Button openbisEntityIdHelpButton = getInputTooltipHelpButton(i18n.get("sync_tasks.modal_panel.sync_task_modal.entity_id_help_tooltip"));
        openbisEntityIdLabelBox.getChildren().addAll(openbisEntityIdLabel, openbisEntityIdHelpButton);
        openbisEntityIdAutocompletion = getOpenbisEntityIdAutocompletion();
        openbisEntityIdValue = getEntityIdTextField();

        Label openbisServerDirectoryLabel = new Label();
        openbisServerDirectoryLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.server_directory"));
        openbisServerDirectoryLabel.setPadding(new Insets(30, 0, 0, 0));
        openbisServerDirectoryValue = getRemoteDirectoryTextField();

        Label localDirectoryLabel = new Label();
        localDirectoryLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.local_directory"));
        localDirectoryLabel.setPadding(new Insets(30, 0, 0, 0));
        localDirectoryValue = getLocalDirectoryTextField();

        leftTextParametersBox.getChildren().addAll(
                titleLabel, titleValue,
                openbisServerUrlLabel, openbisServerUrlValue,
                localDirectoryLabel, localDirectoryValue);

        rightTextParametersBox.getChildren().addAll(
                openbisEntityIdLabelBox, openbisEntityIdValue,
                openbisServerDirectoryLabel, openbisServerDirectoryValue,
                personalAccessTokenLabel, personalAccessTokenValue);

        textParametersBox.getChildren().addAll(leftTextParametersBox, rightTextParametersBox);

        HBox syncModeChoiceBox = getSyncModeChoice(i18n);

        enabledCheckBox = getEnableCheckBox(i18n);

        content.getChildren().add(headerBox);
        content.getChildren().add(textParametersBox);
        content.getChildren().add(syncModeChoiceBox);
        content.getChildren().add(enabledCheckBox);

        Accordion accordion = new Accordion();
        ignoredPathPatterns = getIgnoredPathPatternsTextArea();
        TextArea emptyPathPatternsTextArea = getEmptyPathPatternsTextArea();
        globalDefaultPathPatternsTextArea = getGlobalDefaultPathPatternsTextArea();
        VBox ignoredFilesBox = getIgnoredFilesBox(i18n, emptyPathPatternsTextArea);
        TitledPane ignoredFilesPane = new TitledPane(i18n.get("generic_messages.advanced_settings"), ignoredFilesBox);
        accordion.getPanes().addAll(ignoredFilesPane);

        content.getChildren().add(accordion);

        getDialogPane().setContent(content);

        getDialogPane().getButtonTypes().add(ButtonType.APPLY);
        Button applyButton = (Button) getDialogPane().lookupButton(ButtonType.APPLY);
        applyButton.textProperty().bind(i18n.createStringBinding("generic_buttons.apply"));
        allValid.addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                applyButton.setDisable(false);
            } else {
                applyButton.setDisable(true);
            }
        });

        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        ((Button) getDialogPane().lookupButton(ButtonType.CANCEL)).textProperty().bind(i18n.createStringBinding("generic_buttons.cancel"));

        //Validation
        applyButton.addEventFilter(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                doValidationOnAllInputFields();
                if (validationErrors.stream().anyMatch(BooleanExpression::getValue)) {
                    applyButton.setDisable(true);
                    actionEvent.consume();
                }
            }
        });
        setResultConverter((dialogButton) -> {
            if (dialogButton.getButtonData().getTypeCode().equals(ButtonType.APPLY.getButtonData().getTypeCode())) {
                SyncJob newSyncJob = new SyncJob();
                newSyncJob.setTitle(titleValue.getText());
                newSyncJob.setEnabled(enabledCheckBox.isSelected());
                newSyncJob.setOpenBisUrl(openbisServerUrlValue.getText());
                newSyncJob.setEntityPermId(openbisEntityIdValue.getText());
                if (entityChosen.getValue() != null) {
                    newSyncJob.setEntityType(entityChosen.getValue().getEntityType());
                    newSyncJob.setEntityImmutable(entityChosen.getValue().isImmutable());
                }
                newSyncJob.setRemoteDirectoryRoot(toServerPathString(Path.of(openbisServerDirectoryValue.getText())));
                newSyncJob.setOpenBisPersonalAccessToken(personalAccessTokenValue.getText());
                newSyncJob.setLocalDirectoryRoot(localDirectoryValue.getText());
                newSyncJob.setType(selectedSyncJobType.get());
                newSyncJob.setIgnoreFiles(selectedIgnoredFilesMode.getValue());
                if (selectedIgnoredFilesMode.getValue() == SyncJob.IgnoredFilesMode.SpecificList) {
                    newSyncJob.setIgnoredPathPatterns(
                            new ArrayList<>(Arrays.stream(ignoredPathPatterns.getText().split("[\\r\\n]+"))
                                    .filter(str -> !str.isBlank())
                                    .map(String::trim)
                                    .toList())
                    );
                }
                return newSyncJob;
            } else {
                return null;
            }
        });

        Platform.runLater( () -> {
            getDialogPane().getScene().getWindow().setWidth(EXTENDED_WIDTH);
            getDialogPane().getScene().getWindow().setHeight(EXTENDED_HEIGHT);
        } );

        //Search unit supporting suggestions for openBIS entity-id
        searchUnit = getSearchUnit();

        setOnHidden(new EventHandler<DialogEvent>() {
            @Override
            @SneakyThrows
            public void handle(DialogEvent dialogEvent) {
                searchUnit.close();
            }
        });
    }

    OpenBISQueryUtil.SearchUnit getSearchUnit() {
        final OpenBISQueryUtil.SearchUnit searchUnit;
        searchUnit = new OpenBISQueryUtil.SearchUnit((result, ex) -> {
            if (ex == null) {
                openbisEntityIdAutocompletion.getSuggestions().clear();

                List<EntitySuggestion> resultsWithSectionTitles = new ArrayList<>();
                List<EntitySuggestion> samples = result.stream().filter( it -> it instanceof Sample).map(EntitySuggestion::newEntity).toList();
                List<EntitySuggestion> experiments = result.stream().filter( it -> it instanceof Experiment).map(EntitySuggestion::newEntity).toList();
                List<EntitySuggestion> dataSets = result.stream().filter( it -> it instanceof DataSet).map(EntitySuggestion::newEntity).toList();

                if (!samples.isEmpty()) {
                    resultsWithSectionTitles.add(EntitySuggestion.newTitle("Samples"));
                    resultsWithSectionTitles.addAll(samples);
                }
                if (!experiments.isEmpty()) {
                    resultsWithSectionTitles.add(EntitySuggestion.newTitle("Experiments"));
                    resultsWithSectionTitles.addAll(experiments);
                }
                if (!dataSets.isEmpty()) {
                    resultsWithSectionTitles.add(EntitySuggestion.newTitle("Datasets"));
                    resultsWithSectionTitles.addAll(dataSets);
                }

                openbisEntityIdAutocompletion.getSuggestions().addAll(resultsWithSectionTitles);
                Platform.runLater( () -> {
                    openbisEntityIdAutocompletion.setMinWidth(openbisEntityIdValue.getWidth());
                    openbisEntityIdAutocompletion.show(openbisEntityIdValue);
                });
            } else {
                ex.printStackTrace();

                openbisEntityIdAutocompletion.getSuggestions().clear();
                openbisEntityIdAutocompletion.getSuggestions().addAll(EntitySuggestion.newError(
                        SharedContext.getContext().getI18n().get("sync_tasks.modal_panel.sync_task_modal.error_retrieving_entity_suggestions")
                ));
                Platform.runLater(() -> {
                    openbisEntityIdAutocompletion.setMinWidth(openbisEntityIdValue.getWidth());
                    openbisEntityIdAutocompletion.show(openbisEntityIdValue);
                });
            }
            return null;
        }, openbisEntityIdValueProgressIndicator::setVisible);

        searchUnit.setOpenBISUrl(openbisServerUrlValue.getText());
        searchUnit.setPersonalAccessToken(personalAccessTokenValue.getText().trim());
        return searchUnit;
    }

    VBox getIgnoredFilesBox(I18n i18n, TextArea emptyPathPatternsTextArea) {
        VBox ignoredFilesBox = new VBox();
        ignoredFilesBox.setSpacing(20);
        HBox ignoredFilesModeChoiceBox = getIgnoredFilesModeChoice(i18n);
        ignoredFilesBox.getChildren().add(ignoredFilesModeChoiceBox);

        VBox specificIgnoredPathPatternBox = getIgnoredPathPatternsBox(ignoredPathPatterns, i18n);

        Consumer<SyncJob.IgnoredFilesMode> fillIgnoredFilesBox = (SyncJob.IgnoredFilesMode ignoredFilesMode) -> {
            if (ignoredFilesMode != null) {
                ignoredFilesBox.getChildren().removeIf( node ->
                        node.equals(globalDefaultPathPatternsTextArea) ||
                                node.equals(emptyPathPatternsTextArea) ||
                                node.equals(specificIgnoredPathPatternBox));
                switch (ignoredFilesMode) {
                    case GlobalDefault -> {
                        ignoredFilesBox.getChildren().add(globalDefaultPathPatternsTextArea);
                    }
                    case SpecificList -> {
                        ignoredFilesBox.getChildren().add(specificIgnoredPathPatternBox);
                    }
                    case None -> {
                        ignoredFilesBox.getChildren().add(emptyPathPatternsTextArea);
                    }
                }
            }
        };
        selectedIgnoredFilesMode.addListener( (obs, oldValue, newValue) -> {
            fillIgnoredFilesBox.accept(newValue);
        } );
        selectedIgnoredFilesMode.addListener( (obs, oldValue, newValue) -> {
            doValidationOnAllInputFields();
        } );
        fillIgnoredFilesBox.accept(selectedIgnoredFilesMode.getValue());
        return ignoredFilesBox;
    }

    private CheckBox getEnableCheckBox(I18n i18n) {
        CheckBox enabledCheckBox = new CheckBox();
        enabledCheckBox.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.enabled"));
        if(editedSyncJob != null) {
            enabledCheckBox.setSelected(editedSyncJob.isEnabled());
        } else {
            enabledCheckBox.setSelected(false);
        }
        enabledCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
            enabledCheckBox.setSelected(newValue);
        });
        if (isEditDialog()) {
            enabledCheckBox.setDisable(true);
        }
        return enabledCheckBox;
    }

    TextArea getIgnoredPathPatternsTextArea() {
        TextArea ignoredPathPatterns = new TextArea();
        ignoredPathPatterns.setPrefWidth(2000);
        ignoredPathPatterns.setEditable(false);
        ignoredPathPatterns.setStyle("-fx-text-fill: grey");
        if (editedSyncJob != null) {
            ignoredPathPatterns.setText(
                    String.join(LINE_SEPARATOR, editedSyncJob.getIgnoredPathPatterns())
            );
        } else {
            ignoredPathPatterns.setText(
                    String.join(LINE_SEPARATOR, new SyncJob().getIgnoredPathPatterns())
            );
        }
        ignoredPathPatterns.addEventHandler(MouseEvent.MOUSE_CLICKED, (event) -> {
            if (event.getClickCount() == 2) {
                ignoredPathPatterns.setEditable(true);
                ignoredPathPatterns.setStyle("-fx-text-fill: black");

            }
        });
        ignoredPathPatterns.focusedProperty().addListener( (obs, oldValue, newValue) -> {
            if( !newValue ) {
                ignoredPathPatterns.setEditable(false);
                ignoredPathPatterns.setText(new ArrayList<>(Arrays.stream(ignoredPathPatterns.getText().split("[\\r\\n]+"))
                        .filter(str -> !str.isBlank())
                        .map(String::trim)
                        .toList()).stream().collect(Collectors.joining(LINE_SEPARATOR)));
                ignoredPathPatterns.setStyle("-fx-text-fill: grey");
            }
        });
        addValidationLayerToTextInput(ignoredPathPatterns, textInput -> validateIgnoredPathPatterns(textInput.getText()), ignoredPathPatternPropertyError);
        return ignoredPathPatterns;
    }

    TextArea getEmptyPathPatternsTextArea() {
        TextArea noneIgnoredPathPatterns = new TextArea();
        noneIgnoredPathPatterns.setDisable(true);
        noneIgnoredPathPatterns.setStyle("-fx-text-fill: grey");
        return noneIgnoredPathPatterns;
    }

    TextArea getGlobalDefaultPathPatternsTextArea() {
        TextArea globalDefaultPathPatternsTextArea = new TextArea();
        globalDefaultPathPatternsTextArea.setEditable(false);
        globalDefaultPathPatternsTextArea.setStyle("-fx-text-fill: grey");

        ServiceCallHandler.ServiceCallResult<Settings> settings = SharedContext.getContext().getServiceCallHandler(getDialogPane()).getSettings();

        if (settings.isOk()) {
            globalDefaultPathPatternsTextArea.setText(
                    String.join(LINE_SEPARATOR, settings.getOk().getIgnoredPathPatterns())
            );
        } else {
            globalDefaultPathPatternsTextArea.setText(
                    SharedContext.getContext().getI18n().get("dialog.service_response_error")
            );
            addErrorClass(globalDefaultPathPatternsTextArea);
        }

        return globalDefaultPathPatternsTextArea;
    }

    VBox getIgnoredPathPatternsBox(@NonNull TextArea ignoredPathPatternsTextArea, I18n i18n) {
        VBox ignoredPathPatternBox = new VBox();
        ignoredPathPatternBox.setSpacing(5);
        ignoredPathPatternBox.getChildren().add(new Label(i18n.get("sync_tasks.modal_panel.sync_task_modal.ignored_path_patterns")));

        HBox ignoredPathPatternValuesBox = new HBox();
        ignoredPathPatternValuesBox.setSpacing(5);
        ignoredPathPatternValuesBox.getChildren().add(ignoredPathPatternsTextArea);

        Button restoreDefaultIgnoredPathsButton = new Button(i18n.get("sync_tasks.modal_panel.sync_task_modal.copy_global_default_list"));
        restoreDefaultIgnoredPathsButton.setMinWidth(250);
        restoreDefaultIgnoredPathsButton.setOnAction((event) -> {
            Platform.runLater( () -> {
                ignoredPathPatternsTextArea.setText(
                        globalDefaultPathPatternsTextArea.getText()
                );
                ignoredPathPatternsTextArea.commitValue();
            });
            Platform.runLater(this::doValidationOnAllInputFields);
        });
        ignoredPathPatternValuesBox.getChildren().add(restoreDefaultIgnoredPathsButton);

        ignoredPathPatternBox.getChildren().add(ignoredPathPatternValuesBox);
        return ignoredPathPatternBox;
    }

    private HBox getSyncModeChoice(I18n i18n) {
        HBox syncModeChoiceBox = new HBox();
        syncModeChoiceBox.setSpacing(50);

        Label syncModeChoiceLabel = new Label();
        syncModeChoiceLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.synchronization_mode"));

        ToggleGroup syncModeToggleGroup = new ToggleGroup();

        RadioButton downloadChoice = new RadioButton();
        downloadChoice.setToggleGroup(syncModeToggleGroup);
        downloadChoice.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                selectedSyncJobType.setValue(SyncJob.Type.Download);
            }
        });
        downloadChoice.setSelected(true);
        downloadChoice.textProperty().bind(i18n.createStringBinding("main_panel.sync_tasks.sync_job_card.mode.download"));

        RadioButton uploadChoice = new RadioButton();
        uploadChoice.setToggleGroup(syncModeToggleGroup);
        uploadChoice.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                selectedSyncJobType.setValue(SyncJob.Type.Upload);
            }
        });
        uploadChoice.textProperty().bind(i18n.createStringBinding("main_panel.sync_tasks.sync_job_card.mode.upload"));

        RadioButton bidirectionalChoice = new RadioButton();
        bidirectionalChoice.setToggleGroup(syncModeToggleGroup);
        bidirectionalChoice.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                selectedSyncJobType.setValue(SyncJob.Type.Bidirectional);
            }
        });
        bidirectionalChoice.textProperty().bind(i18n.createStringBinding("main_panel.sync_tasks.sync_job_card.mode.bidirectional"));

        selectedSyncJobType.addListener(((observableValue, oldValue, newValue) -> {
            if(oldValue != newValue && newValue != null) {
                switch (newValue) {
                    case Bidirectional -> bidirectionalChoice.setSelected(true);
                    case Upload -> uploadChoice.setSelected(true);
                    case Download -> downloadChoice.setSelected(true);
                }
            }
        }));
        if (editedSyncJob != null) {
            selectedSyncJobType.setValue(editedSyncJob.getType());

            if (editedSyncJob.isEntityImmutable()) {
                uploadChoice.setDisable(true);
                bidirectionalChoice.setDisable(true);
            }
        }

        entityChosen.addListener( (obs, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.isImmutable()) {
                    selectedSyncJobType.setValue(SyncJob.Type.Download);

                    uploadChoice.setDisable(true);
                    bidirectionalChoice.setDisable(true);
                }
            } else {
                uploadChoice.setDisable(false);
                bidirectionalChoice.setDisable(false);
            }
        });

        syncModeChoiceBox.getChildren().addAll(syncModeChoiceLabel, downloadChoice, uploadChoice, bidirectionalChoice);
        return syncModeChoiceBox;
    }

    TextField getLocalDirectoryTextField() {
        TextField localDirectoryValue = new TextField();
        localDirectoryValue.setPrefWidth(1200);
        localDirectoryValue.setEditable(false);
        DirectoryChooser directoryChooser = new DirectoryChooser();
        localDirectoryValue.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                File file = directoryChooser.showDialog(getDialogPane().getScene().getWindow());
                if (file != null) {
                    localDirectoryValue.setText(file.toPath().toAbsolutePath().normalize().toString());
                }
                doValidationOnTextInputNode(localDirectoryValue, (textInput) -> validateLocalDirectoryValue(textInput.getText()), localDirectoryPropertyError);
            }
        });
        if (editedSyncJob != null) {
            localDirectoryValue.setText(editedSyncJob.getLocalDirectoryRoot());
        }
        return localDirectoryValue;
    }

    private HBox getIgnoredFilesModeChoice(I18n i18n) {
        HBox ignoredFilesModeChoiceBox = new HBox();
        ignoredFilesModeChoiceBox.setSpacing(50);

        Label ignoredFilesModeLabel = new Label();
        ignoredFilesModeLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.ignored_files_mode"));

        ToggleGroup ignoredFilesModeToggleGroup = new ToggleGroup();

        RadioButton globalDefaultChoice = new RadioButton();
        globalDefaultChoice.setToggleGroup(ignoredFilesModeToggleGroup);
        globalDefaultChoice.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                selectedIgnoredFilesMode.setValue(SyncJob.IgnoredFilesMode.GlobalDefault);
            }
        });
        globalDefaultChoice.setSelected(true);
        globalDefaultChoice.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.ignored_files_mode.global_default"));

        RadioButton specificListChoice = new RadioButton();
        specificListChoice.setToggleGroup(ignoredFilesModeToggleGroup);
        specificListChoice.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                selectedIgnoredFilesMode.setValue(SyncJob.IgnoredFilesMode.SpecificList);
            }
        });
        specificListChoice.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.ignored_files_mode.specific_list"));

        RadioButton noneChoice = new RadioButton();
        noneChoice.setToggleGroup(ignoredFilesModeToggleGroup);
        noneChoice.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                selectedIgnoredFilesMode.setValue(SyncJob.IgnoredFilesMode.None);
            }
        });
        noneChoice.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.ignored_files_mode.none"));

        selectedIgnoredFilesMode.addListener(((observableValue, oldValue, newValue) -> {
            if(oldValue != newValue && newValue != null) {
                switch (newValue) {
                    case GlobalDefault -> globalDefaultChoice.setSelected(true);
                    case SpecificList -> specificListChoice.setSelected(true);
                    case None -> noneChoice.setSelected(true);
                }
            }
        }));
        if (editedSyncJob != null) {
            selectedIgnoredFilesMode.setValue(editedSyncJob.getIgnoreFiles());
        }

        ignoredFilesModeChoiceBox.getChildren().addAll(ignoredFilesModeLabel, globalDefaultChoice, specificListChoice, noneChoice);
        return ignoredFilesModeChoiceBox;
    }

    String[] validateLocalDirectoryValue(String localDirectoryInput) {
        if(localDirectoryInput == null || localDirectoryInput.isBlank()) {
            return new String[] { "error_tooltip.required_value" };
        } else {
            if(localDirectoryInput.length() > MAX_TEXT_INPUT_LENGTH) {
                return new String[] { "error_tooltip.too_long_text_input" };
            } else {
                ServiceCallHandler.ServiceCallResult<Settings> currentSettingsResult = SharedContext.getContext().getServiceCallHandler(getDialogPane()).getSettings();
                if (currentSettingsResult.isOk()) {
                    ArrayList<Path> localDirValues = currentSettingsResult.getOk().getJobs().stream()
                            .filter(item -> !item.equals(editedSyncJob))
                            .map( SyncJob::getLocalDirectoryRoot).map(Path::of).map(Path::toAbsolutePath).map(Path::normalize).collect(Collectors.toCollection(ArrayList::new));
                    localDirValues.add(Path.of(localDirectoryInput).toAbsolutePath().normalize());

                    for(int i=0; i<localDirValues.size(); i++) {
                        for(int j=i+1; j<localDirValues.size(); j++) {
                            if(localDirValues.get(i).startsWith(localDirValues.get(j)) || localDirValues.get(j).startsWith(localDirValues.get(i))) {
                                return new String[] { "error_tooltip.local_directory_already_in_use" };
                            }
                        }
                    }
                } else {
                    return new String[] { "error_tooltip.exception_in_validation" };
                }

                return null;
            }
        }
    }

    TextField getPersonalAccessTokenTextField() {
        TextField personalAccessTokenValue = new TextField();
        personalAccessTokenValue.setPrefWidth(1200);
        addValidationLayerToTextInput(personalAccessTokenValue, (textInput) -> validatePersonalAccessTokenValue(textInput.getText()), personalAccessTokenPropertyError);
        if (editedSyncJob != null) {
            personalAccessTokenValue.setText(editedSyncJob.getOpenBisPersonalAccessToken());
        } else {
            personalAccessTokenValue.setText(getMostRecentlyTouchedSyncJob().map( SyncJob::getOpenBisPersonalAccessToken ).orElse(""));
        }
        return personalAccessTokenValue;
    }

    String[] validatePersonalAccessTokenValue(String personalAccessTokenInput) {
        String[] error;
        if(personalAccessTokenInput == null || personalAccessTokenInput.isBlank()) {
            error = new String[] { "error_tooltip.required_value" };
        } else {
            if(personalAccessTokenInput.length() > MAX_TEXT_INPUT_LENGTH) {
                error = new String[] { "error_tooltip.too_long_text_input" };
            } else {
                error = null;
            }
        }

        if (error == null) {
            searchUnit.setPersonalAccessToken(personalAccessTokenInput.trim());
        } else {
            searchUnit.setPersonalAccessToken(null);
        }
        return error;
    }

    TextField getRemoteDirectoryTextField() {
        TextField openbisServerDirectoryValue = new TextField();
        openbisServerDirectoryValue.setPrefWidth(1200);
        addValidationLayerToTextInput(openbisServerDirectoryValue, (textInput) -> validateRemoteDirectoryValue(textInput.getText()), remoteDirectoryPropertyError);
        if (editedSyncJob != null) {
            openbisServerDirectoryValue.setText(editedSyncJob.getRemoteDirectoryRoot());
        } else {
            openbisServerDirectoryValue.setText(SUGGESTED_REMOTE_DIRECTORY);
        }
        return openbisServerDirectoryValue;
    }

    String[] validateRemoteDirectoryValue(String remoteDirectoryInput) {
        if(remoteDirectoryInput == null || remoteDirectoryInput.isBlank()) {
            return new String[] { "error_tooltip.required_value" };
        } else {
            if(remoteDirectoryInput.length() > MAX_TEXT_INPUT_LENGTH) {
                return new String[] { "error_tooltip.too_long_text_input" };
            } else {
                if(Path.of(remoteDirectoryInput).startsWith(File.separator)) {
                    return null;
                } else {
                    return new String[] { "error_tooltip.required_absolute_path" };
                }
            }
        }
    }

    TextField getTitleTextField() {
        TextField titleValue = new TextField();
        titleValue.setPrefWidth(1200);
        addValidationLayerToTextInput(titleValue, (textInput) -> validateTitleValue(textInput.getText()), titlePropertyError);
        if (editedSyncJob != null) {
            titleValue.setText(editedSyncJob.getTitle());
        }
        return titleValue;
    }

    AutoCompletePopup<EntitySuggestion> getOpenbisEntityIdAutocompletion() {
        AutoCompletePopup<EntitySuggestion> autoCompletePopup = new AutoCompletePopup<>();
        autoCompletePopup.setMinWidth(350);
        Callback<ListView<EntitySuggestion>, ListCell<EntitySuggestion>> cellFactory = new Callback<ListView<EntitySuggestion>, ListCell<EntitySuggestion>>() {
            @Override
            public ListCell<EntitySuggestion> call(ListView<EntitySuggestion> abstractEntityListView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(EntitySuggestion item, boolean empty) {
                        super.updateItem(item, empty);

                        Platform.runLater( () -> {
                            if (empty || item == null) {
                                setText(null);
                                setDisable(false);
                            } else {
                                // Detect your header item and disable it
                                if (item.getKind() == EntitySuggestion.Kind.TITLE) {
                                    setText(item.getTitle());
                                    setStyle("-fx-font-weight: bold;");
                                    setDisable(true);
                                } else if (item.getKind() == EntitySuggestion.Kind.ERROR) {
                                    setText(item.getError());
                                    setStyle("-fx-text-fill: red;");
                                    setDisable(true);
                                } else if (item.getKind() == EntitySuggestion.Kind.ENTITY) {
                                    setText(OpenBISQueryUtil.getDisplayName(item.getAbstractEntity()));
                                    setStyle("");
                                    setDisable(false);
                                } else {
                                    setText(null);
                                    setDisable(false);
                                }
                            }
                        });

                    }
                };
            }
        };
        autoCompletePopup.setSkin(new AutoCompletePopupSkin<>(autoCompletePopup, cellFactory));
        autoCompletePopup.setOnSuggestion((abstractEntitySuggestionEvent -> {
            EntitySuggestion entitySuggestion = abstractEntitySuggestionEvent.getSuggestion();

            if( entitySuggestion.getKind() == EntitySuggestion.Kind.ENTITY ) {
                AbstractEntity abstractEntity = entitySuggestion.getAbstractEntity();
                entityChosen.set(
                    new ChosenEntity(
                        OpenBISQueryUtil.getEntityPermId(abstractEntity),
                        toSyncJobEntityType(abstractEntity),
                        !OpenBISQueryUtil.isEntityDataMutable(abstractEntity)
                    )
                );
                openbisEntityIdValue.setText(OpenBISQueryUtil.getEntityPermId(abstractEntity));
                titleValue.setText(OpenBISQueryUtil.getDisplayName(abstractEntity));
                Platform.runLater(autoCompletePopup::hide);
                Platform.runLater(this::doValidationOnAllInputFields);
            }
        }));
        return autoCompletePopup;
    }

    TextField getEntityIdTextField() {
        CustomTextField openbisEntityIdValue = new CustomTextField();
        openbisEntityIdValue.setPrefWidth(1200);
        addValidationLayerToTextInput(openbisEntityIdValue, (textInput) -> validateEntityIdValue(textInput.getText()), entityIdPropertyError);
        if (editedSyncJob != null) {
            openbisEntityIdValue.setText(editedSyncJob.getEntityPermId());
        }
        openbisEntityIdValue.textProperty().addListener( (obs, oldValue, newValue) -> {
            if (newValue == null ||
                    (entityChosen.getValue() != null &&
                            !newValue.trim().equals(entityChosen.getValue().getEntityId()))) {
                entityChosen.setValue(null);
            }
            if ( entityChosen.getValue() == null && newValue != null ) {
                searchUnit.inputSearchText(newValue.trim());
            }
        });
        openbisEntityIdValue.focusedProperty().addListener( (obs, oldValue, newValue) -> {
            if (Boolean.FALSE.equals(oldValue) && Boolean.TRUE.equals(newValue)) {
                searchUnit.inputSearchText(openbisEntityIdValue.getText().trim());
            }
        });

        openbisEntityIdValueProgressIndicator.setVisible(false);
        openbisEntityIdValueProgressIndicator.setMaxSize(15, 15);
        openbisEntityIdValue.setRight(openbisEntityIdValueProgressIndicator);

        return openbisEntityIdValue;
    }

    String[] validateTitleValue(String titleValue) {
        if(titleValue == null || titleValue.isBlank()) {
            return new String[] { "error_tooltip.required_value" };
        } else {
            if(titleValue.length() > MAX_TEXT_INPUT_LENGTH) {
                return new String[] { "error_tooltip.too_long_text_input" };
            } else {
                return null;
            }
        }
    }

    String[] validateEntityIdValue(String entityIdInput) {
        if(entityIdInput == null || entityIdInput.isBlank()) {
            return new String[] { "error_tooltip.required_value" };
        } else if(entityIdInput.length() > MAX_TEXT_INPUT_LENGTH) {
            return new String[] { "error_tooltip.too_long_text_input" };
        } else {
            String acceptedEntityId = ( entityChosen.getValue() != null ) ? entityChosen.getValue().getEntityId() : null;

            if (acceptedEntityId == null || !acceptedEntityId.equals(entityIdInput.trim())) {
                return new String[] { "error_tooltip.entity_id_non_from_server_suggestions" };
            } else {
                return null;
            }
        }
    }

    TextField getOpenbisServerUrlTextField() {
        TextField openbisServerUrlValue = new TextField();
        openbisServerUrlValue.setPrefWidth(1200);
        addValidationLayerToTextInput(openbisServerUrlValue, (textInput) -> validateOpenbisServerUrlValue(openbisServerUrlValue.getText()), openbisUrlPropertyError);
        if (editedSyncJob != null) {
            openbisServerUrlValue.setText(editedSyncJob.getOpenBisUrl());
        } else {
            openbisServerUrlValue.setText(getMostRecentlyTouchedSyncJob().map( SyncJob::getOpenBisUrl ).orElse(""));
        }
        openbisServerUrlValue.textProperty().addListener( (obs, oldValue, newValue) -> {
            entityChosen.setValue(null);
        });
        openbisServerUrlValue.setPromptText("http(s)://myopenbis.com");
        return openbisServerUrlValue;
    }

    String[] validateOpenbisServerUrlValue(String serverUrlInput) {
        String[] error;
        if(serverUrlInput == null || serverUrlInput.isBlank()) {
            error = new String[] { "error_tooltip.required_value" };
        } else {
            if(serverUrlInput.length() > MAX_TEXT_INPUT_LENGTH) {
                error = new String[] { "error_tooltip.too_long_text_input" };
            } else {
                if(HTTP_URL_PATTERN.asMatchPredicate().test(serverUrlInput)) {
                    //No error
                    error = null;
                } else {
                    error = new String[] { "error_tooltip.required_http_or_https_path" };
                }
            }
        }

        if (error == null) {
            searchUnit.setOpenBISUrl(serverUrlInput.trim());
        } else {
            searchUnit.setOpenBISUrl(null);
        }
        return error;
    }

    String[] validateIgnoredPathPatterns(String ignoredPathPatterns) {
        if (selectedIgnoredFilesMode.getValue() == SyncJob.IgnoredFilesMode.SpecificList) {
            if(ignoredPathPatterns != null && !ignoredPathPatterns.isBlank()) {
                for(String ignoredPathPattern : ignoredPathPatterns.split("[\\r\\n]+")) {
                    if (!ignoredPathPattern.isBlank()) {
                        try {
                            GlobUtil.compileIgnoredPathGlob(ignoredPathPattern);
                        } catch (Exception e) {
                            return new String[] { "error_tooltip.wrong_glob", ignoredPathPattern };
                        }
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    boolean isEditDialog() {
        return this.editedSyncJob != null;
    }

    static void addErrorClass(@NonNull Node node) {
        node.getStyleClass().add(DisplaySettings.ERROR_STYLE_CLASS);
    }

    static void removeErrorClass(@NonNull Node node) {
        node.getStyleClass().removeIf(DisplaySettings.ERROR_STYLE_CLASS::equals);
    }

    static void addValidationLayerToTextInput(@NonNull TextArea textField, @NonNull Function<TextInputControl, String[]> errorMessageProducer, @NonNull BooleanProperty errorFlag) {
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue && !newValue) {
                doValidationOnTextInputNode(textField, errorMessageProducer, errorFlag);
            }
        });
    }

    static void addValidationLayerToTextInput(@NonNull TextField textField, @NonNull Function<TextInputControl, String[]> errorMessageProducer, @NonNull BooleanProperty errorFlag) {
        textField.setOnAction((e) -> doValidationOnTextInputNode(textField, errorMessageProducer, errorFlag));
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue && !newValue) {
                doValidationOnTextInputNode(textField, errorMessageProducer, errorFlag);
            }
        });
    }

    static void doValidationOnTextInputNode(@NonNull TextInputControl node, @NonNull Function<TextInputControl, String[]> errorMessageProducer, @NonNull BooleanProperty errorFlag) {
        String[] errorMessage = null;
        try {
            errorMessage = errorMessageProducer.apply(node);
        } catch (Exception e) {
            errorMessage = new String[]{ "error_tooltip.exception_in_validation" };
        }
        if (errorMessage == null) {
            removeErrorClass(node);
            node.setTooltip(null);
            node.setText(node.getText().trim());
            errorFlag.setValue(false);
        } else {
            addErrorClass(node);
            Tooltip tooltip = new Tooltip();
            tooltip.setAutoHide(true);
            tooltip.textProperty().bind(SharedContext.getContext().getI18n().createStringBinding(errorMessage[0], (Object[]) Arrays.copyOfRange(errorMessage, 1, errorMessage.length)));
            node.setTooltip(tooltip);
            errorFlag.setValue(true);
        }
    }

    void doValidationOnAllInputFields() {
        doValidationOnTextInputNode(localDirectoryValue, (textInput) -> validateLocalDirectoryValue(textInput.getText()), localDirectoryPropertyError);
        doValidationOnTextInputNode(personalAccessTokenValue, (textInput) -> validatePersonalAccessTokenValue(textInput.getText()), personalAccessTokenPropertyError);        
        doValidationOnTextInputNode(openbisServerDirectoryValue, (textInput) -> validateRemoteDirectoryValue(textInput.getText()), remoteDirectoryPropertyError);
        doValidationOnTextInputNode(openbisEntityIdValue, (textInput) -> validateEntityIdValue(textInput.getText()), entityIdPropertyError);
        doValidationOnTextInputNode(titleValue, (textInput) -> validateTitleValue(textInput.getText()), titlePropertyError);
        doValidationOnTextInputNode(openbisServerUrlValue, (textInput) -> validateOpenbisServerUrlValue(textInput.getText()), openbisUrlPropertyError);
        doValidationOnTextInputNode(ignoredPathPatterns, (textInput) -> validateIgnoredPathPatterns(textInput.getText()), ignoredPathPatternPropertyError);
    }

    Optional<SyncJob> getMostRecentlyTouchedSyncJob() {
        if (currentSyncJobs != null && !currentSyncJobs.isEmpty()) {
            return Optional.ofNullable(currentSyncJobs.get(currentSyncJobs.size() - 1));
        } else {
            return Optional.empty();
        }
    }

    Button getInputTooltipHelpButton(String tooltipText) {
        Button aboutButton = new Button();
        aboutButton.getStyleClass().add(DisplaySettings.FONT_AWESOME_CLASS);
        aboutButton.setPadding(new Insets(0, 0, 0, 0));
        aboutButton.setTextAlignment(TextAlignment.CENTER);
        aboutButton.setMinSize(12,12);
        aboutButton.setShape(new Circle(10));
        aboutButton.setStyle("-fx-font-size: 13");
        Label questionMarkLabel = new Label(DisplaySettings.FONT_AWESOME_7_FREE_SOLID_CIRCLE_QUESTION_MARK);
        aboutButton.setGraphic(questionMarkLabel);
        questionMarkLabel.setTranslateY(-1);
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setAutoHide(true);
        aboutButton.setOnAction( (e) -> {
            Point2D screenCoordinates = aboutButton.localToScreen(12, 12);
            tooltip.show(openbisEntityIdValue, screenCoordinates.getX(), screenCoordinates.getY());
        });
        return aboutButton;
    }

    @Value
    static class EntitySuggestion {
        enum Kind { ENTITY, TITLE, ERROR}

        static EntitySuggestion newEntity(@NonNull AbstractEntity abstractEntity) {
            return new EntitySuggestion(Kind.ENTITY, null, null, abstractEntity);
        }
        static EntitySuggestion newTitle(@NonNull String title) {
            return new EntitySuggestion(Kind.TITLE, title, null, null);
        }
        static EntitySuggestion newError(@NonNull String error) {
            return new EntitySuggestion(Kind.ERROR, null, error, null);
        }

        Kind kind;

        String title;
        String error;
        AbstractEntity abstractEntity;
    }

    @Value
    static class ChosenEntity {
        @NonNull String entityId;
        SyncJob.EntityType entityType;
        boolean immutable;
    }

    static SyncJob.EntityType toSyncJobEntityType(@NonNull AbstractEntity abstractEntity) {
        if (abstractEntity instanceof Sample) {
            return SyncJob.EntityType.Sample;
        } else if (abstractEntity instanceof Experiment) {
            return SyncJob.EntityType.Experiment;
        } else if (abstractEntity instanceof DataSet) {
            return SyncJob.EntityType.Dataset;
        } else {
            return null;
        }
    }
}
