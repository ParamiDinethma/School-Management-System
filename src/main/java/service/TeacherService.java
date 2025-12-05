package com.wsims.service;

import com.parami.wsims.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeacherService {
    
    // Basic CRUD operations
    Optional<Teacher> findById(Long id);
    List<Teacher> findAllTeachers();
    Teacher saveTeacher(Teacher teacher);
    void deleteTeacher(Long id);

    // Pagination and search
    Page<Teacher> findTeachersWithPagination(Pageable pageable);
    Page<Teacher> searchTeachers(String searchTerm, Pageable pageable);
    Page<Teacher> findTeachersByDepartment(String department, Pageable pageable);
    Page<Teacher> searchTeachersByDepartment(String department, String searchTerm, Pageable pageable);

    // Statistics
    long getTotalTeacherCount();
    long getActiveTeacherCount(LocalDate cutoffDate);
    long getInactiveTeacherCount(LocalDate cutoffDate);

    // Utility methods
    List<String> getDistinctDepartments();
    Optional<Teacher> findByUserId(Long userId);
}

