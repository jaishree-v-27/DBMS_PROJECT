-- ============================================================================
-- Blood Donor Network - Database Triggers (Phase 4)
-- Database: blood_network
-- Target RDBMS: MySQL 8.0+
-- Description: Automates business logic in the database:
--              1. after_donation_insert: Updates donor's last_donation_date
--                 and adds donated units to the hospital's blood_stock.
--              2. after_donation_delete: Deducts donated units from blood_stock
--                 and recalculates donor's last_donation_date.
-- Safe to re-run: Drops triggers first if they already exist.
-- ============================================================================

USE blood_network;

-- Set custom statement delimiter to allow multi-statement trigger bodies
DELIMITER $$

-- ----------------------------------------------------------------------------
-- Trigger 1: after_donation_insert
-- Fires: AFTER INSERT ON donation
-- Purpose:
--   1. Automatically updates donor.last_donation_date to NEW.donation_date.
--   2. Adds NEW.units to blood_stock for the hospital and donor's blood group.
--      Uses INSERT ... ON DUPLICATE KEY UPDATE so missing rows are created.
-- ----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS after_donation_insert$$

CREATE TRIGGER after_donation_insert
AFTER INSERT ON donation
FOR EACH ROW
BEGIN
    -- Declare local variable to hold the donor's blood group
    DECLARE v_blood_group VARCHAR(5);

    -- Retrieve the blood group of the donating donor
    SELECT blood_group INTO v_blood_group
    FROM donor
    WHERE donor_id = NEW.donor_id;

    -- Update the donor's most recent donation date
    UPDATE donor
    SET last_donation_date = NEW.donation_date
    WHERE donor_id = NEW.donor_id;

    -- Increment the hospital's inventory for this blood group.
    -- If the hospital doesn't have an existing stock row for this blood group, insert one.
    INSERT INTO blood_stock (hospital_id, blood_group, units_available)
    VALUES (NEW.hospital_id, v_blood_group, NEW.units)
    ON DUPLICATE KEY UPDATE units_available = units_available + NEW.units;
END$$


-- ----------------------------------------------------------------------------
-- Trigger 2: after_donation_delete
-- Fires: AFTER DELETE ON donation
-- Purpose:
--   1. Reverses inventory by subtracting OLD.units from blood_stock (never below 0).
--   2. Re-evaluates donor.last_donation_date to the latest remaining donation
--      record for that donor, or sets it to NULL if no donations remain.
-- ----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS after_donation_delete$$

CREATE TRIGGER after_donation_delete
AFTER DELETE ON donation
FOR EACH ROW
BEGIN
    -- Declare local variables for blood group and latest remaining date
    DECLARE v_blood_group VARCHAR(5);
    DECLARE v_latest_donation_date DATE;

    -- Retrieve the donor's blood group
    SELECT blood_group INTO v_blood_group
    FROM donor
    WHERE donor_id = OLD.donor_id;

    -- Deduct units from inventory, ensuring stock never drops below 0
    UPDATE blood_stock
    SET units_available = GREATEST(0, units_available - OLD.units)
    WHERE hospital_id = OLD.hospital_id AND blood_group = v_blood_group;

    -- Find the most recent donation date remaining for this donor
    SELECT MAX(donation_date) INTO v_latest_donation_date
    FROM donation
    WHERE donor_id = OLD.donor_id;

    -- Update the donor's last_donation_date (will be NULL if no rows left)
    UPDATE donor
    SET last_donation_date = v_latest_donation_date
    WHERE donor_id = OLD.donor_id;
END$$

-- Reset delimiter back to standard semicolon
DELIMITER ;
