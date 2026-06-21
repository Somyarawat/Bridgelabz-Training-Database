package com.lms.model;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO representing a row in the `candidates` table, along with its
 * related multi-valued attributes (skills, experience) held in
 * separate 4NF junction tables (candidate_skills, candidate_experience).
 */
public class Candidate {

    private int id;
    private String name;
    private String email;
    private String phone;
    private int jobId;
    private String status;
    private List<String> skills = new ArrayList<>();
    private List<String> experience = new ArrayList<>();

    public Candidate() {
    }

    public Candidate(int id, String name, String email, String phone, int jobId, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.jobId = jobId;
        this.status = status;
    }

    public Candidate(String name, String email, String phone, int jobId) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.jobId = jobId;
        this.status = "Applied";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getExperience() {
        return experience;
    }

    public void setExperience(List<String> experience) {
        this.experience = experience;
    }

    @Override
    public String toString() {
        return "Candidate{id=" + id + ", name='" + name + "', email='" + email
                + "', phone='" + phone + "', jobId=" + jobId + ", status='" + status + "'}";
    }
}
