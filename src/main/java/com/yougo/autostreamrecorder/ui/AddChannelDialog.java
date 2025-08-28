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
    
    public AddChannelDialog() {
        setTitle("Add Channel");
        setHeaderText("Add a new channel to monitor");
        
        // Create dialog buttons
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create form content
        GridPane grid = createFormContent();
        getDialogPane().setContent(grid);
        
        // Enable/disable Add button based on form validity
        Button addButton = (Button) getDialogPane().lookupButton(addButtonType);
        addButton.disableProperty().bind(
            Bindings.isEmpty(channelNameField.textProperty())
        );
        
        // Set result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
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
        AddChannelDialog dialog = new AddChannelDialog();
        return dialog.showAndWait();
    }
}