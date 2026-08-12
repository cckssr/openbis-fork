package ch.openbis.drive.gui.util;

import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.openbis.drive.gui.i18n.I18n;
import javafx.application.HostServices;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import lombok.NonNull;

public class AboutDialog extends Dialog<Boolean> {
    private final Logger logger = LogManager.getLogger(this.getClass());

    public static final String MAIN_OPENBIS_CH_PAGE = "https://openbis.ch";

    public AboutDialog(@NonNull I18n i18n, Stage mainStage) {
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

        Label aboutMessage = new Label();
        aboutMessage.setText(i18n.get("dialog.about_message"));
        Hyperlink openbisMainPageUrl = new Hyperlink(MAIN_OPENBIS_CH_PAGE);
        openbisMainPageUrl.setOnAction((action) -> {
            try {
                HostServices hostServices = SharedContext.getContext().getHostServices();
                hostServices.showDocument(MAIN_OPENBIS_CH_PAGE);
            } catch (Exception e) {
                logger.catching(new RuntimeException("Error following link: " + MAIN_OPENBIS_CH_PAGE, e));
            }
        });
        getDialogPane().setContent(new VBox(aboutMessage, openbisMainPageUrl));
        getDialogPane().getButtonTypes().add(ButtonType.OK);
        ((Button) getDialogPane().lookupButton(ButtonType.OK)).textProperty().bind(i18n.createStringBinding("generic_buttons.ok"));

        setResultConverter((dialogButton) -> true );
    }
}
