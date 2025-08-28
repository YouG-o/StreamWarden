package com.yougo.autostreamrecorder;

import com.yougo.autostreamrecorder.config.AppSettings;
import com.yougo.autostreamrecorder.config.ChannelConfig;
import com.yougo.autostreamrecorder.core.MonitoringService;
import com.yougo.autostreamrecorder.core.StreamMonitor;
import com.yougo.autostreamrecorder.ui.AddChannelDialog;
import java.util.Optional;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {
    
    private TableView<ChannelEntry> channelTable;
    private ObservableList<ChannelEntry> channelList;
    private TextArea logArea;
    private MonitoringService monitoringService;
    private AppSettings appSettings;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("AutoStreamRecorder");
        
        // Load settings
        appSettings = AppSettings.load();
        
        // Initialize monitoring service
        monitoringService = new MonitoringService(appSettings);
        monitoringService.setStatusCallback(new StreamMonitor.StatusCallback() {
            @Override
            public void onStatusChanged(ChannelEntry channel, String status) {
                // Status is already updated in the channel entry via Platform.runLater in StreamMonitor
                channelTable.refresh();
            }
            
            @Override
            public void onLogMessage(String message) {
                logArea.appendText(message + "\n");
            }
        });
        
        // Create main layout
        BorderPane root = new BorderPane();
        
        // Create top toolbar
        HBox toolbar = createToolbar();
        root.setTop(toolbar);
        
        // Create center content (table + logs)
        VBox centerContent = createCenterContent();
        root.setCenter(centerContent);
        
        // Load saved channels and start monitoring
        loadChannelsFromConfig();
        startAutoMonitoring();
        
        // Create scene
        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Shutdown monitoring service when closing
        primaryStage.setOnCloseRequest(e -> {
            monitoringService.shutdown();
        });
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: #f0f0f0;");
        
        Button addChannelBtn = new Button("Add Channel");
        Button settingsBtn = new Button("Settings");
        Button removeChannelBtn = new Button("Remove Selected");
        Button clearLogsBtn = new Button("Clear Logs");
        
        // Add button actions
        addChannelBtn.setOnAction(e -> showAddChannelDialog());
        settingsBtn.setOnAction(e -> showSettingsDialog());
        removeChannelBtn.setOnAction(e -> removeSelectedChannel());
        clearLogsBtn.setOnAction(e -> logArea.clear());
        
        toolbar.getChildren().addAll(addChannelBtn, removeChannelBtn, new Separator(), 
                                    clearLogsBtn, new Separator(), settingsBtn);
        return toolbar;
    }
    
    private VBox createCenterContent() {
        VBox centerContent = new VBox(10);
        centerContent.setPadding(new Insets(10));
        
        // Channel table
        channelTable = createChannelTable();
        
        // Log area
        Label logLabel = new Label("Activity Logs:");
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(8);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: monospace;");
        
        centerContent.getChildren().addAll(channelTable, logLabel, logArea);
        return centerContent;
    }
    
    private TableView<ChannelEntry> createChannelTable() {
        TableView<ChannelEntry> table = new TableView<>();
        
        // Create columns
        TableColumn<ChannelEntry, String> platformCol = new TableColumn<>("Platform");
        platformCol.setCellValueFactory(new PropertyValueFactory<>("platform"));
        platformCol.setPrefWidth(80);
        
        TableColumn<ChannelEntry, String> nameCol = new TableColumn<>("Channel Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("channelName"));
        nameCol.setPrefWidth(150);
        
        TableColumn<ChannelEntry, String> urlCol = new TableColumn<>("Channel URL");
        urlCol.setCellValueFactory(new PropertyValueFactory<>("channelUrl"));
        urlCol.setPrefWidth(250);
        
        TableColumn<ChannelEntry, Boolean> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(new PropertyValueFactory<>("isActive"));
        activeCol.setPrefWidth(60);
        
        TableColumn<ChannelEntry, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        
        // Custom cell factory for status column with colors
        statusCol.setCellFactory(column -> {
            return new TableCell<ChannelEntry, String>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    
                    if (empty || getIndex() >= getTableView().getItems().size()) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    
                    ChannelEntry channel = getTableView().getItems().get(getIndex());
                    
                    // Only show status if channel is active (being monitored)
                    if (!channel.getIsActive() || status == null || status.isEmpty()) {
                        setText("");
                        setStyle("");
                    } else {
                        setText(status);
                        
                        // Apply colors based on status
                        switch (status) {
                            case "Recording":
                                setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                                break;
                            case "Offline":
                                setStyle("-fx-text-fill: #e74c3c;");
                                break;
                            default:
                                setStyle("");
                                break;
                        }
                    }
                }
            };
        });
        
        TableColumn<ChannelEntry, String> qualityCol = new TableColumn<>("Quality");
        qualityCol.setCellValueFactory(new PropertyValueFactory<>("quality"));
        qualityCol.setPrefWidth(80);
        
        // Add columns to table
        table.getColumns().addAll(platformCol, nameCol, urlCol, activeCol, statusCol, qualityCol);
        
        // Initialize data
        channelList = FXCollections.observableArrayList();
        table.setItems(channelList);
        
        return table;
    }
    
    /**
     * Load channels from JSON configuration file
     */
    private void loadChannelsFromConfig() {
        var channelDataList = ChannelConfig.loadChannels();
        var loadedChannels = ChannelConfig.toObservableList(channelDataList);
        channelList.addAll(loadedChannels);
    }
    
    /**
     * Save current channels to JSON configuration file
     */
    private void saveChannelsToConfig() {
        var channelDataList = ChannelConfig.fromObservableList(channelList);
        ChannelConfig.saveChannels(channelDataList);
    }
    
    /**
     * Start monitoring for all active channels
     */
    private void startAutoMonitoring() {
        if (appSettings.isAutoStartMonitoring()) {
            monitoringService.startAllActiveChannels(channelList);
            logArea.appendText("[System] Auto-monitoring started for active channels\n");
        }
    }
    
    private void showAddChannelDialog() {
        Optional<ChannelEntry> result = AddChannelDialog.showDialog();
        result.ifPresent(channelEntry -> {
            channelList.add(channelEntry);
            saveChannelsToConfig();
            
            // If the new channel is active, start monitoring it immediately
            if (channelEntry.getIsActive()) {
                monitoringService.startMonitoring(channelEntry, appSettings.getDefaultCheckInterval());
                logArea.appendText(String.format("[System] Started monitoring %s: %s\n", 
                    channelEntry.getPlatform(), channelEntry.getChannelName()));
            }
        });
    }
    
    private void showSettingsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText("Settings Dialog");
        alert.setContentText("This dialog will be implemented later.");
        alert.showAndWait();
    }
    
    private void removeSelectedChannel() {
        ChannelEntry selected = channelTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Stop monitoring if it's currently being monitored
            if (monitoringService.isMonitoring(selected)) {
                monitoringService.stopMonitoring(selected);
                logArea.appendText(String.format("[System] Stopped monitoring %s: %s\n", 
                    selected.getPlatform(), selected.getChannelName()));
            }
            
            channelList.remove(selected);
            saveChannelsToConfig();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No channel selected");
            alert.setContentText("Please select a channel to remove.");
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}