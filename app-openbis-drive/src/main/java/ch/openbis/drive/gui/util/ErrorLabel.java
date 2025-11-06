package ch.openbis.drive.gui.util;

import javafx.scene.control.Label;

public class ErrorLabel extends Label {
    public ErrorLabel() {
        this.textProperty().bind(
                SharedContext.getContext().getI18n().createStringBinding(
                        "generic_messages.error"));
    }
}
