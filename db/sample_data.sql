-- ============================================================================
-- Blood Donor Network - Sample Data (Phase 1)
-- Database: blood_network
-- Target RDBMS: MySQL 8.0+
-- Description: Inserts fictional starter data for testing and demonstration:
--              - 1 Admin + 3 Hospital Requesters + 5 Registered Donors
--              - 3 Hospitals across 3 cities (Chennai, Coimbatore, Madurai)
--              - 20 Donors covering all 8 blood groups
--              - 6 Blood Requests with varying urgency and statuses
--              - Initial Blood Stock for all 3 hospitals (all 8 blood groups)
--              - Donation table is intentionally left empty
-- ============================================================================

USE blood_network;

-- Disable foreign key checks temporarily to allow clean data insertion
SET FOREIGN_KEY_CHECKS = 0;

-- Clear any existing rows to prevent duplicate primary/unique key conflicts
TRUNCATE TABLE donation;
TRUNCATE TABLE blood_stock;
TRUNCATE TABLE blood_request;
TRUNCATE TABLE donor;
TRUNCATE TABLE hospital;
TRUNCATE TABLE app_user;

SET FOREIGN_KEY_CHECKS = 1;


-- ----------------------------------------------------------------------------
-- 1. Insert app_user records
-- Passwords are mock bcrypt hashes for fictional demonstration purposes.
-- ----------------------------------------------------------------------------
INSERT INTO app_user (user_id, username, password_hash, role) VALUES
-- Admin user
(1, 'admin',           '$2a$12$e8Y5c2QxW.mockHashAdmin001', 'ADMIN'),

-- Hospital coordinator users (Requester role)
(2, 'apollo_chennai',  '$2a$12$e8Y5c2QxW.mockHashHosp001',  'REQUESTER'),
(3, 'kmch_cbe',        '$2a$12$e8Y5c2QxW.mockHashHosp002',  'REQUESTER'),
(4, 'meenakshi_mdu',   '$2a$12$e8Y5c2QxW.mockHashHosp003',  'REQUESTER'),

-- Donor login accounts
(5, 'rahul_sharma',    '$2a$12$e8Y5c2QxW.mockHashDonor01',  'DONOR'),
(6, 'priya_ramesh',    '$2a$12$e8Y5c2QxW.mockHashDonor02',  'DONOR'),
(7, 'anand_kumar',     '$2a$12$e8Y5c2QxW.mockHashDonor03',  'DONOR'),
(8, 'deepa_nair',      '$2a$12$e8Y5c2QxW.mockHashDonor04',  'DONOR'),
(9, 'vijay_seth',      '$2a$12$e8Y5c2QxW.mockHashDonor05',  'DONOR');


-- ----------------------------------------------------------------------------
-- 2. Insert hospital records
-- 3 hospitals in 3 cities (including Chennai, Coimbatore, and Madurai)
-- ----------------------------------------------------------------------------
INSERT INTO hospital (hospital_id, user_id, name, city, phone) VALUES
(1, 2, 'Apollo Speciality Hospital', 'Chennai',    '044-28290200'),
(2, 3, 'Kovai Medical Center (KMCH)', 'Coimbatore', '0422-4323800'),
(3, 4, 'Meenakshi Mission Hospital',  'Madurai',    '0452-4263000');


-- ----------------------------------------------------------------------------
-- 3. Insert donor records (20 fictional donors)
-- Requirements:
--   - Covers all 8 blood groups: A+, A-, B+, B-, AB+, AB-, O+, O-
--   - Distributed across Chennai, Coimbatore, and Madurai
--   - Exactly 5 donors donated within the last 30 days (ineligible)
--   - Remaining 15 donors have NULL (never donated) or > 90 days ago (eligible)
-- ----------------------------------------------------------------------------
INSERT INTO donor (donor_id, user_id, name, age, gender, blood_group, phone, city, last_donation_date, available) VALUES
-- Donors with recent donations (< 30 days ago -> NOT eligible to donate today)
(1,  5,    'Rahul Sharma',     24, 'M', 'A+',  '9840112345', 'Chennai',    DATE_SUB(CURDATE(), INTERVAL 10 DAY), TRUE),
(2,  6,    'Priya Ramesh',     29, 'F', 'O+',  '9840223456', 'Chennai',    DATE_SUB(CURDATE(), INTERVAL 15 DAY), TRUE),
(3,  7,    'Anand Kumar',      35, 'M', 'B+',  '9840334567', 'Coimbatore', DATE_SUB(CURDATE(), INTERVAL 20 DAY), TRUE),
(4,  8,    'Deepa Nair',       27, 'F', 'AB+', '9840445678', 'Madurai',    DATE_SUB(CURDATE(), INTERVAL 25 DAY), TRUE),
(5,  9,    'Vijay Seth',       42, 'M', 'O-',  '9840556789', 'Chennai',    DATE_SUB(CURDATE(), INTERVAL 28 DAY), TRUE),

