import java.awt.*; // Use specific imports or wildcard
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder; // For secondary button border

public class Subject extends JFrame {

    private static final long serialVersionUID = 1L;

    // --- UI Style Constants ---
    private static final Color COLOR_BACKGROUND = new Color(240, 244, 248); // Very light grey/blue
    private static final Color COLOR_CONTENT_BACKGROUND = Color.WHITE;
    private static final Color COLOR_PRIMARY_BUTTON = new Color(0, 123, 255); // Blue
    private static final Color COLOR_PRIMARY_BUTTON_TEXT = Color.WHITE;
    private static final Color COLOR_SECONDARY_BUTTON_TEXT = new Color(0, 123, 255); // Blue text
    private static final Color COLOR_TEXT = new Color(51, 51, 51); // Dark grey
    private static final Font FONT_PRIMARY = new Font("Segoe UI", Font.PLAIN, 13); // Primary font
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 12);


    // --- Database Connection Details ---
    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String DB_USERNAME = "system";
    private static final String DB_PASSWORD = "123"; // Replace with your actual DB password

    // --- User Information ---
    private String userSapid;

    // --- UI Components ---
    private JPanel contentPane; // Main content pane of JFrame
    private JPanel formPanel;     // Panel holding the input fields (white background)
    private JPanel buttonPanel;   // Panel holding the buttons (white background)
    private JTextField[] subjectFields = new JTextField[7];
    private JTextField[] targetTotalLecturesFields = new JTextField[7];
    private JButton btnSave;
    private JButton btnLoadExisting;
    private JButton btnBack; // New Back button

    /**
     * Constructor - Requires SAPID of the logged-in user.
     * @param sapid The SAPID of the user entering/editing subjects.
     */
    public Subject(String sapid) {
        this.userSapid = sapid;

        if (this.userSapid == null || this.userSapid.trim().isEmpty()) {
            showErrorDialog("Error: User SAPID is missing. Cannot initialize subject management.", "Initialization Error");
            // Prevent the frame from becoming fully operational
             SwingUtilities.invokeLater(() -> {
                 setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                 setVisible(false);
                 dispose();
             });
            return;
        }

        setTitle("Manage Subjects - SAPID: " + this.userSapid);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Or DISPOSE_ON_CLOSE
        // Increased size slightly for better spacing
        setBounds(100, 100, 650, 550);
        setLocationRelativeTo(null); // Center on screen

        // Main content pane setup
        contentPane = new JPanel(new BorderLayout(10, 10)); // Use BorderLayout
        contentPane.setBackground(COLOR_BACKGROUND); // Light background for the window frame area
        contentPane.setBorder(new EmptyBorder(15, 15, 15, 15)); // Padding around the main content
        setContentPane(contentPane);

        createComponents(); // Create and layout UI elements
        addListeners(); // Add action listeners
        applyStyling(); // Apply custom colors and fonts
        loadExistingSubjects(); // Load data initially
    }

    /** Creates and lays out the UI components using appropriate layout managers. */
    private void createComponents() {

        // --- Form Panel (Center) ---
        formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(COLOR_CONTENT_BACKGROUND); // White background for the form area
        formPanel.setBorder(new EmptyBorder(25, 30, 25, 30)); // Padding inside the form panel
        contentPane.add(formPanel, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5); // Spacing around components (top, left, bottom, right)

        // Header Label
        JLabel lblHeader = new JLabel("Manage Subjects & Target Lectures");
        lblHeader.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3; // Span across 3 columns
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 5, 20, 5); // Extra bottom margin for header
        formPanel.add(lblHeader, gbc);
        gbc.gridwidth = 1; // Reset gridwidth
        gbc.insets = new Insets(8, 5, 8, 5); // Reset insets

        // Column Headers
        JLabel lblSubjectHeader = new JLabel("Subject Name");
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.6; // Give subject name more space
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(lblSubjectHeader, gbc);

        JLabel lblHoursHeader = new JLabel("Target Lectures");
        lblHoursHeader.setToolTipText("Total number of lectures planned for this subject.");
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.3; // Less space for target number
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(lblHoursHeader, gbc);

        // Input Fields and Row Labels
        for (int i = 0; i < 7; i++) {
            gbc.gridy++; // Move to the next row

            // Row Label (e.g., "Subject 1:")
            JLabel lblSubjectN = new JLabel("Subject " + (i + 1) + ":");
            lblSubjectN.setHorizontalAlignment(SwingConstants.RIGHT);
            gbc.gridx = 0;
            gbc.weightx = 0.1; // Small weight for the label column
            gbc.anchor = GridBagConstraints.EAST; // Align label text to the right
            gbc.fill = GridBagConstraints.NONE; // Don't stretch label horizontally
            formPanel.add(lblSubjectN, gbc);

            // Subject Name Field
            subjectFields[i] = new JTextField();
            gbc.gridx = 1;
            gbc.weightx = 0.6; // Takes up more horizontal space
            gbc.anchor = GridBagConstraints.WEST; // Reset anchor
            gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch horizontally
            formPanel.add(subjectFields[i], gbc);

            // Target Total Lectures Field
            targetTotalLecturesFields[i] = new JTextField();
            gbc.gridx = 2;
            gbc.weightx = 0.3; // Less horizontal space
            formPanel.add(targetTotalLecturesFields[i], gbc);
        }

         // Info Label
         JLabel lblInfo = new JLabel("Leave rows blank if fewer than 7 subjects.");
         lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
         gbc.gridx = 0;
         gbc.gridy++;
         gbc.gridwidth = 3;
         gbc.insets = new Insets(15, 5, 0, 5); // Add space above info label
         formPanel.add(lblInfo, gbc);


        // --- Button Panel (South) ---
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10)); // Center buttons with gaps
        buttonPanel.setBackground(COLOR_CONTENT_BACKGROUND); // White background
        buttonPanel.setBorder(new EmptyBorder(10, 0, 10, 0)); // Padding above/below buttons
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        btnBack = new JButton("Back to Timetable");
        btnBack.setToolTipText("Return to the attendance marking screen (discards changes).");
        buttonPanel.add(btnBack);

        btnLoadExisting = new JButton("Reload Saved");
        btnLoadExisting.setToolTipText("Discard current changes and load saved subjects.");
        buttonPanel.add(btnLoadExisting);

        btnSave = new JButton("Save Subjects");
        btnSave.setToolTipText("Save subject names and target lectures to the database.");
        buttonPanel.add(btnSave);
    }

    /** Applies custom fonts and colors to components */
    private void applyStyling() {
        // Set fonts
        setFontRecursively(contentPane, FONT_PRIMARY); // Apply primary font to all
        // Override specific components
        contentPane.getComponent(0).setFont(FONT_HEADER); // Header in formPanel
        ((JLabel)formPanel.getComponent(1)).setFont(FONT_BOLD); // Subject Header
        ((JLabel)formPanel.getComponent(2)).setFont(FONT_BOLD); // Target Header
         // Info label styling
         Component infoLabel = formPanel.getComponent(formPanel.getComponentCount() - 1); // Last component is info label
         if (infoLabel instanceof JLabel) {
             infoLabel.setFont(FONT_LABEL.deriveFont(Font.ITALIC));
             ((JLabel) infoLabel).setForeground(Color.GRAY);
         }

        // Style input fields and row labels
        for(int i = 0; i < 7; i++) {
             JLabel rowLabel = (JLabel)formPanel.getComponent(3 + (i*3)); // Calculate index of row label
             rowLabel.setFont(FONT_BOLD);
             rowLabel.setForeground(COLOR_TEXT);

             subjectFields[i].setFont(FONT_PRIMARY);
             subjectFields[i].setBorder(BorderFactory.createCompoundBorder(
                     BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                     BorderFactory.createEmptyBorder(3, 5, 3, 5) // Internal padding
             ));

             targetTotalLecturesFields[i].setFont(FONT_PRIMARY);
             targetTotalLecturesFields[i].setBorder(BorderFactory.createCompoundBorder(
                     BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                     BorderFactory.createEmptyBorder(3, 5, 3, 5) // Internal padding
             ));
        }

        // Style Buttons
        stylePrimaryButton(btnSave);
        styleSecondaryButton(btnLoadExisting);
        styleSecondaryButton(btnBack);
    }

    // Helper to apply font recursively
    private void setFontRecursively(Container container, Font font) {
        container.setFont(font);
        for (Component c : container.getComponents()) {
            if (c instanceof Container) {
                setFontRecursively((Container) c, font);
            } else {
                c.setFont(font);
            }
        }
         // Special case for JPanel itself if it needs the font
         if (container instanceof JPanel) {
              container.setFont(font);
         }
    }

    // Helper to style primary buttons (like Login)
    private void stylePrimaryButton(JButton button) {
        button.setFont(FONT_BOLD);
        button.setBackground(COLOR_PRIMARY_BUTTON);
        button.setForeground(COLOR_PRIMARY_BUTTON_TEXT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20)); // Padding
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // Helper to style secondary buttons (like Sign Up link, but as button)
    private void styleSecondaryButton(JButton button) {
        button.setFont(FONT_BOLD);
        button.setBackground(COLOR_CONTENT_BACKGROUND); // Match content background
        button.setForeground(COLOR_SECONDARY_BUTTON_TEXT);
        button.setFocusPainted(false);
         // Subtle border matching text color
        button.setBorder(BorderFactory.createCompoundBorder(
             new LineBorder(COLOR_SECONDARY_BUTTON_TEXT, 1, true), // Rounded? maybe not needed
             BorderFactory.createEmptyBorder(7, 18, 7, 18) // Padding (1px less vertically due to border)
        ));
        // Alternative: No border, like a link
        // button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        // button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }


    /** Adds ActionListeners to buttons. */
    private void addListeners() {
        btnSave.addActionListener(e -> saveOrUpdateSubjects());

        btnLoadExisting.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                Subject.this,
                "Reloading will discard any unsaved changes.\nAre you sure you want to proceed?",
                "Confirm Reload",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                loadExistingSubjects();
            }
        });

        // Listener for the new Back button
        btnBack.addActionListener(e -> {
            System.out.println("Navigating back to Timetable for SAPID: " + userSapid);
            // Open the Timetable frame
            Timetable timetableFrame = new Timetable(this.userSapid);
            timetableFrame.setVisible(true);
            // Close this Subject frame
            this.dispose();
        });
    }


    /** Loads existing subjects and their TARGET TOTAL LECTURES for the current user. */
    private void loadExistingSubjects() {
        clearFields();
        String sql = "SELECT subjects, target_total_lectures FROM timetable WHERE sapid = ? ORDER BY subjects";
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int index = 0;

        System.out.println("Loading subjects and target lectures for SAPID: " + this.userSapid);

        try {
            con = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, this.userSapid);
            rs = pstmt.executeQuery();

            while (rs.next() && index < 7) {
                subjectFields[index].setText(rs.getString("subjects"));
                targetTotalLecturesFields[index].setText(String.valueOf(rs.getInt("target_total_lectures")));
                index++;
            }
            System.out.println("Loaded " + index + " existing subjects.");

        } catch (SQLException ex) {
            showErrorDialog("Database Error loading subjects: " + ex.getMessage(), "Database Error");
            ex.printStackTrace();
        } finally {
            closeQuietly(rs);
            closeQuietly(pstmt);
            closeQuietly(con);
        }
    }

    /** Saves new subjects or updates existing ones. Initializes attendance counts to 0. */
    private void saveOrUpdateSubjects() {
        List<SubjectTargetData> subjectsToSave = new ArrayList<>();
        boolean validationError = false;

        // 1. Collect and Validate Input
        for (int i = 0; i < 7; i++) {
            String name = subjectFields[i].getText().trim();
            String targetLecturesStr = targetTotalLecturesFields[i].getText().trim();

            if (!name.isEmpty() || !targetLecturesStr.isEmpty()) { // Process row only if at least one field has content
                if (name.isEmpty()) {
                    showErrorDialog("Subject Name cannot be empty for row " + (i + 1) + " if Target Lectures is filled.", "Validation Error");
                    subjectFields[i].requestFocus();
                    validationError = true;
                    break;
                }
                 if (targetLecturesStr.isEmpty()) {
                    showErrorDialog("Target Lectures cannot be empty for row " + (i + 1) + " if Subject Name is filled.", "Validation Error");
                    targetTotalLecturesFields[i].requestFocus();
                    validationError = true;
                    break;
                }

                // If both are filled, validate target lectures
                try {
                    int targetLectures = Integer.parseInt(targetLecturesStr);
                    if (targetLectures <= 0) {
                        showErrorDialog("Target Lectures for '" + name + "' must be a positive number.", "Validation Error");
                        targetTotalLecturesFields[i].requestFocus();
                        validationError = true;
                        break;
                    }
                    // Check for duplicate subject names within the input fields
                    for (SubjectTargetData existing : subjectsToSave) {
                        if (existing.getName().equalsIgnoreCase(name)) {
                            showErrorDialog("Duplicate subject name entered: '" + name + "'. Subject names must be unique.", "Validation Error");
                            subjectFields[i].requestFocus();
                            validationError = true;
                            break;
                        }
                    }
                    if (validationError) break;

                    subjectsToSave.add(new SubjectTargetData(name, targetLectures));

                } catch (NumberFormatException ex) {
                    showErrorDialog("Invalid number format for Target Lectures in row " + (i + 1) + ".", "Validation Error");
                    targetTotalLecturesFields[i].requestFocus();
                    validationError = true;
                    break;
                }
            }
            // If both fields are empty, just ignore the row.
        }


        if (validationError) return;
        if (subjectsToSave.isEmpty()) {
            // Allow saving empty list if user explicitly clears all subjects
             int choice = JOptionPane.showConfirmDialog(
                 this,
                 "No subjects were entered. This will remove all existing subjects for this user.\nAre you sure you want to proceed?",
                 "Confirm Empty Save",
                 JOptionPane.YES_NO_OPTION,
                 JOptionPane.WARNING_MESSAGE);
             if (choice != JOptionPane.YES_OPTION) {
                 return; // User cancelled empty save
             }
            // Proceed to save empty list (will just perform the delete)
        }


        // 2. Database Operations within a Transaction
        Connection con = null;
        PreparedStatement pstmtDelete = null;
        PreparedStatement pstmtInsert = null;
        PreparedStatement pstmtUpdateFlag = null;

        try {
            con = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            con.setAutoCommit(false); // Start transaction

            // a. Delete existing subjects for this user
            String deleteSql = "DELETE FROM timetable WHERE sapid = ?";
            pstmtDelete = con.prepareStatement(deleteSql);
            pstmtDelete.setString(1, this.userSapid);
            int deletedRows = pstmtDelete.executeUpdate();
            System.out.println("Deleted " + deletedRows + " existing timetable rows for SAPID: " + this.userSapid);

            // b. Insert the new set of subjects (if any)
            if (!subjectsToSave.isEmpty()) {
                String insertSql = "INSERT INTO timetable (sapid, subjects, target_total_lectures, attended_count, lectures_taken_so_far) " +
                                   "VALUES (?, ?, ?, 0, 0)";
                pstmtInsert = con.prepareStatement(insertSql);

                for (SubjectTargetData subject : subjectsToSave) {
                    pstmtInsert.setString(1, this.userSapid);
                    pstmtInsert.setString(2, subject.getName());
                    pstmtInsert.setInt(3, subject.getTargetTotalLectures());
                    pstmtInsert.addBatch();
                }
                int[] insertCounts = pstmtInsert.executeBatch();
                System.out.println("Inserted " + insertCounts.length + " new timetable rows.");
            }

            // c. Update the 'subjects_added' flag in the users table
            // Set flag to 1 if saving >0 subjects, set to 0 if saving 0 subjects
            int flagValue = subjectsToSave.isEmpty() ? 0 : 1;
            String updateFlagSql = "UPDATE users SET subjects_added = ? WHERE sapid = ?";
            pstmtUpdateFlag = con.prepareStatement(updateFlagSql);
            pstmtUpdateFlag.setInt(1, flagValue);
            pstmtUpdateFlag.setString(2, this.userSapid);
            int flagUpdateCount = pstmtUpdateFlag.executeUpdate();
            System.out.println("User flag 'subjects_added' set to " + flagValue + " for SAPID: " + this.userSapid + " (Rows updated: " + flagUpdateCount + ")");

            con.commit(); // Commit transaction

            JOptionPane.showMessageDialog(this,
                subjectsToSave.isEmpty() ? "All subjects removed successfully." : "Subjects saved successfully!",
                "Success", JOptionPane.INFORMATION_MESSAGE);

            // 3. Proceed to the main Timetable view
            Timetable timetableFrame = new Timetable(this.userSapid);
            timetableFrame.setVisible(true);
            this.dispose(); // Close this subject entry window

        } catch (SQLException ex) {
            try { if (con != null) con.rollback(); } catch (SQLException e_rb) { System.err.println("Rollback failed: " + e_rb.getMessage()); }

            String errorMessage = "Database Error saving subjects: " + ex.getMessage();
            if (ex.getErrorCode() == 1 || (ex.getMessage() != null && ex.getMessage().toUpperCase().contains("PK_TIMETABLE"))) {
                errorMessage = "Database Error: Primary key violation. This might happen if there's an unexpected issue during delete/insert.";
            } else if (ex.getErrorCode() == 2290) { // Check constraint violation
                 errorMessage = "Database Error: Check constraint violation. Ensure target lectures are positive if required by DB constraints.";
            }
            showErrorDialog(errorMessage, "Save Error");
            ex.printStackTrace();

        } finally {
            try { if (con != null) con.setAutoCommit(true); } catch (SQLException ignored) {} // Restore auto-commit
            closeQuietly(pstmtDelete);
            closeQuietly(pstmtInsert);
            closeQuietly(pstmtUpdateFlag);
            closeQuietly(con);
        }
    }


    /** Helper method to clear all input fields */
    private void clearFields() {
        for (int i = 0; i < 7; i++) {
            subjectFields[i].setText("");
            targetTotalLecturesFields[i].setText("");
        }
    }

    // Helper method to show error dialogs consistently
    private void showErrorDialog(String message, String title) {
         JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }


    // --- Helper Methods for Closing JDBC Resources ---
    private static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception ex) {
                System.err.println("Failed to close resource (" + resource.getClass().getSimpleName() + "): " + ex.getMessage());
            }
        }
    }

    // --- Inner class for holding subject data ---
    private static class SubjectTargetData {
        private String name;
        private int targetTotalLectures;

        public SubjectTargetData(String name, int targetTotalLectures) {
            this.name = name;
            this.targetTotalLectures = targetTotalLectures;
        }
        public String getName() { return name; }
        public int getTargetTotalLectures() { return targetTotalLectures; }

        @Override
        public String toString() {
            return "SubjectTargetData [name=" + name + ", targetTotalLectures=" + targetTotalLectures + "]";
        }
    }

    // --- Main method removed (frame should be launched from elsewhere) ---
}