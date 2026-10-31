package com.bloodnetwork.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled background tasks for expiring stale notifications
 * and logging audit monitoring telemetry.
 */
@Service
public class ScheduledTasksService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasksService.class);
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ScheduledTasksService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Runs every hour to mark unanswered notifications older than 72 hours as EXPIRED.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void expireStaleNotifications() {
        try {
            int expired = jdbcTemplate.update(
                    "UPDATE donor_notification SET status = 'EXPIRED' "
                    + "WHERE status = 'SENT' AND created_at < DATE_SUB(NOW(), INTERVAL 72 HOUR)"
            );
            if (expired > 0) {
                log.info("Scheduled task: marked {} stale donor notifications as EXPIRED.", expired);
            }
        } catch (Exception e) {
            log.warn("Scheduled notification expiration check error: {}", e.getMessage());
        }
    }
}
