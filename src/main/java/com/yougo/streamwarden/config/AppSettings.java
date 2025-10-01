/* 
 * Copyright (C) 2025-present YouGo (https://github.com/youg-o)
 * This program is licensed under the GNU Affero General Public License v3.0.
 * You may redistribute it and/or modify it under the terms of the license.
 * 
 * Attribution must be given to the original author.
 * This program is distributed without any warranty; see the license for details.
 */


package com.yougo.streamwarden.config;

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
    private static final String DOWNLOADS_DIR = "downloads";
    private static final String SETTINGS_FILE = CONFIG_DIR + File.separator + "settings.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Ensure config and downloads directories exist
    static {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        File downloadsDir = new File(DOWNLOADS_DIR);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }
    }
    
    // Default settings
    private String outputDirectory = getDefaultDownloadsDirectory();
    private boolean autoStartMonitoring = true;
    private int defaultCheckInterval = 60;
    private String defaultQuality = "1080p";
    private boolean minimizeToTray = false;
    private boolean showActivityLogs = false;
    private boolean recordHighFps = true;

    /**
     * Get the default downloads directory (local downloads folder)
     */
    private static String getDefaultDownloadsDirectory() {
        File downloadsDir = new File(DOWNLOADS_DIR);
        return downloadsDir.getAbsolutePath();
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
        return isSystemTraySupported() && minimizeToTray;
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
    
    public boolean isRecordHighFps() {
        return recordHighFps;
    }
    
    public void setRecordHighFps(boolean recordHighFps) {
        this.recordHighFps = recordHighFps;
    }
    
    /**
     * Get the path to streamlink executable based on OS and availability
     * Logs which streamlink is used and its version.
     */
    public String getStreamlinkPath() {
        String streamlinkPath;
        if (isWindowsOS()) {
            // Try to find Streamlink in app/bin/windows/ (packaged), then bin/windows/ (dev)
            String[] baseDirs = { "app" + File.separator + "bin" + File.separator + "windows", "bin" + File.separator + "windows" };
            for (String baseDir : baseDirs) {
                File windowsBinDir = new File(baseDir);
                if (windowsBinDir.exists() && windowsBinDir.isDirectory()) {
                    File[] candidates = windowsBinDir.listFiles((dir, name) -> name.startsWith("streamlink") && new File(dir, name).isDirectory());
                    if (candidates != null && candidates.length > 0) {
                        // Use the first matching streamlink folder
                        File streamlinkDir = candidates[0];
                        String bundledPath = streamlinkDir.getPath() + File.separator + "bin" + File.separator + "streamlink.exe";
                        File bundledFile = new File(bundledPath);
                        if (bundledFile.exists()) {
                            streamlinkPath = bundledPath;
                            System.out.println("[AppSettings] Using bundled Streamlink: " + streamlinkPath + " (version: " + getStreamlinkVersion(streamlinkPath) + ")");
                            return streamlinkPath;
                        }
                    }
                }
            }
            // Fallback to system Streamlink
            streamlinkPath = "streamlink";
            System.out.println("[AppSettings] Using system Streamlink from PATH (version: " + getStreamlinkVersion(streamlinkPath) + ")");
            return streamlinkPath;
        } else {
            // Linux/Mac: use system installation
            streamlinkPath = "streamlink";
            System.out.println("[AppSettings] Using system Streamlink from PATH (version: " + getStreamlinkVersion(streamlinkPath) + ")");
            return streamlinkPath;
        }
    }

    /**
     * Get Streamlink version for a given executable path.
     */
    private String getStreamlinkVersion(String streamlinkPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(streamlinkPath, "--version");
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

    /**
     * Check if the current operating system is Windows
     */
    public static boolean isWindowsOS() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    /**
     * Check if system tray functionality is available and supported
     * Currently only supported on Windows due to Linux compatibility issues
     */
    public static boolean isSystemTraySupported() {
        return isWindowsOS() && java.awt.SystemTray.isSupported();
    }
}