package com.wsims.repository;

import com.parami.wsims.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    // Custom method to find a role by its name
    Optional<Role> findByName(String name);
}
