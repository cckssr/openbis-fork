package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.SharedContext;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

public class SyncJobDeleteDialog extends Dialog<Boolean> {
    public SyncJobDeleteDialog(Stage mainStage) {
        super();

        I18n i18n = SharedContext.getContext().getI18n();
        initStyle(StageStyle.DECORATED);

        final Window window = getDialogPane().getScene().getWindow();
        window.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                DisplaySettings.centerStageOnMainStage((Stage) window, mainStage);
            }
        });

        VBox content = new VBox();

        Label description = new Label();
        description.textProperty().bind(i18n.createStringBinding("sync_tasks.modal_panel.delete_sync_task_confirmation"));

        content.getChildren().add(description);
        getDialogPane().setContent(content);

        getDialogPane().getButtonTypes().add(ButtonType.YES);
        ((Button) getDialogPane().lookupButton(ButtonType.YES)).textProperty().bind(i18n.createStringBinding("generic_buttons.yes"));
        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        ((Button) getDialogPane().lookupButton(ButtonType.CANCEL)).textProperty().bind(i18n.createStringBinding("generic_buttons.cancel"));


        setResultConverter((dialogButton) -> dialogButton.getButtonData().getTypeCode().equals(ButtonType.YES.getButtonData().getTypeCode()));
    }

}
