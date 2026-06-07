package com.project.controller;
import com.project.model.HeritageProperty;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PropertyController {
    private final String url = "jdbc:postgresql://ep-long-silence-ao4r46pb.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
    private final String user = "neondb_owner";
    private final String password = "npg_Cy0XQEZk2KSG"; 

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public List<HeritageProperty> getAllActiveProperties() {
        List<HeritageProperty> properties = new ArrayList<>();
        String query = "SELECT * FROM HeritageProperty WHERE is_deleted = FALSE ORDER BY property_id ASC";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                properties.add(new HeritageProperty(rs.getInt("property_id"), rs.getString("property_name"), rs.getDouble("daily_rate"), rs.getString("current_status"), rs.getBoolean("is_deleted")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return properties;
    }

    public boolean updatePropertyStatus(int id, String newStatus) {
        String query = "UPDATE HeritageProperty SET current_status = ? WHERE property_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newStatus); pstmt.setInt(2, id); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean softDeleteProperty(int id) {
        String query = "UPDATE HeritageProperty SET is_deleted = TRUE WHERE property_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
}