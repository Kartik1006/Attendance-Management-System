import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.sql.*;

// ... imports remain unchanged

public class LoginPage extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField textField; // SAPID field
    private JTextField textField_1; // Name field
    private JPasswordField passwordField; // Password field
    private JPasswordField passwordField_1; // Confirm Password field

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                LoginPage frame = new LoginPage();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public LoginPage() {
        setTitle("User Registration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 600, 400);
        setLocationRelativeTo(null);

        contentPane = new JPanel();
        contentPane.setBackground(new Color(245, 248, 255)); // Match login bg
        contentPane.setLayout(null);
        setContentPane(contentPane);

        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(null);
        cardPanel.setBounds(130, 30, 300, 300);
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        contentPane.add(cardPanel);

        JLabel lblTitle = new JLabel("Register");
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setBounds(30, 10, 240, 30);
        cardPanel.add(lblTitle);

        JLabel lblSapid = new JLabel("SAP ID");
        lblSapid.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSapid.setBounds(30, 50, 100, 20);
        cardPanel.add(lblSapid);

        textField = new JTextField();
        textField.setBounds(30, 70, 240, 25);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        cardPanel.add(textField);

        JLabel lblName = new JLabel("Name");
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblName.setBounds(30, 100, 100, 20);
        cardPanel.add(lblName);

        textField_1 = new JTextField();
        textField_1.setBounds(30, 120, 240, 25);
        textField_1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        cardPanel.add(textField_1);

        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPassword.setBounds(30, 150, 100, 20);
        cardPanel.add(lblPassword);

        passwordField = new JPasswordField();
        passwordField.setBounds(30, 170, 240, 25);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        cardPanel.add(passwordField);

        JLabel lblConfirmPassword = new JLabel("Confirm Password");
        lblConfirmPassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblConfirmPassword.setBounds(30, 200, 120, 20);
        cardPanel.add(lblConfirmPassword);

        passwordField_1 = new JPasswordField();
        passwordField_1.setBounds(30, 220, 240, 25);
        passwordField_1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        cardPanel.add(passwordField_1);

        Color normalBg = new Color(33, 150, 243);  // Blue
        Color hoverBg = new Color(30, 136, 235);  // Darker on hover

        JButton btnRegister = new JButton("Register");
        btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setBackground(normalBg);
        btnRegister.setFocusPainted(false);
        btnRegister.setBounds(35, 255, 110, 30);
        btnRegister.setBorder(BorderFactory.createEmptyBorder());
        cardPanel.add(btnRegister);

        btnRegister.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnRegister.setBackground(hoverBg);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnRegister.setBackground(normalBg);
            }
        });

        JButton btnGoToLogin = new JButton("Go to Login");
        btnGoToLogin.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnGoToLogin.setForeground(normalBg);
        btnGoToLogin.setContentAreaFilled(false);
        btnGoToLogin.setBorderPainted(false);
        btnGoToLogin.setFocusPainted(false);
        btnGoToLogin.setBounds(170, 260, 110, 20);
        cardPanel.add(btnGoToLogin);

        btnRegister.addActionListener(e -> registerUser());

        btnGoToLogin.addActionListener(e -> {
            login loginPage = new login();
            loginPage.setVisible(true);
            dispose();
        });
    }

    private void registerUser() {
        String sapid = textField.getText().trim();
        String name = textField_1.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(passwordField_1.getPassword());

        if (sapid.isEmpty() || name.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String url = "jdbc:oracle:thin:@localhost:1521:xe";
            String username = "system";
            String dbPassword = "123";

            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection con = DriverManager.getConnection(url, username, dbPassword);

            PreparedStatement checkStmt = con.prepareStatement("SELECT * FROM users WHERE sapid = ?");
            checkStmt.setString(1, sapid);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "User already exists", "Registration Failed", JOptionPane.ERROR_MESSAGE);
            } else {
                PreparedStatement insertStmt = con.prepareStatement("INSERT INTO users (sapid, name, password, subjects_added) VALUES (?, ?, ?, 0)");
                insertStmt.setString(1, sapid);
                insertStmt.setString(2, name);
                insertStmt.setString(3, password);

                int result = insertStmt.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Registration Successful", "Success", JOptionPane.INFORMATION_MESSAGE);
                    new login().setVisible(true);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to register", "Error", JOptionPane.ERROR_MESSAGE);
                }
                insertStmt.close();
            }

            rs.close();
            checkStmt.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
        }
    }
}
