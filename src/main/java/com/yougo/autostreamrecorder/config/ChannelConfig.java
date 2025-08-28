package com.yougo.autostreamrecorder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yougo.autostreamrecorder.ChannelEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ChannelConfig {
    
    private static final String CHANNELS_FILE = "channels.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Data class for JSON serialization of channel entries
     */
    public static class ChannelData {
        public String platform;
        public String channelName;
        public String channelUrl;
        public boolean isActive;
        public String quality;
        public int checkInterval = 60;
        
        public ChannelData() {}
        
        public ChannelData(ChannelEntry entry, int checkInterval) {
            this.platform = entry.getPlatform();
            this.channelName = entry.getChannelName();
            this.channelUrl = entry.getChannelUrl();
            this.isActive = entry.getIsActive();
            this.quality = entry.getQuality();
            this.checkInterval = checkInterval;
        }
        
        public ChannelEntry toChannelEntry() {
            return new ChannelEntry(platform, channelName, channelUrl, isActive, "Offline", quality);
        }
    }
    
    /**
     * Load channels from JSON file
     */
    public static List<ChannelData> loadChannels() {
        File channelsFile = new File(CHANNELS_FILE);
        
        if (!channelsFile.exists()) {
            return new ArrayList<>();
        }
        
        try (FileReader reader = new FileReader(channelsFile)) {
            Type listType = new TypeToken<List<ChannelData>>(){}.getType();
            List<ChannelData> channels = gson.fromJson(reader, listType);
            return channels != null ? channels : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error loading channels: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Save channels to JSON file
     */
    public static void saveChannels(List<ChannelData> channels) {
        try (FileWriter writer = new FileWriter(CHANNELS_FILE)) {
            gson.toJson(channels, writer);
        } catch (IOException e) {
            System.err.println("Error saving channels: " + e.getMessage());
        }
    }
    
    /**
     * Convert channel data list to observable list for UI
     */
    public static ObservableList<ChannelEntry> toObservableList(List<ChannelData> channelDataList) {
        ObservableList<ChannelEntry> entries = FXCollections.observableArrayList();
        for (ChannelData data : channelDataList) {
            entries.add(data.toChannelEntry());
        }
        return entries;
    }
    
    /**
     * Convert observable list to channel data list for saving
     */
    public static List<ChannelData> fromObservableList(ObservableList<ChannelEntry> channelEntries) {
        List<ChannelData> channels = new ArrayList<>();
        for (ChannelEntry entry : channelEntries) {
            channels.add(new ChannelData(entry, 60)); // Default check interval
        }
        return channels;
    }
    
    /**
     * Add a single channel and save
     */
    public static void addChannel(ChannelEntry entry, int checkInterval) {
        List<ChannelData> channels = loadChannels();
        channels.add(new ChannelData(entry, checkInterval));
        saveChannels(channels);
    }
    
    /**
     * Remove a channel and save
     */
    public static void removeChannel(ChannelEntry entry) {
        List<ChannelData> channels = loadChannels();
        channels.removeIf(data -> 
            data.platform.equals(entry.getPlatform()) && 
            data.channelName.equals(entry.getChannelName())
        );
        saveChannels(channels);
    }
}