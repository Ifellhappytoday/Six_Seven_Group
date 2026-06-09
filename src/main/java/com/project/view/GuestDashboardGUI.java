package com.project.view;

import com.project.controller.BookingController;
import com.project.controller.ProfileController;
import com.project.controller.MaintenanceController;
import com.project.controller.PropertyController;
import com.project.model.Booking;
import com.project.model.HeritageProperty;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class GuestDashboardGUI extends JFrame {

    private int guestId;
    private String guestEmail;

    private PropertyController propertyController = new PropertyController();
    private BookingController bookingController   = new BookingController();
    private ProfileController profileController   = new ProfileController();
    private MaintenanceController maintenanceController = new MaintenanceController();

    private JTable propertyTable;
    private JTable bookingTable;
    private DefaultTableModel propertyModel;
    private DefaultTableModel bookingModel;
    private JTabbedPane tabbedPane;

    // Calendar fields
    private CalendarPanel checkInCalendar;
    private CalendarPanel checkOutCalendar;
    private JLabel checkInLabel;
    private JLabel checkOutLabel;
    private LocalDate selectedCheckIn  = null;
    private LocalDate selectedCheckOut = null;

    // Booked dates for selected property (from DB)
    private Set<LocalDate> bookedDates = new HashSet<>();
    private Set<LocalDate> maintenanceDates = new HashSet<>();

    // Property IDs this guest personally booked (for per-guest status display)
    private Set<Integer> myBookedPropertyIds = new HashSet<>();

    // ── Constructors ──────────────────────────────────────────────────────────
    public GuestDashboardGUI(int guestId, String guestEmail) {
        this.guestId    = guestId;
        this.guestEmail = guestEmail;
        buildUI();
    }

    public GuestDashboardGUI(String guestEmail) {
        this.guestEmail = guestEmail;
        this.guestId    = bookingController.getGuestIdByEmail(guestEmail);
        buildUI();
    }

    public GuestDashboardGUI() {
        this.guestId    = -1;
        this.guestEmail = "";
        buildUI();
    }

    // ── Main UI ───────────────────────────────────────────────────────────────
    private void buildUI() {
        setTitle("Smart Heritage Stay – Guest Portal");
        setSize(980, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 240, 230));

        // Header
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
        logoutBtn.addActionListener(e -> { dispose(); new LoginFrame(); });
        header.add(logoutBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(245, 240, 230));
        tabbedPane.addTab("🏡  Browse Properties", buildPropertyTab());
        tabbedPane.addTab("📋  My Bookings",        buildBookingsTab());
        tabbedPane.addTab("👤  My Profile",          buildProfileTab());
        add(tabbedPane, BorderLayout.CENTER);

        setVisible(true);
        refreshMyBookedProperties();
        loadProperties();
        loadMyBookings();
    }

    // ── Tab 1: Browse & Book ──────────────────────────────────────────────────
    private JPanel buildPropertyTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(245, 240, 230));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] cols = {"ID", "Property Name", "Daily Rate (RM)", "Status"};
        propertyModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        propertyTable = new JTable(propertyModel);
        propertyTable.setRowHeight(26);
        propertyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleTable(propertyTable);
        
        // Hide the Status column but keep it for color rendering
        propertyTable.getColumnModel().getColumn(3).setMaxWidth(0);
        propertyTable.getColumnModel().getColumn(3).setMinWidth(0);
        propertyTable.getColumnModel().getColumn(3).setPreferredWidth(0);
        propertyTable.getTableHeader().getColumnModel().getColumn(3).setMaxWidth(0);
        
        // Color renderer for all cells based on Status column
        DefaultTableCellRenderer colorRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel,
                                                           boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                String status = (String) propertyModel.getValueAt(r, 3);
                if (!sel) {
                    // "Booked by You" shows the same as Available — no red highlight
                    setBackground(switch (status) {
                        case "Under Maintenance" -> new Color(255, 200, 100);  // Light orange
                        default -> Color.WHITE;
                    });
                    setForeground(switch (status) {
                        case "Under Maintenance" -> new Color(180, 100, 0);
                        default -> Color.BLACK;
                    });
                }
                return this;
            }
        };
        propertyTable.getColumnModel().getColumn(0).setCellRenderer(colorRenderer);
        propertyTable.getColumnModel().getColumn(1).setCellRenderer(colorRenderer);
        propertyTable.getColumnModel().getColumn(2).setCellRenderer(colorRenderer);

        // On row select, load booked dates into the calendar
        propertyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = propertyTable.getSelectedRow();
                if (row >= 0) {
                    int propId = (int) propertyModel.getValueAt(row, 0);
                    loadBookedDates(propId);
                    selectedCheckIn  = null;
                    selectedCheckOut = null;
                    updateDateLabels();
                    checkInCalendar.refresh();
                    checkOutCalendar.refresh();
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(propertyTable);
        tableScroll.setPreferredSize(new Dimension(0, 160));

        // Calendars
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBackground(new Color(245, 240, 230));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Select Dates – Click a property above first"));

        JPanel calendarsRow = new JPanel(new GridLayout(1, 2, 10, 0));
        calendarsRow.setBackground(new Color(245, 240, 230));

        JPanel checkInPanel = new JPanel(new BorderLayout(0, 4));
        checkInPanel.setBackground(new Color(245, 240, 230));
        checkInLabel = new JLabel("Check-in: Not selected", SwingConstants.CENTER);
        checkInLabel.setFont(new Font("Serif", Font.BOLD, 13));
        checkInLabel.setForeground(new Color(101, 55, 0));
        checkInCalendar = new CalendarPanel(true);
        checkInPanel.add(checkInLabel, BorderLayout.NORTH);
        checkInPanel.add(checkInCalendar, BorderLayout.CENTER);

        JPanel checkOutPanel = new JPanel(new BorderLayout(0, 4));
        checkOutPanel.setBackground(new Color(245, 240, 230));
        checkOutLabel = new JLabel("Check-out: Not selected", SwingConstants.CENTER);
        checkOutLabel.setFont(new Font("Serif", Font.BOLD, 13));
        checkOutLabel.setForeground(new Color(101, 55, 0));
        checkOutCalendar = new CalendarPanel(false);
        checkOutPanel.add(checkOutLabel, BorderLayout.NORTH);
        checkOutPanel.add(checkOutCalendar, BorderLayout.CENTER);

        calendarsRow.add(checkInPanel);
        calendarsRow.add(checkOutPanel);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));
        legend.setBackground(new Color(245, 240, 230));
        legend.add(legendItem(new Color(220, 80, 60),  "Already booked"));
        legend.add(legendItem(new Color(180, 100, 0),  "Maintenance"));
        legend.add(legendItem(new Color(34, 139, 34),  "Check-in"));
        legend.add(legendItem(new Color(60, 140, 200), "Check-out"));
        legend.add(legendItem(new Color(180, 220, 180),"Selected range"));

        JButton bookBtn = new JButton("  Book Now  ");
        bookBtn.setBackground(new Color(101, 55, 0));
        bookBtn.setForeground(Color.WHITE);
        bookBtn.setFont(new Font("Serif", Font.BOLD, 14));
        bookBtn.setFocusPainted(false);
        bookBtn.setPreferredSize(new Dimension(140, 36));
        bookBtn.addActionListener(e -> handleBooking());

        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(new Color(245, 240, 230));
        bottomBar.add(legend, BorderLayout.WEST);
        bottomBar.add(bookBtn, BorderLayout.EAST);

        bottomPanel.add(calendarsRow, BorderLayout.CENTER);
        bottomPanel.add(bottomBar, BorderLayout.SOUTH);

        panel.add(tableScroll, BorderLayout.NORTH);
        panel.add(bottomPanel, BorderLayout.CENTER);
        return panel;
    }

    // ── Tab 2: My Bookings ────────────────────────────────────────────────────
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

    // ── Tab 3: My Profile ────────────────────────────────────────────────────
    private JPanel buildProfileTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(new Color(245, 240, 230));
        outer.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 185, 160), 1),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));

        // ── Personal Information ──
        JLabel secInfo = sectionLabel("Personal Information");
        card.add(secInfo);
        card.add(Box.createVerticalStrut(12));

        JTextField fName    = formField();
        JTextField fEmail   = formField();
        JTextField fContact = formField();

        card.add(formRow("Full Name:", fName));
        card.add(Box.createVerticalStrut(8));
        card.add(formRow("Email:", fEmail));
        card.add(Box.createVerticalStrut(8));
        card.add(formRow("Contact Number:", fContact));
        card.add(Box.createVerticalStrut(16));

        JButton saveInfoBtn = styledButton("Save Changes", new Color(101, 55, 0));
        saveInfoBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(saveInfoBtn);

        card.add(Box.createVerticalStrut(28));
        card.add(separator());
        card.add(Box.createVerticalStrut(20));

        // ── Change Password ──
        JLabel secPass = sectionLabel("Change Password");
        card.add(secPass);
        card.add(Box.createVerticalStrut(12));

        JPasswordField fCurrent = passField();
        JPasswordField fNew     = passField();
        JPasswordField fConfirm = passField();

        card.add(formRow("Current Password:", fCurrent));
        card.add(Box.createVerticalStrut(8));
        card.add(formRow("New Password:", fNew));
        card.add(Box.createVerticalStrut(8));
        card.add(formRow("Confirm New Password:", fConfirm));
        card.add(Box.createVerticalStrut(16));

        JButton savePassBtn = styledButton("Change Password", new Color(101, 55, 0));
        savePassBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(savePassBtn);

        outer.add(card, BorderLayout.CENTER);

        // Load profile data when tab is opened
        tabbedPane = tabbedPane; // forward reference — wired after tab add
        // We populate lazily via a listener added after tabbedPane is built
        // Use a flag to load once
        outer.putClientProperty("profileFields", new Object[]{fName, fEmail, fContact});

        // ── Save info action ──
        saveInfoBtn.addActionListener(e -> {
            String name    = fName.getText().trim();
            String email   = fEmail.getText().trim();
            String contact = fContact.getText().trim();

            if (name.isEmpty() || email.isEmpty()) {
                error("Name and email cannot be empty."); return;
            }
            if (!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$", email)) {
                error("Invalid email format."); return;
            }

            String result = profileController.updateGuestProfile(guestId, name, email, contact);
            switch (result) {
                case "SUCCESS" -> {
                    this.guestEmail = email;
                    info("Profile updated successfully.");
                }
                case "EMAIL_TAKEN" -> error("This email is already in use by another account.");
                default -> error("Failed to update profile. Please try again.");
            }
        });

        // ── Save password action ──
        savePassBtn.addActionListener(e -> {
            String current = new String(fCurrent.getPassword());
            String newPass = new String(fNew.getPassword());
            String confirm = new String(fConfirm.getPassword());

            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                error("Please fill all password fields."); return;
            }
            if (newPass.length() < 6) {
                error("New password must be at least 6 characters."); return;
            }
            if (!newPass.equals(confirm)) {
                error("New passwords do not match."); return;
            }

            String result = profileController.changeGuestPassword(guestId, current, newPass);
            switch (result) {
                case "SUCCESS"        -> { info("Password changed successfully."); fCurrent.setText(""); fNew.setText(""); fConfirm.setText(""); }
                case "WRONG_PASSWORD" -> error("Current password is incorrect.");
                default               -> error("Failed to change password. Please try again.");
            }
        });

        // Populate fields on tab switch
        outer.addAncestorListener(new javax.swing.event.AncestorListener() {
            boolean loaded = false;
            public void ancestorAdded(javax.swing.event.AncestorEvent e) {
                if (!loaded && guestId > 0) {
                    String[] profile = profileController.getGuestProfile(guestId);
                    if (profile != null) {
                        fName.setText(profile[0]);
                        fEmail.setText(profile[1]);
                        fContact.setText(profile[2]);
                    }
                    loaded = true;
                }
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent e) {}
            public void ancestorMoved(javax.swing.event.AncestorEvent e) {}
        });

        return outer;
    }

    // ── Calendar Inner Class ──────────────────────────────────────────────────
    class CalendarPanel extends JPanel {
        private boolean isCheckIn;
        private YearMonth displayMonth;
        private JLabel monthLabel;
        private JPanel daysGrid;

        CalendarPanel(boolean isCheckIn) {
            this.isCheckIn    = isCheckIn;
            this.displayMonth = YearMonth.now();
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(new Color(200, 185, 160)));
            setLayout(new BorderLayout(0, 0));

            JPanel nav = new JPanel(new BorderLayout());
            nav.setBackground(new Color(101, 55, 0));
            nav.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

            JButton prev = navBtn("‹");
            JButton next = navBtn("›");
            monthLabel = new JLabel("", SwingConstants.CENTER);
            monthLabel.setForeground(Color.WHITE);
            monthLabel.setFont(new Font("Serif", Font.BOLD, 13));

            prev.addActionListener(e -> { displayMonth = displayMonth.minusMonths(1); refresh(); });
            next.addActionListener(e -> { displayMonth = displayMonth.plusMonths(1); refresh(); });

            nav.add(prev, BorderLayout.WEST);
            nav.add(monthLabel, BorderLayout.CENTER);
            nav.add(next, BorderLayout.EAST);
            add(nav, BorderLayout.NORTH);

            JPanel headers = new JPanel(new GridLayout(1, 7));
            headers.setBackground(new Color(220, 200, 170));
            for (String d : new String[]{"Sun","Mon","Tue","Wed","Thu","Fri","Sat"}) {
                JLabel lbl = new JLabel(d, SwingConstants.CENTER);
                lbl.setFont(new Font("Serif", Font.BOLD, 11));
                lbl.setForeground(new Color(80, 40, 0));
                headers.add(lbl);
            }
            add(headers, BorderLayout.CENTER);

            daysGrid = new JPanel(new GridLayout(0, 7, 1, 1));
            daysGrid.setBackground(new Color(245, 240, 230));
            add(daysGrid, BorderLayout.SOUTH);
            refresh();
        }

        private JButton navBtn(String text) {
            JButton b = new JButton(text);
            b.setBackground(new Color(101, 55, 0));
            b.setForeground(Color.WHITE);
            b.setBorderPainted(true);
            b.setFocusPainted(false);
            b.setFont(new Font("Serif", Font.BOLD, 18));
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setPreferredSize(new Dimension(38, 26));
            b.setBorder(new javax.swing.border.LineBorder(new Color(200, 150, 80), 1, true));
            return b;
        }

        void refresh() {
            monthLabel.setText(displayMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")));
            daysGrid.removeAll();

            LocalDate first   = displayMonth.atDay(1);
            int startDow      = first.getDayOfWeek().getValue() % 7;
            LocalDate today   = LocalDate.now();

            for (int i = 0; i < startDow; i++) daysGrid.add(new JLabel(""));

            for (int day = 1; day <= displayMonth.lengthOfMonth(); day++) {
                LocalDate date = displayMonth.atDay(day);
                JButton btn    = new JButton(String.valueOf(day));
                btn.setFont(new Font("Serif", Font.PLAIN, 12));
                btn.setFocusPainted(false);
                btn.setBorderPainted(true);
                btn.setBorder(BorderFactory.createLineBorder(new Color(220, 210, 195), 1));
                btn.setMargin(new Insets(1, 1, 1, 1));

                boolean isPast        = date.isBefore(today);
                boolean isBooked      = bookedDates.contains(date);
                boolean isMaintenance = maintenanceDates.contains(date);
                boolean isCIDay       = date.equals(selectedCheckIn);
                boolean isCODay       = date.equals(selectedCheckOut);
                boolean inRange       = selectedCheckIn != null && selectedCheckOut != null
                                        && date.isAfter(selectedCheckIn) && date.isBefore(selectedCheckOut);

                if      (isCIDay)        { btn.setBackground(new Color(34, 139, 34));  btn.setForeground(Color.WHITE); btn.setFont(new Font("Serif", Font.BOLD, 12)); }
                else if (isCODay)        { btn.setBackground(new Color(60, 140, 200)); btn.setForeground(Color.WHITE); btn.setFont(new Font("Serif", Font.BOLD, 12)); }
                else if (inRange)        { btn.setBackground(new Color(180, 220, 180)); btn.setForeground(new Color(30, 80, 30)); }
                else if (isBooked)       { btn.setBackground(new Color(220, 80, 60));  btn.setForeground(Color.WHITE); btn.setToolTipText("Already booked"); btn.setEnabled(false); }
                else if (isMaintenance)  { btn.setBackground(new Color(180, 100, 0));  btn.setForeground(Color.WHITE); btn.setToolTipText("Under maintenance"); btn.setEnabled(false); }
                else if (isPast)         { btn.setBackground(new Color(235, 230, 220)); btn.setForeground(new Color(180, 170, 155)); btn.setEnabled(false); }
                else                     { btn.setBackground(Color.WHITE); btn.setForeground(new Color(50, 25, 0)); }

                if (!isPast && !isBooked && !isMaintenance) {
                    final LocalDate d = date;
                    btn.addActionListener(e -> onDateClicked(d));
                }
                daysGrid.add(btn);
            }
            daysGrid.revalidate();
            daysGrid.repaint();
        }

        private void onDateClicked(LocalDate date) {
            if (isCheckIn) {
                selectedCheckIn = date;
                if (selectedCheckOut != null && !selectedCheckOut.isAfter(selectedCheckIn))
                    selectedCheckOut = null;
            } else {
                if (selectedCheckIn == null) {
                    JOptionPane.showMessageDialog(GuestDashboardGUI.this,
                        "Please select a check-in date first.", "Select check-in", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (!date.isAfter(selectedCheckIn)) {
                    JOptionPane.showMessageDialog(GuestDashboardGUI.this,
                        "Check-out must be after check-in.", "Invalid date", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                selectedCheckOut = date;
            }
            updateDateLabels();
            checkInCalendar.refresh();
            checkOutCalendar.refresh();
        }
    }

    // ── Data Loaders ─────────────────────────────────────────────────────────

    /** Refreshes which property IDs this guest has future/current bookings for */
    private void refreshMyBookedProperties() {
        if (guestId > 0)
            myBookedPropertyIds = bookingController.getActivePropertyIdsForGuest(guestId);
        else
            myBookedPropertyIds = new HashSet<>();
    }

    private void loadProperties() {
        refreshMyBookedProperties();
        propertyModel.setRowCount(0);
        for (HeritageProperty p : propertyController.getAllActiveProperties()) {
            // Show "Booked by You" only for THIS guest's active bookings;
            // everyone else sees the real property status (Available / Under Maintenance)
            String displayStatus;
            if (myBookedPropertyIds.contains(p.getPropertyId())) {
                displayStatus = "Booked by You";
            } else {
                // Hide "Occupied" from other guests — to them it looks Available
                // unless it's Under Maintenance (which genuinely blocks all bookings)
                displayStatus = p.getCurrentStatus().equals("Under Maintenance")
                        ? "Under Maintenance" : "Available";
            }

            propertyModel.addRow(new Object[]{
                p.getPropertyId(),
                p.getPropertyName(),
                String.format("%.2f", p.getDailyRate()),
                displayStatus
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

    private void loadBookedDates(int propertyId) {
        bookedDates.clear();
        maintenanceDates.clear();

        // Load booking dates
        for (Booking b : bookingController.getAllActiveBookings()) {
            if (b.getPropertyId() == propertyId) {
                LocalDate s = b.getStartDate().toLocalDate();
                LocalDate e = b.getEndDate().toLocalDate();
                for (LocalDate d = s; !d.isAfter(e.minusDays(1)); d = d.plusDays(1))
                    bookedDates.add(d);
            }
        }

        // Load maintenance dates
        for (com.project.model.Maintenance m : maintenanceController.getMaintenanceByProperty(propertyId)) {
            if (!m.getStatus().equals("Completed") && !m.getStatus().equals("Cancelled")) {
                LocalDate s = m.getStartDate().toLocalDate();
                LocalDate e = m.getEndDate().toLocalDate();
                for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1))
                    maintenanceDates.add(d);
            }
        }
    }

    private void updateDateLabels() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        checkInLabel.setText("Check-in: "  + (selectedCheckIn  != null ? selectedCheckIn.format(fmt)  : "Not selected"));
        checkOutLabel.setText("Check-out: " + (selectedCheckOut != null ? selectedCheckOut.format(fmt) : "Not selected"));
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void handleBooking() {
        int selectedRow = propertyTable.getSelectedRow();
        if (selectedRow < 0)                        { warn("Please select a property first.");                                         return; }
        if (guestId < 0)                            { error("Session error – please log in again.");                                   return; }
        if (selectedCheckIn == null || selectedCheckOut == null) { warn("Please select both check-in and check-out dates."); return; }

        int    propertyId = (int) propertyModel.getValueAt(selectedRow, 0);
        double dailyRate  = Double.parseDouble(propertyModel.getValueAt(selectedRow, 2).toString());
        long   days       = ChronoUnit.DAYS.between(selectedCheckIn, selectedCheckOut);
        double total      = days * dailyRate;

        int confirm = JOptionPane.showConfirmDialog(this,
            String.format("Booking Summary:\n• Property: %s\n• Check-in: %s\n• Check-out: %s\n• Duration: %d night(s)\n• Total: RM %.2f\n\nConfirm booking?",
                propertyModel.getValueAt(selectedRow, 1), selectedCheckIn, selectedCheckOut, days, total),
            "Confirm Booking", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            String result = bookingController.createBooking(propertyId, guestId,
                    Date.valueOf(selectedCheckIn), Date.valueOf(selectedCheckOut), dailyRate);
            switch (result) {
                case "SUCCESS" -> {
                    info("Booking confirmed! Enjoy your heritage stay.");
                    selectedCheckIn = null; selectedCheckOut = null;
                    updateDateLabels();
                    loadProperties(); loadMyBookings();
                    loadBookedDates(propertyId);
                    checkInCalendar.refresh(); checkOutCalendar.refresh();
                    tabbedPane.setSelectedIndex(1);
                }
                case "CONFLICT_BOOKING" ->
                    warn("These dates overlap with an existing booking. Please choose different dates.");
                case "CONFLICT_MAINTENANCE" ->
                    warn("These dates overlap with a scheduled maintenance period.\nProperty will be closed for preservation work.");
                default -> error("Booking failed. Please try again.");
            }
        }
    }

    private void cancelSelectedBooking() {
        int row = bookingTable.getSelectedRow();
        if (row < 0) { warn("Please select a booking to cancel."); return; }
        int bookingId = (int) bookingModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Cancel booking #" + bookingId + "?",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (bookingController.cancelBooking(bookingId)) {
                info("Booking cancelled successfully.");
                loadMyBookings(); loadProperties();
            } else error("Failed to cancel booking.");
        }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────
    private JPanel legendItem(Color color, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setBackground(new Color(245, 240, 230));
        JLabel dot = new JLabel("  "); dot.setOpaque(true);
        dot.setBackground(color); dot.setPreferredSize(new Dimension(14, 14));
        JLabel lbl = new JLabel(text); lbl.setFont(new Font("Serif", Font.PLAIN, 11));
        p.add(dot); p.add(lbl); return p;
    }

    private void styleTable(JTable t) {
        t.getTableHeader().setBackground(new Color(101, 55, 0));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(new Font("Serif", Font.BOLD, 13));
        t.setGridColor(new Color(200, 185, 160));
        t.setSelectionBackground(new Color(200, 160, 100));
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Serif", Font.BOLD, 15));
        lbl.setForeground(new Color(101, 55, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel formRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(labelText);
        lbl.setPreferredSize(new Dimension(160, 30));
        lbl.setFont(new Font("Serif", Font.PLAIN, 13));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JTextField formField() {
        JTextField f = new JTextField();
        f.setFont(new Font("Serif", Font.PLAIN, 13));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return f;
    }

    private JPasswordField passField() {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("Serif", Font.PLAIN, 13));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return f;
    }

    private JButton styledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        return b;
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(200, 185, 160));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE); }
    private void warn(String msg)  { JOptionPane.showMessageDialog(this, msg, "Notice",  JOptionPane.WARNING_MESSAGE);     }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error",   JOptionPane.ERROR_MESSAGE);       }
}