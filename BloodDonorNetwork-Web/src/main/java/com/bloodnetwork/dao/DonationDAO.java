package com.bloodnetwork.dao;

import com.bloodnetwork.model.Donation;
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
 * Data Access Object (DAO) for the 'donation' table.
 * 
 * Note: Insertion and deletion triggers (after_donation_insert, after_donation_delete)
 * automatically maintain donor.last_donation_date and blood_stock in the database.
 * No update method is provided: to correct a donation, delete it and re-add.
 */
public class DonationDAO {

    /**
     * Inserts a new donation record into the database and returns the generated donation_id.
     * The database trigger 'after_donation_insert' will automatically update the donor's
     * last donation date and increment the hospital's blood stock.
     *
     * @param donation The donation to insert.
     * @return Generated primary key (donation_id).
     * @throws SQLException if a database error occurs.
     */
    public int add(Donation donation) throws SQLException {
        String sql = "INSERT INTO donation (donor_id, hospital_id, request_id, units, donation_date) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, donation.getDonorId());
            pstmt.setInt(2, donation.getHospitalId());

            if (donation.getRequestId() != null) {
                pstmt.setInt(3, donation.getRequestId());
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }

            pstmt.setInt(4, donation.getUnits());
            pstmt.setDate(5, Date.valueOf(donation.getDonationDate()));

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    donation.setDonationId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating donation failed; no ID was generated.");
                }
            }
        }
    }

    /**
     * Retrieves a single donation by its primary key.
     *
     * @param donationId The ID of the donation to look up.
     * @return Donation entity if found, or null otherwise.
     * @throws SQLException if a database error occurs.
     */
    public Donation getById(int donationId) throws SQLException {
        String sql = "SELECT donation_id, donor_id, hospital_id, request_id, units, donation_date "
                   + "FROM donation WHERE donation_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, donationId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToDonation(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all recorded donations ordered by donation_date DESC, donation_id DESC.
     *
     * @return List of all donations.
     * @throws SQLException if a database error occurs.
     */
    public List<Donation> getAll() throws SQLException {
        List<Donation> donations = new ArrayList<>();
        String sql = "SELECT donation_id, donor_id, hospital_id, request_id, units, donation_date "
                   + "FROM donation ORDER BY donation_date DESC, donation_id DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                donations.add(mapRowToDonation(rs));
            }
        }
        return donations;
    }

    /**
     * Retrieves all donations made by a specific donor.
     *
     * @param donorId The donor's ID.
     * @return List of donations by this donor.
     * @throws SQLException if a database error occurs.
     */
    public List<Donation> getByDonor(int donorId) throws SQLException {
        List<Donation> donations = new ArrayList<>();
        String sql = "SELECT donation_id, donor_id, hospital_id, request_id, units, donation_date "
                   + "FROM donation WHERE donor_id = ? ORDER BY donation_date DESC, donation_id DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, donorId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    donations.add(mapRowToDonation(rs));
                }
            }
        }
        return donations;
    }

    /**
     * Retrieves all donations conducted at a specific hospital.
     *
     * @param hospitalId The hospital's ID.
     * @return List of donations at this hospital.
     * @throws SQLException if a database error occurs.
     */
    public List<Donation> getByHospital(int hospitalId) throws SQLException {
        List<Donation> donations = new ArrayList<>();
        String sql = "SELECT donation_id, donor_id, hospital_id, request_id, units, donation_date "
                   + "FROM donation WHERE hospital_id = ? ORDER BY donation_date DESC, donation_id DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, hospitalId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    donations.add(mapRowToDonation(rs));
                }
            }
        }
        return donations;
    }

    /**
     * Deletes a donation record by its primary key.
     * The database trigger 'after_donation_delete' will automatically deduct the
     * units from blood_stock and recalculate the donor's last_donation_date.
     *
     * @param donationId The ID of the donation to delete.
     * @return true if deleted; false if donation_id not found.
     * @throws SQLException if a database error occurs.
     */
    public boolean delete(int donationId) throws SQLException {
        String sql = "DELETE FROM donation WHERE donation_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, donationId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Maps a ResultSet row to a Donation entity.
     */
    private Donation mapRowToDonation(ResultSet rs) throws SQLException {
        int donationId = rs.getInt("donation_id");
        int donorId = rs.getInt("donor_id");
        int hospitalId = rs.getInt("hospital_id");

        int reqIdVal = rs.getInt("request_id");
        Integer requestId = rs.wasNull() ? null : reqIdVal;

        int units = rs.getInt("units");
        Date dDate = rs.getDate("donation_date");
        LocalDate donationDate = (dDate != null) ? dDate.toLocalDate() : null;

        return new Donation(donationId, donorId, hospitalId, requestId, units, donationDate);
    }
}
