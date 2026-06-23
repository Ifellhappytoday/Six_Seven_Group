package com.project.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.project.controller.AuthController;
import com.project.controller.BookingController;
import com.project.controller.MaintenanceController;
import com.project.controller.ProfileController;
import com.project.controller.PropertyController;
import com.project.model.Booking;
import com.project.model.HeritageProperty;

public class GuestDashboardGUI extends JFrame {

    // ── Colour Palette ────────────────────────────────────────────────────────
    private static final Color C_BG = new Color(248, 246, 242);   // warm off-white
    private static final Color C_HEADER_TOP = new Color(62, 38, 12);    // dark espresso
    private static final Color C_HEADER_BOT = new Color(101, 62, 20);    // warm brown
    private static final Color C_ACCENT = new Color(180, 115, 40);   // golden amber
    private static final Color C_ACCENT_DARK = new Color(120, 72, 10);   // deep amber
    private static final Color C_TEXT = new Color(40, 25, 8);   // near-black warm
    private static final Color C_MUTED = new Color(140, 115, 90);   // muted sand
    private static final Color C_TABLE_ALT = new Color(252, 248, 241);   // subtle stripe
    private static final Color C_TABLE_HDR = new Color(62, 38, 12);
    private static final Color C_SEL = new Color(240, 210, 150);   // golden selection
    private static final Color C_DANGER = new Color(190, 55, 40);
    private static final Color C_SUCCESS = new Color(46, 130, 69);
    private static final Color C_CARD = Color.WHITE;
    private static final Color C_BORDER = new Color(220, 205, 180);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font F_TITLE = new Font("SansSerif", Font.BOLD, 20);
    private static final Font F_SUB = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font F_TAB = new Font("SansSerif", Font.BOLD, 13);
    private static final Font F_HDR = new Font("SansSerif", Font.BOLD, 13);
    private static final Font F_BODY = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font F_BOLD = new Font("SansSerif", Font.BOLD, 13);
    private static final Font F_SECTION = new Font("SansSerif", Font.BOLD, 14);
    private static final Font F_SMALL = new Font("SansSerif", Font.PLAIN, 11);

    // ── State ─────────────────────────────────────────────────────────────────
    private int guestId;
    private String guestEmail;

    private PropertyController propertyController = new PropertyController();
    private BookingController bookingController = new BookingController();
    private ProfileController profileController = new ProfileController();
    private MaintenanceController maintenanceController = new MaintenanceController();

    private JTable propertyTable;
    private JTable bookingTable;
    private DefaultTableModel propertyModel;
    private DefaultTableModel bookingModel;
    private JTabbedPane tabbedPane;

    private JLabel checkInLabel;
    private JLabel checkOutLabel;
    private LocalDate selectedCheckIn = null;
    private LocalDate selectedCheckOut = null;

    private Set<LocalDate> bookedDates = new HashSet<>();
    private Set<LocalDate> maintenanceDates = new HashSet<>();
    private Set<Integer> myBookedPropertyIds = new HashSet<>();

    // ── Carousel State ────────────────────────────────────────────────────────
    private java.util.List<HeritageProperty> carouselProperties = new java.util.ArrayList<>();
    private int carouselIndex = 0;
    private JPanel carouselCardPanel;
    private JLabel carouselImageLabel;
    private JLabel carouselNameLabel;
    private JLabel carouselPriceLabel;
    private JLabel carouselDescLabel;
    private JButton carouselBookBtn;
    private JLabel carouselCounterLabel;
    private JButton carouselPrevBtn;
    private JButton carouselNextBtn;

    // ── Constructors ──────────────────────────────────────────────────────────
    public GuestDashboardGUI(int guestId, String guestEmail) {
        this.guestId = guestId;
        this.guestEmail = guestEmail;
        buildUI();
    }

    public GuestDashboardGUI(String guestEmail) {
        this.guestEmail = guestEmail;
        this.guestId = bookingController.getGuestIdByEmail(guestEmail);
        buildUI();
    }

    public GuestDashboardGUI() {
        this.guestId = -1;
        this.guestEmail = "";
        buildUI();
    }

    // ── Main UI Build ─────────────────────────────────────────────────────────
    private void buildUI() {
        setTitle("Smart Heritage Stay – Guest Portal");
        setSize(1020, 720);
        setMinimumSize(new Dimension(820, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);

        buildHeader();

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBackground(C_BG);
        tabbedPane.setFont(F_TAB);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));

        tabbedPane.addTab("  🏡  Browse Properties  ", buildPropertyTab());
        if (guestId != -1) {
            tabbedPane.addTab("  📋  My Bookings  ", buildBookingsTab());
            tabbedPane.addTab("  👤  My Profile  ", buildProfileTab());
        }
        add(tabbedPane, BorderLayout.CENTER);

        setVisible(true);
        if (guestId != -1) {
            refreshMyBookedProperties();
            loadMyBookings();
        }
        loadProperties();
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private void buildHeader() {
        // Gradient panel
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, C_HEADER_TOP, getWidth(), getHeight(), C_HEADER_BOT);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        // Left: title + subtitle
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("🏛  Smart Heritage Stay");
        title.setFont(F_TITLE);
        title.setForeground(new Color(255, 240, 195));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Guest Portal" + (guestEmail != null && !guestEmail.isEmpty() ? "  ·  " + guestEmail : ""));
        sub.setFont(F_SUB);
        sub.setForeground(new Color(210, 185, 145));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(title);
        left.add(Box.createVerticalStrut(3));
        left.add(sub);
        header.add(left, BorderLayout.WEST);

        // Right: Login button (not logged in) OR Avatar circle (logged in)
        if (guestId == -1) {
            // ── Not logged in: show Login button ──────────────────────────────
            JButton loginBtn = makeAccentButton("⏻  Login", C_DANGER);
            loginBtn.addActionListener(e -> new LoginFrame(this));
            header.add(loginBtn, BorderLayout.EAST);
        } else {
            // ── Logged in: show circular avatar with first letter of full name ─
            String[] profile = profileController.getGuestProfile(guestId);
            String fullName = (profile != null && profile[0] != null && !profile[0].isBlank())
                    ? profile[0] : guestEmail;
            String initial = fullName.trim().substring(0, 1).toUpperCase();

            JButton avatarBtn = new JButton(initial) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // Shadow
                    g2.setColor(new Color(0, 0, 0, 40));
                    g2.fillOval(3, 3, getWidth() - 2, getHeight() - 2);
                    // Amber gradient circle
                    java.awt.GradientPaint gp = new java.awt.GradientPaint(
                            0, 0, new Color(210, 145, 50),
                            0, getHeight(), new Color(150, 90, 15));
                    g2.setPaint(gp);
                    g2.fillOval(0, 0, getWidth() - 2, getHeight() - 2);
                    // Hover glow
                    if (getModel().isRollover()) {
                        g2.setColor(new Color(255, 255, 255, 35));
                        g2.fillOval(0, 0, getWidth() - 2, getHeight() - 2);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            avatarBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
            avatarBtn.setForeground(Color.WHITE);
            avatarBtn.setFocusPainted(false);
            avatarBtn.setBorderPainted(false);
            avatarBtn.setContentAreaFilled(false);
            avatarBtn.setOpaque(false);
            avatarBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            avatarBtn.setPreferredSize(new Dimension(48, 48));
            avatarBtn.setToolTipText(fullName);

            avatarBtn.addActionListener(e -> showUserMenu(avatarBtn, fullName));
            header.add(avatarBtn, BorderLayout.EAST);
        }

        // Bottom accent line
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(header, BorderLayout.CENTER);
        JPanel accent = new JPanel();
        accent.setBackground(C_ACCENT);
        accent.setPreferredSize(new Dimension(0, 3));
        wrapper.add(accent, BorderLayout.SOUTH);

        add(wrapper, BorderLayout.NORTH);
    }

