package ch.openbis.drive.gui.util;

import ch.openbis.drive.logging.Logging;
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
                        Logging.tryCatchErrorInStaticMethod(NodeNavigationUtil.class, new RuntimeException(String.format("Error closing node %s%n", node.getClass().getCanonicalName()), e));
                    }
                }
            }
        });
        mainContentPanel.getChildren().clear();
    }
}
