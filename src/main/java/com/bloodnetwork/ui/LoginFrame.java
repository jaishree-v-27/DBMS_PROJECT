package com.bloodnetwork.ui;

import com.bloodnetwork.exception.AuthenticationException;
import com.bloodnetwork.model.Admin;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Requester;
import com.bloodnetwork.model.User;
import com.bloodnetwork.service.AuthService;
import com.bloodnetwork.util.Session;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Authentication login window for Blood Donor Network.
 * Dispatches authenticated users to their corresponding role dashboards
 * (AdminDashboard, DonorDashboard, RequesterDashboard).
 * Also provides self-service donor registration via SignUpDialog.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService = new AuthService();

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblError;
    private JButton btnLogin;
    private JButton btnSignUp;

    public LoginFrame() {
        setTitle("Blood Donor Network - Clinical Sign In");
        setSize(520, 520);
        setMinimumSize(new Dimension(480, 480));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Header Banner with Clinical Identity
        JPanel header = UITheme.createHeaderBanner("Blood Donor Network", "v1.0 • Clinical Auth");
        add(header, BorderLayout.NORTH);

        // Main Background Panel
        JPanel backgroundWrapper = new JPanel(new GridBagLayout());
        backgroundWrapper.setBackground(UITheme.LIGHT_BG);
        backgroundWrapper.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Center Card Panel
        JPanel cardPanel = UITheme.createCardPanel();
        cardPanel.setLayout(new GridBagLayout());
        cardPanel.setPreferredSize(new Dimension(430, 410));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Medical Icon & Title
        JLabel lblLogo = new JLabel("🩸 SYSTEM SIGN IN", SwingConstants.CENTER);
        lblLogo.setFont(UITheme.FONT_TITLE);
        lblLogo.setForeground(UITheme.PRIMARY_RED);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        cardPanel.add(lblLogo, gbc);

        JLabel lblSubtitle = new JLabel("Clinical Transfusion & Voluntary Donor Network", SwingConstants.CENTER);
        lblSubtitle.setFont(UITheme.FONT_SMALL);
        lblSubtitle.setForeground(UITheme.TEXT_MUTED);
        gbc.gridy = 1;
        cardPanel.add(lblSubtitle, gbc);

        // Error message label
        lblError = new JLabel(" ", SwingConstants.CENTER);
        lblError.setFont(UITheme.FONT_BODY_BOLD);
        lblError.setForeground(UITheme.DANGER_RED);
        gbc.gridy = 2;
        cardPanel.add(lblError, gbc);

        // Username Label & Field
        gbc.gridwidth = 1;
        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel lblUser = new JLabel("Username:");
        lblUser.setFont(UITheme.FONT_BODY_BOLD);
        lblUser.setForeground(UITheme.TEXT_DARK);
        cardPanel.add(lblUser, gbc);

        gbc.gridx = 1;
        txtUsername = new JTextField(16);
        UITheme.styleTextField(txtUsername);
        cardPanel.add(txtUsername, gbc);

        // Password Label & Field
        gbc.gridy = 4;
        gbc.gridx = 0;
        JLabel lblPass = new JLabel("Password:");
        lblPass.setFont(UITheme.FONT_BODY_BOLD);
        lblPass.setForeground(UITheme.TEXT_DARK);
        cardPanel.add(lblPass, gbc);

        gbc.gridx = 1;
        txtPassword = new JPasswordField(16);
        UITheme.styleTextField(txtPassword);
        cardPanel.add(txtPassword, gbc);

        // Buttons Panel (Login + Sign Up)
        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        btnPanel.setOpaque(false);

        btnLogin = UITheme.createPrimaryButton("Sign In");
        btnLogin.setPreferredSize(new Dimension(120, 36));
        btnLogin.addActionListener(e -> performLogin());
        btnPanel.add(btnLogin);

        btnSignUp = UITheme.createSecondaryButton("New Donor? Register");
        btnSignUp.setPreferredSize(new Dimension(170, 36));
        btnSignUp.addActionListener(e -> {
            SignUpDialog dialog = new SignUpDialog(this);
            dialog.setVisible(true);
        });
        btnPanel.add(btnSignUp);

        cardPanel.add(btnPanel, gbc);

        // Demo Credentials Hint Card
        gbc.gridy = 6;
        JPanel demoPanel = new JPanel(new BorderLayout());
        demoPanel.setBackground(UITheme.BG_HOVER_LIGHT);
        demoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblHints = new JLabel("<html><center><font color='#475569'><b>Quick Demo Credentials:</b><br>"
                + "👑 Admin: <b>admin</b> / admin123<br>"
                + "🏥 Hospital: <b>apollo_chennai</b> / hospital123<br>"
                + "🩸 Donor: <b>rahul_sharma</b> / donor123</font></center></html>", SwingConstants.CENTER);
        lblHints.setFont(UITheme.FONT_SMALL);
        demoPanel.add(lblHints, BorderLayout.CENTER);

        cardPanel.add(demoPanel, gbc);

        backgroundWrapper.add(cardPanel);
        add(backgroundWrapper, BorderLayout.CENTER);

        // Set Enter key to trigger login button automatically
        getRootPane().setDefaultButton(btnLogin);
    }

    private void performLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        lblError.setText(" "); // clear error

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Please enter both username and password.");
            return;
        }

        try {
            User user = authService.login(username, password);

            // Successfully authenticated
            dispose();

            if (user instanceof Admin) {
                new AdminDashboard((Admin) user).setVisible(true);
            } else if (user instanceof Donor) {
                new DonorDashboard((Donor) user, username).setVisible(true);
            } else if (user instanceof Requester) {
                new RequesterDashboard((Requester) user, username).setVisible(true);
            } else {
                new PlaceholderFrame(user.getRole(), username).setVisible(true);
            }

        } catch (AuthenticationException e) {
            lblError.setText(e.getMessage());
            txtPassword.setText("");
            txtPassword.requestFocus();
        } catch (SQLException e) {
            lblError.setText("Database connection error: " + e.getMessage());
        } catch (Exception e) {
            lblError.setText("Login error: " + e.getMessage());
        }
    }
}
