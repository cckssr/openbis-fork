package ch.openbis.drive.gui.maincontent.settings;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.maincontent.ResizablePanel;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.ErrorLabel;
import ch.openbis.drive.gui.util.ServiceCallHandler;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.util.GlobUtil;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.NonNull;
import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GeneralSettingsPanel extends ResizablePanel {
    final static String LINE_SEPARATOR = System.lineSeparator();

    private final BooleanProperty startAtLogin = new SimpleBooleanProperty(false);
    private final StringProperty language = new SimpleStringProperty("en");
    private final ObjectProperty<Integer> syncIntervalMinutes = new SimpleObjectProperty<>(1);
    private final ObjectProperty<Integer> expiringSessionWarningDays = new SimpleObjectProperty<>(
            Settings.DEFAULT_EXPIRING_SESSION_WARNING_DAYS
    );
    final TextArea ignoredPathPatterns = new TextArea();
    Accordion advancedSettingsAccordion = new Accordion();
    final VBox mainContentVBox = new VBox();

    final BooleanProperty ignoredPathPatternPropertyError = new SimpleBooleanProperty(false);
    final List<BooleanProperty> validationErrors = List.of(
            ignoredPathPatternPropertyError);
    final BooleanBinding allValid = Bindings.createBooleanBinding(
            () -> validationErrors.stream().noneMatch(BooleanProperty::getValue), validationErrors.toArray(BooleanProperty[]::new));
    private final ObjectProperty<Boolean> toBeApplied = new SimpleObjectProperty<>(false);

    private final Callback<GeneralSettingsPanelContext, Void> refreshAll;

    public GeneralSettingsPanel(@NonNull Pane parent, @NonNull GeneralSettingsPanelContext generalSettingsPanelContext, @NonNull Callback<GeneralSettingsPanelContext, Void> refreshAll) {
        super(parent);
        this.refreshAll = refreshAll;
        I18n i18n = SharedContext.getContext().getI18n();

        mainContentVBox.getStyleClass().add(DisplaySettings.MAIN_CONTENT_PADDED_FRAME_CLASS);
        this.getChildren().add(mainContentVBox);

        ServiceCallHandler.ServiceCallResult<Settings> settingsResult = SharedContext.getContext().getServiceCallHandler(parent).getSettings();
        if (settingsResult.isOk()) {
            Settings settings = settingsResult.getOk();
            this.startAtLogin.setValue(settings.isStartAtLogin());
            this.language.setValue(settings.getLanguage());
            this.syncIntervalMinutes.setValue((int)Math.ceil(((double) settings.getSyncInterval()) / 60));
            this.expiringSessionWarningDays.setValue(settings.getExpiringSessionWarningDays());

            //Initialize content of general settings
            CheckBox startAtLoginCheckbox = getStartAtLoginCheckbox(i18n);
            mainContentVBox.getChildren().add(startAtLoginCheckbox);

            Label languageSelectionLabel = new Label();
            languageSelectionLabel.textProperty().bind(i18n.createStringBinding("main_panel.settings.general.language_selection_label"));
            mainContentVBox.getChildren().add(languageSelectionLabel);

            ChoiceBox<String> languageChoiceBox = getLanguageChoiceBox(i18n);
            mainContentVBox.getChildren().add(languageChoiceBox);

            Label syncCheckIntervalLabel = new Label();
            syncCheckIntervalLabel.setPadding(new Insets(40, 0, 0, 0));
            syncCheckIntervalLabel.textProperty().bind(i18n.createStringBinding("main_panel.settings.general.sync_check_interval_label"));
            mainContentVBox.getChildren().add(syncCheckIntervalLabel);

            HBox syncIntervalControlRow = getSyncIntervalControlRow();
            mainContentVBox.getChildren().add(syncIntervalControlRow);

            Label expiringSessionWarningDaysLabel = new Label();
            expiringSessionWarningDaysLabel.setPadding(new Insets(40, 0, 0, 0));
            expiringSessionWarningDaysLabel.textProperty().bind(SharedContext.getContext().getI18n().createStringBinding("main_panel.settings.general.expiring_session_warning"));
            mainContentVBox.getChildren().add(expiringSessionWarningDaysLabel);

            HBox expiringSessionWarningDaysControlRow = getExpiringSessionWarningDaysControlRow();
            mainContentVBox.getChildren().add(expiringSessionWarningDaysControlRow);

            advancedSettingsAccordion.setPadding(new Insets(40, 0, 0, 0));
            VBox ignoredFilesBox = new VBox();
            ignoredFilesBox.setSpacing(20);

            initializeIgnoredPathPatternsTextArea(settings.getIgnoredPathPatterns());
            VBox globalDefaultIgnoredPathPatternBox = getIgnoredPathPatternsBox(ignoredPathPatterns, i18n);
            ignoredFilesBox.getChildren().add(globalDefaultIgnoredPathPatternBox);

            TitledPane ignoredFilesPane = new TitledPane(i18n.get("generic_messages.advanced_settings"), ignoredFilesBox);
            advancedSettingsAccordion.getPanes().addAll(ignoredFilesPane);
            if (generalSettingsPanelContext.isOpenAdvancedSettings()) {
                advancedSettingsAccordion.setExpandedPane(ignoredFilesPane);
            }
            mainContentVBox.getChildren().add(advancedSettingsAccordion);

            HBox confirmCancelButtons = new HBox();
            confirmCancelButtons.setPadding(new Insets(20, 0, 0, 0));
            confirmCancelButtons.setSpacing(20);
            Button okButton = getOkButton();
            Button cancelButton = getCancelButton();
            confirmCancelButtons.getChildren().add(okButton);
            confirmCancelButtons.getChildren().add(cancelButton);
            mainContentVBox.getChildren().add(confirmCancelButtons);
            confirmCancelButtons.getStyleClass().add(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS);
            toBeApplied.addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
                    if (newValue) {
                        confirmCancelButtons.getStyleClass().removeIf(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS::equals);
                    } else {
                        confirmCancelButtons.getStyleClass().add(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS);
                    }
                }
            });

            resize();
        } else {
            ErrorLabel errorLabel = new ErrorLabel();
            mainContentVBox.getChildren().add(errorLabel);
        }
    }

    private HBox getSyncIntervalControlRow() {
        HBox syncIntervalControlRow = new HBox();
        syncIntervalControlRow.setSpacing(20);
        Spinner<Integer> syncIntervalField = new Spinner<>(1,100000, syncIntervalMinutes.getValue());
        syncIntervalField.setEditable(true);
        syncIntervalField.setMaxWidth(200);
        syncIntervalField.getEditor().textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                try {
                    int newIntValue = Integer.parseInt(newValue);
                    if(newIntValue > 1 && newIntValue < 100001) {
                        syncIntervalField.commitValue();
                    } else {
                        syncIntervalField.getEditor().setText("1");
                    }
                } catch (Exception e) {
                    syncIntervalField.getEditor().setText("1");
                }
            }
        });
        syncIntervalField.valueProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue<? extends Integer> observableValue, Integer oldValue, Integer newValue) {
                syncIntervalMinutes.setValue(newValue);
                toBeApplied.setValue(true);
            }
        });
        Label minutesTemporalUnitLabel = new Label();
        minutesTemporalUnitLabel.textProperty().bind(SharedContext.getContext().getI18n().createStringBinding("main_panel.settings.general.minutes_time_unit"));
        syncIntervalControlRow.getChildren().add(syncIntervalField);
        syncIntervalControlRow.getChildren().add(minutesTemporalUnitLabel);
        return syncIntervalControlRow;
    }

    private HBox getExpiringSessionWarningDaysControlRow() {
        HBox expiringSessionWarningDaysControlRow = new HBox();
        expiringSessionWarningDaysControlRow.setSpacing(20);
        Spinner<Integer> expiringSessionWarningDaysField = new Spinner<>(1,100000, expiringSessionWarningDays.getValue());
        expiringSessionWarningDaysField.setEditable(true);
        expiringSessionWarningDaysField.setMaxWidth(200);
        expiringSessionWarningDaysField.getEditor().textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                try {
                    int newIntValue = Integer.parseInt(newValue);
                    if(newIntValue > 1 && newIntValue < 100001) {
                        expiringSessionWarningDaysField.commitValue();
                    } else {
                        expiringSessionWarningDaysField.getEditor().setText(
                                String.valueOf(Settings.DEFAULT_EXPIRING_SESSION_WARNING_DAYS)
                        );
                    }
                } catch (Exception e) {
                    expiringSessionWarningDaysField.getEditor().setText(
                            String.valueOf(Settings.DEFAULT_EXPIRING_SESSION_WARNING_DAYS)
                    );
                }
            }
        });
        expiringSessionWarningDaysField.valueProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue<? extends Integer> observableValue, Integer oldValue, Integer newValue) {
                expiringSessionWarningDays.setValue(newValue);
                toBeApplied.setValue(true);
            }
        });
        Label daysTemporalUnitLabel = new Label();
        daysTemporalUnitLabel.textProperty().bind(SharedContext.getContext().getI18n().createStringBinding("main_panel.settings.general.days_time_unit"));
        expiringSessionWarningDaysControlRow.getChildren().add(expiringSessionWarningDaysField);
        expiringSessionWarningDaysControlRow.getChildren().add(daysTemporalUnitLabel);
        return expiringSessionWarningDaysControlRow;
    }

    private ChoiceBox<String> getLanguageChoiceBox(I18n i18n) {
        ChoiceBox<String> languageChoiceBox = new ChoiceBox<>();
        languageChoiceBox.getItems().addAll(I18n.SUPPORTED_LANGUAGES);
        languageChoiceBox.converterProperty().bind(Bindings.createObjectBinding(() -> new StringConverter<String>() {
            @Override
            public String toString(String s) {
                return i18n.get("main_panel.settings.general.language_" + s);
            }

            @Override
            public String fromString(String s) {
                return null;
            }
        }, language));
        languageChoiceBox.setValue(language.getValue());
        languageChoiceBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                language.setValue(newValue);
                toBeApplied.setValue(true);
            }
        });
        return languageChoiceBox;
    }

    private CheckBox getStartAtLoginCheckbox(I18n i18n) {
        CheckBox startAtLoginCheckbox = new CheckBox();
        startAtLoginCheckbox.textProperty().bind(i18n.createStringBinding("main_panel.settings.general.start_at_login_option"));
        startAtLoginCheckbox.selectedProperty().setValue(startAtLogin.getValue());
        startAtLoginCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
                startAtLogin.setValue(newValue);
                toBeApplied.setValue(true);
            }
        });
        startAtLoginCheckbox.setPadding(new Insets(0, 0, 40, 0));
        return startAtLoginCheckbox;
    }

    private Button getOkButton() {
        Button okButton = new Button();
        okButton.setDefaultButton(true);
        okButton.textProperty().bind(SharedContext.getContext().getI18n().createStringBinding("generic_buttons.apply"));
        okButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Platform.runLater(() -> doValidationOnAllInputFields());
                Platform.runLater(() -> {
                    if ( allValid.get() ) {
                        ServiceCallHandler.ServiceCallResult<Settings> freshSettingsToBeUpdated = SharedContext.getContext().getServiceCallHandler(parent).getSettings();
                        if (freshSettingsToBeUpdated.isOk()) {
                            Settings current = freshSettingsToBeUpdated.getOk();
                            current.setLanguage(language.getValue());
                            current.setStartAtLogin(startAtLogin.getValue());
                            current.setSyncInterval(syncIntervalMinutes.getValue() * 60);
                            current.setExpiringSessionWarningDays(expiringSessionWarningDays.getValue());
                            current.setIgnoredPathPatterns(
                                    new ArrayList<>(Arrays.stream(ignoredPathPatterns.getText().split("[\\r\\n]+"))
                                            .filter(str -> !str.isBlank())
                                            .map(String::trim)
                                            .toList())
                            );
                            SharedContext.getContext().getServiceCallHandler(parent).setSettings(current);
                        }
                        refreshAll(new GeneralSettingsPanelContext(advancedSettingsAccordion.getExpandedPane() != null));
                    }
                });
            }
        });
        allValid.addListener( (obs, oldValue, newValue) -> {
            okButton.setDisable(!newValue);
        });
        return okButton;
    }

    private Button getCancelButton() {
        Button cancelButton = new Button();
        cancelButton.setCancelButton(true);
        cancelButton.textProperty().bind(SharedContext.getContext().getI18n().createStringBinding("generic_buttons.cancel"));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                refreshAll(new GeneralSettingsPanelContext(advancedSettingsAccordion.getExpandedPane() != null));
            }
        });
        return cancelButton;
    }

    void initializeIgnoredPathPatternsTextArea(@NonNull List<String> ignoredPathPatternsStrings) {
        ignoredPathPatterns.setEditable(false);
        ignoredPathPatterns.setStyle("-fx-text-fill: grey");
        ignoredPathPatterns.setPrefHeight(400);
        ignoredPathPatterns.setText(
                String.join(LINE_SEPARATOR, ignoredPathPatternsStrings)
        );
        ignoredPathPatterns.textProperty().addListener( (obs, oldValue, newValue) -> {
                toBeApplied.setValue(true);
        });
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
    }

    VBox getIgnoredPathPatternsBox(@NonNull TextArea ignoredPathPatternsTextArea, I18n i18n) {
        VBox ignoredPathPatternBox = new VBox();
        ignoredPathPatternBox.setSpacing(5);
        ignoredPathPatternBox.getChildren().add(new Label(i18n.get("main_panel.settings.general.ignored_path_patterns")));

        HBox ignoredPathPatternValuesBox = new HBox();
        ignoredPathPatternValuesBox.setSpacing(5);
        ignoredPathPatternValuesBox.getChildren().add(ignoredPathPatternsTextArea);

        Button restoreDefaultIgnoredPathsButton = new Button(i18n.get("main_panel.settings.general.restore_factory_default_list"));
        restoreDefaultIgnoredPathsButton.setOnAction((event) -> {
            Platform.runLater( () -> {
                ignoredPathPatternsTextArea.setText(
                        String.join(LINE_SEPARATOR, SyncJob.getDefaultIgnoredPathPatterns())
                );
                ignoredPathPatternsTextArea.commitValue();
            });
            Platform.runLater(this::doValidationOnAllInputFields);
        });
        ignoredPathPatternValuesBox.getChildren().add(restoreDefaultIgnoredPathsButton);

        ignoredPathPatternBox.getChildren().add(ignoredPathPatternValuesBox);
        return ignoredPathPatternBox;
    }

    String[] validateIgnoredPathPatterns(String ignoredPathPatterns) {
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
    }

    static void addValidationLayerToTextInput(@NonNull TextArea textField, @NonNull Function<TextInputControl, String[]> errorMessageProducer, @NonNull BooleanProperty errorFlag) {
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
        doValidationOnTextInputNode(ignoredPathPatterns, (textInput) -> validateIgnoredPathPatterns(textInput.getText()), ignoredPathPatternPropertyError);
    }

    static void addErrorClass(@NonNull Node node) {
        node.getStyleClass().add(DisplaySettings.ERROR_STYLE_CLASS);
    }

    static void removeErrorClass(@NonNull Node node) {
        node.getStyleClass().removeIf(DisplaySettings.ERROR_STYLE_CLASS::equals);
    }

    private void refreshAll(@NonNull GeneralSettingsPanelContext generalSettingsPanelContext) {
        this.refreshAll.call(generalSettingsPanelContext);
    }

    @Override
    protected synchronized void resize() {
        this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.setPrefSize(parent.getWidth(), parent.getHeight());
        this.mainContentVBox.setPrefHeight(parent.getHeight());
    }

    @Value
    public static class GeneralSettingsPanelContext {
        boolean openAdvancedSettings;
    }
}
