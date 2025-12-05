package com.wsims.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_name", nullable = false, length = 255)
    private String examName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "academic_year", length = 10)
    private String academicYear;

    @Column(name = "semester", length = 50)
    private String semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ExamStatus status = ExamStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Enum for exam status
    public enum ExamStatus {
        ACTIVE,
        INACTIVE,
        COMPLETED
    }

    // Helper methods
    public boolean isActive() {
        return status == ExamStatus.ACTIVE;
    }

    public boolean isCompleted() {
        return status == ExamStatus.COMPLETED;
    }

    public boolean isUpcoming() {
        return startDate.isAfter(LocalDate.now()) && isActive();
    }

    public boolean isOngoing() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate) && isActive();
    }

    public boolean isPast() {
        return endDate.isBefore(LocalDate.now());
    }

    // Validation method
    public boolean isValidDateRange() {
        return endDate.isAfter(startDate) || endDate.isEqual(startDate);
    }
}

