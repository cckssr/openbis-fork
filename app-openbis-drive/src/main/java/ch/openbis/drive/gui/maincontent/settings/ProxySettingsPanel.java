package ch.openbis.drive.gui.maincontent.settings;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.maincontent.ResizablePanel;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.SharedContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.NonNull;

public class ProxySettingsPanel extends ResizablePanel {

    public ProxySettingsPanel(@NonNull Pane parent) {
        super(parent);

        I18n i18n = SharedContext.getContext().getI18n();

        //TODO: proxy settings
        VBox vBox = new VBox();
        vBox.getStyleClass().add(DisplaySettings.MAIN_CONTENT_PADDED_FRAME_CLASS);
        Label label = new Label("TODO: Proxy settings");
        vBox.getChildren().add(label);
        this.getChildren().add(vBox);

        resize();
    }

    @Override
    protected synchronized void resize() {
        this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.setPrefSize(parent.getWidth(), parent.getHeight());
    }
}
