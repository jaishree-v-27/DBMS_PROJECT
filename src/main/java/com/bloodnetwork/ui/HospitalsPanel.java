package com.bloodnetwork.ui;

import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.util.Validator;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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
 * Management panel for Hospitals: viewing, adding, editing, and deleting hospital records.
 */
public class HospitalsPanel extends JPanel {

    private final HospitalDAO hospitalDAO = new HospitalDAO();

    private JTable table;
    private DefaultTableModel tableModel;

    public HospitalsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(UITheme.LIGHT_BG);

        // Top Toolbar
        JPanel topBar = new JPanel(new BorderLayout(10, 10));
        topBar.setBackground(UITheme.LIGHT_BG);

        JLabel lblTitle = new JLabel("Registered Hospitals and Medical Centers");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_DARK);
        topBar.add(lblTitle, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setBackground(UITheme.LIGHT_BG);

        JButton btnAdd = UITheme.createPrimaryButton("+ Add Hospital");
        btnAdd.addActionListener(e -> showHospitalDialog(null));
        actionPanel.add(btnAdd);

        JButton btnEdit = UITheme.createSecondaryButton("Edit");
        btnEdit.addActionListener(e -> handleEdit());
        actionPanel.add(btnEdit);

        JButton btnDelete = UITheme.createDangerButton("Delete");
        btnDelete.addActionListener(e -> handleDelete());
        actionPanel.add(btnDelete);

        JButton btnRefresh = UITheme.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> loadHospitals());
        actionPanel.add(btnRefresh);

        topBar.add(actionPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"Hospital ID", "Hospital Name", "City", "Contact Phone"};
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

        loadHospitals();
    }

    private void loadHospitals() {
        tableModel.setRowCount(0);
        try {
            List<Hospital> list = hospitalDAO.getAll();
            for (Hospital h : list) {
                tableModel.addRow(new Object[]{
                        h.getHospitalId(),
                        h.getName(),
                        h.getCity(),
                        h.getPhone() != null ? h.getPhone() : "N/A"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load hospitals: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleEdit() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a hospital to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        try {
            Hospital hospital = hospitalDAO.getById(id);
            if (hospital != null) {
                showHospitalDialog(hospital);
            } else {
                JOptionPane.showMessageDialog(this, "Hospital not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDelete() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a hospital to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete '" + name + "' (ID: " + id + ")?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean ok = hospitalDAO.delete(id);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Hospital deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadHospitals();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete hospital.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showHospitalDialog(Hospital existingHospital) {
        boolean isEdit = (existingHospital != null);
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, isEdit ? "Edit Hospital" : "Add Hospital", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(380, 260);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField(isEdit ? existingHospital.getName() : "", 16);
        JTextField txtCity = new JTextField(isEdit ? existingHospital.getCity() : "", 16);
        JTextField txtPhone = new JTextField(isEdit && existingHospital.getPhone() != null ? existingHospital.getPhone() : "", 16);

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Hospital Name:"), gbc);
        gbc.gridx = 1;
        form.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("City:"), gbc);
        gbc.gridx = 1;
        form.add(txtCity, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        form.add(txtPhone, gbc);

        dialog.add(form, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSave = UITheme.createPrimaryButton(isEdit ? "Update" : "Save");
        JButton btnCancel = UITheme.createSecondaryButton("Cancel");

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                Validator.validateNonEmpty(name, "Hospital Name");

                String city = txtCity.getText().trim();
                Validator.validateNonEmpty(city, "City");

                String phone = txtPhone.getText().trim();
                if (phone.isEmpty()) {
                    phone = null;
                }

                if (isEdit) {
                    existingHospital.setName(name);
                    existingHospital.setCity(city);
                    existingHospital.setPhone(phone);
                    boolean ok = hospitalDAO.update(existingHospital);
                    if (ok) {
                        JOptionPane.showMessageDialog(dialog, "Hospital updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        loadHospitals();
                    }
                } else {
                    Hospital newHosp = new Hospital(name, city, phone);
                    int id = hospitalDAO.add(newHosp);
                    JOptionPane.showMessageDialog(dialog, "Hospital added successfully with ID: " + id, "Success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    loadHospitals();
                }
            } catch (InvalidInputException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBar.add(btnCancel);
        btnBar.add(btnSave);
        dialog.add(btnBar, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}
