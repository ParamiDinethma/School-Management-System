package com.wsims.repository;

import com.parami.wsims.entity.Attendance;
import com.parami.wsims.entity.User;
import com.parami.wsims.entity.Subject;
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
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    // Find attendance records for a specific student
    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId")
    Page<Attendance> findByStudentId(@Param("studentId") Long studentId, Pageable pageable);
    
    // Find attendance records for a specific subject and date
    @Query("SELECT a FROM Attendance a WHERE a.subject.id = :subjectId AND a.attendanceDate = :date")
    List<Attendance> findBySubjectIdAndDate(@Param("subjectId") Long subjectId, @Param("date") LocalDate date);
    
    // Find attendance records for a specific student and subject
    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND a.subject.id = :subjectId ORDER BY a.attendanceDate DESC")
    List<Attendance> findByStudentIdAndSubjectId(@Param("studentId") Long studentId, @Param("subjectId") Long subjectId);
    
    // Find attendance records for a specific student within date range
    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND a.attendanceDate BETWEEN :startDate AND :endDate")
    Page<Attendance> findByStudentIdAndDateRange(@Param("studentId") Long studentId, 
                                                @Param("startDate") LocalDate startDate, 
                                                @Param("endDate") LocalDate endDate, 
                                                Pageable pageable);
    
    // Find attendance records for a specific student and status
    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND a.status = :status")
    Page<Attendance> findByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") String status, Pageable pageable);
    
    // Find attendance records for a specific student, status, and date range
    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND a.status = :status AND a.attendanceDate BETWEEN :startDate AND :endDate")
    Page<Attendance> findByStudentIdAndStatusAndAttendanceDateBetween(@Param("studentId") Long studentId, 
                                                                     @Param("status") String status,
                                                                     @Param("startDate") LocalDate startDate, 
                                                                     @Param("endDate") LocalDate endDate, 
                                                                     Pageable pageable);
    
    // Find attendance records for a specific teacher (marked by)
    @Query("SELECT a FROM Attendance a WHERE a.markedBy.id = :teacherId")
    Page<Attendance> findByMarkedBy(@Param("teacherId") Long teacherId, Pageable pageable);
    
    // Find attendance records for a specific subject
    @Query("SELECT a FROM Attendance a WHERE a.subject.id = :subjectId")
    Page<Attendance> findBySubjectId(@Param("subjectId") Long subjectId, Pageable pageable);
    
    // Check if attendance already exists for a student, subject, and date
    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND a.subject.id = :subjectId AND a.attendanceDate = :date")
    Optional<Attendance> findByStudentIdAndSubjectIdAndDate(@Param("studentId") Long studentId, 
                                                           @Param("subjectId") Long subjectId, 
                                                           @Param("date") LocalDate date);
    
    // Get attendance statistics for a student
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.status = :status")
    Long countByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") Attendance.AttendanceStatus status);
    
    // Get total attendance days for a student
    @Query("SELECT COUNT(DISTINCT a.attendanceDate) FROM Attendance a WHERE a.student.id = :studentId")
    Long countTotalAttendanceDaysByStudentId(@Param("studentId") Long studentId);
    
    // Get attendance statistics for a subject
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.subject.id = :subjectId AND a.status = :status")
    Long countBySubjectIdAndStatus(@Param("subjectId") Long subjectId, @Param("status") Attendance.AttendanceStatus status);
    
    // Find subjects that have attendance records for a specific student
    @Query("SELECT DISTINCT a.subject FROM Attendance a WHERE a.student.id = :studentId")
    List<Subject> findDistinctSubjectsByStudentId(@Param("studentId") Long studentId);
    
    // Find subjects taught by a specific teacher (through course-subject links)
    @Query("SELECT DISTINCT csl.subject FROM CourseSubjectLink csl " +
           "WHERE csl.course.id IN (" +
           "    SELECT DISTINCT c.id FROM Course c " +
           "    WHERE c.id IN (" +
           "        SELECT DISTINCT e.course.id FROM Enrollment e " +
           "        WHERE e.student.id IN (" +
           "            SELECT DISTINCT a.student.id FROM Attendance a " +
           "            WHERE a.markedBy.id = :teacherId" +
           "        )" +
           "    )" +
           ")")
    List<Subject> findSubjectsByTeacherId(@Param("teacherId") Long teacherId);
    
    // Find students enrolled in a subject (through enrollments and course-subject links)
    @Query("SELECT DISTINCT e.student FROM Enrollment e " +
           "JOIN e.course c " +
           "JOIN c.courseSubjects csl " +
           "WHERE csl.subject.id = :subjectId AND e.status = 'ACTIVE' AND e.student.role.name = 'STUDENT'")
    List<User> findStudentsEnrolledInSubject(@Param("subjectId") Long subjectId);
    
    // Fallback query - get all students for debugging
    @Query("SELECT u FROM User u WHERE u.role.name = 'STUDENT'")
    List<User> findAllStudents();
    
    // Get attendance summary for a student (count by status)
    @Query("SELECT a.status, COUNT(a) FROM Attendance a WHERE a.student.id = :studentId GROUP BY a.status")
    List<Object[]> getAttendanceSummaryByStudentId(@Param("studentId") Long studentId);
    
    // Find attendance records by date range and status
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate BETWEEN :startDate AND :endDate AND a.status = :status")
    Page<Attendance> findByDateRangeAndStatus(@Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate, 
                                            @Param("status") Attendance.AttendanceStatus status, 
                                            Pageable pageable);
    
    // Count total attendance records for a student
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);
    
    // Count present attendance records for a student
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.status = 'PRESENT'")
    long countPresentByStudentId(@Param("studentId") Long studentId);
    
    // Count absent attendance records for a student
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.status = 'ABSENT'")
    long countAbsentByStudentId(@Param("studentId") Long studentId);
}