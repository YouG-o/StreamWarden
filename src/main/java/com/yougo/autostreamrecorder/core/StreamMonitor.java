package com.yougo.autostreamrecorder.core;

import com.yougo.autostreamrecorder.ChannelEntry;
import com.yougo.autostreamrecorder.config.AppSettings;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class StreamMonitor implements Runnable {
    
    private final ChannelEntry channelEntry;
    private final AppSettings settings;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private final int checkInterval;
    
    // Callback interfaces for UI updates
    public interface StatusCallback {
        void onStatusChanged(ChannelEntry channel, String status);
        void onLogMessage(String message);
    }
    
    private StatusCallback statusCallback;
    
    public StreamMonitor(ChannelEntry channelEntry, AppSettings settings, int checkInterval) {
        this.channelEntry = channelEntry;
        this.settings = settings;
        this.checkInterval = checkInterval;
    }
    
    public void setStatusCallback(StatusCallback callback) {
        this.statusCallback = callback;
    }
    
    @Override
    public void run() {
        running.set(true);
        updateStatus("Offline"); // Start with Offline status when monitoring begins
        logMessage(String.format("[%s] Started monitoring channel: %s", 
            channelEntry.getPlatform(), channelEntry.getChannelName()));
        
        while (running.get() && channelEntry.getIsActive()) {
            try {
                if (isStreamLive()) {
                    if (!recording.get()) {
                        startRecording();
                    }
                    // Wait longer when recording to avoid spamming
                    Thread.sleep(30 * 1000L); // 30 seconds when recording
                } else {
                    if (recording.get()) {
                        // Stream ended, recording will stop automatically
                        recording.set(false);
                        updateStatus("Offline");
                        logMessage(String.format("[%s] Stream ended for: %s", 
                            channelEntry.getPlatform(), channelEntry.getChannelName()));
                    } else {
                        // Ensure status shows Offline when not recording
                        updateStatus("Offline");
                        // Only log every few checks to avoid spam
                        logMessage(String.format("[%s] Channel %s is offline, waiting %d seconds...", 
                            channelEntry.getPlatform(), channelEntry.getChannelName(), checkInterval));
                    }
                    
                    Thread.sleep(checkInterval * 1000L);
                }
                
            } catch (InterruptedException e) {
                logMessage(String.format("[%s] Monitor interrupted for: %s", 
                    channelEntry.getPlatform(), channelEntry.getChannelName()));
                break;
            } catch (Exception e) {
                logMessage(String.format("[%s] Error monitoring %s: %s", 
                    channelEntry.getPlatform(), channelEntry.getChannelName(), e.getMessage()));
                try {
                    Thread.sleep(checkInterval * 1000L);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
        
        running.set(false);
        updateStatus(""); // Clear status when not monitoring
        logMessage(String.format("[%s] Stopped monitoring: %s", 
            channelEntry.getPlatform(), channelEntry.getChannelName()));
    }
    
    private boolean isStreamLive() {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(settings.getStreamlinkPath(), channelEntry.getChannelUrl(), "--json");
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            // Streamlink returns 0 if stream is available, non-zero if not
            return exitCode == 0;
            
        } catch (Exception e) {
            logMessage(String.format("[%s] Error checking stream status for %s: %s", 
                channelEntry.getPlatform(), channelEntry.getChannelName(), e.getMessage()));
            return false;
        }
    }
    
    private void startRecording() {
        if (recording.get()) {
            return; // Already recording
        }
        
        recording.set(true);
        updateStatus("Recording");
        logMessage(String.format("[%s] Stream is live! Starting recording: %s", 
            channelEntry.getPlatform(), channelEntry.getChannelName()));
        
        // Start recording in a separate thread
        Thread recordingThread = new Thread(() -> {
            try {
                String outputFile = generateOutputFilename();
                
                // Create channel-specific directory structure
                File outputDir = createChannelDirectory();
                
                ProcessBuilder pb = new ProcessBuilder();
                pb.command(
                    settings.getStreamlinkPath(),
                    channelEntry.getChannelUrl(),
                    channelEntry.getQuality(),
                    "-o", outputFile
                );
                pb.directory(outputDir);
                
                logMessage(String.format("[%s] Recording to: %s%s%s", 
                    channelEntry.getPlatform(), outputDir.getAbsolutePath(), File.separator, outputFile));
                
                Process process = pb.start();
                
                // Read process output
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && recording.get()) {
                        // Optional: log streamlink output for debugging
                        // logMessage(line);
                    }
                }
                
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    logMessage(String.format("[%s] Recording completed successfully: %s", 
                        channelEntry.getPlatform(), outputFile));
                } else {
                    logMessage(String.format("[%s] Recording ended with exit code %d: %s", 
                        channelEntry.getPlatform(), exitCode, channelEntry.getChannelName()));
                }
                
            } catch (Exception e) {
                logMessage(String.format("[%s] Recording error for %s: %s", 
                    channelEntry.getPlatform(), channelEntry.getChannelName(), e.getMessage()));
                updateStatus("Error");
            } finally {
                recording.set(false);
            }
        });
        
        recordingThread.setDaemon(true);
        recordingThread.start();
    }
    
    /**
     * Create and return the channel-specific directory for recordings
     */
    private File createChannelDirectory() {
        // Base output directory
        File baseDir = new File(settings.getOutputDirectory());
        
        // Sanitize channel name for directory creation
        String channelDirName = sanitizeFilename(channelEntry.getChannelName());
        
        // Create channel directory: outputDir/ChannelName/
        File channelDir = new File(baseDir, channelDirName);
        
        // Ensure the directory exists
        if (!channelDir.exists()) {
            if (channelDir.mkdirs()) {
                logMessage(String.format("[%s] Created channel directory: %s", 
                    channelEntry.getPlatform(), channelDir.getAbsolutePath()));
            } else {
                logMessage(String.format("[%s] Failed to create channel directory: %s", 
                    channelEntry.getPlatform(), channelDir.getAbsolutePath()));
                // Fallback to base directory if channel directory creation fails
                return baseDir;
            }
        }
        
        return channelDir;
    }
    
    private String generateOutputFilename() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyy"));
        String channelName = sanitizeFilename(channelEntry.getChannelName());
        String platform = channelEntry.getPlatform().toLowerCase();
        
        // Try to get stream title if possible (simplified for now)
        String streamTitle = "stream";
        
        return String.format("%s-%s-%s-%s-LIVE.ts", 
            datePart, platform, channelName, sanitizeFilename(streamTitle));
    }
    
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "unknown";
        }
        return filename.replaceAll("[^\\w\\-]", "_").replaceAll("_+", "_").trim();
    }
    
    private void updateStatus(String status) {
        Platform.runLater(() -> {
            channelEntry.setStatus(status);
            if (statusCallback != null) {
                statusCallback.onStatusChanged(channelEntry, status);
            }
        });
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logLine = String.format("[%s] %s", timestamp, message);
        
        Platform.runLater(() -> {
            if (statusCallback != null) {
                statusCallback.onLogMessage(logLine);
            }
        });
        
        // Also print to console for debugging
        System.out.println(logLine);
    }
    
    public void stop() {
        running.set(false);
        recording.set(false);
        updateStatus(""); // Clear status when stopped
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public boolean isRecording() {
        return recording.get();
    }
    
    public ChannelEntry getChannelEntry() {
        return channelEntry;
    }
}