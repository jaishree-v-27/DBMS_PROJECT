package com.bloodnetwork.ui;

import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.dao.RequestDAO;
import com.bloodnetwork.dao.StockDAO;
import com.bloodnetwork.exception.AccessDeniedException;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.exception.NoMatchFoundException;
import com.bloodnetwork.matching.CompatibleMatcher;
import com.bloodnetwork.matching.ExactMatcher;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.BloodStock;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.model.RequestStatus;
import com.bloodnetwork.model.Requester;
import com.bloodnetwork.model.Urgency;
import com.bloodnetwork.service.RequestService;
import com.bloodnetwork.util.Session;
import com.bloodnetwork.util.Validator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Hospital Requester Portal Dashboard (Phase 5B):
 * Allows logged-in hospital staff to raise blood requests, track and cancel
 * their own hospital's requests with strict access control, run donor matching
 * algorithms (Exact vs. Compatible), and monitor their hospital blood inventory.
 */
public class RequesterDashboard extends JFrame {

    private final RequestService requestService = new RequestService();
    private final RequestDAO requestDAO = new RequestDAO();
    private final HospitalDAO hospitalDAO = new HospitalDAO();
    private final StockDAO stockDAO = new StockDAO();
    private final ExactMatcher exactMatcher = new ExactMatcher();
    private final CompatibleMatcher compatibleMatcher = new CompatibleMatcher();

    private final Requester requester;
    private final String username;
    private final int hospitalId;
    private String hospitalName = "Unknown Hospital";
    private String hospitalCity = "";

    // Tab components for cross-tab refreshing
    private DefaultTableModel requestsTableModel;
    private JComboBox<String> cmbRequestStatusFilter;
    private JComboBox<RequestComboItem> cmbMatchRequests;
    private DefaultTableModel matchedDonorsModel;
    private DefaultTableModel stockTableModel;

    public RequesterDashboard(Requester requester, String username) {
        this.requester = requester;
        this.username = username;

        // Resolve hospital ID from requester or session
        int resolvedHospId = 0;
        if (requester != null && requester.getHospitalId() != null) {
            resolvedHospId = requester.getHospitalId();
        } else if (Session.get() != null && Session.get().getHospitalId() != null) {
            resolvedHospId = Session.get().getHospitalId();
        }
        this.hospitalId = resolvedHospId;

        // Lookup hospital metadata
        try {
            Hospital h = hospitalDAO.getById(hospitalId);
            if (h != null) {
                this.hospitalName = h.getName();
                this.hospitalCity = h.getCity();
            }
        } catch (SQLException e) {
            System.err.println("Warning: Could not fetch hospital details: " + e.getMessage());
        }

        setTitle("Blood Donor Network - Hospital Portal (" + username + " - " + hospitalName + ")");
        setSize(1000, 680);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Header Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UITheme.PRIMARY_RED);
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JLabel lblLogo = new JLabel("🏥");
        lblLogo.setFont(new java.awt.Font("Segoe UI Emoji", java.awt.Font.PLAIN, 22));
        leftPanel.add(lblLogo);

        JLabel lblTitle = new JLabel("Blood Donor Network — Hospital Portal (" + hospitalName + ")");
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

        JLabel lblUser = new JLabel("● " + username + " (" + hospitalCity + ")");
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

