package com.wsims.service;

import com.parami.wsims.entity.ExamSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ExamScheduleService {

    // Basic CRUD operations
    ExamSchedule createExamSchedule(ExamSchedule examSchedule);
    ExamSchedule updateExamSchedule(Long id, ExamSchedule examSchedule);
    void deleteExamSchedule(Long id);
    ExamSchedule getExamScheduleById(Long id);
    
    // Find operations
    Page<ExamSchedule> getAllExamSchedules(Pageable pageable);
    Page<ExamSchedule> getAllExamSchedules(String searchTerm, Pageable pageable);
    List<ExamSchedule> getAllExamSchedules();
    
    // Find by specific criteria
    List<ExamSchedule> findByStatus(ExamSchedule.ExamStatus status);
    List<ExamSchedule> findByAcademicYear(String academicYear);
    List<ExamSchedule> findBySemester(String semester);
    List<ExamSchedule> findByAcademicYearAndSemester(String academicYear, String semester);
    List<ExamSchedule> getActiveExamSchedules();
    
    // Find by date ranges
    List<ExamSchedule> findByDateRange(LocalDate startDate, LocalDate endDate);
    List<ExamSchedule> findUpcomingExams();
    List<ExamSchedule> findOngoingExams();
    List<ExamSchedule> findPastExams();
    
    // Search functionality
    Page<ExamSchedule> searchExamSchedules(String searchTerm, Pageable pageable);
    
    // Validation and business logic
    boolean isExamScheduleValid(ExamSchedule examSchedule);
    boolean hasDateConflicts(ExamSchedule examSchedule);
    List<ExamSchedule> findOverlappingExamSchedules(ExamSchedule examSchedule);
    
    // Statistics and analytics
    Map<String, Long> getExamScheduleStatistics();
    long countByStatus(ExamSchedule.ExamStatus status);
    long countByAcademicYear(String academicYear);
    
    // Status management
    ExamSchedule activateExamSchedule(Long id);
    ExamSchedule deactivateExamSchedule(Long id);
    ExamSchedule completeExamSchedule(Long id);
    
    // Bulk operations
    void bulkUpdateStatus(List<Long> ids, ExamSchedule.ExamStatus status);
    void bulkDeleteExamSchedules(List<Long> ids);
}

