-- ============================================================================
-- Blood Donor Network - Demo Accounts Configuration (Phase 5A)
-- Database: blood_network
-- Target RDBMS: MySQL 8.0+
-- Description: Updates existing sample users with known SHA-256 demo passwords
--              so users can log in via the Swing graphical interface.
-- Demo Passwords:
--   - Admin ('admin')                       -> 'admin123'
--   - Requesters ('apollo_chennai', etc.)   -> 'hospital123'
--   - Donors ('rahul_sharma', etc.)         -> 'donor123'
-- Safe to re-run anytime.
-- ============================================================================

USE blood_network;

-- 1. Update Admin account
UPDATE app_user 
SET password_hash = SHA2('admin123', 256)
WHERE username = 'admin';

-- 2. Update Hospital Requester accounts
UPDATE app_user 
SET password_hash = SHA2('hospital123', 256)
WHERE username IN ('apollo_chennai', 'kmch_cbe', 'meenakshi_mdu');

-- 3. Update Donor accounts
UPDATE app_user 
SET password_hash = SHA2('donor123', 256)
WHERE username IN ('rahul_sharma', 'priya_ramesh', 'anand_kumar', 'deepa_nair', 'vijay_seth','karthik_raja');
