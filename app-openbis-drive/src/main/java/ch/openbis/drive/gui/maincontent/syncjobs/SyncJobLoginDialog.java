package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.openbis.drive.gui.MainViewController;
import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.gui.util.Style;
import ch.openbis.drive.util.OpenBISQueryUtil;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import lombok.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static ch.openbis.drive.gui.maincontent.syncjobs.SyncJobDialog.*;


public class SyncJobLoginDialog extends Dialog<SyncJobSessionChoiceResult> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    final int MAX_TEXT_INPUT_LENGTH = 1000;
    Pattern HTTP_URL_PATTERN = Pattern.compile("^(http|https)://[^\\s/$.?#][^/]*$");
    public static final int EXTENDED_HEIGHT = SyncJobDialog.EXTENDED_HEIGHT - 100;

    final TextField openbisServerUrlValue;
    final BooleanProperty openbisUrlPropertyError = new SimpleBooleanProperty(false);

    final TextField usernameValue;
    final BooleanProperty usernamePropertyError = new SimpleBooleanProperty(false);

    final PasswordField passwordValue;
    final BooleanProperty passwordPropertyError = new SimpleBooleanProperty(false);

    final List<BooleanProperty> validationErrors = List.of(
            openbisUrlPropertyError, usernamePropertyError, passwordPropertyError);
    final BooleanBinding allValid = Bindings.createBooleanBinding(
            () -> validationErrors.stream().noneMatch(BooleanProperty::getValue), validationErrors.toArray(BooleanProperty[]::new));

    final ProgressIndicator loggingInProgressIndicator = new ProgressIndicator();

    public SyncJobLoginDialog(Stage mainStage) {
        super();
        I18n i18n = SharedContext.getContext().getI18n();


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

        Label openbisServerUrlLabel = new Label();
        openbisServerUrlLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.openbis_server_url"));
        openbisServerUrlLabel.setPadding(new Insets(30, 0, 0, 0));
        openbisServerUrlValue = getOpenbisServerUrlTextField();

        Label usernameLabel = new Label();
        usernameLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.login.user"));
        usernameLabel.setPadding(new Insets(30, 0, 0, 0));
        usernameValue = getUsernameTextField();

        Label passwordLabel = new Label();
        passwordLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.login.password"));
        passwordLabel.setPadding(new Insets(30, 0, 0, 0));
        passwordValue = getPasswordTextField();

        gridPane.add(openbisServerUrlLabel, 1, 0, 4, 1);
        gridPane.add(openbisServerUrlValue, 1, 1, 4, 1);
        gridPane.add(usernameLabel, 1, 2, 4, 1);
        gridPane.add(usernameValue, 1, 3, 4, 1);
        gridPane.add(passwordLabel, 1, 4, 4, 1);
        gridPane.add(passwordValue, 1, 5, 4, 1);

        VBox progressIndicatorVBox = new VBox();
        progressIndicatorVBox.setPadding(new Insets(30, 0, 0, 0));
        progressIndicatorVBox.getChildren().add(loggingInProgressIndicator);
        loggingInProgressIndicator.getStyleClass().add(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS);
        gridPane.add(progressIndicatorVBox, 1, 6, 1, 1);

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

        getDialogPane().getButtonTypes().add(ButtonType.APPLY);
        Button applyButton = (Button) getDialogPane().lookupButton(ButtonType.APPLY);
        applyButton.textProperty().bind(i18n.createStringBinding("generic_buttons.next"));

        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        ((Button) getDialogPane().lookupButton(ButtonType.CANCEL)).textProperty().bind(i18n.createStringBinding("generic_buttons.cancel"));

        Platform.runLater( () -> {
            getDialogPane().getScene().getWindow().setWidth(SyncJobDialog.EXTENDED_WIDTH);
            getDialogPane().getScene().getWindow().setHeight(EXTENDED_HEIGHT);
        } );

        allValid.addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                applyButton.setDisable(false);
            } else {
                applyButton.setDisable(true);
            }
        });

        //Validation and login attempt
        applyButton.addEventFilter(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                doValidationOnAllInputFields();
                if (validationErrors.stream().anyMatch(BooleanExpression::getValue)) {
                    applyButton.setDisable(true);
                    actionEvent.consume();
                }

                CompletableFuture<OpenBISQueryUtil.NewSessionResult> newSessionCompletableFuture =
                    CompletableFuture.supplyAsync(
                        () -> OpenBISQueryUtil.getInstance().getNewSession(
                                openbisServerUrlValue.getText().trim(),
                                usernameValue.getText().trim(), passwordValue.getText().trim()
                        )
                    );
                Platform.runLater(
                        () -> loggingInProgressIndicator.getStyleClass()
                                .remove(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS)
                );

                Exception exception = null;
                OpenBISQueryUtil.NewSessionResult newSessionResult = null;
                try {
                    newSessionResult =
                            newSessionCompletableFuture.get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.catching(e);
                    exception = e;
                } finally {
                    Platform.runLater(
                            () -> loggingInProgressIndicator.getStyleClass()
                                    .add(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS)
                    );
                }

                if (newSessionResult != null) {
                    if (newSessionResult.result() == OpenBISQueryUtil.NewSessionResultEnum.OK) {
                        final OpenBISQueryUtil.AvailableSession availableSession = newSessionResult.availableSession();
                        Platform.runLater( () -> {
                            setResult(
                                    new SyncJobSessionChoiceResult(
                                            true,
                                            availableSession
                                    )
                            );
                            close();
                        });
                    } else {
                        String errorTooltip = switch (newSessionResult.result()) {
                            case BAD_CREDENTIALS -> i18n.get("sync_tasks.login.login_error.bad_credentials");
                            case ERROR_REACHING_SERVER -> i18n.get("sync_tasks.login.login_error.server_unreachable");
                            default -> i18n.get("sync_tasks.login.login_error.unknown_error");
                        };
                        Platform.runLater( () -> {
                            setLoginAttemptErrorOnAllFields(errorTooltip);
                        });
                    }
                } else if (exception != null) {
                    Platform.runLater( () -> {
                        setLoginAttemptErrorOnAllFields(i18n.get("sync_tasks.login.login_error.unknown_error"));
                    });
                }

                actionEvent.consume();
            }
        });
        setResultConverter((dialogButton) ->
            new SyncJobSessionChoiceResult(false, null)
        );
    }

    TextField getOpenbisServerUrlTextField() {
        TextField openbisServerUrlValue = new TextField();
        openbisServerUrlValue.setEditable(true);
        openbisServerUrlValue.setPrefWidth(1200);
        addValidationLayerToTextInput(openbisServerUrlValue, (textInput) -> validateOpenbisServerUrlValue(openbisServerUrlValue.getText()), openbisUrlPropertyError);

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

        return error;
    }

    TextField getUsernameTextField() {
        TextField usernameValue = new TextField();
        usernameValue.setStyle(String.format("-fx-text-fill: %s",
                Style.toCssValue(Color.BLACK)));
        usernameValue.setPrefWidth(1200);
        addValidationLayerToTextInput(usernameValue, (textInput) -> validateUsernameValue(textInput.getText()), usernamePropertyError);
        return usernameValue;
    }

    String[] validateUsernameValue(String usernameInput) {
        String[] error;
        if(usernameInput == null || usernameInput.isBlank()) {
            error = new String[] { "error_tooltip.required_value" };
        } else {
            if(usernameInput.length() > MAX_TEXT_INPUT_LENGTH) {
                error = new String[] { "error_tooltip.too_long_text_input" };
            } else {
                error = null;
            }
        }

        return error;
    }

    PasswordField getPasswordTextField() {
        PasswordField passwordValue = new PasswordField();
        passwordValue.setStyle(String.format("-fx-text-fill: %s",
                Style.toCssValue(Color.BLACK)));
        passwordValue.setPrefWidth(1200);
        addValidationLayerToTextInput(passwordValue, (textInput) -> validatePasswordValue(textInput.getText()), passwordPropertyError);
        return passwordValue;
    }

    String[] validatePasswordValue(String passwordInput) {
        String[] error;
        if(passwordInput == null || passwordInput.isBlank()) {
            error = new String[] { "error_tooltip.required_value" };
        } else {
            if(passwordInput.length() > MAX_TEXT_INPUT_LENGTH) {
                error = new String[] { "error_tooltip.too_long_text_input" };
            } else {
                error = null;
            }
        }

        return error;
    }

    void doValidationOnAllInputFields() {
        doValidationOnTextInputNode(openbisServerUrlValue, (textInput) -> validateOpenbisServerUrlValue(textInput.getText()), openbisUrlPropertyError);
        doValidationOnTextInputNode(usernameValue, (textInput) -> validateUsernameValue(textInput.getText()), usernamePropertyError);
        doValidationOnTextInputNode(passwordValue, (textInput) -> validatePasswordValue(textInput.getText()), passwordPropertyError);
    }

    static void setLoginAttemptError(TextInputControl node, @NonNull String errorTooltip) {
        addErrorClass(node);
        Tooltip tooltip = new Tooltip();
        tooltip.setAutoHide(true);
        tooltip.setText(errorTooltip);
        node.setTooltip(tooltip);
    }

    void setLoginAttemptErrorOnAllFields(@NonNull String errorTooltip) {
        setLoginAttemptError(openbisServerUrlValue, errorTooltip);
        setLoginAttemptError(usernameValue, errorTooltip);
        setLoginAttemptError(passwordValue, errorTooltip);
    }
}
