package com.wsims.repository;

import com.parami.wsims.entity.ExamSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {

    // Find exam schedules by status
    List<ExamSchedule> findByStatus(ExamSchedule.ExamStatus status);

    // Find exam schedules by academic year
    List<ExamSchedule> findByAcademicYear(String academicYear);

    // Find exam schedules by semester
    List<ExamSchedule> findBySemester(String semester);

    // Find exam schedules by academic year and semester
    List<ExamSchedule> findByAcademicYearAndSemester(String academicYear, String semester);

    // Find exam schedules by date range
    @Query("SELECT e FROM ExamSchedule e WHERE e.startDate >= :startDate AND e.endDate <= :endDate")
    List<ExamSchedule> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Find upcoming exams
    @Query("SELECT e FROM ExamSchedule e WHERE e.startDate > :currentDate AND e.status = 'ACTIVE' ORDER BY e.startDate ASC")
    List<ExamSchedule> findUpcomingExams(@Param("currentDate") LocalDate currentDate);

    // Find ongoing exams
    @Query("SELECT e FROM ExamSchedule e WHERE e.startDate <= :currentDate AND e.endDate >= :currentDate AND e.status = 'ACTIVE'")
    List<ExamSchedule> findOngoingExams(@Param("currentDate") LocalDate currentDate);

    // Find past exams
    @Query("SELECT e FROM ExamSchedule e WHERE e.endDate < :currentDate ORDER BY e.endDate DESC")
    List<ExamSchedule> findPastExams(@Param("currentDate") LocalDate currentDate);

    // Find exam schedule by name (case insensitive)
    @Query("SELECT e FROM ExamSchedule e WHERE LOWER(e.examName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<ExamSchedule> findByExamNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    // Search exam schedules with pagination
    @Query("SELECT e FROM ExamSchedule e WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(e.examName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.academicYear) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.semester) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<ExamSchedule> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Find exam schedules by created by user
    List<ExamSchedule> findByCreatedBy_Id(Long createdById);

    // Count exams by status
    long countByStatus(ExamSchedule.ExamStatus status);

    // Count exams by academic year
    long countByAcademicYear(String academicYear);

    // Get exam schedule statistics
    @Query("SELECT e.status, COUNT(e) FROM ExamSchedule e GROUP BY e.status")
    List<Object[]> getExamScheduleStatistics();

    // Find exam schedules with pagination and sorting
    Page<ExamSchedule> findAll(Pageable pageable);

    // Find active exam schedules with pagination
    Page<ExamSchedule> findByStatus(ExamSchedule.ExamStatus status, Pageable pageable);

    // Find exam schedules by academic year with pagination
    Page<ExamSchedule> findByAcademicYear(String academicYear, Pageable pageable);

    // Find exam schedules by semester with pagination
    Page<ExamSchedule> findBySemester(String semester, Pageable pageable);

    // Find exam schedules by academic year and semester with pagination
    Page<ExamSchedule> findByAcademicYearAndSemester(String academicYear, String semester, Pageable pageable);

    // Check if exam schedule exists by name and date range
    @Query("SELECT COUNT(e) > 0 FROM ExamSchedule e WHERE " +
           "LOWER(e.examName) = LOWER(:examName) AND " +
           "e.startDate = :startDate AND e.endDate = :endDate")
    boolean existsByExamNameAndDateRange(@Param("examName") String examName, 
                                       @Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);

    // Find exam schedules overlapping with given date range
    @Query("SELECT e FROM ExamSchedule e WHERE " +
           "e.status = 'ACTIVE' AND " +
           "((e.startDate <= :startDate AND e.endDate >= :startDate) OR " +
           "(e.startDate <= :endDate AND e.endDate >= :endDate) OR " +
           "(e.startDate >= :startDate AND e.endDate <= :endDate))")
    List<ExamSchedule> findOverlappingExamSchedules(@Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);
}
