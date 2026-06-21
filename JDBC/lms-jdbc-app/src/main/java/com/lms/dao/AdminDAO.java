package com.lms.dao;

import com.lms.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles table initialization and administrative operations:
 * job postings, training courses, interviewers, and reporting views.
 */
public class AdminDAO {

    public AdminDAO() {
        initializeTable();
        seedDefaultAdmin();
    }

    private void initializeTable() {
        String sql = "CREATE TABLE IF NOT EXISTS admins (" +
                "id SERIAL PRIMARY KEY," +
                "name VARCHAR(100) NOT NULL," +
                "email VARCHAR(150) UNIQUE NOT NULL," +
                "password VARCHAR(100) NOT NULL" +
                ")";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating admins table: " + e.getMessage());
        }
    }

    private void seedDefaultAdmin() {
        String checkSql = "SELECT COUNT(*) FROM admins WHERE email = ?";
        String insertSql = "INSERT INTO admins (name, email, password) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setString(1, "admin@gmail.com");
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                            insert.setString(1, "User Admin");
                            insert.setString(2, "admin@gmail.com");
                            insert.setString(3, "260124");
                            insert.executeUpdate();
                            System.out.println("Default admin account seeded (admin@gmail.com).");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
        }
    }

    public boolean authenticateAdmin(String email, String password) {
        String sql = "SELECT id FROM admins WHERE email = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
            return false;
        }
    }

    public void addJob(String title, String department) {
        String sql = "INSERT INTO jobs (title, department) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, department);
            ps.executeUpdate();
            System.out.println("Job opening added successfully.");
        } catch (SQLException e) {
            System.err.println("Error adding job: " + e.getMessage());
        }
    }

    public void addCourse(String title, String description) {
        String sql = "INSERT INTO courses (title, description) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.executeUpdate();
            System.out.println("Training course added successfully.");
        } catch (SQLException e) {
            System.err.println("Error adding course: " + e.getMessage());
        }
    }

    public void addInterviewer(String name, String email) {
        String sql = "INSERT INTO interviewers (name, email) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.executeUpdate();
            System.out.println("Interviewer registered successfully.");
        } catch (SQLException e) {
            System.err.println("Error adding interviewer: " + e.getMessage());
        }
    }

    public void viewHiringPipeline() {
        String sql = "SELECT c.id, c.name, c.email, j.title AS job_title, c.status " +
                "FROM candidates c LEFT JOIN jobs j ON c.job_id = j.id ORDER BY c.id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- Candidate Hiring Pipeline ---");
            System.out.printf("%-5s %-20s %-25s %-25s %-15s%n", "ID", "Name", "Email", "Job", "Status");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%-5d %-20s %-25s %-25s %-15s%n",
                        rs.getInt("id"), rs.getString("name"), rs.getString("email"),
                        rs.getString("job_title"), rs.getString("status"));
            }
            if (!any) System.out.println("No candidates found yet.");
        } catch (SQLException e) {
            System.err.println("Error viewing hiring pipeline: " + e.getMessage());
        }
    }

    public void viewOnboardingSummary() {
        String sql = "SELECT c.id, c.name, o.status AS onboarding_status, " +
                "COUNT(cc.id) AS total_courses, " +
                "SUM(CASE WHEN cc.status = 'Completed' THEN 1 ELSE 0 END) AS completed_courses " +
                "FROM onboardings o " +
                "JOIN candidates c ON o.candidate_id = c.id " +
                "LEFT JOIN candidate_courses cc ON cc.candidate_id = c.id " +
                "GROUP BY c.id, c.name, o.status " +
                "ORDER BY c.id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- Onboarding Summary ---");
            System.out.printf("%-5s %-20s %-15s %-10s %-10s%n", "ID", "Name", "Status", "Total", "Completed");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%-5d %-20s %-15s %-10d %-10d%n",
                        rs.getInt("id"), rs.getString("name"), rs.getString("onboarding_status"),
                        rs.getInt("total_courses"), rs.getInt("completed_courses"));
            }
            if (!any) System.out.println("No onboarded candidates yet.");
        } catch (SQLException e) {
            System.err.println("Error viewing onboarding summary: " + e.getMessage());
        }
    }
}
