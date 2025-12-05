package com.wsims.service;

import com.parami.wsims.entity.Attendance;
import com.parami.wsims.entity.User;
import com.parami.wsims.entity.Subject;
import com.parami.wsims.repository.AttendanceRepository;
import com.parami.wsims.repository.UserRepository;
import com.parami.wsims.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {
    
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    
    @Autowired
    public AttendanceServiceImpl(AttendanceRepository attendanceRepository,
                               UserRepository userRepository,
                               SubjectRepository subjectRepository) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
    }
    
    @Override
    public void markAttendance(List<Attendance> attendanceRecords) {
        // Validate and save all attendance records
        for (Attendance attendance : attendanceRecords) {
            // Check if attendance already exists
            if (attendanceExists(attendance.getStudent().getId(), 
                               attendance.getSubject().getId(), 
                               attendance.getAttendanceDate())) {
                throw new IllegalArgumentException("Attendance already exists for student " + 
                    attendance.getStudent().getId() + " in subject " + 
                    attendance.getSubject().getId() + " on " + attendance.getAttendanceDate());
            }
        }
        attendanceRepository.saveAll(attendanceRecords);
    }
    
    @Override
    public Attendance markSingleAttendance(Long studentId, Long subjectId, LocalDate date, 
                                         Attendance.AttendanceStatus status, String remarks, Long teacherId) {
        // Check if attendance already exists
        if (attendanceExists(studentId, subjectId, date)) {
            throw new IllegalArgumentException("Attendance already exists for this student, subject, and date");
        }
        
        // Get student, subject, and teacher
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found with id: " + subjectId));
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + teacherId));
        
        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setSubject(subject);
        attendance.setAttendanceDate(date);
        attendance.setStatus(status);
        attendance.setRemarks(remarks);
        attendance.setMarkedBy(teacher);
        
        return attendanceRepository.save(attendance);
    }
    
    @Override
    public Page<Attendance> getAttendanceByStudentId(Long studentId, int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return attendanceRepository.findByStudentId(studentId, pageable);
    }
    
    @Override
    public Page<Attendance> getAttendanceByStudentIdAndDateRange(Long studentId, LocalDate startDate, 
                                                               LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return attendanceRepository.findByStudentIdAndDateRange(studentId, startDate, endDate, pageable);
    }
    
    @Override
    public List<Attendance> getAttendanceBySubjectAndDate(Long subjectId, LocalDate date) {
        return attendanceRepository.findBySubjectIdAndDate(subjectId, date);
    }
    
    @Override
    public Page<Attendance> getAttendanceByTeacher(Long teacherId, int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return attendanceRepository.findByMarkedBy(teacherId, pageable);
    }
    
    @Override
    public Map<String, Object> getAttendanceStatistics(Long studentId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // Get total attendance days
        Long totalDays = attendanceRepository.countTotalAttendanceDaysByStudentId(studentId);
        
        // Get count by status
        Long presentCount = attendanceRepository.countByStudentIdAndStatus(studentId, Attendance.AttendanceStatus.PRESENT);
        Long absentCount = attendanceRepository.countByStudentIdAndStatus(studentId, Attendance.AttendanceStatus.ABSENT);
        Long lateCount = attendanceRepository.countByStudentIdAndStatus(studentId, Attendance.AttendanceStatus.LATE);
        Long excusedCount = attendanceRepository.countByStudentIdAndStatus(studentId, Attendance.AttendanceStatus.EXCUSED);
        
        // Calculate attendance percentage
        double attendancePercentage = 0.0;
        if (totalDays > 0) {
            attendancePercentage = (presentCount.doubleValue() / totalDays.doubleValue()) * 100;
        }
        
        statistics.put("totalDays", totalDays);
        statistics.put("presentCount", presentCount);
        statistics.put("absentCount", absentCount);
        statistics.put("lateCount", lateCount);
        statistics.put("excusedCount", excusedCount);
        statistics.put("attendancePercentage", Math.round(attendancePercentage * 100.0) / 100.0);
        
        return statistics;
    }
    
    @Override
    public Map<String, Object> getSubjectAttendanceStatistics(Long subjectId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // Get count by status for the subject
        Long presentCount = attendanceRepository.countBySubjectIdAndStatus(subjectId, Attendance.AttendanceStatus.PRESENT);
        Long absentCount = attendanceRepository.countBySubjectIdAndStatus(subjectId, Attendance.AttendanceStatus.ABSENT);
        Long lateCount = attendanceRepository.countBySubjectIdAndStatus(subjectId, Attendance.AttendanceStatus.LATE);
        Long excusedCount = attendanceRepository.countBySubjectIdAndStatus(subjectId, Attendance.AttendanceStatus.EXCUSED);
        
        Long totalRecords = presentCount + absentCount + lateCount + excusedCount;
        
        // Calculate percentages
        double presentPercentage = totalRecords > 0 ? (presentCount.doubleValue() / totalRecords.doubleValue()) * 100 : 0.0;
        double absentPercentage = totalRecords > 0 ? (absentCount.doubleValue() / totalRecords.doubleValue()) * 100 : 0.0;
        
        statistics.put("totalRecords", totalRecords);
        statistics.put("presentCount", presentCount);
        statistics.put("absentCount", absentCount);
        statistics.put("lateCount", lateCount);
        statistics.put("excusedCount", excusedCount);
        statistics.put("presentPercentage", Math.round(presentPercentage * 100.0) / 100.0);
        statistics.put("absentPercentage", Math.round(absentPercentage * 100.0) / 100.0);
        
        return statistics;
    }
    
    @Override
    public List<User> getStudentsEnrolledInSubject(Long subjectId) {
        return attendanceRepository.findStudentsEnrolledInSubject(subjectId);
    }
    
    @Override
    public List<Subject> getSubjectsForStudent(Long studentId) {
        return attendanceRepository.findDistinctSubjectsByStudentId(studentId);
    }

    @Override
    public List<Subject> getSubjectsForTeacher(Long teacherId) {
        return attendanceRepository.findSubjectsByTeacherId(teacherId);
    }
    
    @Override
    public Attendance updateAttendance(Long attendanceId, Attendance.AttendanceStatus status, String remarks) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
            .orElseThrow(() -> new IllegalArgumentException("Attendance not found with id: " + attendanceId));
        
        attendance.setStatus(status);
        attendance.setRemarks(remarks);
        
        return attendanceRepository.save(attendance);
    }
    
    @Override
    public void deleteAttendance(Long attendanceId) {
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new IllegalArgumentException("Attendance not found with id: " + attendanceId);
        }
        attendanceRepository.deleteById(attendanceId);
    }
    
    @Override
    public boolean attendanceExists(Long studentId, Long subjectId, LocalDate date) {
        return attendanceRepository.findByStudentIdAndSubjectIdAndDate(studentId, subjectId, date).isPresent();
    }
    
    @Override
    public Attendance getAttendanceById(Long id) {
        return attendanceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Attendance not found with id: " + id));
    }
    
    @Override
    public Page<Attendance> getAttendanceByDateRangeAndStatus(LocalDate startDate, LocalDate endDate, 
                                                            Attendance.AttendanceStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return attendanceRepository.findByDateRangeAndStatus(startDate, endDate, status, pageable);
    }
    
    @Override
    public Map<Attendance.AttendanceStatus, Long> getAttendanceSummary(Long studentId) {
        List<Object[]> summaryData = attendanceRepository.getAttendanceSummaryByStudentId(studentId);
        
        Map<Attendance.AttendanceStatus, Long> summary = new HashMap<>();
        for (Object[] row : summaryData) {
            Attendance.AttendanceStatus status = (Attendance.AttendanceStatus) row[0];
            Long count = (Long) row[1];
            summary.put(status, count);
        }
        
        return summary;
    }
    
    @Override
    public double getAttendancePercentage(Long studentId) {
        Map<String, Object> stats = getAttendanceStatistics(studentId);
        return (Double) stats.get("attendancePercentage");
    }
    
    @Override
    public double getAttendancePercentageBySubject(Long studentId, Long subjectId) {
        List<Attendance> attendanceList = attendanceRepository.findByStudentIdAndSubjectId(studentId, subjectId);
        
        if (attendanceList.isEmpty()) {
            return 0.0;
        }
        
        long presentCount = attendanceList.stream()
            .mapToLong(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT ? 1 : 0)
            .sum();
        
        return (presentCount * 100.0) / attendanceList.size();
    }
    
    @Override
    public Page<Attendance> getStudentAttendanceHistory(Long studentId, LocalDate startDate, LocalDate endDate, String status, Pageable pageable) {
        if (startDate != null && endDate != null && status != null && !status.isEmpty()) {
            return attendanceRepository.findByStudentIdAndStatusAndAttendanceDateBetween(studentId, status, startDate, endDate, pageable);
        } else if (startDate != null && endDate != null) {
            return attendanceRepository.findByStudentIdAndDateRange(studentId, startDate, endDate, pageable);
        } else if (status != null && !status.isEmpty()) {
            return attendanceRepository.findByStudentIdAndStatus(studentId, status, pageable);
        } else {
            return attendanceRepository.findByStudentId(studentId, pageable);
        }
    }
    
    @Override
    public Map<String, Long> getStudentAttendanceStatistics(Long studentId) {
        long total = attendanceRepository.countByStudentId(studentId);
        long present = attendanceRepository.countPresentByStudentId(studentId);
        long absent = attendanceRepository.countAbsentByStudentId(studentId);
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("present", present);
        stats.put("absent", absent);
        // Calculate percentage
        if (total > 0) {
            stats.put("presentPercentage", (present * 100) / total);
        } else {
            stats.put("presentPercentage", 0L);
        }
        return stats;
    }
}
