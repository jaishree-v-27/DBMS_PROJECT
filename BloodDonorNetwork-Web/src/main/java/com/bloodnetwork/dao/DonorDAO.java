package com.bloodnetwork.dao;

import com.bloodnetwork.model.Donor;
import com.bloodnetwork.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for the 'donor' table.
 * Implements CRUD operations and custom search queries using PreparedStatement
 * and try-with-resources for safe connection management.
 */
public class DonorDAO {

    /**
     * Inserts a new donor into the database and returns the auto-generated donor_id.
     *
     * @param donor The donor object to be inserted.
     * @return The auto-generated donor_id.
     * @throws SQLException if a database access error occurs.
     */
    public int add(Donor donor) throws SQLException {
        String sql = "INSERT INTO donor (user_id, name, age, gender, blood_group, phone, city, last_donation_date, available) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // user_id can be null
            if (donor.getUserId() != null) {
                pstmt.setInt(1, donor.getUserId());
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }

            pstmt.setString(2, donor.getName());
            pstmt.setInt(3, donor.getAge());
            pstmt.setString(4, donor.getGender());
            pstmt.setString(5, donor.getBloodGroup());
            pstmt.setString(6, donor.getPhone());
            pstmt.setString(7, donor.getCity());

            // last_donation_date can be null
            if (donor.getLastDonationDate() != null) {
                pstmt.setDate(8, Date.valueOf(donor.getLastDonationDate()));
            } else {
                pstmt.setNull(8, Types.DATE);
            }

            pstmt.setBoolean(9, donor.isAvailable());

            pstmt.executeUpdate();

            // Retrieve the generated primary key (donor_id)
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    donor.setDonorId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating donor failed; no ID was generated.");
                }
            }
        }
    }

    /**
     * Retrieves a single donor by their primary key (donor_id).
     *
     * @param donorId The donor ID to look up.
     * @return The Donor object if found, or null if no record exists.
     * @throws SQLException if a database access error occurs.
     */
    public Donor getById(int donorId) throws SQLException {
        String sql = "SELECT donor_id, user_id, name, age, gender, blood_group, phone, city, last_donation_date, available "
                   + "FROM donor WHERE donor_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, donorId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToDonor(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all donor records from the database.
     *
     * @return List of all donors, ordered by donor_id.
     * @throws SQLException if a database access error occurs.
     */
    public List<Donor> getAll() throws SQLException {
        List<Donor> donors = new ArrayList<>();
        String sql = "SELECT donor_id, user_id, name, age, gender, blood_group, phone, city, last_donation_date, available "
                   + "FROM donor ORDER BY donor_id ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                donors.add(mapRowToDonor(rs));
            }
        }
        return donors;
    }

    /**
     * Updates an existing donor's details in the database.
     *
     * @param donor The donor object with updated information.
     * @return true if the record was updated; false if the donor was not found.
     * @throws SQLException if a database access error occurs.
     */
    public boolean update(Donor donor) throws SQLException {
        String sql = "UPDATE donor SET name = ?, age = ?, gender = ?, blood_group = ?, phone = ?, "
                   + "city = ?, last_donation_date = ?, available = ? WHERE donor_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, donor.getName());
            pstmt.setInt(2, donor.getAge());
            pstmt.setString(3, donor.getGender());
            pstmt.setString(4, donor.getBloodGroup());
            pstmt.setString(5, donor.getPhone());
            pstmt.setString(6, donor.getCity());

            if (donor.getLastDonationDate() != null) {
                pstmt.setDate(7, Date.valueOf(donor.getLastDonationDate()));
            } else {
                pstmt.setNull(7, Types.DATE);
            }

            pstmt.setBoolean(8, donor.isAvailable());
            pstmt.setInt(9, donor.getDonorId());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Deletes a donor record by donor_id.
     *
     * @param donorId ID of the donor to remove.
     * @return true if a record was deleted; false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean delete(int donorId) throws SQLException {
        String sql = "DELETE FROM donor WHERE donor_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, donorId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Finds donors located in a specific city (case-insensitive search).
     *
     * @param city The city name to filter by.
     * @return List of matching donors.
     * @throws SQLException if a database access error occurs.
     */
    public List<Donor> findByCity(String city) throws SQLException {
        List<Donor> donors = new ArrayList<>();
        String sql = "SELECT donor_id, user_id, name, age, gender, blood_group, phone, city, last_donation_date, available "
                   + "FROM donor WHERE LOWER(TRIM(city)) = LOWER(TRIM(?)) ORDER BY name ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, city);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    donors.add(mapRowToDonor(rs));
                }
            }
        }
        return donors;
    }

    /**
     * Finds donors with a specific blood group.
     *
     * @param group The blood group to search for (e.g., 'A+', 'O-').
     * @return List of matching donors.
     * @throws SQLException if a database access error occurs.
     */
    public List<Donor> findByBloodGroup(String group) throws SQLException {
        List<Donor> donors = new ArrayList<>();
        String sql = "SELECT donor_id, user_id, name, age, gender, blood_group, phone, city, last_donation_date, available "
                   + "FROM donor WHERE blood_group = ? ORDER BY name ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, group != null ? group.trim().toUpperCase() : "");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    donors.add(mapRowToDonor(rs));
                }
            }
        }
        return donors;
    }

    /**
     * Finds donors located in a specific city matching any of the specified blood groups.
     * Builds a PreparedStatement dynamically with the exact number of '?' placeholders needed.
     *
     * @param city The city name.
     * @param groups List of blood groups to match (e.g. ["A+", "O-"]).
     * @return List of matching donors.
     * @throws SQLException if a database access error occurs.
     */
    public List<Donor> findByCityAndGroups(String city, List<String> groups) throws SQLException {
        List<Donor> donors = new ArrayList<>();
        if (groups == null || groups.isEmpty()) {
            return donors;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT donor_id, user_id, name, age, gender, blood_group, phone, city, last_donation_date, available "
                + "FROM donor WHERE LOWER(TRIM(city)) = LOWER(TRIM(?)) AND blood_group IN ("
        );

        for (int i = 0; i < groups.size(); i++) {
            sql.append("?");
            if (i < groups.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(") ORDER BY name ASC");

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            pstmt.setString(1, city);
            for (int i = 0; i < groups.size(); i++) {
                pstmt.setString(i + 2, groups.get(i).trim().toUpperCase());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    donors.add(mapRowToDonor(rs));
                }
            }
        }
        return donors;
    }

    /**
     * Helper method to map a single row of a ResultSet to a Donor object.
     * Accurately handles nullable fields like last_donation_date and user_id.
     */
    private Donor mapRowToDonor(ResultSet rs) throws SQLException {
        int donorId = rs.getInt("donor_id");

        int userIdVal = rs.getInt("user_id");
        Integer userId = rs.wasNull() ? null : userIdVal;

        String name = rs.getString("name");
        int age = rs.getInt("age");
        String gender = rs.getString("gender");
        String bloodGroup = rs.getString("blood_group");
        String phone = rs.getString("phone");
        String city = rs.getString("city");

        Date sqlDate = rs.getDate("last_donation_date");
        LocalDate lastDonationDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;

        boolean available = rs.getBoolean("available");

        return new Donor(donorId, userId, name, age, gender, bloodGroup, phone, city, lastDonationDate, available);
    }
}
