package com.project.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.project.controller.AuthController;
import com.project.controller.BookingController;
import com.project.controller.MaintenanceController;
import com.project.controller.ProfileController;
import com.project.controller.PropertyController;
import com.project.controller.ReportController;
import com.project.controller.ReportController.PropertyReport;
import com.project.model.Booking;
import com.project.model.HeritageProperty;
import com.project.model.Maintenance;
import com.toedter.calendar.JDateChooser;

public class StaffDashboardGUI extends JFrame {

    private final String userRole;
    private String staffEmail; // kept for profile lookup
    private int staffId = -1;

    private PropertyController propertyController = new PropertyController();
    private BookingController bookingController = new BookingController();
    private MaintenanceController maintController = new MaintenanceController();
    private ReportController reportController = new ReportController();
    private ProfileController profileController = new ProfileController();
    private AuthController authController = new AuthController();

    // Property tab
    private JTable propertyTable;
    private DefaultTableModel propertyModel;

    // Bookings tab — now shows Guest Name + Email instead of Guest ID
    private JTable bookingTable;
    private DefaultTableModel bookingModel;

    // Maintenance tab
    private JTable maintTable;
    private DefaultTableModel maintModel;
    private JComboBox<String> fMaintProperty;
    private JComboBox<String> fMaintAssigned;

    // Reports tab
    private JTable reportTable;
    private DefaultTableModel reportModel;
    private JLabel lblRevenue, lblMaintCost, lblActiveBookings, lblActiveMaint;

    // Staff tab
    private JTable staffTable;
    private DefaultTableModel staffModel;

    // Profile tab fields (kept as instance vars for the load-once listener)
    private JTextField pfName, pfEmail, pfContact;
    private JLabel pfRoleLabel;

    public StaffDashboardGUI(String role) {
        this.userRole = role;
        this.staffEmail = "";
        buildUI();
    }

    /**
     * Preferred constructor — pass email so profile tab can load data
     */
    public StaffDashboardGUI(String role, String email) {
        this.userRole = role;
        this.staffEmail = email;
        buildUI();
    }

    private JTabbedPane mainTabs; // keep reference for menu navigation

