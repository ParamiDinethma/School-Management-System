package com.wsims.service;

import com.parami.wsims.entity.Attendance;
import com.parami.wsims.entity.User;
import com.parami.wsims.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {
    
    // Mark attendance for multiple students
    void markAttendance(List<Attendance> attendanceRecords);
    
    // Mark attendance for a single student
    Attendance markSingleAttendance(Long studentId, Long subjectId, LocalDate date, 
                                  Attendance.AttendanceStatus status, String remarks, Long teacherId);
    
    // Get attendance records for a student with pagination
    Page<Attendance> getAttendanceByStudentId(Long studentId, int page, int size, String sortBy, String sortDir);
    
    // Get attendance records for a student within date range
    Page<Attendance> getAttendanceByStudentIdAndDateRange(Long studentId, LocalDate startDate, 
                                                         LocalDate endDate, int page, int size);
    
    // Get attendance records for a subject and date
    List<Attendance> getAttendanceBySubjectAndDate(Long subjectId, LocalDate date);
    
    // Get attendance records for a teacher (marked by them)
    Page<Attendance> getAttendanceByTeacher(Long teacherId, int page, int size, String sortBy, String sortDir);
    
    // Get attendance statistics for a student
    Map<String, Object> getAttendanceStatistics(Long studentId);
    
    // Get student attendance history with filtering
    Page<Attendance> getStudentAttendanceHistory(Long studentId, LocalDate startDate, LocalDate endDate, String status, Pageable pageable);
    
    // Get student attendance statistics
    Map<String, Long> getStudentAttendanceStatistics(Long studentId);
    
    // Get attendance statistics for a subject
    Map<String, Object> getSubjectAttendanceStatistics(Long subjectId);
    
    // Get students enrolled in a subject
    List<User> getStudentsEnrolledInSubject(Long subjectId);
    
    // Get subjects that a student has attendance records for
    List<Subject> getSubjectsForStudent(Long studentId);
    
    // Get subjects that a teacher is assigned to teach
    List<Subject> getSubjectsForTeacher(Long teacherId);
    
    // Update attendance record
    Attendance updateAttendance(Long attendanceId, Attendance.AttendanceStatus status, String remarks);
    
    // Delete attendance record
    void deleteAttendance(Long attendanceId);
    
    // Check if attendance already exists
    boolean attendanceExists(Long studentId, Long subjectId, LocalDate date);
    
    // Get attendance record by ID
    Attendance getAttendanceById(Long id);
    
    // Get attendance records by date range and status
    Page<Attendance> getAttendanceByDateRangeAndStatus(LocalDate startDate, LocalDate endDate, 
                                                      Attendance.AttendanceStatus status, int page, int size);
    
    // Get attendance summary for a student (count by status)
    Map<Attendance.AttendanceStatus, Long> getAttendanceSummary(Long studentId);
    
    // Get attendance percentage for a student
    double getAttendancePercentage(Long studentId);
    
    // Get attendance percentage for a student in a specific subject
    double getAttendancePercentageBySubject(Long studentId, Long subjectId);
}
