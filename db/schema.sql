-- ============================================================================
-- Blood Donor Network - Database Schema (Phase 1)
-- Database: blood_network
-- Target RDBMS: MySQL 8.0+
-- Description: Creates the database and core tables for managing users,
--              donors, hospitals, blood requests, donations, and inventory.
-- ============================================================================

-- Drop database if it already exists to allow safe re-execution of this script
DROP DATABASE IF EXISTS blood_network;

-- Create the database with UTF-8 character encoding for proper text handling
CREATE DATABASE blood_network CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Switch to the newly created database
USE blood_network;


-- ----------------------------------------------------------------------------
-- Table 1: app_user
-- Purpose: Stores login credentials and system roles for authentication.
-- Roles:
--   - 'ADMIN': Manages system-wide operations.
--   - 'DONOR': Regular donors who register and donate.
--   - 'REQUESTER': Hospital staff or coordinators who request blood.
-- ----------------------------------------------------------------------------
CREATE TABLE app_user (
    user_id INT AUTO_INCREMENT PRIMARY KEY,          -- Unique ID for each system user
    username VARCHAR(50) NOT NULL UNIQUE,            -- Login username (must be unique)
    password_hash VARCHAR(255) NOT NULL,             -- Hashed password string for security
    role ENUM('ADMIN', 'DONOR', 'REQUESTER') NOT NULL -- Access control role
);


-- ----------------------------------------------------------------------------
-- Table 2: donor
-- Purpose: Stores personal and medical eligibility information for donors.
-- Notes:
--   - user_id links to app_user (optional: ON DELETE SET NULL preserves records).
--   - Age is restricted between 18 and 65 (legal/medical criteria for donation).
-- ----------------------------------------------------------------------------
CREATE TABLE donor (
    donor_id INT AUTO_INCREMENT PRIMARY KEY,         -- Unique ID for each donor
    user_id INT UNIQUE NULL,                         -- Linked user account (if donor has login)
    name VARCHAR(100) NOT NULL,                      -- Full name of donor
    age INT NOT NULL CHECK (age >= 18 AND age <= 65),-- Age must be between 18 and 65 years
    gender ENUM('M', 'F', 'O'),                      -- Donor gender
    blood_group ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL, -- Blood group
    phone VARCHAR(15) NOT NULL,                      -- Primary contact number
    city VARCHAR(50) NOT NULL,                       -- City of residence
    last_donation_date DATE NULL,                    -- Date of most recent donation (NULL if never)
    available BOOLEAN DEFAULT TRUE,                  -- Self-reported readiness to donate

    -- If the login user is deleted, keep donor record but set user_id to NULL
    CONSTRAINT fk_donor_user FOREIGN KEY (user_id) 
        REFERENCES app_user(user_id) 
        ON DELETE SET NULL
);


-- ----------------------------------------------------------------------------
-- Table 3: hospital
-- Purpose: Stores details of participating hospitals and blood bank centers.
-- Notes:
--   - user_id links hospital coordinator/staff login in app_user.
-- ----------------------------------------------------------------------------
CREATE TABLE hospital (
    hospital_id INT AUTO_INCREMENT PRIMARY KEY,      -- Unique ID for each hospital
    user_id INT UNIQUE NULL,                         -- Linked requester login account
    name VARCHAR(100) NOT NULL,                      -- Name of hospital / medical center
    city VARCHAR(50) NOT NULL,                       -- Location city
    phone VARCHAR(15),                               -- Emergency / blood bank contact number

    -- If the hospital login account is deleted, keep hospital details
    CONSTRAINT fk_hospital_user FOREIGN KEY (user_id) 
        REFERENCES app_user(user_id) 
        ON DELETE SET NULL
);


-- ----------------------------------------------------------------------------
-- Table 4: blood_request
-- Purpose: Tracks requests raised by hospitals for specific blood groups.
-- Urgency: CRITICAL, HIGH, NORMAL
-- Status: PENDING (initial), MATCHED, FULFILLED, CANCELLED
-- ----------------------------------------------------------------------------
CREATE TABLE blood_request (
    request_id INT AUTO_INCREMENT PRIMARY KEY,       -- Unique ID for each blood request
    hospital_id INT NOT NULL,                        -- Hospital raising the request
    patient_name VARCHAR(100) NOT NULL,              -- Name of the patient in need
    blood_group ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL, -- Required blood group
    units_needed INT NOT NULL CHECK (units_needed > 0), -- Number of blood units needed (must be > 0)
    urgency ENUM('CRITICAL', 'HIGH', 'NORMAL') DEFAULT 'NORMAL',                  -- Priority level
    status ENUM('PENDING', 'MATCHED', 'FULFILLED', 'CANCELLED') DEFAULT 'PENDING',-- Processing status
    request_date DATETIME DEFAULT CURRENT_TIMESTAMP, -- Timestamp when request was submitted

    -- Enforce valid hospital reference
    CONSTRAINT fk_request_hospital FOREIGN KEY (hospital_id) 
        REFERENCES hospital(hospital_id)
);


-- ----------------------------------------------------------------------------
-- Table 5: donation
-- Purpose: Records completed donation events by donors at hospitals.
-- Notes:
--   - request_id is optional (NULL for general donations, set if fulfilling a specific request).
-- ----------------------------------------------------------------------------
CREATE TABLE donation (
    donation_id INT AUTO_INCREMENT PRIMARY KEY,      -- Unique ID for each donation record
    donor_id INT NOT NULL,                           -- Donor who provided the blood
    hospital_id INT NOT NULL,                        -- Hospital where donation took place
    request_id INT NULL,                             -- Associated request (if fulfilling one)
    units INT NOT NULL DEFAULT 1 CHECK (units > 0),  -- Units of blood donated (must be > 0)
    donation_date DATE NOT NULL,                     -- Date when donation happened

    -- Foreign keys connecting donation to donor, hospital, and request
    CONSTRAINT fk_donation_donor FOREIGN KEY (donor_id) 
        REFERENCES donor(donor_id),
    CONSTRAINT fk_donation_hospital FOREIGN KEY (hospital_id) 
        REFERENCES hospital(hospital_id),
    CONSTRAINT fk_donation_request FOREIGN KEY (request_id) 
        REFERENCES blood_request(request_id)
);


-- ----------------------------------------------------------------------------
-- Table 6: blood_stock
-- Purpose: Tracks current inventory levels of each blood group at each hospital.
-- Notes:
--   - Composite primary key: (hospital_id, blood_group) ensures each hospital
--     has exactly one inventory row per blood group.
-- ----------------------------------------------------------------------------
CREATE TABLE blood_stock (
    hospital_id INT NOT NULL,                        -- Hospital holding the stock
    blood_group ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL, -- Blood group
    units_available INT NOT NULL DEFAULT 0 CHECK (units_available >= 0),          -- Quantity on hand (>= 0)

    -- Composite primary key
    PRIMARY KEY (hospital_id, blood_group),

    -- Foreign key to hospital
    CONSTRAINT fk_stock_hospital FOREIGN KEY (hospital_id) 
        REFERENCES hospital(hospital_id)
);
