-- ============================================================================
-- Blood Donor Network - Test & Verification Queries (Phase 1)
-- Database: blood_network
-- Target RDBMS: MySQL 8.0+
-- Description: Test queries to verify database setup, relationships,
--              business logic, and sample data.
-- ============================================================================

USE blood_network;


-- ----------------------------------------------------------------------------
-- a) Query: Retrieve all donors
-- Description: Lists every registered donor with their demographic, blood
--              group, and availability details.
-- ----------------------------------------------------------------------------
SELECT 
    donor_id,
    name,
    age,
    gender,
    blood_group,
    phone,
    city,
    last_donation_date,
    available
FROM donor
ORDER BY donor_id ASC;


-- ----------------------------------------------------------------------------
-- b) Query: Retrieve eligible donors in Chennai
-- Description: Finds donors located in Chennai who are marked as available
--              AND either:
--                - have never donated before (last_donation_date IS NULL), OR
--                - have not donated in the last 90 days (medically eligible).
-- ----------------------------------------------------------------------------
SELECT 
    donor_id,
    name,
    age,
    gender,
    blood_group,
    phone,
    city,
    last_donation_date,
    available
FROM donor
WHERE city = 'Chennai'
  AND available = TRUE
  AND (
      last_donation_date IS NULL 
      OR last_donation_date <= DATE_SUB(CURDATE(), INTERVAL 90 DAY)
  )
ORDER BY blood_group, name;


-- ----------------------------------------------------------------------------
-- c) Query: Retrieve pending requests sorted by urgency, then oldest first
-- Description: Displays requests currently in 'PENDING' status.
--              Ordering rules:
--                1. Urgency: 'CRITICAL' first, followed by 'HIGH', then 'NORMAL'
--                   (implemented via MySQL FIELD() function).
--                2. Request Date: Oldest submission first (request_date ASC).
-- ----------------------------------------------------------------------------
SELECT 
    r.request_id,
    h.name AS hospital_name,
    h.city AS hospital_city,
    r.patient_name,
    r.blood_group,
    r.units_needed,
    r.urgency,
    r.status,
    r.request_date
FROM blood_request r
JOIN hospital h ON r.hospital_id = h.hospital_id
WHERE r.status = 'PENDING'
ORDER BY 
    FIELD(r.urgency, 'CRITICAL', 'HIGH', 'NORMAL'),
    r.request_date ASC;


-- ----------------------------------------------------------------------------
-- d) Query: Total blood stock per blood group
-- Description: Aggregates the available units of blood across all participating
--              hospitals grouped by blood group.
-- ----------------------------------------------------------------------------
SELECT 
    blood_group,
    SUM(units_available) AS total_units_available
FROM blood_stock
GROUP BY blood_group
ORDER BY blood_group ASC;
