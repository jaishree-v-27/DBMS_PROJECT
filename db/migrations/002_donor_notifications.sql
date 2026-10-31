-- ==============================================================================
-- Migration 002: In-App Donor Notifications
-- Description: In-app matching requests dispatched to eligible voluntary donors
-- Idempotent: Safe to execute repeatedly
-- ==============================================================================

CREATE TABLE IF NOT EXISTS donor_notification (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    donor_id INT NOT NULL,
    request_id INT NOT NULL,
    status ENUM('SENT', 'ACCEPTED', 'DECLINED', 'EXPIRED') DEFAULT 'SENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    INDEX idx_dn_donor (donor_id),
    INDEX idx_dn_request (request_id),
    UNIQUE KEY uq_donor_request (donor_id, request_id),
    FOREIGN KEY (donor_id) REFERENCES donor(donor_id) ON DELETE CASCADE,
    FOREIGN KEY (request_id) REFERENCES blood_request(request_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
