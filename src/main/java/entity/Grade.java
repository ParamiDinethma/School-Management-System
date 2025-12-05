package com.wsims.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "grades", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"student_id", "course_id", "subject_id", "exam_schedule_id"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Grade {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    @JsonIgnore
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_schedule_id", nullable = false)
    @JsonIgnore
    private ExamSchedule examSchedule;

    @Column(name = "marks_obtained", precision = 5, scale = 2)
    private BigDecimal marksObtained;

    @Column(name = "total_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "letter_grade", length = 5)
    private String letterGrade;

    @Column(name = "grade_point", precision = 3, scale = 2)
    private BigDecimal gradePoint;

    @Column(name = "comments", length = 500)
    private String comments;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private GradeStatus status = GradeStatus.ACTIVE;

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
        calculateGradeInfo();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateGradeInfo();
    }

    // Enum for letter grades
    public enum LetterGrade {
        A_PLUS("A", new BigDecimal("4.00")),
        A("A", new BigDecimal("3.75")),
        A_MINUS("A", new BigDecimal("3.50")),
        B_PLUS("B", new BigDecimal("3.25")),
        B("B", new BigDecimal("3.00")),
        B_MINUS("B", new BigDecimal("2.75")),
        C_PLUS("C", new BigDecimal("2.50")),
        C("C", new BigDecimal("2.25")),
        C_MINUS("C", new BigDecimal("2.00")),
        D("D", new BigDecimal("1.75")),
        F("F", new BigDecimal("0.00"));

        private final String displayName;
        private final BigDecimal pointValue;

        LetterGrade(String displayName, BigDecimal pointValue) {
            this.displayName = displayName;
            this.pointValue = pointValue;
        }

        public String getDisplayName() {
            return displayName;
        }

        public BigDecimal getPointValue() {
            return pointValue;
        }
    }

    // Enum for grade status
    public enum GradeStatus {
        ACTIVE,
        INACTIVE,
        PENDING
    }

    // Helper method to calculate grade information
    private void calculateGradeInfo() {
        if (marksObtained != null && totalMarks != null && totalMarks.compareTo(BigDecimal.ZERO) > 0) {
            // Calculate percentage
            percentage = marksObtained.divide(totalMarks, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);

            // Calculate letter grade based on percentage
            LetterGrade calculatedGrade = calculateLetterGrade(percentage);
            letterGrade = calculatedGrade != null ? calculatedGrade.getDisplayName() : null;
            
            // Set grade point based on letter grade
            if (calculatedGrade != null) {
                gradePoint = calculatedGrade.getPointValue();
            }
        }
    }

    // Calculate letter grade based on percentage
    private LetterGrade calculateLetterGrade(BigDecimal percentage) {
        if (percentage.compareTo(new BigDecimal("90")) >= 0) {
            return LetterGrade.A_PLUS;
        } else if (percentage.compareTo(new BigDecimal("80")) >= 0) {
            return LetterGrade.A;
        } else if (percentage.compareTo(new BigDecimal("75")) >= 0) {
            return LetterGrade.A_MINUS;
        } else if (percentage.compareTo(new BigDecimal("70")) >= 0) {
            return LetterGrade.B_PLUS;
        } else if (percentage.compareTo(new BigDecimal("65")) >= 0) {
            return LetterGrade.B;
        } else if (percentage.compareTo(new BigDecimal("60")) >= 0) {
            return LetterGrade.B_MINUS;
        } else if (percentage.compareTo(new BigDecimal("55")) >= 0) {
            return LetterGrade.C_PLUS;
        } else if (percentage.compareTo(new BigDecimal("50")) >= 0) {
            return LetterGrade.C;
        } else if (percentage.compareTo(new BigDecimal("45")) >= 0) {
            return LetterGrade.C_MINUS;
        } else if (percentage.compareTo(new BigDecimal("40")) >= 0) {
            return LetterGrade.D;
        } else {
            return LetterGrade.F;
        }
    }

    // Helper methods
    public boolean isPassing() {
        return letterGrade != null && !letterGrade.equals("F");
    }

    public boolean isExcellent() {
        return "A".equals(letterGrade);
    }

    public boolean isGood() {
        return "A".equals(letterGrade) || "B".equals(letterGrade);
    }

    public boolean isAverage() {
        return "B".equals(letterGrade) || "C".equals(letterGrade);
    }

    public boolean isBelowAverage() {
        return "C".equals(letterGrade) || "D".equals(letterGrade);
    }

    public boolean isFailing() {
        return "F".equals(letterGrade);
    }

    // Validation methods
    public boolean isValidMarks() {
        if (marksObtained == null || totalMarks == null) {
            return false;
        }
        return marksObtained.compareTo(BigDecimal.ZERO) >= 0 && 
               marksObtained.compareTo(totalMarks) <= 0 && 
               totalMarks.compareTo(BigDecimal.ZERO) > 0;
    }

    public String getLetterGradeDisplay() {
        return letterGrade; // letterGrade is already the display name now
    }
}
