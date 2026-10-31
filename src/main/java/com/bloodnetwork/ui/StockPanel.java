package com.bloodnetwork.ui;

import com.bloodnetwork.dao.ReportDAO;
import com.bloodnetwork.dao.StockDAO;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Inventory monitoring panel displaying stock per hospital and aggregate totals.
 * Automatically highlights critical low stock (under 3 units) in light red.
 */
public class StockPanel extends JPanel {

    private final StockDAO stockDAO = new StockDAO();
    private final ReportDAO reportDAO = new ReportDAO();

    private JTable hospitalStockTable;
    private DefaultTableModel hospitalStockModel;

    private JTable totalStockTable;
    private DefaultTableModel totalStockModel;

    public StockPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(UITheme.LIGHT_BG);

        // Top Toolbar
        JPanel topBar = new JPanel(new BorderLayout(10, 10));
        topBar.setBackground(UITheme.LIGHT_BG);

        JLabel lblTitle = new JLabel("Blood Inventory & Stock Management");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_DARK);
        topBar.add(lblTitle, BorderLayout.WEST);

        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightBar.setBackground(UITheme.LIGHT_BG);

        JLabel lblLegend = new JLabel("■ Low Stock Alert (< 3 units)");
        lblLegend.setFont(UITheme.FONT_BODY_BOLD);
        lblLegend.setForeground(new java.awt.Color(198, 40, 40));
        rightBar.add(lblLegend);

        JButton btnRefresh = UITheme.createPrimaryButton("Refresh Stock");
        btnRefresh.addActionListener(e -> loadStockData());
        rightBar.add(btnRefresh);

        topBar.add(rightBar, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Table 1: Detailed stock per hospital
        String[] hospCols = {"Hospital Name", "City", "Blood Group", "Units Available"};
        hospitalStockModel = new DefaultTableModel(hospCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        hospitalStockTable = new JTable(hospitalStockModel);
        hospitalStockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        hospitalStockTable.setRowHeight(24);
        hospitalStockTable.setFont(UITheme.FONT_BODY);
        hospitalStockTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);

        // Custom Cell Renderer to highlight rows with units < 3 in light red
        LowStockRenderer hospRenderer = new LowStockRenderer(3);
        for (int i = 0; i < hospCols.length; i++) {
            hospitalStockTable.getColumnModel().getColumn(i).setCellRenderer(hospRenderer);
        }

        JPanel pnlHosp = new JPanel(new BorderLayout());
        pnlHosp.setBorder(BorderFactory.createTitledBorder("Inventory Per Hospital"));
        pnlHosp.add(new JScrollPane(hospitalStockTable), BorderLayout.CENTER);

        // Table 2: Aggregate totals per blood group
        String[] totalCols = {"Blood Group", "Total Available Across All Hospitals"};
        totalStockModel = new DefaultTableModel(totalCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        totalStockTable = new JTable(totalStockModel);
        totalStockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        totalStockTable.setRowHeight(24);
        totalStockTable.setFont(UITheme.FONT_BODY);
        totalStockTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);

        LowStockRenderer totalRenderer = new LowStockRenderer(1);
        for (int i = 0; i < totalCols.length; i++) {
            totalStockTable.getColumnModel().getColumn(i).setCellRenderer(totalRenderer);
        }

        JPanel pnlTotal = new JPanel(new BorderLayout());
        pnlTotal.setBorder(BorderFactory.createTitledBorder("Total Aggregate Stock"));
        pnlTotal.add(new JScrollPane(totalStockTable), BorderLayout.CENTER);

        // Split Pane (65% hospital stock, 35% totals)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlHosp, pnlTotal);
        splitPane.setResizeWeight(0.65);
        add(splitPane, BorderLayout.CENTER);

        loadStockData();
    }

    private void loadStockData() {
        hospitalStockModel.setRowCount(0);
        totalStockModel.setRowCount(0);

        try {
            // 1. Load detailed stock per hospital
            List<Map<String, Object>> hospRows = reportDAO.getStockPerHospitalAndGroup();
            for (Map<String, Object> r : hospRows) {
                hospitalStockModel.addRow(new Object[]{
                        r.get("hospital_name"),
                        r.get("city"),
                        r.get("blood_group"),
                        r.get("units_available")
                });
            }

            // 2. Load total stock per blood group
            Map<String, Integer> totals = stockDAO.getTotalByBloodGroup();
            for (Map.Entry<String, Integer> entry : totals.entrySet()) {
                totalStockModel.addRow(new Object[]{
                        entry.getKey(),
                        entry.getValue()
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load stock data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Custom TableCellRenderer that colors rows with units < 3 in light red.
     */
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
                    c.setForeground(new java.awt.Color(183, 28, 28));
                } else {
                    c.setBackground(UITheme.WHITE);
                    c.setForeground(UITheme.TEXT_DARK);
                }
            }

            return c;
        }
    }
}
