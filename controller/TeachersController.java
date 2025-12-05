package com.wsims.controller;

import com.parami.wsims.entity.Teacher;
import com.parami.wsims.service.TeacherService;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/teachers")
public class TeachersController {

    private final TeacherService teacherService;

    @Autowired
    public TeachersController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    // Display the teachers management page
    @GetMapping
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    public String teachersPage() {
        return "teachers";
    }

    // Test endpoint to check basic functionality
    @GetMapping("/api/test")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testEndpoint() {
        Map<String, Object> response = new HashMap<>();
        try {
            long totalTeachers = teacherService.getTotalTeacherCount();
            response.put("success", true);
            response.put("message", "Teachers endpoint is working");
            response.put("totalTeachers", totalTeachers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get all teachers with pagination and search
    @GetMapping("/api")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTeachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department) {

        try {
            // Fix sorting field mapping
            String sortField = sortBy;
            if ("id".equals(sortBy)) {
                sortField = "userId";
            } else if ("firstName".equals(sortBy)) {
                sortField = "user.firstName";
            } else if ("lastName".equals(sortBy)) {
                sortField = "user.lastName";
            } else if ("email".equals(sortBy)) {
                sortField = "user.email";
            }
            
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortField).descending() : Sort.by(sortField).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Teacher> teachers;
            if ((search != null && !search.trim().isEmpty()) && 
                (department != null && !department.trim().isEmpty() && !department.equals("ALL"))) {
                teachers = teacherService.searchTeachersByDepartment(department, search, pageable);
            } else if (search != null && !search.trim().isEmpty()) {
                teachers = teacherService.searchTeachers(search, pageable);
            } else if (department != null && !department.trim().isEmpty() && !department.equals("ALL")) {
                teachers = teacherService.findTeachersByDepartment(department, pageable);
            } else {
                teachers = teacherService.findTeachersWithPagination(pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("teachers", teachers.getContent());
            response.put("currentPage", teachers.getNumber());
            response.put("totalElements", teachers.getTotalElements());
            response.put("totalPages", teachers.getTotalPages());
            response.put("pageSize", teachers.getSize());
            response.put("hasNext", teachers.hasNext());
            response.put("hasPrevious", teachers.hasPrevious());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load teachers");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("details", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get teacher by ID
    @GetMapping("/api/{id}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Teacher> getTeacherById(@PathVariable Long id) {
        Optional<Teacher> teacher = teacherService.findById(id);
        return teacher.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    // Get teacher statistics
    @GetMapping("/api/statistics")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTeacherStatistics() {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate cutoffDate = LocalDate.now().minusYears(2); // Active within last 2 years
            
            response.put("total", teacherService.getTotalTeacherCount());
            response.put("active", teacherService.getActiveTeacherCount(cutoffDate));
            response.put("inactive", teacherService.getInactiveTeacherCount(cutoffDate));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to load statistics");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get distinct departments
    @GetMapping("/api/departments")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<List<String>> getDepartments() {
        try {
            List<String> departments = teacherService.getDistinctDepartments();
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}

