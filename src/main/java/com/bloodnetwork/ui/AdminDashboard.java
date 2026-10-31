package com.bloodnetwork.ui;

import com.bloodnetwork.model.Admin;
import com.bloodnetwork.util.Session;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Main administrative dashboard window for managing the blood donor network.
 * Dynamically builds navigation tabs based on the polymorphic getDashboardMenu()
 * method of the Admin model.
 */
public class AdminDashboard extends JFrame {

    private final Admin admin;

    public AdminDashboard(Admin admin) {
        this.admin = admin;

        setTitle("Blood Donor Network - Administrator Portal (" + admin.getUsername() + ")");
        setSize(1060, 720);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top Navigation Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UITheme.PRIMARY_RED);
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JLabel lblLogo = new JLabel("🏥");
        lblLogo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        leftPanel.add(lblLogo);

        JLabel lblTitle = new JLabel("Blood Donor Network — Administrative Console");
        lblTitle.setFont(UITheme.FONT_HEADER);
        lblTitle.setForeground(UITheme.WHITE);
        leftPanel.add(lblTitle);

        topBar.add(leftPanel, BorderLayout.WEST);

        // User info & Logout on right
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        userPanel.setOpaque(false);

        // User status pill
        JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        pill.setBackground(UITheme.PRIMARY_DARK);
        pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(239, 83, 80), 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));

        JLabel lblUser = new JLabel("● " + admin.getUsername() + " (ADMIN)");
        lblUser.setFont(UITheme.FONT_BODY_BOLD);
        lblUser.setForeground(new Color(255, 235, 238));
        pill.add(lblUser);
        userPanel.add(pill);

        JButton btnLogout = UITheme.createSecondaryButton("Sign Out");
        btnLogout.addActionListener(e -> {
            Session.logout();
            dispose();
            new LoginFrame().setVisible(true);
        });
        userPanel.add(btnLogout);

        topBar.add(userPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // JTabbedPane built dynamically from admin.getDashboardMenu()
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BODY_BOLD);
        tabbedPane.setBackground(UITheme.WHITE);

        String[] menuItems = admin.getDashboardMenu();
        for (String item : menuItems) {
            switch (item) {
                case "Donors":
                    tabbedPane.addTab("  👤 Donors  ", new DonorsPanel());
                    break;
                case "Hospitals":
                    tabbedPane.addTab("  🏥 Hospitals  ", new HospitalsPanel());
                    break;
                case "Requests":
                    tabbedPane.addTab("  📋 Blood Requests  ", new RequestsPanel());
                    break;
                case "Donations":
                    tabbedPane.addTab("  💉 Donations  ", new DonationsPanel());
                    break;
                case "Stock":
                    tabbedPane.addTab("  📊 Blood Stock  ", new StockPanel());
                    break;
                case "Reports":
                    tabbedPane.addTab("  📈 Reports & Analytics  ", new ReportsPanel());
                    break;
                default:
                    JPanel p = new JPanel();
                    p.add(new JLabel("Tab: " + item));
                    tabbedPane.addTab(item, p);
                    break;
            }
        }

        add(tabbedPane, BorderLayout.CENTER);
    }
}
