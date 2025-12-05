package com.wsims.service;

import com.parami.wsims.entity.Grade;
import com.parami.wsims.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface GradeService {
    
    /**
     * Save a single grade
     */
    Grade saveGrade(Grade grade);
    
    /**
     * Save multiple grades in bulk
     */
    List<Grade> saveBulkGrades(List<Map<String, Object>> gradeEntries, Long courseId, Long examScheduleId);
    
    /**
     * Get grades by student ID
     */
    List<Grade> getGradesByStudent(Long studentId);
    
    /**
     * Get grades by course ID
     */
    List<Grade> getGradesByCourse(Long courseId);
    
    /**
     * Get grades by course and exam schedule
     */
    List<Grade> getGradesByCourseAndExam(Long courseId, Long examScheduleId);
    
    /**
     * Get grades by student and course
     */
    List<Grade> getGradesByStudentAndCourse(Long studentId, Long courseId);
    
    /**
     * Get grades by student, course, and exam schedule
     */
    List<Grade> getGradesByStudentCourseAndExam(Long studentId, Long courseId, Long examScheduleId);
    
    /**
     * Get grades with pagination
     */
    Page<Grade> getGrades(Pageable pageable);
    
    /**
     * Get grades by student with pagination
     */
    Page<Grade> getGradesByStudent(Long studentId, Pageable pageable);
    
    /**
     * Get grades by course with pagination
     */
    Page<Grade> getGradesByCourse(Long courseId, Pageable pageable);
    
    /**
     * Delete a grade by ID
     */
    void deleteGrade(Long gradeId);
    
    /**
     * Get grade by ID
     */
    Grade getGradeById(Long gradeId);
    
    /**
     * Check if grade exists for student, course, subject, and exam schedule
     */
    boolean gradeExists(Long studentId, Long courseId, Long subjectId, Long examScheduleId);
    
    /**
     * Get or create grade for student, course, subject, and exam schedule
     */
    Grade getOrCreateGrade(Long studentId, Long courseId, Long subjectId, Long examScheduleId);
}

