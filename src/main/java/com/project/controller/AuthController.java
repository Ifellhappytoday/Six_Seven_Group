package com.project.controller;
import java.sql.*;

public class AuthController {
    // ⚠️ CRITICAL: YOU MUST PUT YOUR REAL PASSWORD HERE
    private final String url = "jdbc:postgresql://ep-long-silence-ao4r46pb.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
    private final String user = "neondb_owner";
    private final String password = "npg_Cy0XQEZk2KSG"; 

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    // Unified Login: Uses EMAIL instead of Username
    public String authenticateUser(String email, String pass) {
        // 1. Check if they are Staff/Admin
        String staffQuery = "SELECT job_role FROM Staff WHERE email = ? AND password = ? AND is_deleted = FALSE";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(staffQuery)) {
            pstmt.setString(1, email);
            pstmt.setString(2, pass);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("job_role"); // Will return "Admin" or "Staff"
            }
        } catch (SQLException e) { System.out.println("DB Error: " + e.getMessage()); }

        // 2. If not staff, check if they are a Guest
        String guestQuery = "SELECT guest_id FROM Guest WHERE email = ? AND password = ? AND is_deleted = FALSE";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(guestQuery)) {
            pstmt.setString(1, email);
            pstmt.setString(2, pass);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return "Guest";
            }
        } catch (SQLException e) { System.out.println("DB Error: " + e.getMessage()); }

        return null; // Login failed
    }

    // Register Guest using EMAIL — returns "SUCCESS", "EMAIL_TAKEN", "NAME_TAKEN", or "ERROR"
    public String registerGuest(String fullName, String email, String pass) {
        if (nameExists(fullName))  return "NAME_TAKEN";
        if (emailExists(email))    return "EMAIL_TAKEN";

        String query = "INSERT INTO Guest (full_name, email, password) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, fullName);
            pstmt.setString(2, email);
            pstmt.setString(3, pass);
            return pstmt.executeUpdate() > 0 ? "SUCCESS" : "ERROR";
        } catch (SQLException e) {
            System.out.println("Register Error: " + e.getMessage());
            return "ERROR";
        }
    }

    // Check if email already exists in Guest or Staff table
    public boolean emailExists(String email) {
        // Check Guest table
        String guestQuery = "SELECT 1 FROM Guest WHERE email = ? AND is_deleted = FALSE";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(guestQuery)) {
            pstmt.setString(1, email);
            if (pstmt.executeQuery().next()) {
                return true;
            }
        } catch (SQLException e) { System.out.println("DB Error: " + e.getMessage()); }

        // Check Staff table
        String staffQuery = "SELECT 1 FROM Staff WHERE email = ? AND is_deleted = FALSE";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(staffQuery)) {
            pstmt.setString(1, email);
            if (pstmt.executeQuery().next()) {
                return true;
            }
        } catch (SQLException e) { System.out.println("DB Error: " + e.getMessage()); }

        return false;
    }

    // Check if full_name already exists in Guest or Staff table
    public boolean nameExists(String fullName) {
        String guestQuery = "SELECT 1 FROM Guest WHERE LOWER(full_name) = LOWER(?) AND is_deleted = FALSE";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(guestQuery)) {
            ps.setString(1, fullName);
            if (ps.executeQuery().next()) return true;
        } catch (SQLException e) { System.out.println("DB Error: " + e.getMessage()); }

        String staffQuery = "SELECT 1 FROM Staff WHERE LOWER(full_name) = LOWER(?) AND is_deleted = FALSE";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(staffQuery)) {
            ps.setString(1, fullName);
            if (ps.executeQuery().next()) return true;
        } catch (SQLException e) { System.out.println("DB Error: " + e.getMessage()); }

        return false;
    }

    /**
     * Registers a new staff member. Only callable by Admin.
     * Returns "SUCCESS", "EMAIL_TAKEN", or "ERROR".
     */
    public String registerStaff(String fullName, String email, String password, String contactNumber, String jobRole) {
        // Check name and email uniqueness across both tables
        if (nameExists(fullName))  return "NAME_TAKEN";
        if (emailExists(email))    return "EMAIL_TAKEN";

        String query = "INSERT INTO Staff (full_name, email, password, contact_number, job_role) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, fullName);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, contactNumber.isEmpty() ? null : contactNumber);
            pstmt.setString(5, jobRole);
            return pstmt.executeUpdate() > 0 ? "SUCCESS" : "ERROR";
        } catch (SQLException e) {
            System.out.println("Register Staff Error: " + e.getMessage());
            return "ERROR";
        }
    }

    /**
     * Returns all active staff members as a 2D array: {staff_id, full_name, email, contact_number, job_role}.
     */
    public java.util.List<String[]> getAllStaff() {
        java.util.List<String[]> list = new java.util.ArrayList<>();
        String query = "SELECT staff_id, full_name, email, contact_number, job_role FROM Staff WHERE is_deleted = FALSE ORDER BY staff_id";
        try (Connection conn = getConnection(); java.sql.Statement st = conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("staff_id")),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("contact_number") == null ? "" : rs.getString("contact_number"),
                    rs.getString("job_role")
                });
            }
        } catch (SQLException e) { System.out.println("DB Error: " + e.getMessage()); }
        return list;
    }

    /**
     * Soft-deletes a staff member by staff_id.
     */
    public boolean deleteStaff(int staffId) {
        String query = "UPDATE Staff SET is_deleted = TRUE WHERE staff_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, staffId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Delete Staff Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Soft-deletes a guest account by guest_id.
     */
    public boolean deleteGuest(int guestId) {
        String query = "UPDATE Guest SET is_deleted = TRUE WHERE guest_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, guestId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Delete Guest Error: " + e.getMessage());
            return false;
        }
    }
}