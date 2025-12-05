package com.wsims.repository;


import com.parami.wsims.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Custom method to find a user by their username
    Optional<User> findByUsername(String username);
    
    // Search users by name, email, or username
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Find users by role
    Page<User> findByRoleName(String roleName, Pageable pageable);
    
    // Find users by role with search term
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<User> findByRoleAndSearchTerm(@Param("roleName") String roleName, @Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Check if email exists (excluding current user for updates)
    boolean existsByEmailAndIdNot(String email, Long id);
    
    // Check if username exists (excluding current user for updates)
    boolean existsByUsernameAndIdNot(String username, Long id);
    
    // Messaging-related queries - very simple approach
    @Query("SELECT u FROM User u WHERE u.role.name = 'STUDENT'")
    List<User> findStudentsForTeacher(@Param("teacherId") Long teacherId);
    
    @Query("SELECT DISTINCT p FROM User p " +
           "JOIN StudentParentLink spl ON p.id = spl.id.parentUserId " +
           "WHERE spl.id.studentUserId = :studentId AND p.role.name = 'PARENT'")
    List<User> findParentsByStudentId(@Param("studentId") Long studentId);
    
    List<User> findByRoleName(String roleName);
    
    // Find students by grade level - removed as gradeLevel field doesn't exist in User entity
    // @Query("SELECT u FROM User u WHERE u.role.name = 'STUDENT' AND u.gradeLevel = :gradeLevel")
    // List<User> findStudentsByGradeLevel(@Param("gradeLevel") String gradeLevel);
}