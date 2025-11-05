package ch.openbis.drive.gui.maincontent;

import ch.openbis.drive.gui.MainViewController;
import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.maincontent.settings.GeneralSettingsPanel;
import ch.openbis.drive.gui.maincontent.settings.ProxySettingsPanel;
import ch.openbis.drive.gui.maincontent.settings.UsageSettingsPanel;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.SharedContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ch.openbis.drive.gui.util.NodeNavigationUtil.closeAndClearChildNodes;

public class SettingsPanel extends ResizablePanel {
    public static final String TOP_TAB_LABELS_ID = "top-tab-labels";
    public static final String SETTINGS_MAIN_CONTENT_ID = "settings-main-content";
    public static final String TOP_TAB_GENERAL_LABEL_ID = "top-tab-label-general";
    public static final String TOP_TAB_PROXY_LABEL_ID = "top-tab-label-proxy";
    public static final String TOP_TAB_USAGE_LABEL_ID = "top-tab-label-usage";

    @NonNull private final AnchorPane mainContentSpace;
    @NonNull private final HBox topTabLabelRow;

    final SimpleStringProperty activeTabId = new SimpleStringProperty();

    public SettingsPanel(@NonNull Pane parent) {
        super(parent);

        I18n i18n = SharedContext.getContext().getI18n();

        //Initialize top tab labels
        topTabLabelRow = new HBox();
        topTabLabelRow.setId(TOP_TAB_LABELS_ID);
        topTabLabelRow.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        topTabLabelRow.setPrefSize(parent.getWidth(), DisplaySettings.MAIN_CONTENT_TAB_LABEL_HEIGHT);
        this.getChildren().add(topTabLabelRow);

        Button generalSectionButton = new Button(); generalSectionButton.setId(TOP_TAB_GENERAL_LABEL_ID);
        Button proxySectionButton = new Button(); proxySectionButton.setId(TOP_TAB_PROXY_LABEL_ID);
        Button usageSectionButton = new Button(); usageSectionButton.setId(TOP_TAB_USAGE_LABEL_ID);

        generalSectionButton.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> activeTabId.setValue(TOP_TAB_GENERAL_LABEL_ID));
        proxySectionButton.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> activeTabId.setValue(TOP_TAB_PROXY_LABEL_ID));
        usageSectionButton.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> activeTabId.setValue(TOP_TAB_USAGE_LABEL_ID));

        activeTabId.addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                switch (newValue) {
                    case TOP_TAB_GENERAL_LABEL_ID -> activateGeneralSubpanel();
                    case TOP_TAB_PROXY_LABEL_ID -> activateProxySubpanel();
                    case TOP_TAB_USAGE_LABEL_ID -> activateUsageSubpanel();
                }
            }
        });

        List<Button> tabButtons = List.of(generalSectionButton, proxySectionButton, usageSectionButton);

        tabButtons.forEach( (button) -> {
            button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            button.setMinWidth(100);
            button.setPrefHeight(DisplaySettings.MAIN_CONTENT_TAB_LABEL_HEIGHT);
        });

        generalSectionButton.textProperty().bind(i18n.createStringBinding("main_panel.settings.general"));
        proxySectionButton.textProperty().bind(i18n.createStringBinding("main_panel.settings.proxy"));
        usageSectionButton.textProperty().bind(i18n.createStringBinding("main_panel.settings.usage"));

        topTabLabelRow.getChildren().addAll(tabButtons);
        //end initialization top tab labels

        //Initialize main-content space
        mainContentSpace = new AnchorPane();
        mainContentSpace.setId(SETTINGS_MAIN_CONTENT_ID);
        mainContentSpace.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.getChildren().add(mainContentSpace);
        mainContentSpace.setTranslateY(DisplaySettings.MAIN_CONTENT_TAB_LABEL_HEIGHT);
        //

        resize();
        activateGeneralSubpanel();
    }

    @Override
    protected synchronized void resize() {
        this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.setPrefSize(parent.getWidth(), parent.getHeight());
        this.topTabLabelRow.setPrefWidth(parent.getWidth());
        this.mainContentSpace.setPrefWidth(parent.getWidth());
        this.mainContentSpace.setPrefHeight(parent.getHeight() - DisplaySettings.MAIN_CONTENT_TAB_LABEL_HEIGHT);
        this.mainContentSpace.resize(parent.getWidth(), parent.getHeight() - DisplaySettings.MAIN_CONTENT_TAB_LABEL_HEIGHT);
    }

    synchronized void activateGeneralSubpanel() {
        closeAndClearChildNodes(mainContentSpace);
        mainContentSpace.getChildren().add(new GeneralSettingsPanel(mainContentSpace, (voidArg) -> { this.activateGeneralSubpanel(); return null;}));
        markButtonAsActiveById(TOP_TAB_GENERAL_LABEL_ID);
    }

    synchronized void activateProxySubpanel() {
        closeAndClearChildNodes(mainContentSpace);
        mainContentSpace.getChildren().add(new ProxySettingsPanel(mainContentSpace));
        markButtonAsActiveById(TOP_TAB_PROXY_LABEL_ID);
    }

    synchronized void activateUsageSubpanel() {
        closeAndClearChildNodes(mainContentSpace);
        mainContentSpace.getChildren().add(new UsageSettingsPanel(mainContentSpace));
        markButtonAsActiveById(TOP_TAB_USAGE_LABEL_ID);
    }

    synchronized List<Button> getTopTabLabelButtons() {
        HBox topTabLabelWrapper = (HBox) this.getChildren().filtered(it -> it instanceof HBox && TOP_TAB_LABELS_ID.equals(it.getId()))
                        .stream().findFirst().orElse(null);

        return Optional.ofNullable(topTabLabelWrapper)
                .map(hBox ->
                        hBox.getChildren().stream().filter(it -> it instanceof Button)
                                .map(it -> (Button) it).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    synchronized void markButtonAsActiveById(@NonNull String id) {
        getTopTabLabelButtons().forEach(
                button -> {
                    if (id.equals(button.getId())) {
                        button.getStyleClass().add(MainViewController.ACTIVE_TAB_BUTTON_STYLE_CLASS);
                    } else {
                        button.getStyleClass().removeIf( styleClass -> styleClass.equals(MainViewController.ACTIVE_TAB_BUTTON_STYLE_CLASS));
                    }
                }
        );
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (mainContentSpace != null) {
            closeAndClearChildNodes(mainContentSpace);
        }
    }
}
