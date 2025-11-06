package ch.openbis.drive.gui.util;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import lombok.NonNull;

import java.util.function.Consumer;

public class NodeNavigationUtil {
    public static void closeAndClearChildNodes(@NonNull Pane mainContentPanel) {
        mainContentPanel.getChildren().forEach(new Consumer<Node>() {
            @Override
            public void accept(Node node) {
                if(node instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) node).close();
                    } catch (Exception e) {
                        System.err.printf("Error closing node %s%n", node.getClass().getCanonicalName());
                    }
                }
            }
        });
        mainContentPanel.getChildren().clear();
    }
}
