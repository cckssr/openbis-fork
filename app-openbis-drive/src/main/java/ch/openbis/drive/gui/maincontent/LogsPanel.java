package ch.openbis.drive.gui.maincontent;

import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.gui.util.*;
import ch.openbis.drive.model.Event;
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
    private static final int EVENTS_PER_PAGE = 20;
    private static final int FIXED_CELL_SIZE = 25;

    private final VBox mainVBox;
    private final TableView<EventRow> tableView;
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
        resize();
    }

    private AnchorPane getFilters() {
        AnchorPane filters = new AnchorPane();

        HBox refreshButton = new HBox();
        refreshButton.setPadding(new Insets(15, 15, 15, 15));
        refreshButton.setSpacing(20);
        refreshButton.setAlignment(Pos.CENTER_LEFT);
        Button refresh = new RefreshButton();
        refresh.setOnAction((e) -> {
            refreshEventList();
            refreshEventTableAtPage(pagination.getCurrentPageIndex(), pagination);
        });
        AnchorPane.setLeftAnchor(refreshButton, 0.0);
        AnchorPane.setTopAnchor(refreshButton, 0.0);
        refreshButton.getChildren().add(refresh);
        filters.getChildren().add(refreshButton);

        HBox textFilter = new HBox();
        textFilter.setPadding(new Insets(15, 15, 15, 15));
        textFilter.setSpacing(20);
        textFilter.setAlignment(Pos.CENTER_RIGHT);
        SearchField searchField = new SearchField(filterText);
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

        TableColumn<EventRow, String> localFileColumn = new TableColumn<>();
        localFileColumn.textProperty().bind(i18n.createStringBinding("log_panel.event_table.column_title.local_file"));
        localFileColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getLocalFileColumn()));
        localFileColumn.setCellFactory(SELECTABLE_CELL_FACTORY);
        localFileColumn.setPrefWidth(200);

        TableColumn<EventRow, String> remoteFileColumn = new TableColumn<>();
        remoteFileColumn.textProperty().bind(i18n.createStringBinding("log_panel.event_table.column_title.remote_file"));
        remoteFileColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getRemoteFileColumn()));
        remoteFileColumn.setCellFactory(SELECTABLE_CELL_FACTORY);
        remoteFileColumn.setPrefWidth(200);

        TableColumn<EventRow, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.textProperty().bind(i18n.createStringBinding("log_panel.event_table.column_title.file_type"));
        fileTypeColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getFileTypeColumn()));
        fileTypeColumn.setCellFactory(SELECTABLE_CELL_FACTORY);
        fileTypeColumn.setPrefWidth(150);

        TableColumn<EventRow, String> dateAndTimeColumn = new TableColumn<>();
        dateAndTimeColumn.textProperty().bind(i18n.createStringBinding("log_panel.event_table.column_title.date_and_time"));
        dateAndTimeColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getDateAndTimeColumn()));
        dateAndTimeColumn.setCellFactory(SELECTABLE_CELL_FACTORY);
        dateAndTimeColumn.setPrefWidth(200);

        TableColumn<EventRow, String> eventTypeColumn = new TableColumn<>();
        eventTypeColumn.textProperty().bind(i18n.createStringBinding("log_panel.event_table.column_title.event_type"));
        eventTypeColumn.setCellValueFactory(eventData -> new ReadOnlyObjectWrapper<>(eventData.getValue().getEventTypeColumn()));
        eventTypeColumn.setCellFactory(SELECTABLE_CELL_FACTORY);
        eventTypeColumn.setPrefWidth(200);

        tableView.getColumns().addAll(List.of(localFileColumn, remoteFileColumn, fileTypeColumn, dateAndTimeColumn, eventTypeColumn));
        return tableView;
    }

    private synchronized void refreshEventList() {
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
    }

    @Override
    protected synchronized void resize() {
        mainVBox.setMinSize(500, parent.getHeight());
        mainVBox.setMaxSize(parent.getWidth(), parent.getHeight());
        mainVBox.setPrefSize(parent.getWidth(), parent.getHeight());

        tableView.setMinSize(parent.getWidth() - 80, 80);
        tableView.setMaxSize(parent.getWidth() - 80, parent.getHeight() - 80);
        tableView.setPrefSize(parent.getWidth() - 80, (EVENTS_PER_PAGE + 1.1) * FIXED_CELL_SIZE);
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

            this.localFileColumn = event.getLocalFile();
            this.remoteFileColumn = !event.isSourceDeleted() ? event.getRemoteFile() : "";
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