    private void buildUI() {
        setTitle("Smart Heritage Stay – " + userRole + " Dashboard");
        setSize(1050, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(240, 244, 248));

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 41, 59));
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("Smart Heritage Stay  –  " + userRole + " Portal");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(248, 250, 252));
        header.add(title, BorderLayout.WEST);

        // Avatar button: first letter of full name
        String[] staffProfile = profileController.getStaffProfile(staffEmail);
        String fullName = (staffProfile != null && staffProfile[1] != null && !staffProfile[1].isBlank())
                ? staffProfile[1] : (staffEmail.isEmpty() ? userRole : staffEmail);
        if (staffProfile != null && staffProfile[0] != null) {
            try { staffId = Integer.parseInt(staffProfile[0]); } catch (NumberFormatException ignored) {}
        }
        String initial = fullName.trim().substring(0, 1).toUpperCase();

        JButton avatarBtn = new JButton(initial) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                // Shadow
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillOval(3, 3, getWidth() - 2, getHeight() - 2);
                // Gradient circle (teal-blue for staff)
                java.awt.GradientPaint gp = new java.awt.GradientPaint(
                        0, 0, new Color(56, 140, 200),
                        0, getHeight(), new Color(20, 80, 140));
                g2.setPaint(gp);
                g2.fillOval(0, 0, getWidth() - 2, getHeight() - 2);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 35));
                    g2.fillOval(0, 0, getWidth() - 2, getHeight() - 2);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatarBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        avatarBtn.setForeground(Color.WHITE);
        avatarBtn.setFocusPainted(false);
        avatarBtn.setBorderPainted(false);
        avatarBtn.setContentAreaFilled(false);
        avatarBtn.setOpaque(false);
        avatarBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatarBtn.setPreferredSize(new Dimension(46, 46));
        avatarBtn.setToolTipText(fullName + " (" + userRole + ")");

        final String capturedFullName = fullName;
        avatarBtn.addActionListener(e -> showStaffUserMenu(avatarBtn, capturedFullName));
        header.add(avatarBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        mainTabs = new JTabbedPane();
        mainTabs.addTab("🏡  Properties", buildPropertiesTab());
        mainTabs.addTab("📅  Bookings", buildBookingsTab());
        mainTabs.addTab("🔧  Maintenance", buildMaintenanceTab());
        mainTabs.addTab("📊  Reports", buildReportsTab());
        if (userRole.equalsIgnoreCase("Admin")) {
            mainTabs.addTab("👥  Staff", buildStaffTab());
        }
        mainTabs.addTab("👤  My Profile", buildProfileTab());
        add(mainTabs, BorderLayout.CENTER);

        setVisible(true);
        refreshAll();
    }

    /** Shows the staff/admin account popup menu below the avatar button. */
    private void showStaffUserMenu(JButton anchor, String fullName) {
        JDialog menu = new JDialog(this, false);
        menu.setUndecorated(true);
        menu.setBackground(new Color(0, 0, 0, 0));

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(4, 4, getWidth() - 4, getHeight() - 4, 16, 16);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 16, 16);
                g2.setColor(new Color(200, 215, 230));
                g2.drawRoundRect(0, 0, getWidth() - 5, getHeight() - 5, 16, 16);
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        // Name + role header
        JLabel nameLabel = new JLabel("  " + fullName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(new Color(15, 30, 55));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 16, 2, 16));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(nameLabel);

        JLabel roleLabel = new JLabel("  " + userRole);
        roleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        roleLabel.setForeground(userRole.equalsIgnoreCase("Admin")
                ? new Color(180, 20, 20) : new Color(20, 80, 160));
        roleLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(roleLabel);

        JSeparator sep1 = new JSeparator();
        sep1.setForeground(new Color(200, 215, 230));
        sep1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep1);
        card.add(Box.createVerticalStrut(6));

        // My Profile — navigate to last tab
        card.add(staffMenuItem("👤  My Profile", () -> {
            menu.dispose();
            int lastTab = mainTabs.getTabCount() - 1;
            mainTabs.setSelectedIndex(lastTab);
        }));

        // Settings
        card.add(Box.createVerticalStrut(4));
        JSeparator sep2 = new JSeparator();
        sep2.setForeground(new Color(200, 215, 230));
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep2);
        card.add(Box.createVerticalStrut(4));

        JLabel settingsHdr = new JLabel("  ⚙  Settings");
        settingsHdr.setFont(new Font("Segoe UI", Font.BOLD, 11));
        settingsHdr.setForeground(new Color(100, 120, 150));
        settingsHdr.setBorder(BorderFactory.createEmptyBorder(2, 16, 4, 16));
        settingsHdr.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(settingsHdr);

        card.add(staffMenuItem("    🔑  Change Password", () -> {
            menu.dispose();
            showStaffChangePasswordDialog();
        }));
        card.add(staffMenuItem("    🗑  Delete Account", () -> {
            menu.dispose();
            showStaffDeleteAccountDialog();
        }));

        card.add(Box.createVerticalStrut(4));
        JSeparator sep3 = new JSeparator();
        sep3.setForeground(new Color(200, 215, 230));
        sep3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep3);
        card.add(Box.createVerticalStrut(6));

        card.add(staffMenuItem("⏻  Logout", () -> {
            menu.dispose();
            dispose();
            new GuestDashboardGUI();
        }));

        menu.setContentPane(card);
        menu.pack();
        menu.setMinimumSize(new Dimension(230, menu.getHeight()));

        java.awt.Point pt = anchor.getLocationOnScreen();
        menu.setLocation(pt.x + anchor.getWidth() - menu.getWidth(), pt.y + anchor.getHeight() + 4);

        menu.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent e) {}
            public void windowLostFocus(java.awt.event.WindowEvent e) { menu.dispose(); }
        });
        menu.setVisible(true);
    }

    /** Styled hover menu item for staff popup. */
    private JButton staffMenuItem(String text, Runnable action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                if (getModel().isRollover()) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    g2.setColor(new Color(235, 242, 252));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(new Color(15, 30, 55));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(7, 20, 7, 20));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    /** Change Password dialog for staff/admin. */
    private void showStaffChangePasswordDialog() {
        JDialog dlg = new JDialog(this, "Change Password", true);
        dlg.setSize(400, 260);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(new Color(240, 244, 248));
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new java.awt.GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Change Password"),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 6, 5, 6);
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;

        JPasswordField fCurrent = passField();
        JPasswordField fNew = passField();
        JPasswordField fConfirm = passField();
        addFormRow(form, gbc, 0, "Current Password:", fCurrent);
        addFormRow(form, gbc, 1, "New Password:", fNew);
        addFormRow(form, gbc, 2, "Confirm New Password:", fConfirm);

        JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 8));
        footer.setBackground(new Color(240, 244, 248));
        JButton save = styledButton("Change Password", new Color(30, 41, 59));
        JButton cancel = styledButton("Cancel", new Color(150, 160, 175));
        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            String current = new String(fCurrent.getPassword());
            String newPass = new String(fNew.getPassword());
            String confirm = new String(fConfirm.getPassword());
            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) { error("Fill all password fields."); return; }
            if (newPass.length() < 6) { error("New password must be at least 6 characters."); return; }
            if (!newPass.equals(confirm)) { error("New passwords do not match."); return; }
            if (staffId < 0) { error("Profile not loaded. Please re-login."); return; }
            String result = profileController.changeStaffPassword(staffId, current, newPass);
            switch (result) {
                case "SUCCESS" -> { info("Password changed successfully."); dlg.dispose(); }
                case "WRONG_PASSWORD" -> error("Current password is incorrect.");
                default -> error("Failed to change password.");
            }
        });
        footer.add(cancel);
        footer.add(save);

        dlg.add(form, BorderLayout.CENTER);
        dlg.add(footer, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    /** Delete Account dialog for staff/admin. */
    private void showStaffDeleteAccountDialog() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><b>Delete your account?</b><br><br>"
                + "This action is permanent and cannot be undone.<br>"
                + "Your maintenance records will remain in the system.</html>",
                "Delete Account", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (authController.deleteStaff(staffId)) {
                JOptionPane.showMessageDialog(this, "Your account has been deleted.");
                dispose();
                new GuestDashboardGUI();
            } else {
                error("Failed to delete account. Please try again.");
            }
        }
    }


    // =====================================================================
    //  TAB 1 – PROPERTIES
    // =====================================================================
    private JPanel buildPropertiesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(240, 244, 248));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] cols = {"ID", "Property Name", "Daily Rate (RM)", "Status"};
        propertyModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        propertyTable = new JTable(propertyModel);
        propertyTable.setRowHeight(32);
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
                    setBackground(switch (status) {
                        case "Occupied" ->
                            new Color(255, 200, 200);            // Light red
                        case "Under Maintenance" ->
                            new Color(255, 200, 100);  // Light orange
                        case "Available" ->
                            Color.WHITE;
                        default ->
                            Color.WHITE;
                    });
                    setForeground(switch (status) {
                        case "Occupied" ->
                            new Color(180, 0, 0);
                        case "Under Maintenance" ->
                            new Color(180, 100, 0);
                        case "Available" ->
                            Color.BLACK;
                        default ->
                            Color.BLACK;
                    });
                }
                return this;
            }
        };
        propertyTable.getColumnModel().getColumn(0).setCellRenderer(colorRenderer);
        propertyTable.getColumnModel().getColumn(1).setCellRenderer(colorRenderer);
        propertyTable.getColumnModel().getColumn(2).setCellRenderer(colorRenderer);
        panel.add(new JScrollPane(propertyTable), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnRow.setBackground(new Color(240, 244, 248));

        JButton btnAdd = new JButton("＋  Add Property");
        btnAdd.setBackground(new Color(14, 165, 233));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);

        JButton btnDelete = new JButton("Delete Property");
        btnDelete.setBackground(new Color(239, 68, 68));
        btnDelete.setForeground(Color.WHITE);

        btnAdd.addActionListener(e -> showAddPropertyDialog());

        btnDelete.addActionListener(e -> {
            if (!userRole.equalsIgnoreCase("Admin")) {
                permissionDenied();
                return;
            }
            int row = propertyTable.getSelectedRow();
            if (row < 0) {
                noSelection();
                return;
            }
            int id = (int) propertyModel.getValueAt(row, 0);
            if (confirm("Delete property ID " + id + "?")) {
                propertyController.softDeleteProperty(id);
                loadProperties();
            }
        });

        btnRow.add(btnAdd);
        btnRow.add(Box.createHorizontalStrut(20));
        btnRow.add(btnDelete);
        panel.add(btnRow, BorderLayout.SOUTH);
        return panel;
    }

    private void showAddPropertyDialog() {
        JDialog dlg = new JDialog(this, "Add New Property", true);
        dlg.setSize(480, 320);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.getContentPane().setBackground(new Color(240, 244, 248));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Property Details"),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField fName = new JTextField(20);
        JTextField fRate = new JTextField(10);

        JTextField fPhotoPath = new JTextField();
        fPhotoPath.setEditable(false);
        fPhotoPath.setBackground(Color.WHITE);
        JButton btnBrowse = new JButton("Browse…");
        btnBrowse.setFocusPainted(false);
        final java.io.File[] chosenFile = {null};

        btnBrowse.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Choose Property Photo");
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Image files (JPG, PNG, GIF, BMP)", "jpg", "jpeg", "png", "gif", "bmp"));
            fc.setAcceptAllFileFilterUsed(false);
            if (fc.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                chosenFile[0] = fc.getSelectedFile();
                fPhotoPath.setText(chosenFile[0].getName());
            }
        });

        JPanel photoRow = new JPanel(new BorderLayout(6, 0));
        photoRow.setOpaque(false);
        photoRow.add(fPhotoPath, BorderLayout.CENTER);
        photoRow.add(btnBrowse, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        form.add(new JLabel("Property Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(fName, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(new JLabel("Daily Rate (RM):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(fRate, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        form.add(new JLabel("Property Photo:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(photoRow, gbc);

        dlg.add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        footer.setBackground(new Color(240, 244, 248));
        JButton btnSave = new JButton("Add Property");
        JButton btnCancel = new JButton("Cancel");
        btnSave.setBackground(new Color(14, 165, 233));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);

        btnCancel.addActionListener(ev -> dlg.dispose());
        btnSave.addActionListener(ev -> {
            String name = fName.getText().trim();
            String rateStr = fRate.getText().trim();

            if (name.isEmpty()) {
                error("Property name is required.");
                return;
            }
            if (rateStr.isEmpty()) {
                error("Daily rate is required.");
                return;
            }
            if (chosenFile[0] == null) {
                error("Please select a photo for the property.");
                return;
            }

            double rate;
            try {
                rate = Double.parseDouble(rateStr);
            } catch (NumberFormatException ex) {
                error("Daily rate must be a valid number.");
                return;
            }
            if (rate <= 0) {
                error("Daily rate must be greater than zero.");
                return;
            }

            String result = propertyController.addProperty(name, rate, chosenFile[0]);
            switch (result) {
                case "SUCCESS" -> {
                    info("Property '" + name + "' added successfully.");
                    loadProperties();
                    loadMaintDropdowns();
                    dlg.dispose();
                }
                case "NAME_TAKEN" ->
                    error("A property with the name '" + name + "' already exists. Please use a different name.");
                default ->
                    error("Failed to add property. Please try again.");
            }
        });

        footer.add(btnCancel);
        footer.add(btnSave);
        dlg.add(footer, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // =====================================================================
    //  TAB 2 – BOOKINGS  (Guest Name + Email instead of Guest ID)
    // =====================================================================
    private JPanel buildBookingsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(240, 244, 248));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ── Guest Name + Email columns replace Guest ID ──
        String[] cols = {"Booking ID", "Property", "Guest Name", "Guest Email", "Check-in", "Check-out", "Total (RM)"};
        bookingModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        bookingTable = new JTable(bookingModel);
        bookingTable.setRowHeight(32);
        styleTable(bookingTable);
        panel.add(new JScrollPane(bookingTable), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnRow.setBackground(new Color(240, 244, 248));

        JButton btnCancel = new JButton("Cancel Booking (Admin Only)");
        btnCancel.setBackground(new Color(239, 68, 68));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.addActionListener(e -> {
            if (!userRole.equalsIgnoreCase("Admin")) {
                permissionDenied();
                return;
            }
            int row = bookingTable.getSelectedRow();
            if (row < 0) {
                noSelection();
                return;
            }
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
        panel.setBackground(new Color(240, 244, 248));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] cols = {"ID", "Property ID", "Type", "Description", "Start", "End", "Status", "Assigned To", "Est. Cost (RM)"};
        maintModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        maintTable = new JTable(maintModel);
        maintTable.setRowHeight(32);
        styleTable(maintTable);
        panel.add(new JScrollPane(maintTable), BorderLayout.CENTER);

        // Schedule form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Schedule New Maintenance / Preservation Work"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        fMaintProperty = new JComboBox<>();
        JComboBox<String> fType = new JComboBox<>(new String[]{
            "Structural Repair", "Timber Restoration", "Roof Repair",
            "Painting & Finishing", "Electrical", "Plumbing", "Foundation Work", "General Upkeep", "Other..."
        });
        fType.addActionListener(e -> {
            if ("Other...".equals(fType.getSelectedItem())) {
                String custom = JOptionPane.showInputDialog(form, "Enter custom maintenance type:");
                if (custom != null && !custom.trim().isEmpty()) {
                    fType.removeItem("Other...");
                    fType.addItem(custom.trim());
                    fType.addItem("Other...");
                    fType.setSelectedItem(custom.trim());
                } else {
                    fType.setSelectedIndex(0);
                }
            }
        });
        JTextField fDesc = new JTextField(20);
        JDateChooser fStart = new JDateChooser();
        fStart.setDateFormatString("yyyy-MM-dd");
        JDateChooser fEnd = new JDateChooser();
        fEnd.setDateFormatString("yyyy-MM-dd");
        fMaintAssigned = new JComboBox<>();
        JTextField fCost = new JTextField(8);

        addFormRow(form, gbc, 0, "Property ID:", fMaintProperty);
        addFormRow(form, gbc, 1, "Type:", fType);
        addFormRow(form, gbc, 2, "Description:", fDesc);
        addFormRow(form, gbc, 3, "Start Date:", fStart);
        addFormRow(form, gbc, 4, "End Date:", fEnd);
        addFormRow(form, gbc, 5, "Assigned To:", fMaintAssigned);
        addFormRow(form, gbc, 6, "Est. Cost (RM):", fCost);

        JButton btnSchedule = new JButton("  Schedule  ");
        btnSchedule.setBackground(new Color(15, 23, 42));
        btnSchedule.setForeground(Color.WHITE);
        btnSchedule.setFocusPainted(false);
        gbc.gridx = 1;
        gbc.gridy = 7;
        form.add(btnSchedule, gbc);

        btnSchedule.addActionListener(e -> {
            try {
                if (fMaintProperty.getSelectedItem() == null) {
                    error("Property must be selected.");
                    return;
                }
                if (fType.getSelectedItem() == null || fType.getSelectedItem().toString().trim().isEmpty() || "Other...".equals(fType.getSelectedItem())) {
                    error("Maintenance Type must be properly specified.");
                    return;
                }
                if (fDesc.getText().trim().isEmpty()) {
                    error("Description is required.");
                    return;
                }
                if (fStart.getDate() == null) {
                    error("Start Date must be selected from the calendar.");
                    return;
                }
                if (fEnd.getDate() == null) {
                    error("End Date must be selected from the calendar.");
                    return;
                }
                if (fMaintAssigned.getSelectedItem() == null) {
                    error("Assignee must be selected from staff list.");
                    return;
                }
                if (fCost.getText().trim().isEmpty()) {
                    error("Estimated Cost is required.");
                    return;
                }

                int propId = Integer.parseInt(fMaintProperty.getSelectedItem().toString().split(" - ")[0]);
                String type = fType.getSelectedItem().toString();
                String desc = fDesc.getText().trim();
                Date start = new Date(fStart.getDate().getTime());
                Date end = new Date(fEnd.getDate().getTime());
                String assgn = fMaintAssigned.getSelectedItem().toString();
                double cost = Double.parseDouble(fCost.getText().trim());

                if (!end.after(start) && !end.equals(start)) {
                    error("End date must be on or after start date.");
                    return;
                }

                String result = maintController.scheduleMaintenance(propId, type, desc, start, end, assgn, cost);
                switch (result) {
                    case "SUCCESS" -> {
                        info("Maintenance scheduled. Property status set to 'Under Maintenance'.");
                        loadMaintenance();
                        loadProperties();
                        loadMaintDropdowns();
                        fMaintProperty.setSelectedIndex(0);
                        fType.setSelectedIndex(0);
                        fDesc.setText("");
                        fStart.setDate(null);
                        fEnd.setDate(null);
                        fMaintAssigned.setSelectedIndex(0);
                        fCost.setText("");
                    }
                    case "CONFLICT_BOOKING" ->
                        warn(
                                "⚠ A guest booking exists in this period!\nCancel or move the booking before scheduling maintenance.");
                    case "CONFLICT_MAINTENANCE" ->
                        warn("Another maintenance task overlaps these dates.");
                    default ->
                        error("Failed to schedule maintenance.");
                }
            } catch (NumberFormatException ex) {
                error("Invalid number format.");
            } catch (IllegalArgumentException ex) {
                error("Invalid date format. Use YYYY-MM-DD.");
            }
        });

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actionRow.setBackground(new Color(240, 244, 248));

        JButton btnInProgress = new JButton("Mark In Progress");
        JButton btnCompleted = new JButton("Mark Completed");
        JButton btnCancelled = new JButton("Mark Cancelled");
        JButton btnDeleteMaint = new JButton("Delete");
        btnDeleteMaint.setBackground(new Color(239, 68, 68));
        btnDeleteMaint.setForeground(Color.WHITE);

        btnInProgress.addActionListener(e -> updateMaintStatus("In Progress"));
        btnCompleted.addActionListener(e -> updateMaintStatus("Completed"));
        btnCancelled.addActionListener(e -> updateMaintStatus("Cancelled"));
        btnDeleteMaint.addActionListener(e -> {
            int row = maintTable.getSelectedRow();
            if (row < 0) {
                noSelection();
                return;
            }
            int id = (int) maintModel.getValueAt(row, 0);
            if (confirm("Delete maintenance record #" + id + "?")) {
                maintController.deleteMaintenance(id);
                loadMaintenance();
            }
        });

        actionRow.add(new JLabel("Selected:"));
        actionRow.add(btnInProgress);
        actionRow.add(btnCompleted);
        actionRow.add(btnCancelled);
        actionRow.add(Box.createHorizontalStrut(15));
        actionRow.add(btnDeleteMaint);

        JPanel south = new JPanel(new BorderLayout());
        south.add(form, BorderLayout.CENTER);
        south.add(actionRow, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private void updateMaintStatus(String status) {
        int row = maintTable.getSelectedRow();
        if (row < 0) {
            noSelection();
            return;
        }
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
        panel.setBackground(new Color(240, 244, 248));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ── Summary cards ──
        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
        cards.setBackground(new Color(240, 244, 248));
        cards.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        lblRevenue = makeSummaryCard("Total Revenue", "RM 0.00", new Color(14, 165, 233));
        lblMaintCost = makeSummaryCard("Maintenance Cost", "RM 0.00", new Color(160, 80, 20));
        lblActiveBookings = makeSummaryCard("Active Bookings", "0", new Color(20, 80, 160));
        lblActiveMaint = makeSummaryCard("Active Maintenance", "0", new Color(120, 20, 120));

        cards.add(lblRevenue.getParent());
        cards.add(lblMaintCost.getParent());
        cards.add(lblActiveBookings.getParent());
        cards.add(lblActiveMaint.getParent());
        panel.add(cards, BorderLayout.NORTH);

        // ── Report table ──
        String[] cols = {"Property", "Rental Days", "Maintenance Days", "Occupancy %", "Revenue (RM)", "Maint. Cost (RM)", "Heritage Health"};
        reportModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        reportTable = new JTable(reportModel);
        reportTable.setRowHeight(32);
        styleTable(reportTable);

        reportTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                String v = val == null ? "" : val.toString();
                setForeground(switch (v) {
                    case "Good" ->
                        new Color(0, 128, 0);
                    case "Needs Attention" ->
                        new Color(180, 100, 0);
                    case "At Risk" ->
                        new Color(180, 0, 0);
                    default ->
                        Color.DARK_GRAY;
                });
                setFont(getFont().deriveFont(Font.BOLD));
                return this;
            }
        });
        panel.add(new JScrollPane(reportTable), BorderLayout.CENTER);

        // ── Report type selector ──
        JPanel filterPanel = new JPanel(new BorderLayout(0, 6));
        filterPanel.setBackground(new Color(240, 244, 248));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        // Radio buttons for Annual vs Monthly
        JRadioButton rbAnnual = new JRadioButton("Annual Report", true);
        JRadioButton rbMonthly = new JRadioButton("Monthly Report", false);
        rbAnnual.setBackground(new Color(240, 244, 248));
        rbMonthly.setBackground(new Color(240, 244, 248));
        rbAnnual.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rbMonthly.setFont(new Font("Segoe UI", Font.BOLD, 13));
        ButtonGroup grp = new ButtonGroup();
        grp.add(rbAnnual);
        grp.add(rbMonthly);

        // Year spinner (shared)
        int thisYear = LocalDate.now().getYear();
        SpinnerNumberModel yearModel = new SpinnerNumberModel(thisYear, 2000, 2100, 1);
        JSpinner yearSpinner = new JSpinner(yearModel);
        yearSpinner.setPreferredSize(new Dimension(80, 28));
        ((JSpinner.NumberEditor) yearSpinner.getEditor()).getTextField().setColumns(4);

        // Month spinner (only active when Monthly selected)
        SpinnerNumberModel monthModel = new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1);
        JSpinner monthSpinner = new JSpinner(monthModel);
        monthSpinner.setPreferredSize(new Dimension(60, 28));
        monthSpinner.setEnabled(false);
        JLabel monthLabel = new JLabel("Month:");
        monthLabel.setEnabled(false);

        rbAnnual.addActionListener(e -> {
            monthSpinner.setEnabled(false);
            monthLabel.setEnabled(false);
        });
        rbMonthly.addActionListener(e -> {
            monthSpinner.setEnabled(true);
            monthLabel.setEnabled(true);
        });

        JButton btnGenerate = new JButton("Generate Report");
        btnGenerate.setBackground(new Color(15, 23, 42));
        btnGenerate.setForeground(Color.WHITE);
        btnGenerate.setFocusPainted(false);

        btnGenerate.addActionListener(e -> {
            int year = (int) yearSpinner.getValue();
            Date from, to;
            if (rbAnnual.isSelected()) {
                // Full calendar year
                from = Date.valueOf(LocalDate.of(year, 1, 1));
                to = Date.valueOf(LocalDate.of(year, 12, 31));
            } else {
                // Specific month
                int month = (int) monthSpinner.getValue();
                YearMonth ym = YearMonth.of(year, month);
                from = Date.valueOf(ym.atDay(1));
                to = Date.valueOf(ym.atEndOfMonth());
            }
            loadReport(from, to);
        });

        JPanel radioRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        radioRow.setBackground(new Color(240, 244, 248));
        radioRow.add(rbAnnual);
        radioRow.add(rbMonthly);
        radioRow.add(Box.createHorizontalStrut(10));
        radioRow.add(new JLabel("Year:"));
        radioRow.add(yearSpinner);
        radioRow.add(monthLabel);
        radioRow.add(monthSpinner);
        radioRow.add(Box.createHorizontalStrut(10));
        radioRow.add(btnGenerate);

        filterPanel.add(radioRow, BorderLayout.CENTER);
        panel.add(filterPanel, BorderLayout.SOUTH);
        return panel;
    }

    // =====================================================================
    //  TAB 5 – STAFF MANAGEMENT (Admin only)
    // =====================================================================
    private JPanel buildStaffTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(240, 244, 248));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ── Staff table ──
        String[] cols = {"ID", "Full Name", "Email", "Contact", "Job Role"};
        staffModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        staffTable = new JTable(staffModel);
        staffTable.setRowHeight(32);
        styleTable(staffTable);
        panel.add(new JScrollPane(staffTable), BorderLayout.CENTER);

        // ── Add staff form ──
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Add New Staff Member"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField fName = new JTextField(18);
        JTextField fEmail = new JTextField(18);
        JPasswordField fPass = new JPasswordField(14);
        JTextField fContact = new JTextField(12);
        JComboBox<String> fRole = new JComboBox<>(new String[]{"Staff", "Admin"});

        addFormRow(form, gbc, 0, "Full Name:", fName);
        addFormRow(form, gbc, 1, "Email:", fEmail);
        addFormRow(form, gbc, 2, "Password:", fPass);
        addFormRow(form, gbc, 3, "Contact No.:", fContact);
        addFormRow(form, gbc, 4, "Job Role:", fRole);

        JButton btnAdd = new JButton("  Add Staff  ");
        btnAdd.setBackground(new Color(14, 165, 233));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        gbc.gridx = 1;
        gbc.gridy = 5;
        form.add(btnAdd, gbc);

        btnAdd.addActionListener(e -> {
            String name = fName.getText().trim();
            String email = fEmail.getText().trim();
            String pass = new String(fPass.getPassword());
            String contact = fContact.getText().trim();
            String role = fRole.getSelectedItem().toString();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                error("Name, Email and Password are required.");
                return;
            }
            if (!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$", email)) {
                error("Invalid email format.");
                return;
            }
            if (pass.length() < 6) {
                error("Password must be at least 6 characters.");
                return;
            }

            String result = authController.registerStaff(name, email, pass, contact, role);
            switch (result) {
                case "SUCCESS" -> {
                    info("Staff member '" + name + "' added successfully.");
                    fName.setText("");
                    fEmail.setText("");
                    fPass.setText("");
                    fContact.setText("");
                    loadStaff();
                }
                case "NAME_TAKEN" ->
                    error("This name is already registered. Please use a different full name.");
                case "EMAIL_TAKEN" ->
                    error("This email is already registered.");
                default ->
                    error("Failed to add staff. Please try again.");
            }
        });

        // ── Delete button row ──
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.setBackground(new Color(240, 244, 248));
        JButton btnDelete = new JButton("Remove Selected Staff");
        btnDelete.setBackground(new Color(239, 68, 68));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFocusPainted(false);
        btnDelete.addActionListener(e -> {
            int row = staffTable.getSelectedRow();
            if (row < 0) {
                noSelection();
                return;
            }
            int staffId = Integer.parseInt(staffModel.getValueAt(row, 0).toString());
            String staffName = staffModel.getValueAt(row, 1).toString();
            if (staffId == this.staffId) {
                error("You cannot remove your own account.");
                return;
            }

            // ── Guard: block deletion if staff has active maintenance assigned ──
            if (maintController.hasActiveMaintenance(staffName)) {
                error("<html><b>Cannot remove '" + staffName + "'.</b><br><br>"
                        + "This staff member is currently assigned to one or more<br>"
                        + "active maintenance tasks (Scheduled or In Progress).<br><br>"
                        + "Please reassign or complete those maintenance records first.</html>");
                return;
            }

            if (confirm("Remove staff member '" + staffName + "'? They will no longer be able to log in.")) {
                if (authController.deleteStaff(staffId)) {
                    info("Staff member removed.");
                    loadStaff();
                } else {
                    error("Failed to remove staff member.");
                }
            }
        });
        btnRow.add(btnDelete);

        JPanel south = new JPanel(new BorderLayout());
        south.add(form, BorderLayout.CENTER);
        south.add(btnRow, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);

        // Load staff list when tab is first shown
        panel.addAncestorListener(new javax.swing.event.AncestorListener() {
            boolean loaded = false;

            public void ancestorAdded(javax.swing.event.AncestorEvent e) {
                if (!loaded) {
                    loadStaff();
                    loaded = true;
                }
            }

            public void ancestorRemoved(javax.swing.event.AncestorEvent e) {
            }

            public void ancestorMoved(javax.swing.event.AncestorEvent e) {
            }
        });

        return panel;
    }

    // =====================================================================
    //  TAB 5 – MY PROFILE
    // =====================================================================
    private JPanel buildProfileTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(new Color(240, 244, 248));
        outer.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));

        // Role badge
        pfRoleLabel = new JLabel("Role: " + userRole);
        pfRoleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pfRoleLabel.setForeground(userRole.equalsIgnoreCase("Admin")
                ? new Color(140, 20, 20) : new Color(20, 80, 140));
        pfRoleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(pfRoleLabel);
        card.add(Box.createVerticalStrut(16));

        JLabel secInfo = sectionLabel("Personal Information");
        card.add(secInfo);
        card.add(Box.createVerticalStrut(12));

        pfName = formField();
        pfEmail = formField();
        pfContact = formField();

        card.add(staffFormRow("Full Name:", pfName));
        card.add(Box.createVerticalStrut(8));
        card.add(staffFormRow("Email:", pfEmail));
        card.add(Box.createVerticalStrut(8));
        card.add(staffFormRow("Contact Number:", pfContact));
        card.add(Box.createVerticalStrut(16));

        JButton saveInfoBtn = styledButton("Save Changes", new Color(30, 41, 59));
        saveInfoBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(saveInfoBtn);

        card.add(Box.createVerticalStrut(28));
        card.add(staffSeparator());
        card.add(Box.createVerticalStrut(20));

        JLabel secPass = sectionLabel("Change Password");
        card.add(secPass);
        card.add(Box.createVerticalStrut(12));

        JPasswordField fCurrent = passField();
        JPasswordField fNew = passField();
        JPasswordField fConfirm = passField();

        card.add(staffFormRow("Current Password:", fCurrent));
        card.add(Box.createVerticalStrut(8));
        card.add(staffFormRow("New Password:", fNew));
        card.add(Box.createVerticalStrut(8));
        card.add(staffFormRow("Confirm New Password:", fConfirm));
        card.add(Box.createVerticalStrut(16));

        JButton savePassBtn = styledButton("Change Password", new Color(30, 41, 59));
        savePassBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(savePassBtn);

        outer.add(card, BorderLayout.CENTER);

        // ── Save info ──
        saveInfoBtn.addActionListener(e -> {
            String name = pfName.getText().trim();
            String email = pfEmail.getText().trim();
            String contact = pfContact.getText().trim();
            if (name.isEmpty() || email.isEmpty()) {
                error("Name and email cannot be empty.");
                return;
            }
            if (!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$", email)) {
                error("Invalid email format.");
                return;
            }
            if (staffId < 0) {
                error("Profile not loaded. Please re-login.");
                return;
            }

            String result = profileController.updateStaffProfile(staffId, name, email, contact);
            switch (result) {
                case "SUCCESS" -> {
                    staffEmail = email;
                    info("Profile updated successfully.");
                }
                case "NAME_TAKEN" ->
                    error("This name is already in use by another account. Please choose a different name.");
                case "EMAIL_TAKEN" ->
                    error("This email is already in use by another staff account.");
                default ->
                    error("Failed to update profile.");
            }
        });

        // ── Change password ──
        savePassBtn.addActionListener(e -> {
            String current = new String(fCurrent.getPassword());
            String newPass = new String(fNew.getPassword());
            String confirm = new String(fConfirm.getPassword());
            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                error("Please fill all password fields.");
                return;
            }
            if (newPass.length() < 6) {
                error("New password must be at least 6 characters.");
                return;
            }
            if (!newPass.equals(confirm)) {
                error("New passwords do not match.");
                return;
            }
            if (staffId < 0) {
                error("Profile not loaded. Please re-login.");
                return;
            }

            String result = profileController.changeStaffPassword(staffId, current, newPass);
            switch (result) {
                case "SUCCESS" -> {
                    info("Password changed successfully.");
                    fCurrent.setText("");
                    fNew.setText("");
                    fConfirm.setText("");
                }
                case "WRONG_PASSWORD" ->
                    error("Current password is incorrect.");
                default ->
                    error("Failed to change password.");
            }
        });

        // Load profile once when the tab panel becomes visible
        outer.addAncestorListener(new javax.swing.event.AncestorListener() {
            boolean loaded = false;

            public void ancestorAdded(javax.swing.event.AncestorEvent e) {
                if (!loaded && !staffEmail.isEmpty()) {
                    String[] p = profileController.getStaffProfile(staffEmail);
                    if (p != null) {
                        staffId = Integer.parseInt(p[0]);
                        pfName.setText(p[1]);
                        pfEmail.setText(p[2]);
                        pfContact.setText(p[3]);
                    }
                    loaded = true;
                }
            }

            public void ancestorRemoved(javax.swing.event.AncestorEvent e) {
            }

            public void ancestorMoved(javax.swing.event.AncestorEvent e) {
            }
        });

        return outer;
    }

    // =====================================================================
    //  DATA LOADERS
    // =====================================================================
    private void refreshAll() {
        loadProperties();
        loadBookings();
        loadMaintenance();
        loadSummaryCards();
        loadMaintDropdowns();
    }

    private void loadMaintDropdowns() {
        if (fMaintProperty != null) {
            Object currentSelection = fMaintProperty.getSelectedItem();
            fMaintProperty.removeAllItems();
            for (HeritageProperty p : propertyController.getAllActiveProperties()) {
                fMaintProperty.addItem(p.getPropertyId() + " - " + p.getPropertyName());
            }
            if (currentSelection != null) {
                fMaintProperty.setSelectedItem(currentSelection);
            }
        }
        if (fMaintAssigned != null) {
            Object currentSelection = fMaintAssigned.getSelectedItem();
            fMaintAssigned.removeAllItems();
            for (String[] s : authController.getAllStaff()) {
                fMaintAssigned.addItem(s[1]); // Assuming s[1] is the name of the staff
            }
            if (currentSelection != null) {
                fMaintAssigned.setSelectedItem(currentSelection);
            }
        }
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

    /**
     * Bookings: fetch guest name + email via ProfileController
     */
    private void loadBookings() {
        bookingModel.setRowCount(0);
        for (Booking b : bookingController.getAllActiveBookings()) {
            String[] guestInfo = profileController.getGuestNameAndEmail(b.getGuestId());
            bookingModel.addRow(new Object[]{
                b.getBookingId(),
                bookingController.getPropertyName(b.getPropertyId()),
                guestInfo[0], // Full name
                guestInfo[1], // Email
                b.getStartDate(), b.getEndDate(),
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
        List<PropertyReport> results = reportController.getOccupancyReport(from, to);
        if (results.isEmpty()) {
            // Show a hint row so the user knows data was searched but none found
            reportModel.addRow(new Object[]{"No data found for the selected period.", "-", "-", "-", "-", "-", "-"});
        } else {
            for (PropertyReport r : results) {
                // Compute occupancy % against total calendar days in the period
                long periodDays = (to.getTime() - from.getTime()) / (1000L * 60 * 60 * 24) + 1;
                double occ = periodDays > 0 ? (r.rentalDays * 100.0 / periodDays) : 0;
                reportModel.addRow(new Object[]{
                    r.propertyName, r.rentalDays, r.maintenanceDays,
                    String.format("%.1f%%", occ),
                    String.format("%.2f", r.totalRevenue),
                    String.format("%.2f", r.totalMaintenanceCost),
                    r.healthStatus
                });
            }
        }
        loadSummaryCards();
    }

    private void loadStaff() {
        if (staffModel == null) {
            return;
        }
        staffModel.setRowCount(0);
        for (String[] s : authController.getAllStaff()) {
            staffModel.addRow(new Object[]{s[0], s[1], s[2], s[3], s[4]});
        }
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================
    private JLabel makeSummaryCard(String label, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 2),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(Color.GRAY);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 18));
        val.setForeground(accent);
        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        return val;
    }

    private void addFormRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(field, gbc);
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 15));
        l.setForeground(new Color(30, 41, 59));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel staffFormRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(labelText);
        lbl.setPreferredSize(new Dimension(160, 30));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JTextField formField() {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return f;
    }

    private JPasswordField passField() {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return f;
    }

    private JButton styledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        return b;
    }

    private JSeparator staffSeparator() {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(226, 232, 240));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    private void styleTable(JTable t) {
        t.getTableHeader().setBackground(new Color(15, 23, 42));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setGridColor(new Color(226, 232, 240));
        t.setSelectionBackground(new Color(186, 230, 253));
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

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Notice", JOptionPane.WARNING_MESSAGE);
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
