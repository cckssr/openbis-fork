package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.ServiceCallHandler;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.gui.util.Style;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ch.ethz.sis.afsclient.client.AfsClientUploadHelper.toServerPathString;

public class SyncJobDialog extends Dialog<SyncJob> {
    final int MAX_TEXT_INPUT_LENGTH = 300;
    Pattern HTTP_URL_PATTERN = Pattern.compile("^(http|https)://[^\\s/$.?#].[^\\s]*$");
    final static String SUGGESTED_REMOTE_DIRECTORY = "/";

    final SyncJob editedSyncJob;
    final List<SyncJob> currentSyncJobs;

    final TextField openbisServerUrlValue;
    final TextField openbisEntityIdValue;
    final TextField openbisServerDirectoryValue;
    final TextField personalAccessTokenValue;
    final TextField localDirectoryValue;
    final SimpleObjectProperty<SyncJob.Type> selectedSyncJobType = new SimpleObjectProperty<>(SyncJob.Type.Bidirectional);
    private final CheckBox enabledCheckBox;

    final BooleanProperty openbisUrlPropertyError = new SimpleBooleanProperty(false);
    final BooleanProperty entityIdPropertyError = new SimpleBooleanProperty(false);
    final BooleanProperty remoteDirectoryPropertyError = new SimpleBooleanProperty(false);
    final BooleanProperty personalAccessTokenPropertyError = new SimpleBooleanProperty(false);
    final BooleanProperty localDirectoryPropertyError = new SimpleBooleanProperty(false);
    final List<BooleanProperty> validationErrors = List.of(
            openbisUrlPropertyError, entityIdPropertyError, remoteDirectoryPropertyError, personalAccessTokenPropertyError, localDirectoryPropertyError);
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
        content.setSpacing(50);

        Label description = new Label();
        description.textProperty().bind(i18n.createStringBinding(
                isEditDialog() ? "sync_tasks.modal_panel.edit_sync_task_description" :
                        "sync_tasks.modal_panel.add_new_sync_task_description"
        ));

        HBox textParametersBox = new HBox();
        textParametersBox.setSpacing(80);
        VBox leftTextParametersBox = new VBox();
        VBox rightTextParametersBox = new VBox();

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

