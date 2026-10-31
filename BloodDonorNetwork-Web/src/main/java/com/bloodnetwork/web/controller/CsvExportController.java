package com.bloodnetwork.web.controller;

import com.bloodnetwork.dao.UserDAO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller providing server-side CSV exports for authorized administrators and hospital staff.
 */
@Controller
@RequestMapping("/export")
public class CsvExportController {

    private final JdbcTemplate jdbcTemplate;
    private final UserDAO userDAO = new UserDAO();

    @Autowired
    public CsvExportController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Exports blood requests to CSV with role scoping.
     */
    @GetMapping("/requests.csv")
    public void exportRequestsCsv(HttpSession session, HttpServletResponse response) throws IOException {
        String role = (session != null) ? (String) session.getAttribute("role") : null;
        Integer userId = (session != null) ? (Integer) session.getAttribute("userId") : null;

        if (role == null || (!"ADMIN".equalsIgnoreCase(role) && !"REQUESTER".equalsIgnoreCase(role))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied to export data.");
            return;
        }

        Integer hospitalScope = null;
        if ("REQUESTER".equalsIgnoreCase(role) && userId != null) {
            try {
                hospitalScope = userDAO.findHospitalIdByUserId(userId);
            } catch (SQLException ignored) {}
        }

        StringBuilder sql = new StringBuilder(
                "SELECT r.request_id, h.name AS hospital_name, r.patient_name, r.blood_group, "
                + "       r.units_needed, r.urgency, r.status, r.request_date "
                + "FROM blood_request r "
                + "JOIN hospital h ON r.hospital_id = h.hospital_id "
        );
        List<Object> params = new ArrayList<>();
        if (hospitalScope != null) {
            sql.append("WHERE r.hospital_id = ? ");
            params.add(hospitalScope);
        }
        sql.append("ORDER BY r.request_date DESC, r.request_id DESC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"blood_requests_" + LocalDate.now() + ".csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Request ID,Hospital,Patient Name,Blood Group,Units Needed,Urgency,Status,Date Raised");

        for (Map<String, Object> r : rows) {
            writer.printf("%s,\"%s\",\"%s\",%s,%s,%s,%s,%s%n",
                    r.get("request_id"),
                    escapeCsv((String) r.get("hospital_name")),
                    escapeCsv((String) r.get("patient_name")),
                    r.get("blood_group"),
                    r.get("units_needed"),
                    r.get("urgency"),
                    r.get("status"),
                    r.get("request_date")
            );
        }
        writer.flush();
    }

    /**
     * Exports blood inventory stock to CSV with role scoping.
     */
    @GetMapping("/inventory.csv")
    public void exportInventoryCsv(HttpSession session, HttpServletResponse response) throws IOException {
        String role = (session != null) ? (String) session.getAttribute("role") : null;
        Integer userId = (session != null) ? (Integer) session.getAttribute("userId") : null;

        if (role == null || (!"ADMIN".equalsIgnoreCase(role) && !"REQUESTER".equalsIgnoreCase(role))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied to export data.");
            return;
        }

        Integer hospitalScope = null;
        if ("REQUESTER".equalsIgnoreCase(role) && userId != null) {
            try {
                hospitalScope = userDAO.findHospitalIdByUserId(userId);
            } catch (SQLException ignored) {}
        }

        StringBuilder sql = new StringBuilder(
                "SELECT s.hospital_id, h.name AS hospital_name, h.city, s.blood_group, s.units_available "
                + "FROM blood_stock s "
                + "JOIN hospital h ON s.hospital_id = h.hospital_id "
        );
        List<Object> params = new ArrayList<>();
        if (hospitalScope != null) {
            sql.append("WHERE s.hospital_id = ? ");
            params.add(hospitalScope);
        }
        sql.append("ORDER BY h.name ASC, s.blood_group ASC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"blood_inventory_" + LocalDate.now() + ".csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Hospital ID,Hospital Name,City,Blood Group,Units Available,Shortage Warning");

        for (Map<String, Object> r : rows) {
            int units = ((Number) r.get("units_available")).intValue();
            String alert = units < 3 ? "LOW_STOCK" : "ADEQUATE";
            writer.printf("%s,\"%s\",\"%s\",%s,%d,%s%n",
                    r.get("hospital_id"),
                    escapeCsv((String) r.get("hospital_name")),
                    escapeCsv((String) r.get("city")),
                    r.get("blood_group"),
                    units,
                    alert
            );
        }
        writer.flush();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }
}
