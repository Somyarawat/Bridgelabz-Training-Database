package com.lms;

import com.lms.dao.AdminDAO;
import com.lms.dao.CandidateDAO;
import com.lms.dao.OnboardingDAO;

import java.util.Scanner;

/**
 * CLI entry point for the Learning Management System (LMS).
 * Instantiating the DAOs triggers all table creation / default-data seeding.
 */
public class LMSApplication {

    private static final Scanner scanner = new Scanner(System.in);

    private static CandidateDAO candidateDAO;
    private static OnboardingDAO onboardingDAO;
    private static AdminDAO adminDAO;

    public static void main(String[] args) {
        System.out.println("Initializing Learning Management System...");
        try {
            // Order matters: jobs/candidates/interviewers tables must exist
            // before onboarding (FK to candidates) and admin (inserts into jobs/courses).
            candidateDAO = new CandidateDAO();
            onboardingDAO = new OnboardingDAO();
            adminDAO = new AdminDAO();
        } catch (Exception e) {
            System.err.println("Failed to initialize the application: " + e.getMessage());
            System.err.println("Please verify PostgreSQL is running and that the 'lms_db' database exists.");
            return;
        }
        System.out.println("System ready.\n");

        mainMenu();

        scanner.close();
    }

    private static void mainMenu() {
        while (true) {
            System.out.println("\n===== LMS Main Menu =====");
            System.out.println("1. Admin Dashboard");
            System.out.println("2. Candidate Hiring Portal");
            System.out.println("3. Candidate Onboarding Portal");
            System.out.println("4. Exit");
            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 -> adminLogin();
                case 2 -> hiringPortal();
                case 3 -> onboardingPortal();
                case 4 -> {
                    System.out.println("Exiting LMS. Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // ---------------------------------------------------------------
    // Module A: Admin Dashboard
    // ---------------------------------------------------------------

    private static void adminLogin() {
        System.out.println("\n--- Admin Login ---");
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        if (adminDAO.authenticateAdmin(email, password)) {
            System.out.println("Login successful. Welcome, Admin!");
            adminMenu();
        } else {
            System.out.println("Invalid credentials. Access denied.");
        }
    }

    private static void adminMenu() {
        while (true) {
            System.out.println("\n===== Admin Dashboard =====");
            System.out.println("1. Add Job Opening");
            System.out.println("2. Add Training Course");
            System.out.println("3. Add Interviewer");
            System.out.println("4. View Candidate Hiring Pipeline");
            System.out.println("5. View Onboarding Summary");
            System.out.println("6. Logout");
            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 -> {
                    System.out.print("Job Title: ");
                    String title = scanner.nextLine().trim();
                    System.out.print("Department: ");
                    String dept = scanner.nextLine().trim();
                    adminDAO.addJob(title, dept);
                }
                case 2 -> {
                    System.out.print("Course Title: ");
                    String title = scanner.nextLine().trim();
                    System.out.print("Description: ");
                    String desc = scanner.nextLine().trim();
                    adminDAO.addCourse(title, desc);
                }
                case 3 -> {
                    System.out.print("Interviewer Name: ");
                    String name = scanner.nextLine().trim();
                    System.out.print("Interviewer Email: ");
                    String email = scanner.nextLine().trim();
                    adminDAO.addInterviewer(name, email);
                }
                case 4 -> adminDAO.viewHiringPipeline();
                case 5 -> adminDAO.viewOnboardingSummary();
                case 6 -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // ---------------------------------------------------------------
    // Module B: Candidate Hiring Portal
    // ---------------------------------------------------------------

    private static void hiringPortal() {
        while (true) {
            System.out.println("\n===== Candidate Hiring Portal =====");
            System.out.println("1. View Open Jobs");
            System.out.println("2. Apply for a Job");
            System.out.println("3. List Candidates");
            System.out.println("4. View Candidate Full Profile");
            System.out.println("5. List Interviewers");
            System.out.println("6. Schedule Interview");
            System.out.println("7. List Interviews");
            System.out.println("8. Submit Interview Feedback & Score");
            System.out.println("9. Back to Main Menu");
            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 -> candidateDAO.listOpenJobs();
                case 2 -> applyForJob();
                case 3 -> candidateDAO.listAllCandidates();
                case 4 -> {
                    int id = readInt("Enter Candidate ID: ");
                    candidateDAO.viewCandidateDetails(id);
                }
                case 5 -> candidateDAO.listInterviewers();
                case 6 -> scheduleInterview();
                case 7 -> candidateDAO.listInterviews();
                case 8 -> submitFeedback();
                case 9 -> { return; }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void applyForJob() {
        System.out.println("\n--- Apply for a Job ---");
        System.out.print("Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Phone: ");
        String phone = scanner.nextLine().trim();
        int jobId = readInt("Job ID (see 'View Open Jobs'): ");

        System.out.print("Skills (comma-separated): ");
        String[] skills = scanner.nextLine().split(",");

        System.out.print("Previous Companies (comma-separated, leave blank if none): ");
        String[] companies = scanner.nextLine().split(",");

        boolean success = candidateDAO.applyJob(name, email, phone, jobId, skills, companies);
        if (success) {
            System.out.println("Application submitted successfully!");
        } else {
            System.out.println("Application failed. No data was saved (transaction rolled back).");
        }
    }

    private static void scheduleInterview() {
        int candidateId = readInt("Candidate ID: ");
        int interviewerId = readInt("Interviewer ID: ");
        System.out.print("Interview Date/Time (yyyy-MM-dd HH:mm:ss): ");
        String dateTime = scanner.nextLine().trim();
        candidateDAO.scheduleInterview(candidateId, interviewerId, dateTime);
    }

    private static void submitFeedback() {
        int interviewId = readInt("Interview ID: ");
        int score = readInt("Score (0-100): ");
        System.out.print("Feedback: ");
        String feedback = scanner.nextLine().trim();
        System.out.print("Status (e.g., Passed/Failed): ");
        String status = scanner.nextLine().trim();
        candidateDAO.submitInterviewScore(interviewId, score, feedback, status);
    }

    // ---------------------------------------------------------------
    // Module C: Candidate Onboarding Portal
    // ---------------------------------------------------------------

    private static void onboardingPortal() {
        while (true) {
            System.out.println("\n===== Candidate Onboarding Portal =====");
            System.out.println("1. Hire Candidate");
            System.out.println("2. View Candidate Training Courses");
            System.out.println("3. Update Training Course Progress");
            System.out.println("4. Back to Main Menu");
            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 -> {
                    int id = readInt("Candidate ID to hire: ");
                    onboardingDAO.hireCandidate(id);
                }
                case 2 -> {
                    int id = readInt("Candidate ID: ");
                    onboardingDAO.viewCandidateCourses(id);
                }
                case 3 -> {
                    int candidateId = readInt("Candidate ID: ");
                    int courseId = readInt("Course ID: ");
                    System.out.print("Status (e.g., In Progress/Completed): ");
                    String status = scanner.nextLine().trim();
                    int percentage = readInt("Progress Percentage (0-100): ");
                    onboardingDAO.updateCourseProgress(candidateId, courseId, status, percentage);
                }
                case 4 -> { return; }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter a numeric value.");
            }
        }
    }
}
