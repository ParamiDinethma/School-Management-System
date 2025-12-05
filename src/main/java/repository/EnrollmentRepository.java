package com.wsims.repository;

import com.parami.wsims.entity.Enrollment;
import com.parami.wsims.entity.User;
import com.parami.wsims.entity.Course;
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
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    
    // Find enrollments by student
    List<Enrollment> findByStudent(User student);
    Page<Enrollment> findByStudent(User student, Pageable pageable);
    
    // Find enrollments by course
    List<Enrollment> findByCourse(Course course);
    Page<Enrollment> findByCourse(Course course, Pageable pageable);
    
    // Find enrollment by student and course
    Optional<Enrollment> findByStudentAndCourse(User student, Course course);
    boolean existsByStudentAndCourse(User student, Course course);
    
    // Find enrollments by status
    List<Enrollment> findByStatus(String status);
    Page<Enrollment> findByStatus(String status, Pageable pageable);
    
    // Find active enrollments
    List<Enrollment> findByStatusAndStudent(String status, User student);
    List<Enrollment> findByStatusAndCourse(String status, Course course);
    
    // Search enrollments
    @Query("SELECT e FROM Enrollment e WHERE " +
           "(LOWER(e.student.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.student.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.student.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.course.courseName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.course.courseCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Enrollment> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Find enrollments by date range
    List<Enrollment> findByEnrollmentDateBetween(LocalDate startDate, LocalDate endDate);
    
    // Find enrollments by student ID and course ID
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId")
    Optional<Enrollment> findByStudentIdAndCourseId(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    
    // Check if student is enrolled in course
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId AND e.status = 'ACTIVE'")
    boolean isStudentEnrolledInCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    
    // Get enrollment statistics
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.status = :status")
    long countByStatus(@Param("status") String status);
    
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.status = :status")
    long countByCourseIdAndStatus(@Param("courseId") Long courseId, @Param("status") String status);
    
    // Get available courses for student (not enrolled)
    @Query("SELECT c FROM Course c WHERE c.isActive = true AND c.id NOT IN " +
           "(SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE')")
    List<Course> findAvailableCoursesForStudent(@Param("studentId") Long studentId);
}