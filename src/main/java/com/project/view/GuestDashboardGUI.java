package com.project.view;

import com.project.controller.BookingController;
import com.project.controller.PropertyController;
import com.project.model.Booking;
import com.project.model.HeritageProperty;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class GuestDashboardGUI extends JFrame {

    private int guestId;

    private PropertyController propertyController = new PropertyController();
    private BookingController bookingController = new BookingController();

    private JTable propertyTable;
    private JTable bookingTable;
    private DefaultTableModel propertyModel;
    private DefaultTableModel bookingModel;
    private JTabbedPane tabbedPane;

    public GuestDashboardGUI(int guestId, String guestEmail) {
        this.guestId = guestId;
        buildUI();
    }

    // Overloaded: resolve guestId from email (called from LoginFrame)
    public GuestDashboardGUI(String guestEmail) {
        this.guestId = new BookingController().getGuestIdByEmail(guestEmail);
        buildUI();
    }

    // No-arg constructor kept for compatibility
    public GuestDashboardGUI() {
        this.guestId = -1;
        buildUI();
    }

    private void buildUI() {
        setTitle("Smart Heritage Stay – Guest Portal");
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 240, 230));

        // ---- HEADER ----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(101, 55, 0));
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("Smart Heritage Stay – Guest Portal");
        title.setFont(new Font("Serif", Font.BOLD, 20));
        title.setForeground(new Color(255, 235, 180));
        header.add(title, BorderLayout.WEST);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(180, 80, 30));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });
        header.add(logoutBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ---- TABS ----
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(245, 240, 230));
        tabbedPane.addTab("🏡  Browse Properties", buildPropertyTab());
        tabbedPane.addTab("📋  My Bookings", buildBookingsTab());
        add(tabbedPane, BorderLayout.CENTER);

        setVisible(true);
        loadProperties();
        loadMyBookings();
    }

    // ===================== TAB 1: Browse & Book =====================
    private JPanel buildPropertyTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 240, 230));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Property Name", "Daily Rate (RM)", "Status"};
        propertyModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        propertyTable = new JTable(propertyModel);
        propertyTable.setRowHeight(26);
        propertyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleTable(propertyTable);

        // Colour the Status column
        propertyTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                String status = val == null ? "" : val.toString();
                setForeground(switch (status) {
                    case "Available" -> new Color(0, 128, 0);
                    case "Occupied" -> new Color(180, 0, 0);
                    case "Under Maintenance" -> new Color(180, 100, 0);
                    default -> Color.DARK_GRAY;
                });
                return this;
            }
        });

        panel.add(new JScrollPane(propertyTable), BorderLayout.CENTER);

        // Book panel
        JPanel bookPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        bookPanel.setBackground(new Color(245, 240, 230));
        bookPanel.setBorder(BorderFactory.createTitledBorder("Book Selected Property"));

        bookPanel.add(new JLabel("Check-in (YYYY-MM-DD):"));
        JTextField checkIn = new JTextField(10);
        bookPanel.add(checkIn);

        bookPanel.add(new JLabel("Check-out (YYYY-MM-DD):"));
        JTextField checkOut = new JTextField(10);
        bookPanel.add(checkOut);

        JButton bookBtn = new JButton("  Book Now  ");
        bookBtn.setBackground(new Color(101, 55, 0));
        bookBtn.setForeground(Color.WHITE);
        bookBtn.setFocusPainted(false);
        bookPanel.add(bookBtn);

        bookBtn.addActionListener(e -> handleBooking(checkIn.getText().trim(), checkOut.getText().trim()));

        panel.add(bookPanel, BorderLayout.SOUTH);
        return panel;
    }

    // ===================== TAB 2: My Bookings =====================
    private JPanel buildBookingsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 240, 230));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"Booking ID", "Property", "Check-in", "Check-out", "Total (RM)"};
        bookingModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        bookingTable = new JTable(bookingModel);
        bookingTable.setRowHeight(26);
        styleTable(bookingTable);
        panel.add(new JScrollPane(bookingTable), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRow.setBackground(new Color(245, 240, 230));

        JButton cancelBtn = new JButton("Cancel Selected Booking");
        cancelBtn.setBackground(new Color(180, 50, 30));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> cancelSelectedBooking());

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadMyBookings());

        btnRow.add(cancelBtn);
        btnRow.add(refreshBtn);
        panel.add(btnRow, BorderLayout.SOUTH);
        return panel;
    }

    // ===================== DATA LOADING =====================
    private void loadProperties() {
        propertyModel.setRowCount(0);
        for (HeritageProperty p : propertyController.getAllActiveProperties()) {
            propertyModel.addRow(new Object[]{
                p.getPropertyId(),
                p.getPropertyName(),
                String.format("%.2f", p.getDailyRate()),
                p.getCurrentStatus()
            });
        }
    }

    private void loadMyBookings() {
        bookingModel.setRowCount(0);
        if (guestId < 0) return;
        for (Booking b : bookingController.getBookingsByGuest(guestId)) {
            bookingModel.addRow(new Object[]{
                b.getBookingId(),
                bookingController.getPropertyName(b.getPropertyId()),
                b.getStartDate(),
                b.getEndDate(),
                String.format("%.2f", b.getTotalPrice())
            });
        }
    }

    // ===================== ACTIONS =====================
    private void handleBooking(String checkInStr, String checkOutStr) {
        int selectedRow = propertyTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a property first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (guestId < 0) {
            JOptionPane.showMessageDialog(this, "Session error – please log in again.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String status = propertyModel.getValueAt(selectedRow, 3).toString();
        if (!status.equals("Available")) {
            JOptionPane.showMessageDialog(this, "This property is currently not available for booking.\nStatus: " + status,
                    "Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            LocalDate ci = LocalDate.parse(checkInStr);
            LocalDate co = LocalDate.parse(checkOutStr);
            if (!co.isAfter(ci)) {
                JOptionPane.showMessageDialog(this, "Check-out must be after check-in.", "Date Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (ci.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "Check-in cannot be in the past.", "Date Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int propertyId = (int) propertyModel.getValueAt(selectedRow, 0);
            double dailyRate = Double.parseDouble(propertyModel.getValueAt(selectedRow, 2).toString());
            long days = ChronoUnit.DAYS.between(ci, co);
            double total = days * dailyRate;

            int confirm = JOptionPane.showConfirmDialog(this,
                    String.format("Booking Summary:\n• Property: %s\n• Check-in: %s\n• Check-out: %s\n• Duration: %d night(s)\n• Total: RM %.2f\n\nConfirm booking?",
                            propertyModel.getValueAt(selectedRow, 1), ci, co, days, total),
                    "Confirm Booking", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                String result = bookingController.createBooking(propertyId, guestId,
                        Date.valueOf(ci), Date.valueOf(co), dailyRate);
                switch (result) {
                    case "SUCCESS" -> {
                        JOptionPane.showMessageDialog(this, "Booking confirmed! Enjoy your heritage stay.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadProperties();
                        loadMyBookings();
                        tabbedPane.setSelectedIndex(1);
                    }
                    case "CONFLICT_BOOKING" -> JOptionPane.showMessageDialog(this,
                            "These dates overlap with an existing booking. Please choose different dates.",
                            "Date Conflict", JOptionPane.WARNING_MESSAGE);
                    case "CONFLICT_MAINTENANCE" -> JOptionPane.showMessageDialog(this,
                            "These dates overlap with a scheduled maintenance period.\nThe property will be closed for preservation work.",
                            "Maintenance Conflict", JOptionPane.WARNING_MESSAGE);
                    default -> JOptionPane.showMessageDialog(this, "Booking failed. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.", "Format Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelSelectedBooking() {
        int selectedRow = bookingTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a booking to cancel.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int bookingId = (int) bookingModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Cancel booking #" + bookingId + "?",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (bookingController.cancelBooking(bookingId)) {
                JOptionPane.showMessageDialog(this, "Booking cancelled successfully.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                loadMyBookings();
                loadProperties();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to cancel booking.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void styleTable(JTable table) {
        table.getTableHeader().setBackground(new Color(101, 55, 0));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Serif", Font.BOLD, 13));
        table.setGridColor(new Color(200, 185, 160));
        table.setSelectionBackground(new Color(200, 160, 100));
    }
}