package com.wsims.service;

import com.parami.wsims.entity.Enrollment;
import com.parami.wsims.entity.User;
import com.parami.wsims.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EnrollmentService {
    
    // CRUD Operations
    Enrollment saveEnrollment(Enrollment enrollment);
    Enrollment updateEnrollment(Enrollment enrollment);
    void deleteEnrollment(Long id);
    Optional<Enrollment> findById(Long id);
    List<Enrollment> findAllEnrollments();
    
    // Student-specific operations
    List<Enrollment> findEnrollmentsByStudent(User student);
    Page<Enrollment> findEnrollmentsByStudent(User student, Pageable pageable);
    List<Course> getAvailableCoursesForStudent(Long studentId);
    Enrollment enrollStudentInCourse(Long studentId, Long courseId, String remarks);
    void unenrollStudentFromCourse(Long studentId, Long courseId);
    boolean isStudentEnrolledInCourse(Long studentId, Long courseId);
    
    // Course-specific operations
    List<Enrollment> findEnrollmentsByCourse(Course course);
    Page<Enrollment> findEnrollmentsByCourse(Course course, Pageable pageable);
    List<Enrollment> findActiveEnrollmentsByCourse(Long courseId);
    List<User> getStudentsByCourse(Long courseId);
    
    // Status-based operations
    List<Enrollment> findEnrollmentsByStatus(String status);
    Page<Enrollment> findEnrollmentsByStatus(String status, Pageable pageable);
    Enrollment updateEnrollmentStatus(Long enrollmentId, String status, String remarks);
    Enrollment updateEnrollmentGrade(Long enrollmentId, String grade);
    
    // Search and Pagination
    Page<Enrollment> findEnrollmentsWithPagination(Pageable pageable);
    Page<Enrollment> searchEnrollments(String searchTerm, Pageable pageable);
    
    // Date-based operations
    List<Enrollment> findEnrollmentsByDateRange(LocalDate startDate, LocalDate endDate);
    
    // Statistics
    long getTotalEnrollmentCount();
    long getActiveEnrollmentCount();
    long getEnrollmentCountByStatus(String status);
    long getEnrollmentCountByCourse(Long courseId);
    long getEnrollmentCountByCourseAndStatus(Long courseId, String status);
    
    // Validation
    boolean canStudentEnrollInCourse(Long studentId, Long courseId);
    boolean isValidEnrollmentStatus(String status);
    boolean isValidGrade(String grade);
}
