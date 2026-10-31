package com.bloodnetwork.ui;

import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.util.Validator;

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
import javax.swing.JCheckBox;
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
 * Management panel for Donors: viewing, searching, adding, editing, and deleting records.
 */
public class DonorsPanel extends JPanel {

    private final DonorDAO donorDAO = new DonorDAO();

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JComboBox<String> cmbSearchType;

    public DonorsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(UITheme.LIGHT_BG);

        // Top Toolbar: Search & Action Buttons
        JPanel topBar = new JPanel(new BorderLayout(10, 10));
        topBar.setBackground(UITheme.LIGHT_BG);

        // Search Controls
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchPanel.setBackground(UITheme.LIGHT_BG);
        searchPanel.add(new JLabel("Search:"));

        txtSearch = new JTextField(14);
        UITheme.styleTextField(txtSearch);
        searchPanel.add(txtSearch);

        cmbSearchType = new JComboBox<>(new String[]{"City", "Blood Group"});
        cmbSearchType.setFont(UITheme.FONT_BODY);
        searchPanel.add(cmbSearchType);

        JButton btnSearch = UITheme.createSecondaryButton("Search");
        btnSearch.addActionListener(e -> performSearch());
        searchPanel.add(btnSearch);

        JButton btnReset = UITheme.createSecondaryButton("Show All");
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            loadDonors();
        });
        searchPanel.add(btnReset);

        topBar.add(searchPanel, BorderLayout.WEST);

        // CRUD Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setBackground(UITheme.LIGHT_BG);

        JButton btnAdd = UITheme.createPrimaryButton("+ Add Donor");
        btnAdd.addActionListener(e -> showDonorDialog(null));
        actionPanel.add(btnAdd);

        JButton btnEdit = UITheme.createSecondaryButton("Edit");
        btnEdit.addActionListener(e -> handleEdit());
        actionPanel.add(btnEdit);

        JButton btnDelete = UITheme.createDangerButton("Delete");
        btnDelete.addActionListener(e -> handleDelete());
        actionPanel.add(btnDelete);

        topBar.add(actionPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Center Table
        String[] columnNames = {"ID", "Name", "Age", "Gender", "Group", "Phone", "City", "Last Donation", "Available"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only cells
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(table);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Initial data load
        loadDonors();
    }

    private void loadDonors() {
        tableModel.setRowCount(0);
        try {
            List<Donor> donors = donorDAO.getAll();
            for (Donor d : donors) {
                tableModel.addRow(new Object[]{
                        d.getDonorId(),
                        d.getName(),
                        d.getAge(),
                        d.getGender() != null ? d.getGender() : "-",
                        d.getBloodGroup(),
                        d.getPhone(),
                        d.getCity(),
                        d.getLastDonationDate() != null ? d.getLastDonationDate().toString() : "Never",
                        d.isAvailable() ? "YES" : "NO"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load donors: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performSearch() {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) {
            loadDonors();
            return;
        }

        tableModel.setRowCount(0);
        try {
            List<Donor> results;
            if ("City".equals(cmbSearchType.getSelectedItem())) {
                results = donorDAO.findByCity(query);
            } else {
                results = donorDAO.findByBloodGroup(query);
            }

            for (Donor d : results) {
                tableModel.addRow(new Object[]{
                        d.getDonorId(),
                        d.getName(),
                        d.getAge(),
                        d.getGender() != null ? d.getGender() : "-",
                        d.getBloodGroup(),
                        d.getPhone(),
                        d.getCity(),
                        d.getLastDonationDate() != null ? d.getLastDonationDate().toString() : "Never",
                        d.isAvailable() ? "YES" : "NO"
                });
            }

            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No donors found matching: " + query, "Search Result", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Search error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleEdit() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a donor from the table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int donorId = (int) tableModel.getValueAt(selectedRow, 0);
        try {
            Donor donor = donorDAO.getById(donorId);
            if (donor != null) {
                showDonorDialog(donor);
            } else {
                JOptionPane.showMessageDialog(this, "Donor not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching donor: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDelete() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a donor to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int donorId = (int) tableModel.getValueAt(selectedRow, 0);
        String donorName = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete donor '" + donorName + "' (ID: " + donorId + ")?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean deleted = donorDAO.delete(donorId);
                if (deleted) {
                    JOptionPane.showMessageDialog(this, "Donor deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadDonors();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not delete donor.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database error deleting donor: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showDonorDialog(Donor existingDonor) {
        boolean isEdit = (existingDonor != null);
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, isEdit ? "Edit Donor" : "Add Donor", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 460);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField(isEdit ? existingDonor.getName() : "", 15);
        JTextField txtAge = new JTextField(isEdit ? String.valueOf(existingDonor.getAge()) : "", 15);
        JComboBox<String> cmbGender = new JComboBox<>(new String[]{"M", "F", "O"});
        if (isEdit && existingDonor.getGender() != null) cmbGender.setSelectedItem(existingDonor.getGender());

        JComboBox<String> cmbGroup = new JComboBox<>(new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        if (isEdit && existingDonor.getBloodGroup() != null) cmbGroup.setSelectedItem(existingDonor.getBloodGroup());

        JTextField txtPhone = new JTextField(isEdit ? existingDonor.getPhone() : "", 15);
        JTextField txtCity = new JTextField(isEdit ? existingDonor.getCity() : "", 15);
        JTextField txtLastDonation = new JTextField(isEdit && existingDonor.getLastDonationDate() != null ? existingDonor.getLastDonationDate().toString() : "", 15);
        JCheckBox chkAvailable = new JCheckBox("Available to Donate", isEdit ? existingDonor.isAvailable() : true);

        int row = 0;
        addFormField(formPanel, gbc, row++, "Full Name:", txtName);
        addFormField(formPanel, gbc, row++, "Age (18-65):", txtAge);
        addFormField(formPanel, gbc, row++, "Gender:", cmbGender);
        addFormField(formPanel, gbc, row++, "Blood Group:", cmbGroup);
        addFormField(formPanel, gbc, row++, "Phone (10 digits):", txtPhone);
        addFormField(formPanel, gbc, row++, "City:", txtCity);
        addFormField(formPanel, gbc, row++, "Last Donation (YYYY-MM-DD):", txtLastDonation);

        gbc.gridx = 1;
        gbc.gridy = row++;
        formPanel.add(chkAvailable, gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSave = UITheme.createPrimaryButton(isEdit ? "Update" : "Save");
        JButton btnCancel = UITheme.createSecondaryButton("Cancel");

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                Validator.validateNonEmpty(name, "Name");

                int age;
                try {
                    age = Integer.parseInt(txtAge.getText().trim());
                } catch (NumberFormatException ex) {
                    throw new InvalidInputException("Age must be a valid number.");
                }
                Validator.validateAge(age);

                String gender = (String) cmbGender.getSelectedItem();
                String group = (String) cmbGroup.getSelectedItem();
                Validator.validateBloodGroup(group);

                String phone = txtPhone.getText().trim();
                Validator.validatePhone(phone);

                String city = txtCity.getText().trim();
                Validator.validateNonEmpty(city, "City");

                String lastDonStr = txtLastDonation.getText().trim();
                LocalDate lastDonDate = null;
                if (!lastDonStr.isEmpty()) {
                    try {
                        lastDonDate = LocalDate.parse(lastDonStr);
                    } catch (DateTimeParseException ex) {
                        throw new InvalidInputException("Last donation date must be in YYYY-MM-DD format.");
                    }
                }

                boolean available = chkAvailable.isSelected();

                if (isEdit) {
                    existingDonor.setName(name);
                    existingDonor.setAge(age);
                    existingDonor.setGender(gender);
                    existingDonor.setBloodGroup(group);
                    existingDonor.setPhone(phone);
                    existingDonor.setCity(city);
                    existingDonor.setLastDonationDate(lastDonDate);
                    existingDonor.setAvailable(available);

                    boolean ok = donorDAO.update(existingDonor);
                    if (ok) {
                        JOptionPane.showMessageDialog(dialog, "Donor updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        loadDonors();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to update donor.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    Donor newDonor = new Donor(name, age, gender, group, phone, city, lastDonDate, available);
                    int newId = donorDAO.add(newDonor);
                    JOptionPane.showMessageDialog(dialog, "Donor added successfully with ID: " + newId, "Success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    loadDonors();
                }

            } catch (InvalidInputException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_BODY_BOLD);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        panel.add(comp, gbc);
    }
}
