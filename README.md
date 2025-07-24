# Attendance Management System

A simple, robust desktop application built with Java Swing for students to track their class attendance. The application connects to an Oracle database to store user and attendance data, providing statistics to help users stay above attendance requirements.

## Features

-   **User Authentication**: Secure user registration and login system.
-   **One-Time Subject Registration**: On first login, users register their subjects and the total number of lectures planned for each subject. The application is smart enough not to ask for this information on subsequent logins.
-   **Session-Based Attendance Marking**: A clean interface to mark "Present" or "Absent" for each subject for a given session.
-   **Detailed Statistics**: Users can instantly view a summary of their attendance, including:
    -   Total lectures attended per subject.
    -   Total lectures recorded so far.
    -   The target number of total lectures for the course.
    -   A calculation of how many more classes can be missed while maintaining an 80% attendance rate.
-   **Full Data Reset**: A secure, confirmation-based option to reset all attendance data for a user to start fresh.

## Technologies Used

-   **Language**: Java
-   **GUI**: Java Swing
-   **Database**: Oracle Database (Tested with Express Edition - XE)
-   **Connectivity**: Oracle JDBC (Java Database Connectivity)

---

## Setup and Installation

Follow these steps to get the project running on your local machine.

### 1. Prerequisites

-   **Java Development Kit (JDK)**: Version 8 or higher (JDK 11/17 recommended).
-   **Oracle Database**: An active installation of Oracle Database, such as the free [Express Edition (XE)](https://www.oracle.com/database/technologies/xe-downloads.html).
-   **IDE**: An Integrated Development Environment like [Eclipse](https://www.eclipse.org/), [IntelliJ IDEA](https://www.jetbrains.com/idea/), or [NetBeans](https://netbeans.apache.org/).
-   **Oracle JDBC Driver**: The `ojdbcX.jar` file compatible with your JDK and Oracle DB version. You can [download it from Oracle's website](https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html).

### 2. Database Setup

This is the most critical step. The application expects a specific database schema. Connect to your Oracle database using a tool like SQL Developer or SQL*Plus and run the following scripts.

**Script 1: Create the `users` table**
This table stores login credentials and a flag to check if subjects have been registered.

```sql
CREATE TABLE users (
    sapid VARCHAR2(20) PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    password VARCHAR2(50) NOT NULL, -- Note: In a real-world app, this should be a hashed password.
    subjects_added NUMBER(1) DEFAULT 0 NOT NULL -- 0=false, 1=true
);
```
**Script 2: Create the timetable table**
This table stores the attendance data for each subject linked to a user.
```sql
CREATE TABLE timetable (
    sapid VARCHAR2(20) NOT NULL,
    subjects VARCHAR2(100) NOT NULL,
    target_total_lectures NUMBER DEFAULT 0 NOT NULL,
    attended_count NUMBER DEFAULT 0 NOT NULL,
    lectures_taken_so_far NUMBER DEFAULT 0 NOT NULL,
    CONSTRAINT pk_timetable PRIMARY KEY (sapid, subjects),
    CONSTRAINT fk_timetable_user FOREIGN KEY (sapid) REFERENCES users(sapid) ON DELETE CASCADE
);
```
(Note: ON DELETE CASCADE ensures that if a user is deleted, all their attendance records are also deleted.)

### 3. Java Project Setup
Clone the Repository:
```
git clone <your-repository-url>
```

-    Open in IDE: Open the cloned project folder in your IDE.
-    Add JDBC Driver: Add the ojdbcX.jar file you downloaded to your project's build path.
-    Eclipse: Right-click on the project -> Build Path -> Configure Build Path -> Libraries tab -> Add External JARs... -> Select your ojdbcX.jar file.
-    IntelliJ: Go to File -> Project Structure -> Modules -> Dependencies tab -> Click the + icon -> JARs or directories... -> Select your ojdbcX.jar file.
-    Verify Connection String: Inside the .java files (e.g., login.java, Timetable.java), check the database constants and update them if your setup is different:
```java
private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:xe";
private static final String DB_USERNAME = "system";
private static final String DB_PASSWORD = "123"; // Change this to your actual password
```

Run the Application: The main entry point for the application is the login.java file. Right-click on login.java and run it as a Java Application.

**How to Use**
1.    Run login.java. The Login window will appear.
2.    If you are a new user, click Register. This will open the Registration window (LoginPage.java).
3.    Enter your details and register.
4.    Return to the Login window and log in with your new credentials.
5.    First Login: You will be directed to the "Register Subjects" window (Subject.java). Enter the name of each subject and the total number of lectures planned for the entire course. Click Save Subjects.
6.    You will now be taken to the main Attendance Tracker window (Timetable.java).
7.    For your current class session, select "Present" or "Absent" for the subjects you attended. Click SAVE Session to record the data.
8.    Click View STATS at any time to see your attendance summary.
9.    Subsequent Logins: After your first login, you will be taken directly to the Attendance Tracker window.

**Project Structure**
-    LoginPage.java: GUI for new user registration. Inserts new records into the users table.
-    login.java: The main entry point. Handles user authentication. Checks the subjects_added flag to correctly navigate to either the Subject or Timetable window.
-    Subject.java: GUI for one-time registration of subjects and their total target lectures. Populates the timetable table for the user and sets the subjects_added flag in the users table.
-    Timetable.java: The main dashboard of the application. Allows users to mark attendance, view statistics, and reset their attendance data.
