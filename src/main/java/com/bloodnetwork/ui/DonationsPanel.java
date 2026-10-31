package com.bloodnetwork.ui;

import com.bloodnetwork.dao.DonationDAO;
import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.dao.RequestDAO;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.exception.NotEligibleException;
import com.bloodnetwork.model.Donation;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.service.DonationService;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Management panel for recording donations, viewing donation logs,
 * and deleting donations with automated trigger adjustments.
 */
public class DonationsPanel extends JPanel {

    private final DonationDAO donationDAO = new DonationDAO();
    private final DonorDAO donorDAO = new DonorDAO();
    private final HospitalDAO hospitalDAO = new HospitalDAO();
    private final RequestDAO requestDAO = new RequestDAO();
    private final DonationService donationService = new DonationService(donationDAO, donorDAO, hospitalDAO, requestDAO);

    private JTable table;
    private DefaultTableModel tableModel;

    public DonationsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(UITheme.LIGHT_BG);

        // Top Toolbar
        JPanel topBar = new JPanel(new BorderLayout(10, 10));
        topBar.setBackground(UITheme.LIGHT_BG);

        JLabel lblTitle = new JLabel("Recorded Blood Donations");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_DARK);
        topBar.add(lblTitle, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setBackground(UITheme.LIGHT_BG);

        JButton btnRecord = UITheme.createPrimaryButton("+ Record Donation");
        btnRecord.addActionListener(e -> showRecordDonationDialog());
        actionPanel.add(btnRecord);

        JButton btnDelete = UITheme.createDangerButton("Delete Donation");
        btnDelete.addActionListener(e -> handleDeleteDonation());
        actionPanel.add(btnDelete);

        JButton btnRefresh = UITheme.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> loadDonations());
        actionPanel.add(btnRefresh);

        topBar.add(actionPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"Donation ID", "Donor ID", "Donor Name", "Hospital", "Request ID", "Units", "Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(table);

        add(new JScrollPane(table), BorderLayout.CENTER);

        loadDonations();
    }

    private void loadDonations() {
        tableModel.setRowCount(0);
        try {
            List<Donation> list = donationDAO.getAll();
            for (Donation d : list) {
                Donor donor = donorDAO.getById(d.getDonorId());
                Hospital hosp = hospitalDAO.getById(d.getHospitalId());

                String donorName = (donor != null) ? donor.getName() : "ID #" + d.getDonorId();
                String hospName = (hosp != null) ? hosp.getName() : "Hosp #" + d.getHospitalId();

                tableModel.addRow(new Object[]{
                        d.getDonationId(),
                        d.getDonorId(),
                        donorName,
                        hospName,
                        (d.getRequestId() != null ? String.valueOf(d.getRequestId()) : "Voluntary"),
                        d.getUnits(),
                        (d.getDonationDate() != null ? d.getDonationDate().toString() : "N/A")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load donations: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showRecordDonationDialog() {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Record Blood Donation", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(440, 360);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtDonorId = new JTextField(14);
        JComboBox<HospitalItem> cmbHosp = new JComboBox<>();
        try {
            for (Hospital h : hospitalDAO.getAll()) {
                cmbHosp.addItem(new HospitalItem(h));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading hospitals: " + ex.getMessage());
        }

        JTextField txtRequestId = new JTextField(14); // optional
        JTextField txtUnits = new JTextField("1", 14);
        JTextField txtDate = new JTextField(LocalDate.now().toString(), 14);

        int r = 0;
        addFormRow(form, gbc, r++, "Donor ID:", txtDonorId);
        addFormRow(form, gbc, r++, "Hospital:", cmbHosp);
        addFormRow(form, gbc, r++, "Request ID (optional):", txtRequestId);
        addFormRow(form, gbc, r++, "Units Donated:", txtUnits);
        addFormRow(form, gbc, r++, "Donation Date (YYYY-MM-DD):", txtDate);

        dialog.add(form, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSave = UITheme.createPrimaryButton("Record");
        JButton btnCancel = UITheme.createSecondaryButton("Cancel");

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            try {
                int donorId;
                try {
                    donorId = Integer.parseInt(txtDonorId.getText().trim());
                } catch (NumberFormatException ex) {
                    throw new InvalidInputException("Donor ID must be a valid number.");
                }

                HospitalItem hospItem = (HospitalItem) cmbHosp.getSelectedItem();
                if (hospItem == null) throw new InvalidInputException("Hospital must be selected.");
                int hospitalId = hospItem.hospital.getHospitalId();

                String reqStr = txtRequestId.getText().trim();
                Integer reqId = null;
                if (!reqStr.isEmpty()) {
                    try {
                        reqId = Integer.parseInt(reqStr);
                    } catch (NumberFormatException ex) {
                        throw new InvalidInputException("Request ID must be a number or left blank.");
                    }
                }

                int units;
                try {
                    units = Integer.parseInt(txtUnits.getText().trim());
                } catch (NumberFormatException ex) {
                    throw new InvalidInputException("Units must be a valid number.");
                }

                LocalDate donationDate;
                try {
                    donationDate = LocalDate.parse(txtDate.getText().trim());
                } catch (DateTimeParseException ex) {
                    throw new InvalidInputException("Donation date must be in YYYY-MM-DD format.");
                }

                int newId = donationService.recordDonation(donorId, hospitalId, reqId, units, donationDate);

                JOptionPane.showMessageDialog(dialog, "Donation recorded successfully!\nDonation ID: " + newId
                        + "\n(Database Trigger automatically updated donor's last donation date and inventory stock).", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadDonations();

            } catch (InvalidInputException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
            } catch (NotEligibleException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Donor Ineligible", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBar.add(btnCancel);
        btnBar.add(btnSave);
        dialog.add(btnBar, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void handleDeleteDonation() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a donation record to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int donId = (int) tableModel.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete Donation #" + donId + "?\n"
                        + "This will trigger the database to deduct units from blood stock and recalculate donor date.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean ok = donationDAO.delete(donId);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Donation #" + donId + " deleted successfully.\nStock and donor date were adjusted by database triggers.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadDonations();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete donation.", "Error", JOptionPane.ERROR_MESSAGE);
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

    private static class HospitalItem {
        final Hospital hospital;
        HospitalItem(Hospital h) { this.hospital = h; }
        @Override
        public String toString() {
            return hospital.getName() + " (" + hospital.getCity() + ")";
        }
    }
}
