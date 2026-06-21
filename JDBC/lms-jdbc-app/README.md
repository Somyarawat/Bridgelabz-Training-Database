# LMS JDBC Application

Console-based Learning Management System (Candidate Hiring + Onboarding + Admin)
built with Java JDBC and PostgreSQL.

## Before you open this in IntelliJ

You only need to do **two things** on your machine first — the app builds its own
tables automatically.

### 1. Create the PostgreSQL database (one time only)

The app connects to a database called `lms_db`. PostgreSQL must already have this
database before the app's first run (Java cannot create the database it's about
to connect to). Open `psql` or pgAdmin and run:

```sql
CREATE DATABASE lms_db;
```

(A copy of this is also in `create_database.sql` in this folder.)

### 2. Confirm your PostgreSQL credentials match the code

This project is hard-coded (per the assignment spec) to connect with:

```
URL:      jdbc:postgresql://localhost:5432/lms_db
Username: postgres
Password: Kiran@12
```

If your local PostgreSQL installation uses a **different password** (or a
different port), open:

```
src/main/java/com/lms/config/DatabaseConnection.java
```

and update `DB_URL`, `DB_USER`, `DB_PASSWORD` to match your setup. This is the
only file you should ever need to touch for configuration.

## Opening in IntelliJ IDEA

1. **File → Open** → select the `lms-jdbc-app` folder (the one containing `pom.xml`).
2. IntelliJ detects it as a Maven project and shows a Maven import notification
   in the bottom-right — click **Load Maven Project** (or it loads automatically
   if the Maven plugin auto-import is on). This downloads the PostgreSQL driver
   automatically; you need an internet connection for this one-time step.
3. Wait for the elephant/Maven icon in the bottom status bar to finish indexing.
4. Make sure IntelliJ's configured **Project SDK is Java 17 or higher**:
   `File → Project Structure → Project → SDK`. If no SDK is listed, click
   **Add SDK → Download JDK** and pick version 17 or 21.
5. Open `src/main/java/com/lms/LMSApplication.java` and click the green ▶ run
   arrow next to `public static void main(...)`. That's it — no run
   configuration setup needed, IntelliJ creates one automatically.

## Running

* Make sure your local PostgreSQL **server is running** (e.g. via pgAdmin,
  `services.msc` on Windows, `brew services start postgresql` on Mac, or
  `sudo systemctl start postgresql` on Linux) before clicking Run.
* On first run you'll see console output confirming tables were created and the
  default admin + default courses were seeded.
* Default admin login: `admin@gmail.com` / `Kiran@12`

## Resetting the database for a clean test run

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

Run the app again afterward — all tables and seed data regenerate automatically.

## Project structure

```
lms-jdbc-app/
├── pom.xml
├── create_database.sql
└── src/main/java/com/lms/
    ├── LMSApplication.java        Main CLI entry point
    ├── config/DatabaseConnection.java
    ├── dao/AdminDAO.java
    ├── dao/CandidateDAO.java
    ├── dao/OnboardingDAO.java
    └── model/{Candidate,Course,Job}.java
```
