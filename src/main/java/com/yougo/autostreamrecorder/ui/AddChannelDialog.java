package com.yougo.autostreamrecorder.ui;

import com.yougo.autostreamrecorder.ChannelEntry;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.Optional;

public class AddChannelDialog extends Dialog<ChannelEntry> {
    
    private ComboBox<String> platformCombo;
    private TextField channelNameField;
    private ComboBox<String> qualityCombo;
    private Spinner<Integer> checkIntervalSpinner;
    private CheckBox enabledCheckBox;
    private boolean isEditMode = false;
    
    public AddChannelDialog() {
        this(false);
    }
    
    public AddChannelDialog(boolean editMode) {
        this.isEditMode = editMode;
        setTitle(editMode ? "Edit Channel" : "Add Channel");
        setHeaderText(editMode ? "Edit channel settings" : "Add a new channel to monitor");
        
        // Create dialog buttons
        ButtonType actionButtonType = new ButtonType(
            editMode ? "Save" : "Add", 
            ButtonBar.ButtonData.OK_DONE
        );
        getDialogPane().getButtonTypes().addAll(actionButtonType, ButtonType.CANCEL);
        
        // Create form content
        GridPane grid = createFormContent();
        getDialogPane().setContent(grid);
        
        // Enable/disable action button based on form validity
        Button actionButton = (Button) getDialogPane().lookupButton(actionButtonType);
        actionButton.disableProperty().bind(
            Bindings.isEmpty(channelNameField.textProperty())
        );
        
        // Set result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == actionButtonType) {
                return createChannelEntry();
            }
            return null;
        });
        
        // Request focus on channel name field
        Platform.runLater(() -> channelNameField.requestFocus());
    }
    
    private GridPane createFormContent() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Platform selection
        grid.add(new Label("Platform:"), 0, 0);
        platformCombo = new ComboBox<>(FXCollections.observableArrayList("YouTube", "Twitch"));
        platformCombo.setValue("YouTube");
        grid.add(platformCombo, 1, 0);
        
        // Channel name
        grid.add(new Label("Channel Name:"), 0, 1);
        channelNameField = new TextField();
        channelNameField.setPromptText("Enter channel name or handle");
        grid.add(channelNameField, 1, 1);
        
        // Quality selection
        grid.add(new Label("Quality:"), 0, 2);
        qualityCombo = new ComboBox<>(FXCollections.observableArrayList(
            "best", "1080p", "720p", "480p", "360p", "audio_only"
        ));
        qualityCombo.setValue("best");
        grid.add(qualityCombo, 1, 2);
        
        // Check interval
        grid.add(new Label("Check Interval (seconds):"), 0, 3);
        checkIntervalSpinner = new Spinner<>(10, 3600, 60, 10);
        checkIntervalSpinner.setEditable(true);
        grid.add(checkIntervalSpinner, 1, 3);
        
        // Enabled checkbox
        enabledCheckBox = new CheckBox("Enable monitoring");
        enabledCheckBox.setSelected(true);
        grid.add(enabledCheckBox, 1, 4);
        
        return grid;
    }
    
    private ChannelEntry createChannelEntry() {
        String platform = platformCombo.getValue();
        String channelName = channelNameField.getText().trim();
        String quality = qualityCombo.getValue();
        boolean isEnabled = enabledCheckBox.isSelected();
        
        // Generate URL based on platform
        String channelUrl = generateChannelUrl(platform, channelName);
        
        return new ChannelEntry(
            platform,
            channelName,
            channelUrl,
            isEnabled,
            "Offline", // Initial status
            quality
        );
    }
    
    private String generateChannelUrl(String platform, String channelName) {
        switch (platform.toLowerCase()) {
            case "youtube":
                // Handle both @username and channel name formats
                if (channelName.startsWith("@")) {
                    return "https://www.youtube.com/" + channelName + "/live";
                } else {
                    return "https://www.youtube.com/@" + channelName + "/live";
                }
            case "twitch":
                return "https://www.twitch.tv/" + channelName;
            default:
                return "";
        }
    }
    
    public static Optional<ChannelEntry> showDialog() {
        AddChannelDialog dialog = new AddChannelDialog(false);
        return dialog.showAndWait();
    }
    
    public static Optional<ChannelEntry> showDialog(ChannelEntry existing) {
        AddChannelDialog dialog = new AddChannelDialog(true);
        // Pre-fill fields with existing channel info
        dialog.platformCombo.setValue(existing.getPlatform());
        dialog.channelNameField.setText(existing.getChannelName());
        dialog.qualityCombo.setValue(existing.getQuality());
        dialog.enabledCheckBox.setSelected(existing.getIsActive());
        return dialog.showAndWait();
    }
}