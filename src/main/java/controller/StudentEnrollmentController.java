package com.wsims.controller;

import com.parami.wsims.entity.Enrollment;
import com.parami.wsims.entity.User;
import com.parami.wsims.entity.Course;
import com.parami.wsims.service.EnrollmentService;
import com.parami.wsims.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/student/enrollment")
public class StudentEnrollmentController {

    private final EnrollmentService enrollmentService;
    private final UserService userService;

    @Autowired
    public StudentEnrollmentController(EnrollmentService enrollmentService, UserService userService) {
        this.enrollmentService = enrollmentService;
        this.userService = userService;
    }

    // Display the student enrollment page
    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT')")
    public String studentEnrollmentPage(Authentication authentication) {
        return "student-enrollment";
    }

    // Get current student's enrollments
    @GetMapping("/api/my-enrollments")
    @PreAuthorize("hasAuthority('STUDENT')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMyEnrollments(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "enrollmentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            String username = authentication.getName();
            Optional<User> studentOpt = userService.findByUsername(username);
            
            if (!studentOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Student not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            User student = studentOpt.get();
            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Enrollment> enrollments = enrollmentService.findEnrollmentsByStudent(student, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("enrollments", enrollments.getContent());
            response.put("currentPage", enrollments.getNumber());
            response.put("totalItems", enrollments.getTotalElements());
            response.put("totalPages", enrollments.getTotalPages());
            response.put("pageSize", enrollments.getSize());
            response.put("hasNext", enrollments.hasNext());
            response.put("hasPrevious", enrollments.hasPrevious());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load enrollments");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get available courses for enrollment
    @GetMapping("/api/available-courses")
    @PreAuthorize("hasAuthority('STUDENT')")
    @ResponseBody
    public ResponseEntity<List<Course>> getAvailableCourses(Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> studentOpt = userService.findByUsername(username);
            
            if (!studentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            User student = studentOpt.get();
            List<Course> availableCourses = enrollmentService.getAvailableCoursesForStudent(student.getId());

            return ResponseEntity.ok(availableCourses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Enroll in a course
    @PostMapping("/api/enroll")
    @PreAuthorize("hasAuthority('STUDENT')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enrollInCourse(
            Authentication authentication,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = authentication.getName();
            Optional<User> studentOpt = userService.findByUsername(username);
            
            if (!studentOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Student not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            User student = studentOpt.get();
            Long courseId = Long.valueOf(request.get("courseId").toString());
            String remarks = request.get("remarks") != null ? request.get("remarks").toString() : "";

            // Check if student can enroll
            if (!enrollmentService.canStudentEnrollInCourse(student.getId(), courseId)) {
                response.put("success", false);
                response.put("message", "Cannot enroll in this course");
                return ResponseEntity.badRequest().body(response);
            }

            Enrollment enrollment = enrollmentService.enrollStudentInCourse(student.getId(), courseId, remarks);
            
            response.put("success", true);
            response.put("message", "Successfully enrolled in course");
            response.put("enrollment", enrollment);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error enrolling in course: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Unenroll from a course
    @DeleteMapping("/api/unenroll/{courseId}")
    @PreAuthorize("hasAuthority('STUDENT')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unenrollFromCourse(
            Authentication authentication,
            @PathVariable Long courseId) {

        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = authentication.getName();
            Optional<User> studentOpt = userService.findByUsername(username);
            
            if (!studentOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Student not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            User student = studentOpt.get();
            
            // Check if student is enrolled
            if (!enrollmentService.isStudentEnrolledInCourse(student.getId(), courseId)) {
                response.put("success", false);
                response.put("message", "Student is not enrolled in this course");
                return ResponseEntity.badRequest().body(response);
            }

            enrollmentService.unenrollStudentFromCourse(student.getId(), courseId);
            
            response.put("success", true);
            response.put("message", "Successfully unenrolled from course");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error unenrolling from course: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get enrollment statistics for student
    @GetMapping("/api/statistics")
    @PreAuthorize("hasAuthority('STUDENT')")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getMyStatistics(Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> studentOpt = userService.findByUsername(username);
            
            if (!studentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            User student = studentOpt.get();
            Map<String, Long> stats = new HashMap<>();
            
            List<Enrollment> enrollments = enrollmentService.findEnrollmentsByStudent(student);
            stats.put("total", (long) enrollments.size());
            stats.put("active", enrollments.stream().filter(e -> "ACTIVE".equals(e.getStatus())).count());
            stats.put("completed", enrollments.stream().filter(e -> "COMPLETED".equals(e.getStatus())).count());
            stats.put("dropped", enrollments.stream().filter(e -> "DROPPED".equals(e.getStatus())).count());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
