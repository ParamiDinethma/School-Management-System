package com.wsims.service;

import com.parami.wsims.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SubjectService {
    
    // CRUD Operations
    Subject saveSubject(Subject subject);
    Subject updateSubject(Subject subject);
    void deleteSubject(Long id);
    Optional<Subject> findById(Long id);
    Subject getSubjectById(Long id);
    List<Subject> findAllSubjects();
    
    // Search and Pagination
    Page<Subject> findSubjectsWithPagination(Pageable pageable);
    Page<Subject> searchSubjects(String searchTerm, Pageable pageable);
    Page<Subject> findActiveSubjects(Pageable pageable);
    Page<Subject> searchActiveSubjects(String searchTerm, Pageable pageable);
    
    // Statistics
    long getTotalSubjectCount();
    long getActiveSubjectCount();
    long getInactiveSubjectCount();
    
    // Validation
    boolean isSubjectCodeExists(String subjectCode);
    boolean isSubjectCodeExistsForOtherSubject(String subjectCode, Long subjectId);
    
    // Course Management
    List<Subject> getAvailableSubjectsForCourse(Long courseId);
}
