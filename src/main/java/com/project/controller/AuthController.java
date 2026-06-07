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

    // Register Guest using EMAIL
    public boolean registerGuest(String fullName, String email, String pass) {
        String query = "INSERT INTO Guest (full_name, email, password) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, fullName);
            pstmt.setString(2, email);
            pstmt.setString(3, pass);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { 
            System.out.println("Register Error: " + e.getMessage()); 
            return false; 
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
}