-- Donors with donation history > 90 days ago (Eligible if available)
(6,  NULL, 'Karthik Raja',     31, 'M', 'A-',  '9840667890', 'Chennai',    DATE_SUB(CURDATE(), INTERVAL 110 DAY), TRUE),
(7,  NULL, 'Sneha Krishnan',   26, 'F', 'B-',  '9840778901', 'Chennai',    DATE_SUB(CURDATE(), INTERVAL 140 DAY), TRUE),
(8,  NULL, 'Suresh Babu',      48, 'M', 'AB-', '9840889012', 'Coimbatore', DATE_SUB(CURDATE(), INTERVAL 105 DAY), TRUE),
(9,  NULL, 'Lakshmi Devi',     38, 'F', 'O+',  '9840990123', 'Madurai',    DATE_SUB(CURDATE(), INTERVAL 180 DAY), TRUE),
(10, NULL, 'Manoj Kumar',      22, 'M', 'A+',  '9841112233', 'Chennai',    DATE_SUB(CURDATE(), INTERVAL 95 DAY),  TRUE),
(11, NULL, 'Divya Bharathi',   30, 'F', 'B+',  '9841223344', 'Coimbatore', DATE_SUB(CURDATE(), INTERVAL 125 DAY), TRUE),
(12, NULL, 'Ganesh Moorthy',   50, 'M', 'O-',  '9841334455', 'Chennai',    DATE_SUB(CURDATE(), INTERVAL 150 DAY), FALSE), -- Marked temporarily unavailable
(13, NULL, 'Aravind Swamy',    34, 'M', 'AB+', '9841445566', 'Madurai',    DATE_SUB(CURDATE(), INTERVAL 210 DAY), TRUE),

-- Donors with NULL last_donation_date (First-time donors -> Eligible if available)
(14, NULL, 'Pooja Hegde',      21, 'F', 'A-',  '9841556677', 'Chennai',    NULL, TRUE),
(15, NULL, 'Dinesh Karthik',   28, 'M', 'B-',  '9841667788', 'Coimbatore', NULL, TRUE),
(16, NULL, 'Meera Jasmine',    23, 'F', 'AB-', '9841778899', 'Chennai',    NULL, TRUE),
(17, NULL, 'Saravanan M',      39, 'M', 'O+',  '9841889900', 'Madurai',    NULL, TRUE),
(18, NULL, 'Swetha Sundar',    25, 'F', 'A+',  '9841990011', 'Chennai',    NULL, TRUE),
(19, NULL, 'Naveen Prashanth', 33, 'M', 'B+',  '9842001122', 'Chennai',    NULL, TRUE),
(20, NULL, 'Kavitha R',        29, 'F', 'O-',  '9842112233', 'Coimbatore', NULL, TRUE);


-- ----------------------------------------------------------------------------
-- 4. Insert blood_request records
-- 6 requests featuring different hospitals, urgencies, and statuses
-- ----------------------------------------------------------------------------
INSERT INTO blood_request (request_id, hospital_id, patient_name, blood_group, units_needed, urgency, status, request_date) VALUES
-- Pending requests with distinct urgencies and timestamps
(1, 1, 'Ramesh Chandran', 'O-',  3, 'CRITICAL', 'PENDING',   DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(2, 2, 'Kavya S',         'A+',  2, 'HIGH',     'PENDING',   DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(3, 1, 'Murugan K',       'B+',  1, 'NORMAL',   'PENDING',   DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(4, 3, 'Balaji Narayanan','AB-', 2, 'CRITICAL', 'PENDING',   DATE_SUB(NOW(), INTERVAL 2 HOUR)),

-- Matched and fulfilled requests
(5, 1, 'Anita Joseph',    'O+',  4, 'HIGH',     'MATCHED',   DATE_SUB(NOW(), INTERVAL 36 HOUR)),
(6, 2, 'Syed Ibrahim',    'A-',  1, 'NORMAL',   'FULFILLED', DATE_SUB(NOW(), INTERVAL 72 HOUR));


-- ----------------------------------------------------------------------------
-- 5. Insert blood_stock records
-- All 8 blood groups tracked across all 3 hospitals (3 x 8 = 24 records)
-- ----------------------------------------------------------------------------
INSERT INTO blood_stock (hospital_id, blood_group, units_available) VALUES
-- Hospital 1: Apollo Speciality Hospital (Chennai)
(1, 'A+',  15),
(1, 'A-',   6),
(1, 'B+',  12),
(1, 'B-',   4),
(1, 'AB+',  8),
(1, 'AB-',  2),
(1, 'O+',  20),
(1, 'O-',   3),

-- Hospital 2: Kovai Medical Center (Coimbatore)
(2, 'A+',  10),
(2, 'A-',   5),
(2, 'B+',  14),
(2, 'B-',   3),
(2, 'AB+',  6),
(2, 'AB-',  1),
(2, 'O+',  18),
(2, 'O-',   2),

-- Hospital 3: Meenakshi Mission Hospital (Madurai)
(3, 'A+',   8),
(3, 'A-',   3),
(3, 'B+',  11),
(3, 'B-',   2),
(3, 'AB+',  5),
(3, 'AB-',  1),
(3, 'O+',  14),
(3, 'O-',   2);


-- ----------------------------------------------------------------------------
-- 6. Table: donation
-- Intentionally left empty for Phase 1 as specified in project requirements.
-- ----------------------------------------------------------------------------
