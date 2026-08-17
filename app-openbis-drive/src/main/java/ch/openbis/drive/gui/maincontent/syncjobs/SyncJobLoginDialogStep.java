package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.openbis.drive.gui.MainViewController;
import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.gui.util.Style;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.util.OpenBISQueryUtil;
import impl.org.controlsfx.skin.AutoCompletePopup;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import lombok.NonNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ch.openbis.drive.gui.maincontent.syncjobs.SyncJobFormDialogStep.*;


public class SyncJobLoginDialogStep implements DialogStep<SyncJobDialogContext, SyncJob> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    final int MAX_TEXT_INPUT_LENGTH = 1000;
    Pattern HTTP_URL_PATTERN = Pattern.compile("^(http|https)://[^\\s/$.?#][^/]*$");

    volatile TextField openbisServerUrlValue;
    volatile AutoCompletePopup<String> openbisServerUrlAutocomplete;
    final BooleanProperty openbisUrlPropertyError = new SimpleBooleanProperty(false);
    volatile Set<String> knownOpenbisUrls;

    volatile TextField usernameValue;
    final BooleanProperty usernamePropertyError = new SimpleBooleanProperty(false);

    volatile PasswordField passwordValue;
    final BooleanProperty passwordPropertyError = new SimpleBooleanProperty(false);

    final List<BooleanProperty> validationErrors = List.of(
            openbisUrlPropertyError, usernamePropertyError, passwordPropertyError);
    final BooleanBinding allValid = Bindings.createBooleanBinding(
            () -> validationErrors.stream().noneMatch(BooleanProperty::getValue), validationErrors.toArray(BooleanProperty[]::new));
    volatile ChangeListener<Boolean> allValidListener;
    volatile BooleanProperty applyDisableProperty;

    final ProgressIndicator loggingInProgressIndicator = new ProgressIndicator();

    private volatile Node content;
    private volatile CompletableFuture<DialogStepResult<SyncJobDialogContext, SyncJob>> result;
    private volatile EventHandler<ActionEvent> applyButtonHandler;

    @Override
    public void initialize(
            @NonNull SyncJobDialogContext context,
            @NonNull Dialog<SyncJob> parentDialog,
            @NonNull BooleanProperty applyDisableProperty,
            @NonNull CompletableFuture<DialogStepResult<SyncJobDialogContext, SyncJob>> resultFuture
    ) {
        I18n i18n = SharedContext.getContext().getI18n();
        this.applyDisableProperty = applyDisableProperty;
        this.result = resultFuture;
        this.knownOpenbisUrls = context.currentSyncJobs().stream()
                .map(SyncJob::getOpenBisUrl).collect(Collectors.toSet());

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

        Label openbisServerUrlLabel = new Label();
        openbisServerUrlLabel.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.sync_task_modal.openbis_server_url"));
        openbisServerUrlLabel.setPadding(new Insets(30, 0, 0, 0));
        openbisServerUrlValue = getOpenbisServerUrlTextField();
        openbisServerUrlAutocomplete = getOpenbisServerUrlAutocomplete();

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

        allValidListener = (obs, oldValue, newValue) -> {
            if (newValue) {
                applyDisableProperty.setValue(false);
            } else {
                applyDisableProperty.setValue(true);
            }
        };
        allValid.addListener(allValidListener);

        //Validation and login attempt
        applyButtonHandler = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                doValidationOnAllInputFields();
                if (validationErrors.stream().anyMatch(BooleanExpression::getValue)) {
                    applyDisableProperty.setValue(true);
                    return;
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
                            result.complete(
                                    new DialogStepResult<>(
                                            DialogStepResultEnum.NEXT,
                                            new SyncJobDialogContext(
                                                    context.toBeModified(),
                                                    context.currentSyncJobs(),
                                                    new SyncJobSessionChoiceResult(
                                                            true,
                                                            availableSession
                                                    )
                                            ),
                                            null
                                    )
                            );
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
            }
        };
    }

    AutoCompletePopup<String> getOpenbisServerUrlAutocomplete() {
        AutoCompletePopup<String> openbisServerUrlAutocomplete = new AutoCompletePopup<>();
        openbisServerUrlAutocomplete.getSuggestions().addAll(
                knownOpenbisUrls.stream()
                        .limit(10).toList()
        );
        openbisServerUrlAutocomplete.setOnSuggestion(new EventHandler<AutoCompletePopup.SuggestionEvent<String>>() {
            @Override
            public void handle(AutoCompletePopup.SuggestionEvent<String> event) {
                openbisServerUrlValue.setText(event.getSuggestion());
            }
        });
        openbisServerUrlValue.textProperty().addListener((obs, oldValue, newValue) -> {
            openbisServerUrlAutocomplete.getSuggestions().clear();
            openbisServerUrlAutocomplete.getSuggestions().addAll(
                    knownOpenbisUrls.stream().filter( value -> value.contains(newValue.trim()))
                            .limit(10).toList()
            );
            openbisServerUrlAutocomplete.setMinWidth(openbisServerUrlValue.getWidth());
            openbisServerUrlAutocomplete.show(openbisServerUrlValue);
        });
        openbisServerUrlValue.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                openbisServerUrlAutocomplete.setMinWidth(openbisServerUrlValue.getWidth());
                openbisServerUrlAutocomplete.show(openbisServerUrlValue);
            } else {
                openbisServerUrlAutocomplete.hide();
            }
        });
        return openbisServerUrlAutocomplete;
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

    @Override
    public @NonNull Node getContent() {
        return content;
    }

    @Override
    public @NonNull EventHandler<ActionEvent> getApplyHandler() {
        return applyButtonHandler;
    }

    @Override
    public CompletableFuture<DialogStepResult<SyncJobDialogContext, SyncJob>> getResult() {
        return result;
    }

    @Override
    public void close() throws Exception {
        if (allValidListener != null) {
            allValid.removeListener(allValidListener);
        }
        if (applyDisableProperty != null) {
            applyDisableProperty.setValue(false);
        }
    }
}
