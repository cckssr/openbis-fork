package ch.openbis.drive.gui.maincontent.settings;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.maincontent.ResizablePanel;
import ch.openbis.drive.gui.util.DisplaySettings;
import ch.openbis.drive.gui.util.ErrorLabel;
import ch.openbis.drive.gui.util.SharedContext;
import ch.openbis.drive.gui.util.UsageUtil;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Side;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.NonNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class UsageSettingsPanel extends ResizablePanel {

    private final VBox mainVBox;
    private final I18n i18n;
    private final ProgressIndicator progressIndicator;
    private volatile VBox pieChart = null;
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);

    public UsageSettingsPanel(@NonNull Pane parent) {
        super(parent);
        i18n = SharedContext.getContext().getI18n();
        UsageUtil.load(parent);
        isLoading.bind(UsageUtil.getLoadingProperty());

        progressIndicator = new ProgressIndicator();
        progressIndicator.visibleProperty().bind(isLoading);

        mainVBox = new VBox();
        mainVBox.getStyleClass().add(DisplaySettings.MAIN_CONTENT_PADDED_FRAME_CLASS);

        isLoading.addListener((obs, oldValue, newValue) -> {
            UsageUtil.Data data = UsageUtil.getData();
            Platform.runLater(() -> {
                refreshAccordingToLoadingState(newValue, data);
            });
        });

        this.getChildren().addAll(mainVBox);

        refreshAccordingToLoadingState(isLoading.getValue(), UsageUtil.getData());
    }

    private void refreshAccordingToLoadingState(Boolean newValue, UsageUtil.Data data) {
        mainVBox.getChildren().clear();
        if (!newValue) {
            if (data != null) {
                pieChart = getPieChart(data, i18n);
                mainVBox.getChildren().add(pieChart);
            } else {
                ErrorLabel errorLabel = new ErrorLabel();
                mainVBox.getChildren().add(errorLabel);
            }
        } else {
            mainVBox.getChildren().add(progressIndicator);
        }
        resize();
    }

    private VBox getPieChart(@NonNull UsageUtil.Data data, @NonNull I18n i18n) {
        VBox vBox = new VBox();
        vBox.getChildren().add(getLabelledSizeIndicator(i18n.get("main_panel.settings.usage.total_space"), getFileSizeWithUnitOfMeasurement(data.getTotalSize())));
        vBox.getChildren().add(getLabelledSizeIndicator(i18n.get("main_panel.settings.usage.available_space"), getFileSizeWithUnitOfMeasurement(data.getAvailableSpace())));
        vBox.getChildren().add(getLabelledSizeIndicator(i18n.get("main_panel.settings.usage.app_occupied_space"), getFileSizeWithUnitOfMeasurement(data.getTotalLocalDirSpace())));
        List<PieChart.Data> pieChartData = new ArrayList<>();
        pieChartData.add(new PieChart.Data(i18n.get("main_panel.settings.usage.available_space"), data.getAvailableSpacePercentage()));
        pieChartData.add(new PieChart.Data(
                getFileSizeWithUnitOfMeasurement(data.getAvailableSpace() - data.getTotalLocalDirSpace()) + "  " + i18n.get("main_panel.settings.usage.other_files"),
                100 - data.getAvailableSpacePercentage() - data.getTotalLocalDirSpacePercentage()));
        data.getLocalDirUsedPercentageMap().forEach( (locDir, usedSpacePercentage) -> {
            pieChartData.add(new PieChart.Data(getFileSizeWithUnitOfMeasurement(data.getLocalDirUsedSpaceMap().get(locDir)) + "  " + locDir, usedSpacePercentage));
        });
        PieChart pieChart = new PieChart();
        pieChart.setData(new ObservableListWrapper<>(pieChartData));
        pieChart.setLegendSide(Side.BOTTOM);
        pieChart.setMinSize(800, 300);
        pieChart.setMaxSize(1400, 900);
        vBox.getChildren().add(pieChart);
        return vBox;
    }

    private HBox getLabelledSizeIndicator(String label, String sizeWithUnit) {
        HBox hBox = new HBox();
        hBox.setSpacing(20);
        Label name = new Label();
        name.setText(label);
        name.setStyle("-fx-font-weight: bold");
        Label size = new Label();
        size.setText(sizeWithUnit);
        hBox.getChildren().addAll(name, size);
        return hBox;
    }

    private String getFileSizeWithUnitOfMeasurement(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1048576) {
            return new DecimalFormat("#.##").format(((double) bytes) / 1024) + " KB";
        } else if (bytes < 1073741824) {
            return new DecimalFormat("#.##").format(((double) bytes) / 1048576) + " MB";
        } else if (bytes < 1099511627776L) {
            return new DecimalFormat("#.##").format(((double) bytes) / 1073741824) + " GB";
        } else {
            return new DecimalFormat("#.##").format(((double) bytes) / 1099511627776L) + " TB";
        }
    }

    @Override
    protected synchronized void resize() {
        this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.setPrefSize(parent.getWidth(), parent.getHeight());
        mainVBox.setMaxSize(parent.getWidth(), parent.getHeight());
        if(pieChart != null) {
            pieChart.setPrefSize(parent.getWidth(), parent.getHeight());
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        isLoading.unbind();
    }
}
