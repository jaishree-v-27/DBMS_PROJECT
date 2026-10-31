package com.bloodnetwork.ui;

import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.dao.RequestDAO;
import com.bloodnetwork.exception.InsufficientStockException;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.exception.NoMatchFoundException;
import com.bloodnetwork.matching.BloodMatcher;
import com.bloodnetwork.matching.CompatibleMatcher;
import com.bloodnetwork.matching.ExactMatcher;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.model.RequestStatus;
import com.bloodnetwork.model.Urgency;
import com.bloodnetwork.service.BloodIssueService;
import com.bloodnetwork.dao.StockDAO;
import com.bloodnetwork.util.Validator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Management panel for blood requests: filtering by status/urgency,
 * raising new requests, running matching strategies, issuing blood via transactions,
 * and cancelling requests.
 */
public class RequestsPanel extends JPanel {

    private final RequestDAO requestDAO = new RequestDAO();
    private final HospitalDAO hospitalDAO = new HospitalDAO();
    private final DonorDAO donorDAO = new DonorDAO();
    private final StockDAO stockDAO = new StockDAO();
    private final BloodIssueService bloodIssueService = new BloodIssueService(requestDAO, stockDAO, hospitalDAO);

    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<String> cmbFilter;

    public RequestsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(UITheme.LIGHT_BG);

        // Top Toolbar
        JPanel topBar = new JPanel(new BorderLayout(10, 10));
        topBar.setBackground(UITheme.LIGHT_BG);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterPanel.setBackground(UITheme.LIGHT_BG);
        filterPanel.add(new JLabel("Filter View:"));

        cmbFilter = new JComboBox<>(new String[]{
                "Pending (Sorted by Urgency)",
                "All Requests",
                "PENDING",
                "MATCHED",
                "FULFILLED",
                "CANCELLED"
        });
        cmbFilter.setFont(UITheme.FONT_BODY);
        cmbFilter.addActionListener(e -> loadRequests());
        filterPanel.add(cmbFilter);

