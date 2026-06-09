package com.project.view;

import com.project.controller.AuthController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.Pattern;

public class SmartHeritageStay {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }
}

// -------------------- LOGIN FRAME --------------------
class LoginFrame extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;
    private AuthController authController;

    public LoginFrame() {
        authController = new AuthController();
        
        setTitle("Smart Heritage Stay - Login");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(new Color(245, 240, 230));

        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Welcome to Smart Heritage Stay", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        add(titleLabel, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        panel.setBackground(new Color(245, 240, 230));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 50, 30, 50));

        emailField = new JTextField();
        passwordField = new JPasswordField();
        Dimension fieldSize = new Dimension(Integer.MAX_VALUE, 30);
        emailField.setMaximumSize(fieldSize);
        passwordField.setMaximumSize(fieldSize);

        // --- EMAIL ---
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(emailLabel);
        panel.add(Box.createVerticalStrut(3)); 
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(emailField);
        panel.add(Box.createVerticalStrut(15)); 

        // --- PASSWORD ---
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(passwordLabel);
        panel.add(Box.createVerticalStrut(3)); 
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(30)); 

        // --- LOGIN BUTTON ---
        JButton loginButton = new JButton("Login");
        loginButton.setBackground(new Color(160, 82, 45)); 
        loginButton.setForeground(Color.BLACK);
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35)); 
        panel.add(loginButton);

        add(panel, BorderLayout.CENTER);

        JLabel registerLabel = new JLabel("Don't have an account? Register Now", SwingConstants.CENTER);
        registerLabel.setForeground(new Color(0, 102, 51));
        registerLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        add(registerLabel, BorderLayout.SOUTH);

        // Actions
        loginButton.addActionListener(e -> validateLogin());
        registerLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
                new RegisterFrame();
            }
        });

        setVisible(true);
    }

    // ── Replace the validateLogin() method in LoginFrame with this version ──
// This passes the email to both dashboards so the profile tab and per-guest
// status logic work correctly.

private void validateLogin() {
    String email    = emailField.getText().trim();
    String password = new String(passwordField.getPassword());

    if (email.isEmpty() || password.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    String role = authController.authenticateUser(email, password);

    if (role != null) {
        JOptionPane.showMessageDialog(this, "Login successful! Role: " + role, "Success", JOptionPane.INFORMATION_MESSAGE);
        dispose();

        if (role.equalsIgnoreCase("Guest")) {
            // Pass email so GuestDashboardGUI can resolve guestId and load profile
            new GuestDashboardGUI(email).setVisible(true);
        } else {
            // Pass role AND email so StaffDashboardGUI can load staff profile
            new StaffDashboardGUI(role, email).setVisible(true);
        }
    } else {
        JOptionPane.showMessageDialog(this, "Invalid email or password", "Login Failed", JOptionPane.ERROR_MESSAGE);
    }
}
}

// -------------------- REGISTER FRAME --------------------
class RegisterFrame extends JFrame {
    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private AuthController authController;

    public RegisterFrame() {
        authController = new AuthController();
        
        setTitle("Smart Heritage Stay - Register");
        setSize(450, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(new Color(245, 240, 230));

        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Create Your Account", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        add(titleLabel, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        panel.setBackground(new Color(245, 240, 230));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        nameField = new JTextField();
        emailField = new JTextField();
        passwordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();

        Dimension fieldSize = new Dimension(Integer.MAX_VALUE, 30);
        nameField.setMaximumSize(fieldSize);
        emailField.setMaximumSize(fieldSize);
        passwordField.setMaximumSize(fieldSize);
        confirmPasswordField.setMaximumSize(fieldSize);

        // --- FULL NAME ---
        JLabel nameLabel = new JLabel("Full Name:");
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(nameLabel);
        panel.add(Box.createVerticalStrut(3));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(12));

        // --- EMAIL ---
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(emailLabel);
        panel.add(Box.createVerticalStrut(3));
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(emailField);
        panel.add(Box.createVerticalStrut(12));

        // --- PASSWORD ---
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(passwordLabel);
        panel.add(Box.createVerticalStrut(3));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(12));

        // --- CONFIRM PASSWORD ---
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(confirmLabel);
        panel.add(Box.createVerticalStrut(3));
        confirmPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(confirmPasswordField);
        panel.add(Box.createVerticalStrut(25));

        // --- REGISTER BUTTON ---
        JButton registerButton = new JButton("Register");
        registerButton.setBackground(new Color(160, 82, 45)); 
        registerButton.setForeground(Color.BLACK);
        registerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        panel.add(registerButton);

        add(panel, BorderLayout.CENTER);

        JLabel registerLabel = new JLabel("Back To Login", SwingConstants.CENTER);
        registerLabel.setForeground(new Color(0, 102, 51));
        registerLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        add(registerLabel, BorderLayout.SOUTH);

        // Actions
        registerButton.addActionListener(e -> validateRegister());
        registerLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                dispose();
                new LoginFrame();
            }
        });

        setVisible(true);
    }

    private void validateRegister() {
        String name = nameField.getText();
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$", email)) {
            JOptionPane.showMessageDialog(this, "Invalid email format", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if email already exists in Guest or Staff table
        if (authController.emailExists(email)) {
            JOptionPane.showMessageDialog(this, "Email already exists. Please use a different email.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Send to Database
        if (authController.registerGuest(name, email, password)) {
            JOptionPane.showMessageDialog(this, "Registration successful! You can now login.", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            new LoginFrame();
        } else {
            JOptionPane.showMessageDialog(this, "Registration failed. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}