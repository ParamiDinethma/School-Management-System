package com.wsims.controller;

import com.parami.wsims.entity.Attendance;
import com.parami.wsims.entity.User;
import com.parami.wsims.service.AttendanceService;
import com.parami.wsims.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Map;

@Controller
@RequestMapping("/student/attendance")
public class StudentAttendanceController {
    
    private final AttendanceService attendanceService;
    private final UserService userService;
    
    @Autowired
    public StudentAttendanceController(AttendanceService attendanceService,
                                    UserService userService) {
        this.attendanceService = attendanceService;
        this.userService = userService;
    }
    
    // Display the student attendance page
    @GetMapping
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT')")
    public String studentAttendancePage(Model model) {
        // Get the current logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("studentId", currentUser.getId());
        
        return "student-attendance";
    }
    
    // Get attendance records for the logged-in student
    @GetMapping("/api/my-attendance")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMyAttendance(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "attendanceDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String subjectId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Student attendance request - page: " + page + ", size: " + size + ", sortBy: " + sortBy);
            
            // Get the current logged-in user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            System.out.println("Getting attendance for student: " + username);
            
            User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            System.out.println("Found user with ID: " + currentUser.getId());
            
            // For now, we'll get all attendance records for the student
            // You might want to add filtering by date range, status, or subject in the future
            var attendancePage = attendanceService.getAttendanceByStudentId(
                currentUser.getId(), page, size, sortBy, sortDir);
            
            System.out.println("Found " + attendancePage.getTotalElements() + " attendance records");
            
            response.put("content", attendancePage.getContent());
            response.put("totalElements", attendancePage.getTotalElements());
            response.put("totalPages", attendancePage.getTotalPages());
            response.put("currentPage", attendancePage.getNumber());
            response.put("size", attendancePage.getSize());
            response.put("first", attendancePage.isFirst());
            response.put("last", attendancePage.isLast());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Error retrieving student attendance: " + e.getMessage());
            e.printStackTrace();
            response.put("error", "Error retrieving attendance records: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Get attendance statistics for the logged-in student
    @GetMapping("/api/my-statistics")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMyAttendanceStatistics() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Getting attendance statistics for student");
            
            // Get the current logged-in user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            System.out.println("Getting statistics for user ID: " + currentUser.getId());
            
            Map<String, Object> statistics = attendanceService.getAttendanceStatistics(currentUser.getId());
            
            System.out.println("Statistics retrieved: " + statistics);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            System.err.println("Error retrieving attendance statistics: " + e.getMessage());
            e.printStackTrace();
            response.put("error", "Error retrieving attendance statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Get attendance records for a specific date range
    @GetMapping("/api/my-attendance/date-range")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMyAttendanceByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get the current logged-in user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            
            var attendancePage = attendanceService.getAttendanceByStudentIdAndDateRange(
                currentUser.getId(), start, end, page, size);
            
            response.put("content", attendancePage.getContent());
            response.put("totalElements", attendancePage.getTotalElements());
            response.put("totalPages", attendancePage.getTotalPages());
            response.put("currentPage", attendancePage.getNumber());
            response.put("size", attendancePage.getSize());
            response.put("first", attendancePage.isFirst());
            response.put("last", attendancePage.isLast());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "Error retrieving attendance records: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Get subjects that the student has attendance records for
    @GetMapping("/api/my-subjects")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMySubjects() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get the current logged-in user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            var subjects = attendanceService.getSubjectsForStudent(currentUser.getId());
            
            response.put("subjects", subjects);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "Error retrieving subjects: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Get attendance summary (count by status) for the logged-in student
    @GetMapping("/api/my-summary")
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMyAttendanceSummary() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get the current logged-in user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Map<Attendance.AttendanceStatus, Long> summary = attendanceService.getAttendanceSummary(currentUser.getId());
            
            response.put("summary", summary);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "Error retrieving attendance summary: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