        // Build JTabbedPane dynamically from requester.getDashboardMenu()
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BODY_BOLD);
        tabbedPane.setBackground(UITheme.WHITE);

        String[] menuItems = (requester != null) ? requester.getDashboardMenu()
                : new String[]{"Raise Request", "My Requests", "Find Donors", "Our Stock"};

        for (String item : menuItems) {
            switch (item) {
                case "Raise Request":
                    tabbedPane.addTab("  📝 Raise Request  ", createRaiseRequestPanel());
                    break;
                case "My Requests":
                    tabbedPane.addTab("  📋 My Requests  ", createMyRequestsPanel());
                    break;
                case "Find Donors":
                    tabbedPane.addTab("  🔍 Find Donors  ", createFindDonorsPanel());
                    break;
                case "Our Stock":
                    tabbedPane.addTab("  📦 Our Stock  ", createOurStockPanel());
                    break;
                default:
                    JPanel p = new JPanel();
                    p.add(new JLabel("Tab: " + item));
                    tabbedPane.addTab(item, p);
                    break;
            }
        }

        // When switching tabs, automatically refresh relevant data
        tabbedPane.addChangeListener(e -> {
            int selectedIdx = tabbedPane.getSelectedIndex();
            String title = tabbedPane.getTitleAt(selectedIdx).trim();
            if ("My Requests".equals(title)) {
                loadHospitalRequests();
            } else if ("Find Donors".equals(title)) {
                loadMatchRequestsCombo();
            } else if ("Our Stock".equals(title)) {
                loadStockData();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==========================================
    // TAB 1: RAISE REQUEST
    // ==========================================
    private JPanel createRaiseRequestPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UITheme.LIGHT_BG);
        wrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(UITheme.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
                BorderFactory.createEmptyBorder(24, 30, 24, 30)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Card Header
        JLabel lblHeader = new JLabel("Raise New Patient Blood Request");
        lblHeader.setFont(UITheme.FONT_TITLE);
        lblHeader.setForeground(UITheme.PRIMARY_RED);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formCard.add(lblHeader, gbc);

        JLabel lblSub = new JLabel("<html>Submit an urgent or scheduled blood demand on behalf of <b>"
                + hospitalName + "</b> (City: " + hospitalCity + ").</html>");
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(UITheme.TEXT_MUTED);
        gbc.gridy = 1;
        formCard.add(lblSub, gbc);

        // Hospital Field (Read-only)
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel lblHospTitle = new JLabel("Requesting Hospital:");
        lblHospTitle.setFont(UITheme.FONT_BODY_BOLD);
        formCard.add(lblHospTitle, gbc);

        gbc.gridx = 1;
        JLabel lblHospVal = new JLabel(hospitalName + " (ID: #" + hospitalId + ", " + hospitalCity + ")");
        lblHospVal.setFont(UITheme.FONT_BODY);
        lblHospVal.setForeground(UITheme.TEXT_MUTED);
        formCard.add(lblHospVal, gbc);

        // Patient Name
        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel lblPatient = new JLabel("Patient Name *:");
        lblPatient.setFont(UITheme.FONT_BODY_BOLD);
        formCard.add(lblPatient, gbc);

        gbc.gridx = 1;
        JTextField txtPatient = new JTextField(22);
        txtPatient.setFont(UITheme.FONT_BODY);
        formCard.add(txtPatient, gbc);

        // Blood Group
        gbc.gridy = 4;
        gbc.gridx = 0;
        JLabel lblGroup = new JLabel("Required Blood Group *:");
        lblGroup.setFont(UITheme.FONT_BODY_BOLD);
        formCard.add(lblGroup, gbc);

        gbc.gridx = 1;
        JComboBox<String> cmbGroup = new JComboBox<>(new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        cmbGroup.setFont(UITheme.FONT_BODY);
        formCard.add(cmbGroup, gbc);

        // Units Needed
        gbc.gridy = 5;
        gbc.gridx = 0;
        JLabel lblUnits = new JLabel("Units Needed (1 - 10) *:");
        lblUnits.setFont(UITheme.FONT_BODY_BOLD);
        formCard.add(lblUnits, gbc);

        gbc.gridx = 1;
        JTextField txtUnits = new JTextField("1", 10);
        txtUnits.setFont(UITheme.FONT_BODY);
        formCard.add(txtUnits, gbc);

        // Urgency
        gbc.gridy = 6;
        gbc.gridx = 0;
        JLabel lblUrgency = new JLabel("Urgency Level *:");
        lblUrgency.setFont(UITheme.FONT_BODY_BOLD);
        formCard.add(lblUrgency, gbc);

        gbc.gridx = 1;
        JComboBox<Urgency> cmbUrgency = new JComboBox<>(Urgency.values());
        cmbUrgency.setSelectedItem(Urgency.NORMAL);
        cmbUrgency.setFont(UITheme.FONT_BODY);
        formCard.add(cmbUrgency, gbc);

        // Button Panel
        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setOpaque(false);

        JButton btnClear = UITheme.createSecondaryButton("Reset Form");
        btnClear.addActionListener(e -> {
            txtPatient.setText("");
            cmbGroup.setSelectedIndex(0);
            txtUnits.setText("1");
            cmbUrgency.setSelectedItem(Urgency.NORMAL);
        });
        btnPanel.add(btnClear);

        JButton btnSubmit = UITheme.createPrimaryButton("Submit Request");
        btnSubmit.addActionListener(e -> {
            String patientName = txtPatient.getText().trim();
            String group = (String) cmbGroup.getSelectedItem();
            String unitsText = txtUnits.getText().trim();
            Urgency urgency = (Urgency) cmbUrgency.getSelectedItem();

            try {
                Validator.validateNonEmpty(patientName, "Patient name");
                Validator.validateBloodGroup(group);

                int units;
                try {
                    units = Integer.parseInt(unitsText);
                    if (units <= 0 || units > 20) {
                        throw new InvalidInputException("Units needed must be between 1 and 20.");
                    }
                } catch (NumberFormatException nfe) {
                    throw new InvalidInputException("Units needed must be a valid whole number.");
                }

                BloodRequest req = new BloodRequest(
                        hospitalId,
                        patientName,
                        group,
                        units,
                        urgency
                );

                int newId = requestDAO.add(req);

                JOptionPane.showMessageDialog(this,
                        "Blood request #" + newId + " has been successfully submitted!\nStatus: PENDING",
                        "Request Submitted",
                        JOptionPane.INFORMATION_MESSAGE);

                txtPatient.setText("");
                cmbGroup.setSelectedIndex(0);
                txtUnits.setText("1");
                cmbUrgency.setSelectedItem(Urgency.NORMAL);

                // Refresh table if initialized
                loadHospitalRequests();
                loadMatchRequestsCombo();

            } catch (InvalidInputException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Input Validation Error", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error submitting request: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnPanel.add(btnSubmit);

        formCard.add(btnPanel, gbc);

        GridBagConstraints wrapperGbc = new GridBagConstraints();
        wrapperGbc.gridx = 0;
        wrapperGbc.gridy = 0;
        wrapper.add(formCard, wrapperGbc);

        return wrapper;
    }

    // ==========================================
    // TAB 2: MY REQUESTS
    // ==========================================
    private JPanel createMyRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(UITheme.LIGHT_BG);

        // Top Toolbar
        JPanel topBar = new JPanel(new BorderLayout(10, 10));
        topBar.setBackground(UITheme.LIGHT_BG);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterPanel.setBackground(UITheme.LIGHT_BG);
        filterPanel.add(new JLabel("Status Filter:"));

        cmbRequestStatusFilter = new JComboBox<>(new String[]{"ALL", "PENDING", "MATCHED", "FULFILLED", "CANCELLED"});
        cmbRequestStatusFilter.setFont(UITheme.FONT_BODY);
        cmbRequestStatusFilter.addActionListener(e -> loadHospitalRequests());
        filterPanel.add(cmbRequestStatusFilter);

        JButton btnRefresh = UITheme.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> loadHospitalRequests());
        filterPanel.add(btnRefresh);

        topBar.add(filterPanel, BorderLayout.WEST);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setBackground(UITheme.LIGHT_BG);

        JButton btnCancel = UITheme.createDangerButton("Cancel Selected Request");

        JTable table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(table);

        String[] cols = {"Request ID", "Patient Name", "Blood Group", "Units Needed", "Urgency", "Status", "Request Date"};
        requestsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table.setModel(requestsTableModel);

        btnCancel.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a request from the table to cancel.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int reqId = (int) table.getValueAt(selectedRow, 0);
            String patient = (String) table.getValueAt(selectedRow, 1);
            String status = (String) table.getValueAt(selectedRow, 5);

            if ("CANCELLED".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(this, "Request #" + reqId + " is already cancelled.", "Information", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if ("FULFILLED".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(this, "Cannot cancel a request that has already been fulfilled.", "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to cancel Blood Request #" + reqId + " for " + patient + "?",
                    "Confirm Cancellation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    // Invokes RequestService enforcing hospital isolation check
                    boolean ok = requestService.cancelOwnRequest(reqId, hospitalId);
                    if (ok) {
                        JOptionPane.showMessageDialog(this, "Request #" + reqId + " has been successfully cancelled.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                        loadHospitalRequests();
                        loadMatchRequestsCombo();
                    }
                } catch (AccessDeniedException ade) {
                    JOptionPane.showMessageDialog(this, ade.getMessage(), "Security Violation", JOptionPane.ERROR_MESSAGE);
                } catch (InvalidInputException iie) {
                    JOptionPane.showMessageDialog(this, iie.getMessage(), "Invalid Request", JOptionPane.WARNING_MESSAGE);
                } catch (SQLException sqle) {
                    JOptionPane.showMessageDialog(this, "Database error: " + sqle.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        actionsPanel.add(btnCancel);
        topBar.add(actionsPanel, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        loadHospitalRequests();
        return panel;
    }

    private void loadHospitalRequests() {
        if (requestsTableModel == null) return;
        requestsTableModel.setRowCount(0);

        String selectedFilter = cmbRequestStatusFilter != null ? (String) cmbRequestStatusFilter.getSelectedItem() : "ALL";

        try {
            List<BloodRequest> list = requestService.getRequestsForHospital(hospitalId);
            for (BloodRequest req : list) {
                if (selectedFilter != null && !selectedFilter.equals("ALL")) {
                    if (!req.getStatus().name().equalsIgnoreCase(selectedFilter)) {
                        continue;
                    }
                }
                requestsTableModel.addRow(new Object[]{
                        req.getRequestId(),
                        req.getPatientName(),
                        req.getBloodGroup(),
                        req.getUnitsNeeded(),
                        req.getUrgency().name(),
                        req.getStatus().name(),
                        req.getRequestDate() != null ? req.getRequestDate().toString() : "N/A"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load hospital requests: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // TAB 3: FIND DONORS
    // ==========================================
    private JPanel createFindDonorsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(UITheme.LIGHT_BG);

        // Control Panel
        JPanel controlCard = new JPanel(new GridBagLayout());
        controlCard.setBackground(UITheme.WHITE);
        controlCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblSelReq = new JLabel("Select Blood Request:");
        lblSelReq.setFont(UITheme.FONT_BODY_BOLD);
        gbc.gridx = 0;
        gbc.gridy = 0;
        controlCard.add(lblSelReq, gbc);

        cmbMatchRequests = new JComboBox<>();
        cmbMatchRequests.setFont(UITheme.FONT_BODY);
        cmbMatchRequests.setPreferredSize(new Dimension(380, 28));
        gbc.gridx = 1;
        controlCard.add(cmbMatchRequests, gbc);

        JButton btnReloadCombo = UITheme.createSecondaryButton("Refresh Requests");
        btnReloadCombo.addActionListener(e -> loadMatchRequestsCombo());
        gbc.gridx = 2;
        controlCard.add(btnReloadCombo, gbc);

        // Match Strategy Radios
        JLabel lblStrategy = new JLabel("Matching Strategy:");
        lblStrategy.setFont(UITheme.FONT_BODY_BOLD);
        gbc.gridy = 1;
        gbc.gridx = 0;
        controlCard.add(lblStrategy, gbc);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        radioPanel.setOpaque(false);

        JRadioButton radExact = new JRadioButton("Exact Match Only (Same Blood Group)");
        radExact.setSelected(true);
        radExact.setFont(UITheme.FONT_BODY);
        radExact.setOpaque(false);

        JRadioButton radCompatible = new JRadioButton("Compatible Match (All Clinically Safe Blood Groups)");
        radCompatible.setFont(UITheme.FONT_BODY);
        radCompatible.setOpaque(false);

        ButtonGroup bGroup = new ButtonGroup();
        bGroup.add(radExact);
        bGroup.add(radCompatible);
        radioPanel.add(radExact);
        radioPanel.add(radCompatible);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        controlCard.add(radioPanel, gbc);

        // Run Match Button
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        runPanel.setOpaque(false);

        JButton btnFind = UITheme.createPrimaryButton("Find Matching Donors in " + (hospitalCity.isEmpty() ? "City" : hospitalCity));
        btnFind.addActionListener(e -> {
            RequestComboItem item = (RequestComboItem) cmbMatchRequests.getSelectedItem();
            if (item == null || item.request == null) {
                JOptionPane.showMessageDialog(this, "No active request selected. Please select or raise a request first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            BloodRequest req = item.request;
            matchedDonorsModel.setRowCount(0);

            try {
                List<Donor> matches;
                if (radExact.isSelected()) {
                    matches = exactMatcher.findMatches(req);
                } else {
                    matches = compatibleMatcher.findMatches(req);
                }

                if (matches.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "No eligible donors found matching patient group " + req.getBloodGroup()
                                    + " in " + hospitalCity + ".\n"
                                    + (radExact.isSelected() ? "Try selecting 'Compatible Match' for wider results." : "Please check back later or contact other centers."),
                            "No Donors Found",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                // If currently PENDING, update status to MATCHED in database
                if (req.getStatus() == RequestStatus.PENDING) {
                    requestDAO.updateStatus(req.getRequestId(), RequestStatus.MATCHED);
                    req.setStatus(RequestStatus.MATCHED);
                    loadHospitalRequests();
                    loadMatchRequestsCombo();
                }

                for (Donor d : matches) {
                    String lastDate = (d.getLastDonationDate() != null) ? d.getLastDonationDate().toString() : "Never Donated";
                    matchedDonorsModel.addRow(new Object[]{
                            d.getDonorId(),
                            d.getName(),
                            d.getBloodGroup(),
                            d.getAge(),
                            d.getGender() != null ? d.getGender() : "N/A",
                            d.getPhone(),
                            d.getCity(),
                            lastDate,
                            d.isAvailable() ? "Eligible & Ready" : "Unavailable"
                    });
                }

                JOptionPane.showMessageDialog(this,
                        "Found " + matches.size() + " eligible donor(s) in " + hospitalCity + "!\n"
                                + "Request #" + req.getRequestId() + " status is now MATCHED.",
                        "Matches Found",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error during donor matching: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        runPanel.add(btnFind);
        controlCard.add(runPanel, gbc);

        panel.add(controlCard, BorderLayout.NORTH);

        // Results Table
        String[] cols = {"Donor ID", "Donor Name", "Blood Group", "Age", "Gender", "Phone", "City", "Last Donation", "Status"};
        matchedDonorsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable donorTable = new JTable(matchedDonorsModel);
        donorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(donorTable);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Matched Donors Located in " + hospitalCity));
        tablePanel.add(new JScrollPane(donorTable), BorderLayout.CENTER);

        panel.add(tablePanel, BorderLayout.CENTER);

        loadMatchRequestsCombo();
        return panel;
    }

    private void loadMatchRequestsCombo() {
        if (cmbMatchRequests == null) return;
        cmbMatchRequests.removeAllItems();

        try {
            List<BloodRequest> list = requestService.getRequestsForHospital(hospitalId);
            for (BloodRequest req : list) {
                // Focus on requests that are PENDING or MATCHED
                if (req.getStatus() == RequestStatus.PENDING || req.getStatus() == RequestStatus.MATCHED) {
                    cmbMatchRequests.addItem(new RequestComboItem(req));
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not load matching requests combo: " + e.getMessage());
        }
    }

    // ==========================================
    // TAB 4: OUR STOCK
    // ==========================================
    private JPanel createOurStockPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(UITheme.LIGHT_BG);

        // Top Toolbar
        JPanel topBar = new JPanel(new BorderLayout(10, 10));
        topBar.setBackground(UITheme.LIGHT_BG);

        JLabel lblTitle = new JLabel("Inventory Levels for " + hospitalName);
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_DARK);
        topBar.add(lblTitle, BorderLayout.WEST);

        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightBar.setBackground(UITheme.LIGHT_BG);

        JLabel lblLegend = new JLabel("■ Low Stock Alert (< 3 units)");
        lblLegend.setFont(UITheme.FONT_BODY_BOLD);
        lblLegend.setForeground(new Color(198, 40, 40));
        rightBar.add(lblLegend);

        JButton btnRefresh = UITheme.createPrimaryButton("Refresh Stock");
        btnRefresh.addActionListener(e -> loadStockData());
        rightBar.add(btnRefresh);

        topBar.add(rightBar, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        // Table
        String[] cols = {"Blood Group", "Units Available", "Stock Status"};
        stockTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(stockTableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.setFont(UITheme.FONT_BODY);
        table.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);

        // Low stock cell renderer: highlights column with units < 3 in soft red
        LowStockRenderer renderer = new LowStockRenderer(1);
        for (int i = 0; i < cols.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        loadStockData();
        return panel;
    }

    private void loadStockData() {
        if (stockTableModel == null) return;
        stockTableModel.setRowCount(0);

        try {
            List<BloodStock> stocks = stockDAO.getByHospital(hospitalId);
            for (BloodStock s : stocks) {
                String status = (s.getUnitsAvailable() < 3) ? "LOW STOCK - Urgent replenishment needed" : "Sufficient";
                stockTableModel.addRow(new Object[]{
                        s.getBloodGroup(),
                        s.getUnitsAvailable(),
                        status
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load blood stock inventory: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // HELPER CLASSES & RENDERERS
    // ==========================================
    private static class RequestComboItem {
        final BloodRequest request;

        public RequestComboItem(BloodRequest req) {
            this.request = req;
        }

        @Override
        public String toString() {
            if (request == null) return "None";
            return "Req #" + request.getRequestId() + ": " + request.getPatientName()
                    + " (" + request.getBloodGroup() + ", " + request.getUnitsNeeded() + " units, "
                    + request.getUrgency() + " - " + request.getStatus() + ")";
        }
    }

    private static class LowStockRenderer extends DefaultTableCellRenderer {
        private final int unitsColumnIndex;

        public LowStockRenderer(int unitsColumnIndex) {
            this.unitsColumnIndex = unitsColumnIndex;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                Object unitsObj = table.getValueAt(row, unitsColumnIndex);
                int units = 0;
                if (unitsObj instanceof Number) {
                    units = ((Number) unitsObj).intValue();
                } else if (unitsObj != null) {
                    try {
                        units = Integer.parseInt(unitsObj.toString());
                    } catch (NumberFormatException ignored) {}
                }

                if (units < 3) {
                    c.setBackground(UITheme.WARNING_RED_BG);
                    c.setForeground(new Color(183, 28, 28));
                } else {
                    c.setBackground(UITheme.WHITE);
                    c.setForeground(UITheme.TEXT_DARK);
                }
            }

            return c;
        }
    }
}
