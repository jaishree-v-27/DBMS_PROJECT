package com.bloodnetwork.dao;

import com.bloodnetwork.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO) for analytical reporting queries across hospitals,
 * blood stock, donor contributions, and request fulfillment.
 * Uses SQL JOINs, GROUP BY aggregations, and PreparedStatement.
 */
public class ReportDAO {

    /**
     * a) Generates blood stock inventory detailed per hospital and blood group.
     *
     * @return List of rows containing hospital name, city, blood group, and available units.
     * @throws SQLException if a database error occurs.
     */
    public List<Map<String, Object>> getStockPerHospitalAndGroup() throws SQLException {
        String sql = "SELECT h.name AS hospital_name, h.city, s.blood_group, s.units_available "
                   + "FROM blood_stock s "
                   + "JOIN hospital h ON s.hospital_id = h.hospital_id "
                   + "ORDER BY h.name ASC, s.blood_group ASC";

        return executeQuery(sql);
    }

    /**
     * b) Generates aggregate total blood stock across all hospitals grouped by blood group.
     *
     * @return List of rows containing blood group and total available units.
     * @throws SQLException if a database error occurs.
     */
    public List<Map<String, Object>> getTotalStockPerBloodGroup() throws SQLException {
        String sql = "SELECT blood_group, SUM(units_available) AS total_units "
                   + "FROM blood_stock "
                   + "GROUP BY blood_group "
                   + "ORDER BY blood_group ASC";

        return executeQuery(sql);
    }

    /**
     * c) Generates summary of blood requests grouped by their processing status.
     *
     * @return List of rows containing status, request count, and total units requested.
     * @throws SQLException if a database error occurs.
     */
    public List<Map<String, Object>> getRequestCountByStatus() throws SQLException {
        String sql = "SELECT status, COUNT(*) AS request_count, SUM(units_needed) AS total_units_needed "
                   + "FROM blood_request "
                   + "GROUP BY status "
                   + "ORDER BY FIELD(status, 'PENDING', 'MATCHED', 'FULFILLED', 'CANCELLED')";

        return executeQuery(sql);
    }

    /**
     * d) Generates monthly donation trends for the current calendar year.
     *
     * @return List of rows containing month name, number of donations, and total units donated.
     * @throws SQLException if a database error occurs.
     */
    public List<Map<String, Object>> getDonationsPerMonthCurrentYear() throws SQLException {
        String sql = "SELECT MONTH(donation_date) AS month_num, "
                   + "       MONTHNAME(donation_date) AS month_name, "
                   + "       COUNT(*) AS donation_count, "
                   + "       SUM(units) AS total_units "
                   + "FROM donation "
                   + "WHERE YEAR(donation_date) = YEAR(CURDATE()) "
                   + "GROUP BY MONTH(donation_date), MONTHNAME(donation_date) "
                   + "ORDER BY month_num ASC";

        return executeQuery(sql);
    }

    /**
     * e) Retrieves top 5 most generous donors ranked by total units donated and donation count.
     *
     * @return List of top 5 donor contribution summaries.
     * @throws SQLException if a database error occurs.
     */
    public List<Map<String, Object>> getTop5Donors() throws SQLException {
        String sql = "SELECT d.donor_id, d.name, d.blood_group, d.city, "
                   + "       COUNT(dn.donation_id) AS total_donations, "
                   + "       SUM(dn.units) AS total_units_donated "
                   + "FROM donor d "
                   + "JOIN donation dn ON d.donor_id = dn.donor_id "
                   + "GROUP BY d.donor_id, d.name, d.blood_group, d.city "
                   + "ORDER BY total_units_donated DESC, total_donations DESC "
                   + "LIMIT 5";

        return executeQuery(sql);
    }

    /**
     * f) Retrieves the chronological donation history of a single donor including hospital names.
     *
     * @param donorId The ID of the donor.
     * @return List of donation records with hospital details.
     * @throws SQLException if a database error occurs.
     */
    public List<Map<String, Object>> getDonationHistoryByDonor(int donorId) throws SQLException {
        String sql = "SELECT dn.donation_id, dn.donation_date, h.name AS hospital_name, "
                   + "       h.city AS hospital_city, dn.units, dn.request_id "
                   + "FROM donation dn "
                   + "JOIN hospital h ON dn.hospital_id = h.hospital_id "
                   + "WHERE dn.donor_id = ? "
                   + "ORDER BY dn.donation_date DESC, dn.donation_id DESC";

        return executeQuery(sql, donorId);
    }

    /**
     * Generic helper to execute a parameterized report query and return results
     * as a List of Maps preserving column ordering.
     */
    private List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        String colLabel = meta.getColumnLabel(i);
                        row.put(colLabel, rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        }
        return results;
    }
}
