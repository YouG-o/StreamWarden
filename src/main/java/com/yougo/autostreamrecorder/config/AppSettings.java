package com.yougo.autostreamrecorder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AppSettings {
    
    private static final String CONFIG_DIR = "config";
    private static final String SETTINGS_FILE = CONFIG_DIR + File.separator + "settings.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Ensure config directory exists
    static {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
    }
    
    // Default settings
    private String outputDirectory = getDefaultDownloadsDirectory();
    private boolean autoStartMonitoring = true;
    private int defaultCheckInterval = 60;
    private String defaultQuality = "best";
    private boolean minimizeToTray = false;

    /**
     * Get the default Downloads directory based on the operating system
     */
    private static String getDefaultDownloadsDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        
        if (os.contains("win")) {
            // Windows: %USERPROFILE%\Downloads
            return userHome + File.separator + "Downloads" + File.separator + "AutoStreamRecorder";
        } else {
            // Linux/Mac: ~/Downloads
            return userHome + File.separator + "Downloads" + File.separator + "AutoStreamRecorder";
        }
    }


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
    
    /**
     * Get the path to streamlink executable based on OS and availability
     */
    public String getStreamlinkPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows: try bundled first, fallback to system
            String bundledPath = "bin" + File.separator + "windows" + File.separator + 
                                "streamlink-7.5.0-1-py313-x86_64" + File.separator + 
                                "bin" + File.separator + "streamlink.exe";
            File bundledFile = new File(bundledPath);
            if (bundledFile.exists()) {
                return bundledPath;
            }
            return "streamlink"; // Fallback to system PATH
        } else {
            // Linux: use system installation
            return "streamlink";
        }
    }
    
    /**
     * Get the path to yt-dlp executable based on OS and availability
     */
    public String getYtDlpPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows: try bundled first, fallback to system
            String bundledPath = "bin" + File.separator + "windows" + File.separator + "yt-dlp.exe";
            File bundledFile = new File(bundledPath);
            if (bundledFile.exists()) {
                return bundledPath;
            }
            return "yt-dlp"; // Fallback to system PATH
        } else {
            // Linux: use system installation
            return "yt-dlp";
        }
    }
}