package com.yougo.streamwarden.core;

import com.yougo.streamwarden.ChannelEntry;
import com.yougo.streamwarden.config.AppSettings;
import javafx.collections.ObservableList;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MonitoringService {
    
    private final ExecutorService executorService;
    private final Map<String, StreamMonitor> activeMonitors;
    private final AppSettings settings;
    private StreamMonitor.StatusCallback statusCallback;
    
    public MonitoringService(AppSettings settings) {
        this.settings = settings;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("StreamMonitor-" + t.getId());
            return t;
        });
        this.activeMonitors = new ConcurrentHashMap<>();
    }
    
    public void setStatusCallback(StreamMonitor.StatusCallback callback) {
        this.statusCallback = callback;
    }
    
    public void startMonitoring(ChannelEntry channelEntry, int checkInterval) {
        String key = getChannelKey(channelEntry);
        
        if (activeMonitors.containsKey(key)) {
            System.out.println("Already monitoring: " + key);
            return;
        }
        
        StreamMonitor monitor = new StreamMonitor(channelEntry, settings, checkInterval);
        monitor.setStatusCallback(statusCallback);
        
        activeMonitors.put(key, monitor);
        executorService.submit(monitor);
        
        System.out.println("Started monitoring: " + key);
    }
    
    public void stopMonitoring(ChannelEntry channelEntry) {
        String key = getChannelKey(channelEntry);
        StreamMonitor monitor = activeMonitors.remove(key);
        
        if (monitor != null) {
            monitor.stop();
            System.out.println("Stopped monitoring: " + key);
        }
    }
    
    public void startAllActiveChannels(ObservableList<ChannelEntry> channels) {
        for (ChannelEntry channel : channels) {
            if (channel.getIsActive()) {
                startMonitoring(channel, settings.getDefaultCheckInterval());
            }
        }
    }
    
    public void stopAllMonitoring() {
        for (StreamMonitor monitor : activeMonitors.values()) {
            monitor.stop();
        }
        activeMonitors.clear();
        System.out.println("Stopped all monitoring");
    }
    
    public boolean isMonitoring(ChannelEntry channelEntry) {
        String key = getChannelKey(channelEntry);
        StreamMonitor monitor = activeMonitors.get(key);
        return monitor != null && monitor.isRunning();
    }
    
    public void shutdown() {
        logMessage("Shutting down monitoring service...");
        
        // Stop all monitors and force stop their recording processes
        for (StreamMonitor monitor : activeMonitors.values()) {
            monitor.stop();
        }
        
        activeMonitors.clear();
        
        // Shutdown executor service
        executorService.shutdown();
        try {
            // Wait a bit for tasks to terminate
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logMessage("Executor did not terminate gracefully, forcing shutdown...");
                executorService.shutdownNow();
                // Wait a bit more
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    logMessage("Executor did not terminate after forced shutdown!");
                }
            }
        } catch (InterruptedException e) {
            logMessage("Shutdown interrupted, forcing immediate shutdown...");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logMessage("Monitoring service shutdown complete.");
    }
    
    private String getChannelKey(ChannelEntry channelEntry) {
        return channelEntry.getPlatform() + ":" + channelEntry.getChannelName();
    }
    
    public Map<String, StreamMonitor> getActiveMonitors() {
        return new ConcurrentHashMap<>(activeMonitors);
    }
    
    private void logMessage(String message) {
        System.out.println(message);
    }
}