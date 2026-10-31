package com.bloodnetwork.ui;

import com.bloodnetwork.dao.ReportDAO;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Analytical reporting panel allowing administrators to select and execute
 * any of the 6 core business reports, with results formatted in an interactive table.
 */
public class ReportsPanel extends JPanel {

    private final ReportDAO reportDAO = new ReportDAO();

    private JComboBox<String> cmbReports;
    private JTextField txtDonorId;
    private JLabel lblDonorId;
    private JTable reportTable;
    private DefaultTableModel tableModel;
    private JLabel lblStatus;

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(UITheme.LIGHT_BG);

        // Top Toolbar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.setBackground(UITheme.LIGHT_BG);

        JLabel lblSelect = new JLabel("Select Report:");
        lblSelect.setFont(UITheme.FONT_BODY_BOLD);
        topBar.add(lblSelect);

        String[] reportOptions = {
                "1. Stock Per Hospital and Blood Group",
                "2. Total Aggregate Stock Per Blood Group",
                "3. Number of Requests Per Status",
                "4. Monthly Donations Trend (Current Year)",
                "5. Top 5 Donors by Total Contributions",
                "6. Donation History of a Donor"
        };

        cmbReports = new JComboBox<>(reportOptions);
        cmbReports.setFont(UITheme.FONT_BODY);
        topBar.add(cmbReports);

        // Optional Donor ID input for report 6
        lblDonorId = new JLabel("Donor ID:");
        lblDonorId.setFont(UITheme.FONT_BODY_BOLD);
        lblDonorId.setVisible(false);
        topBar.add(lblDonorId);

        txtDonorId = new JTextField(6);
        UITheme.styleTextField(txtDonorId);
        txtDonorId.setVisible(false);
        topBar.add(txtDonorId);

        cmbReports.addActionListener(e -> {
            boolean isDonorReport = cmbReports.getSelectedIndex() == 5;
            lblDonorId.setVisible(isDonorReport);
            txtDonorId.setVisible(isDonorReport);
            topBar.revalidate();
            topBar.repaint();
        });

        JButton btnRun = UITheme.createPrimaryButton("Run Report");
        btnRun.addActionListener(e -> runSelectedReport());
        topBar.add(btnRun);

        add(topBar, BorderLayout.NORTH);

        // Center Table
        tableModel = new DefaultTableModel();
        reportTable = new JTable(tableModel);
        reportTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(reportTable);

        add(new JScrollPane(reportTable), BorderLayout.CENTER);

        // Bottom Status Bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBackground(UITheme.LIGHT_BG);
        lblStatus = new JLabel("Select a report and click 'Run Report' to view analysis.");
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        statusBar.add(lblStatus);
        add(statusBar, BorderLayout.SOUTH);

        // Run default report on initial view
        runSelectedReport();
    }

    private void runSelectedReport() {
        int index = cmbReports.getSelectedIndex();
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        try {
            List<Map<String, Object>> rows = null;

            switch (index) {
                case 0:
                    rows = reportDAO.getStockPerHospitalAndGroup();
                    break;
                case 1:
                    rows = reportDAO.getTotalStockPerBloodGroup();
                    break;
                case 2:
                    rows = reportDAO.getRequestCountByStatus();
                    break;
                case 3:
                    rows = reportDAO.getDonationsPerMonthCurrentYear();
                    break;
                case 4:
                    rows = reportDAO.getTop5Donors();
                    break;
                case 5:
                    String idStr = txtDonorId.getText().trim();
                    if (idStr.isEmpty()) {
                        idStr = JOptionPane.showInputDialog(this, "Enter Donor ID to view donation history:", "Donor History", JOptionPane.QUESTION_MESSAGE);
                        if (idStr == null || idStr.trim().isEmpty()) {
                            return;
                        }
                        txtDonorId.setText(idStr.trim());
                    }
                    try {
                        int donorId = Integer.parseInt(idStr.trim());
                        rows = reportDAO.getDonationHistoryByDonor(donorId);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Donor ID must be a valid integer number.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    break;
            }

            if (rows == null || rows.isEmpty()) {
                tableModel.setColumnCount(0);
                tableModel.addColumn("Notice");
                tableModel.addRow(new Object[]{"No data records available for this query."});
                lblStatus.setText("Query executed successfully. Results: 0 rows.");
                return;
            }

            // Populate table columns dynamically based on Map keys
            Map<String, Object> firstRow = rows.get(0);
            for (String key : firstRow.keySet()) {
                tableModel.addColumn(key.replace('_', ' ').toUpperCase());
            }

            // Populate table rows
            for (Map<String, Object> row : rows) {
                Object[] rowData = new Object[firstRow.size()];
                int i = 0;
                for (String key : firstRow.keySet()) {
                    Object val = row.get(key);
                    rowData[i++] = (val != null) ? val.toString() : "-";
                }
                tableModel.addRow(rowData);
            }

            lblStatus.setText("Query completed successfully. Total records found: " + rows.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error generating report: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            lblStatus.setText("Error executing query.");
        }
    }
}
