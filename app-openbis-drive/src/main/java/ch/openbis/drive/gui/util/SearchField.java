package ch.openbis.drive.gui.util;

import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import lombok.NonNull;
import org.controlsfx.control.textfield.CustomTextField;


public class SearchField extends CustomTextField {

    public SearchField(@NonNull StringProperty boundStringProperty) {
        this.setPromptText(SharedContext.getContext().getI18n().get("generic_messages.filter"));

        Label leftMagnifyingGlass = new Label();
        leftMagnifyingGlass.getStyleClass().add(DisplaySettings.FONT_AWESOME_CLASS);
        leftMagnifyingGlass.setText(" " + DisplaySettings.FONTAWESOME_7_FREE_SOLID_MAGNIFYING_GLASS);
        this.setLeft(leftMagnifyingGlass);

        Label rightCancelSymbol = new Label();
        rightCancelSymbol.getStyleClass().addAll(DisplaySettings.FONT_AWESOME_CLASS, DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS);
        rightCancelSymbol.setText(DisplaySettings.FONTAWESOME_7_FREE_SOLID_X_SYMBOL + " ");
        leftMagnifyingGlass.setPadding(new Insets(0, 5, 0, 0));
        this.setRight(rightCancelSymbol);

        this.textProperty().addListener( (obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                rightCancelSymbol.getStyleClass().removeIf(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS::equals);
            } else {
                rightCancelSymbol.getStyleClass().add(DisplaySettings.HIDDEN_DISPLAY_STYLE_CLASS);
            }
        });

        CustomTextField self = this;
        rightCancelSymbol.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> {
            self.setText("");
        });

        this.textProperty().bindBidirectional(boundStringProperty);
    }
}
