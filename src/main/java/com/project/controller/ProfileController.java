package com.project.controller;

import java.sql.*;

public class ProfileController {
    private final String url = "jdbc:postgresql://ep-long-silence-ao4r46pb.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
    private final String dbUser = "neondb_owner";
    private final String password = "npg_Cy0XQEZk2KSG"; // ⚠️ Fill in your password

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, dbUser, password);
    }

    // ─────────────────────────────────────────────
    //  GUEST PROFILE
    // ─────────────────────────────────────────────

    /** Returns {full_name, email, contact_number} for a guest */
    public String[] getGuestProfile(int guestId) {
        String query = "SELECT full_name, email, contact_number FROM Guest WHERE guest_id = ? AND is_deleted = FALSE";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, guestId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new String[]{
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("contact_number") == null ? "" : rs.getString("contact_number")
                };
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Updates guest name, email, and contact. Returns "SUCCESS", "EMAIL_TAKEN", or "ERROR" */
    public String updateGuestProfile(int guestId, String fullName, String email, String contact) {
        // Check if email is taken by another guest
        String checkEmail = "SELECT guest_id FROM Guest WHERE email = ? AND guest_id != ? AND is_deleted = FALSE";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(checkEmail)) {
            ps.setString(1, email);
            ps.setInt(2, guestId);
            if (ps.executeQuery().next()) return "EMAIL_TAKEN";
        } catch (SQLException e) { e.printStackTrace(); return "ERROR"; }

        String update = "UPDATE Guest SET full_name = ?, email = ?, contact_number = ? WHERE guest_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, contact);
            ps.setInt(4, guestId);
            return ps.executeUpdate() > 0 ? "SUCCESS" : "ERROR";
        } catch (SQLException e) { e.printStackTrace(); return "ERROR"; }
    }

    /** Changes guest password. Returns "SUCCESS", "WRONG_PASSWORD", or "ERROR" */
    public String changeGuestPassword(int guestId, String currentPass, String newPass) {
        String verify = "SELECT guest_id FROM Guest WHERE guest_id = ? AND password = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(verify)) {
            ps.setInt(1, guestId);
            ps.setString(2, currentPass);
            if (!ps.executeQuery().next()) return "WRONG_PASSWORD";
        } catch (SQLException e) { e.printStackTrace(); return "ERROR"; }

        String update = "UPDATE Guest SET password = ? WHERE guest_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setString(1, newPass);
            ps.setInt(2, guestId);
            return ps.executeUpdate() > 0 ? "SUCCESS" : "ERROR";
        } catch (SQLException e) { e.printStackTrace(); return "ERROR"; }
    }

    // ─────────────────────────────────────────────
    //  STAFF PROFILE
    // ─────────────────────────────────────────────

    /** Returns {full_name, email, contact_number, job_role} for a staff member */
    public String[] getStaffProfile(String email) {
        String query = "SELECT staff_id, full_name, email, contact_number, job_role FROM Staff WHERE email = ? AND is_deleted = FALSE";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new String[]{
                    String.valueOf(rs.getInt("staff_id")),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("contact_number") == null ? "" : rs.getString("contact_number"),
                    rs.getString("job_role")
                };
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /** Updates staff name, email, and contact. Returns "SUCCESS", "EMAIL_TAKEN", or "ERROR" */
    public String updateStaffProfile(int staffId, String fullName, String email, String contact) {
        String checkEmail = "SELECT staff_id FROM Staff WHERE email = ? AND staff_id != ? AND is_deleted = FALSE";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(checkEmail)) {
            ps.setString(1, email);
            ps.setInt(2, staffId);
            if (ps.executeQuery().next()) return "EMAIL_TAKEN";
        } catch (SQLException e) { e.printStackTrace(); return "ERROR"; }

        String update = "UPDATE Staff SET full_name = ?, email = ?, contact_number = ? WHERE staff_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, contact);
            ps.setInt(4, staffId);
            return ps.executeUpdate() > 0 ? "SUCCESS" : "ERROR";
        } catch (SQLException e) { e.printStackTrace(); return "ERROR"; }
    }

    /** Changes staff password. Returns "SUCCESS", "WRONG_PASSWORD", or "ERROR" */
    public String changeStaffPassword(int staffId, String currentPass, String newPass) {
        String verify = "SELECT staff_id FROM Staff WHERE staff_id = ? AND password = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(verify)) {
            ps.setInt(1, staffId);
            ps.setString(2, currentPass);
            if (!ps.executeQuery().next()) return "WRONG_PASSWORD";
        } catch (SQLException e) { e.printStackTrace(); return "ERROR"; }

        String update = "UPDATE Staff SET password = ? WHERE staff_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setString(1, newPass);
            ps.setInt(2, staffId);
            return ps.executeUpdate() > 0 ? "SUCCESS" : "ERROR";
        } catch (SQLException e) { e.printStackTrace(); return "ERROR"; }
    }

    // ─────────────────────────────────────────────
    //  LOOKUP HELPERS (used by staff booking view)
    // ─────────────────────────────────────────────

    /** Returns {full_name, email} for a guest ID */
    public String[] getGuestNameAndEmail(int guestId) {
        String query = "SELECT full_name, email FROM Guest WHERE guest_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, guestId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new String[]{rs.getString("full_name"), rs.getString("email")};
        } catch (SQLException e) { e.printStackTrace(); }
        return new String[]{"Unknown", ""};
    }
}