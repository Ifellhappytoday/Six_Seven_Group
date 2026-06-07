package com.project.controller;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportController {
    private final String url = "jdbc:postgresql://ep-long-silence-ao4r46pb.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
    private final String dbUser = "neondb_owner";
    private final String password = "npg_Cy0XQEZk2KSG"; // ⚠️ Fill in your password

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, dbUser, password);
    }

    // -----------------------------------------------------------
    // Report Data Holder (used for per-property summary rows)
    // -----------------------------------------------------------
    public static class PropertyReport {
        public int propertyId;
        public String propertyName;
        public int rentalDays;
        public int maintenanceDays;
        public int totalDays;         // rentalDays + maintenanceDays
        public double occupancyRate;  // rentalDays / totalDays * 100
        public double totalRevenue;
        public double totalMaintenanceCost;
        public String healthStatus;   // "Good", "Needs Attention", "At Risk"

        public PropertyReport(int propertyId, String propertyName, int rentalDays, int maintenanceDays,
                              double totalRevenue, double totalMaintenanceCost) {
            this.propertyId = propertyId;
            this.propertyName = propertyName;
            this.rentalDays = rentalDays;
            this.maintenanceDays = maintenanceDays;
            this.totalDays = rentalDays + maintenanceDays;
            this.occupancyRate = totalDays > 0 ? (rentalDays * 100.0 / totalDays) : 0;
            this.totalRevenue = totalRevenue;
            this.totalMaintenanceCost = totalMaintenanceCost;
            this.healthStatus = calculateHealth(maintenanceDays, totalDays);
        }

        private String calculateHealth(int maintDays, int total) {
            if (total == 0) return "No Data";
            double maintRatio = maintDays * 100.0 / total;
            if (maintRatio >= 20) return "Good";            // Enough preservation time
            else if (maintRatio >= 10) return "Needs Attention";
            else return "At Risk";                          // Almost no preservation time
        }
    }

    // -----------------------------------------------------------
    // Monthly Revenue vs Maintenance Cost (for bar chart data)
    // -----------------------------------------------------------
    public static class MonthlyData {
        public String monthLabel;    // e.g. "Jan 2025"
        public double revenue;
        public double maintenanceCost;

        public MonthlyData(String monthLabel, double revenue, double maintenanceCost) {
            this.monthLabel = monthLabel;
            this.revenue = revenue;
            this.maintenanceCost = maintenanceCost;
        }
    }

    // -----------------------------------------------------------
    // Get per-property occupancy report for a date range
    // -----------------------------------------------------------
    public List<PropertyReport> getOccupancyReport(Date fromDate, Date toDate) {
        List<PropertyReport> reports = new ArrayList<>();

        String query = """
            SELECT
                hp.property_id,
                hp.property_name,
                COALESCE(SUM(
                    GREATEST(0, EXTRACT(DAY FROM (
                        LEAST(b.end_date, ?) - GREATEST(b.start_date, ?)
                    ))::int)
                ) FILTER (WHERE b.booking_id IS NOT NULL AND b.is_deleted = FALSE), 0) AS rental_days,
                COALESCE(SUM(
                    GREATEST(0, EXTRACT(DAY FROM (
                        LEAST(m.end_date, ?) - GREATEST(m.start_date, ?)
                    ))::int)
                ) FILTER (WHERE m.maintenance_id IS NOT NULL AND m.is_deleted = FALSE AND m.status NOT IN ('Cancelled')), 0) AS maintenance_days,
                COALESCE(SUM(b.total_price) FILTER (WHERE b.booking_id IS NOT NULL AND b.is_deleted = FALSE), 0) AS total_revenue,
                COALESCE(SUM(m.estimated_cost) FILTER (WHERE m.maintenance_id IS NOT NULL AND m.is_deleted = FALSE AND m.status NOT IN ('Cancelled')), 0) AS total_maint_cost
            FROM HeritageProperty hp
            LEFT JOIN Booking b ON hp.property_id = b.property_id
                AND b.start_date < ? AND b.end_date > ?
            LEFT JOIN Maintenance m ON hp.property_id = m.property_id
                AND m.start_date < ? AND m.end_date > ?
            WHERE hp.is_deleted = FALSE
            GROUP BY hp.property_id, hp.property_name
            ORDER BY hp.property_id
        """;

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            // LEAST/GREATEST bounds
            ps.setDate(1, toDate);   ps.setDate(2, fromDate); // rental days
            ps.setDate(3, toDate);   ps.setDate(4, fromDate); // maint days
            // JOIN conditions
            ps.setDate(5, toDate);   ps.setDate(6, fromDate); // booking join
            ps.setDate(7, toDate);   ps.setDate(8, fromDate); // maint join

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reports.add(new PropertyReport(
                    rs.getInt("property_id"),
                    rs.getString("property_name"),
                    rs.getInt("rental_days"),
                    rs.getInt("maintenance_days"),
                    rs.getDouble("total_revenue"),
                    rs.getDouble("total_maint_cost")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return reports;
    }

    // -----------------------------------------------------------
    // Monthly breakdown (last 12 months) for charts
    // -----------------------------------------------------------
    public List<MonthlyData> getMonthlyBreakdown() {
        List<MonthlyData> data = new ArrayList<>();

        String revenueSQL = """
            SELECT TO_CHAR(start_date, 'Mon YYYY') AS month,
                   DATE_TRUNC('month', start_date) AS month_sort,
                   SUM(total_price) AS revenue
            FROM Booking
            WHERE is_deleted = FALSE AND start_date >= NOW() - INTERVAL '12 months'
            GROUP BY month, month_sort
            ORDER BY month_sort
        """;

        String maintSQL = """
            SELECT TO_CHAR(start_date, 'Mon YYYY') AS month,
                   DATE_TRUNC('month', start_date) AS month_sort,
                   SUM(estimated_cost) AS maint_cost
            FROM Maintenance
            WHERE is_deleted = FALSE AND status NOT IN ('Cancelled')
                  AND start_date >= NOW() - INTERVAL '12 months'
            GROUP BY month, month_sort
            ORDER BY month_sort
        """;

        Map<String, double[]> combined = new LinkedHashMap<>();

        try (Connection conn = getConnection()) {
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(revenueSQL)) {
                while (rs.next()) {
                    String month = rs.getString("month");
                    combined.computeIfAbsent(month, k -> new double[]{0, 0})[0] = rs.getDouble("revenue");
                }
            }
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(maintSQL)) {
                while (rs.next()) {
                    String month = rs.getString("month");
                    combined.computeIfAbsent(month, k -> new double[]{0, 0})[1] = rs.getDouble("maint_cost");
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        for (Map.Entry<String, double[]> entry : combined.entrySet()) {
            data.add(new MonthlyData(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }
        return data;
    }

    // -----------------------------------------------------------
    // Quick summary stats for the report header cards
    // -----------------------------------------------------------
    public Map<String, Object> getSummaryStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = getConnection()) {
            // Total revenue (all time)
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(total_price),0) FROM Booking WHERE is_deleted=FALSE")) {
                if (rs.next()) stats.put("totalRevenue", rs.getDouble(1));
            }
            // Total maintenance cost
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(estimated_cost),0) FROM Maintenance WHERE is_deleted=FALSE AND status != 'Cancelled'")) {
                if (rs.next()) stats.put("totalMaintenanceCost", rs.getDouble(1));
            }
            // Active bookings count
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Booking WHERE is_deleted=FALSE AND end_date >= CURRENT_DATE")) {
                if (rs.next()) stats.put("activeBookings", rs.getInt(1));
            }
            // Scheduled/in-progress maintenance count
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Maintenance WHERE is_deleted=FALSE AND status IN ('Scheduled','In Progress')")) {
                if (rs.next()) stats.put("activeMaintenance", rs.getInt(1));
            }
            // Properties at risk (less than 10% maintenance time)
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM HeritageProperty WHERE is_deleted=FALSE AND current_status='Available'")) {
                if (rs.next()) stats.put("availableProperties", rs.getInt(1));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return stats;
    }
}