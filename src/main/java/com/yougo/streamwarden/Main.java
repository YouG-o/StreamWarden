/* 
 * Copyright (C) 2025-present YouGo (https://github.com/youg-o)
 * This program is licensed under the GNU Affero General Public License v3.0.
 * You may redistribute it and/or modify it under the terms of the license.
 * 
 * Attribution must be given to the original author.
 * This program is distributed without any warranty; see the license for details.
 */


package com.yougo.streamwarden;

import com.yougo.streamwarden.config.AppSettings;
import com.yougo.streamwarden.config.ChannelConfig;
import com.yougo.streamwarden.core.MonitoringService;
import com.yougo.streamwarden.core.StreamMonitor;
import com.yougo.streamwarden.ui.AddChannelDialog;
import com.yougo.streamwarden.ui.SettingsDialog;
import com.yougo.streamwarden.ui.TraySupport;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {
    
    private TableView<ChannelEntry> channelTable;
    private ObservableList<ChannelEntry> channelList;
    private TextArea logArea;
    private Label logLabel;
    private CheckBox showLogsCheckBox;
    private MonitoringService monitoringService;
    private AppSettings appSettings;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("StreamWarden");
        
        // Load settings
        appSettings = AppSettings.load();
        
        // Check Streamlink version and show alert if Kick is not supported
        checkStreamlinkVersion();
        
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
        
        // Shutdown monitoring service when closing (via window close button)
        primaryStage.setOnCloseRequest(e -> {
            // Prevent immediate closure; run our graceful shutdown and real exit
            e.consume();
            requestAppExit(primaryStage);
        });
        
        // Set application icon
        try {
            javafx.scene.image.Image appIcon = new javafx.scene.image.Image(
                getClass().getResourceAsStream("/assets/icons/app_icon.png")
            );
            primaryStage.getIcons().add(appIcon);
            // Optionally log success
            System.out.println("Application icon loaded successfully");
        } catch (Exception e) {
            System.err.println("Failed to load application icon: " + e.getMessage());
        }
        
        // Install/uninstall tray integration based on settings
        updateTrayIntegration(primaryStage);

        // If minimized and minimizeToTray is enabled, hide to tray (no taskbar entry)
        primaryStage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
            if (isIconified && appSettings.isMinimizeToTray() && TraySupport.isInstalled()) {
                primaryStage.hide();
                TraySupport.displayInfo("StreamWarden", "Still running in the system tray");
            }
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
        Button supportBtn = new Button("Support me 💌");

        // Add button actions
        addChannelBtn.setOnAction(e -> showAddChannelDialog());
        editChannelBtn.setOnAction(e -> editSelectedChannel());
        settingsBtn.setOnAction(e -> showSettingsDialog());
        removeChannelBtn.setOnAction(e -> removeSelectedChannel());
        supportBtn.setOnAction(e -> showSupportDialog());

        // Create spacer to push support button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(
            addChannelBtn, 
            editChannelBtn, 
            removeChannelBtn, 
            new Separator(), 
            settingsBtn,
            spacer,
            supportBtn
        );
        return toolbar;
    }
    
    private VBox createCenterContent() {
        VBox centerContent = new VBox(10);
        centerContent.setPadding(new Insets(10));
        
        // Channel table
        channelTable = createChannelTable();
        VBox.setVgrow(channelTable, Priority.ALWAYS); // Make table grow to fill available space
        
        // Log area
        logLabel = new Label("Activity Logs:");
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(8);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: monospace;");
        
        // Create log controls container
        HBox logControls = createLogControls();
        
        centerContent.getChildren().addAll(channelTable, logControls);
        
        // Add log components if enabled
        if (appSettings.isShowActivityLogs()) {
            centerContent.getChildren().addAll(logLabel, logArea);
            VBox.setVgrow(channelTable, Priority.SOMETIMES);
        }
        
        return centerContent;
    }
    
    /**
     * Create log controls container with checkbox and clear button
     */
    private HBox createLogControls() {
        HBox logControls = new HBox(10);
        logControls.setAlignment(Pos.CENTER_LEFT);
        
        // Show logs checkbox
        showLogsCheckBox = new CheckBox("Show Activity Logs");
        showLogsCheckBox.setSelected(appSettings.isShowActivityLogs());
        showLogsCheckBox.setOnAction(e -> toggleActivityLogs());
        
        // Create spacer to push clear button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Clear logs button
        Button clearLogsBtn = new Button("Clear Logs");
        clearLogsBtn.setOnAction(e -> logArea.clear());
        
        // Only show clear button if logs are visible
        if (appSettings.isShowActivityLogs()) {
            logControls.getChildren().addAll(showLogsCheckBox, spacer, clearLogsBtn);
        } else {
            logControls.getChildren().add(showLogsCheckBox);
        }
        
        return logControls;
    }
    
    /**
     * Toggle visibility of activity logs
     */
    private void toggleActivityLogs() {
        boolean showLogs = showLogsCheckBox.isSelected();
        appSettings.setShowActivityLogs(showLogs);
        
        VBox centerContent = (VBox) ((BorderPane) channelTable.getParent().getParent()).getCenter();
        
        // Find the log controls container
        HBox logControls = (HBox) centerContent.getChildren().get(1); // Second child after table
        
        if (showLogs) {
            // Add log components if not already present
            if (!centerContent.getChildren().contains(logLabel)) {
                centerContent.getChildren().addAll(logLabel, logArea);
                VBox.setVgrow(channelTable, Priority.SOMETIMES);
            }
            
            // Add clear button to log controls if not present
            if (logControls.getChildren().size() == 1) {
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                
                Button clearLogsBtn = new Button("Clear Logs");
                clearLogsBtn.setOnAction(e -> logArea.clear());
                
                logControls.getChildren().addAll(spacer, clearLogsBtn);
            }
        } else {
            // Remove log components
            centerContent.getChildren().removeAll(logLabel, logArea);
            VBox.setVgrow(channelTable, Priority.ALWAYS);
            
            // Remove only clear button and spacer from log controls (keep checkbox)
            if (logControls.getChildren().size() > 1) {
                // Create a new list with only the checkbox (first element)
                CheckBox checkbox = (CheckBox) logControls.getChildren().get(0);
                logControls.getChildren().clear();
                logControls.getChildren().add(checkbox);
            }
        }
    }
    
    private TableView<ChannelEntry> createChannelTable() {
        TableView<ChannelEntry> table = new TableView<>();
        
        // Create columns
        TableColumn<ChannelEntry, String> platformCol = new TableColumn<>("Platform");
        platformCol.setCellValueFactory(new PropertyValueFactory<>("platform"));
        platformCol.setPrefWidth(80);
        
        // Custom cell factory for platform column to display icons
        platformCol.setCellFactory(column -> {
            return new TableCell<ChannelEntry, String>() {
                @Override
                protected void updateItem(String platform, boolean empty) {
                    super.updateItem(platform, empty);
                    
                    if (empty || platform == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    
                    // Load platform icon
                    ImageView icon = loadPlatformIcon(platform);
                    if (icon != null) {
                        setText(""); // Remove text, show only icon
                        setGraphic(icon);
                        
                        // Center the icon in the cell
                        setAlignment(Pos.CENTER);
                        
                        // Add tooltip with platform name for accessibility
                        Tooltip tooltip = new Tooltip(platform);
                        setTooltip(tooltip);
                    } else {
                        // Fallback to text if icon can't be loaded
                        setText(platform);
                        setGraphic(null);
                        setTooltip(null);
                    }
                }
            };
        });
        
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
        
        // Create context menu for right-click actions
        ContextMenu contextMenu = createChannelContextMenu();
        
        table.setRowFactory(tv -> {
            TableRow<ChannelEntry> row = new TableRow<>();
            
            // Double-click to edit
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ChannelEntry selected = row.getItem();
                    showEditChannelDialog(selected);
                }
            });
            
            // Context menu for right-click (only show on non-empty rows)
            row.setContextMenu(null);
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    row.setContextMenu(contextMenu);
                } else {
                    row.setContextMenu(null);
                }
            });
            
            return row;
        });
        
        return table;
    }
    
    /**
     * Create context menu for channel table rows
     */
    private ContextMenu createChannelContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        // Edit menu item
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> {
            ChannelEntry selected = channelTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditChannelDialog(selected);
            }
        });
        
        // Remove menu item
        MenuItem removeItem = new MenuItem("Remove");
        removeItem.setOnAction(e -> {
            ChannelEntry selected = channelTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                removeSelectedChannel();
            }
        });
        
        // Open in browser menu item
        MenuItem openBrowserItem = new MenuItem("Open in Browser");
        openBrowserItem.setOnAction(e -> {
            ChannelEntry selected = channelTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openChannelInBrowser(selected);
            }
        });
        
        // Add separator for visual grouping
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
        contextMenu.getItems().addAll(editItem, removeItem, separator, openBrowserItem);
        
        return contextMenu;
    }
    
        /**
     * Open channel URL in default system browser
     */
    private void openChannelInBrowser(ChannelEntry channel) {
        // Execute browser opening in a separate thread to avoid freezing UI
        Task<Void> browserTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    String url = channel.getChannelUrl();
                    
                    // Try system-specific approaches to avoid GTK warnings
                    String os = System.getProperty("os.name").toLowerCase();
                    
                    if (os.contains("linux")) {
                        // Use xdg-open on Linux to avoid GTK/GDK warnings
                        ProcessBuilder pb = new ProcessBuilder("xdg-open", url);
                        pb.start();
                    } else if (os.contains("windows")) {
                        // Use Windows specific command
                        ProcessBuilder pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
                        pb.start();
                    } else if (os.contains("mac")) {
                        // Use macOS specific command
                        ProcessBuilder pb = new ProcessBuilder("open", url);
                        pb.start();
                    } else {
                        // Fallback to Desktop API for other systems
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                            if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                                java.net.URI uri = new java.net.URI(url);
                                desktop.browse(uri);
                            } else {
                                throw new UnsupportedOperationException("Browser action not supported");
                            }
                        } else {
                            throw new UnsupportedOperationException("Desktop integration not supported");
                        }
                    }
                    
                    // Log success on JavaFX thread
                    Platform.runLater(() -> {
                        logArea.appendText(String.format("[System] Opened %s: %s in browser\n", 
                            channel.getPlatform(), channel.getChannelName()));
                    });
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showBrowserError("Failed to open browser: " + e.getMessage());
                        logArea.appendText(String.format("[System] Error opening browser for %s: %s - %s\n", 
                            channel.getPlatform(), channel.getChannelName(), e.getMessage()));
                    });
                }
                return null;
            }
        };
        
        // Run the task in a background thread
        Thread browserThread = new Thread(browserTask);
        browserThread.setDaemon(true);
        browserThread.start();
    }
    
    /**
     * Show error dialog when browser opening fails
     */
    private void showBrowserError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Browser Error");
        alert.setHeaderText("Cannot Open Browser");
        alert.setContentText(message);
        alert.showAndWait();
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
                monitoringService.startMonitoring(channelEntry);
                logArea.appendText(String.format("[System] Started monitoring %s: %s\n", 
                    channelEntry.getPlatform(), channelEntry.getChannelName()));
            }
        });
    }
    
    private void updateTrayIntegration(Stage primaryStage) {
        if (appSettings.isMinimizeToTray() && TraySupport.isSupported()) {
            // Keep FX alive when window is hidden to tray
            Platform.setImplicitExit(false);
            // Tray "Exit" should perform full shutdown
            TraySupport.install(primaryStage, () -> Platform.runLater(() -> requestAppExit(primaryStage)));
        } else {
            TraySupport.uninstall();
            Platform.setImplicitExit(true);
        }
    }

    /**
     * Initiate graceful shutdown and exit JVM once done.
     */
    private void requestAppExit(Stage primaryStage) {
        if (!shuttingDown.compareAndSet(false, true)) {
            return; // Shutdown already in progress
        }
        // Remove tray icon so the user cannot reopen during shutdown
        TraySupport.uninstall();
        // Ensure JavaFX will stop once last window closes
        Platform.setImplicitExit(true);

        showShutdownModal(primaryStage, () -> {
            // Close primary stage if still showing
            if (primaryStage.isShowing()) {
                primaryStage.close();
            }
            // Exit JavaFX and then the JVM to kill any non-daemon threads (AWT, stream watchers, etc.)
            Platform.exit();
            System.exit(0);
        });
    }

    private void showSettingsDialog() {
        boolean settingsChanged = SettingsDialog.showDialog();
        if (settingsChanged) {
            // Reload settings in case they changed
            appSettings = AppSettings.load();
            logArea.appendText("[System] Settings updated successfully\n");

            // Re-apply tray behavior based on new setting
            updateTrayIntegration((Stage) channelTable.getScene().getWindow());
        }
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
     * Show shutdown modal while properly closing active recordings.
     * onComplete is executed after shutdown finishes to perform final exit logic.
     */
    private void showShutdownModal(Stage primaryStage, Runnable onComplete) {
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

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(-1);

        Label statusLabel = new Label("Stopping active recordings...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

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
                Platform.runLater(() -> statusLabel.setText("Stopping monitoring services..."));
                monitoringService.shutdown();

                Platform.runLater(() -> statusLabel.setText("Finalizing shutdown..."));
                Thread.sleep(500); // small grace period
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    shutdownStage.close();
                    if (onComplete != null) onComplete.run();
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    statusLabel.setText("Shutdown completed with warnings.");
                    statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #e74c3c;");
                    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1.5), event -> {
                        shutdownStage.close();
                        if (onComplete != null) onComplete.run();
                    }));
                    timeline.play();
                });
            }
        };

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

    /**
     * Check Streamlink version and alert user if Kick platform is not supported
     */
    private void checkStreamlinkVersion() {
        if (!appSettings.isKickSupported()) {
            String currentVersion = appSettings.getStreamlinkVersion();
            
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Streamlink Version Notice");
            alert.setHeaderText("Kick Platform Not Supported");
            alert.setContentText(String.format(
                "Your current Streamlink version (%s) does not support Kick platform.\n\n" +
                "Kick streaming requires Streamlink 7.3.0 or higher.\n" +
                "You can still use YouTube and Twitch platforms normally.\n\n" +
                "To add Kick support, please update Streamlink:\n" +
                "https://github.com/streamlink/streamlink",
                currentVersion
            ));
            
            // Show alert without blocking the application startup
            Platform.runLater(() -> alert.showAndWait());
        }
    }

    /**
     * Load platform icon from assets/icons directory
     */
    private ImageView loadPlatformIcon(String platform) {
        try {
            String iconPath = "/assets/icons/" + platform.toLowerCase() + ".png";
            InputStream iconStream = getClass().getResourceAsStream(iconPath);
            
            if (iconStream != null) {
                Image image = new Image(iconStream);
                
                // Check if image loaded successfully
                if (!image.isError()) {
                    ImageView imageView = new ImageView(image);
                    
                    // Set icon size slightly larger for table display (20px)
                    imageView.setFitWidth(20);
                    imageView.setFitHeight(20);
                    imageView.setPreserveRatio(true);
                    
                    return imageView;
                }
            }
        } catch (Exception e) {
            // If icon loading fails, continue without icon
            System.err.println("Failed to load icon for platform: " + platform + " - " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Show support dialog with Ko-fi link
     */
    private void showSupportDialog() {
        Dialog<Void> supportDialog = new Dialog<>();
        supportDialog.setTitle("Support StreamWarden");
        supportDialog.setHeaderText("Support This Project");
        
        // Create dialog content
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.setPrefWidth(400);
        
        // Support text
        Label supportText = new Label(
            "This project is completely free and open source. " +
            "If my work has been useful to you and you'd like to support it, " +
            "this is the right place.\n\n" +
            "Any amount is welcome. Your support helps sustain the work and " +
            "encourages continued development."
        );
        supportText.setWrapText(true);
        supportText.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
        supportText.setTextAlignment(TextAlignment.CENTER);
        
        // Ko-fi button with image
        Button kofiButton = createKofiButton();
        
        content.getChildren().addAll(supportText, kofiButton);
        
        // Set dialog content
        supportDialog.getDialogPane().setContent(content);
        
        // Add close button
        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        supportDialog.getDialogPane().getButtonTypes().add(closeButtonType);
        
        // Show dialog
        supportDialog.showAndWait();
    }
    
    /**
     * Create Ko-fi button with image
     */
    private Button createKofiButton() {
        Button kofiButton = new Button();
        
        try {
            // Load Ko-fi image
            String imagePath = "/assets/support/ko-fi.png";
            InputStream imageStream = getClass().getResourceAsStream(imagePath);
            
            if (imageStream != null) {
                Image kofiImage = new Image(imageStream);
                
                if (!kofiImage.isError()) {
                    ImageView imageView = new ImageView(kofiImage);
                    // Set image size (adjust as needed based on your image)
                    imageView.setFitHeight(40);
                    imageView.setPreserveRatio(true);
                    
                    kofiButton.setGraphic(imageView);
                    kofiButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
                } else {
                    // Fallback to text if image fails
                    kofiButton.setText("Support me on Ko-fi");
                    kofiButton.setStyle("-fx-background-color: #FF5E5B; -fx-text-fill: white; -fx-font-weight: bold;");
                }
            } else {
                // Fallback to text if image not found
                kofiButton.setText("Support me on Ko-fi");
                kofiButton.setStyle("-fx-background-color: #FF5E5B; -fx-text-fill: white; -fx-font-weight: bold;");
            }
        } catch (Exception e) {
            // Fallback to text if any error occurs
            kofiButton.setText("Support me on Ko-fi");
            kofiButton.setStyle("-fx-background-color: #FF5E5B; -fx-text-fill: white; -fx-font-weight: bold;");
            System.err.println("Failed to load Ko-fi image: " + e.getMessage());
        }
        
        // Set button action to open Ko-fi link
        kofiButton.setOnAction(e -> openKofiLink());
        
        return kofiButton;
    }
    
    /**
     * Open Ko-fi link in browser
     */
    private void openKofiLink() {
        Task<Void> browserTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    String kofiUrl = "https://ko-fi.com/yougo";
                    String os = System.getProperty("os.name").toLowerCase();
                    
                    if (os.contains("linux")) {
                        ProcessBuilder pb = new ProcessBuilder("xdg-open", kofiUrl);
                        pb.start();
                    } else if (os.contains("windows")) {
                        ProcessBuilder pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", kofiUrl);
                        pb.start();
                    } else if (os.contains("mac")) {
                        ProcessBuilder pb = new ProcessBuilder("open", kofiUrl);
                        pb.start();
                    } else {
                        // Fallback to Desktop API
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                            if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                                java.net.URI uri = new java.net.URI(kofiUrl);
                                desktop.browse(uri);
                            }
                        }
                    }
                    
                    Platform.runLater(() -> {
                        logArea.appendText("[System] Opened Ko-fi support page in browser\n");
                    });
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showBrowserError("Failed to open Ko-fi link: " + e.getMessage());
                        logArea.appendText("[System] Error opening Ko-fi link: " + e.getMessage() + "\n");
                    });
                }
                return null;
            }
        };
        
        Thread browserThread = new Thread(browserTask);
        browserThread.setDaemon(true);
        browserThread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}