        content.getChildren().add(description);
        content.getChildren().add(textParametersBox);
        content.getChildren().add(syncModeChoiceBox);
        content.getChildren().add(enabledCheckBox);
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
                newSyncJob.setEnabled(enabledCheckBox.isSelected());
                newSyncJob.setOpenBisUrl(openbisServerUrlValue.getText());
                newSyncJob.setEntityPermId(openbisEntityIdValue.getText());
                newSyncJob.setRemoteDirectoryRoot(toServerPathString(Path.of(openbisServerDirectoryValue.getText())));
                newSyncJob.setOpenBisPersonalAccessToken(personalAccessTokenValue.getText());
                newSyncJob.setLocalDirectoryRoot(localDirectoryValue.getText());
                newSyncJob.setType(selectedSyncJobType.get());
                return newSyncJob;
            } else {
                return null;
            }
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

    private HBox getSyncModeChoice(I18n i18n) {
        HBox syncModeChoiceBox = new HBox();
        syncModeChoiceBox.setSpacing(50);

        Label syncModeChoiceLabel = new Label();
        syncModeChoiceLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.synchronization_mode"));

        ToggleGroup syncModeToggleGroup = new ToggleGroup();

        RadioButton bidirectionalChoice = new RadioButton();
        bidirectionalChoice.setToggleGroup(syncModeToggleGroup);
        bidirectionalChoice.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                selectedSyncJobType.setValue(SyncJob.Type.Bidirectional);
            }
        });
        bidirectionalChoice.setSelected(true);
        bidirectionalChoice.textProperty().bind(i18n.createStringBinding("main_panel.sync_tasks.sync_job_card.mode.bidirectional"));

        RadioButton uploadChoice = new RadioButton();
        uploadChoice.setToggleGroup(syncModeToggleGroup);
        uploadChoice.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                selectedSyncJobType.setValue(SyncJob.Type.Upload);
            }
        });
        uploadChoice.textProperty().bind(i18n.createStringBinding("main_panel.sync_tasks.sync_job_card.mode.upload"));

        RadioButton downloadChoice = new RadioButton();
        downloadChoice.setToggleGroup(syncModeToggleGroup);
        downloadChoice.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                selectedSyncJobType.setValue(SyncJob.Type.Download);
            }
        });
        downloadChoice.textProperty().bind(i18n.createStringBinding("main_panel.sync_tasks.sync_job_card.mode.download"));

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

        syncModeChoiceBox.getChildren().addAll(syncModeChoiceLabel, bidirectionalChoice, uploadChoice, downloadChoice);
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

    String validateLocalDirectoryValue(String localDirectoryInput) {
        if(localDirectoryInput == null || localDirectoryInput.isBlank()) {
            return "error_tooltip.required_value";
        } else {
            if(localDirectoryInput.length() > MAX_TEXT_INPUT_LENGTH) {
                return "error_tooltip.too_long_text_input";
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
                                return "error_tooltip.local_directory_already_in_use";
                            }
                        }
                    }
                } else {
                    return  "error_tooltip.exception_in_validation";
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

    String validatePersonalAccessTokenValue(String personalAccessTokenInput) {
        if(personalAccessTokenInput == null || personalAccessTokenInput.isBlank()) {
            return "error_tooltip.required_value";
        } else {
            if(personalAccessTokenInput.length() > MAX_TEXT_INPUT_LENGTH) {
                return "error_tooltip.too_long_text_input";
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

    String validateRemoteDirectoryValue(String remoteDirectoryInput) {
        if(remoteDirectoryInput == null || remoteDirectoryInput.isBlank()) {
            return "error_tooltip.required_value";
        } else {
            if(remoteDirectoryInput.length() > MAX_TEXT_INPUT_LENGTH) {
                return "error_tooltip.too_long_text_input";
            } else {
                if(Path.of(remoteDirectoryInput).startsWith(File.separator)) {
                    return null;
                } else {
                    return "error_tooltip.required_absolute_path";
                }
            }
        }
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

    String validateEntityIdValue(String entityIdInput) {
        if(entityIdInput == null || entityIdInput.isBlank()) {
            return "error_tooltip.required_value";
        } else {
            if(entityIdInput.length() > MAX_TEXT_INPUT_LENGTH) {
                return "error_tooltip.too_long_text_input";
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

    String validateOpenbisServerUrlValue(String serverUrlInput) {
        if(serverUrlInput == null || serverUrlInput.isBlank()) {
            return "error_tooltip.required_value";
        } else {
            if(serverUrlInput.length() > MAX_TEXT_INPUT_LENGTH) {
                return "error_tooltip.too_long_text_input";
            } else {
                if(HTTP_URL_PATTERN.asMatchPredicate().test(serverUrlInput)) {
                    return null;
                } else {
                    return "error_tooltip.required_http_or_https_path";
                }
            }
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

    static void addValidationLayerToTextInput(@NonNull TextField textField, @NonNull Function<TextInputControl, String> errorMessageProducer, @NonNull BooleanProperty errorFlag) {
        textField.setOnAction((e) -> doValidationOnTextInputNode(textField, errorMessageProducer, errorFlag));
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue && !newValue) {
                doValidationOnTextInputNode(textField, errorMessageProducer, errorFlag);
            }
        });
    }

    static void doValidationOnTextInputNode(@NonNull TextInputControl node, @NonNull Function<TextInputControl, String> errorMessageProducer, @NonNull BooleanProperty errorFlag) {
        String errorMessage = null;
        try {
            errorMessage = errorMessageProducer.apply(node);
        } catch (Exception e) {
            errorMessage = "error_tooltip.exception_in_validation";
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
            tooltip.textProperty().bind(SharedContext.getContext().getI18n().createStringBinding(errorMessage));
            node.setTooltip(tooltip);
            errorFlag.setValue(true);
        }
    }

    void doValidationOnAllInputFields() {
        doValidationOnTextInputNode(localDirectoryValue, (textInput) -> validateLocalDirectoryValue(textInput.getText()), localDirectoryPropertyError);
        doValidationOnTextInputNode(personalAccessTokenValue, (textInput) -> validatePersonalAccessTokenValue(textInput.getText()), personalAccessTokenPropertyError);        
        doValidationOnTextInputNode(openbisServerDirectoryValue, (textInput) -> validateRemoteDirectoryValue(textInput.getText()), remoteDirectoryPropertyError);
        addValidationLayerToTextInput(openbisEntityIdValue, (textInput) -> validateEntityIdValue(textInput.getText()), entityIdPropertyError);
        addValidationLayerToTextInput(openbisServerUrlValue, (textInput) -> validateOpenbisServerUrlValue(textInput.getText()), openbisUrlPropertyError);
    }

    Optional<SyncJob> getMostRecentlyTouchedSyncJob() {
        if (currentSyncJobs != null && !currentSyncJobs.isEmpty()) {
            return Optional.ofNullable(currentSyncJobs.get(currentSyncJobs.size() - 1));
        } else {
            return Optional.empty();
        }
    }
}
