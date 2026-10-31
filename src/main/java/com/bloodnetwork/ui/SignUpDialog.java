package com.bloodnetwork.ui;

import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.service.RegistrationService;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Sign-up dialog allowing new voluntary blood donors to register.
 */
public class SignUpDialog extends JDialog {

    private final RegistrationService registrationService = new RegistrationService();

    public SignUpDialog(Frame owner) {
        super(owner, "Donor Registration - Blood Donor Network", true);
        setSize(480, 560);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // Header Banner
        JPanel header = UITheme.createHeaderBanner("New Donor Sign-Up", "Join the Network");
        add(header, BorderLayout.NORTH);

        // Form Panel
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.LIGHT_BG);
        form.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtUsername = new JTextField(15);
        UITheme.styleTextField(txtUsername);

        JPasswordField txtPassword = new JPasswordField(15);
        UITheme.styleTextField(txtPassword);

        JPasswordField txtConfirmPass = new JPasswordField(15);
        UITheme.styleTextField(txtConfirmPass);

        JTextField txtName = new JTextField(15);
        UITheme.styleTextField(txtName);

        JTextField txtAge = new JTextField(15);
        UITheme.styleTextField(txtAge);

        JComboBox<String> cmbGender = new JComboBox<>(new String[]{"M", "F", "O"});
        cmbGender.setFont(UITheme.FONT_BODY);

        JComboBox<String> cmbGroup = new JComboBox<>(new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        cmbGroup.setFont(UITheme.FONT_BODY);

        JTextField txtPhone = new JTextField(15);
        UITheme.styleTextField(txtPhone);

        JTextField txtCity = new JTextField(15);
        UITheme.styleTextField(txtCity);

        int r = 0;
        addFormRow(form, gbc, r++, "Username:", txtUsername);
        addFormRow(form, gbc, r++, "Password (min 6 chars):", txtPassword);
        addFormRow(form, gbc, r++, "Confirm Password:", txtConfirmPass);
        addFormRow(form, gbc, r++, "Full Name:", txtName);
        addFormRow(form, gbc, r++, "Age (18 - 65):", txtAge);
        addFormRow(form, gbc, r++, "Gender:", cmbGender);
        addFormRow(form, gbc, r++, "Blood Group:", cmbGroup);
        addFormRow(form, gbc, r++, "Phone (10 digits):", txtPhone);
        addFormRow(form, gbc, r++, "City:", txtCity);

        add(form, BorderLayout.CENTER);

        // Button Bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnBar.setBackground(UITheme.WHITE);
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(224, 224, 224)));

        JButton btnRegister = UITheme.createPrimaryButton("Register Account");
        JButton btnCancel = UITheme.createSecondaryButton("Cancel");

        btnCancel.addActionListener(e -> dispose());
        btnRegister.addActionListener(e -> {
            try {
                String username = txtUsername.getText().trim();
                String pass = new String(txtPassword.getPassword());
                String confirmPass = new String(txtConfirmPass.getPassword());
                String name = txtName.getText().trim();

                int age;
                try {
                    age = Integer.parseInt(txtAge.getText().trim());
                } catch (NumberFormatException ex) {
                    throw new InvalidInputException("Age must be a valid number between 18 and 65.");
                }

                String gender = (String) cmbGender.getSelectedItem();
                String group = (String) cmbGroup.getSelectedItem();
                String phone = txtPhone.getText().trim();
                String city = txtCity.getText().trim();

                int donorId = registrationService.registerDonor(
                        username, pass, confirmPass, name, age, gender, group, phone, city
                );

                JOptionPane.showMessageDialog(this,
                        "Registration Successful! Welcome to the network, " + name + ".\n"
                        + "Your Donor ID is #" + donorId + ".\nYou can now log in with your credentials.",
                        "Welcome", JOptionPane.INFORMATION_MESSAGE);

                dispose();

            } catch (InvalidInputException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration Error", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBar.add(btnCancel);
        btnBar.add(btnRegister);
        add(btnBar, BorderLayout.SOUTH);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component comp) {
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_BODY_BOLD);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        panel.add(comp, gbc);
    }
}
