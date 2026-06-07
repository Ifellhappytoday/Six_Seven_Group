package com.project.controller;
import java.sql.*;

public class GuestAuthController {
    private final String url = "jdbc:postgresql://ep-long-silence-ao4r46pb.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
    private final String user = "neondb_owner";
    private final String password = "npg_Cy0XQEZk2KSG"; 

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public boolean login(String username, String pass) {
        String query = "SELECT * FROM Guest WHERE username = ? AND password = ? AND is_deleted = FALSE";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username); pstmt.setString(2, pass);
            return pstmt.executeQuery().next();
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean register(String fullName, String contact, String email, String username, String pass) {
        String query = "INSERT INTO Guest (full_name, contact_number, email, username, password) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, fullName); pstmt.setString(2, contact); pstmt.setString(3, email);
            pstmt.setString(4, username); pstmt.setString(5, pass);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { 
            System.out.println("GUEST DB ERROR: " + e.getMessage()); return false; 
        }
    }
}