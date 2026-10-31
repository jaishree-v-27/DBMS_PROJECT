-- ==============================================================================
-- Migration 003: Ethical Donor Recognition & Milestones
-- Description: Achievement definitions and earned milestones from verified donations
-- Idempotent: Safe to execute repeatedly
-- ==============================================================================

CREATE TABLE IF NOT EXISTS achievement_definition (
    code VARCHAR(50) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    threshold INT NOT NULL,
    active BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS donor_achievement (
    achievement_id INT AUTO_INCREMENT PRIMARY KEY,
    donor_id INT NOT NULL,
    code VARCHAR(50) NOT NULL,
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_donor_code (donor_id, code),
    FOREIGN KEY (donor_id) REFERENCES donor(donor_id) ON DELETE CASCADE,
    FOREIGN KEY (code) REFERENCES achievement_definition(code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO achievement_definition (code, title, description, threshold, active) VALUES
('FIRST_DROP', 'First Drop', 'First verified whole blood donation.', 1, TRUE),
('LIFE_SAVER', 'Life Saver', 'Five verified whole blood donations.', 5, TRUE),
('COMMUNITY_HERO', 'Community Hero', 'Ten verified whole blood donations.', 10, TRUE),
('DEDICATED_DONOR', 'Dedicated Donor', 'Twenty five verified whole blood donations.', 25, TRUE)
ON DUPLICATE KEY UPDATE title = VALUES(title), description = VALUES(description), threshold = VALUES(threshold);
