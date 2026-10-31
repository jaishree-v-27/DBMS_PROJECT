package com.bloodnetwork.ui;

import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.dao.ReportDAO;
import com.bloodnetwork.dao.RequestDAO;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.matching.CompatibilityTable;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.model.RequestStatus;
import com.bloodnetwork.model.Urgency;
import com.bloodnetwork.util.Session;
import com.bloodnetwork.util.Validator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Donor Portal Dashboard (Phase 5B):
 * Allows logged-in blood donors to view/edit their own profile, evaluate their 90-day
 * medical donation eligibility, review their donation history, and browse open hospital
 * blood requests in their city that match their blood type.
 */
public class DonorDashboard extends JFrame {

    private final DonorDAO donorDAO = new DonorDAO();
    private final ReportDAO reportDAO = new ReportDAO();
    private final RequestDAO requestDAO = new RequestDAO();
    private final HospitalDAO hospitalDAO = new HospitalDAO();

    private final int donorId;
    private final String username;
    private Donor donor;

    public DonorDashboard(Donor donor, String username) {
        this.donor = donor;
        this.donorId = donor != null ? donor.getDonorId() : (Session.get() != null ? Session.get().getDonorId() : 0);
        this.username = username;

        setTitle("Blood Donor Network - Donor Portal (" + username + ")");
        setSize(960, 640);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Header Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UITheme.PRIMARY_RED);
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JLabel lblLogo = new JLabel("🩸");
        lblLogo.setFont(new java.awt.Font("Segoe UI Emoji", java.awt.Font.PLAIN, 22));
        leftPanel.add(lblLogo);

        JLabel lblTitle = new JLabel("Blood Donor Network — Voluntary Donor Portal");
        lblTitle.setFont(UITheme.FONT_HEADER);
        lblTitle.setForeground(UITheme.WHITE);
        leftPanel.add(lblTitle);

        topBar.add(leftPanel, BorderLayout.WEST);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        userPanel.setOpaque(false);

