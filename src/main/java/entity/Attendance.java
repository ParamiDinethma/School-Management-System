package com.wsims.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private User student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    @JsonIgnore
    private Subject subject;
    
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;
    
    @Column(name = "remarks", length = 200)
    private String remarks;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by")
    @JsonIgnore
    private User markedBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
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
    
    public Long getSubjectId() {
        return subject != null ? subject.getId() : null;
    }
    
    public String getSubjectName() {
        return subject != null ? subject.getSubjectName() : null;
    }
    
    public String getSubjectCode() {
        return subject != null ? subject.getSubjectCode() : null;
    }
    
    public Long getMarkedById() {
        return markedBy != null ? markedBy.getId() : null;
    }
    
    public String getMarkedByName() {
        return markedBy != null ? markedBy.getFirstName() + " " + markedBy.getLastName() : null;
    }
    
    public enum AttendanceStatus {
        PRESENT, ABSENT, LATE, EXCUSED
    }
}