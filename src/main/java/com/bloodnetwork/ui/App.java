package com.bloodnetwork.ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main application launcher for the Java Swing Graphical User Interface (GUI).
 */
public class App {

    public static void main(String[] args) {
        // Set native system look and feel for modern desktop appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default Swing look and feel if system theme fails
        }

        // Launch login window safely on the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
