package ch.openbis.drive.gui.util;

import ch.openbis.drive.gui.i18n.I18n;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import lombok.NonNull;

public class ServiceNotRunningDialog extends Dialog<Boolean> {
    public ServiceNotRunningDialog(@NonNull I18n i18n, Stage mainStage) {
        super();
        initStyle(StageStyle.DECORATED);

        final Window window = getDialogPane().getScene().getWindow();

        window.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                DisplaySettings.centerStageOnMainStage(
                        (Stage) window,
                        mainStage);
            }
        });

        getDialogPane().contentTextProperty().bind(i18n.createStringBinding("dialog.service_not_running"));
        getDialogPane().getButtonTypes().add(ButtonType.YES);
        ((Button) getDialogPane().lookupButton(ButtonType.YES)).textProperty().bind(i18n.createStringBinding("generic_buttons.yes"));
        getDialogPane().getButtonTypes().add(ButtonType.NO);
        ((Button) getDialogPane().lookupButton(ButtonType.NO)).textProperty().bind(i18n.createStringBinding("generic_buttons.no"));

        setResultConverter((dialogButton) ->
            dialogButton.getButtonData().getTypeCode().equals(ButtonType.YES.getButtonData().getTypeCode())
        );
    }
}
