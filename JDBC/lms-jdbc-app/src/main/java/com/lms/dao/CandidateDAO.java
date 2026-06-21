package com.lms.dao;

import com.lms.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Manages the hiring workflow: job listings, candidate applications,
 * interviewer registration, interview scheduling, and feedback/scoring.
 */
public class CandidateDAO {

    public CandidateDAO() {
        initializeTables();
    }

    private void initializeTables() {
        String jobs = "CREATE TABLE IF NOT EXISTS jobs (" +
                "id SERIAL PRIMARY KEY," +
                "title VARCHAR(150) NOT NULL," +
                "department VARCHAR(100) NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        String candidates = "CREATE TABLE IF NOT EXISTS candidates (" +
                "id SERIAL PRIMARY KEY," +
                "name VARCHAR(150) NOT NULL," +
                "email VARCHAR(150) NOT NULL," +
                "phone VARCHAR(20)," +
                "job_id INT REFERENCES jobs(id)," +
                "status VARCHAR(30) NOT NULL DEFAULT 'Applied'," +
                "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        String interviewers = "CREATE TABLE IF NOT EXISTS interviewers (" +
                "id SERIAL PRIMARY KEY," +
                "name VARCHAR(150) NOT NULL," +
                "email VARCHAR(150) NOT NULL" +
                ")";

        String interviews = "CREATE TABLE IF NOT EXISTS interviews (" +
                "id SERIAL PRIMARY KEY," +
                "candidate_id INT NOT NULL REFERENCES candidates(id)," +
                "interviewer_id INT NOT NULL REFERENCES interviewers(id)," +
                "interview_date TIMESTAMP NOT NULL," +
                "score INT CHECK (score >= 0 AND score <= 100)," +
                "feedback TEXT," +
                "status VARCHAR(30) NOT NULL DEFAULT 'Scheduled'" +
                ")";

        String candidateSkills = "CREATE TABLE IF NOT EXISTS candidate_skills (" +
                "id SERIAL PRIMARY KEY," +
                "candidate_id INT NOT NULL REFERENCES candidates(id)," +
                "skill VARCHAR(100) NOT NULL" +
                ")";

        String candidateExperience = "CREATE TABLE IF NOT EXISTS candidate_experience (" +
                "id SERIAL PRIMARY KEY," +
                "candidate_id INT NOT NULL REFERENCES candidates(id)," +
                "company_name VARCHAR(150) NOT NULL" +
                ")";

        String idxEmail = "CREATE INDEX IF NOT EXISTS idx_candidate_email ON candidates(email)";
        String idxStatus = "CREATE INDEX IF NOT EXISTS idx_candidate_status ON candidates(status)";
        String idxInterviewsCandidate = "CREATE INDEX IF NOT EXISTS idx_interviews_candidate ON interviews(candidate_id)";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(jobs);
            stmt.execute(candidates);
            stmt.execute(interviewers);
            stmt.execute(interviews);
            stmt.execute(candidateSkills);
            stmt.execute(candidateExperience);
            stmt.execute(idxEmail);
            stmt.execute(idxStatus);
            stmt.execute(idxInterviewsCandidate);
        } catch (SQLException e) {
            System.err.println("Error initializing candidate/hiring tables: " + e.getMessage());
        }
    }

    /**
     * Atomic transaction: inserts the candidate, then batch-inserts skills and
     * previous companies. Rolls back everything if any step fails.
     */
    public boolean applyJob(String name, String email, String phone, int jobId, String[] skills, String[] companies) {
        String insertCandidate = "INSERT INTO candidates (name, email, phone, job_id, status) VALUES (?, ?, ?, ?, 'Applied')";
        String insertSkill = "INSERT INTO candidate_skills (candidate_id, skill) VALUES (?, ?)";
        String insertExperience = "INSERT INTO candidate_experience (candidate_id, company_name) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int candidateId;
            try (PreparedStatement ps = conn.prepareStatement(insertCandidate, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, phone);
                ps.setInt(4, jobId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        candidateId = keys.getInt(1);
                    } else {
                        throw new SQLException("Failed to retrieve generated candidate ID.");
                    }
                }
            }

            if (skills != null) {
                try (PreparedStatement ps = conn.prepareStatement(insertSkill)) {
                    for (String skill : skills) {
                        if (skill == null || skill.trim().isEmpty()) continue;
                        ps.setInt(1, candidateId);
                        ps.setString(2, skill.trim());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            if (companies != null) {
                try (PreparedStatement ps = conn.prepareStatement(insertExperience)) {
                    for (String company : companies) {
                        if (company == null || company.trim().isEmpty()) continue;
                        ps.setInt(1, candidateId);
                        ps.setString(2, company.trim());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Error applying for job, rolling back transaction: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    public void scheduleInterview(int candidateId, int interviewerId, String dateTimeStr) {
        String sql = "INSERT INTO interviews (candidate_id, interviewer_id, interview_date, status) " +
                "VALUES (?, ?, ?, 'Scheduled')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, candidateId);
            ps.setInt(2, interviewerId);
            ps.setTimestamp(3, Timestamp.valueOf(dateTimeStr));
            ps.executeUpdate();
            System.out.println("Interview scheduled successfully.");
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid date/time format. Expected: yyyy-MM-dd HH:mm:ss");
        } catch (SQLException e) {
            System.err.println("Error scheduling interview: " + e.getMessage());
        }
    }

    public void submitInterviewScore(int interviewId, int score, String feedback, String status) {
        String sql = "UPDATE interviews SET score = ?, feedback = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, score);
            ps.setString(2, feedback);
            ps.setString(3, status);
            ps.setInt(4, interviewId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Interview feedback and score submitted successfully.");
            } else {
                System.out.println("No interview found with ID " + interviewId);
            }
        } catch (SQLException e) {
            System.err.println("Error submitting interview score: " + e.getMessage());
        }
    }

    public void listAllCandidates() {
        String sql = "SELECT c.id, c.name, c.email, c.phone, j.title AS job_title, c.status " +
                "FROM candidates c LEFT JOIN jobs j ON c.job_id = j.id ORDER BY c.id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- All Candidates ---");
            System.out.printf("%-5s %-20s %-25s %-15s %-20s %-12s%n", "ID", "Name", "Email", "Phone", "Job", "Status");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%-5d %-20s %-25s %-15s %-20s %-12s%n",
                        rs.getInt("id"), rs.getString("name"), rs.getString("email"),
                        rs.getString("phone"), rs.getString("job_title"), rs.getString("status"));
            }
            if (!any) System.out.println("No candidates found yet.");
        } catch (SQLException e) {
            System.err.println("Error listing candidates: " + e.getMessage());
        }
    }

    public void listOpenJobs() {
        String sql = "SELECT id, title, department, created_at FROM jobs ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- Open Jobs ---");
            System.out.printf("%-5s %-30s %-20s%n", "ID", "Title", "Department");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%-5d %-30s %-20s%n",
                        rs.getInt("id"), rs.getString("title"), rs.getString("department"));
            }
            if (!any) System.out.println("No job openings found yet.");
        } catch (SQLException e) {
            System.err.println("Error listing jobs: " + e.getMessage());
        }
    }

    public void listInterviewers() {
        String sql = "SELECT id, name, email FROM interviewers ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- Interviewers ---");
            System.out.printf("%-5s %-25s %-25s%n", "ID", "Name", "Email");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%-5d %-25s %-25s%n",
                        rs.getInt("id"), rs.getString("name"), rs.getString("email"));
            }
            if (!any) System.out.println("No interviewers registered yet.");
        } catch (SQLException e) {
            System.err.println("Error listing interviewers: " + e.getMessage());
        }
    }

    public void listInterviews() {
        String sql = "SELECT i.id, c.name AS candidate_name, iv.name AS interviewer_name, " +
                "i.interview_date, i.score, i.status " +
                "FROM interviews i " +
                "JOIN candidates c ON i.candidate_id = c.id " +
                "JOIN interviewers iv ON i.interviewer_id = iv.id " +
                "ORDER BY i.id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- Interviews ---");
            System.out.printf("%-5s %-20s %-20s %-20s %-7s %-12s%n",
                    "ID", "Candidate", "Interviewer", "Date/Time", "Score", "Status");
            boolean any = false;
            while (rs.next()) {
                any = true;
                Object score = rs.getObject("score");
                System.out.printf("%-5d %-20s %-20s %-20s %-7s %-12s%n",
                        rs.getInt("id"), rs.getString("candidate_name"), rs.getString("interviewer_name"),
                        rs.getTimestamp("interview_date"), (score == null ? "-" : score), rs.getString("status"));
            }
            if (!any) System.out.println("No interviews scheduled yet.");
        } catch (SQLException e) {
            System.err.println("Error listing interviews: " + e.getMessage());
        }
    }

    public void viewCandidateDetails(int candidateId) {
        String candidateSql = "SELECT c.id, c.name, c.email, c.phone, j.title AS job_title, c.status " +
                "FROM candidates c LEFT JOIN jobs j ON c.job_id = j.id WHERE c.id = ?";
        String skillsSql = "SELECT skill FROM candidate_skills WHERE candidate_id = ?";
        String experienceSql = "SELECT company_name FROM candidate_experience WHERE candidate_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(candidateSql)) {
                ps.setInt(1, candidateId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("No candidate found with ID " + candidateId);
                        return;
                    }
                    System.out.println("\n--- Candidate Profile ---");
                    System.out.println("ID: " + rs.getInt("id"));
                    System.out.println("Name: " + rs.getString("name"));
                    System.out.println("Email: " + rs.getString("email"));
                    System.out.println("Phone: " + rs.getString("phone"));
                    System.out.println("Job Applied: " + rs.getString("job_title"));
                    System.out.println("Status: " + rs.getString("status"));
                }
            }

            System.out.println("Skills:");
            try (PreparedStatement ps = conn.prepareStatement(skillsSql)) {
                ps.setInt(1, candidateId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean any = false;
                    while (rs.next()) {
                        System.out.println("  - " + rs.getString("skill"));
                        any = true;
                    }
                    if (!any) System.out.println("  (none listed)");
                }
            }

            System.out.println("Previous Companies:");
            try (PreparedStatement ps = conn.prepareStatement(experienceSql)) {
                ps.setInt(1, candidateId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean any = false;
                    while (rs.next()) {
                        System.out.println("  - " + rs.getString("company_name"));
                        any = true;
                    }
                    if (!any) System.out.println("  (none listed)");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error viewing candidate details: " + e.getMessage());
        }
    }
}
