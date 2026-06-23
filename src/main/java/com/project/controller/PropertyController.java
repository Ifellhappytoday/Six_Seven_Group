package com.project.controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.project.model.HeritageProperty;

public class PropertyController {

    private final String url = "jdbc:postgresql://ep-long-silence-ao4r46pb.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
    private final String user = "neondb_owner";
    private final String password = "npg_Cy0XQEZk2KSG";

    // ── The "Golden Hybrid" Method: Creates a folder safely in the project root ──
    private static java.io.File getUploadsDir() {
        String projectDir = System.getProperty("user.dir");
        java.io.File uploadFolder = new java.io.File(projectDir, "uploaded_images");
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
        }
        return uploadFolder;
    }

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(url, user, password);
        ensureImagePathColumn(conn);
        return conn;
    }

    private void ensureImagePathColumn(Connection conn) {
        String sql = "ALTER TABLE HeritageProperty ADD COLUMN IF NOT EXISTS image_path VARCHAR(255)";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            // Column may already exist on older PostgreSQL — ignore
        }
    }

    public List<HeritageProperty> getAllActiveProperties() {
        List<HeritageProperty> properties = new ArrayList<>();
        String query = "SELECT property_id, property_name, daily_rate, current_status, is_deleted, image_path "
                     + "FROM HeritageProperty WHERE is_deleted = FALSE ORDER BY property_id ASC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                properties.add(new HeritageProperty(
                        rs.getInt("property_id"),
                        rs.getString("property_name"),
                        rs.getDouble("daily_rate"),
                        rs.getString("current_status"),
                        rs.getBoolean("is_deleted"),
                        rs.getString("image_path"))); 
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return properties;
    }

    public boolean updatePropertyStatus(int id, String newStatus) {
        String query = "UPDATE HeritageProperty SET current_status = ? WHERE property_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean propertyNameExists(String name) {
        String query = "SELECT 1 FROM HeritageProperty WHERE LOWER(property_name) = LOWER(?) AND is_deleted = FALSE";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String addProperty(String name, double dailyRate, java.io.File imageFile) {
        if (propertyNameExists(name)) {
            return "NAME_TAKEN";
        }

        String storedFileName = null;
        if (imageFile != null && imageFile.exists()) {
            String ext = "";
            int dot = imageFile.getName().lastIndexOf('.');
            if (dot >= 0) {
                ext = imageFile.getName().substring(dot).toLowerCase();
            }
            // Generate a safe name with a timestamp to prevent overwriting files with the same name
            String safeName = name.replaceAll("[^a-zA-Z0-9]", "_") + "_" + System.currentTimeMillis() + ext;
            
            // Save exactly into the relative uploaded_images folder
            java.io.File dest = new java.io.File(getUploadsDir(), safeName);
            try {
                java.nio.file.Files.copy(imageFile.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                storedFileName = safeName;
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
            }
        }

        String query = "INSERT INTO HeritageProperty (property_name, daily_rate, current_status, image_path) "
                     + "VALUES (?, ?, 'Available', ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setDouble(2, dailyRate);
            ps.setString(3, storedFileName);
            return ps.executeUpdate() > 0 ? "SUCCESS" : "ERROR";
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    public boolean softDeleteProperty(int id) {
        String query = "UPDATE HeritageProperty SET is_deleted = TRUE WHERE property_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
}