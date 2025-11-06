package ch.openbis.drive.gui.util;

import javafx.scene.Scene;
import lombok.NonNull;

import java.util.Objects;

public class Style {
    private static final String SINGLE_STYLE_SHEET =
            Objects.requireNonNull(Style.class.getClassLoader().getResource("style.css")).toExternalForm();

    public static void applyStyle(@NonNull Scene scene) {
        scene.getStylesheets().add(SINGLE_STYLE_SHEET);
    }
}
