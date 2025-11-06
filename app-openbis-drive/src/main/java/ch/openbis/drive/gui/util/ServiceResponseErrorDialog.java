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

public class ServiceResponseErrorDialog extends Dialog<Void> {
    private final Stage mainStage;

    public ServiceResponseErrorDialog(@NonNull I18n i18n, Stage mainStage) {
        super();
        this.mainStage = mainStage;
        initStyle(StageStyle.DECORATED);

        final Window window = getDialogPane().getScene().getWindow();

        window.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                DisplaySettings.centerStageInScreen((Stage) window, mainStage);
            }
        });

        getDialogPane().contentTextProperty().bind(i18n.createStringBinding("dialog.service_response_error"));
        getDialogPane().getButtonTypes().add(ButtonType.OK);
        ((Button) getDialogPane().lookupButton(ButtonType.OK)).textProperty().bind(i18n.createStringBinding("generic_buttons.ok"));

        setResultConverter((dialogButton) -> null);
    }
}
