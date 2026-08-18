package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.gui.util.Style;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.util.ParallelExecutionUtil;
import jakarta.annotation.Nullable;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.stage.*;
import lombok.NonNull;

import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SyncJobDialog extends Dialog<SyncJob> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final I18n i18n;

    final static int EXTENDED_HEIGHT = 750;
    final static int EXTENDED_WIDTH = 900;

    public enum SyncJobDialogStep {
        SESSION_CHOICE,
        LOGIN,
        FORM
    }

    private final Button applyOrNextButton;
    private final Button cancelOrBackButton;

    public record HistoryState(
            @NonNull SyncJobDialogStep step,
            @NonNull SyncJobDialogContext context
    ) {}
    private final Deque<HistoryState> previousSteps = new ConcurrentLinkedDeque<>();

    private volatile SyncJobDialogStep currentStep;
    private volatile SyncJobDialogContext currentContext;
    private volatile DialogStep<SyncJobDialogContext, SyncJob> currentDialogStep;
    private volatile EventHandler<ActionEvent> currentApplyActionHandler;
    private volatile CompletableFuture<DialogStep.DialogStepResult<SyncJobDialogContext, SyncJob>> currentResultFuture;

    public SyncJobDialog(
            @Nullable SyncJob toBeModified,
            @NonNull List<SyncJob> currentSyncJobs,
            Stage mainStage
    ) {
        super();
        initStyle(StageStyle.DECORATED);
        i18n = SharedContext.getContext().getI18n();

        final Window window = getDialogPane().getScene().getWindow();
        window.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Platform.runLater(() -> DisplaySettings.centerStageOnMainStage((Stage) window, mainStage));
            }
        });
        Style.applyStyle(getDialogPane().getScene());

        getDialogPane().getButtonTypes().add(ButtonType.APPLY);
        applyOrNextButton = (Button) getDialogPane().lookupButton(ButtonType.APPLY);
        applyOrNextButton.setText(i18n.get("generic_buttons.apply"));

        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        cancelOrBackButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelOrBackButton.setText(i18n.get("generic_buttons.cancel"));

        cancelOrBackButton.addEventFilter(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                event.consume();
                handleBackEvent(event);
            }
        });

        ParallelExecutionUtil.EXECUTOR_SERVICE.submit(
                () -> {
                    SyncJobDialogContext initialContext = new SyncJobDialogContext(toBeModified, currentSyncJobs, null);
                    SyncJobDialogStep initialStep = SyncJobDialogStep.SESSION_CHOICE;

                    initializeStep(initialStep, initialContext);

                    while (true) {
                        try {
                            CompletableFuture<DialogStep.DialogStepResult<SyncJobDialogContext, SyncJob>> resultFuture =
                                    getCurrentResultFuture();

                            if (resultFuture == null) {
                                return;
                            }

                            DialogStep.DialogStepResult<SyncJobDialogContext, SyncJob> result =
                                    resultFuture.get();

                            closeCurrentStepAndInitializeNewStepBasedOnResult(result);
                        } catch (Exception e) {
                            logger.catching(e);
                            Platform.runLater( () -> {
                                setResult(null);
                                close();
                            });
                            closeCurrentStep();
                            break;
                        }
                    }
                }
        );

        Platform.runLater( () -> {
            getDialogPane().getScene().getWindow().setWidth(EXTENDED_WIDTH);
            getDialogPane().getScene().getWindow().setHeight(EXTENDED_HEIGHT);
        } );

        setOnHidden(new EventHandler<DialogEvent>() {
            @Override
            public void handle(DialogEvent dialogEvent) {
                closeCurrentStep();
            }
        });
    }

    synchronized void closeCurrentStepAndInitializeNewStepBasedOnResult(DialogStep.DialogStepResult<SyncJobDialogContext, SyncJob> result) {
        closeCurrentStep();

        switch (result.result()) {
            case FINAL ->
                Platform.runLater(
                    () -> {
                        setResult(result.finalValue());
                        close();
                    }
                );
            case NEXT -> {
                Optional<SyncJobDialogStep> nextStep = getNextStep(currentStep, result.context());
                if (nextStep.isPresent()) {
                    storePreviousState();
                    initializeStep(nextStep.get(), result.context());
                } else {
                    Platform.runLater(
                            () -> {
                                setResult(null);
                                close();
                            }
                    );
                }
            }
            case BACK -> {
                if (isFirstStep(currentStep) || previousSteps.isEmpty()) {
                    Platform.runLater( () -> {
                        setResult(null);
                        close();
                    });
                } else {
                    HistoryState historyState = previousSteps.pop();
                    initializeStep(historyState.step(), historyState.context());
                }
            }
            case CANCEL ->
                Platform.runLater( () -> {
                    setResult(null);
                    close();
                });
        }
    }

    synchronized void handleBackEvent(ActionEvent ignored) {
        currentResultFuture.complete(new DialogStep.DialogStepResult<>(
                DialogStep.DialogStepResultEnum.BACK,
                currentContext,
                null
        ));
    }

    synchronized void initializeStep(
            @NonNull SyncJobDialogStep step,
            @NonNull SyncJobDialogContext context
    ) {
        CompletableFuture<DialogStep.DialogStepResult<SyncJobDialogContext, SyncJob>> resultFuture = new CompletableFuture<>();
        currentResultFuture = resultFuture;
        currentStep = step;
        DialogStep<SyncJobDialogContext, SyncJob> dialogStepFromEnum = getDialogStepFromEnum(step);
        currentDialogStep = dialogStepFromEnum;
        currentContext = context;
        EventHandler<ActionEvent> oldApplyHandler = currentApplyActionHandler;
        EventHandler<ActionEvent> newApplyHandler = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                dialogStepFromEnum.getApplyHandler().handle(event);
                event.consume();
            }
        };
        currentApplyActionHandler = newApplyHandler;

        Platform.runLater(
                () -> initializeStepGraphically(
                        resultFuture,
                        step,
                        dialogStepFromEnum,
                        context,
                        oldApplyHandler,
                        newApplyHandler
                )
        );
    }

    synchronized void initializeStepGraphically(
            CompletableFuture<DialogStep.DialogStepResult<SyncJobDialogContext, SyncJob>> resultFuture,
            SyncJobDialogStep step,
            DialogStep<SyncJobDialogContext, SyncJob> dialogStep,
            SyncJobDialogContext context,
            EventHandler<ActionEvent> oldActionHandler,
            EventHandler<ActionEvent> newActionHandler
    ) {
        dialogStep.initialize(
                context,
                this,
                applyOrNextButton.disableProperty(),
                resultFuture
        );
        getDialogPane().setContent(dialogStep.getContent());
        if (oldActionHandler != null) {
            applyOrNextButton.removeEventFilter(ActionEvent.ACTION, oldActionHandler);
        }
        cancelOrBackButton.setText(i18n.get(isFirstStep(step) ? "generic_buttons.cancel" : "generic_buttons.back"));
        applyOrNextButton.setText(i18n.get(isLastStep(step) ? "generic_buttons.apply" : "generic_buttons.next"));
        applyOrNextButton.addEventFilter(ActionEvent.ACTION, newActionHandler);
    }

    public static DialogStep<SyncJobDialogContext, SyncJob> getDialogStepFromEnum(@NonNull SyncJobDialogStep stepEnum) {
        return switch (stepEnum) {
            case SESSION_CHOICE -> new SyncJobSessionChoiceDialogStep();
            case LOGIN -> new SyncJobLoginDialogStep();
            case FORM -> new SyncJobFormDialogStep();
        };
    }

    public static Optional<SyncJobDialogStep> getNextStep(
            @NonNull SyncJobDialogStep currentStep,
            @NonNull SyncJobDialogContext newJobDialogContext
    ) {
        return switch (currentStep) {
            case SESSION_CHOICE -> {
                if (
                        newJobDialogContext.sessionChoiceResult().next() &&
                                newJobDialogContext.sessionChoiceResult().availableSession() != null
                ) {
                    yield Optional.of(SyncJobDialogStep.FORM);
                } else {
                    yield Optional.of(SyncJobDialogStep.LOGIN);
                }
            }
            case LOGIN -> Optional.of(SyncJobDialogStep.FORM);
            case FORM -> Optional.empty();
        };
    }

    public static boolean isFirstStep(@NonNull SyncJobDialogStep step) {
        return switch (step) {
            case SESSION_CHOICE -> true;
            default -> false;
        };
    }

    public static boolean isLastStep(@NonNull SyncJobDialogStep step) {
        return switch (step) {
            case FORM -> true;
            default -> false;
        };
    }

    synchronized void storePreviousState() {
        previousSteps.push(new HistoryState(currentStep, currentContext));
    }

    synchronized void closeCurrentStep() {
        if (currentDialogStep != null) {
            try {
                currentDialogStep.close();
            } catch (Exception e) {
                logger.catching(e);
            }
        }
        if (currentResultFuture != null) {
            currentResultFuture.cancel(true);
            currentResultFuture = null;
        }
    }

    synchronized CompletableFuture<DialogStep.DialogStepResult<SyncJobDialogContext, SyncJob>> getCurrentResultFuture() {
        return currentResultFuture;
    }
}
