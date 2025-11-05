package ch.openbis.drive.gui.util;

import javafx.scene.control.Button;
import javafx.scene.control.Label;


public class RefreshButton extends Button {

    public RefreshButton() {
        Label refreshingIcon = new Label();
        refreshingIcon.getStyleClass().add(DisplaySettings.FONT_AWESOME_CLASS);
        refreshingIcon.setText(DisplaySettings.FONTAWESOME_7_FREE_SOLID_ROTATING_ARROWS);

        this.setGraphic(refreshingIcon);
        this.setText(SharedContext.getContext().getI18n().get("generic_messages.refresh"));
    }
}
