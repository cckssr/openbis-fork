package ch.openbis.drive.gui.maincontent;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.*;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.tasks.SyncOperation;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.NonNull;
import lombok.Value;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NotificationsPanel extends ResizablePanel {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss z");
    private static final int NOTIFICATIONS_PER_PAGE = 20;
    private static final int FIXED_CELL_SIZE = 85;

    private final VBox mainVBox;
    private final TableView<NotificationRow> tableView;
    private final Pagination pagination;

    private final ErrorLabel errorLabel = new ErrorLabel();

    private final ObjectProperty<Notification.Type> notificationTypeFilter = new SimpleObjectProperty<>(null);
    private final ObjectProperty<List<NotificationRow>> notificationRows = new SimpleObjectProperty<>(Collections.emptyList());
    private final ObjectProperty<List<NotificationRow>> filteredNotifications = new SimpleObjectProperty<>(Collections.emptyList());

    private final StringProperty filterText = new SimpleStringProperty();

    public NotificationsPanel(@NonNull Pane parent) {
        super(parent);
        I18n i18n = SharedContext.getContext().getI18n();

        mainVBox = new VBox();
        mainVBox.getStyleClass().add(DisplaySettings.MAIN_CONTENT_PADDED_FRAME_CLASS);
        this.getChildren().add(mainVBox);

        AnchorPane filters = getFilters(i18n);

        tableView = initializeTable();
        pagination = initializePagination();
        mainVBox.getChildren().addAll(filters, tableView, pagination);

        refreshNotificationList();
        refreshNotificationTableAtPage(0, pagination);
        resize();
    }

    private AnchorPane getFilters(I18n i18n) {
        AnchorPane filters = new AnchorPane();

        HBox leftFilters = new HBox();
        leftFilters.setPadding(new Insets(15, 15, 15, 15));
        leftFilters.setSpacing(20);
        leftFilters.setAlignment(Pos.CENTER_LEFT);
        RefreshButton refreshButton = new RefreshButton();
        refreshButton.setOnAction((e) -> {
            refreshNotificationList();
            refreshNotificationTableAtPage(pagination.getCurrentPageIndex(), pagination);
        });
        leftFilters.getChildren().add(refreshButton);

        AnchorPane.setLeftAnchor(leftFilters, 0.0);
        AnchorPane.setTopAnchor(leftFilters, 0.0);

        HBox rightFilters = new HBox();
        rightFilters.setPadding(new Insets(15, 15, 15, 15));
        rightFilters.setSpacing(20);
        rightFilters.setAlignment(Pos.CENTER_RIGHT);
        Label notificationTypeSelectionLabel = new Label();
        notificationTypeSelectionLabel.textProperty().bind(i18n.createStringBinding("main_panel.notification_table.type_filter_label"));
        ChoiceBox<Optional<Notification.Type>> languageChoiceBox = getNotificationTypeFilterSelection(i18n);
        rightFilters.getChildren().add(notificationTypeSelectionLabel);
        rightFilters.getChildren().add(languageChoiceBox);
        rightFilters.getChildren().add(new SearchField(filterText));
        filterText.addListener((obs, oldValue, newValue) -> {
            if(newValue != null && !newValue.equals(oldValue)) {
                refreshNotificationTableAtPage(pagination.getCurrentPageIndex(), pagination);
            }
        });

        AnchorPane.setRightAnchor(rightFilters, 0.0);
        AnchorPane.setTopAnchor(rightFilters, 0.0);

        filters.getChildren().addAll(leftFilters, rightFilters);

        return filters;
    }

    private TableView<NotificationRow> initializeTable() {
        I18n i18n = SharedContext.getContext().getI18n();

        final TableView<NotificationRow> tableView;
        tableView = new TableView<>();
        tableView.setFixedCellSize(FIXED_CELL_SIZE);
        tableView.getItems().setAll(notificationRows.getValue().subList(0, Math.min(NOTIFICATIONS_PER_PAGE, notificationRows.getValue().size())));

        TableColumn<NotificationRow, Label> iconColumn = new TableColumn<>();
        iconColumn.setText("");
        iconColumn.getStyleClass().add(DisplaySettings.NOTIFICATION_TABLE_ICON_COLUMN_CLASS);
        iconColumn.setCellValueFactory(eventData -> {
            Label label = new Label();
            switch (eventData.getValue().getNotification().getType()) {
                case Conflict -> {
                    label.setText(DisplaySettings.FONT_AWESOME_7_FREE_SOLID_CIRCLE_QUESTION_MARK);
                    label.setTextFill(Paint.valueOf("red"));
                }
                case JobStopped -> {
                    label.setText(DisplaySettings.FONT_AWESOME_7_FREE_SOLID_SQUARE_WITH_X);
                    label.setTextFill(Paint.valueOf("grey"));
                }
                case JobException -> {
                    label.setText(DisplaySettings.FONT_AWESOME_7_FREE_SOLID_WARNING_TRIANGLE);
                    label.setTextFill(Paint.valueOf("orange"));
                }
            }
            return new ReadOnlyObjectWrapper<>(
                label
            );
        });
        iconColumn.setPrefWidth(40);

        TableColumn<NotificationRow, String> typeColumn = new TableColumn<>();
        typeColumn.textProperty().bind(i18n.createStringBinding("notification_panel.notification_table.column_title.type"));
        typeColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getTypeColumn()));
        typeColumn.setCellFactory(WRAPPING_CELL_FACTORY);
        typeColumn.setPrefWidth(150);

        TableColumn<NotificationRow, String> messageColumn = new TableColumn<>();
        messageColumn.textProperty().bind(i18n.createStringBinding("notification_panel.notification_table.column_title.notification"));
        messageColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getMessageColumn()));
        messageColumn.setCellFactory(WRAPPING_CELL_FACTORY);
        messageColumn.setPrefWidth(300);

        TableColumn<NotificationRow, String> localDirColumn = new TableColumn<>();
        localDirColumn.textProperty().bind(i18n.createStringBinding("notification_panel.notification_table.column_title.local_directory"));
        localDirColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getLocalDirColumn()));
        localDirColumn.setCellFactory(WRAPPING_CELL_FACTORY);
        localDirColumn.setPrefWidth(250);

        TableColumn<NotificationRow, String> fileColumn = new TableColumn<>();
        fileColumn.textProperty().bind(i18n.createStringBinding("notification_panel.notification_table.column_title.file"));
        fileColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getFileColumn()));
        fileColumn.setCellFactory(WRAPPING_CELL_FACTORY);
        fileColumn.setPrefWidth(200);

        TableColumn<NotificationRow, String> dateAndTimeColumn = new TableColumn<>();
        dateAndTimeColumn.textProperty().bind(i18n.createStringBinding("notification_panel.notification_table.column_title.date_and_time"));
        dateAndTimeColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getDateAndTimeColumn()));
        dateAndTimeColumn.setCellFactory(WRAPPING_CELL_FACTORY);
        dateAndTimeColumn.setPrefWidth(200);

        tableView.getColumns().addAll(List.of(iconColumn, typeColumn, messageColumn, localDirColumn, fileColumn, dateAndTimeColumn));
        return tableView;
    }

    private Pagination initializePagination() {
        final Pagination pagination;
        pagination = new Pagination(filteredNotifications.getValue().size() / NOTIFICATIONS_PER_PAGE, 0);
        pagination.setPageCount((filteredNotifications.getValue().size() / NOTIFICATIONS_PER_PAGE) + 1);
        pagination.currentPageIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                refreshNotificationTableAtPage(newValue, pagination);
            }
        });
        return pagination;
    }

    private void refreshNotificationTableAtPage(Number newValue, Pagination pagination) {
        refreshFilteredNotificationList();
        List<NotificationRow> refreshedNotificationList = filteredNotifications.get();
        int newOffset = newValue.intValue() * NOTIFICATIONS_PER_PAGE;
        if (newOffset < refreshedNotificationList.size()) {
            tableView.getItems().setAll(refreshedNotificationList.subList(newOffset, Math.min(refreshedNotificationList.size(), newOffset + NOTIFICATIONS_PER_PAGE)));
        } else {
            tableView.getItems().setAll(Collections.emptyList());
        }
        pagination.setPageCount((refreshedNotificationList.size() / NOTIFICATIONS_PER_PAGE) + 1);
    }

    @Override
    protected synchronized void resize() {
        mainVBox.setMinSize(800, parent.getHeight());
        mainVBox.setMaxSize(parent.getWidth(), parent.getHeight());
        mainVBox.setPrefSize(parent.getWidth(), parent.getHeight());

        tableView.setMinSize(parent.getWidth() - 80, 80);
        tableView.setMaxSize(parent.getWidth() - 80, parent.getHeight() - 80);
        tableView.setPrefSize(parent.getWidth() - 80, (NOTIFICATIONS_PER_PAGE + 1.1) * FIXED_CELL_SIZE);
    }

    @NonNull static String getNormalizedLocalFile(@NonNull Notification notification) {
        if (notification.getLocalFile() != null && !notification.getLocalFile().isEmpty()) {
            try {
                Path locaFilePath = Path.of(notification.getLocalFile());
                Path localDirectory = Path.of(notification.getLocalDirectory());
                return localDirectory.relativize(locaFilePath).toString();
            } catch (Exception e) {
                return notification.getLocalFile();
            }
        } else {
            return "";
        }
    }

    private ChoiceBox<Optional<Notification.Type>> getNotificationTypeFilterSelection(I18n i18n) {
        ChoiceBox<Optional<Notification.Type>> notificationTypeSelection = new ChoiceBox<>();
        notificationTypeSelection.getItems().addAll(List.of(
                Optional.empty(),
                Optional.of(Notification.Type.Conflict),
                Optional.of(Notification.Type.JobException),
                Optional.of(Notification.Type.JobStopped)));
        notificationTypeSelection.converterProperty().setValue(new StringConverter<>() {
            @Override
            public String toString(Optional<Notification.Type> type) {
                if (type.isEmpty()) {
                    return i18n.get("main_panel.notification_table.type_filter.all");
                } else {
                    return switch (type.get()) {
                        case Conflict -> i18n.get("main_panel.notification_table.type_filter.conflict");
                        case JobStopped -> i18n.get("main_panel.notification_table.type_filter.job_stopped");
                        case JobException -> i18n.get("main_panel.notification_table.type_filter.job_exception");
                    };
                }
            }

            @Override
            public Optional<Notification.Type> fromString(String s) {
                return Optional.empty();
            }
        });
        notificationTypeSelection.setValue(Optional.empty());
        notificationTypeSelection.valueProperty().addListener((obs, old, newValue) -> {
            notificationTypeFilter.setValue(newValue.orElse(null));
            if(pagination != null) {
                refreshNotificationTableAtPage(pagination.getCurrentPageIndex(), pagination);
            }
        });
        return notificationTypeSelection;
    }

    private synchronized void refreshNotificationList() {
        ServiceCallHandler.ServiceCallResult<List<Notification>> notificationsResult =
                SharedContext.getContext().getServiceCallHandler(parent).getNotifications(2000);

        if (notificationsResult.isOk()) {
            notificationRows.setValue(notificationsResult.getOk().stream().map(NotificationRow::new).toList());
            mainVBox.getChildren().remove(errorLabel);
            if (!mainVBox.getChildren().contains(tableView) || !mainVBox.getChildren().contains(pagination)) {
                mainVBox.getChildren().remove(tableView);
                mainVBox.getChildren().remove(pagination);
                mainVBox.getChildren().add(tableView);
                mainVBox.getChildren().add(pagination);
            }
        } else {
            notificationRows.setValue(Collections.emptyList());
            mainVBox.getChildren().remove(tableView);
            mainVBox.getChildren().remove(pagination);
            if(!mainVBox.getChildren().contains(errorLabel)) {
                mainVBox.getChildren().add(errorLabel);
            }
        }
    }

    private synchronized void refreshFilteredNotificationList() {
        filteredNotifications.setValue(notificationRows.getValue().stream().filter( notificationRow -> {
            Notification.Type typeFilterValue = notificationTypeFilter.getValue();
            if (typeFilterValue != null) {
                return notificationRow.getNotification().getType() == typeFilterValue;
            } else {
                return true;
            }
        }).filter( (notificationRow -> {
            String filter = Optional.ofNullable(filterText.getValue())
                    .map(String::trim)
                    .map(String::toLowerCase).orElse("");
            if (!filter.isEmpty()) {
                return notificationRow.getTypeColumn().toLowerCase().contains(filter) ||
                        notificationRow.getMessageColumn().toLowerCase().contains(filter) ||
                        notificationRow.getLocalDirColumn().toLowerCase().contains(filter) ||
                        notificationRow.getFileColumn().toLowerCase().contains(filter) ||
                        notificationRow.getDateAndTimeColumn().toLowerCase().contains(filter);
            } else {
                return true;
            }
        })).toList());
    }

    public static final Callback<TableColumn<NotificationRow,String>, TableCell<NotificationRow,String>> WRAPPING_CELL_FACTORY =
            new Callback<>() {
                @Override public TableCell<NotificationRow,String> call(TableColumn<NotificationRow,String> param) {
                    TextArea textArea = new TextArea();

                    TableCell<NotificationRow,String> tableCell = new TableCell<>() {
                        @Override
                        protected void updateItem(String s, boolean b) {
                            super.updateItem(s, b);
                            textArea.setText(s);
                        }
                    };

                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    textArea.getStyleClass().add(DisplaySettings.TEXT_AREA_TABLE_CELL_CLASS);
                    tableCell.setGraphic(textArea);
                    return tableCell;
                }
            };

    @Value
    public static class NotificationRow {
        Notification notification;

        String typeColumn;
        String messageColumn;
        String localDirColumn;
        String fileColumn;
        String dateAndTimeColumn;

        public NotificationRow(@NonNull Notification notification) {
            I18n i18n = SharedContext.getContext().getI18n();

            this.notification = notification;
            this.typeColumn = switch (notification.getType()) {
                case Conflict -> i18n.get("main_panel.notification_table.type_filter.conflict");
                case JobStopped -> i18n.get("main_panel.notification_table.type_filter.job_stopped");
                case JobException -> i18n.get("main_panel.notification_table.type_filter.job_exception");
            };
            this.messageColumn = switch (notification.getType()) {
                case JobStopped -> i18n.get("notification_panel.notification_table.job_stopped_message");
                case JobException -> i18n.get("notification_panel.notification_table.job_exception_message", notification.getMessage());
                case Conflict -> i18n.get("notification_panel.notification_table.conflict_message", getNormalizedLocalFile(notification) + SyncOperation.CONFLICT_FILE_SUFFIX);
            };
            this.localDirColumn = notification.getLocalDirectory();
            this.fileColumn = getNormalizedLocalFile(notification);
            this.dateAndTimeColumn = Instant.ofEpochMilli(notification.getTimestamp()).atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
        }
    }
}
