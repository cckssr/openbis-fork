package ch.openbis.drive.gui.maincontent;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.*;
import ch.openbis.drive.model.Event;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LogsPanel extends ResizablePanel {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss z");
    private static final int EVENTS_PER_PAGE = 35;
    private static final int FIXED_CELL_SIZE = 25;

    private final VBox mainVBox;
    private final TableView<EventRow> tableView;
    private TableColumn<EventRow, String> localFileColumn;
    private TableColumn<EventRow, String> remoteFileColumn;
    private TableColumn<EventRow, String> fileTypeColumn;
    private TableColumn<EventRow, String> dateAndTimeColumn;
    private TableColumn<EventRow, String> eventTypeColumn;
    private final Pagination pagination;

    private final ErrorLabel errorLabel = new ErrorLabel();

    private final StringProperty filterText = new SimpleStringProperty();

    private final ObjectProperty<List<EventRow>> eventRows = new SimpleObjectProperty<>(Collections.emptyList());
    private final ObjectProperty<List<EventRow>> filteredEvents = new SimpleObjectProperty<>(Collections.emptyList());

    public LogsPanel(@NonNull Pane parent) {
        super(parent);

        mainVBox = new VBox();
        mainVBox.getStyleClass().add(DisplaySettings.MAIN_CONTENT_PADDED_FRAME_CLASS);
        this.getChildren().add(mainVBox);

        AnchorPane filters = getFilters();

        tableView = initializeTable();
        pagination = initializePagination();
        mainVBox.getChildren().addAll(filters, tableView, pagination);

        refreshEventList();
        refreshEventTableAtPage(0, pagination);
        applyAndLinkToTableState();
        resize();
    }

    private AnchorPane getFilters() {
        AnchorPane filters = new AnchorPane();

        HBox refreshButton = new HBox();
        refreshButton.setPadding(new Insets(0, 10, 30, 0));
        refreshButton.setSpacing(20);
        refreshButton.setAlignment(Pos.CENTER_LEFT);
        Button refresh = new RefreshButton();
        refresh.setMinSize(DisplaySettings.TOP_CONTROL_WIDTH, DisplaySettings.TOP_CONTROL_HEIGHT);
        refresh.setOnAction((e) -> {
            refreshEventList();
            refreshEventTableAtPage(pagination.getCurrentPageIndex(), pagination);
        });
        AnchorPane.setLeftAnchor(refreshButton, 0.0);
        AnchorPane.setTopAnchor(refreshButton, 0.0);
        refreshButton.getChildren().add(refresh);
        filters.getChildren().add(refreshButton);

        HBox textFilter = new HBox();
        textFilter.setPadding(new Insets(0, 0, 0, 0));
        textFilter.setSpacing(20);
        textFilter.setMinSize(DisplaySettings.TOP_CONTROL_WIDTH, DisplaySettings.TOP_CONTROL_HEIGHT);
        textFilter.setAlignment(Pos.CENTER_RIGHT);
        SearchField searchField = new SearchField(filterText);
        searchField.setMinSize(DisplaySettings.TOP_CONTROL_WIDTH, DisplaySettings.TOP_CONTROL_HEIGHT);
        textFilter.getChildren().add(searchField);
        AnchorPane.setRightAnchor(textFilter, 0.0);
        AnchorPane.setTopAnchor(textFilter, 0.0);
        filters.getChildren().add(textFilter);
        filterText.addListener((obs, oldValue, newValue) -> {
            if(newValue != null && !newValue.equals(oldValue)) {
                refreshEventTableAtPage(pagination.getCurrentPageIndex(), pagination);
            }
        });

        return filters;
    }

    private TableView<EventRow> initializeTable() {
        I18n i18n = SharedContext.getContext().getI18n();

        final TableView<EventRow> tableView;
        tableView = new TableView<>();
        tableView.setFixedCellSize(FIXED_CELL_SIZE);
        tableView.getItems().setAll(eventRows.getValue().subList(0, Math.min(EVENTS_PER_PAGE, eventRows.getValue().size())));

        localFileColumn = new TableColumn<>();
        localFileColumn.textProperty().bind(i18n.createStringBinding("log_panel.event_table.column_title.local_file"));
        localFileColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getLocalFileColumn()));
        localFileColumn.setCellFactory(SELECTABLE_CELL_FACTORY);
        localFileColumn.setPrefWidth(200);

        remoteFileColumn = new TableColumn<>();
        remoteFileColumn.textProperty().bind(i18n.createStringBinding("log_panel.event_table.column_title.remote_file"));
        remoteFileColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getRemoteFileColumn()));
        remoteFileColumn.setCellFactory(SELECTABLE_CELL_FACTORY);
        remoteFileColumn.setPrefWidth(200);

        fileTypeColumn = new TableColumn<>();
        fileTypeColumn.textProperty().bind(i18n.createStringBinding("log_panel.event_table.column_title.file_type"));
        fileTypeColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getFileTypeColumn()));
        fileTypeColumn.setCellFactory(SELECTABLE_CELL_FACTORY);
        fileTypeColumn.setPrefWidth(150);

        dateAndTimeColumn = new TableColumn<>();
        dateAndTimeColumn.textProperty().bind(i18n.createStringBinding("log_panel.event_table.column_title.date_and_time"));
        dateAndTimeColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getDateAndTimeColumn()));
        dateAndTimeColumn.setCellFactory(SELECTABLE_CELL_FACTORY);
        dateAndTimeColumn.setPrefWidth(200);

        eventTypeColumn = new TableColumn<>();
        eventTypeColumn.textProperty().bind(i18n.createStringBinding("log_panel.event_table.column_title.event_type"));
        eventTypeColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getEventTypeColumn()));
        eventTypeColumn.setCellFactory(SELECTABLE_CELL_FACTORY);
        eventTypeColumn.setPrefWidth(200);

        tableView.getColumns().addAll(List.of(localFileColumn, remoteFileColumn, fileTypeColumn, dateAndTimeColumn, eventTypeColumn));
        return tableView;
    }

    private void applyTableState() {
        SharedContext sharedContext = SharedContext.getContext();
        Optional.ofNullable(sharedContext.getColumSize(SharedContext.LogTableColumn.LOCAL_FILE)).ifPresent(
                localFileColumn::setPrefWidth
        );
        Optional.ofNullable(sharedContext.getColumSize(SharedContext.LogTableColumn.REMOTE_FILE)).ifPresent(
                remoteFileColumn::setPrefWidth
        );
        Optional.ofNullable(sharedContext.getColumSize(SharedContext.LogTableColumn.FILE_TYPE)).ifPresent(
                fileTypeColumn::setPrefWidth
        );
        Optional.ofNullable(sharedContext.getColumSize(SharedContext.LogTableColumn.DATE_TIME)).ifPresent(
                dateAndTimeColumn::setPrefWidth
        );
        Optional.ofNullable(sharedContext.getColumSize(SharedContext.LogTableColumn.EVENT)).ifPresent(
                eventTypeColumn::setPrefWidth
        );
        Optional.ofNullable(sharedContext.getLogTableSorting()).ifPresent( sortingPair -> {
            switch (sortingPair.getKey()) {
                case LOCAL_FILE -> {
                    localFileColumn.setSortType(sortingPair.getValue());
                    tableView.getSortOrder().clear();
                    tableView.getSortOrder().add(localFileColumn);
                }
                case REMOTE_FILE -> {
                    remoteFileColumn.setSortType(sortingPair.getValue());
                    tableView.getSortOrder().clear();
                    tableView.getSortOrder().add(remoteFileColumn);
                }
                case FILE_TYPE -> {
                    fileTypeColumn.setSortType(sortingPair.getValue());
                    tableView.getSortOrder().clear();
                    tableView.getSortOrder().add(fileTypeColumn);
                }
                case DATE_TIME -> {
                    dateAndTimeColumn.setSortType(sortingPair.getValue());
                    tableView.getSortOrder().clear();
                    tableView.getSortOrder().add(dateAndTimeColumn);
                }
                case EVENT -> {
                    eventTypeColumn.setSortType(sortingPair.getValue());
                    tableView.getSortOrder().clear();
                    tableView.getSortOrder().add(eventTypeColumn);
                }
            }
            tableView.sort();
        });
    }

    private void applyAndLinkToTableState() {
        SharedContext sharedContext = SharedContext.getContext();
        applyTableState();

        localFileColumn.widthProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                sharedContext.setColumSize(SharedContext.LogTableColumn.LOCAL_FILE, newValue.intValue());
            }
        });
        remoteFileColumn.widthProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                sharedContext.setColumSize(SharedContext.LogTableColumn.REMOTE_FILE, newValue.intValue());
            }
        });
        fileTypeColumn.widthProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                sharedContext.setColumSize(SharedContext.LogTableColumn.FILE_TYPE, newValue.intValue());
            }
        });
        dateAndTimeColumn.widthProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                sharedContext.setColumSize(SharedContext.LogTableColumn.DATE_TIME, newValue.intValue());
            }
        });
        eventTypeColumn.widthProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                sharedContext.setColumSize(SharedContext.LogTableColumn.EVENT, newValue.intValue());
            }
        });

        tableView.getSortOrder().addListener(new ListChangeListener<TableColumn<LogsPanel.EventRow, ?>>() {
            @Override
            public void onChanged(Change<? extends TableColumn<LogsPanel.EventRow, ?>> c) {
                if (tableView.getSortOrder().contains(localFileColumn)) {
                    sharedContext.setSortedColumn(SharedContext.LogTableColumn.LOCAL_FILE, localFileColumn.getSortType());
                } else if (tableView.getSortOrder().contains(remoteFileColumn)) {
                    sharedContext.setSortedColumn(SharedContext.LogTableColumn.REMOTE_FILE, remoteFileColumn.getSortType());
                } else if (tableView.getSortOrder().contains(fileTypeColumn)) {
                    sharedContext.setSortedColumn(SharedContext.LogTableColumn.FILE_TYPE, fileTypeColumn.getSortType());
                } else if (tableView.getSortOrder().contains(dateAndTimeColumn)) {
                    sharedContext.setSortedColumn(SharedContext.LogTableColumn.DATE_TIME, dateAndTimeColumn.getSortType());
                } else if (tableView.getSortOrder().contains(eventTypeColumn)) {
                    sharedContext.setSortedColumn(SharedContext.LogTableColumn.EVENT, eventTypeColumn.getSortType());
                } else {
                    sharedContext.setLogTableUnsorted();
                    Platform.runLater( () -> refreshEventTableAtPage(pagination.getCurrentPageIndex(), pagination) );
                }
            }
        });
        localFileColumn.sortTypeProperty().addListener((obs, oldValue, newValue) -> {
            sharedContext.setSortedColumn(SharedContext.LogTableColumn.LOCAL_FILE, newValue);
        });
        remoteFileColumn.sortTypeProperty().addListener((obs, oldValue, newValue) -> {
            sharedContext.setSortedColumn(SharedContext.LogTableColumn.REMOTE_FILE, newValue);
        });
        fileTypeColumn.sortTypeProperty().addListener((obs, oldValue, newValue) -> {
            sharedContext.setSortedColumn(SharedContext.LogTableColumn.FILE_TYPE, newValue);
        });
        dateAndTimeColumn.sortTypeProperty().addListener((obs, oldValue, newValue) -> {
            sharedContext.setSortedColumn(SharedContext.LogTableColumn.DATE_TIME, newValue);
        });
        eventTypeColumn.sortTypeProperty().addListener((obs, oldValue, newValue) -> {
            sharedContext.setSortedColumn(SharedContext.LogTableColumn.EVENT, newValue);
        });
    }

    private synchronized void refreshEventList() {
        //tableView.getSortOrder().clear(); //Uncomment this, to clear sorting upon table-data-refreshing
        ServiceCallHandler.ServiceCallResult<List<? extends Event>> eventListResult = SharedContext.getContext().getServiceCallHandler(parent).getEvents(2000);
        if (eventListResult.isOk()) {
            eventRows.setValue(eventListResult.getOk().stream().map(EventRow::new).toList());
            mainVBox.getChildren().remove(errorLabel);
            if (!mainVBox.getChildren().contains(tableView) || !mainVBox.getChildren().contains(pagination)) {
                mainVBox.getChildren().remove(tableView);
                mainVBox.getChildren().remove(pagination);
                mainVBox.getChildren().add(tableView);
                mainVBox.getChildren().add(pagination);
            }
        } else {
            eventRows.setValue(Collections.emptyList());
            mainVBox.getChildren().remove(tableView);
            mainVBox.getChildren().remove(pagination);
            if(!mainVBox.getChildren().contains(errorLabel)) {
                mainVBox.getChildren().add(errorLabel);
            }
        }
    }

    private synchronized void refreshFilteredEventList() {
        filteredEvents.setValue(eventRows.getValue().stream().filter( (eventRow -> {
            String filter = Optional.ofNullable(filterText.getValue())
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .orElse("");
            if (!filter.isEmpty()) {
                return eventRow.getEventTypeColumn().toLowerCase().contains(filter) ||
                        eventRow.getFileTypeColumn().toLowerCase().contains(filter) ||
                        eventRow.getLocalFileColumn().toLowerCase().contains(filter) ||
                        eventRow.getRemoteFileColumn().toLowerCase().contains(filter) ||
                        eventRow.getDateAndTimeColumn().toLowerCase().contains(filter);
            } else {
                return true;
            }
        })).toList());
    }

    private Pagination initializePagination() {
        LogsPanel self = this;
        final Pagination pagination;
        pagination = new Pagination(filteredEvents.getValue().size() / EVENTS_PER_PAGE, 0);
        pagination.setPageCount((filteredEvents.getValue().size() / EVENTS_PER_PAGE ) + 1);
        pagination.currentPageIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                refreshEventTableAtPage(newValue, pagination);
            }
        });
        return pagination;
    }

    private void refreshEventTableAtPage(Number newValue, Pagination pagination) {
        refreshFilteredEventList();
        List<EventRow> refreshedEventList = filteredEvents.get();
        int newOffset = newValue.intValue() * EVENTS_PER_PAGE;
        if (newOffset < refreshedEventList.size()) {
            tableView.getItems().setAll(refreshedEventList.subList(newOffset, Math.min(refreshedEventList.size(), newOffset + EVENTS_PER_PAGE)));
        } else {
            tableView.getItems().setAll(Collections.emptyList());
        }
        pagination.setPageCount((refreshedEventList.size() / EVENTS_PER_PAGE) + 1);
        applyTableState();
    }

    @Override
    protected synchronized void resize() {
        mainVBox.setMinSize(500, parent.getHeight());
        mainVBox.setMaxSize(parent.getWidth(), parent.getHeight());
        mainVBox.setPrefSize(parent.getWidth(), parent.getHeight());

        tableView.setMinSize(parent.getWidth() - 80, 80);
        tableView.setMaxSize(parent.getWidth() - 80, parent.getHeight() - 80);
        tableView.setPrefSize(parent.getWidth() - 80, 32 + EVENTS_PER_PAGE * FIXED_CELL_SIZE);
    }

    public static final Callback<TableColumn<EventRow,String>, TableCell<EventRow,String>> SELECTABLE_CELL_FACTORY =
            new Callback<>() {
                @Override public TableCell<EventRow,String> call(TableColumn<EventRow,String> param) {
                    TextArea textArea = new TextArea();

                    TableCell<EventRow,String> tableCell = new TableCell<>() {
                        @Override
                        protected void updateItem(String s, boolean b) {
                            super.updateItem(s, b);
                            textArea.setText(s);
                        }
                    };

                    textArea.setEditable(false);
                    textArea.setWrapText(false);
                    textArea.getStyleClass().add(DisplaySettings.TEXT_AREA_TABLE_CELL_CLASS);
                    tableCell.setGraphic(textArea);
                    return tableCell;
                }
            };

    @Value
    public static class EventRow {
        Event event;

        String localFileColumn;
        String remoteFileColumn;
        String fileTypeColumn;
        String dateAndTimeColumn;
        String eventTypeColumn;

        public EventRow(@NonNull Event event) {
            I18n i18n = SharedContext.getContext().getI18n();

            this.event = event;

            this.localFileColumn = event.isSourceDeleted() && event.getSyncDirection() == Event.SyncDirection.DOWN ?
                    "" : event.getLocalFile();
            this.remoteFileColumn = event.isSourceDeleted() && event.getSyncDirection() == Event.SyncDirection.UP ?
                    "" : event.getRemoteFile();
            this.fileTypeColumn = event.isDirectory() ?
                    i18n.get("log_panel.event_table.file_type.directory") : i18n.get("log_panel.event_table.file_type.file");
            this.dateAndTimeColumn = Instant.ofEpochMilli(event.getTimestamp()).atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
            this.eventTypeColumn = event.isSourceDeleted() ?
                    i18n.get("log_panel.event_table.column_title.deletion") :
                    (event.getSyncDirection() == Event.SyncDirection.UP ?
                            i18n.get("log_panel.event_table.column_title.upload") : i18n.get("log_panel.event_table.column_title.download"));

        }
    }
}