    /**
     * Shows the user account popup menu below the avatar button.
     * Contains: My Profile · History · Settings (Change Password, Delete Account) · Logout
     */
    private void showUserMenu(JButton anchor, String fullName) {
        JDialog menu = new JDialog(this, false);
        menu.setUndecorated(true);
        menu.setBackground(new Color(0, 0, 0, 0));

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(4, 4, getWidth() - 4, getHeight() - 4, 16, 16);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 16, 16);
                g2.setColor(new Color(220, 205, 180));
                g2.drawRoundRect(0, 0, getWidth() - 5, getHeight() - 5, 16, 16);
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        // ── Name header ────────────────────────────────────────────────────────
        JLabel nameLabel = new JLabel("  " + fullName);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setForeground(new Color(62, 38, 12));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 16, 8, 16));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(nameLabel);

        // Thin divider
        JSeparator sep1 = new JSeparator();
        sep1.setForeground(new Color(220, 205, 180));
        sep1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep1);
        card.add(Box.createVerticalStrut(6));

        // ── Menu items ─────────────────────────────────────────────────────────
        card.add(menuItem("👤  My Profile", () -> {
            menu.dispose();
            tabbedPane.setSelectedIndex(2); // My Profile tab
        }));
        card.add(menuItem("📋  Booking History", () -> {
            menu.dispose();
            tabbedPane.setSelectedIndex(1); // My Bookings tab
        }));

        // Settings sub-section
        card.add(Box.createVerticalStrut(4));
        JSeparator sep2 = new JSeparator();
        sep2.setForeground(new Color(220, 205, 180));
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep2);
        card.add(Box.createVerticalStrut(4));

        JLabel settingsHdr = new JLabel("  ⚙  Settings");
        settingsHdr.setFont(new Font("SansSerif", Font.BOLD, 11));
        settingsHdr.setForeground(new Color(140, 115, 90));
        settingsHdr.setBorder(BorderFactory.createEmptyBorder(2, 16, 4, 16));
        settingsHdr.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(settingsHdr);

        card.add(menuItem("    🔑  Change Password", () -> {
            menu.dispose();
            showChangePasswordDialog();
        }));
        card.add(menuItem("    🗑  Delete Account", () -> {
            menu.dispose();
            showDeleteAccountDialog();
        }));

        card.add(Box.createVerticalStrut(4));
        JSeparator sep3 = new JSeparator();
        sep3.setForeground(new Color(220, 205, 180));
        sep3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep3);
        card.add(Box.createVerticalStrut(6));

        // Logout
        card.add(menuItem("⏻  Logout", () -> {
            menu.dispose();
            dispose();
            new GuestDashboardGUI();
        }));

        menu.setContentPane(card);
        menu.pack();
        menu.setMinimumSize(new Dimension(220, menu.getHeight()));

        // Position below the avatar button
        java.awt.Point pt = anchor.getLocationOnScreen();
        menu.setLocation(pt.x + anchor.getWidth() - menu.getWidth(), pt.y + anchor.getHeight() + 4);

        // Close when clicking elsewhere
        menu.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent e) {}
            public void windowLostFocus(java.awt.event.WindowEvent e) { menu.dispose(); }
        });
        menu.setVisible(true);
    }

    /** Creates a styled hover-sensitive menu item button. */
    private JButton menuItem(String text, Runnable action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isRollover()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(252, 243, 228));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setForeground(new Color(40, 25, 8));
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

    /** Change Password dialog for guests. */
    private void showChangePasswordDialog() {
        JDialog dlg = new JDialog(this, "Change Password", true);
        dlg.setSize(380, 260);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(C_BG);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(C_BG);
        form.setBorder(BorderFactory.createEmptyBorder(18, 24, 10, 24));

        JPasswordField fCurrent = styledPassField();
        JPasswordField fNew = styledPassField();
        JPasswordField fConfirm = styledPassField();
        form.add(profileRow("Current Password", fCurrent));
        form.add(Box.createVerticalStrut(10));
        form.add(profileRow("New Password", fNew));
        form.add(Box.createVerticalStrut(10));
        form.add(profileRow("Confirm New Password", fConfirm));

        JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 8));
        footer.setBackground(C_BG);
        JButton save = makeAccentButton("🔑  Save", C_ACCENT_DARK);
        JButton cancel = makeAccentButton("Cancel", C_MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            String current = new String(fCurrent.getPassword());
            String newPass = new String(fNew.getPassword());
            String confirm = new String(fConfirm.getPassword());
            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) { error("Fill all password fields."); return; }
            if (newPass.length() < 6) { error("New password must be at least 6 characters."); return; }
            if (!newPass.equals(confirm)) { error("New passwords do not match."); return; }
            String result = profileController.changeGuestPassword(guestId, current, newPass);
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

    /** Delete Account confirmation dialog for guests. */
    private void showDeleteAccountDialog() {
        int confirm = javax.swing.JOptionPane.showConfirmDialog(this,
                "<html><b>Delete your account?</b><br><br>"
                + "This action is permanent and cannot be undone.<br>"
                + "All your bookings will remain in the system.</html>",
                "Delete Account", javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);
        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            AuthController auth = new AuthController();
            if (auth.deleteGuest(guestId)) {
                javax.swing.JOptionPane.showMessageDialog(this, "Your account has been deleted.");
                dispose();
                new GuestDashboardGUI();
            } else {
                error("Failed to delete account. Please try again.");
            }
        }
    }


    private JPanel buildPropertyTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(C_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        // Section heading
        JLabel heading = new JLabel("Available Heritage Properties");
        heading.setFont(F_SECTION);
        heading.setForeground(C_ACCENT_DARK);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 2, 14, 0));
        panel.add(heading, BorderLayout.NORTH);

        // ── Outer wrapper: arrow LEFT | card CENTER | arrow RIGHT ─────────────
        JPanel carouselWrapper = new JPanel(new BorderLayout(14, 0));
        carouselWrapper.setBackground(C_BG);

        // ── Card panel ────────────────────────────────────────────────────────
        carouselCardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 28));
                g2.fillRoundRect(6, 6, getWidth() - 6, getHeight() - 6, 22, 22);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 6, 22, 22);
                g2.dispose();
            }
        };
        carouselCardPanel.setLayout(new BoxLayout(carouselCardPanel, BoxLayout.Y_AXIS));
        carouselCardPanel.setOpaque(false);
        carouselCardPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 6));

        // ── Counter label row at the top of the card ───────────────────────────
        carouselCounterLabel = new JLabel("1 / 1", SwingConstants.RIGHT);
        carouselCounterLabel.setFont(F_SMALL);
        carouselCounterLabel.setForeground(C_MUTED);
        JPanel counterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 8));
        counterRow.setOpaque(false);
        counterRow.add(carouselCounterLabel);
        counterRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        counterRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        carouselCardPanel.add(counterRow);

        // ── Image area ─────────────────────────────────────────────────────
        carouselImageLabel = new JLabel("", SwingConstants.CENTER);
        carouselImageLabel.setOpaque(false);
        carouselImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel imgWrapper = new JPanel(new BorderLayout());
        imgWrapper.setOpaque(false);
        imgWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        imgWrapper.setMinimumSize(new Dimension(0, 260));
        imgWrapper.setPreferredSize(new Dimension(600, 260));
        imgWrapper.add(carouselImageLabel, BorderLayout.CENTER);
        carouselCardPanel.add(imgWrapper);

        // ── Text content ─────────────────────────────────────────────────────
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.setBorder(BorderFactory.createEmptyBorder(16, 28, 20, 28));
        textPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        carouselNameLabel = new JLabel("Loading...");
        carouselNameLabel.setFont(new Font("SansSerif", Font.BOLD, 19));
        carouselNameLabel.setForeground(C_TEXT);
        carouselNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        carouselPriceLabel = new JLabel("RM — / night");
        carouselPriceLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        carouselPriceLabel.setForeground(C_ACCENT);
        carouselPriceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        carouselDescLabel = new JLabel("<html><body style='width:480px; color:#7a6a55; font-size:10px;'>—</body></html>");
        carouselDescLabel.setFont(F_SMALL);
        carouselDescLabel.setForeground(C_MUTED);
        carouselDescLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(carouselNameLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(carouselPriceLabel);
        textPanel.add(Box.createVerticalStrut(10));
        textPanel.add(carouselDescLabel);
        textPanel.add(Box.createVerticalStrut(20));

        // ── Book Now button ───────────────────────────────────────────────────
        carouselBookBtn = new JButton("Book Now") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = isEnabled()
                        ? (getModel().isRollover() ? C_ACCENT : C_ACCENT_DARK)
                        : C_MUTED;
                GradientPaint gp = new GradientPaint(0, 0, base.brighter(), 0, getHeight(), base.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(255, 255, 255, 28));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        carouselBookBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        carouselBookBtn.setForeground(Color.WHITE);
        carouselBookBtn.setFocusPainted(false);
        carouselBookBtn.setBorderPainted(false);
        carouselBookBtn.setContentAreaFilled(false);
        carouselBookBtn.setOpaque(false);
        carouselBookBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        carouselBookBtn.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        carouselBookBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        carouselBookBtn.setMaximumSize(new Dimension(200, 44));
        carouselBookBtn.addActionListener(e -> onCarouselBookNow());
        textPanel.add(carouselBookBtn);
        carouselCardPanel.add(textPanel);

        // ── Left / Right arrow buttons (vertically centred beside the card) ─────
        carouselPrevBtn = makeCarouselArrow("‹");
        carouselNextBtn = makeCarouselArrow("›");

        carouselPrevBtn.addActionListener(e -> {
            if (carouselProperties.isEmpty()) {
                return;
            }
            carouselIndex = (carouselIndex - 1 + carouselProperties.size()) % carouselProperties.size();
            updateCarouselCard();
        });
        carouselNextBtn.addActionListener(e -> {
            if (carouselProperties.isEmpty()) {
                return;
            }
            carouselIndex = (carouselIndex + 1) % carouselProperties.size();
            updateCarouselCard();
        });

        // Wrap each arrow in a panel that centres it vertically
        JPanel prevWrapper = new JPanel(new GridBagLayout());
        prevWrapper.setBackground(C_BG);
        prevWrapper.setPreferredSize(new Dimension(72, 0));
        prevWrapper.add(carouselPrevBtn);

        JPanel nextWrapper = new JPanel(new GridBagLayout());
        nextWrapper.setBackground(C_BG);
        nextWrapper.setPreferredSize(new Dimension(72, 0));
        nextWrapper.add(carouselNextBtn);

        carouselWrapper.add(prevWrapper,      BorderLayout.WEST);
        carouselWrapper.add(carouselCardPanel, BorderLayout.CENTER);
        carouselWrapper.add(nextWrapper,      BorderLayout.EAST);
        panel.add(carouselWrapper, BorderLayout.CENTER);

        // Hidden property model (data store only)
        String[] cols = {"No.", "Property Name", "Daily Rate (RM)", "Status", ""};
        propertyModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        propertyTable = new JTable(propertyModel);
        return panel;
    }
    
    /**
     * Creates a circular arrow navigation button for the carousel.
     */
    private JButton makeCarouselArrow(String symbol) {
        JButton b = new JButton(symbol) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? C_ACCENT : new Color(62, 38, 12);
                g2.setColor(bg);
                g2.fillOval(0, 0, getWidth(), getHeight());
                // Subtle inner glow ring on hover
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 40));
                    g2.fillOval(4, 4, getWidth() - 8, getHeight() - 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 30));
        b.setForeground(new Color(255, 230, 170));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(58, 58));
        return b;
    }

    /**
     * Refreshes all card UI elements to match the current carousel index.
     */
    private void updateCarouselCard() {
        if (carouselProperties.isEmpty()) {
            carouselNameLabel.setText("No properties available");
            carouselPriceLabel.setText("");
            carouselDescLabel.setText("<html><body style='width:480px;'>—</body></html>");
            carouselBookBtn.setEnabled(false);
            carouselBookBtn.setText("Book Now");
            carouselCounterLabel.setText("0 / 0");
            carouselImageLabel.setIcon(null);
            carouselImageLabel.setText("No properties");
            return;
        }
        HeritageProperty p = carouselProperties.get(carouselIndex);
        carouselCounterLabel.setText((carouselIndex + 1) + " / " + carouselProperties.size());
        carouselNameLabel.setText(p.getPropertyName());
        carouselPriceLabel.setText(String.format("RM %.2f / night", p.getDailyRate()));
        String desc = generatePropertyDescription(p);
        carouselDescLabel.setText("<html><body style='width:480px; color:#7a6a55;'>" + desc + "</body></html>");
        // Only disable the button if the property is under maintenance
        boolean maintenance = "Under Maintenance".equals(p.getCurrentStatus());
        if (maintenance) {
            carouselBookBtn.setEnabled(false);
            carouselBookBtn.setText("Under Maintenance");
        } else {
            carouselBookBtn.setEnabled(true);
            carouselBookBtn.setText("Book Now");
        }
        // Scale image to the wider card
        carouselImageLabel.setIcon(buildPropertyImage(p));
        carouselImageLabel.setText("");
        carouselCardPanel.revalidate();
        carouselCardPanel.repaint();
    }

    /**
     * Returns a short descriptive sentence for the property.
     */
    private String generatePropertyDescription(HeritageProperty p) {
        String[] descs = {
            "A beautifully preserved heritage homestay offering an immersive cultural retreat with warm, authentic charm.",
            "Step into history at this stunning traditional residence, lovingly restored to blend heritage with modern comfort.",
            "Experience tranquil heritage living — hand-crafted details, lush surroundings, and genuine local hospitality.",
            "A timeless sanctuary steeped in culture, perfect for travellers seeking calm, character, and connection.",
            "Discover the soul of local heritage in this exquisite homestay, where every corner tells a story."
        };
        return descs[Math.abs(p.getPropertyName().hashCode()) % descs.length];
    }

    /**
     * Loads the image for a property dynamically using multiple fallback strategies.
     * This guarantees the image shows up whether it was uploaded, added to the
     * resources folder, or if VS Code hasn't synced the target folder yet.
     */
    private ImageIcon buildPropertyImage(HeritageProperty p) {
        int w = 600, h = 260;
        java.awt.image.BufferedImage src = null;

        String imgPath = p.getImagePath();
        String propName = p.getPropertyName();
        
        // Compile a list of possible filenames to look for
        java.util.List<String> possibleNames = new java.util.ArrayList<>();
        
        // 1. The exact path from the database
        if (imgPath != null && !imgPath.isBlank()) {
            possibleNames.add(imgPath);
        }
        
        // 2. Guess based on property name (e.g., "UTeM.png" or "UTeM.jpg")
        String safeName = propName.replaceAll("[^a-zA-Z0-9]", "_");
        possibleNames.add(propName + ".png");
        possibleNames.add(propName + ".jpg");
        possibleNames.add(safeName + ".png");
        possibleNames.add(safeName + ".jpg");

        // 3. The default fallbacks
        String[] defaults = {"homestay1.jpg", "homestay2.jpg", "Homestay3.jpg"};
        possibleNames.add(defaults[Math.max(0, (p.getPropertyId() - 1) % defaults.length)]);

        // Search through the possible names
        for (String name : possibleNames) {
            // Strategy A: Check the relative uploaded_images folder
            try {
                String projectDir = System.getProperty("user.dir");
                java.io.File imgFile = new java.io.File(projectDir + java.io.File.separator + "uploaded_images", name);
                if (imgFile.exists()) {
                    src = javax.imageio.ImageIO.read(imgFile);
                    if (src != null) break;
                }
            } catch (Exception ignored) {}

            // Strategy B: Check the raw src folder (Fixes VS Code not syncing to target/classes)
            if (src == null) {
                try {
                    String projectDir = System.getProperty("user.dir");
                    java.io.File devFile = new java.io.File(projectDir + "/src/main/resources/images/" + name);
                    if (devFile.exists()) {
                        src = javax.imageio.ImageIO.read(devFile);
                        if (src != null) break;
                    }
                } catch (Exception ignored) {}
            }

            // Strategy C: Check the internal Classpath
            if (src == null) {
                try (java.io.InputStream is = getClass().getResourceAsStream("/images/" + name)) {
                    if (is != null) {
                        src = javax.imageio.ImageIO.read(is);
                        if (src != null) break;
                    }
                } catch (Exception ignored) {}
            }
            
            if (src != null) break; // Break out of the loop if we found an image
        }

        // Scale and Draw the Image
        if (src != null) {
            try {
                double scaleX = (double) w / src.getWidth();
                double scaleY = (double) h / src.getHeight();
                double scale = Math.max(scaleX, scaleY);
                int sw = (int) (src.getWidth() * scale);
                int sh = (int) (src.getHeight() * scale);
                java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(w, h,
                        java.awt.image.BufferedImage.TYPE_INT_RGB);
                Graphics2D sg = scaled.createGraphics();
                sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                sg.drawImage(src, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
                drawStatusBadge(sg, w, p);
                sg.dispose();
                return new ImageIcon(scaled);
            } catch (Exception ignored) { /* fall through to gradient */ }
        }
        
        // Gradient Fallback (If no image is found anywhere)
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        float hue = (0.06f + p.getPropertyId() * 0.13f) % 1f;
        Color top = Color.getHSBColor(hue, 0.38f, 0.62f);
        Color bottom = Color.getHSBColor((hue + 0.05f) % 1f, 0.55f, 0.35f);
        g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g2.fillRect(0, 0, w, h);
        g2.setColor(new Color(0, 0, 0, 35));
        g2.fillRoundRect(w / 2 - 55, 25, 110, 130, 55, 55);
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fillRoundRect(w / 2 - 42, 40, 84, 100, 42, 42);
        g2.setColor(new Color(255, 230, 150, 130));
        g2.fillOval(w / 2 - 16, 68, 13, 13);
        g2.fillOval(w / 2 + 5, 68, 13, 13);
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillRect(0, h - 38, w, 38);
        drawStatusBadge(g2, w, p);
        g2.dispose();
        return new ImageIcon(img);
    }

    /**
     * Paints a coloured availability badge in the top-right of the image.
     * Text is centered inside the pill using FontMetrics.
     */
    private void drawStatusBadge(Graphics2D g2, int w, HeritageProperty p) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean maintenance = "Under Maintenance".equals(p.getCurrentStatus());
        Font badgeFont = new Font("SansSerif", Font.BOLD, 14);
        g2.setFont(badgeFont);
        FontMetrics fm = g2.getFontMetrics();
        if (maintenance) {
            String text = "Under Maintenance";
            int pillW = fm.stringWidth(text) + 24;
            int pillH = 30;
            int pillX = w - pillW - 10;
            int pillY = 10;
            g2.setColor(new Color(190, 115, 0, 220));
            g2.fillRoundRect(pillX, pillY, pillW, pillH, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString(text, pillX + (pillW - fm.stringWidth(text)) / 2, pillY + pillH - (pillH - fm.getAscent()) / 2 - 2);
        } else {
            String text = "Available";
            int pillW = fm.stringWidth(text) + 24;
            int pillH = 30;
            int pillX = w - pillW - 10;
            int pillY = 10;
            g2.setColor(new Color(46, 130, 69, 220));
            g2.fillRoundRect(pillX, pillY, pillW, pillH, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString(text, pillX + (pillW - fm.stringWidth(text)) / 2, pillY + pillH - (pillH - fm.getAscent()) / 2 - 2);
        }
    }

    /**
     * Triggered when the Book Now button is clicked in the carousel.
     */
    private void onCarouselBookNow() {
        if (carouselProperties.isEmpty()) {
            return;
        }
        if (guestId == -1) {
            JOptionPane.showMessageDialog(this, "Please login to book a property.", "Login Required", JOptionPane.INFORMATION_MESSAGE);
            new LoginFrame(this);
            return;
        }
        HeritageProperty p = carouselProperties.get(carouselIndex);
        showCalendarPopup(p.getPropertyId(), p.getPropertyName(), p.getDailyRate());
    }

    // ── Book Button Renderer ──────────────────────────────────────────────────
    class BookButtonRenderer extends JPanel implements TableCellRenderer {

        private final JButton button;

        BookButtonRenderer() {
            setLayout(new GridBagLayout());
            setOpaque(true);
            button = makePillButton("Book Stay", C_ACCENT_DARK);
            button.setPreferredSize(new Dimension(112, 28));
            add(button);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            String status = (String) table.getModel().getValueAt(row, 3);
            boolean maintenance = "Under Maintenance".equals(status);
            if (isSelected) {
                setBackground(C_SEL);
            } else if (maintenance) {
                setBackground(new Color(255, 243, 215));
            } else {
                setBackground(row % 2 == 0 ? Color.WHITE : C_TABLE_ALT);
            }
            button.setEnabled(!maintenance);
            button.setBackground(maintenance ? C_MUTED : C_ACCENT_DARK);
            button.setText(maintenance ? "Maintenance" : "Book Stay");
            return this;
        }
    }

    class BookButtonEditor extends DefaultCellEditor {

        private final JPanel panel;
        private final JButton button;
        private boolean isClicked;
        private int selectedRowIdx;

        public BookButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            panel = new JPanel(new GridBagLayout());
            panel.setOpaque(true);
            button = makePillButton("Book Stay", C_ACCENT_DARK);
            button.setPreferredSize(new Dimension(112, 28));
            button.addActionListener(e -> fireEditingStopped());
            panel.add(button);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            selectedRowIdx = row;
            isClicked = true;
            String status = (String) table.getModel().getValueAt(row, 3);
            boolean maintenance = "Under Maintenance".equals(status);
            panel.setBackground(maintenance ? new Color(255, 243, 215) : (row % 2 == 0 ? Color.WHITE : C_TABLE_ALT));
            button.setEnabled(!maintenance);
            button.setBackground(maintenance ? C_MUTED : C_ACCENT_DARK);
            button.setText(maintenance ? "Maintenance" : "Book Stay");
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            if (isClicked) {
                if (guestId == -1) {
                    JOptionPane.showMessageDialog(panel, "Please login to book a property.", "Login Required", JOptionPane.INFORMATION_MESSAGE);
                    new LoginFrame(GuestDashboardGUI.this);
                } else {
                    int propId = (int) propertyModel.getValueAt(selectedRowIdx, 0);
                    String propName = (String) propertyModel.getValueAt(selectedRowIdx, 1);
                    double dailyRate = Double.parseDouble(propertyModel.getValueAt(selectedRowIdx, 2).toString());
                    showCalendarPopup(propId, propName, dailyRate);
                }
            }
            isClicked = false;
            return "Book Stay";
        }

        @Override
        public boolean stopCellEditing() {
            isClicked = false;
            return super.stopCellEditing();
        }
    }

    // ── Booking Calendar Dialog ───────────────────────────────────────────────
    private void showCalendarPopup(int propertyId, String propertyName, double dailyRate) {
        loadBookedDates(propertyId);
        selectedCheckIn = null;
        selectedCheckOut = null;

        JDialog dialog = new JDialog(this, "Book  ·  " + propertyName, true);
        dialog.setSize(480, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.getContentPane().setBackground(C_BG);

        // ── Top: property title + date pills ─────────────────────────────────
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(C_BG);
        topPanel.setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 16));

        JLabel propLabel = new JLabel("🏡  " + propertyName);
        propLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        propLabel.setForeground(C_ACCENT_DARK);
        propLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(propLabel);
        topPanel.add(Box.createVerticalStrut(10));

        JPanel dateRow = new JPanel(new GridLayout(1, 2, 10, 0));
        dateRow.setBackground(C_BG);
        dateRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        checkInLabel = makeDatePill("Check-in", "Not selected", new Color(46, 130, 69));
        checkOutLabel = makeDatePill("Check-out", "Not selected", new Color(50, 120, 190));
        dateRow.add(checkInLabel);
        dateRow.add(checkOutLabel);
        topPanel.add(dateRow);
        dialog.add(topPanel, BorderLayout.NORTH);

        // ── Calendar ─────────────────────────────────────────────────────────
        CalendarPanel cal = new CalendarPanel();
        JPanel calWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        calWrapper.setBackground(C_BG);
        cal.setPreferredSize(new Dimension(420, 240));
        calWrapper.add(cal);
        dialog.add(calWrapper, BorderLayout.CENTER);

        // ── Footer: legend + confirm ──────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout(0, 0));
        footer.setBackground(C_BG);
        footer.setBorder(BorderFactory.createEmptyBorder(4, 14, 14, 14));

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        legend.setBackground(C_BG);
        legend.add(legendDot(new Color(220, 80, 60), "Booked"));
        legend.add(legendDot(new Color(180, 100, 0), "Maint."));
        legend.add(legendDot(new Color(46, 130, 69), "Check-in"));
        legend.add(legendDot(new Color(50, 120, 190), "Check-out"));

        JButton confirmBtn = makeAccentButton("✔  Confirm Booking", C_SUCCESS);
        confirmBtn.setFont(F_BOLD);

        confirmBtn.addActionListener(e -> {
            if (selectedCheckIn == null || selectedCheckOut == null) {
                warn("Please select both check-in and check-out dates.");
                return;
            }
            long days = ChronoUnit.DAYS.between(selectedCheckIn, selectedCheckOut);
            double total = days * dailyRate;

            int confirm = JOptionPane.showConfirmDialog(dialog,
                    String.format(
                            "<html><b>Booking Summary</b><br><br>"
                            + "Property : %s<br>"
                            + "Check-in : %s<br>"
                            + "Check-out: %s<br>"
                            + "Duration : %d night(s)<br><br>"
                            + "<b>Total: RM %.2f</b><br><br>Confirm booking?</html>",
                            propertyName, selectedCheckIn, selectedCheckOut, days, total),
                    "Confirm Booking", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                String result = bookingController.createBooking(propertyId, guestId,
                        Date.valueOf(selectedCheckIn), Date.valueOf(selectedCheckOut), dailyRate);
                switch (result) {
                    case "SUCCESS" -> {
                        info("Booking confirmed! Enjoy your heritage stay.");
                        dialog.dispose();
                        loadProperties();
                        loadMyBookings();
                        tabbedPane.setSelectedIndex(1);
                    }
                    case "CONFLICT_BOOKING" ->
                        warn("These dates overlap with an existing booking.");
                    case "CONFLICT_MAINTENANCE" ->
                        warn("These dates overlap with scheduled maintenance.");
                    default ->
                        error("Booking failed. Please try again.");
                }
            }
        });

        footer.add(legend, BorderLayout.WEST);
        footer.add(confirmBtn, BorderLayout.EAST);
        dialog.add(footer, BorderLayout.SOUTH);

        updateDateLabels();
        cal.refresh();
        dialog.setVisible(true);
    }

    /**
     * Returns a styled label that looks like a pill date display.
     */
    private JLabel makeDatePill(String type, String value, Color accent) {
        JLabel lbl = new JLabel("<html><font color='#888888' size='2'>" + type
                + "</font><br><b>" + value + "</b></html>") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(F_BODY);
        lbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        lbl.setOpaque(false);
        return lbl;
    }

    // ── Tab 2: My Bookings ────────────────────────────────────────────────────
    private JPanel buildBookingsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(C_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel heading = new JLabel("My Bookings");
        heading.setFont(F_SECTION);
        heading.setForeground(C_ACCENT_DARK);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 2, 10, 0));
        panel.add(heading, BorderLayout.NORTH);

        String[] cols = {"Booking ID", "Property", "Check-in", "Check-out", "Total (RM)"};
        bookingModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        bookingTable = new JTable(bookingModel);
        bookingTable.setRowHeight(38);
        bookingTable.setShowVerticalLines(true);
        bookingTable.setIntercellSpacing(new Dimension(1, 1));
        bookingTable.setFont(F_BODY);
        styleTable(bookingTable);
        styleTableHeader(bookingTable);

        // Per-column renderers with alignment:
        //  Col 0: Booking ID   -> CENTER
        //  Col 1: Property     -> CENTER
        //  Col 2: Check-in     -> CENTER
        //  Col 3: Check-out    -> CENTER
        //  Col 4: Total (RM)   -> CENTER
        DefaultTableCellRenderer bCenter = makeBookingCellRenderer(SwingConstants.CENTER);
        for (int i = 0; i < cols.length; i++) {
            bookingTable.getColumnModel().getColumn(i).setCellRenderer(bCenter);
        }

        // Column widths
        bookingTable.getColumnModel().getColumn(0).setMinWidth(80);
        bookingTable.getColumnModel().getColumn(0).setMaxWidth(110);
        bookingTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        bookingTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        bookingTable.getColumnModel().getColumn(2).setMinWidth(110);
        bookingTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        bookingTable.getColumnModel().getColumn(3).setMinWidth(110);
        bookingTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        bookingTable.getColumnModel().getColumn(4).setMinWidth(110);
        bookingTable.getColumnModel().getColumn(4).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(bookingTable);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);

        // Button row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        btnRow.setBackground(C_BG);

        JButton cancelBtn = makeAccentButton("✖  Cancel Selected Booking", C_DANGER);
        JButton refreshBtn = makeAccentButton("↻  Refresh", C_MUTED);
        cancelBtn.addActionListener(e -> cancelSelectedBooking());
        refreshBtn.addActionListener(e -> loadMyBookings());

        btnRow.add(cancelBtn);
        btnRow.add(refreshBtn);
        panel.add(btnRow, BorderLayout.SOUTH);
        return panel;
    }

    // ── Tab 3: Profile ────────────────────────────────────────────────────────
    private JPanel buildProfileTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(C_BG);
        outer.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        // Scroll pane wrapper for the card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(C_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(C_BORDER, 1, true),
                BorderFactory.createEmptyBorder(28, 36, 28, 36)
        ));

        // ── Personal Information ──────────────────────────────────────────────
        card.add(sectionHeader("👤  Personal Information"));
        card.add(Box.createVerticalStrut(14));

        JTextField fName = styledField();
        JTextField fEmail = styledField();
        JTextField fContact = styledField();

        card.add(profileRow("Full Name", fName));
        card.add(Box.createVerticalStrut(10));
        card.add(profileRow("Email Address", fEmail));
        card.add(Box.createVerticalStrut(10));
        card.add(profileRow("Contact Number", fContact));
        card.add(Box.createVerticalStrut(18));

        JButton saveInfoBtn = makeAccentButton("💾  Save Changes", C_ACCENT_DARK);
        saveInfoBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveInfoBtn.setMaximumSize(new Dimension(200, 36));
        card.add(saveInfoBtn);

        // Divider
        card.add(Box.createVerticalStrut(26));
        card.add(sectionDivider());
        card.add(Box.createVerticalStrut(22));

        // ── Change Password ───────────────────────────────────────────────────
        card.add(sectionHeader("🔒  Change Password"));
        card.add(Box.createVerticalStrut(14));

        JPasswordField fCurrent = styledPassField();
        JPasswordField fNew = styledPassField();
        JPasswordField fConfirm = styledPassField();

        card.add(profileRow("Current Password", fCurrent));
        card.add(Box.createVerticalStrut(10));
        card.add(profileRow("New Password", fNew));
        card.add(Box.createVerticalStrut(10));
        card.add(profileRow("Confirm New Password", fConfirm));
        card.add(Box.createVerticalStrut(18));

        JButton savePassBtn = makeAccentButton("🔑  Change Password", C_ACCENT_DARK);
        savePassBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        savePassBtn.setMaximumSize(new Dimension(200, 36));
        card.add(savePassBtn);

        JScrollPane scrollCard = new JScrollPane(card);
        scrollCard.setBorder(null);
        scrollCard.getVerticalScrollBar().setUnitIncrement(12);
        outer.add(scrollCard, BorderLayout.CENTER);

        // ── Event Listeners ───────────────────────────────────────────────────
        saveInfoBtn.addActionListener(e -> {
            String name = fName.getText().trim();
            String email = fEmail.getText().trim();
            String contact = fContact.getText().trim();

            if (name.isEmpty() || email.isEmpty()) {
                error("Name and email cannot be empty.");
                return;
            }
            if (!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$", email)) {
                error("Invalid email format.");
                return;
            }
            if (!contact.isEmpty() && !Pattern.matches("^[0-9+\\s-]+$", contact)) {
                error("Contact number can only contain digits, spaces, hyphens (-), or (+).");
                return;
            }
            String result = profileController.updateGuestProfile(guestId, name, email, contact);
            switch (result) {
                case "SUCCESS" -> {
                    this.guestEmail = email;
                    info("Profile updated successfully.");
                }
                case "NAME_TAKEN" ->
                    error("This name is already in use by another account. Please choose a different name.");
                case "EMAIL_TAKEN" ->
                    error("This email is already in use by another account.");
                default ->
                    error("Failed to update profile. Please try again.");
            }
        });

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

            String result = profileController.changeGuestPassword(guestId, current, newPass);
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
                    error("Failed to change password. Please try again.");
            }
        });

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

            public void ancestorRemoved(javax.swing.event.AncestorEvent e) {
            }

            public void ancestorMoved(javax.swing.event.AncestorEvent e) {
            }
        });

        return outer;
    }

    // ── Calendar Panel Component ──────────────────────────────────────────────
    class CalendarPanel extends JPanel {

        private YearMonth displayMonth;
        private JLabel monthLabel;
        private JPanel daysGrid;

        CalendarPanel() {
            this.displayMonth = YearMonth.now();
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
            setLayout(new BorderLayout(0, 0));

            // Nav bar
            JPanel nav = new JPanel(new BorderLayout());
            nav.setBackground(C_HEADER_TOP);
            nav.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

            JButton prev = calNavBtn("‹");
            JButton next = calNavBtn("›");
            monthLabel = new JLabel("", SwingConstants.CENTER);
            monthLabel.setForeground(new Color(255, 240, 195));
            monthLabel.setFont(F_BOLD);

            prev.addActionListener(e -> {
                displayMonth = displayMonth.minusMonths(1);
                refresh();
            });
            next.addActionListener(e -> {
                displayMonth = displayMonth.plusMonths(1);
                refresh();
            });

            nav.add(prev, BorderLayout.WEST);
            nav.add(monthLabel, BorderLayout.CENTER);
            nav.add(next, BorderLayout.EAST);
            add(nav, BorderLayout.NORTH);

            // Day-of-week headers
            JPanel headers = new JPanel(new GridLayout(1, 7));
            headers.setBackground(new Color(80, 52, 18));
            for (String d : new String[]{"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"}) {
                JLabel lbl = new JLabel(d, SwingConstants.CENTER);
                lbl.setFont(F_SMALL);
                lbl.setForeground(new Color(210, 185, 145));
                lbl.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
                headers.add(lbl);
            }
            add(headers, BorderLayout.CENTER);

            daysGrid = new JPanel(new GridLayout(0, 7, 2, 2));
            daysGrid.setBackground(new Color(240, 236, 228));
            daysGrid.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            add(daysGrid, BorderLayout.SOUTH);
            refresh();
        }

        private JButton calNavBtn(String text) {
            JButton b = new JButton(text);
            b.setBackground(new Color(80, 52, 18));
            b.setForeground(new Color(255, 220, 150));
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setFont(new Font("SansSerif", Font.BOLD, 18));
            b.setPreferredSize(new Dimension(36, 26));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }

        void refresh() {
            monthLabel.setText(displayMonth.format(DateTimeFormatter.ofPattern("MMMM  yyyy")));
            daysGrid.removeAll();

            LocalDate first = displayMonth.atDay(1);
            int startDow = first.getDayOfWeek().getValue() % 7;
            LocalDate today = LocalDate.now();

            for (int i = 0; i < startDow; i++) {
                JLabel blank = new JLabel();
                blank.setOpaque(true);
                blank.setBackground(new Color(248, 244, 238));
                daysGrid.add(blank);
            }

            for (int day = 1; day <= displayMonth.lengthOfMonth(); day++) {
                LocalDate date = displayMonth.atDay(day);
                JButton btn = new JButton(String.valueOf(day));
                btn.setFont(F_SMALL);
                btn.setFocusPainted(false);
                btn.setBorderPainted(false);
                btn.setMargin(new Insets(0, 0, 0, 0));
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btn.setOpaque(true);

                boolean isPast = date.isBefore(today);
                boolean isBooked = bookedDates.contains(date);
                boolean isMaintenance = maintenanceDates.contains(date);
                boolean isCIDay = date.equals(selectedCheckIn);
                boolean isCODay = date.equals(selectedCheckOut);
                boolean inRange = selectedCheckIn != null && selectedCheckOut != null
                        && date.isAfter(selectedCheckIn) && date.isBefore(selectedCheckOut);

                if (isCIDay) {
                    btn.setBackground(new Color(46, 130, 69));
                    btn.setForeground(Color.WHITE);
                    btn.setFont(F_BOLD);
                } else if (isCODay) {
                    btn.setBackground(new Color(50, 120, 190));
                    btn.setForeground(Color.WHITE);
                    btn.setFont(F_BOLD);
                } else if (inRange) {
                    btn.setBackground(new Color(195, 235, 205));
                    btn.setForeground(new Color(30, 90, 40));
                } else if (isBooked) {
                    btn.setBackground(new Color(220, 80, 60));
                    btn.setForeground(Color.WHITE);
                    btn.setEnabled(false);
                } else if (isMaintenance) {
                    btn.setBackground(new Color(190, 115, 0));
                    btn.setForeground(Color.WHITE);
                    btn.setEnabled(false);
                } else if (isPast) {
                    btn.setBackground(new Color(238, 234, 226));
                    btn.setForeground(new Color(190, 180, 165));
                    btn.setEnabled(false);
                } else {
                    btn.setBackground(Color.WHITE);
                    btn.setForeground(C_TEXT);
                }

                if (!isPast && !isBooked && !isMaintenance) {
                    final LocalDate d = date;
                    btn.addActionListener(ev -> onDateClicked(d));
                }
                daysGrid.add(btn);
            }
            daysGrid.revalidate();
            daysGrid.repaint();
        }

        private void onDateClicked(LocalDate date) {
            if (selectedCheckIn == null || (selectedCheckIn != null && selectedCheckOut != null)) {
                selectedCheckIn = date;
                selectedCheckOut = null;
            } else {
                if (date.isBefore(selectedCheckIn) || date.equals(selectedCheckIn)) {
                    selectedCheckIn = date; 
                }else {
                    selectedCheckOut = date;
                }
            }
            updateDateLabels();
            refresh();
        }
    }

    // ── Data Sync ─────────────────────────────────────────────────────────────
    private void refreshMyBookedProperties() {
        myBookedPropertyIds = (guestId > 0)
                ? bookingController.getActivePropertyIdsForGuest(guestId)
                : new HashSet<>();
    }

    private void loadProperties() {
        refreshMyBookedProperties();
        propertyModel.setRowCount(0);
        carouselProperties.clear();
        carouselIndex = 0;
        for (HeritageProperty p : propertyController.getAllActiveProperties()) {
            carouselProperties.add(p);
            String displayStatus;
            if (myBookedPropertyIds.contains(p.getPropertyId())) {
                displayStatus = "Booked by You"; 
            }else {
                displayStatus = p.getCurrentStatus().equals("Under Maintenance") ? "Under Maintenance" : "Available";
            }

            propertyModel.addRow(new Object[]{
                p.getPropertyId(),
                p.getPropertyName(),
                String.format("%.2f", p.getDailyRate()),
                displayStatus,
                "Book Stay"
            });
        }
        if (carouselCardPanel != null) {
            updateCarouselCard();
        }
    }

    private void loadMyBookings() {
        bookingModel.setRowCount(0);
        if (guestId < 0) {
            return;
        }
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
        for (Booking b : bookingController.getAllActiveBookings()) {
            if (b.getPropertyId() == propertyId) {
                LocalDate s = b.getStartDate().toLocalDate();
                LocalDate e = b.getEndDate().toLocalDate();
                for (LocalDate d = s; !d.isAfter(e.minusDays(1)); d = d.plusDays(1)) {
                    bookedDates.add(d);
                }
            }
        }
        for (com.project.model.Maintenance m : maintenanceController.getMaintenanceByProperty(propertyId)) {
            if (!m.getStatus().equals("Completed") && !m.getStatus().equals("Cancelled")) {
                LocalDate s = m.getStartDate().toLocalDate();
                LocalDate e = m.getEndDate().toLocalDate();
                for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
                    maintenanceDates.add(d);
                }
            }
        }
    }

    private void updateDateLabels() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        if (checkInLabel != null) {
            String ci = selectedCheckIn != null ? selectedCheckIn.format(fmt) : "Not selected";
            String co = selectedCheckOut != null ? selectedCheckOut.format(fmt) : "Not selected";
            checkInLabel.setText("<html><font color='#666666' size='2'>Check-in</font><br><b>" + ci + "</b></html>");
            checkOutLabel.setText("<html><font color='#666666' size='2'>Check-out</font><br><b>" + co + "</b></html>");
        }
    }

    private void cancelSelectedBooking() {
        int row = bookingTable.getSelectedRow();
        if (row < 0) {
            warn("Please select a booking to cancel.");
            return;
        }
        int bookingId = (int) bookingModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this,
                "Cancel booking #" + bookingId + "?",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (bookingController.cancelBooking(bookingId)) {
                info("Booking cancelled successfully.");
                loadMyBookings();
                loadProperties();
            } else {
                error("Failed to cancel booking.");
            }
        }
    }

    // ── Shared UI Helpers ─────────────────────────────────────────────────────
    /**
     * Styled accent button with hover effect.
     */
    private JButton makeAccentButton(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE);
        b.setFont(F_BOLD);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        return b;
    }

    /**
     * Small pill-style button for table rows.
     */
    private JButton makePillButton(String text, Color bg) {
        JButton b = makeAccentButton(text, bg);
        b.setFont(F_SMALL);
        b.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        return b;
    }

    /**
     * Creates a striped property-table cell renderer with the given horizontal
     * alignment. Applies maintenance highlight colouring and alternating row
     * stripes.
     */
    private DefaultTableCellRenderer makePropertyCellRenderer(int hAlign) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                setFont(F_BODY);
                setHorizontalAlignment(hAlign);
                String status = (String) propertyModel.getValueAt(r, 3);
                if (sel) {
                    setBackground(C_SEL);
                    setForeground(C_TEXT);
                } else if ("Under Maintenance".equals(status)) {
                    setBackground(new Color(255, 243, 215));
                    setForeground(new Color(160, 80, 0));
                } else {
                    setBackground(r % 2 == 0 ? Color.WHITE : C_TABLE_ALT);
                    setForeground(C_TEXT);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        };
    }

    /**
     * Striped cell renderer for the My Bookings table with configurable
     * alignment.
     */
    private DefaultTableCellRenderer makeBookingCellRenderer(int hAlign) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                setFont(F_BODY);
                setHorizontalAlignment(hAlign);
                setBackground(sel ? C_SEL : (r % 2 == 0 ? Color.WHITE : C_TABLE_ALT));
                setForeground(C_TEXT);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        };
    }

    private void styleTable(JTable t) {
        t.setSelectionBackground(C_SEL);
        t.setSelectionForeground(C_TEXT);
        t.setGridColor(new Color(190, 170, 140));   // warm visible grid line
        t.setBackground(Color.WHITE);
    }

    private void styleTableHeader(JTable t) {
        Color hdrBg = new Color(235, 220, 195);   // light warm sand
        Color hdrFg = new Color(80, 48, 10);    // dark amber text
        Color hdrLine = new Color(180, 140, 80);     // amber underline
        t.getTableHeader().setBackground(hdrBg);
        t.getTableHeader().setForeground(hdrFg);
        t.getTableHeader().setFont(F_HDR);
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, hdrLine));
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(hdrBg);
                setForeground(hdrFg);
                setFont(F_HDR);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, hdrLine),
                        BorderFactory.createEmptyBorder(4, 6, 4, 6)
                ));
                return this;
            }
        });
    }

    private JLabel sectionHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(F_SECTION);
        lbl.setForeground(C_ACCENT_DARK);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private Component sectionDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(C_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    private JPanel profileRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(C_CARD);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(labelText);
        lbl.setPreferredSize(new Dimension(180, 32));
        lbl.setFont(F_BODY);
        lbl.setForeground(C_MUTED);
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JTextField styledField() {
        JTextField f = new JTextField();
        f.setFont(F_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return f;
    }

    private JPasswordField styledPassField() {
        JPasswordField f = new JPasswordField();
        f.setFont(F_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return f;
    }

    private JPanel legendDot(Color color, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setBackground(C_BG);
        JLabel dot = new JLabel("  ");
        dot.setOpaque(true);
        dot.setBackground(color);
        dot.setPreferredSize(new Dimension(10, 10));
        JLabel lbl = new JLabel(text);
        lbl.setFont(F_SMALL);
        lbl.setForeground(C_MUTED);
        p.add(dot);
        p.add(lbl);
        return p;
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