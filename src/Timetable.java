import java.awt.*; // Use specific imports or wildcard
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder; // For bottom border on rows

public class Timetable extends JFrame {

    private static final long serialVersionUID = 1L;

    // --- UI Style Constants ---
    private static final Color COLOR_BACKGROUND = new Color(240, 244, 248);
    private static final Color COLOR_CONTENT_BACKGROUND = Color.WHITE;
    private static final Color COLOR_PRIMARY_BUTTON = new Color(0, 123, 255);
    private static final Color COLOR_PRIMARY_BUTTON_TEXT = Color.WHITE;
    private static final Color COLOR_SECONDARY_BUTTON_TEXT = new Color(0, 123, 255);
    private static final Color COLOR_WARNING_BUTTON_TEXT = new Color(220, 53, 69); // Red for warning/reset
    private static final Color COLOR_TEXT = new Color(51, 51, 51);
    private static final Color COLOR_BORDER = Color.LIGHT_GRAY;
    private static final Font FONT_PRIMARY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_MONOSPACED = new Font("Monospaced", Font.PLAIN, 12); // For stats

    // --- Database Connection Details ---
    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String DB_USERNAME = "system";
    private static final String DB_PASSWORD = "123"; // Replace with your actual DB password

    // --- User Information ---
    private String currentUserSapid;

    // --- UI Components & Data ---
    private Connection conn;
    private List<String> subjects = new ArrayList<>();
    private Map<String, JRadioButton> presentRadioButtons = new HashMap<>();
    private Map<String, JRadioButton> absentRadioButtons = new HashMap<>();
    private Map<String, ButtonGroup> subjectButtonGroups = new HashMap<>();
    private JPanel contentPane;     // Main content pane of JFrame
    private JPanel mainPanel;       // Panel holding header, subjects, buttons (white background)
    private JPanel subjectPanel;    // Panel inside scrollpane holding subject rows
    private JScrollPane scrollPane;
    private JButton btnSave;
    private JButton btnResetData;
    private JButton btnViewStats;
    private JButton btnResetUI;
    private JButton btnEditSubjects;

    /**
     * Constructor - Requires SAPID of the logged-in user.
     * @param sapid The SAPID of the user whose timetable to display.
     */
    public Timetable(String sapid) {
        this.currentUserSapid = sapid;

        if (this.currentUserSapid == null || this.currentUserSapid.trim().isEmpty()) {
            handleFatalError("User SAPID missing. Cannot load timetable.");
            return;
        }

        setTitle("Attendance Tracker - SAPID: " + this.currentUserSapid);
        initializeDatabaseConnection();

        if (conn == null) {
            handleFatalError("Fatal Error: Database connection failed. Exiting.");
            return;
        }

        // --- Frame Setup ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Increased size slightly for better spacing with new layout
        setPreferredSize(new Dimension(700, 650));
        setMinimumSize(new Dimension(600, 500)); // Prevent resizing too small
        setLocationRelativeTo(null); // Center screen

        // Main content pane setup (outer area)
        contentPane = new JPanel(new BorderLayout(0, 0)); // No gaps needed here
        contentPane.setBackground(COLOR_BACKGROUND); // Light background for the window frame area
        contentPane.setBorder(new EmptyBorder(15, 15, 15, 15)); // Padding around the main content
        setContentPane(contentPane);

        // Central panel for holding actual content (white background)
        mainPanel = new JPanel(new BorderLayout(10, 15)); // Gaps between sections
        mainPanel.setBackground(COLOR_CONTENT_BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15)); // Padding inside the white area
        contentPane.add(mainPanel, BorderLayout.CENTER);

        // --- Create Components ---
        createHeaderPanel();        // NORTH
        setupSubjectPanel();        // CENTER (contains scroll pane)
        createButtonPanel();        // SOUTH

        // --- Load Data & Apply Style ---
        loadSubjectsAndCreateUI();
        applyStyling();
        resetRadioButtonsToDefault(); // Ensure initial state is unselected

