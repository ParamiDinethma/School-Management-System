package com.wsims.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subjects")
public class Subject {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_name", nullable = false, length = 100)
    private String subjectName;

    @Column(name = "subject_code", nullable = false, unique = true, length = 20)
    private String subjectCode;

    @Column(name = "description", length = 500)
    private String description;

    // Note: These columns don't exist in the current database schema
    // They are kept for future use when the database is updated
    // @Column(name = "credits")
    // private Integer credits;

    // @Column(name = "hours_per_week")
    // private Integer hoursPerWeek;

    // @Column(name = "status")
    // private String status;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CourseSubjectLink> courseSubjects;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Attendance> attendances;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Manual getters and setters (temporary fix for Lombok issue)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<CourseSubjectLink> getCourseSubjects() { return courseSubjects; }
    public void setCourseSubjects(List<CourseSubjectLink> courseSubjects) { this.courseSubjects = courseSubjects; }
    public List<Attendance> getAttendances() { return attendances; }
    public void setAttendances(List<Attendance> attendances) { this.attendances = attendances; }
}
