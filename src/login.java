import java.awt.EventQueue;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.sql.*;

public class login extends JFrame {
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField textField_1;
    private JPasswordField passwordField;
    

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                login frame = new login();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public login() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        contentPane = new JPanel();
        contentPane.setBackground(new Color(245, 248, 255));
        contentPane.setLayout(null);
        setContentPane(contentPane);

        JPanel loginCard = new JPanel();
        loginCard.setBounds(130, 30, 300, 270);
        loginCard.setBackground(Color.WHITE);
        loginCard.setLayout(null);
        loginCard.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true));
        contentPane.add(loginCard);

        JLabel lblTitle = new JLabel("Login");
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setBounds(0, 10, 300, 30);
        loginCard.add(lblTitle);

        JLabel lblSapId = new JLabel("SAP ID");
        lblSapId.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSapId.setBounds(30, 60, 80, 15);
        loginCard.add(lblSapId);

        textField_1 = new JTextField();
        textField_1.setBounds(30, 78, 240, 30);
        textField_1.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textField_1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        loginCard.add(textField_1);

        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPassword.setBounds(30, 115, 80, 15);
        loginCard.add(lblPassword);

        passwordField = new JPasswordField();
        passwordField.setBounds(30, 133, 240, 30);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        loginCard.add(passwordField);

        Color normalBg = new Color(33, 150, 243);       // Original blue
        Color hoverBg = new Color(30, 136, 235);        // Slightly darker on hover
        JButton btnLogin = new JButton("Login");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setBackground(new Color(0, 120, 255));
        btnLogin.setFocusPainted(false);
        btnLogin.setBounds(30, 180, 240, 35);
        btnLogin.setBorder(BorderFactory.createEmptyBorder());
        loginCard.add(btnLogin);
        btnLogin.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnLogin.setBackground(hoverBg);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnLogin.setBackground(normalBg);
            }
        });

        JButton btnRegister = new JButton("Sign Up");
        btnRegister.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnRegister.setForeground(new Color(0, 120, 255));
        btnRegister.setContentAreaFilled(false);
        btnRegister.setBorderPainted(false);
        btnRegister.setFocusPainted(false);
        btnRegister.setBounds(110, 220, 80, 20);
        loginCard.add(btnRegister);

        // --- LOGIC STARTS HERE (UNCHANGED) ---
        btnLogin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("-----------------------------------------");
                System.out.println("DEBUG: Login button clicked!");

                String sapid = textField_1.getText().trim();
                String password = new String(passwordField.getPassword());

                if (sapid.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(login.this, "SAP ID and Password are required!", "Input Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Connection con = null;
                PreparedStatement pstmtAuth = null;
                ResultSet rsAuth = null;

                try {
                    String url = "jdbc:oracle:thin:@localhost:1521:xe";
                    String username = "system";
                    String dbPassword = "123";

                    try {
                        Class.forName("oracle.jdbc.driver.OracleDriver");
                    } catch (ClassNotFoundException cnfe) {
                        JOptionPane.showMessageDialog(login.this, "Oracle JDBC Driver not found!", "Driver Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    DriverManager.setLoginTimeout(15);
                    con = DriverManager.getConnection(url, username, dbPassword);

                    String authSql = "SELECT name, subjects_added FROM users WHERE sapid = ? AND password = ?";
                    pstmtAuth = con.prepareStatement(authSql);
                    pstmtAuth.setString(1, sapid);
                    pstmtAuth.setString(2, password);
                    rsAuth = pstmtAuth.executeQuery();

                    if (rsAuth.next()) {
                        String userName = rsAuth.getString("name");
                        int subjectsAddedFlag = rsAuth.getInt("subjects_added");

                        JOptionPane.showMessageDialog(login.this, "Login Successful! Welcome " + userName, "Success", JOptionPane.INFORMATION_MESSAGE);

                        if (subjectsAddedFlag == 1) {
                            Timetable timetableFrame = new Timetable(sapid);
                            timetableFrame.setVisible(true);
                            dispose();
                        } else {
                            Subject subjectPage = new Subject(sapid);
                            subjectPage.setVisible(true);
                            dispose();
                        }

                    } else {
                        JOptionPane.showMessageDialog(login.this, "Invalid SAP ID or Password", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    }

                } catch (SQLException ex_sql) {
                    ex_sql.printStackTrace();
                    JOptionPane.showMessageDialog(login.this, "Database Error: " + ex_sql.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex_gen) {
                    ex_gen.printStackTrace();
                    JOptionPane.showMessageDialog(login.this, "An unexpected error occurred: " + ex_gen.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    try { if (rsAuth != null) rsAuth.close(); } catch (SQLException ignored) {}
                    try { if (pstmtAuth != null) pstmtAuth.close(); } catch (SQLException ignored) {}
                    try { if (con != null) con.close(); } catch (SQLException ignored) {}
                }
            }
        });

        btnRegister.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LoginPage registrationPage = new LoginPage();
                registrationPage.setVisible(true);
                dispose();
            }
        });

        System.out.println("DEBUG: Login constructor finished.");
    }
}
