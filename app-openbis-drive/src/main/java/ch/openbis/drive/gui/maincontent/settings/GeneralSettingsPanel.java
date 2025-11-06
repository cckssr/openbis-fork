package ch.openbis.drive.gui.maincontent.settings;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.maincontent.ResizablePanel;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.ErrorLabel;
import ch.openbis.drive.gui.util.ServiceCallHandler;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.model.Settings;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.NonNull;

public class GeneralSettingsPanel extends ResizablePanel {
    private final BooleanProperty startAtLogin = new SimpleBooleanProperty(false);
    private final StringProperty language = new SimpleStringProperty("en");
    private final ObjectProperty<Integer> syncIntervalMinutes = new SimpleObjectProperty<>(1);

    private final ObjectProperty<Boolean> toBeApplied = new SimpleObjectProperty<>(false);

    private final Callback<Void, Void> refreshAll;

    public GeneralSettingsPanel(@NonNull Pane parent, @NonNull Callback<Void, Void> refreshAll) {
        super(parent);
        this.refreshAll = refreshAll;
        I18n i18n = SharedContext.getContext().getI18n();

        VBox vBox = new VBox();
        vBox.getStyleClass().add(DisplaySettings.MAIN_CONTENT_PADDED_FRAME_CLASS);
        this.getChildren().add(vBox);

        ServiceCallHandler.ServiceCallResult<Settings> settingsResult = SharedContext.getContext().getServiceCallHandler(parent).getSettings();
        if (settingsResult.isOk()) {
            Settings settings = settingsResult.getOk();
            this.startAtLogin.setValue(settings.isStartAtLogin());
            this.language.setValue(settings.getLanguage());
            this.syncIntervalMinutes.setValue((int)Math.ceil(((double) settings.getSyncInterval()) / 60));

            //Initialize content of general settings
            CheckBox startAtLoginCheckbox = getStartAtLoginCheckbox(i18n);
            vBox.getChildren().add(startAtLoginCheckbox);

            Label languageSelectionLabel = new Label();
            languageSelectionLabel.textProperty().bind(i18n.createStringBinding("main_panel.settings.general.language_selection_label"));
            vBox.getChildren().add(languageSelectionLabel);

            ChoiceBox<String> languageChoiceBox = getLanguageChoiceBox(i18n);
            vBox.getChildren().add(languageChoiceBox);

            Label syncCheckIntervalLabel = new Label();
            syncCheckIntervalLabel.setPadding(new Insets(40, 0, 0, 0));
            syncCheckIntervalLabel.textProperty().bind(i18n.createStringBinding("main_panel.settings.general.sync_check_interval_label"));
            vBox.getChildren().add(syncCheckIntervalLabel);

            HBox syncIntervalControlRow = getSyncIntervalControlRow();
            vBox.getChildren().add(syncIntervalControlRow);

            HBox confirmCancelButtons = new HBox();
            confirmCancelButtons.setPadding(new Insets(80, 0, 0, 0));
            confirmCancelButtons.setSpacing(20);
            Button okButton = getOkButton();
            Button cancelButton = getCancelButton();
            confirmCancelButtons.getChildren().add(okButton);
            confirmCancelButtons.getChildren().add(cancelButton);
            vBox.getChildren().add(confirmCancelButtons);
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
            vBox.getChildren().add(errorLabel);
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
                ServiceCallHandler.ServiceCallResult<Settings> freshSettingsToBeUpdated = SharedContext.getContext().getServiceCallHandler(parent).getSettings();
                if (freshSettingsToBeUpdated.isOk()) {
                    Settings current = freshSettingsToBeUpdated.getOk();
                    current.setLanguage(language.getValue());
                    current.setStartAtLogin(startAtLogin.getValue());
                    current.setSyncInterval(syncIntervalMinutes.getValue() * 60);
                    SharedContext.getContext().getServiceCallHandler(parent).setSettings(current);
                }
                refreshAll();
            }
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
                refreshAll.call(null);
            }
        });
        return cancelButton;
    }

    private void refreshAll() {
        this.refreshAll.call(null);
    }

    @Override
    protected synchronized void resize() {
        this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.setPrefSize(parent.getWidth(), parent.getHeight());
    }
}
