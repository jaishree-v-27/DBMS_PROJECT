package com.bloodnetwork.web.controller;

import com.bloodnetwork.dao.ReportDAO;
import com.bloodnetwork.dao.StockDAO;
import com.bloodnetwork.dao.UserDAO;
import com.bloodnetwork.matching.CompatibilityTable;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.*;

/**
 * REST API delivering structured data for Chart.js interactive charts,
 * Stock vs Demand comparisons, and analytical telemetry.
 */
@RestController
@RequestMapping("/api/analytics")
public class ApiReportsController {

    private final JdbcTemplate jdbcTemplate;
    private final StockDAO stockDAO = new StockDAO();
    private final ReportDAO reportDAO = new ReportDAO();
    private final UserDAO userDAO = new UserDAO();

    // Standard clinical display ordering required by Section 8
    public static final String[] ORDERED_BLOOD_GROUPS = {"O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"};

    @Autowired
    public ApiReportsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Grouped bar chart data comparing available blood inventory vs open requested units.
     * Order: O-, O+, A-, A+, B-, B+, AB-, AB+.
     */
    @GetMapping("/stock-vs-demand")
    public Map<String, Object> getStockVsDemand(@RequestParam(value = "hospitalId", required = false) Integer hospitalIdParam,
                                                HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        // Resolve authorization scope
        String role = (session != null) ? (String) session.getAttribute("role") : null;
        Integer userId = (session != null) ? (Integer) session.getAttribute("userId") : null;

        Integer effectiveHospitalId = hospitalIdParam;
        if ("REQUESTER".equalsIgnoreCase(role) && userId != null) {
            try {
                effectiveHospitalId = userDAO.findHospitalIdByUserId(userId);
            } catch (SQLException ignored) {}
        }

        Map<String, Integer> availableMap = new LinkedHashMap<>();
        Map<String, Integer> requestedMap = new LinkedHashMap<>();
        for (String bg : ORDERED_BLOOD_GROUPS) {
            availableMap.put(bg, 0);
            requestedMap.put(bg, 0);
        }

        // 1. Query available stock
        StringBuilder stockSql = new StringBuilder("SELECT blood_group, SUM(units_available) AS total FROM blood_stock ");
        List<Object> stockParams = new ArrayList<>();
        if (effectiveHospitalId != null) {
            stockSql.append("WHERE hospital_id = ? ");
            stockParams.add(effectiveHospitalId);
        }
        stockSql.append("GROUP BY blood_group");

        jdbcTemplate.query(stockSql.toString(), (rs) -> {
            String bg = rs.getString("blood_group");
            int total = rs.getInt("total");
            if (availableMap.containsKey(bg)) {
                availableMap.put(bg, total);
            }
        }, stockParams.toArray());

        // 2. Query open demands (PENDING or MATCHED)
        StringBuilder reqSql = new StringBuilder("SELECT blood_group, SUM(units_needed) AS total FROM blood_request ");
        List<Object> reqParams = new ArrayList<>();
        reqSql.append("WHERE status IN ('PENDING', 'MATCHED') ");
        if (effectiveHospitalId != null) {
            reqSql.append("AND hospital_id = ? ");
            reqParams.add(effectiveHospitalId);
        }
        reqSql.append("GROUP BY blood_group");

        jdbcTemplate.query(reqSql.toString(), (rs) -> {
            String bg = rs.getString("blood_group");
            int total = rs.getInt("total");
            if (requestedMap.containsKey(bg)) {
                requestedMap.put(bg, total);
            }
        }, reqParams.toArray());

        List<String> labels = Arrays.asList(ORDERED_BLOOD_GROUPS);
        List<Integer> availableValues = new ArrayList<>();
        List<Integer> requestedValues = new ArrayList<>();
        List<Integer> shortageValues = new ArrayList<>();

        int totalShortage = 0;
        List<String> shortageGroups = new ArrayList<>();

        for (String bg : ORDERED_BLOOD_GROUPS) {
            int avail = availableMap.get(bg);
            int req = requestedMap.get(bg);
            int shortf = Math.max(0, req - avail);

            availableValues.add(avail);
            requestedValues.add(req);
            shortageValues.add(shortf);

            if (shortf > 0) {
                totalShortage += shortf;
                shortageGroups.add(bg + " (" + shortf + "u needed)");
            }
        }

        String summaryText;
        if (totalShortage == 0) {
            summaryText = "Inventory Adequate: All current open patient demands can be satisfied by on-hand reserves.";
        } else {
            summaryText = "Critical Shortage Detected: Deficit of " + totalShortage + " units across: " 
                        + String.join(", ", shortageGroups) + ". Recommended action: Dispatch notifications to compatible voluntary donors.";
        }

        response.put("labels", labels);
        response.put("available", availableValues);
        response.put("requested", requestedValues);
        response.put("shortages", shortageValues);
        response.put("totalShortage", totalShortage);
        response.put("summary", summaryText);

        return response;
    }

    /**
     * Returns aggregate blood inventory levels across all 8 blood groups in ordered sequence.
     */
    @GetMapping("/stock-by-group")
    public Map<String, Object> getStockByGroup() {
        Map<String, Object> response = new HashMap<>();
        List<String> labels = Arrays.asList(ORDERED_BLOOD_GROUPS);
        List<Integer> values = new ArrayList<>();

        Map<String, Integer> map = new HashMap<>();
        for (String g : ORDERED_BLOOD_GROUPS) map.put(g, 0);

        try {
            Map<String, Integer> stockMap = stockDAO.getTotalByBloodGroup();
            map.putAll(stockMap);
        } catch (SQLException ignored) {}

        for (String g : ORDERED_BLOOD_GROUPS) {
            values.add(map.getOrDefault(g, 0));
        }

        response.put("labels", labels);
        response.put("data", values);
        return response;
    }

    /**
     * Returns distribution of blood requests by lifecycle status.
     */
    @GetMapping("/requests-by-status")
    public Map<String, Object> getRequestsByStatus() {
        Map<String, Object> response = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        try {
            List<Map<String, Object>> rows = reportDAO.getRequestCountByStatus();
            for (Map<String, Object> row : rows) {
                Object status = row.get("status");
                Object count = row.get("request_count");
                labels.add(status != null ? status.toString() : "UNKNOWN");
                values.add(count instanceof Number ? ((Number) count).intValue() : 0);
            }
        } catch (SQLException e) {
            labels.add("PENDING"); values.add(0);
            labels.add("MATCHED"); values.add(0);
            labels.add("FULFILLED"); values.add(0);
            labels.add("CANCELLED"); values.add(0);
        }

        response.put("labels", labels);
        response.put("data", values);
        return response;
    }

    /**
     * Returns monthly donation volume for the current year.
     */
    @GetMapping("/monthly-trends")
    public Map<String, Object> getMonthlyTrends() {
        Map<String, Object> response = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        try {
            List<Map<String, Object>> rows = reportDAO.getDonationsPerMonthCurrentYear();
            for (Map<String, Object> row : rows) {
                Object monthName = row.get("month_name");
                Object units = row.get("total_units");
                labels.add(monthName != null ? monthName.toString() : "Month");
                values.add(units instanceof Number ? ((Number) units).intValue() : 0);
            }
        } catch (SQLException ignored) {}

        if (labels.isEmpty()) {
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            for (String m : months) {
                labels.add(m);
                values.add(0);
            }
        }

        response.put("labels", labels);
        response.put("data", values);
        return response;
    }
}
