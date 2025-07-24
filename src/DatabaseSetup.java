import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {

    // --- !!! IMPORTANT CONFIGURATION - USER MAY NEED TO CHANGE THESE !!! ---
    // Database Connection Details for LOCAL Oracle instance
    // Common URL format: jdbc:oracle:thin:@<hostname>:<port>:<SID_or_ServiceName>
    // For Oracle XE, SID is often 'XE' or 'XEPDB1' (for pluggable DBs)
    // For other versions, it might be 'ORCL' or similar.
    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:XE"; // Adjust SID (XE/XEPDB1/ORCL...) if needed
    private static final String DB_USER = "system"; // Common admin user
    private static final String DB_PASSWORD = "123"; // !! CHANGE THIS to your local system password !!

    // Names of the tables to check/create (Oracle stores them typically in UPPERCASE)
    private static final String USERS_TABLE = "USERS";
    private static final String TIMETABLE_TABLE = "TIMETABLE";

    // SQL Script for creating tables
    // NOTE: Removed DROP statements to avoid accidental data loss.
    //       The logic now checks if tables exist first.
    private static final String SETUP_SQL =
            // -----------------------------------------------------
            // Table: users
            // Stores user login credentials and status
            // -----------------------------------------------------
            "CREATE TABLE users ( " +
            "  sapid VARCHAR2(20) NOT NULL, " +
            "  name VARCHAR2(100) NOT NULL, " +
            "  password VARCHAR2(100) NOT NULL, " + // STORE HASHED VALUE IN REAL APP!
            "  subjects_added NUMBER(1) DEFAULT 0 NOT NULL " +
            ")"
            + ";" + // Statement separator
            // Add Primary Key constraint for users table
            "ALTER TABLE users ADD CONSTRAINT pk_users PRIMARY KEY (sapid)"
            + ";" +
            // Add Check constraint for the flag
            "ALTER TABLE users ADD CONSTRAINT chk_subjects_added CHECK (subjects_added IN (0, 1))"
            + ";" +
            "COMMENT ON TABLE users IS 'Stores user login credentials and status flag for subject registration.'"
            + ";" +
            "COMMENT ON COLUMN users.sapid IS 'User''s unique SAP ID (Primary Key)'"
            + ";" +
            "COMMENT ON COLUMN users.password IS 'User''s password - SHOULD BE HASHED!'"
            + ";" +
            "COMMENT ON COLUMN users.subjects_added IS 'Flag indicating if user has added subjects (0=No, 1=Yes)'"
            + ";" +
            // -----------------------------------------------------
            // Table: timetable
            // Stores subjects registered by a user and their attendance details
            // -----------------------------------------------------
            "CREATE TABLE timetable ( " +
            "  sapid VARCHAR2(20) NOT NULL, " +
            "  subjects VARCHAR2(100) NOT NULL, " +
            "  target_total_lectures NUMBER DEFAULT 0 NOT NULL, " +
            "  attended_count NUMBER DEFAULT 0 NOT NULL, " +
            "  lectures_taken_so_far NUMBER DEFAULT 0 NOT NULL " +
            ")"
            + ";" +
            // Add Composite Primary Key constraint for timetable table
            "ALTER TABLE timetable ADD CONSTRAINT pk_timetable PRIMARY KEY (sapid, subjects)"
            + ";" +
            // Add Foreign Key constraint linking timetable to users
            "ALTER TABLE timetable ADD CONSTRAINT fk_timetable_user " +
            "  FOREIGN KEY (sapid) " +
            "  REFERENCES users(sapid) " +
            "  ON DELETE CASCADE" // If a user is deleted, their timetable entries are also deleted
            + ";" +
            "COMMENT ON TABLE timetable IS 'Stores subjects registered by users and their attendance tracking data.'"
            + ";" +
            "COMMENT ON COLUMN timetable.sapid IS 'Foreign Key referencing the user''s SAP ID'"
            + ";" +
            "COMMENT ON COLUMN timetable.subjects IS 'Name of the subject registered by the user'"
            + ";" +
            "COMMENT ON COLUMN timetable.target_total_lectures IS 'Total number of lectures planned for the subject'"
            + ";" +
            "COMMENT ON COLUMN timetable.attended_count IS 'Number of lectures marked as attended by the user'"
            + ";" +
            "COMMENT ON COLUMN timetable.lectures_taken_so_far IS 'Number of lectures where attendance (P/A) was recorded'"
            + ";" +
            // Commit the changes
            "COMMIT"; // Explicit COMMIT

    /**
     * Checks if the required database tables exist and creates them if they don't.
     * MUST be called at the start of your application.
     *
     * @return true if setup was successful or already done, false otherwise.
     */
    public static boolean setupDatabaseIfNeeded() {
        System.out.println("--- Database Setup Check ---");

        // 1. Load the Oracle JDBC driver
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            System.out.println("Oracle JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("ERROR: Oracle JDBC Driver not found!");
            System.err.println("Please ensure the Oracle JDBC driver JAR (e.g., ojdbc11.jar) is included in the classpath.");
            e.printStackTrace();
            return false;
        }

        // 2. Check if tables already exist
        boolean usersExists = false;
        boolean timetableExists = false;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Connected to database: " + DB_URL);
            DatabaseMetaData dbm = conn.getMetaData();

            // Check for USERS table (use uppercase as Oracle typically stores it)
            try (ResultSet tables = dbm.getTables(null, null, USERS_TABLE, new String[]{"TABLE"})) {
                if (tables.next()) {
                    usersExists = true;
                    System.out.println("Table '" + USERS_TABLE + "' already exists.");
                } else {
                    System.out.println("Table '" + USERS_TABLE + "' not found.");
                }
            }

            // Check for TIMETABLE table
            try (ResultSet tables = dbm.getTables(null, null, TIMETABLE_TABLE, new String[]{"TABLE"})) {
                if (tables.next()) {
                    timetableExists = true;
                    System.out.println("Table '" + TIMETABLE_TABLE + "' already exists.");
                } else {
                    System.out.println("Table '" + TIMETABLE_TABLE + "' not found.");
                }
            }

        } catch (SQLException e) {
            System.err.println("ERROR: Failed to connect to the database or check table status.");
            System.err.println("Please ensure:");
            System.err.println("  1. Oracle Database (e.g., XE) is installed and running.");
            System.err.println("  2. The Listener service is running (port 1521 by default).");
            System.err.println("  3. The DB_URL, DB_USER, and DB_PASSWORD constants in DatabaseSetup.java are correct for your local setup.");
            System.err.println("  URL: " + DB_URL + ", User: " + DB_USER);
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            return false; // Cannot proceed if connection fails
        }

        // 3. If tables DO NOT exist, run the setup script
        if (!usersExists || !timetableExists) {
            System.out.println("Required table(s) missing. Attempting database setup...");

            // Use try-with-resources for Connection and Statement
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement()) {

                System.out.println("Connected to database for setup: " + DB_URL);

                // Split the script into individual statements based on semicolon (;)
                // This is a basic split, assumes no semicolons within strings/comments in the SQL
                String[] sqlStatements = SETUP_SQL.split(";");

                for (String sql : sqlStatements) {
                    sql = sql.trim(); // Remove leading/trailing whitespace
                    if (!sql.isEmpty()) {
                        try {
                            System.out.println("Executing SQL: " + sql.substring(0, Math.min(sql.length(), 80)) + (sql.length() > 80 ? "..." : "")); // Print truncated SQL
                            stmt.execute(sql); // Use execute() for DDL and mixed statements
                        } catch (SQLException e) {
                            // Handle cases where a specific object might already exist (e.g., constraint)
                            // ORA-00955: name is already used by an existing object
                            // ORA-02260: table can have only one primary key
                            // ORA-02264: name already used by an existing constraint
                            // ORA-01442: column to be modified to NOT NULL is already NOT NULL (less likely here)
                             if (e.getErrorCode() == 955 || e.getErrorCode() == 2260 || e.getErrorCode() == 2264 || e.getErrorCode() == 1442) {
                                System.out.println("  -> Warning: Object likely already exists (Error code: " + e.getErrorCode() + "). Skipping this statement.");
                             } else {
                                 // Throw other errors
                                throw e;
                             }
                        }
                    }
                }

                System.out.println("Database setup script executed successfully.");
                System.out.println("--- Database Setup Finished ---");
                return true;

            } catch (SQLException e) {
                System.err.println("ERROR: Database setup failed during SQL execution!");
                System.err.println("SQLState: " + e.getSQLState());
                System.err.println("Error Code: " + e.getErrorCode());
                e.printStackTrace();
                System.out.println("--- Database Setup Failed ---");
                return false;
            }
        } else {
            System.out.println("Database tables already exist. No setup needed.");
            System.out.println("--- Database Setup Finished ---");
            return true;
        }
    }

    // Optional: A simple main method to test the setup standalone
    public static void main(String[] args) {
        System.out.println("Running Database Setup Test...");
        boolean setupOk = setupDatabaseIfNeeded();
        if (setupOk) {
            System.out.println("Database setup check completed successfully.");
            // You could add code here to verify connection again or test inserts/selects
        } else {
            System.err.println("Database setup check failed. Please review the errors above.");
        }
    }
}