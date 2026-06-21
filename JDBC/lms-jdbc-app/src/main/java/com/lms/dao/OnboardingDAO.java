package com.lms.dao;

import com.lms.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages candidates transitioning into onboarding and training courses.
 */
public class OnboardingDAO {

    public OnboardingDAO() {
        initializeTables();
        seedDefaultCourses();
    }

    private void initializeTables() {
        String onboardings = "CREATE TABLE IF NOT EXISTS onboardings (" +
                "id SERIAL PRIMARY KEY," +
                "candidate_id INT NOT NULL UNIQUE REFERENCES candidates(id)," +
                "status VARCHAR(30) NOT NULL DEFAULT 'In Progress'," +
                "start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        String courses = "CREATE TABLE IF NOT EXISTS courses (" +
                "id SERIAL PRIMARY KEY," +
                "title VARCHAR(150) NOT NULL," +
                "description TEXT" +
                ")";

        String candidateCourses = "CREATE TABLE IF NOT EXISTS candidate_courses (" +
                "id SERIAL PRIMARY KEY," +
                "candidate_id INT NOT NULL REFERENCES candidates(id)," +
                "course_id INT NOT NULL REFERENCES courses(id)," +
                "progress_percentage INT NOT NULL DEFAULT 0 CHECK (progress_percentage >= 0 AND progress_percentage <= 100)," +
                "status VARCHAR(30) NOT NULL DEFAULT 'Not Started'" +
                ")";

        String idxCandidateCourses = "CREATE INDEX IF NOT EXISTS idx_candidate_courses_candidate ON candidate_courses(candidate_id)";
        String idxOnboardings = "CREATE INDEX IF NOT EXISTS idx_onboardings_candidate ON onboardings(candidate_id)";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(onboardings);
            stmt.execute(courses);
            stmt.execute(candidateCourses);
            stmt.execute(idxCandidateCourses);
            stmt.execute(idxOnboardings);
        } catch (SQLException e) {
            System.err.println("Error initializing onboarding tables: " + e.getMessage());
        }
    }

    private void seedDefaultCourses() {
        String countSql = "SELECT COUNT(*) FROM courses";
        String insertSql = "INSERT INTO courses (title, description) VALUES (?, ?)";
        String[][] defaultCourses = {
                {"Java Advanced JDBC Programming", "Deep dive into JDBC, connection pooling, and transaction management."},
                {"Enterprise Git & Version Control", "Branching strategies, code review workflows, and team collaboration."},
                {"Information Security & Compliance", "Core principles of data security, privacy, and regulatory compliance."}
        };

        try (Connection conn = DatabaseConnection.getConnection()) {
            int count;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {
                count = rs.next() ? rs.getInt(1) : -1;
            }

            if (count == 0) {
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    for (String[] course : defaultCourses) {
                        ps.setString(1, course[0]);
                        ps.setString(2, course[1]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    System.out.println("Default training courses seeded.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error seeding default courses: " + e.getMessage());
        }
    }

    /**
     * Atomic transaction: marks candidate as Hired, creates an onboarding profile,
     * and maps ALL existing courses to the candidate at 0% progress.
     */
    public boolean hireCandidate(int candidateId) {
        String checkStatusSql = "SELECT status FROM candidates WHERE id = ?";
        String updateStatusSql = "UPDATE candidates SET status = 'Hired' WHERE id = ?";
        String insertOnboardingSql = "INSERT INTO onboardings (candidate_id, status) VALUES (?, 'In Progress')";
        String courseIdsSql = "SELECT id FROM courses";
        String mapCourseSql = "INSERT INTO candidate_courses (candidate_id, course_id, progress_percentage, status) " +
                "VALUES (?, ?, 0, 'Not Started')";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String currentStatus = null;
            try (PreparedStatement ps = conn.prepareStatement(checkStatusSql)) {
                ps.setInt(1, candidateId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentStatus = rs.getString("status");
                    } else {
                        System.out.println("No candidate found with ID " + candidateId);
                        conn.rollback();
                        return false;
                    }
                }
            }

            if ("Hired".equalsIgnoreCase(currentStatus)) {
                System.out.println("Candidate is already hired.");
                conn.rollback();
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(updateStatusSql)) {
                ps.setInt(1, candidateId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(insertOnboardingSql)) {
                ps.setInt(1, candidateId);
                ps.executeUpdate();
            }

            List<Integer> courseIds = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(courseIdsSql)) {
                while (rs.next()) {
                    courseIds.add(rs.getInt("id"));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(mapCourseSql)) {
                for (int courseId : courseIds) {
                    ps.setInt(1, candidateId);
                    ps.setInt(2, courseId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            System.out.println("Candidate hired and onboarded. " + courseIds.size() + " training course(s) assigned.");
            return true;

        } catch (SQLException e) {
            System.err.println("Error hiring candidate, rolling back transaction: " + e.getMessage());
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

    /**
     * Transaction: updates a candidate's course progress, then checks whether
     * all assigned courses are now Completed; if so, auto-completes onboarding.
     */
    public void updateCourseProgress(int candidateId, int courseId, String status, int percentage) {
        String updateSql = "UPDATE candidate_courses SET status = ?, progress_percentage = ? " +
                "WHERE candidate_id = ? AND course_id = ?";
        String countTotalSql = "SELECT COUNT(*) FROM candidate_courses WHERE candidate_id = ?";
        String countCompletedSql = "SELECT COUNT(*) FROM candidate_courses WHERE candidate_id = ? AND status = 'Completed'";
        String updateOnboardingSql = "UPDATE onboardings SET status = 'Completed' WHERE candidate_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int rows;
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, status);
                ps.setInt(2, percentage);
                ps.setInt(3, candidateId);
                ps.setInt(4, courseId);
                rows = ps.executeUpdate();
            }

            if (rows == 0) {
                System.out.println("No matching course enrollment found for candidate " + candidateId
                        + " and course " + courseId + ". (Has this candidate been hired/onboarded?)");
                conn.rollback();
                return;
            }

            int total = 0;
            int completed = 0;
            try (PreparedStatement ps = conn.prepareStatement(countTotalSql)) {
                ps.setInt(1, candidateId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) total = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(countCompletedSql)) {
                ps.setInt(1, candidateId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) completed = rs.getInt(1);
                }
            }

            if (total > 0 && total == completed) {
                try (PreparedStatement ps = conn.prepareStatement(updateOnboardingSql)) {
                    ps.setInt(1, candidateId);
                    ps.executeUpdate();
                }
                System.out.println("All courses completed - onboarding status auto-updated to 'Completed'.");
            }

            conn.commit();
            System.out.println("Course progress updated successfully.");

        } catch (SQLException e) {
            System.err.println("Error updating course progress, rolling back transaction: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
            }
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

    public void viewCandidateCourses(int candidateId) {
        String sql = "SELECT co.id, co.title, cc.progress_percentage, cc.status " +
                "FROM candidate_courses cc JOIN courses co ON cc.course_id = co.id " +
                "WHERE cc.candidate_id = ? ORDER BY co.id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n--- Training Courses for Candidate " + candidateId + " ---");
                System.out.printf("%-10s %-35s %-12s %-15s%n", "CourseID", "Title", "Progress%", "Status");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("%-10d %-35s %-12d %-15s%n",
                            rs.getInt("id"), rs.getString("title"),
                            rs.getInt("progress_percentage"), rs.getString("status"));
                }
                if (!any) System.out.println("No courses assigned. Candidate may not be onboarded yet.");
            }
        } catch (SQLException e) {
            System.err.println("Error viewing candidate courses: " + e.getMessage());
        }
    }
}
