package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.ServiceCallHandler;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.gui.util.Style;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.util.GlobUtil;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import lombok.NonNull;

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

    final SyncJob editedSyncJob;
    final List<SyncJob> currentSyncJobs;

    final TextField openbisServerUrlValue;
    final TextField titleValue;
    final TextField openbisEntityIdValue;
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

    public SyncJobDialog(@Nullable SyncJob toBeModified, Stage mainStage, List<SyncJob> currentSyncJobs) {
        super();
        this.editedSyncJob = toBeModified;
        this.currentSyncJobs = currentSyncJobs;

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

        Label titleLabel = new Label();
        titleLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.title"));
        titleLabel.setPadding(new Insets(30, 0, 0, 0));
        titleValue = getTitleTextField();
        headerBox.getChildren().addAll(description, titleLabel, titleValue);

        Label openbisServerUrlLabel = new Label();
        openbisServerUrlLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.openbis_server_url"));
        openbisServerUrlValue = getOpenbisServerUrlTextField();
        Label openbisEntityIdLabel = new Label();
        openbisEntityIdLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.openbis_entity_id"));
        openbisEntityIdLabel.setPadding(new Insets(30, 0, 0, 0));
        openbisEntityIdValue = getEntityIdTextField();
        Label openbisServerDirectoryLabel = new Label();
        openbisServerDirectoryLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.server_directory"));
        openbisServerDirectoryLabel.setPadding(new Insets(30, 0, 0, 0));
        openbisServerDirectoryValue = getRemoteDirectoryTextField();
        leftTextParametersBox.getChildren().addAll(
                openbisServerUrlLabel, openbisServerUrlValue,
                openbisEntityIdLabel, openbisEntityIdValue,
                openbisServerDirectoryLabel, openbisServerDirectoryValue);

        Label personalAccessTokenLabel = new Label();
        personalAccessTokenLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.personal_access_token"));
        personalAccessTokenValue = getPersonalAccessTokenTextField();
        Label localDirectoryLabel = new Label();
        localDirectoryLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.local_directory"));
        localDirectoryLabel.setPadding(new Insets(30, 0, 0, 0));
        localDirectoryValue = getLocalDirectoryTextField();

        rightTextParametersBox.getChildren().addAll(
                personalAccessTokenLabel, personalAccessTokenValue,
                localDirectoryLabel, localDirectoryValue);

        textParametersBox.getChildren().addAll(leftTextParametersBox, rightTextParametersBox);

        HBox syncModeChoiceBox = getSyncModeChoice(i18n);

        enabledCheckBox = getEnableCheckBox(i18n);

        content.getChildren().add(headerBox);
        content.getChildren().add(textParametersBox);
        content.getChildren().add(syncModeChoiceBox);
        content.getChildren().add(enabledCheckBox);

        Accordion accordion = new Accordion();
        VBox ignoredFilesBox = new VBox();
        ignoredFilesBox.setSpacing(20);
        HBox ignoredFilesModeChoiceBox = getIgnoredFilesModeChoice(i18n);
        ignoredFilesBox.getChildren().add(ignoredFilesModeChoiceBox);

        ignoredPathPatterns = getIgnoredPathPatternsTextArea();
        VBox specificIgnoredPathPatternBox = getIgnoredPathPatternsBox(ignoredPathPatterns, i18n);
        TextArea emptyPathPatternsTextArea = getEmptyPathPatternsTextArea();
        globalDefaultPathPatternsTextArea = getGlobalDefaultPathPatternsTextArea();
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

        accordion.expandedPaneProperty().addListener((obs, oldValue, newValue) -> {
            Platform.runLater(() -> {
                getDialogPane().getScene().getWindow().setHeight(newValue != null ? 800 : 650);
            });
        });
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

        Button restoreDefaultIgnoredPathsButton = new Button(i18n.get("sync_tasks.modal_panel.sync_task_modal.restore_default_list"));
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
        }

        syncModeChoiceBox.getChildren().addAll(syncModeChoiceLabel, downloadChoice, uploadChoice, bidirectionalChoice);
        return syncModeChoiceBox;
    }

    TextField getLocalDirectoryTextField() {
        TextField localDirectoryValue = new TextField();
        localDirectoryValue.setPrefWidth(350);
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
        personalAccessTokenValue.setPrefWidth(350);
        addValidationLayerToTextInput(personalAccessTokenValue, (textInput) -> validatePersonalAccessTokenValue(textInput.getText()), personalAccessTokenPropertyError);
        if (editedSyncJob != null) {
            personalAccessTokenValue.setText(editedSyncJob.getOpenBisPersonalAccessToken());
        } else {
            personalAccessTokenValue.setText(getMostRecentlyTouchedSyncJob().map( SyncJob::getOpenBisPersonalAccessToken ).orElse(""));
        }
        return personalAccessTokenValue;
    }

    String[] validatePersonalAccessTokenValue(String personalAccessTokenInput) {
        if(personalAccessTokenInput == null || personalAccessTokenInput.isBlank()) {
            return new String[] { "error_tooltip.required_value" };
        } else {
            if(personalAccessTokenInput.length() > MAX_TEXT_INPUT_LENGTH) {
                return new String[] { "error_tooltip.too_long_text_input" };
            } else {
                return null;
            }
        }
    }

    TextField getRemoteDirectoryTextField() {
        TextField openbisServerDirectoryValue = new TextField();
        openbisServerDirectoryValue.setPrefWidth(350);
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
        titleValue.setPrefWidth(350);
        titleValue.setMaxWidth(350);
        addValidationLayerToTextInput(titleValue, (textInput) -> validateTitleValue(textInput.getText()), titlePropertyError);
        if (editedSyncJob != null) {
            titleValue.setText(editedSyncJob.getTitle());
        }
        return titleValue;
    }

    TextField getEntityIdTextField() {
        TextField openbisEntityIdValue = new TextField();
        openbisEntityIdValue.setPrefWidth(350);
        addValidationLayerToTextInput(openbisEntityIdValue, (textInput) -> validateEntityIdValue(textInput.getText()), entityIdPropertyError);
        if (editedSyncJob != null) {
            openbisEntityIdValue.setText(editedSyncJob.getEntityPermId());
        }
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
        } else {
            if(entityIdInput.length() > MAX_TEXT_INPUT_LENGTH) {
                return new String[] { "error_tooltip.too_long_text_input" };
            } else {
                return null;
            }
        }
    }

    TextField getOpenbisServerUrlTextField() {
        TextField openbisServerUrlValue = new TextField();
        openbisServerUrlValue.setPrefWidth(350);
        addValidationLayerToTextInput(openbisServerUrlValue, (textInput) -> validateOpenbisServerUrlValue(openbisServerUrlValue.getText()), openbisUrlPropertyError);
        if (editedSyncJob != null) {
            openbisServerUrlValue.setText(editedSyncJob.getOpenBisUrl());
        } else {
            openbisServerUrlValue.setText(getMostRecentlyTouchedSyncJob().map( SyncJob::getOpenBisUrl ).orElse(""));
        }
        return openbisServerUrlValue;
    }

    String[] validateOpenbisServerUrlValue(String serverUrlInput) {
        if(serverUrlInput == null || serverUrlInput.isBlank()) {
            return new String[] { "error_tooltip.required_value" };
        } else {
            if(serverUrlInput.length() > MAX_TEXT_INPUT_LENGTH) {
                return new String[] { "error_tooltip.too_long_text_input" };
            } else {
                if(HTTP_URL_PATTERN.asMatchPredicate().test(serverUrlInput)) {
                    return null;
                } else {
                    return new String[] { "error_tooltip.required_http_or_https_path" };
                }
            }
        }
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
}