        JButton btnRefresh = UITheme.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> loadRequests());
        filterPanel.add(btnRefresh);

        topBar.add(filterPanel, BorderLayout.WEST);

        // Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setBackground(UITheme.LIGHT_BG);

        JButton btnNew = UITheme.createPrimaryButton("+ New Request");
        btnNew.addActionListener(e -> showNewRequestDialog());
        actionPanel.add(btnNew);

        JButton btnMatch = UITheme.createSecondaryButton("Find Matches");
        btnMatch.addActionListener(e -> handleFindMatches());
        actionPanel.add(btnMatch);

        JButton btnIssue = UITheme.createSecondaryButton("Issue Blood");
        btnIssue.addActionListener(e -> handleIssueBlood());
        actionPanel.add(btnIssue);

        JButton btnCancel = UITheme.createDangerButton("Cancel Request");
        btnCancel.addActionListener(e -> handleCancelRequest());
        actionPanel.add(btnCancel);

        topBar.add(actionPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"Req ID", "Hospital", "City", "Patient", "Group", "Units", "Urgency", "Status", "Request Date"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(table);

        add(new JScrollPane(table), BorderLayout.CENTER);

        loadRequests();
    }

    private void loadRequests() {
        tableModel.setRowCount(0);
        String selectedFilter = (String) cmbFilter.getSelectedItem();

        try {
            List<BloodRequest> list;
            if ("Pending (Sorted by Urgency)".equals(selectedFilter)) {
                list = requestDAO.getPendingByUrgency();
            } else if ("All Requests".equals(selectedFilter)) {
                list = requestDAO.getAll();
            } else {
                RequestStatus status = RequestStatus.valueOf(selectedFilter);
                list = requestDAO.getByStatus(status);
            }

            for (BloodRequest req : list) {
                Hospital h = hospitalDAO.getById(req.getHospitalId());
                String hospName = (h != null) ? h.getName() : "Hosp #" + req.getHospitalId();
                String city = (h != null) ? h.getCity() : "-";

                tableModel.addRow(new Object[]{
                        req.getRequestId(),
                        hospName,
                        city,
                        req.getPatientName(),
                        req.getBloodGroup(),
                        req.getUnitsNeeded(),
                        req.getUrgency(),
                        req.getStatus(),
                        req.getRequestDate() != null ? req.getRequestDate().toString().replace('T', ' ') : "-"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load blood requests: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showNewRequestDialog() {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Raise New Blood Request", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(420, 360);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<HospitalComboItem> cmbHospital = new JComboBox<>();
        try {
            for (Hospital h : hospitalDAO.getAll()) {
                cmbHospital.addItem(new HospitalComboItem(h));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading hospitals: " + ex.getMessage());
        }

        JTextField txtPatient = new JTextField(16);
        JComboBox<String> cmbGroup = new JComboBox<>(new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        JTextField txtUnits = new JTextField("1", 16);
        JComboBox<Urgency> cmbUrgency = new JComboBox<>(Urgency.values());
        cmbUrgency.setSelectedItem(Urgency.NORMAL);

        int r = 0;
        addFormRow(form, gbc, r++, "Hospital:", cmbHospital);
        addFormRow(form, gbc, r++, "Patient Name:", txtPatient);
        addFormRow(form, gbc, r++, "Blood Group Needed:", cmbGroup);
        addFormRow(form, gbc, r++, "Units Needed:", txtUnits);
        addFormRow(form, gbc, r++, "Urgency Level:", cmbUrgency);

        dialog.add(form, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSubmit = UITheme.createPrimaryButton("Submit Request");
        JButton btnCancel = UITheme.createSecondaryButton("Cancel");

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSubmit.addActionListener(e -> {
            try {
                HospitalComboItem item = (HospitalComboItem) cmbHospital.getSelectedItem();
                if (item == null) {
                    throw new InvalidInputException("Please select a hospital.");
                }

                String patient = txtPatient.getText().trim();
                Validator.validateNonEmpty(patient, "Patient Name");

                String group = (String) cmbGroup.getSelectedItem();
                Validator.validateBloodGroup(group);

                int units;
                try {
                    units = Integer.parseInt(txtUnits.getText().trim());
                    if (units <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    throw new InvalidInputException("Units needed must be a positive integer.");
                }

                Urgency urgency = (Urgency) cmbUrgency.getSelectedItem();

                BloodRequest req = new BloodRequest(item.hospital.getHospitalId(), patient, group, units, urgency);
                int generatedId = requestDAO.add(req);

                JOptionPane.showMessageDialog(dialog, "Blood Request created successfully!\nRequest ID: " + generatedId, "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadRequests();

            } catch (InvalidInputException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Input Error", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBar.add(btnCancel);
        btnBar.add(btnSubmit);
        dialog.add(btnBar, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void handleFindMatches() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request from the table to find matching donors.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int reqId = (int) tableModel.getValueAt(row, 0);

        try {
            BloodRequest request = requestDAO.getById(reqId);
            if (request == null) {
                JOptionPane.showMessageDialog(this, "Request not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
            JDialog matchDialog = new JDialog(parent, "Find Matching Donors - Request #" + reqId, true);
            matchDialog.setLayout(new BorderLayout(10, 10));
            matchDialog.setSize(620, 420);
            matchDialog.setLocationRelativeTo(this);

            // Top Options Panel
            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
            topPanel.setBackground(UITheme.LIGHT_BG);
            topPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

            JLabel lblMode = new JLabel("Strategy:");
            lblMode.setFont(UITheme.FONT_BODY_BOLD);
            topPanel.add(lblMode);

            JRadioButton rbCompatible = new JRadioButton("Compatible Match (Recommended)", true);
            rbCompatible.setFont(UITheme.FONT_BODY);
            JRadioButton rbExact = new JRadioButton("Exact Match Only (" + request.getBloodGroup() + ")");
            rbExact.setFont(UITheme.FONT_BODY);

            ButtonGroup group = new ButtonGroup();
            group.add(rbCompatible);
            group.add(rbExact);

            topPanel.add(rbCompatible);
            topPanel.add(rbExact);

            JButton btnRunMatch = UITheme.createPrimaryButton("Search Donors");
            topPanel.add(btnRunMatch);
            matchDialog.add(topPanel, BorderLayout.NORTH);

            // Results Table
            String[] cols = {"Donor ID", "Name", "Group", "City", "Phone", "Last Donation"};
            DefaultTableModel matchModel = new DefaultTableModel(cols, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };
            JTable matchTable = new JTable(matchModel);
            matchTable.setRowHeight(22);
            matchTable.setFont(UITheme.FONT_BODY);
            matchTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);

            matchDialog.add(new JScrollPane(matchTable), BorderLayout.CENTER);

            // Search execution logic
            Runnable executeMatch = () -> {
                matchModel.setRowCount(0);
                BloodMatcher matcher = rbExact.isSelected()
                        ? new ExactMatcher(donorDAO, hospitalDAO)
                        : new CompatibleMatcher(donorDAO, hospitalDAO);

                try {
                    List<Donor> matches = matcher.findMatches(request);
                    if (matches == null || matches.isEmpty()) {
                        throw new NoMatchFoundException("No eligible donors found matching Request #" + reqId
                                + " (" + request.getBloodGroup() + ").");
                    }

                    for (Donor d : matches) {
                        matchModel.addRow(new Object[]{
                                d.getDonorId(),
                                d.getName(),
                                d.getBloodGroup(),
                                d.getCity(),
                                d.getPhone(),
                                d.getLastDonationDate() != null ? d.getLastDonationDate().toString() : "Never Donated"
                        });
                    }

                    // Update request status to MATCHED if PENDING
                    if (request.getStatus() == RequestStatus.PENDING) {
                        requestDAO.updateStatus(reqId, RequestStatus.MATCHED);
                        loadRequests();
                    }

                } catch (NoMatchFoundException ex) {
                    JOptionPane.showMessageDialog(matchDialog, ex.getMessage(), "No Match Found", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(matchDialog, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            };

            btnRunMatch.addActionListener(e -> executeMatch.run());

            // Bottom Close button
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
            JButton btnClose = UITheme.createSecondaryButton("Close");
            btnClose.addActionListener(e -> matchDialog.dispose());
            bottom.add(btnClose);
            matchDialog.add(bottom, BorderLayout.SOUTH);

            // Run initial match
            executeMatch.run();

            matchDialog.setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching request: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleIssueBlood() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request from the table to issue blood.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int reqId = (int) tableModel.getValueAt(row, 0);
        String patient = (String) tableModel.getValueAt(row, 3);
        String group = (String) tableModel.getValueAt(row, 4);
        int units = (int) tableModel.getValueAt(row, 5);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Issue " + units + " unit(s) of " + group + " for patient '" + patient + "' (Request #" + reqId + ")?\n"
                        + "This will atomically deduct the inventory and fulfill the request.",
                "Confirm Blood Issuance (ACID Transaction)",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                bloodIssueService.issueBlood(reqId);
                JOptionPane.showMessageDialog(this, "Blood successfully issued! Units deducted from stock and Request #" + reqId + " marked as FULFILLED.", "Issuance Complete", JOptionPane.INFORMATION_MESSAGE);
                loadRequests();
            } catch (InsufficientStockException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Insufficient Inventory", JOptionPane.ERROR_MESSAGE);
            } catch (InvalidInputException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Status", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database transaction error: " + ex.getMessage(), "Transaction Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleCancelRequest() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request to cancel.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int reqId = (int) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel Blood Request #" + reqId + "?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean ok = requestDAO.cancel(reqId);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Request #" + reqId + " has been cancelled.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadRequests();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to cancel request.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
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

    private static class HospitalComboItem {
        final Hospital hospital;
        HospitalComboItem(Hospital h) { this.hospital = h; }
        @Override
        public String toString() {
            return hospital.getName() + " (" + hospital.getCity() + ")";
        }
    }
}
