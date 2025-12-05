package com.wsims.repository;

import com.parami.wsims.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    Optional<Course> findByCourseCode(String courseCode);
    
    List<Course> findByIsActiveTrue();
    
    Page<Course> findByIsActiveTrue(Pageable pageable);
    
    @Query("SELECT c FROM Course c WHERE " +
           "LOWER(c.courseName) LIKE LOWER(:searchTerm) OR " +
           "LOWER(c.courseCode) LIKE LOWER(:searchTerm)")
    Page<Course> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT c FROM Course c WHERE c.isActive = true AND " +
           "(LOWER(c.courseName) LIKE LOWER(:searchTerm) OR " +
           "LOWER(c.courseCode) LIKE LOWER(:searchTerm))")
    Page<Course> findActiveBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    boolean existsByCourseCode(String courseCode);
    boolean existsByCourseCodeAndIdNot(String courseCode, Long id);
}
