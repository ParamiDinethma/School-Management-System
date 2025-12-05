package com.wsims.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "teachers")
public class Teacher {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "department", length = 100)
    private String department;
}
