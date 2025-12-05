package com.wsims.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data // Lombok: Generates getters, setters, toString, etc.
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    // Manual getters and setters (temporary fix for Lombok issue)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}