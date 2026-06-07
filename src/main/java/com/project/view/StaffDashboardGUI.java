package com.project.view;

import com.project.controller.*;
import com.project.controller.ReportController.*;
import com.project.model.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.Date;
import java.util.Map;

public class StaffDashboardGUI extends JFrame {

    private final String userRole;
    private PropertyController propertyController = new PropertyController();
    private BookingController bookingController = new BookingController();
    private MaintenanceController maintController = new MaintenanceController();
    private ReportController reportController = new ReportController();

    // Property tab
    private JTable propertyTable;
    private DefaultTableModel propertyModel;

    // Bookings tab
    private JTable bookingTable;
    private DefaultTableModel bookingModel;

    // Maintenance tab
    private JTable maintTable;
    private DefaultTableModel maintModel;

    // Reports tab
    private JTable reportTable;
    private DefaultTableModel reportModel;
    private JLabel lblRevenue, lblMaintCost, lblActiveBookings, lblActiveMaint;

    public StaffDashboardGUI(String role) {
        this.userRole = role;
        buildUI();
    }

    private void buildUI() {
        setTitle("Smart Heritage Stay – " + userRole + " Dashboard");
        setSize(1000, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 240, 230));

        // ---- HEADER ----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(60, 30, 5));
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("Smart Heritage Stay  –  " + userRole + " Portal");
        title.setFont(new Font("Serif", Font.BOLD, 22));
        title.setForeground(new Color(255, 220, 140));
        header.add(title, BorderLayout.WEST);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(160, 55, 20));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });
        header.add(logoutBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ---- TABS ----
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("🏡  Properties", buildPropertiesTab());
        tabs.addTab("📅  Bookings", buildBookingsTab());
        tabs.addTab("🔧  Maintenance", buildMaintenanceTab());
        tabs.addTab("📊  Reports", buildReportsTab());
        add(tabs, BorderLayout.CENTER);

        setVisible(true);
        refreshAll();
    }

    // =====================================================================
    //  TAB 1 – PROPERTIES
    // =====================================================================
    private JPanel buildPropertiesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 240, 230));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        String[] cols = {"ID", "Property Name", "Daily Rate (RM)", "Status"};
        propertyModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        propertyTable = new JTable(propertyModel);
        propertyTable.setRowHeight(26);
        propertyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleTable(propertyTable);
        panel.add(new JScrollPane(propertyTable), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnRow.setBackground(new Color(245, 240, 230));

        JButton btnSetAvailable = new JButton("Set Available");
        JButton btnSetOccupied  = new JButton("Set Occupied");
        JButton btnSetMaint     = new JButton("Set Under Maintenance");
        JButton btnDelete       = new JButton("Delete Property");
        btnDelete.setBackground(new Color(180, 40, 20));
        btnDelete.setForeground(Color.WHITE);

        // Admin-only: delete
        btnDelete.addActionListener(e -> {
            if (!userRole.equalsIgnoreCase("Admin")) {
                permissionDenied(); return;
            }
            int row = propertyTable.getSelectedRow();
            if (row < 0) { noSelection(); return; }
            int id = (int) propertyModel.getValueAt(row, 0);
            if (confirm("Delete property ID " + id + "?")) {
                propertyController.softDeleteProperty(id);
                loadProperties();
            }
        });

        btnSetAvailable.addActionListener(e -> changePropertyStatus("Available"));
        btnSetOccupied.addActionListener(e -> changePropertyStatus("Occupied"));
        btnSetMaint.addActionListener(e -> changePropertyStatus("Under Maintenance"));

        btnRow.add(btnSetAvailable); btnRow.add(btnSetOccupied); btnRow.add(btnSetMaint);
        btnRow.add(Box.createHorizontalStrut(20)); btnRow.add(btnDelete);
        panel.add(btnRow, BorderLayout.SOUTH);
        return panel;
    }

    private void changePropertyStatus(String newStatus) {
        int row = propertyTable.getSelectedRow();
        if (row < 0) { noSelection(); return; }
        int id = (int) propertyModel.getValueAt(row, 0);
        propertyController.updatePropertyStatus(id, newStatus);
        loadProperties();
    }

    // =====================================================================
    //  TAB 2 – BOOKINGS (read-only for staff, cancel for Admin)
    // =====================================================================
    private JPanel buildBookingsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 240, 230));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        String[] cols = {"Booking ID", "Property", "Guest ID", "Check-in", "Check-out", "Total (RM)"};
        bookingModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        bookingTable = new JTable(bookingModel);
        bookingTable.setRowHeight(26);
        styleTable(bookingTable);
        panel.add(new JScrollPane(bookingTable), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnRow.setBackground(new Color(245, 240, 230));

        JButton btnCancel = new JButton("Cancel Booking (Admin Only)");
        btnCancel.setBackground(new Color(180, 40, 20));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.addActionListener(e -> {
            if (!userRole.equalsIgnoreCase("Admin")) { permissionDenied(); return; }
            int row = bookingTable.getSelectedRow();
            if (row < 0) { noSelection(); return; }
            int id = (int) bookingModel.getValueAt(row, 0);
            if (confirm("Cancel booking #" + id + "?")) {
                bookingController.cancelBooking(id);
                loadBookings();
                loadProperties();
            }
        });
        btnRow.add(btnCancel);
        panel.add(btnRow, BorderLayout.SOUTH);
        return panel;
    }

    // =====================================================================
    //  TAB 3 – MAINTENANCE SCHEDULER
    // =====================================================================
    private JPanel buildMaintenanceTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 240, 230));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        String[] cols = {"ID", "Property ID", "Type", "Description", "Start", "End", "Status", "Assigned To", "Est. Cost (RM)"};
        maintModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        maintTable = new JTable(maintModel);
        maintTable.setRowHeight(26);
        styleTable(maintTable);
        panel.add(new JScrollPane(maintTable), BorderLayout.CENTER);

        // ---- FORM to schedule new maintenance ----
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(240, 230, 210));
        form.setBorder(BorderFactory.createTitledBorder("Schedule New Maintenance / Preservation Work"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField fPropertyId   = new JTextField(6);
        JComboBox<String> fType  = new JComboBox<>(new String[]{
            "Structural Repair", "Timber Restoration", "Roof Repair",
            "Painting & Finishing", "Electrical", "Plumbing", "Foundation Work", "General Upkeep"
        });
        JTextField fDesc         = new JTextField(20);
        JTextField fStart        = new JTextField(10);
        JTextField fEnd          = new JTextField(10);
        JTextField fAssigned     = new JTextField(12);
        JTextField fCost         = new JTextField(8);

        addFormRow(form, gbc, 0, "Property ID:", fPropertyId);
        addFormRow(form, gbc, 1, "Type:", fType);
        addFormRow(form, gbc, 2, "Description:", fDesc);
        addFormRow(form, gbc, 3, "Start (YYYY-MM-DD):", fStart);
        addFormRow(form, gbc, 4, "End (YYYY-MM-DD):", fEnd);
        addFormRow(form, gbc, 5, "Assigned To:", fAssigned);
        addFormRow(form, gbc, 6, "Est. Cost (RM):", fCost);

        JButton btnSchedule = new JButton("  Schedule  ");
        btnSchedule.setBackground(new Color(101, 55, 0));
        btnSchedule.setForeground(Color.WHITE);
        btnSchedule.setFocusPainted(false);
        gbc.gridx = 1; gbc.gridy = 7;
        form.add(btnSchedule, gbc);

        btnSchedule.addActionListener(e -> {
            try {
                int propId   = Integer.parseInt(fPropertyId.getText().trim());
                String type  = fType.getSelectedItem().toString();
                String desc  = fDesc.getText().trim();
                Date start   = Date.valueOf(fStart.getText().trim());
                Date end     = Date.valueOf(fEnd.getText().trim());
                String assgn = fAssigned.getText().trim();
                double cost  = Double.parseDouble(fCost.getText().trim());

                if (!end.after(start)) {
                    JOptionPane.showMessageDialog(this, "End date must be after start date.", "Error", JOptionPane.ERROR_MESSAGE); return;
                }

                String result = maintController.scheduleMaintenance(propId, type, desc, start, end, assgn, cost);
                switch (result) {
                    case "SUCCESS" -> {
                        JOptionPane.showMessageDialog(this, "Maintenance scheduled successfully.\nProperty status updated to 'Under Maintenance'.",
                                "Scheduled", JOptionPane.INFORMATION_MESSAGE);
                        loadMaintenance();
                        loadProperties();
                        fPropertyId.setText(""); fDesc.setText(""); fStart.setText(""); fEnd.setText(""); fAssigned.setText(""); fCost.setText("");
                    }
                    case "CONFLICT_BOOKING" -> JOptionPane.showMessageDialog(this,
                            "⚠ A guest booking exists in this period!\nCancel or move the booking before scheduling maintenance.",
                            "Booking Conflict", JOptionPane.WARNING_MESSAGE);
                    case "CONFLICT_MAINTENANCE" -> JOptionPane.showMessageDialog(this,
                            "Another maintenance task overlaps these dates.", "Conflict", JOptionPane.WARNING_MESSAGE);
                    default -> JOptionPane.showMessageDialog(this, "Failed to schedule maintenance.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid number format.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Action buttons for selected maintenance row
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actionRow.setBackground(new Color(245, 240, 230));

        JButton btnInProgress  = new JButton("Mark In Progress");
        JButton btnCompleted   = new JButton("Mark Completed");
        JButton btnCancelled   = new JButton("Mark Cancelled");
        JButton btnDeleteMaint = new JButton("Delete");
        btnDeleteMaint.setBackground(new Color(180, 40, 20));
        btnDeleteMaint.setForeground(Color.WHITE);

        btnInProgress.addActionListener(e -> updateMaintStatus("In Progress"));
        btnCompleted.addActionListener(e  -> updateMaintStatus("Completed"));
        btnCancelled.addActionListener(e  -> updateMaintStatus("Cancelled"));
        btnDeleteMaint.addActionListener(e -> {
            int row = maintTable.getSelectedRow();
            if (row < 0) { noSelection(); return; }
            int id = (int) maintModel.getValueAt(row, 0);
            if (confirm("Delete maintenance record #" + id + "?")) {
                maintController.deleteMaintenance(id);
                loadMaintenance();
            }
        });

        actionRow.add(new JLabel("Selected:"));
        actionRow.add(btnInProgress); actionRow.add(btnCompleted); actionRow.add(btnCancelled);
        actionRow.add(Box.createHorizontalStrut(15)); actionRow.add(btnDeleteMaint);

        JPanel south = new JPanel(new BorderLayout());
        south.add(form, BorderLayout.CENTER);
        south.add(actionRow, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private void updateMaintStatus(String status) {
        int row = maintTable.getSelectedRow();
        if (row < 0) { noSelection(); return; }
        int id = (int) maintModel.getValueAt(row, 0);
        maintController.updateMaintenanceStatus(id, status);
        loadMaintenance();
        loadProperties();
    }

    // =====================================================================
    //  TAB 4 – REPORTS
    // =====================================================================
    private JPanel buildReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 240, 230));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        // ---- Summary cards ----
        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
        cards.setBackground(new Color(245, 240, 230));
        cards.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        lblRevenue       = makeSummaryCard("Total Revenue", "RM 0.00", new Color(34, 100, 50));
        lblMaintCost     = makeSummaryCard("Maintenance Cost", "RM 0.00", new Color(160, 80, 20));
        lblActiveBookings = makeSummaryCard("Active Bookings", "0", new Color(20, 80, 160));
        lblActiveMaint   = makeSummaryCard("Active Maintenance", "0", new Color(120, 20, 120));

        cards.add(lblRevenue.getParent()); cards.add(lblMaintCost.getParent());
        cards.add(lblActiveBookings.getParent()); cards.add(lblActiveMaint.getParent());
        panel.add(cards, BorderLayout.NORTH);

        // ---- Per-property table ----
        String[] cols = {"Property", "Rental Days", "Maintenance Days", "Occupancy %", "Revenue (RM)", "Maint. Cost (RM)", "Heritage Health"};
        reportModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        reportTable = new JTable(reportModel);
        reportTable.setRowHeight(26);
        styleTable(reportTable);

        // Colour the Heritage Health column
        reportTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                String v = val == null ? "" : val.toString();
                setForeground(switch (v) {
                    case "Good"           -> new Color(0, 128, 0);
                    case "Needs Attention"-> new Color(180, 100, 0);
                    case "At Risk"        -> new Color(180, 0, 0);
                    default               -> Color.DARK_GRAY;
                });
                setFont(getFont().deriveFont(Font.BOLD));
                return this;
            }
        });

        panel.add(new JScrollPane(reportTable), BorderLayout.CENTER);

        // ---- Date range filter ----
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterRow.setBackground(new Color(245, 240, 230));

        filterRow.add(new JLabel("From (YYYY-MM-DD):"));
        JTextField fromField = new JTextField(10);
        filterRow.add(fromField);
        filterRow.add(new JLabel("To (YYYY-MM-DD):"));
        JTextField toField = new JTextField(10);
        filterRow.add(toField);

        JButton btnGenerate = new JButton("Generate Report");
        btnGenerate.setBackground(new Color(101, 55, 0));
        btnGenerate.setForeground(Color.WHITE);
        btnGenerate.setFocusPainted(false);
        btnGenerate.addActionListener(e -> {
            try {
                Date from = Date.valueOf(fromField.getText().trim());
                Date to   = Date.valueOf(toField.getText().trim());
                if (!to.after(from)) {
                    JOptionPane.showMessageDialog(this, "To date must be after From date.", "Error", JOptionPane.ERROR_MESSAGE); return;
                }
                loadReport(from, to);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        filterRow.add(btnGenerate);

        panel.add(filterRow, BorderLayout.SOUTH);
        return panel;
    }

    // =====================================================================
    //  DATA LOADERS
    // =====================================================================
    private void refreshAll() {
        loadProperties(); loadBookings(); loadMaintenance(); loadSummaryCards();
    }

    private void loadProperties() {
        propertyModel.setRowCount(0);
        for (HeritageProperty p : propertyController.getAllActiveProperties()) {
            propertyModel.addRow(new Object[]{
                p.getPropertyId(), p.getPropertyName(),
                String.format("%.2f", p.getDailyRate()), p.getCurrentStatus()
            });
        }
    }

    private void loadBookings() {
        bookingModel.setRowCount(0);
        for (Booking b : bookingController.getAllActiveBookings()) {
            bookingModel.addRow(new Object[]{
                b.getBookingId(),
                bookingController.getPropertyName(b.getPropertyId()),
                b.getGuestId(), b.getStartDate(), b.getEndDate(),
                String.format("%.2f", b.getTotalPrice())
            });
        }
    }

    private void loadMaintenance() {
        maintModel.setRowCount(0);
        for (Maintenance m : maintController.getAllActiveMaintenance()) {
            maintModel.addRow(new Object[]{
                m.getMaintenanceId(), m.getPropertyId(), m.getMaintenanceType(),
                m.getDescription(), m.getStartDate(), m.getEndDate(),
                m.getStatus(), m.getAssignedTo(),
                String.format("%.2f", m.getEstimatedCost())
            });
        }
    }

    private void loadSummaryCards() {
        Map<String, Object> stats = reportController.getSummaryStats();
        lblRevenue.setText("RM " + String.format("%,.2f", stats.getOrDefault("totalRevenue", 0.0)));
        lblMaintCost.setText("RM " + String.format("%,.2f", stats.getOrDefault("totalMaintenanceCost", 0.0)));
        lblActiveBookings.setText(String.valueOf(stats.getOrDefault("activeBookings", 0)));
        lblActiveMaint.setText(String.valueOf(stats.getOrDefault("activeMaintenance", 0)));
    }

    private void loadReport(Date from, Date to) {
        reportModel.setRowCount(0);
        for (PropertyReport r : reportController.getOccupancyReport(from, to)) {
            reportModel.addRow(new Object[]{
                r.propertyName, r.rentalDays, r.maintenanceDays,
                String.format("%.1f%%", r.occupancyRate),
                String.format("%.2f", r.totalRevenue),
                String.format("%.2f", r.totalMaintenanceCost),
                r.healthStatus
            });
        }
        loadSummaryCards();
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================
    private JLabel makeSummaryCard(String labelText, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, 2),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Serif", Font.PLAIN, 12));
        lbl.setForeground(Color.GRAY);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Serif", Font.BOLD, 18));
        val.setForeground(accent);
        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        return val; // return value label for later updates
    }

    private void addFormRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(field, gbc);
    }

    private void styleTable(JTable table) {
        table.getTableHeader().setBackground(new Color(101, 55, 0));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Serif", Font.BOLD, 13));
        table.setGridColor(new Color(200, 185, 160));
        table.setSelectionBackground(new Color(200, 160, 100));
    }

    private void permissionDenied() {
        JOptionPane.showMessageDialog(this, "Permission Denied: Only Admins can perform this action.", "Access Restricted", JOptionPane.WARNING_MESSAGE);
    }

    private void noSelection() {
        JOptionPane.showMessageDialog(this, "Please select a row first.", "No Selection", JOptionPane.WARNING_MESSAGE);
    }

    private boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(this, msg, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}