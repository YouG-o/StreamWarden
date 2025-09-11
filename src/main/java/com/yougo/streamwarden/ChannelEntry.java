package com.yougo.streamwarden;

import javafx.beans.property.*;

public class ChannelEntry {
    private final StringProperty platform;
    private final StringProperty channelName;
    private final StringProperty channelUrl;
    private final BooleanProperty isActive;
    private final StringProperty status;
    private final StringProperty quality;
    
    public ChannelEntry(String platform, String channelName, String channelUrl, 
                       boolean isActive, String status, String quality) {
        this.platform = new SimpleStringProperty(platform);
        this.channelName = new SimpleStringProperty(channelName);
        this.channelUrl = new SimpleStringProperty(channelUrl);
        this.isActive = new SimpleBooleanProperty(isActive);
        this.status = new SimpleStringProperty(status);
        this.quality = new SimpleStringProperty(quality);
    }
    
    // Platform property
    public String getPlatform() { return platform.get(); }
    public void setPlatform(String platform) { this.platform.set(platform); }
    public StringProperty platformProperty() { return platform; }
    
    // Channel name property
    public String getChannelName() { return channelName.get(); }
    public void setChannelName(String channelName) { this.channelName.set(channelName); }
    public StringProperty channelNameProperty() { return channelName; }
    
    // Channel URL property
    public String getChannelUrl() { return channelUrl.get(); }
    public void setChannelUrl(String channelUrl) { this.channelUrl.set(channelUrl); }
    public StringProperty channelUrlProperty() { return channelUrl; }
    
    // Active property
    public boolean getIsActive() { return isActive.get(); }
    public void setIsActive(boolean isActive) { this.isActive.set(isActive); }
    public BooleanProperty isActiveProperty() { return isActive; }
    
    // Status property
    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    public StringProperty statusProperty() { return status; }
    
    // Quality property
    public String getQuality() { return quality.get(); }
    public void setQuality(String quality) { this.quality.set(quality); }
    public StringProperty qualityProperty() { return quality; }
}