package com.bloodnetwork.ui;

import com.bloodnetwork.util.Session;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Placeholder frame displayed for DONOR and REQUESTER roles until Phase 5B.
 */
public class PlaceholderFrame extends JFrame {

    public PlaceholderFrame(String role, String username) {
        setTitle("Blood Donor Network - " + role + " Portal");
        setSize(480, 320);
        setMinimumSize(new Dimension(420, 260));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Header Banner
        JPanel header = UITheme.createHeaderBanner("Blood Donor Network", "Role: " + role);
        add(header, BorderLayout.NORTH);

        // Center Content
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(UITheme.LIGHT_BG);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel contentBox = new JPanel(new BorderLayout(10, 15));
        contentBox.setBackground(UITheme.LIGHT_BG);

        JLabel lblWelcome = new JLabel("Welcome, " + username + "!", SwingConstants.CENTER);
        lblWelcome.setFont(UITheme.FONT_HEADER);
        lblWelcome.setForeground(UITheme.TEXT_DARK);

        JLabel lblNotice = new JLabel("<html><center>The <b>" + role + "</b> dashboard is scheduled for <b>Phase 5B</b>.<br>"
                + "Please log in with the <b>admin</b> account to access the full management dashboard.</center></html>", SwingConstants.CENTER);
        lblNotice.setFont(UITheme.FONT_BODY);
        lblNotice.setForeground(UITheme.TEXT_MUTED);

        contentBox.add(lblWelcome, BorderLayout.NORTH);
        contentBox.add(lblNotice, BorderLayout.CENTER);

        centerPanel.add(contentBox);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom Bar with Logout Button
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        bottomBar.setBackground(UITheme.WHITE);
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(224, 224, 224)));

        JButton btnLogout = UITheme.createPrimaryButton("Logout");
        btnLogout.addActionListener(e -> {
            Session.logout();
            dispose();
            new LoginFrame().setVisible(true);
        });

        bottomBar.add(btnLogout);
        add(bottomBar, BorderLayout.SOUTH);
    }
}
