package com.project.controller;

import com.project.model.Maintenance;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceController {
    private final String url = "jdbc:postgresql://ep-long-silence-ao4r46pb.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
    private final String dbUser = "neondb_owner";
    private final String password = "npg_Cy0XQEZk2KSG"; // ⚠️ Fill in your password

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, dbUser, password);
    }

    // Get all active maintenance records
    public List<Maintenance> getAllActiveMaintenance() {
        List<Maintenance> list = new ArrayList<>();
        String query = "SELECT * FROM Maintenance WHERE is_deleted = FALSE ORDER BY start_date DESC";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // Get maintenance by property
    public List<Maintenance> getMaintenanceByProperty(int propertyId) {
        List<Maintenance> list = new ArrayList<>();
        String query = "SELECT * FROM Maintenance WHERE property_id = ? AND is_deleted = FALSE ORDER BY start_date DESC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, propertyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * Schedule new maintenance. Checks for:
     * 1. Overlapping active bookings — warns staff to notify guest
     * 2. Overlapping other maintenance tasks
     */
    public String scheduleMaintenance(int propertyId, String type, String description,
                                       Date startDate, Date endDate, String assignedTo, double cost) {
        // Check for overlapping bookings
        String overlapBooking = "SELECT COUNT(*) FROM Booking WHERE property_id = ? AND is_deleted = FALSE " +
                "AND NOT (end_date <= ? OR start_date >= ?)";
        String overlapMaint = "SELECT COUNT(*) FROM Maintenance WHERE property_id = ? AND is_deleted = FALSE " +
                "AND status NOT IN ('Completed','Cancelled') AND NOT (end_date <= ? OR start_date >= ?)";

        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(overlapBooking)) {
                ps.setInt(1, propertyId);
                ps.setDate(2, startDate);
                ps.setDate(3, endDate);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return "CONFLICT_BOOKING"; // Active booking in this period
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(overlapMaint)) {
                ps.setInt(1, propertyId);
                ps.setDate(2, startDate);
                ps.setDate(3, endDate);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return "CONFLICT_MAINTENANCE";
                }
            }

            String insertSQL = "INSERT INTO Maintenance (property_id, maintenance_type, description, " +
                    "start_date, end_date, status, assigned_to, estimated_cost) VALUES (?, ?, ?, ?, ?, 'Scheduled', ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                ps.setInt(1, propertyId);
                ps.setString(2, type);
                ps.setString(3, description);
                ps.setDate(4, startDate);
                ps.setDate(5, endDate);
                ps.setString(6, assignedTo);
                ps.setDouble(7, cost);
                ps.executeUpdate();
            }

            // Block property during maintenance
            String updateStatus = "UPDATE HeritageProperty SET current_status = 'Under Maintenance' WHERE property_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateStatus)) {
                ps.setInt(1, propertyId);
                ps.executeUpdate();
            }

            return "SUCCESS";
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    // Update maintenance status (e.g. from Scheduled → In Progress → Completed)
    public boolean updateMaintenanceStatus(int maintenanceId, String newStatus) {
        String query = "UPDATE Maintenance SET status = ? WHERE maintenance_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, newStatus);
            ps.setInt(2, maintenanceId);
            boolean updated = ps.executeUpdate() > 0;

            // If completed, set property back to Available
            if (updated && newStatus.equals("Completed")) {
                String getProperty = "SELECT property_id FROM Maintenance WHERE maintenance_id = ?";
                try (PreparedStatement ps2 = conn.prepareStatement(getProperty)) {
                    ps2.setInt(1, maintenanceId);
                    ResultSet rs = ps2.executeQuery();
                    if (rs.next()) {
                        int propId = rs.getInt("property_id");
                        String restore = "UPDATE HeritageProperty SET current_status = 'Available' WHERE property_id = ?";
                        try (PreparedStatement ps3 = conn.prepareStatement(restore)) {
                            ps3.setInt(1, propId);
                            ps3.executeUpdate();
                        }
                    }
                }
            }
            return updated;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // Soft delete a maintenance record
    public boolean deleteMaintenance(int maintenanceId) {
        String query = "UPDATE Maintenance SET is_deleted = TRUE WHERE maintenance_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, maintenanceId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private Maintenance mapRow(ResultSet rs) throws SQLException {
        return new Maintenance(
            rs.getInt("maintenance_id"),
            rs.getInt("property_id"),
            rs.getString("maintenance_type"),
            rs.getString("description"),
            rs.getDate("start_date"),
            rs.getDate("end_date"),
            rs.getString("status"),
            rs.getString("assigned_to"),
            rs.getDouble("estimated_cost"),
            rs.getBoolean("is_deleted")
        );
    }
}