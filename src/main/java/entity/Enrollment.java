package com.wsims.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "enrollments")
public class Enrollment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE"; // ACTIVE, COMPLETED, DROPPED, SUSPENDED

    @Column(name = "grade", length = 5)
    private String grade; // A, B, C, D, F

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (enrollmentDate == null) {
            enrollmentDate = LocalDate.now();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods for JSON serialization
    public Long getStudentId() {
        return student != null ? student.getId() : null;
    }

    public String getStudentName() {
        return student != null ? student.getFirstName() + " " + student.getLastName() : null;
    }

    public String getStudentUsername() {
        return student != null ? student.getUsername() : null;
    }

    public Long getCourseId() {
        return course != null ? course.getId() : null;
    }

    public String getCourseName() {
        return course != null ? course.getCourseName() : null;
    }

    public String getCourseCode() {
        return course != null ? course.getCourseCode() : null;
    }

    // Manual getters and setters (temporary fix for Lombok issue)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}