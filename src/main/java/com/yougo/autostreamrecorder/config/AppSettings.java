package com.yougo.autostreamrecorder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

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
    private boolean showActivityLogs = false;

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
    
    public boolean isShowActivityLogs() {
        return showActivityLogs;
    }
    
    public void setShowActivityLogs(boolean showActivityLogs) {
        this.showActivityLogs = showActivityLogs;
        save();
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
     * Check if Kick platform is supported by current Streamlink version
     * Kick requires Streamlink 7.3.0 or higher
     */
    public boolean isKickSupported() {
        try {
            ProcessBuilder pb = new ProcessBuilder(getStreamlinkPath(), "--version");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String versionLine = reader.readLine();
                if (versionLine != null && versionLine.contains("streamlink")) {
                    // Extract version number (e.g., "streamlink 7.5.1" -> "7.5.1")
                    String[] parts = versionLine.split(" ");
                    if (parts.length >= 2) {
                        String version = parts[1];
                        return isVersionSupported(version, "7.3.0");
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // If we can't determine version, assume Kick is not supported
            return false;
        }
        return false;
    }
    
    /**
     * Compare version strings to check if current version meets minimum requirement
     */
    private boolean isVersionSupported(String currentVersion, String minVersion) {
        try {
            String[] current = currentVersion.split("\\.");
            String[] minimum = minVersion.split("\\.");
            
            int maxLength = Math.max(current.length, minimum.length);
            
            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < current.length ? Integer.parseInt(current[i]) : 0;
                int minimumPart = i < minimum.length ? Integer.parseInt(minimum[i]) : 0;
                
                if (currentPart > minimumPart) {
                    return true;
                } else if (currentPart < minimumPart) {
                    return false;
                }
            }
            
            return true; // Versions are equal
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Get current Streamlink version string
     */
    public String getStreamlinkVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder(getStreamlinkPath(), "--version");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String versionLine = reader.readLine();
                if (versionLine != null && versionLine.contains("streamlink")) {
                    String[] parts = versionLine.split(" ");
                    if (parts.length >= 2) {
                        return parts[1];
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            return "unknown";
        }
        return "unknown";
    }
}