        // User status pill
        JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        pill.setBackground(UITheme.PRIMARY_DARK);
        pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(239, 83, 80), 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));

        JLabel lblUser = new JLabel("● " + username + " (DONOR)");
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

        // Build JTabbedPane dynamically from donor.getDashboardMenu()
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BODY_BOLD);
        tabbedPane.setBackground(UITheme.WHITE);

        String[] menuItems = (donor != null) ? donor.getDashboardMenu()
                : new String[]{"My Profile", "Eligibility", "Donation History", "Open Requests"};

        for (String item : menuItems) {
            switch (item) {
                case "My Profile":
                    tabbedPane.addTab("  👤 My Profile  ", createProfilePanel());
                    break;
                case "Eligibility":
                    tabbedPane.addTab("  🩺 Eligibility  ", createEligibilityPanel());
                    break;
                case "Donation History":
                    tabbedPane.addTab("  📜 Donation History  ", createHistoryPanel());
                    break;
                case "Open Requests":
                    tabbedPane.addTab("  🏥 Open Requests  ", createOpenRequestsPanel());
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

    // ------------------------------------------------------------------------
    // Tab A: My Profile
    // ------------------------------------------------------------------------
    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(UITheme.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 224, 224), 1, true),
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        refreshDonorFromDb();

        JTextField txtName = new JTextField(donor != null ? donor.getName() : "", 16);
        JTextField txtAge = new JTextField(donor != null ? String.valueOf(donor.getAge()) : "", 16);
        JComboBox<String> cmbGender = new JComboBox<>(new String[]{"M", "F", "O"});
        if (donor != null && donor.getGender() != null) cmbGender.setSelectedItem(donor.getGender());

        JTextField txtGroup = new JTextField(donor != null ? donor.getBloodGroup() : "", 16);
        txtGroup.setEditable(false);
        txtGroup.setBackground(new Color(245, 245, 245));

        JTextField txtPhone = new JTextField(donor != null ? donor.getPhone() : "", 16);
        JTextField txtCity = new JTextField(donor != null ? donor.getCity() : "", 16);

        String lastDonStr = (donor != null && donor.getLastDonationDate() != null)
                ? donor.getLastDonationDate().toString() : "Never donated";
        JLabel lblLastDon = new JLabel(lastDonStr);
        lblLastDon.setFont(UITheme.FONT_BODY_BOLD);

        JCheckBox chkAvailable = new JCheckBox("I am available to donate blood when contacted",
                donor != null && donor.isAvailable());
        chkAvailable.setFont(UITheme.FONT_BODY_BOLD);
        chkAvailable.setBackground(UITheme.WHITE);

        int r = 0;
        addFormRow(formCard, gbc, r++, "Full Name:", txtName);
        addFormRow(formCard, gbc, r++, "Age (18 - 65):", txtAge);
        addFormRow(formCard, gbc, r++, "Gender:", cmbGender);
        addFormRow(formCard, gbc, r++, "Blood Group (Read-Only):", txtGroup);
        addFormRow(formCard, gbc, r++, "Phone Number:", txtPhone);
        addFormRow(formCard, gbc, r++, "City:", txtCity);
        addFormRow(formCard, gbc, r++, "Last Donation Date:", lblLastDon);

        gbc.gridx = 1; gbc.gridy = r++;
        formCard.add(chkAvailable, gbc);

        // Save Button
        gbc.gridx = 1; gbc.gridy = r++;
        JButton btnSave = UITheme.createPrimaryButton("Save Profile Changes");
        btnSave.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                Validator.validateNonEmpty(name, "Full Name");

                int age;
                try {
                    age = Integer.parseInt(txtAge.getText().trim());
                } catch (NumberFormatException ex) {
                    throw new InvalidInputException("Age must be a valid number between 18 and 65.");
                }
                Validator.validateAge(age);

                String gender = (String) cmbGender.getSelectedItem();
                String phone = txtPhone.getText().trim();
                Validator.validatePhone(phone);

                String city = txtCity.getText().trim();
                Validator.validateNonEmpty(city, "City");

                boolean available = chkAvailable.isSelected();

                donor.setName(name);
                donor.setAge(age);
                donor.setGender(gender);
                donor.setPhone(phone);
                donor.setCity(city);
                donor.setAvailable(available);

                boolean ok = donorDAO.update(donor);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Could not update profile.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (InvalidInputException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        formCard.add(btnSave, gbc);

        panel.add(formCard, BorderLayout.NORTH);
        return panel;
    }

    // ------------------------------------------------------------------------
    // Tab B: Eligibility Status Card
    // ------------------------------------------------------------------------
    private JPanel createEligibilityPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(UITheme.LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        JPanel card = new JPanel(new BorderLayout(15, 15));
        card.setBackground(UITheme.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 224, 224), 1, true),
                BorderFactory.createEmptyBorder(25, 30, 25, 30)
        ));

        refreshDonorFromDb();

        boolean eligible = (donor != null) && donor.isEligible(90);

        JLabel lblStatus = new JLabel(eligible ? "✓ YOU ARE ELIGIBLE TO DONATE BLOOD" : "✗ NOT CURRENTLY ELIGIBLE TO DONATE", SwingConstants.CENTER);
        lblStatus.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 20));
        lblStatus.setForeground(eligible ? UITheme.SUCCESS_GREEN : UITheme.PRIMARY_RED);
        card.add(lblStatus, BorderLayout.NORTH);

        StringBuilder sb = new StringBuilder("<html><div style='font-size:13px; line-height:1.6;'>");
        if (donor != null) {
            sb.append("<b>Donor Name:</b> ").append(donor.getName()).append("<br>");
            sb.append("<b>Blood Group:</b> ").append(donor.getBloodGroup()).append("<br>");
            sb.append("<b>Age:</b> ").append(donor.getAge()).append(" (Permissible Range: 18 - 65)<br>");
            sb.append("<b>Availability Status:</b> ").append(donor.isAvailable() ? "Active (Available)" : "Marked Unavailable").append("<br>");

            if (donor.getLastDonationDate() == null) {
                sb.append("<b>Last Donation Date:</b> Never donated before (First-Time Donor)<br>");
            } else {
                long daysAgo = ChronoUnit.DAYS.between(donor.getLastDonationDate(), LocalDate.now());
                sb.append("<b>Last Donation Date:</b> ").append(donor.getLastDonationDate())
                  .append(" (").append(daysAgo).append(" days ago)<br>");
            }

            sb.append("<hr style='border:none; border-top:1px solid #ddd; margin:12px 0;'>");

            if (eligible) {
                sb.append("<p style='color:#2e7d32;'><b>Congratulations!</b> You meet all medical and age criteria.<br>")
                  .append("Please visit the <b>Open Requests</b> tab to view urgent hospital requirements in your city.</p>");
            } else {
                sb.append("<p style='color:#c62828;'><b>Reason for Ineligibility:</b><br>");
                if (donor.getAge() < 18 || donor.getAge() > 65) {
                    sb.append("• Age ").append(donor.getAge()).append(" is outside the permissible donation window (18-65).<br>");
                }
                if (!donor.isAvailable()) {
                    sb.append("• Your profile is currently marked as unavailable. You can re-enable availability under the <b>My Profile</b> tab.<br>");
                }
                if (donor.getLastDonationDate() != null) {
                    long daysAgo = ChronoUnit.DAYS.between(donor.getLastDonationDate(), LocalDate.now());
                    long daysLeft = 90 - daysAgo;
                    if (daysLeft > 0) {
                        LocalDate nextEligible = donor.getLastDonationDate().plusDays(90);
                        sb.append("• Mandatory recovery period: You must wait <b>").append(daysLeft)
                          .append(" more day(s)</b>. Your next eligible donation date is <b>")
                          .append(nextEligible).append("</b>.<br>");
                    }
                }
                sb.append("</p>");
            }
        }
        sb.append("</div></html>");

        JLabel lblDetails = new JLabel(sb.toString());
        card.add(lblDetails, BorderLayout.CENTER);

        JButton btnRefresh = UITheme.createPrimaryButton("Refresh Eligibility");
        btnRefresh.addActionListener(e -> {
            panel.removeAll();
            panel.add(createEligibilityPanel(), BorderLayout.CENTER);
            panel.revalidate();
            panel.repaint();
        });

        JPanel btm = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btm.setOpaque(false);
        btm.add(btnRefresh);
        card.add(btm, BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------------------
    // Tab C: Donation History
    // ------------------------------------------------------------------------
    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"Donation Date", "Hospital Name", "City", "Units Donated", "Request Ref"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(table);

        JLabel lblTotal = new JLabel("Total Units Donated: 0 units across 0 donation(s)");
        lblTotal.setFont(UITheme.FONT_BODY_BOLD);

        Runnable loadHistory = () -> {
            model.setRowCount(0);
            int totalUnits = 0;
            int count = 0;

            try {
                List<Map<String, Object>> rows = reportDAO.getDonationHistoryByDonor(donorId);
                for (Map<String, Object> row : rows) {
                    Object date = row.get("donation_date");
                    Object hosp = row.get("hospital_name");
                    Object city = row.get("hospital_city");
                    Object units = row.get("units");
                    Object reqId = row.get("request_id");

                    int u = (units instanceof Number) ? ((Number) units).intValue() : 1;
                    totalUnits += u;
                    count++;

                    model.addRow(new Object[]{
                            date != null ? date.toString() : "-",
                            hosp != null ? hosp.toString() : "-",
                            city != null ? city.toString() : "-",
                            u,
                            reqId != null ? "#" + reqId : "Voluntary"
                    });
                }
                lblTotal.setText("Total Units Donated: " + totalUnits + " unit(s) across " + count + " donation(s)");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Failed to load donation history: " + ex.getMessage());
            }
        };

        loadHistory.run();

        // Top toolbar
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblTotal, BorderLayout.WEST);

        JButton btnRefresh = UITheme.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> loadHistory.run());
        top.add(btnRefresh, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------------------
    // Tab D: Open Requests in Donor's City
    // ------------------------------------------------------------------------
    private JPanel createOpenRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"Hospital Name", "Patient", "Required Group", "Units Needed", "Urgency", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(table);

        JLabel lblCount = new JLabel("Matching Requests in Your City:");
        lblCount.setFont(UITheme.FONT_BODY_BOLD);

        Runnable loadRequests = () -> {
            model.setRowCount(0);
            refreshDonorFromDb();
            if (donor == null) return;

            String donorCity = donor.getCity() != null ? donor.getCity().trim() : "";
            String donorGroup = donor.getBloodGroup() != null ? donor.getBloodGroup().trim().toUpperCase() : "";

            try {
                List<BloodRequest> allRequests = requestDAO.getAll();
                List<BloodRequest> filtered = new ArrayList<>();

                for (BloodRequest req : allRequests) {
                    // Must be PENDING or MATCHED
                    if (req.getStatus() != RequestStatus.PENDING && req.getStatus() != RequestStatus.MATCHED) {
                        continue;
                    }

                    // Must be in the donor's city
                    Hospital h = hospitalDAO.getById(req.getHospitalId());
                    if (h == null || !h.getCity().equalsIgnoreCase(donorCity)) {
                        continue;
                    }

                    // Request blood group must be compatible with donor's blood group
                    List<String> compatibleGroups = CompatibilityTable.donorGroupsFor(req.getBloodGroup());
                    if (!compatibleGroups.contains(donorGroup)) {
                        continue;
                    }

                    filtered.add(req);
                }

                // Sort by urgency: CRITICAL first, then HIGH, then NORMAL
                filtered.sort(Comparator.comparingInt(r -> {
                    if (r.getUrgency() == Urgency.CRITICAL) return 1;
                    if (r.getUrgency() == Urgency.HIGH) return 2;
                    return 3;
                }));

                for (BloodRequest req : filtered) {
                    Hospital h = hospitalDAO.getById(req.getHospitalId());
                    String hospName = (h != null) ? h.getName() : "Hosp #" + req.getHospitalId();

                    // Patient first name only for privacy
                    String firstName = req.getPatientName();
                    if (firstName != null && firstName.contains(" ")) {
                        firstName = firstName.split(" ")[0];
                    }

                    model.addRow(new Object[]{
                            hospName,
                            firstName,
                            req.getBloodGroup(),
                            req.getUnitsNeeded(),
                            req.getUrgency(),
                            req.getStatus()
                    });
                }

                lblCount.setText("Matching Requests in " + donorCity + " for " + donorGroup + " Donors: " + filtered.size() + " open request(s)");

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Failed to load open requests: " + ex.getMessage());
            }
        };

        loadRequests.run();

        // Top toolbar
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblCount, BorderLayout.WEST);

        JButton btnRefresh = UITheme.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> loadRequests.run());
        top.add(btnRefresh, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom Note
        JLabel lblNote = new JLabel("ℹ Note: Please contact the hospital directly if you can donate blood for any of these requests.", SwingConstants.CENTER);
        lblNote.setFont(UITheme.FONT_BODY_BOLD);
        lblNote.setForeground(UITheme.PRIMARY_RED);
        lblNote.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        panel.add(lblNote, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshDonorFromDb() {
        if (donorId > 0) {
            try {
                Donor fresh = donorDAO.getById(donorId);
                if (fresh != null) {
                    this.donor = fresh;
                }
            } catch (SQLException ignored) {}
        }
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
