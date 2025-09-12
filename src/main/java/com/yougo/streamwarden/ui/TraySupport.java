package com.yougo.streamwarden.ui;

import javafx.application.Platform;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public final class TraySupport {

    private static TrayIcon trayIcon;

    private TraySupport() {}

    public static boolean isSupported() {
        return SystemTray.isSupported();
    }

    public static boolean isInstalled() {
        return trayIcon != null;
    }

    /**
     * Install a tray icon with Open and Exit actions.
     * onExit will be called when user clicks Exit in tray menu (use it to trigger graceful shutdown).
     */
    public static synchronized void install(Stage stage, Runnable onExit) {
        if (!isSupported() || isInstalled()) {
            return;
        }

        try {
            System.setProperty("java.awt.headless", "false");
            // Initialize AWT Toolkit to avoid lazy init glitches on some desktops
            Toolkit.getDefaultToolkit();

            // Load tray image from resources and scale to 16x16 for best Windows compatibility
            Image image = loadAwtImage("/assets/icons/app_icon.png", 16);

            PopupMenu popup = new PopupMenu();

            MenuItem openItem = new MenuItem("Open StreamWarden");
            ActionListener openAction = e -> Platform.runLater(() -> {
                try {
                    if (!stage.isShowing()) {
                        stage.show();
                    }
                    stage.setIconified(false);
                    stage.toFront();
                    stage.requestFocus();
                } catch (Exception ignored) {}
            });
            openItem.addActionListener(openAction);
            popup.add(openItem);

            popup.addSeparator();

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                // Remove tray icon immediately, then run provided shutdown routine on FX thread
                try { SystemTray.getSystemTray().remove(trayIcon); } catch (Exception ignored) {}
                trayIcon = null;
                if (onExit != null) {
                    onExit.run();
                }
            });
            popup.add(exitItem);

            trayIcon = new TrayIcon(image, "StreamWarden", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(openAction); // double-click tray icon restores window

            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception e) {
            // If tray fails, just ignore and continue without tray
            System.err.println("Failed to install system tray icon: " + e.getMessage());
            trayIcon = null;
        }
    }

    public static synchronized void uninstall() {
        if (trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (Exception ignored) {}
            trayIcon = null;
        }
    }

    public static void displayInfo(String title, String message) {
        if (trayIcon != null) {
            try {
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
            } catch (Exception ignored) {}
        }
    }

    private static Image loadAwtImage(String resourcePath, int size) throws Exception {
        try (InputStream in = TraySupport.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            BufferedImage original = ImageIO.read(in);
            if (original == null) {
                throw new IllegalArgumentException("Unable to read image: " + resourcePath);
            }
            BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(original, 0, 0, size, size, null);
            g2.dispose();
            return scaled;
        }
    }
}