package ch.openbis.drive.gui.maincontent.syncjobs;

import ch.openbis.drive.gui.util.Style;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import lombok.NonNull;

import java.util.Optional;

public class SyncJobCardLabel extends HBox implements AutoCloseable {
    public static int DEFAULT_BIG_LABEL_SIZE = 14;
    public static int DEFAULT_SMALL_LABEL_SIZE = 10;
    public static int DEFAULT_TAG_MIN_WIDTH = 130;
    private final @NonNull ChangeListener<Number> desiredWidthChangeListener;
    private final @NonNull ObservableValue<Double> desiredWidth;

    public SyncJobCardLabel(@NonNull String tag,
                            @NonNull String value,
                            int fontSize,
                            @NonNull ObservableValue<Double> desiredWidth,
                            EventHandler<MouseEvent> clickHandler
                            ) {
        this(tag, value, fontSize, desiredWidth, DEFAULT_TAG_MIN_WIDTH, clickHandler, null);
    }

    public SyncJobCardLabel(@NonNull String tag,
                            @NonNull String value,
                            int fontSize,
                            @NonNull ObservableValue<Double> desiredWidth,
                            int tagMinWidth,
                            EventHandler<MouseEvent> clickHandler,
                            Color color) {
        SyncJobCardLabel self = this;
        this.setAlignment(Pos.CENTER);
        this.prefWidthProperty().bind(desiredWidth);
        this.minWidthProperty().bind(desiredWidth);
        this.maxWidthProperty().bind(desiredWidth);
        this.setSpacing(15);
        Label labelNode = new Label(tag);
        labelNode.setMinWidth(tagMinWidth);
        labelNode.setStyle(String.format("-fx-font-weight: bold; -fx-font-size: %spt", fontSize - 1));
        TextField valueNode = new TextField(value);
        valueNode.setEditable(false);
        valueNode.setStyle(String.format("-fx-font-size: %spt; -fx-background-color: transparent; -fx-text-fill: %s",
                fontSize, Style.toCssValue(Optional.ofNullable(color).orElse(Color.BLACK))));
        desiredWidthChangeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                self.resize(newValue.doubleValue(), self.getHeight());
                valueNode.setMinWidth(newValue.doubleValue() - tagMinWidth);
                valueNode.setPrefWidth(newValue.doubleValue() - tagMinWidth);
                valueNode.setMaxWidth(newValue.doubleValue() - tagMinWidth);
            }
        };
        if (clickHandler != null) {
            valueNode.setOnMouseClicked(clickHandler);
        }
        this.desiredWidth = desiredWidth;
        this.desiredWidth.addListener(desiredWidthChangeListener);
        this.getChildren().addAll(labelNode, valueNode);
    }

    @Override
    public void close() throws Exception {
        desiredWidth.removeListener(desiredWidthChangeListener);
        this.prefWidthProperty().unbind();
        this.minWidthProperty().unbind();
        this.maxWidthProperty().unbind();
    }
}
