/* 
 * Copyright (C) 2025-present YouGo (https://github.com/youg-o)
 * This program is licensed under the GNU Affero General Public License v3.0.
 * You may redistribute it and/or modify it under the terms of the license.
 * 
 * Attribution must be given to the original author.
 * This program is distributed without any warranty; see the license for details.
 */


package com.yougo.streamwarden.ui;

import com.yougo.streamwarden.config.AppSettings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

public class SettingsDialog extends Dialog<Boolean> {
    
    private TextField outputDirectoryField;
    private CheckBox autoStartMonitoringCheckBox;
    private Spinner<Integer> defaultCheckIntervalSpinner;
    private ComboBox<String> defaultQualityCombo;
    private CheckBox minimizeToTrayCheckBox;
    private CheckBox recordHighFpsCheckBox;
    
    private AppSettings settings;
    
    public SettingsDialog() {
        this.settings = AppSettings.load();
        
        setTitle("Settings");
        setHeaderText("Application Settings");
        
        // Create dialog buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create form content
        GridPane grid = createFormContent();
        getDialogPane().setContent(grid);
        
        // Set result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                saveSettings();
                return true;
            }
            return false;
        });
        
        // Load current settings into form
        loadCurrentSettings();
    }
    
    /**
     * Create the form content with all settings controls
     */
    private GridPane createFormContent() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        int row = 0;
        
        // Output Directory
        grid.add(new Label("Output Directory:"), 0, row);
        outputDirectoryField = new TextField();
        outputDirectoryField.setPrefWidth(300);
        outputDirectoryField.setEditable(false);
        
        Button browseButton = new Button("Browse...");
        browseButton.setOnAction(e -> selectOutputDirectory());
        
        grid.add(outputDirectoryField, 1, row);
        grid.add(browseButton, 2, row);
        row++;
        
        // Auto Start Monitoring
        grid.add(new Label("Auto Start Monitoring:"), 0, row);
        autoStartMonitoringCheckBox = new CheckBox("Start monitoring active channels on application startup");
        grid.add(autoStartMonitoringCheckBox, 1, row, 2, 1);
        row++;
        
        // Default Check Interval
        grid.add(new Label("Default Check Interval:"), 0, row);
        defaultCheckIntervalSpinner = new Spinner<>(10, 3600, 60, 10);
        defaultCheckIntervalSpinner.setEditable(true);
        defaultCheckIntervalSpinner.setPrefWidth(100);
        
        Label intervalLabel = new Label("seconds");
        grid.add(defaultCheckIntervalSpinner, 1, row);
        grid.add(intervalLabel, 2, row);
        row++;
        
        // Default Quality
        grid.add(new Label("Default Quality:"), 0, row);
        defaultQualityCombo = new ComboBox<>(FXCollections.observableArrayList(
            "4k", "1080p", "720p", "480p", "360p", "240p", "144p"
        ));
        defaultQualityCombo.setPrefWidth(150);
        grid.add(defaultQualityCombo, 1, row);
        row++;
        
        // Record High FPS
        grid.add(new Label("High FPS Recording:"), 0, row);
        recordHighFpsCheckBox = new CheckBox("Record at more than 30 fps (if available)");
        grid.add(recordHighFpsCheckBox, 1, row, 2, 1);
        row++;
        
        // Minimize to Tray
        grid.add(new Label("Minimize to Tray:"), 0, row);
        minimizeToTrayCheckBox = new CheckBox("Minimize application to system tray instead of taskbar");
        if (!AppSettings.isSystemTraySupported()) {
            minimizeToTrayCheckBox.setDisable(true);
            minimizeToTrayCheckBox.setText("Minimize to tray (Windows only)");
        }
        grid.add(minimizeToTrayCheckBox, 1, row, 2, 1);
        row++;
        
        // Add help text
        Label helpText = new Label("Note: Some settings may require application restart to take effect.");
        helpText.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-style: italic;");
        grid.add(helpText, 0, row, 3, 1);
        
        return grid;
    }
    
    /**
     * Load current settings into form controls
     */
    private void loadCurrentSettings() {
        outputDirectoryField.setText(settings.getOutputDirectory());
        autoStartMonitoringCheckBox.setSelected(settings.isAutoStartMonitoring());
        defaultCheckIntervalSpinner.getValueFactory().setValue(settings.getDefaultCheckInterval());
        defaultQualityCombo.setValue(settings.getDefaultQuality());
        recordHighFpsCheckBox.setSelected(settings.isRecordHighFps());
        minimizeToTrayCheckBox.setSelected(settings.isMinimizeToTray());
    }
    
    /**
     * Save settings from form controls
     */
    private void saveSettings() {
        settings.setOutputDirectory(outputDirectoryField.getText());
        settings.setAutoStartMonitoring(autoStartMonitoringCheckBox.isSelected());
        settings.setDefaultCheckInterval(defaultCheckIntervalSpinner.getValue());
        settings.setDefaultQuality(defaultQualityCombo.getValue());
        settings.setRecordHighFps(recordHighFpsCheckBox.isSelected());
        settings.setMinimizeToTray(minimizeToTrayCheckBox.isSelected());
        
        // Save to file
        settings.save();
    }
    
    /**
     * Open directory chooser for output directory selection
     */
    private void selectOutputDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Output Directory");
        
        // Set initial directory to current setting
        File currentDir = new File(outputDirectoryField.getText());
        if (currentDir.exists() && currentDir.isDirectory()) {
            directoryChooser.setInitialDirectory(currentDir);
        }
        
        // Show dialog
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);
        
        if (selectedDirectory != null) {
            outputDirectoryField.setText(selectedDirectory.getAbsolutePath());
        }
    }
    
    /**
     * Show settings dialog
     */
    public static boolean showDialog() {
        SettingsDialog dialog = new SettingsDialog();
        Optional<Boolean> result = dialog.showAndWait();
        return result.orElse(false);
    }
}