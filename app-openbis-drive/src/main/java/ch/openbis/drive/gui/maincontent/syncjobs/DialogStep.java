package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.openbis.drive.model.SyncJob;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import lombok.NonNull;

import java.util.concurrent.CompletableFuture;

public interface DialogStep<CTX, RSLT> extends AutoCloseable {
    enum DialogStepResultEnum {
        NEXT, FINAL, BACK, CANCEL
    }

    record DialogStepResult<CTX, RSLT> (
            @NonNull DialogStepResultEnum result,
            @NonNull CTX context,
            @Nullable RSLT finalValue
    ) {}

    void initialize(
            @NonNull CTX context,
            @NonNull Dialog<RSLT> parentDialog,
            @NonNull BooleanProperty applyDisableProperty,
            CompletableFuture<DialogStep.DialogStepResult<CTX, RSLT>> resultFuture
    );

    @NonNull
    EventHandler<ActionEvent> getApplyHandler();

    @NonNull
    Node getContent();

    CompletableFuture<DialogStepResult<CTX, RSLT>> getResult();

    @Override
    void close() throws Exception;
}
