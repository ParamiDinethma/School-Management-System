package com.wsims.controller;

import com.parami.wsims.entity.Course;
import com.parami.wsims.entity.Subject;
import com.parami.wsims.service.CourseService;
import com.parami.wsims.service.SubjectService;
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
@RequestMapping("/admin/courses")
public class CourseController {

    private final CourseService courseService;
    private final SubjectService subjectService;

    @Autowired
    public CourseController(CourseService courseService, SubjectService subjectService) {
        this.courseService = courseService;
        this.subjectService = subjectService;
    }

    // Display the courses management page
    @GetMapping
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    public String coursesPage() {
        return "courses";
    }

    // Get all courses with pagination and search
    @GetMapping("/api")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {

        try {
            // Fix sorting field mapping
            String sortField = sortBy;
            if ("id".equals(sortBy)) {
                sortField = "id";
            } else if ("courseName".equals(sortBy)) {
                sortField = "courseName";
            } else if ("courseCode".equals(sortBy)) {
                sortField = "courseCode";
            }

            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortField).descending() : Sort.by(sortField).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Course> courses;
            if (search != null && !search.trim().isEmpty()) {
                courses = courseService.searchCourses(search, pageable);
            } else {
                courses = courseService.findCoursesWithPagination(pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("courses", courses.getContent());
            response.put("currentPage", courses.getNumber());
            response.put("totalItems", courses.getTotalElements());
            response.put("totalPages", courses.getTotalPages());
            response.put("pageSize", courses.getSize());
            response.put("hasNext", courses.hasNext());
            response.put("hasPrevious", courses.hasPrevious());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load courses");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get course by ID
    @GetMapping("/api/{id}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        Optional<Course> course = courseService.findById(id);
        return course.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    // Create new course
    @PostMapping("/api")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createCourse(@RequestBody Course course) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate course code uniqueness
            if (courseService.isCourseCodeExists(course.getCourseCode())) {
                response.put("success", false);
                response.put("message", "Course code already exists");
                return ResponseEntity.badRequest().body(response);
            }

            // Set default values
            if (course.getIsActive() == null) {
                course.setIsActive(true);
            }

            Course savedCourse = courseService.saveCourse(course);
            response.put("success", true);
            response.put("message", "Course created successfully");
            response.put("course", savedCourse);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Handle validation errors from Strategy Pattern
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating course: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Update course
    @PutMapping("/api/{id}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Course> existingCourse = courseService.findById(id);
            if (!existingCourse.isPresent()) {
                response.put("success", false);
                response.put("message", "Course not found");
                return ResponseEntity.notFound().build();
            }

            // Validate course code uniqueness (excluding current course)
            if (courseService.isCourseCodeExistsForOtherCourse(course.getCourseCode(), id)) {
                response.put("success", false);
                response.put("message", "Course code already exists");
                return ResponseEntity.badRequest().body(response);
            }

            // Update the course
            Course courseToUpdate = existingCourse.get();
            courseToUpdate.setCourseName(course.getCourseName());
            courseToUpdate.setCourseCode(course.getCourseCode());
            courseToUpdate.setDescription(course.getDescription());
            courseToUpdate.setCreditHours(course.getCreditHours());
            courseToUpdate.setDurationMonths(course.getDurationMonths());
            courseToUpdate.setIsActive(course.getIsActive());

            Course updatedCourse = courseService.updateCourse(courseToUpdate);
            response.put("success", true);
            response.put("message", "Course updated successfully");
            response.put("course", updatedCourse);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Handle validation errors from Strategy Pattern
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating course: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Delete course
    @DeleteMapping("/api/{id}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteCourse(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Check if course exists first
            Optional<Course> existingCourse = courseService.findById(id);
            if (!existingCourse.isPresent()) {
                response.put("success", false);
                response.put("message", "Course not found with ID: " + id);
                return ResponseEntity.notFound().build();
            }
            
            courseService.deleteCourse(id);
            response.put("success", true);
            response.put("message", "Course '" + existingCourse.get().getCourseName() + "' deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting course: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get course statistics
    @GetMapping("/api/statistics")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getCourseStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", courseService.getTotalCourseCount());
        stats.put("active", courseService.getActiveCourseCount());
        stats.put("inactive", courseService.getInactiveCourseCount());
        return ResponseEntity.ok(stats);
    }

    // Get subjects for a course
    @GetMapping("/api/{courseId}/subjects")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<List<Subject>> getCourseSubjects(@PathVariable Long courseId) {
        List<Subject> subjects = courseService.getSubjectsByCourseId(courseId);
        return ResponseEntity.ok(subjects);
    }

    // Assign subject to course
    @PostMapping("/api/{courseId}/subjects")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignSubjectToCourse(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Long subjectId = Long.valueOf(request.get("subjectId").toString());
            Boolean isMandatory = Boolean.valueOf(request.get("isMandatory").toString());
            String gradeLevel = request.get("gradeLevel").toString();

            courseService.assignSubjectToCourse(courseId, subjectId, isMandatory, gradeLevel);
            
            response.put("success", true);
            response.put("message", "Subject assigned to course successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error assigning subject: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Remove subject from course
    @DeleteMapping("/api/{courseId}/subjects/{subjectId}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeSubjectFromCourse(
            @PathVariable Long courseId,
            @PathVariable Long subjectId) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            courseService.removeSubjectFromCourse(courseId, subjectId);
            response.put("success", true);
            response.put("message", "Subject removed from course successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error removing subject: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get available subjects for a course
    @GetMapping("/api/{courseId}/available-subjects")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<List<Subject>> getAvailableSubjectsForCourse(@PathVariable Long courseId) {
        List<Subject> subjects = subjectService.getAvailableSubjectsForCourse(courseId);
        return ResponseEntity.ok(subjects);
    }
    
    // Validate course using Strategy Pattern
    @PostMapping("/api/validate")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateCourse(@RequestBody Course course) {
        try {
            Map<String, Object> validationResult = courseService.validateCourse(course);
            return ResponseEntity.ok(validationResult);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Validation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
