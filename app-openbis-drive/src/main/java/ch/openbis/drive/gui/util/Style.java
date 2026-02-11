package ch.openbis.drive.gui.util;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import lombok.NonNull;

import java.util.Objects;

public class Style {
    private static final String SINGLE_STYLE_SHEET =
            Objects.requireNonNull(Style.class.getClassLoader().getResource("style.css")).toExternalForm();

    public static void applyStyle(@NonNull Scene scene) {
        scene.getStylesheets().add(SINGLE_STYLE_SHEET);
    }

    public static String toCssValue(@NonNull Color color) {
        return String.format("#%02x%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255),
                (int) (color.getOpacity() * 255));
    }
}
