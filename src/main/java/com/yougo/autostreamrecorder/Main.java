package com.yougo.autostreamrecorder;

import com.yougo.autostreamrecorder.config.AppSettings;
import com.yougo.autostreamrecorder.config.ChannelConfig;
import com.yougo.autostreamrecorder.core.MonitoringService;
import com.yougo.autostreamrecorder.core.StreamMonitor;
import com.yougo.autostreamrecorder.ui.AddChannelDialog;
import java.util.Optional;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

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
            // Consume the event to prevent immediate closure
            e.consume();
            
            // Show shutdown modal
            showShutdownModal(primaryStage);
        });
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: #f0f0f0;");

        Button addChannelBtn = new Button("Add Channel");
        Button editChannelBtn = new Button("Edit Selected");
        Button settingsBtn = new Button("Settings");
        Button removeChannelBtn = new Button("Remove Selected");
        Button clearLogsBtn = new Button("Clear Logs");

        // Add button actions
        addChannelBtn.setOnAction(e -> showAddChannelDialog());
        editChannelBtn.setOnAction(e -> editSelectedChannel());
        settingsBtn.setOnAction(e -> showSettingsDialog());
        removeChannelBtn.setOnAction(e -> removeSelectedChannel());
        clearLogsBtn.setOnAction(e -> logArea.clear());

        toolbar.getChildren().addAll(
            addChannelBtn, 
            editChannelBtn, 
            removeChannelBtn, 
            new Separator(), 
            clearLogsBtn, 
            new Separator(), 
            settingsBtn
        );
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
        
        table.setRowFactory(tv -> {
            TableRow<ChannelEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ChannelEntry selected = row.getItem();
                    showEditChannelDialog(selected);
                }
            });
            return row;
        });
        
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
            // Show confirmation dialog
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Remove Channel");
            confirmDialog.setHeaderText("Confirm Channel Removal");
            confirmDialog.setContentText(String.format(
                "Are you sure you want to remove the channel:\n\n" +
                "Platform: %s\n" +
                "Channel: %s\n\n" +
                "This action cannot be undone.",
                selected.getPlatform(), 
                selected.getChannelName()
            ));
            
            // Customize button text
            ButtonType removeButton = new ButtonType("Remove", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmDialog.getButtonTypes().setAll(removeButton, cancelButton);
            
            // Show dialog and wait for user response
            Optional<ButtonType> result = confirmDialog.showAndWait();
            
            if (result.isPresent() && result.get() == removeButton) {
                // User confirmed removal
                // Stop monitoring if it's currently being monitored
                if (monitoringService.isMonitoring(selected)) {
                    monitoringService.stopMonitoring(selected);
                    logArea.appendText(String.format("[System] Stopped monitoring %s: %s\n", 
                        selected.getPlatform(), selected.getChannelName()));
                }
                
                channelList.remove(selected);
                saveChannelsToConfig();
                
                logArea.appendText(String.format("[System] Removed channel %s: %s\n", 
                    selected.getPlatform(), selected.getChannelName()));
            }
            // If user clicked Cancel or closed dialog, do nothing
            
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No channel selected");
            alert.setContentText("Please select a channel to remove.");
            alert.showAndWait();
        }
    }

    /**
     * Show shutdown modal while properly closing active recordings
     */
    private void showShutdownModal(Stage primaryStage) {
        // Create modal dialog
        Stage shutdownStage = new Stage();
        shutdownStage.initModality(Modality.APPLICATION_MODAL);
        shutdownStage.initOwner(primaryStage);
        shutdownStage.setTitle("Closing Application");
        shutdownStage.setResizable(false);
        
        // Prevent user from closing this modal
        shutdownStage.setOnCloseRequest(Event::consume);
        
        // Create content
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: white;");
        
        // Progress indicator
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(-1); // Indeterminate progress
        
        // Status label
        Label statusLabel = new Label("Stopping active recordings...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
        
        // Warning label
        Label warningLabel = new Label("Please wait, do not force close the application.");
        warningLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666; -fx-font-style: italic;");
        
        content.getChildren().addAll(progressIndicator, statusLabel, warningLabel);
        
        Scene scene = new Scene(content, 350, 200);
        shutdownStage.setScene(scene);
        shutdownStage.show();
        
        // Center the modal on the parent window
        shutdownStage.setX(primaryStage.getX() + (primaryStage.getWidth() - shutdownStage.getWidth()) / 2);
        shutdownStage.setY(primaryStage.getY() + (primaryStage.getHeight() - shutdownStage.getHeight()) / 2);
        
        // Perform shutdown in background thread
        Task<Void> shutdownTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Update status
                Platform.runLater(() -> statusLabel.setText("Stopping monitoring services..."));
                
                // Shutdown monitoring service
                monitoringService.shutdown();
                
                // Update status
                Platform.runLater(() -> statusLabel.setText("Finalizing shutdown..."));
                
                // Small delay to ensure everything is properly closed
                Thread.sleep(1000);
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    shutdownStage.close();
                    primaryStage.close();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    statusLabel.setText("Shutdown completed with warnings.");
                    statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #e74c3c;");
                    
                    // Still close the application after a short delay
                    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
                        shutdownStage.close();
                        primaryStage.close();
                    }));
                    timeline.play();
                });
            }
        };
        
        // Run shutdown task
        Thread shutdownThread = new Thread(shutdownTask);
        shutdownThread.setDaemon(true);
        shutdownThread.start();
    }

    private void showEditChannelDialog(ChannelEntry channelEntry) {
        Optional<ChannelEntry> result = AddChannelDialog.showDialog(channelEntry);
        result.ifPresent(edited -> {
            // Update channel fields
            channelEntry.setPlatform(edited.getPlatform());
            channelEntry.setChannelName(edited.getChannelName());
            channelEntry.setChannelUrl(edited.getChannelUrl());
            channelEntry.setQuality(edited.getQuality());
            channelEntry.setIsActive(edited.getIsActive());
            // Refresh table and save
            channelTable.refresh();
            saveChannelsToConfig();
            logArea.appendText(String.format("[System] Edited channel %s: %s\n",
                edited.getPlatform(), edited.getChannelName()));
        });
    }

    private void editSelectedChannel() {
        ChannelEntry selected = channelTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showEditChannelDialog(selected);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No channel selected");
            alert.setContentText("Please select a channel to edit.");
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}