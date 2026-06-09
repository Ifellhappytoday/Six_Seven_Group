package com.project.controller;

import com.project.model.Booking;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookingController {
    private final String url = "jdbc:postgresql://ep-long-silence-ao4r46pb.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
    private final String dbUser = "neondb_owner";
    private final String password = "npg_Cy0XQEZk2KSG"; // ⚠️ Fill in your password

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, dbUser, password);
    }

    // ─────────────────────────────────────────────
    //  GUEST SESSION HELPERS
    // ─────────────────────────────────────────────

    public int getGuestIdByEmail(String email) {
        String query = "SELECT guest_id FROM Guest WHERE email = ? AND is_deleted = FALSE";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("guest_id");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    // ─────────────────────────────────────────────
    //  PER-GUEST STATUS: "Occupied" only if THIS guest booked it for today/future
    // ─────────────────────────────────────────────

    /**
     * Returns the set of property IDs that the given guest has active (non-cancelled) bookings for.
     * Used to show "Occupied" only to the guest who made the booking.
     */
    public Set<Integer> getActivePropertyIdsForGuest(int guestId) {
        Set<Integer> ids = new HashSet<>();
        String query = "SELECT DISTINCT property_id FROM Booking " +
                       "WHERE guest_id = ? AND is_deleted = FALSE AND end_date >= CURRENT_DATE";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, guestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("property_id"));
        } catch (SQLException e) { e.printStackTrace(); }
        return ids;
    }

    // ─────────────────────────────────────────────
    //  BOOKINGS
    // ─────────────────────────────────────────────

    public List<Booking> getBookingsByGuest(int guestId) {
        List<Booking> bookings = new ArrayList<>();
        String query = "SELECT * FROM Booking WHERE guest_id = ? AND is_deleted = FALSE ORDER BY start_date DESC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, guestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) bookings.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return bookings;
    }

    public List<Booking> getAllActiveBookings() {
        List<Booking> bookings = new ArrayList<>();
        String query = "SELECT * FROM Booking WHERE is_deleted = FALSE ORDER BY start_date DESC";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) bookings.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return bookings;
    }

    /**
     * Creates a booking with overlap checks.
     * Returns: "SUCCESS", "CONFLICT_BOOKING", "CONFLICT_MAINTENANCE", "ERROR"
     */
    public String createBooking(int propertyId, int guestId, Date startDate, Date endDate, double dailyRate) {
        String overlapBooking = "SELECT COUNT(*) FROM Booking WHERE property_id = ? AND is_deleted = FALSE " +
                "AND NOT (end_date <= ? OR start_date >= ?)";
        String overlapMaint = "SELECT COUNT(*) FROM Maintenance WHERE property_id = ? AND is_deleted = FALSE " +
                "AND status NOT IN ('Completed','Cancelled') AND NOT (end_date <= ? OR start_date >= ?)";

        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(overlapBooking)) {
                ps.setInt(1, propertyId); ps.setDate(2, startDate); ps.setDate(3, endDate);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) return "CONFLICT_BOOKING";
            }
            try (PreparedStatement ps = conn.prepareStatement(overlapMaint)) {
                ps.setInt(1, propertyId); ps.setDate(2, startDate); ps.setDate(3, endDate);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) return "CONFLICT_MAINTENANCE";
            }

            long days = (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
            double totalPrice = days * dailyRate;

            String insertSQL = "INSERT INTO Booking (property_id, guest_id, start_date, end_date, total_price) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                ps.setInt(1, propertyId); ps.setInt(2, guestId);
                ps.setDate(3, startDate); ps.setDate(4, endDate);
                ps.setDouble(5, totalPrice);
                ps.executeUpdate();
            }

            // Only mark Occupied if booking starts today
            String updateStatus = "UPDATE HeritageProperty SET current_status = 'Occupied' " +
                    "WHERE property_id = ? AND ? <= CURRENT_DATE AND ? >= CURRENT_DATE";
            try (PreparedStatement ps = conn.prepareStatement(updateStatus)) {
                ps.setInt(1, propertyId); ps.setDate(2, startDate); ps.setDate(3, endDate);
                ps.executeUpdate();
            }

            return "SUCCESS";
        } catch (SQLException e) { e.printStackTrace(); return "ERROR"; }
    }

    public boolean cancelBooking(int bookingId) {
        String query = "UPDATE Booking SET is_deleted = TRUE WHERE booking_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public String getPropertyName(int propertyId) {
        String query = "SELECT property_name FROM HeritageProperty WHERE property_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, propertyId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("property_name");
        } catch (SQLException e) { e.printStackTrace(); }
        return "Unknown Property";
    }

    private Booking mapRow(ResultSet rs) throws SQLException {
        return new Booking(
            rs.getInt("booking_id"), rs.getInt("property_id"), rs.getInt("guest_id"),
            rs.getDate("start_date"), rs.getDate("end_date"),
            rs.getDouble("total_price"), rs.getBoolean("is_deleted")
        );
    }
}