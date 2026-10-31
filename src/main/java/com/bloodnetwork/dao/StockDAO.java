package com.bloodnetwork.dao;

import com.bloodnetwork.model.BloodStock;
import com.bloodnetwork.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO) for the 'blood_stock' table.
 * Read-only operations for tracking inventory levels across hospitals.
 */
public class StockDAO {

    /**
     * Retrieves current inventory levels for all blood groups at a specific hospital.
     *
     * @param hospitalId The hospital ID to inspect.
     * @return List of BloodStock records for that hospital.
     * @throws SQLException if a database error occurs.
     */
    public List<BloodStock> getByHospital(int hospitalId) throws SQLException {
        List<BloodStock> stocks = new ArrayList<>();
        String sql = "SELECT hospital_id, blood_group, units_available FROM blood_stock "
                   + "WHERE hospital_id = ? ORDER BY blood_group ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, hospitalId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int hId = rs.getInt("hospital_id");
                    String group = rs.getString("blood_group");
                    int units = rs.getInt("units_available");
                    stocks.add(new BloodStock(hId, group, units));
                }
            }
        }
        return stocks;
    }

    /**
     * Calculates the total units available for each blood group aggregated across all hospitals.
     *
     * @return Map with blood group as key and total units available as value (ordered by blood group).
     * @throws SQLException if a database error occurs.
     */
    public Map<String, Integer> getTotalByBloodGroup() throws SQLException {
        Map<String, Integer> totals = new LinkedHashMap<>();
        String sql = "SELECT blood_group, SUM(units_available) AS total_units FROM blood_stock "
                   + "GROUP BY blood_group ORDER BY blood_group ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String group = rs.getString("blood_group");
                int totalUnits = rs.getInt("total_units");
                totals.put(group, totalUnits);
            }
        }
        return totals;
    }

    /**
     * Deducts blood units from the hospital inventory as part of a transaction.
     * Uses atomic condition 'units_available >= ?' to prevent negative stock.
     *
     * @param conn Active transactional connection.
     * @param hospitalId The hospital holding the stock.
     * @param bloodGroup Blood group to deduct.
     * @param units Number of units to deduct.
     * @return Number of rows updated (1 if deduction succeeded, 0 if insufficient stock).
     * @throws SQLException if a database error occurs.
     */
    public int deduct(Connection conn, int hospitalId, String bloodGroup, int units) throws SQLException {
        String sql = "UPDATE blood_stock SET units_available = units_available - ? "
                   + "WHERE hospital_id = ? AND blood_group = ? AND units_available >= ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, units);
            pstmt.setInt(2, hospitalId);
            pstmt.setString(3, bloodGroup);
            pstmt.setInt(4, units);

            return pstmt.executeUpdate();
        }
    }
}
