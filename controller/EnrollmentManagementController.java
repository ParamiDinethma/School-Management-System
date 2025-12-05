package com.wsims.controller;

import com.parami.wsims.entity.Enrollment;
import com.parami.wsims.entity.User;
import com.parami.wsims.entity.Course;
import com.parami.wsims.service.EnrollmentService;
import com.parami.wsims.service.UserService;
import com.parami.wsims.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/enrollments")
public class EnrollmentManagementController {

    private final EnrollmentService enrollmentService;
    private final UserService userService;
    private final CourseService courseService;

    @Autowired
    public EnrollmentManagementController(EnrollmentService enrollmentService, 
                                        UserService userService,
                                        CourseService courseService) {
        this.enrollmentService = enrollmentService;
        this.userService = userService;
        this.courseService = courseService;
    }

    // Display the enrollment management page
    @GetMapping
    @PreAuthorize("hasAuthority('REGISTRAR')")
    public String enrollmentManagementPage() {
        return "enrollment-management";
    }

    // Get all enrollments with pagination and search
    @GetMapping("/api")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "enrollmentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long courseId) {

        try {
            // Fix sorting field mapping
            String sortField = sortBy;
            if ("enrollmentDate".equals(sortBy)) {
                sortField = "enrollmentDate";
            } else if ("status".equals(sortBy)) {
                sortField = "status";
            }

            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortField).descending() : Sort.by(sortField).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Enrollment> enrollments;
            if (search != null && !search.trim().isEmpty()) {
                enrollments = enrollmentService.searchEnrollments(search, pageable);
            } else if (status != null && !status.trim().isEmpty()) {
                enrollments = enrollmentService.findEnrollmentsByStatus(status, pageable);
            } else if (courseId != null) {
                Optional<Course> course = courseService.findById(courseId);
                if (course.isPresent()) {
                    enrollments = enrollmentService.findEnrollmentsByCourse(course.get(), pageable);
                } else {
                    enrollments = Page.empty(pageable);
                }
            } else {
                enrollments = enrollmentService.findEnrollmentsWithPagination(pageable);
            }

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

    // Get enrollment by ID
    @GetMapping("/api/{id}")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    @ResponseBody
    public ResponseEntity<Enrollment> getEnrollmentById(@PathVariable Long id) {
        Optional<Enrollment> enrollment = enrollmentService.findById(id);
        return enrollment.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    // Get all courses for dropdown
    @GetMapping("/api/courses")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    @ResponseBody
    public ResponseEntity<List<Course>> getAllCourses() {
        List<Course> courses = courseService.findAllCourses();
        return ResponseEntity.ok(courses);
    }

    // Get all students for dropdown
    @GetMapping("/api/students")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    @ResponseBody
    public ResponseEntity<List<User>> getAllStudents() {
        List<User> students = userService.findUsersByRole("STUDENT", Pageable.unpaged()).getContent();
        return ResponseEntity.ok(students);
    }

    // Create new enrollment (manual enrollment by registrar)
    @PostMapping("/api")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createEnrollment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Long studentId = Long.valueOf(request.get("studentId").toString());
            Long courseId = Long.valueOf(request.get("courseId").toString());
            String status = request.get("status") != null ? request.get("status").toString() : "ACTIVE";
            String remarks = request.get("remarks") != null ? request.get("remarks").toString() : "";

            // Validate inputs
            if (!enrollmentService.canStudentEnrollInCourse(studentId, courseId)) {
                response.put("success", false);
                response.put("message", "Student cannot enroll in this course");
                return ResponseEntity.badRequest().body(response);
            }

            if (!enrollmentService.isValidEnrollmentStatus(status)) {
                response.put("success", false);
                response.put("message", "Invalid enrollment status");
                return ResponseEntity.badRequest().body(response);
            }

            Enrollment enrollment = enrollmentService.enrollStudentInCourse(studentId, courseId, remarks);
            enrollment.setStatus(status);
            enrollment = enrollmentService.updateEnrollment(enrollment);

            response.put("success", true);
            response.put("message", "Enrollment created successfully");
            response.put("enrollment", enrollment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating enrollment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Update enrollment
    @PutMapping("/api/{id}")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateEnrollment(
            @PathVariable Long id, 
            @RequestBody Map<String, Object> request) {

        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Enrollment> existingEnrollment = enrollmentService.findById(id);
            if (!existingEnrollment.isPresent()) {
                response.put("success", false);
                response.put("message", "Enrollment not found");
                return ResponseEntity.notFound().build();
            }

            String status = request.get("status") != null ? request.get("status").toString() : null;
            String grade = request.get("grade") != null ? request.get("grade").toString() : null;
            String remarks = request.get("remarks") != null ? request.get("remarks").toString() : null;

            Enrollment enrollment = existingEnrollment.get();
            
            if (status != null && enrollmentService.isValidEnrollmentStatus(status)) {
                enrollment.setStatus(status);
            }
            
            if (grade != null && enrollmentService.isValidGrade(grade)) {
                enrollment.setGrade(grade);
            }
            
            if (remarks != null) {
                enrollment.setRemarks(remarks);
            }

            Enrollment updatedEnrollment = enrollmentService.updateEnrollment(enrollment);
            response.put("success", true);
            response.put("message", "Enrollment updated successfully");
            response.put("enrollment", updatedEnrollment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating enrollment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Delete enrollment
    @DeleteMapping("/api/{id}")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteEnrollment(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Enrollment> existingEnrollment = enrollmentService.findById(id);
            if (!existingEnrollment.isPresent()) {
                response.put("success", false);
                response.put("message", "Enrollment not found with ID: " + id);
                return ResponseEntity.notFound().build();
            }
            
            enrollmentService.deleteEnrollment(id);
            response.put("success", true);
            response.put("message", "Enrollment deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting enrollment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get enrollment statistics
    @GetMapping("/api/statistics")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getEnrollmentStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", enrollmentService.getTotalEnrollmentCount());
        stats.put("active", enrollmentService.getActiveEnrollmentCount());
        stats.put("completed", enrollmentService.getEnrollmentCountByStatus("COMPLETED"));
        stats.put("dropped", enrollmentService.getEnrollmentCountByStatus("DROPPED"));
        return ResponseEntity.ok(stats);
    }

    // Get enrollments by course
    @GetMapping("/api/course/{courseId}")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getEnrollmentsByCourse(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Optional<Course> course = courseService.findById(courseId);
            if (!course.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Enrollment> enrollments = enrollmentService.findEnrollmentsByCourse(course.get(), pageable);

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
            errorResponse.put("error", "Failed to load enrollments for course");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
