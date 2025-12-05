package com.wsims.repository;

import com.parami.wsims.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    // Find all students with pagination
    Page<Student> findAll(Pageable pageable);
    
    // Find students by grade level
    Page<Student> findByGradeLevel(String gradeLevel, Pageable pageable);
    
    // Search students by name, email, or username
    @Query("SELECT s FROM Student s JOIN s.user u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Student> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Find students by grade level with search
    @Query("SELECT s FROM Student s JOIN s.user u WHERE s.gradeLevel = :gradeLevel AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Student> findByGradeLevelAndSearchTerm(@Param("gradeLevel") String gradeLevel, 
                                               @Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Count total students
    @Query("SELECT COUNT(s) FROM Student s")
    long countTotalStudents();
    
    // Count active students (enrolled within last year)
    @Query("SELECT COUNT(s) FROM Student s WHERE s.enrollmentDate >= :cutoffDate")
    long countActiveStudents(@Param("cutoffDate") LocalDate cutoffDate);
    
    // Count inactive students (enrolled more than a year ago or no enrollment date)
    @Query("SELECT COUNT(s) FROM Student s WHERE s.enrollmentDate < :cutoffDate OR s.enrollmentDate IS NULL")
    long countInactiveStudents(@Param("cutoffDate") LocalDate cutoffDate);
    
    // Find student by user ID
    Optional<Student> findByUserId(Long userId);
    
    // Find students by grade level list
    List<Student> findByGradeLevelIn(List<String> gradeLevels);
}
