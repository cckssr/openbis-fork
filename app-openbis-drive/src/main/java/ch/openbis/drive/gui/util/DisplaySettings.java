package ch.openbis.drive.gui.util;

import ch.openbis.drive.gui.maincontent.ResizablePanel;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.NonNull;

import java.util.Optional;
import java.util.stream.Stream;

public class DisplaySettings {
    public static final double DEFAULT_INITIAL_WINDOW_WIDTH = 1100;
    public static final double DEFAULT_INITIAL_WINDOW_HEIGHT = 680;

    public static final double SIDE_MENU_WIDTH = 200;
    public static final double SIDE_MENU_BUTTON_HEIGHT = 50;

    public static final double MAIN_CONTENT_TAB_LABEL_HEIGHT = 50;

    public static final double SYNC_TASK_PANEL_TOP_BUTTON_WIDTH = 130;
    public static final double SYNC_TASK_PANEL_TOP_BUTTON_HEIGHT = 40;
    public static final double SYNC_TASK_PANEL_JOB_CARD_HEIGHT = 125;
    public static final int SYNC_JOB_CARD_SPACING = 12;

    public static final String MAIN_CONTENT_PADDED_FRAME_CLASS = "main-content-frame";
    public static final String SYNC_JOB_CARD_CLASS = "sync-job-card";
    public static final String TEXT_AREA_TABLE_CELL_CLASS = "text-area-table-cell";
    public static final String HIDDEN_DISPLAY_STYLE_CLASS = "hidden";
    public static final String ERROR_STYLE_CLASS = "error";
    public static final String NOTIFICATION_TABLE_ICON_COLUMN_CLASS = "notification-icon-table-column";
    public static final String FONT_AWESOME_CLASS = "font-awesome-7-free-solid";

    public static final String FONT_AWESOME_7_FREE_SOLID_CIRCLE_QUESTION_MARK = "\uF29C";
    public static final String FONT_AWESOME_7_FREE_SOLID_SQUARE_WITH_X = "\uF2D3";
    public static final String FONT_AWESOME_7_FREE_SOLID_WARNING_TRIANGLE = "\uF071";
    public static final String FONTAWESOME_7_FREE_SOLID_MAGNIFYING_GLASS = "\uF002";
    public static final String FONTAWESOME_7_FREE_SOLID_X_SYMBOL = "\uF00D";
    public static final String FONTAWESOME_7_FREE_SOLID_ROTATING_ARROWS = "\uF2F1";

    public static final String LOGO_TEXT = "openBIS Drive";

    public static void centerStageInScreen(@NonNull Stage stage) {
        centerStageInScreen(stage, null);
    }

    public static void centerStageInScreen(@NonNull Stage stage, Stage mainStage) {
        Screen screen = findScreenForStage( mainStage != null ? mainStage : stage );
        Rectangle2D screenBounds = screen.getBounds();
        stage.setX(screenBounds.getMinX() + screenBounds.getWidth() / 2 - (stage.getWidth() / 2));
        stage.setY(screenBounds.getMinY() + screenBounds.getHeight() / 2 - (stage.getHeight() / 2));
    }

    public static void centerStageOnMainStage(@NonNull Stage stage, Stage mainStage) {
        if(mainStage != null && isWellPositioned(mainStage)) {
            stage.setX(mainStage.getX() + mainStage.getWidth() / 2 - (stage.getWidth() / 2));
            stage.setY(mainStage.getY() + mainStage.getHeight() / 2 - (stage.getHeight() / 2));
        } else {
            centerStageInScreen(stage);
        }
    }

    public static Screen findScreenForStage(@NonNull Stage stage) {
        if (isWellPositioned(stage)) {

            return Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())
                    .stream().findFirst()
                    .orElse(Screen.getPrimary());
        } else {
            return Screen.getPrimary();
        }
    }

    public static Stage getStageFromNode(Node node) {
        Node inspectedNode = node;
        while (inspectedNode != null) {
            Window window = Optional.of(inspectedNode).map(Node::getScene).map(Scene::getWindow).orElse(null);
            if (window instanceof Stage) {
                return (Stage) window;
            } else {
                Node parent = inspectedNode.getParent();
                if( parent == null && inspectedNode instanceof ResizablePanel) {
                    parent = ((ResizablePanel) inspectedNode).getResizableParent();
                }
                inspectedNode = parent;
            }
        }
        return null;
    }

    static boolean isWellPositioned(@NonNull Stage stage) {
        return Stream.of(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())
                .allMatch( value -> !Double.isNaN(value) && Double.isFinite(value));
    }
}
