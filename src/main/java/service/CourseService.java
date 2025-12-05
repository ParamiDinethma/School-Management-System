package com.wsims.service;

import com.parami.wsims.entity.Course;
import com.parami.wsims.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CourseService {
    
    // CRUD Operations
    Course saveCourse(Course course);
    Course updateCourse(Course course);
    void deleteCourse(Long id);
    Optional<Course> findById(Long id);
    List<Course> findAllCourses();
    
    // Search and Pagination
    Page<Course> findCoursesWithPagination(Pageable pageable);
    Page<Course> searchCourses(String searchTerm, Pageable pageable);
    Page<Course> findActiveCourses(Pageable pageable);
    Page<Course> searchActiveCourses(String searchTerm, Pageable pageable);
    
    // Subject Management
    void assignSubjectToCourse(Long courseId, Long subjectId, Boolean isMandatory, String gradeLevel);
    void removeSubjectFromCourse(Long courseId, Long subjectId);
    List<Subject> getSubjectsByCourseId(Long courseId);
    List<Subject> getSubjectsByCourse(Long courseId);
    List<Subject> getMandatorySubjectsByCourseId(Long courseId);
    List<Subject> getOptionalSubjectsByCourseId(Long courseId);
    
    // Teacher Management
    List<Course> getCoursesByTeacher(Long teacherId);
    Course getCourseById(Long courseId);
    
    // Statistics
    long getTotalCourseCount();
    long getActiveCourseCount();
    long getInactiveCourseCount();
    
    // Validation
    boolean isCourseCodeExists(String courseCode);
    boolean isCourseCodeExistsForOtherCourse(String courseCode, Long courseId);
    
    // Strategy Pattern Validation
    Map<String, Object> validateCourse(Course course);
}
