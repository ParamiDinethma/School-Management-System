package com.wsims.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "parents")
public class Parent {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "occupation")
    private String occupation;

    // Manual getters and setters (temporary fix for Lombok issue)
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
}
