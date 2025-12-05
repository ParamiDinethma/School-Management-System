package com.wsims.repository;

import com.parami.wsims.entity.Grade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    /**
     * Find grades by student ID
     */
    List<Grade> findByStudentId(Long studentId);
    
    /**
     * Find grades by student ID ordered by exam schedule start date (newest first)
     */
    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.student s " +
           "JOIN FETCH g.course c " +
           "JOIN FETCH g.subject sub " +
           "JOIN FETCH g.examSchedule e " +
           "WHERE g.student.id = :studentId " +
           "ORDER BY e.startDate DESC")
    List<Grade> findByStudentIdOrderByExamScheduleStartDateDesc(@Param("studentId") Long studentId);
    
    /**
     * Find grades by student ID with pagination
     */
    Page<Grade> findByStudentId(Long studentId, Pageable pageable);
    
    /**
     * Find grades by course ID
     */
    List<Grade> findByCourseId(Long courseId);
    
    /**
     * Find grades by course ID with pagination
     */
    Page<Grade> findByCourseId(Long courseId, Pageable pageable);
    
    /**
     * Find grades by course ID and exam schedule ID
     */
    List<Grade> findByCourseIdAndExamScheduleId(Long courseId, Long examScheduleId);
    
    /**
     * Find grades by student ID and course ID
     */
    List<Grade> findByStudentIdAndCourseId(Long studentId, Long courseId);
    
    /**
     * Find grades by student ID, course ID, and exam schedule ID
     */
    List<Grade> findByStudentIdAndCourseIdAndExamScheduleId(Long studentId, Long courseId, Long examScheduleId);
    
    /**
     * Find specific grade by student, course, subject, and exam schedule
     */
    Optional<Grade> findByStudentIdAndCourseIdAndSubjectIdAndExamScheduleId(
            Long studentId, Long courseId, Long subjectId, Long examScheduleId);
    
    /**
     * Check if grade exists for student, course, subject, and exam schedule
     */
    boolean existsByStudentIdAndCourseIdAndSubjectIdAndExamScheduleId(
            Long studentId, Long courseId, Long subjectId, Long examScheduleId);
    
    /**
     * Find grades by subject ID
     */
    List<Grade> findBySubjectId(Long subjectId);
    
    /**
     * Find grades by exam schedule ID
     */
    List<Grade> findByExamScheduleId(Long examScheduleId);
    
    /**
     * Find grades by student ID and subject ID
     */
    List<Grade> findByStudentIdAndSubjectId(Long studentId, Long subjectId);
    
    /**
     * Find grades by student ID and exam schedule ID
     */
    List<Grade> findByStudentIdAndExamScheduleId(Long studentId, Long examScheduleId);
    
    /**
     * Find grades by course ID and subject ID
     */
    List<Grade> findByCourseIdAndSubjectId(Long courseId, Long subjectId);
    
    /**
     * Find grades by created by user ID
     */
    List<Grade> findByCreatedById(Long createdById);
    
    /**
     * Find grades by grade status
     */
    List<Grade> findByStatus(Grade.GradeStatus status);
    
    /**
     * Find grades by letter grade
     */
    List<Grade> findByLetterGrade(Grade.LetterGrade letterGrade);
    
    /**
     * Find grades by student ID and grade status
     */
    List<Grade> findByStudentIdAndStatus(Long studentId, Grade.GradeStatus status);
    
    /**
     * Find grades by course ID and grade status
     */
    List<Grade> findByCourseIdAndStatus(Long courseId, Grade.GradeStatus status);
    
    /**
     * Custom query to find grades with student, course, subject, and exam schedule details
     */
    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.student s " +
           "JOIN FETCH g.course c " +
           "JOIN FETCH g.subject sub " +
           "JOIN FETCH g.examSchedule e " +
           "WHERE g.student.id = :studentId")
    List<Grade> findGradesWithDetailsByStudentId(@Param("studentId") Long studentId);
    
    /**
     * Custom query to find grades with details by course ID
     */
    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.student s " +
           "JOIN FETCH g.course c " +
           "JOIN FETCH g.subject sub " +
           "JOIN FETCH g.examSchedule e " +
           "WHERE g.course.id = :courseId")
    List<Grade> findGradesWithDetailsByCourseId(@Param("courseId") Long courseId);
    
    /**
     * Custom query to find grades with details by course and exam schedule
     */
    @Query("SELECT g FROM Grade g " +
           "JOIN FETCH g.student s " +
           "JOIN FETCH g.course c " +
           "JOIN FETCH g.subject sub " +
           "JOIN FETCH g.examSchedule e " +
           "WHERE g.course.id = :courseId AND g.examSchedule.id = :examScheduleId")
    List<Grade> findGradesWithDetailsByCourseAndExam(@Param("courseId") Long courseId, @Param("examScheduleId") Long examScheduleId);
    
    /**
     * Count grades by student ID
     */
    long countByStudentId(Long studentId);
    
    /**
     * Count grades by course ID
     */
    long countByCourseId(Long courseId);
    
    /**
     * Count grades by subject ID
     */
    long countBySubjectId(Long subjectId);
    
    /**
     * Count grades by exam schedule ID
     */
    long countByExamScheduleId(Long examScheduleId);
}