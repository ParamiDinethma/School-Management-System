package com.wsims.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "target_audience", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TargetAudience targetAudience;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum TargetAudience {
        ALL_STUDENTS,
        ALL_TEACHERS,
        ALL_PARENTS,
        ALL_USERS
    }
}