        pack(); // Adjust frame size to fit components based on layout managers
    }

    /** Gracefully handle fatal errors during startup */
    private void handleFatalError(String message) {
        JOptionPane.showMessageDialog(null, message, "Initialization Error", JOptionPane.ERROR_MESSAGE);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        SwingUtilities.invokeLater(() -> {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
            setVisible(false);
            dispose();
        });
    }

    /** Initializes the database connection. */
    private void initializeDatabaseConnection() {
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            System.out.println("Database connection established.");
        } catch (SQLException e) {
            e.printStackTrace();
            conn = null; // Ensure conn is null on failure
        }
    }

     /** Creates the header panel (NORTH). */
    private void createHeaderPanel() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(COLOR_CONTENT_BACKGROUND); // Match inner background
        JLabel lblHeader = new JLabel("Mark Attendance for this Session");
        headerPanel.add(lblHeader);
        mainPanel.add(headerPanel, BorderLayout.NORTH);
    }

    /** Sets up the scrollable panel for subject rows (CENTER). */
    private void setupSubjectPanel() {
        subjectPanel = new JPanel();
        subjectPanel.setLayout(new BoxLayout(subjectPanel, BoxLayout.Y_AXIS)); // Vertical stacking
        subjectPanel.setBackground(COLOR_CONTENT_BACKGROUND); // White background

        scrollPane = new JScrollPane(subjectPanel);
        scrollPane.setBorder(new LineBorder(COLOR_BORDER)); // Simple border for scroll pane
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    /** Creates the bottom button panel (SOUTH). */
    private void createButtonPanel() {
        JPanel buttonPanelContainer = new JPanel(new BorderLayout()); // Container for centering
        buttonPanelContainer.setBackground(COLOR_CONTENT_BACKGROUND);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5)); // Center buttons
        buttonPanel.setBackground(COLOR_CONTENT_BACKGROUND); // White background

        btnSave = new JButton("SAVE Session");
        btnSave.setToolTipText("Save the current Present/Absent status for all subjects");
        btnSave.addActionListener(e -> saveAttendance());
        buttonPanel.add(btnSave);

        btnResetUI = new JButton("Clear Choices");
        btnResetUI.setToolTipText("Clear the Present/Absent selections for this session");
        btnResetUI.addActionListener(e -> resetRadioButtonsToDefault());
        buttonPanel.add(btnResetUI);

        btnViewStats = new JButton("View STATS");
        btnViewStats.setToolTipText("Show current attendance statistics");
        btnViewStats.addActionListener(e -> showStatistics());
        buttonPanel.add(btnViewStats);

        btnEditSubjects = new JButton("Edit Subjects");
        btnEditSubjects.setToolTipText("Add, remove, or modify subjects and target lectures");
        btnEditSubjects.addActionListener(e -> openSubjectEditor());
        buttonPanel.add(btnEditSubjects);

        btnResetData = new JButton("RESET ALL Attendance");
        btnResetData.setToolTipText("WARNING: Resets all attended/taken counts to zero in the database!");
        btnResetData.addActionListener(e -> resetAttendanceData());
        buttonPanel.add(btnResetData);


        buttonPanelContainer.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanelContainer, BorderLayout.SOUTH);
    }

    /** Applies custom fonts and colors to components */
    private void applyStyling() {
        // Set fonts
        setFontRecursively(mainPanel, FONT_PRIMARY); // Apply primary font to inner panel
        // Override specific components
        ((JLabel) ((JPanel) mainPanel.getComponent(0)).getComponent(0)).setFont(FONT_HEADER); // Header label

        // Style Buttons
        stylePrimaryButton(btnSave);
        styleSecondaryButton(btnResetUI);
        styleSecondaryButton(btnViewStats);
        styleSecondaryButton(btnEditSubjects);
        styleWarningButton(btnResetData); // Special style for Reset All

        // Style subject panel and scrollpane
        subjectPanel.setBackground(COLOR_CONTENT_BACKGROUND);
        scrollPane.getViewport().setBackground(COLOR_CONTENT_BACKGROUND); // Background seen during scroll
        scrollPane.setBorder(new LineBorder(COLOR_BORDER));

        // Styling applied within createSubjectRowPanel for rows
    }

    // Helper to apply font recursively
    private void setFontRecursively(Container container, Font font) {
        container.setFont(font);
        for (Component c : container.getComponents()) {
            if (c instanceof JScrollPane) { // Don't recurse into JScrollPane's own children usually
                 c.setFont(font);
                 ((JScrollPane) c).getViewport().setFont(font); // Set font for viewport
                 setFontRecursively(((JScrollPane) c).getViewport(), font); // Recurse into viewport's view
            } else if (c instanceof Container) {
                setFontRecursively((Container) c, font);
            } else {
                c.setFont(font);
            }
        }
         if (container instanceof JPanel) {
              container.setFont(font);
         }
    }

    // Helper to style primary buttons (like Save)
    private void stylePrimaryButton(JButton button) {
        button.setFont(FONT_BOLD);
        button.setBackground(COLOR_PRIMARY_BUTTON);
        button.setForeground(COLOR_PRIMARY_BUTTON_TEXT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20)); // Padding
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // Helper to style secondary buttons
    private void styleSecondaryButton(JButton button) {
        button.setFont(FONT_BOLD);
        button.setBackground(COLOR_CONTENT_BACKGROUND);
        button.setForeground(COLOR_SECONDARY_BUTTON_TEXT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
             new LineBorder(COLOR_SECONDARY_BUTTON_TEXT, 1),
             BorderFactory.createEmptyBorder(7, 18, 7, 18) // Padding (1px less vertically due to border)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

     // Helper to style warning buttons (like Reset All)
    private void styleWarningButton(JButton button) {
        button.setFont(FONT_BOLD);
        button.setBackground(COLOR_CONTENT_BACKGROUND);
        button.setForeground(COLOR_WARNING_BUTTON_TEXT); // Red text
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
             new LineBorder(COLOR_WARNING_BUTTON_TEXT, 1), // Red border
             BorderFactory.createEmptyBorder(7, 18, 7, 18)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Loads subjects for the CURRENT USER and creates UI rows. */
    private void loadSubjectsAndCreateUI() {
        subjectPanel.removeAll(); // Clear previous content
        subjects.clear();
        presentRadioButtons.clear();
        absentRadioButtons.clear();
        subjectButtonGroups.clear();

        String sql = "SELECT subjects FROM timetable WHERE sapid = ? ORDER BY subjects";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, this.currentUserSapid);
            ResultSet rs = pstmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                String subject = rs.getString("subjects");
                subjects.add(subject);
                subjectPanel.add(createSubjectRowPanel(subject));
                // Add separator line between items, but not after the last one
                // subjectPanel.add(Box.createVerticalStrut(5)); // Add spacing between rows
                count++;
            }

             if (count > 0) {
                 // Remove border from the last item to avoid double border at the bottom
                Component lastRow = subjectPanel.getComponent(subjectPanel.getComponentCount() - 1);
                if (lastRow instanceof JPanel) {
                    ((JPanel)lastRow).setBorder(new EmptyBorder(10, 15, 10, 15)); // Keep padding, remove border
                }
            }


            if (subjects.isEmpty()) {
                JLabel noSubjectsLabel = new JLabel("No subjects found. Click 'Edit Subjects' to add some.");
                noSubjectsLabel.setHorizontalAlignment(SwingConstants.CENTER);
                noSubjectsLabel.setFont(FONT_LABEL.deriveFont(Font.ITALIC));
                noSubjectsLabel.setForeground(Color.GRAY);
                // Add padding around the label when it's the only thing
                JPanel emptyPanel = new JPanel(new BorderLayout());
                emptyPanel.setBackground(COLOR_CONTENT_BACKGROUND);
                emptyPanel.setBorder(new EmptyBorder(50, 20, 50, 20));
                emptyPanel.add(noSubjectsLabel, BorderLayout.CENTER);
                subjectPanel.add(emptyPanel);

            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading subjects: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            JLabel errorLabel = new JLabel("Error loading subjects. Check connection or try 'Edit Subjects'.");
            errorLabel.setForeground(Color.RED);
            errorLabel.setFont(FONT_LABEL);
            subjectPanel.add(errorLabel); // Add error label directly
        } finally {
            // Ensure the panel updates its layout and appearance
            subjectPanel.revalidate();
            subjectPanel.repaint();
        }
    }

    /** Creates a JPanel containing the UI elements for a single subject row. */
    private JPanel createSubjectRowPanel(String subject) {
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 5)); // Increased gap
        rowPanel.setBackground(COLOR_CONTENT_BACKGROUND); // White background for the row
         // Add padding and a bottom border only
        rowPanel.setBorder(BorderFactory.createCompoundBorder(
             new MatteBorder(0, 0, 1, 0, COLOR_BORDER), // Bottom border
             new EmptyBorder(10, 15, 10, 15)             // Padding
        ));
        rowPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 60)); // Allow slightly more height


        JLabel lblSubject = new JLabel(subject);
        lblSubject.setFont(FONT_BOLD); // Bolder subject name
        lblSubject.setForeground(COLOR_TEXT);
        lblSubject.setPreferredSize(new Dimension(300, 25)); // Wider label
        rowPanel.add(lblSubject);

        JRadioButton rdoPresent = new JRadioButton("Present");
        rdoPresent.setFont(FONT_PRIMARY);
        rdoPresent.setBackground(COLOR_CONTENT_BACKGROUND); // Match background
        rdoPresent.setFocusPainted(false);
        rdoPresent.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rowPanel.add(rdoPresent);
        presentRadioButtons.put(subject, rdoPresent);

        JRadioButton rdoAbsent = new JRadioButton("Absent");
        rdoAbsent.setFont(FONT_PRIMARY);
        rdoAbsent.setBackground(COLOR_CONTENT_BACKGROUND); // Match background
        rdoAbsent.setFocusPainted(false);
        rdoAbsent.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rowPanel.add(rdoAbsent);
        absentRadioButtons.put(subject, rdoAbsent);

        ButtonGroup group = new ButtonGroup();
        group.add(rdoPresent);
        group.add(rdoAbsent);
        subjectButtonGroups.put(subject, group);

        return rowPanel;
    }


    /** Action handler for the Edit Subjects button */
    private void openSubjectEditor() {
         System.out.println("Opening Subject edit window for SAPID: " + currentUserSapid);
         try {
             Subject subjectFrame = new Subject(currentUserSapid); // Pass the current user's SAPID
             subjectFrame.setVisible(true);
             this.dispose(); // Close the current Timetable window
         } catch (Exception ex) {
             ex.printStackTrace();
             JOptionPane.showMessageDialog(Timetable.this,
                 "Error opening subject editor: " + ex.getMessage(),
                 "Navigation Error", JOptionPane.ERROR_MESSAGE);
         }
    }

    /** Saves the current attendance status for all subjects where a selection is made. */
    private void saveAttendance() {
        int subjectsProcessed = 0;
        int subjectsUpdated = 0;
        int subjectsFailed = 0;
        int subjectsSkipped = 0;
        List<String> errors = new ArrayList<>();

        String sql = "UPDATE timetable SET " +
                     "lectures_taken_so_far = NVL(lectures_taken_so_far, 0) + 1, " +
                     "attended_count = NVL(attended_count, 0) + ? " +
                     "WHERE sapid = ? AND subjects = ?";

        for (String subject : subjects) {
            subjectsProcessed++;
            JRadioButton rdoPresent = presentRadioButtons.get(subject);
            JRadioButton rdoAbsent = absentRadioButtons.get(subject);

            if (rdoPresent == null || rdoAbsent == null) {
                System.err.println("Error: UI component missing for subject " + subject);
                subjectsFailed++;
                errors.add("UI component missing for " + subject);
                continue;
            }

            if (!rdoPresent.isSelected() && !rdoAbsent.isSelected()) {
                subjectsSkipped++;
                System.out.println("Skipping subject '" + subject + "' as no selection was made.");
                continue;
            }

            int attendedIncrement = rdoPresent.isSelected() ? 1 : 0;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, attendedIncrement);
                pstmt.setString(2, this.currentUserSapid);
                pstmt.setString(3, subject);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    subjectsUpdated++;
                } else {
                    subjectsFailed++;
                    errors.add("Failed to update DB for " + subject);
                    System.err.println("Warning: 0 rows updated for subject '" + subject + "', SAPID '" + currentUserSapid + "'");
                }
            } catch (SQLException e) {
                subjectsFailed++;
                errors.add("SQL Error for " + subject + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Report results
        StringBuilder resultMessage = new StringBuilder();
        boolean isError = subjectsFailed > 0;
        boolean isSuccess = subjectsUpdated > 0;

        if (isSuccess) {
            resultMessage.append("Attendance for ").append(subjectsUpdated).append(" subjects saved successfully!\n");
        }
        if (subjectsSkipped > 0) {
             resultMessage.append(subjectsSkipped).append(" subjects were skipped (no selection made).\n");
        }
        if (isError) {
             resultMessage.append("\nErrors occurred while saving attendance for ").append(subjectsFailed).append(" subjects:\n");
             for(String err : errors) {
                 resultMessage.append("- ").append(err).append("\n");
             }
        }
        if (!isSuccess && subjectsSkipped == 0 && !isError && subjectsProcessed > 0) {
             resultMessage.append("No selections were made or no subjects needed updating.");
        } else if (subjectsProcessed == 0) {
            resultMessage.append("No subjects loaded to save attendance for.");
        }

        int messageType = isError ? JOptionPane.ERROR_MESSAGE : (isSuccess ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        String title = isError ? "Saving Error(s)" : (isSuccess ? "Success" : "Information");

        JOptionPane.showMessageDialog(this, resultMessage.toString().trim(), title, messageType);

        if (isSuccess || subjectsSkipped > 0) { // Reset even if some were skipped but others saved
             resetRadioButtonsToDefault();
        }
    }


    /** Resets the attendance counts (attended, taken) to ZERO in the database for the current user. */
    private void resetAttendanceData() {
        // Use styled JOptionPane if possible, or standard one
        UIManager.put("OptionPane.messageFont", FONT_PRIMARY);
        UIManager.put("OptionPane.buttonFont", FONT_BOLD);

        int choice = JOptionPane.showConfirmDialog(
            this,
            "<html><body style='font-family: Segoe UI; font-size: 11pt;'>" // Basic HTML for font consistency
            + "<b>WARNING!</b><br><br>"
            + "This will permanently reset <b>Attended</b> and <b>Taken So Far</b> counts<br>"
            + "to ZERO for <u>ALL</u> subjects registered for SAPID: " + currentUserSapid + ".<br><br>"
            + "<font color='red'>This action cannot be undone.</font><br><br>"
            + "Are you absolutely sure?</body></html>",
            "Confirm Attendance Reset",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

         UIManager.put("OptionPane.messageFont", null); // Reset default
         UIManager.put("OptionPane.buttonFont", null);

        if (choice == JOptionPane.YES_OPTION) {
            String sql = "UPDATE timetable SET attended_count = 0, lectures_taken_so_far = 0 WHERE sapid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, this.currentUserSapid);
                int rowsAffected = pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, rowsAffected + " subjects had their attendance counts reset.",
                                              "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
                System.out.println("Attendance data reset for SAPID: " + currentUserSapid + ", Rows affected: " + rowsAffected);
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error during reset: " + e.getMessage(),
                                              "Reset Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
             JOptionPane.showMessageDialog(this, "Attendance reset cancelled.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Resets all radio buttons on the UI to be unselected. */
    private void resetRadioButtonsToDefault() {
         for (ButtonGroup group : subjectButtonGroups.values()) {
             if (group != null) {
                 group.clearSelection();
             }
         }
         System.out.println("Reset radio button selections (cleared).");
    }


    /** Fetches and displays attendance statistics for the current user. */
    private void showStatistics() {
        JTextArea statsTextArea = new JTextArea(15, 65);
        statsTextArea.setEditable(false);
        statsTextArea.setFont(FONT_MONOSPACED); // Use Monospaced for alignment
        statsTextArea.setBackground(COLOR_CONTENT_BACKGROUND);
        statsTextArea.setForeground(COLOR_TEXT);
        statsTextArea.setMargin(new Insets(10, 10, 10, 10)); // Padding inside text area

        StringBuilder stats = new StringBuilder(" Attendance Statistics for SAPID: " + currentUserSapid + "\n");
        stats.append(" ==============================================================================\n");
        stats.append(String.format(" %-25s | %8s | %8s | %8s | %10s \n",
                "Subject", "Attended", "Taken", "Target", "Can Leave*"));
        stats.append(" ------------------------------------------------------------------------------\n");

        String sql = "SELECT subjects, NVL(attended_count,0) as attended, " +
                     "NVL(lectures_taken_so_far,0) as taken, NVL(target_total_lectures,0) as target " +
                     "FROM timetable WHERE sapid = ? ORDER BY subjects";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, this.currentUserSapid);
            ResultSet rs = pstmt.executeQuery();

            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                String subject = rs.getString("subjects");
                int attended = rs.getInt("attended");
                int taken = rs.getInt("taken");
                int target = rs.getInt("target");

                String canLeaveStr = "N/A";
                if (target > 0) {
                    int minRequiredAttendance = (int) Math.ceil(target * 0.80); // Assuming 80% target
                    int maxAllowedAbsences = target - minRequiredAttendance;
                    int absencesSoFar = taken - attended;
                    int canLeaveMore = Math.max(0, maxAllowedAbsences - absencesSoFar);
                    canLeaveStr = String.valueOf(canLeaveMore);
                } else {
                     canLeaveStr = "(Set Target)";
                }
                stats.append(String.format(" %-25s | %8d | %8d | %8d | %10s \n",
                        subject, attended, taken, target, canLeaveStr));
            }

            if (!hasData) {
                stats.append("\n       No attendance data found for this user.\n");
            }
            stats.append(" ------------------------------------------------------------------------------\n");
            stats.append(" *'Can Leave' estimates additional lectures missable for potential 80% attendance\n");
            stats.append("  by 'Target' count. Assumes remaining lectures are taken. Requires 'Target' > 0.\n");
            stats.append(" ==============================================================================\n");


        } catch (SQLException e) {
            e.printStackTrace();
            stats.append("\n\nError loading statistics: ").append(e.getMessage());
            JOptionPane.showMessageDialog(this, "Error loading statistics: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        statsTextArea.setText(stats.toString());
        statsTextArea.setCaretPosition(0); // Scroll to top

        JScrollPane statsScrollPane = new JScrollPane(statsTextArea);
        statsScrollPane.setBorder(new LineBorder(COLOR_BORDER));
        statsScrollPane.setPreferredSize(new Dimension(650, 350)); // Dialog size

         // Show in a dialog
         UIManager.put("OptionPane.background", COLOR_BACKGROUND);
         UIManager.put("Panel.background", COLOR_BACKGROUND);
         JOptionPane.showMessageDialog(this, statsScrollPane,
                                       "Attendance Statistics", JOptionPane.INFORMATION_MESSAGE);
         UIManager.put("OptionPane.background", null); // Reset
         UIManager.put("Panel.background", null);
    }


    // Close connection when the frame is closed
    @Override
    public void dispose() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        super.dispose();
    }
}