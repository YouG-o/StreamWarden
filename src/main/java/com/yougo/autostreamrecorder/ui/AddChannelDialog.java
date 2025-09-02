package com.yougo.autostreamrecorder.ui;

import com.yougo.autostreamrecorder.ChannelEntry;
import com.yougo.autostreamrecorder.config.AppSettings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Modality;

import java.io.InputStream;
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
        
        // Platform selection - conditionally include Kick based on Streamlink version
        grid.add(new Label("Platform:"), 0, 0);
        
        // Check if Kick is supported by current Streamlink version
        AppSettings settings = AppSettings.load();
        boolean kickSupported = settings.isKickSupported();
        
        if (kickSupported) {
            platformCombo = new ComboBox<>(FXCollections.observableArrayList("YouTube", "Twitch", "Kick"));
        } else {
            platformCombo = new ComboBox<>(FXCollections.observableArrayList("YouTube", "Twitch"));
        }
        
        // Set custom cell factory to display icons with platform names
        platformCombo.setCellFactory(listView -> new PlatformListCell());
        platformCombo.setButtonCell(new PlatformListCell());
        
        platformCombo.setValue("YouTube");
        grid.add(platformCombo, 1, 0);
        
        // Add info label if Kick is not supported
        if (!kickSupported) {
            Label kickInfoLabel = new Label("(Kick requires Streamlink 7.3.0+)");
            kickInfoLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");
            grid.add(kickInfoLabel, 1, 0);
            GridPane.setMargin(kickInfoLabel, new Insets(25, 0, 0, 0));
        }
        
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
            case "kick":
                return "https://kick.com/" + channelName;
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
    
    /**
     * Custom ListCell to display platform icons alongside text
     */
    private static class PlatformListCell extends ListCell<String> {
        @Override
        protected void updateItem(String platform, boolean empty) {
            super.updateItem(platform, empty);
            
            if (empty || platform == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(platform);
                
                // Load platform icon
                ImageView icon = loadPlatformIcon(platform);
                if (icon != null) {
                    setGraphic(icon);
                } else {
                    setGraphic(null);
                }
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
                        
                        // Set icon size to match text height (approximately 16px)
                        imageView.setFitWidth(16);
                        imageView.setFitHeight(16);
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
    }
}