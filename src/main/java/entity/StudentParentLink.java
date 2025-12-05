package com.wsims.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_parent_link")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentParentLink {

    @EmbeddedId
    private StudentParentLinkId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentUserId")
    @JoinColumn(name = "student_user_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("parentUserId")
    @JoinColumn(name = "parent_user_id")
    private Parent parent;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // Admin who created the link

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Composite key class
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentParentLinkId implements java.io.Serializable {
        @Column(name = "student_user_id")
        private Long studentUserId;

        @Column(name = "parent_user_id")
        private Long parentUserId;
    }
}

