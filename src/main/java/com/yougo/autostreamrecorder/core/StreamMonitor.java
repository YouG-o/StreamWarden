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
        updateStatus("Monitoring");
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
        updateStatus("Stopped");
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
                
                // Ensure output directory exists
                File outputDir = new File(settings.getOutputDirectory());
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                    logMessage(String.format("[%s] Created output directory: %s", 
                        channelEntry.getPlatform(), outputDir.getAbsolutePath()));
                }
                
                ProcessBuilder pb = new ProcessBuilder();
                pb.command(
                    settings.getStreamlinkPath(),
                    channelEntry.getChannelUrl(),
                    channelEntry.getQuality(),
                    "-o", outputFile
                );
                pb.directory(outputDir); // Use the File object instead of new File()
                
                logMessage(String.format("[%s] Recording to: %s", 
                    channelEntry.getPlatform(), outputFile));
                
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
        updateStatus("Stopped");
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