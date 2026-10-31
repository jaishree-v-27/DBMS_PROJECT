-- ==============================================================================
-- Migration 001: Request Event Audit Trail
-- Description: Records lifecycle status transitions and administrative actions
-- Idempotent: Safe to execute repeatedly
-- ==============================================================================

CREATE TABLE IF NOT EXISTS request_event (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    request_id INT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    actor_user_id INT,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_re_request (request_id),
    INDEX idx_re_created (created_at),
    FOREIGN KEY (request_id) REFERENCES blood_request(request_id) ON DELETE CASCADE,
    FOREIGN KEY (actor_user_id) REFERENCES app_user(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
