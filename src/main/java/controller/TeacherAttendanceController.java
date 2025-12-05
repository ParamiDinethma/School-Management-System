package com.wsims.controller;

import com.parami.wsims.entity.Attendance;
import com.parami.wsims.entity.Subject;
import com.parami.wsims.entity.User;
import com.parami.wsims.service.AttendanceService;
import com.parami.wsims.service.SubjectService;
import com.parami.wsims.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/teacher/attendance")
public class TeacherAttendanceController {
    
    private final AttendanceService attendanceService;
    private final SubjectService subjectService;
    private final UserService userService;
    
    @Autowired
    public TeacherAttendanceController(AttendanceService attendanceService,
                                    SubjectService subjectService,
                                    UserService userService) {
        this.attendanceService = attendanceService;
        this.subjectService = subjectService;
        this.userService = userService;
    }
    
    // Display the attendance marking page
    @GetMapping
    @PreAuthorize("hasAnyAuthority('TEACHER', 'IT_ADMIN', 'PRINCIPAL')")
    public String attendancePage(Model model) {
        // Get the current logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get subjects that the teacher teaches (you'll need to implement this based on your teacher-subject relationship)
        // For now, we'll get all subjects - you may need to modify this based on your teacher-subject assignment logic
        List<Subject> subjects = subjectService.findAllSubjects();
        
        model.addAttribute("subjects", subjects);
        model.addAttribute("teacherId", currentUser.getId());
        
        return "teacher-attendance";
    }
    
