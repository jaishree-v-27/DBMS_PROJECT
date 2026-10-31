package com.bloodnetwork.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service managing ethical donor recognition, milestone tracking,
 * and verified achievement awards based strictly on audited donation records.
 */
@Service
public class DonorAchievementService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DonorAchievementService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record MilestoneBadge(
            String code,
            String title,
            String description,
            int threshold,
            boolean isEarned,
            LocalDateTime earnedAt,
            int currentProgress,
            int progressPercent
    ) {}

    /**
     * Evaluates verified donations and awards any unearned milestones within the active transaction.
     * Guaranteed idempotent via UNIQUE KEY on (donor_id, code).
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> evaluateAndAwardAchievements(int donorId) {
        // Count verified donations from database
        Integer verifiedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM donation WHERE donor_id = ?", Integer.class, donorId
        );
        int totalDonations = verifiedCount != null ? verifiedCount : 0;

        // Query active definitions
        String defSql = "SELECT code, threshold FROM achievement_definition WHERE active = TRUE ORDER BY threshold ASC";
        List<Map<String, Object>> defs = jdbcTemplate.queryForList(defSql);

        List<String> newlyAwarded = new ArrayList<>();
        for (Map<String, Object> def : defs) {
            String code = (String) def.get("code");
            int threshold = ((Number) def.get("threshold")).intValue();

            if (totalDonations >= threshold) {
                // Insert ignore to ensure milestone is awarded only once
                int inserted = jdbcTemplate.update(
                        "INSERT IGNORE INTO donor_achievement (donor_id, code, earned_at) VALUES (?, ?, NOW())",
                        donorId, code
                );
                if (inserted > 0) {
                    newlyAwarded.add(code);
                }
            }
        }

        return newlyAwarded;
    }

    /**
     * Retrieves all milestone badges for a donor, showing earned status,
     * earned dates, and real progress percentage.
     */
    public List<MilestoneBadge> getBadgesForDonor(int donorId) {
        Integer verifiedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM donation WHERE donor_id = ?", Integer.class, donorId
        );
        int totalDonations = verifiedCount != null ? verifiedCount : 0;

        String sql = "SELECT d.code, d.title, d.description, d.threshold, a.earned_at "
                   + "FROM achievement_definition d "
                   + "LEFT JOIN donor_achievement a ON d.code = a.code AND a.donor_id = ? "
                   + "WHERE d.active = TRUE "
                   + "ORDER BY d.threshold ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String code = rs.getString("code");
            String title = rs.getString("title");
            String desc = rs.getString("description");
            int threshold = rs.getInt("threshold");
            Timestamp earnedTs = rs.getTimestamp("earned_at");
            boolean isEarned = earnedTs != null;
            LocalDateTime earnedAt = isEarned ? earnedTs.toLocalDateTime() : null;

            int progressPercent = (int) Math.min(100, Math.round(((double) totalDonations / threshold) * 100));

            return new MilestoneBadge(code, title, desc, threshold, isEarned, earnedAt, totalDonations, progressPercent);
        }, donorId);
    }
}
