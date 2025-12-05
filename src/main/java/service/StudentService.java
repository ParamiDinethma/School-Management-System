package com.wsims.service;

import com.parami.wsims.entity.Student;
import com.parami.wsims.dto.UserCreationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StudentService {
    
    // CRUD Operations
    Student saveStudent(Student student);
    Student updateStudent(Student student);
    void deleteStudent(Long userId);
    Optional<Student> findById(Long userId);
    Optional<Student> findByUserId(Long userId);
    List<Student> findAllStudents();
    
    // Search and Pagination
    Page<Student> findStudentsWithPagination(Pageable pageable);
    Page<Student> searchStudents(String searchTerm, Pageable pageable);
    Page<Student> findStudentsByGradeLevel(String gradeLevel, Pageable pageable);
    Page<Student> searchStudentsByGradeLevel(String gradeLevel, String searchTerm, Pageable pageable);
    
    // Statistics
    Map<String, Long> getStudentStatistics();
    long getTotalStudentCount();
    long getActiveStudentCount();
    long getInactiveStudentCount();
    
    // Create student from DTO
    Student createStudentFromDTO(UserCreationDTO userData);
    
    // Grade level management
    List<String> getAllGradeLevels();
}
