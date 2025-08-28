package com.yougo.autostreamrecorder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AppSettings {
    
    private static final String SETTINGS_FILE = "settings.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Default settings
    private String outputDirectory = System.getProperty("user.home") + File.separator + "AutoStreamRecorder";
    private boolean autoStartMonitoring = true;
    private int defaultCheckInterval = 60;
    private String defaultQuality = "best";
    private boolean minimizeToTray = false;
    private boolean showNotifications = true;
    // Tool paths - point to bundled executables
    private String streamlinkPath = getBundledStreamlinkPath();
    private String ytDlpPath = getBundledYtDlpPath();
    
    /**
     * Load settings from JSON file
     */
    public static AppSettings load() {
        File settingsFile = new File(SETTINGS_FILE);
        
        if (!settingsFile.exists()) {
            AppSettings defaultSettings = new AppSettings();
            defaultSettings.save(); // Create default settings file
            return defaultSettings;
        }
        
        try (FileReader reader = new FileReader(settingsFile)) {
            AppSettings settings = gson.fromJson(reader, AppSettings.class);
            return settings != null ? settings : new AppSettings();
        } catch (IOException e) {
            System.err.println("Error loading settings: " + e.getMessage());
            return new AppSettings();
        }
    }
    
    /**
     * Save settings to JSON file
     */
    public void save() {
        try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }
    
    // Getters and setters
    public String getOutputDirectory() {
        return outputDirectory;
    }
    
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    public boolean isAutoStartMonitoring() {
        return autoStartMonitoring;
    }
    
    public void setAutoStartMonitoring(boolean autoStartMonitoring) {
        this.autoStartMonitoring = autoStartMonitoring;
    }
    
    public int getDefaultCheckInterval() {
        return defaultCheckInterval;
    }
    
    public void setDefaultCheckInterval(int defaultCheckInterval) {
        this.defaultCheckInterval = defaultCheckInterval;
    }
    
    public String getDefaultQuality() {
        return defaultQuality;
    }
    
    public void setDefaultQuality(String defaultQuality) {
        this.defaultQuality = defaultQuality;
    }
    
    public boolean isMinimizeToTray() {
        return minimizeToTray;
    }
    
    public void setMinimizeToTray(boolean minimizeToTray) {
        this.minimizeToTray = minimizeToTray;
    }
    
    public boolean isShowNotifications() {
        return showNotifications;
    }
    
    public void setShowNotifications(boolean showNotifications) {
        this.showNotifications = showNotifications;
    }
    
    public String getStreamlinkPath() {
        return streamlinkPath;
    }
    
    public void setStreamlinkPath(String streamlinkPath) {
        this.streamlinkPath = streamlinkPath;
    }
    
    public String getYtDlpPath() {
        return ytDlpPath;
    }
    
    public void setYtDlpPath(String ytDlpPath) {
        this.ytDlpPath = ytDlpPath;
    }
    
    /**
     * Get the path to bundled streamlink executable based on OS
     */
    private static String getBundledStreamlinkPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "bin" + File.separator + "windows" + File.separator + "streamlink.exe";
        } else {
            return "bin" + File.separator + "linux" + File.separator + "streamlink";
        }
    }
    
    /**
     * Get the path to bundled yt-dlp executable based on OS
     */
    private static String getBundledYtDlpPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "bin" + File.separator + "windows" + File.separator + "yt-dlp.exe";
        } else {
            return "bin" + File.separator + "linux" + File.separator + "yt-dlp";
        }
    }
}