    // Get subjects that a teacher teaches
    @GetMapping("/api/subjects")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'IT_ADMIN', 'PRINCIPAL')")
    @ResponseBody
    public ResponseEntity<List<Subject>> getTeacherSubjects() {
        try {
            // For now, return all subjects - you may need to modify this based on your teacher-subject assignment logic
            List<Subject> subjects = subjectService.findAllSubjects();
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Get students enrolled in a subject
    @GetMapping("/api/subjects/{subjectId}/students")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'IT_ADMIN', 'PRINCIPAL')")
    @ResponseBody
    public ResponseEntity<List<User>> getStudentsForSubject(@PathVariable Long subjectId) {
        try {
            System.out.println("Getting students for subject ID: " + subjectId);
            
            // First, try the complex query
            List<User> students = attendanceService.getStudentsEnrolledInSubject(subjectId);
            System.out.println("Found " + students.size() + " students for subject " + subjectId);
            
            // If no students found, try a simpler approach - get all students for debugging
            if (students.isEmpty()) {
                System.out.println("No students found with complex query, trying simpler approach...");
                // Get all students with STUDENT role for debugging
                List<User> allStudents = userService.findUsersByRole("STUDENT", Pageable.unpaged()).getContent();
                System.out.println("Total students in system: " + allStudents.size());
                
                // Return first 5 students for debugging (remove this later)
                if (allStudents.size() > 5) {
                    students = allStudents.subList(0, 5);
                } else {
                    students = allStudents;
                }
                System.out.println("Returning " + students.size() + " students for debugging");
            }
            
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            System.err.println("Error getting students for subject " + subjectId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Get existing attendance for a subject and date
    @GetMapping("/api/subjects/{subjectId}/date/{date}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'IT_ADMIN', 'PRINCIPAL')")
    @ResponseBody
    public ResponseEntity<List<Attendance>> getExistingAttendance(@PathVariable Long subjectId, @PathVariable String date) {
        try {
            LocalDate attendanceDate = LocalDate.parse(date);
            List<Attendance> attendance = attendanceService.getAttendanceBySubjectAndDate(subjectId, attendanceDate);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Mark attendance for multiple students
    @PostMapping("/api/mark")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'IT_ADMIN', 'PRINCIPAL')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAttendance(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Received attendance request: " + request);
            
            Object subjectIdObj = request.get("subjectId");
            Object dateObj = request.get("date");
            Object teacherIdObj = request.get("teacherId");

            if (subjectIdObj == null) {
                throw new IllegalArgumentException("Missing subjectId");
            }
            if (dateObj == null) {
                throw new IllegalArgumentException("Missing date");
            }

            Long subjectId = Long.valueOf(subjectIdObj.toString());
            String dateStr = dateObj.toString();

            // Derive teacherId from security context if not provided by client
            Long teacherId;
            if (teacherIdObj != null) {
                teacherId = Long.valueOf(teacherIdObj.toString());
            } else {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String username = auth.getName();
                User currentUser = userService.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                teacherId = currentUser.getId();
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attendanceData = (List<Map<String, Object>>) request.get("attendance");
            if (attendanceData == null || attendanceData.isEmpty()) {
                throw new IllegalArgumentException("No attendance records provided");
            }
            
            System.out.println("Subject ID: " + subjectId + ", Date: " + dateStr + ", Teacher ID: " + teacherId);
            System.out.println("Attendance data count: " + attendanceData.size());
            
            LocalDate date = LocalDate.parse(dateStr);
            
            // Process each attendance record
            for (Map<String, Object> record : attendanceData) {
                Long studentId = Long.valueOf(record.get("studentId").toString());
                String statusStr = record.get("status").toString();
                String remarks = record.get("remarks") != null ? record.get("remarks").toString() : "";
                
                System.out.println("Processing student " + studentId + " with status " + statusStr);
                
                Attendance.AttendanceStatus status = Attendance.AttendanceStatus.valueOf(statusStr);
                
                // Check if attendance already exists
                if (attendanceService.attendanceExists(studentId, subjectId, date)) {
                    System.out.println("Attendance already exists for student " + studentId + ", skipping");
                    continue;
                } else {
                    System.out.println("Creating new attendance record for student " + studentId);
                    // Create new attendance record
                    attendanceService.markSingleAttendance(studentId, subjectId, date, status, remarks, teacherId);
                    System.out.println("Successfully created attendance record for student " + studentId);
                }
            }
            
            System.out.println("Attendance marking completed successfully");
            response.put("success", true);
            response.put("message", "Attendance marked successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Error marking attendance: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error marking attendance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Get attendance records marked by a teacher
    @GetMapping("/api/history")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'IT_ADMIN', 'PRINCIPAL')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTeacherAttendanceHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "attendanceDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get the current logged-in user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Page<Attendance> attendancePage = attendanceService.getAttendanceByTeacher(
                currentUser.getId(), page, size, sortBy, sortDir);
            
            response.put("content", attendancePage.getContent());
            response.put("totalElements", attendancePage.getTotalElements());
            response.put("totalPages", attendancePage.getTotalPages());
            response.put("currentPage", attendancePage.getNumber());
            response.put("size", attendancePage.getSize());
            response.put("first", attendancePage.isFirst());
            response.put("last", attendancePage.isLast());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "Error retrieving attendance history: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Update attendance record
    @PutMapping("/api/{attendanceId}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'IT_ADMIN', 'PRINCIPAL')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateAttendance(@PathVariable Long attendanceId,
                                                              @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String statusStr = request.get("status").toString();
            String remarks = request.get("remarks") != null ? request.get("remarks").toString() : "";
            
            Attendance.AttendanceStatus status = Attendance.AttendanceStatus.valueOf(statusStr);
            
            Attendance updatedAttendance = attendanceService.updateAttendance(attendanceId, status, remarks);
            
            response.put("success", true);
            response.put("message", "Attendance updated successfully");
            response.put("attendance", updatedAttendance);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating attendance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Delete attendance record
    @DeleteMapping("/api/{attendanceId}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'IT_ADMIN', 'PRINCIPAL')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAttendance(@PathVariable Long attendanceId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            attendanceService.deleteAttendance(attendanceId);
            
            response.put("success", true);
            response.put("message", "Attendance record deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting attendance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
