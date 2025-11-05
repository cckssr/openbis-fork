package ch.openbis.drive.gui.maincontent;

import javafx.beans.InvalidationListener;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import lombok.NonNull;

public abstract class ResizablePanel extends AnchorPane implements AutoCloseable {
    private final InvalidationListener sizeListener;
    protected Pane parent;

    public ResizablePanel(@NonNull Pane parent) {
        this.parent = parent;
        sizeListener = (size) -> {
            resize();
        };
        parent.widthProperty().addListener(sizeListener);
        parent.heightProperty().addListener(sizeListener);
    }

    protected abstract void resize();

    public @NonNull Pane getResizableParent() {
        return parent;
    }

    @Override
    public void close() throws Exception {
        parent.widthProperty().removeListener(sizeListener);
        parent.heightProperty().removeListener(sizeListener);
    }
}
