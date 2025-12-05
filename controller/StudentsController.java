package com.wsims.controller;

import com.parami.wsims.dto.UserCreationDTO;
import com.parami.wsims.entity.Student;
import com.parami.wsims.service.StudentService;
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

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/students")
public class StudentsController {

    private final StudentService studentService;

    @Autowired
    public StudentsController(StudentService studentService) {
        this.studentService = studentService;
    }

    // Display the students management page
    @GetMapping
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    public String studentsPage() {
        return "students";
    }

    // Test endpoint to check basic functionality
    @GetMapping("/api/test")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testEndpoint() {
        Map<String, Object> response = new HashMap<>();
        try {
            long totalStudents = studentService.getTotalStudentCount();
            response.put("success", true);
            response.put("message", "Students endpoint is working");
            response.put("totalStudents", totalStudents);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get all students with pagination and search
    @GetMapping("/api")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String gradeLevel) {

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

            Page<Student> students;
            if ((search != null && !search.trim().isEmpty()) && 
                (gradeLevel != null && !gradeLevel.trim().isEmpty() && !gradeLevel.equals("ALL"))) {
                students = studentService.searchStudentsByGradeLevel(gradeLevel, search, pageable);
            } else if (search != null && !search.trim().isEmpty()) {
                students = studentService.searchStudents(search, pageable);
            } else if (gradeLevel != null && !gradeLevel.trim().isEmpty() && !gradeLevel.equals("ALL")) {
                students = studentService.findStudentsByGradeLevel(gradeLevel, pageable);
            } else {
                students = studentService.findStudentsWithPagination(pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("students", students.getContent());
            response.put("currentPage", students.getNumber());
            response.put("totalItems", students.getTotalElements());
            response.put("totalPages", students.getTotalPages());
            response.put("pageSize", students.getSize());
            response.put("hasNext", students.hasNext());
            response.put("hasPrevious", students.hasPrevious());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load students");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("details", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get student by ID
    @GetMapping("/api/{id}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
        Optional<Student> student = studentService.findById(id);
        return student.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    // Create new student
    @PostMapping("/api")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createStudent(@Valid @RequestBody UserCreationDTO userData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate required fields for student
            if (userData.getGradeLevel() == null || userData.getGradeLevel().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Grade level is required for students");
                return ResponseEntity.badRequest().body(response);
            }

            // Set role to STUDENT
            userData.setRoleName("STUDENT");

            Student savedStudent = studentService.createStudentFromDTO(userData);
            response.put("success", true);
            response.put("message", "Student created successfully");
            response.put("student", savedStudent);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating student: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Update student
    @PutMapping("/api/{id}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStudent(@PathVariable Long id, @RequestBody Student student) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<Student> existingStudent = studentService.findById(id);
            if (!existingStudent.isPresent()) {
                response.put("success", false);
                response.put("message", "Student not found");
                return ResponseEntity.notFound().build();
            }

            Student studentToUpdate = existingStudent.get();
            
            // Update student-specific fields
            if (student.getGradeLevel() != null) {
                studentToUpdate.setGradeLevel(student.getGradeLevel());
            }
            if (student.getEmergencyContactPhone() != null) {
                studentToUpdate.setEmergencyContactPhone(student.getEmergencyContactPhone());
            }
            if (student.getEnrollmentDate() != null) {
                studentToUpdate.setEnrollmentDate(student.getEnrollmentDate());
            }

            // Also allow updating nested user fields when provided
            if (student.getUser() != null) {
                if (student.getUser().getFirstName() != null) {
                    existingStudent.get().getUser().setFirstName(student.getUser().getFirstName());
                }
                if (student.getUser().getLastName() != null) {
                    existingStudent.get().getUser().setLastName(student.getUser().getLastName());
                }
                if (student.getUser().getEmail() != null) {
                    existingStudent.get().getUser().setEmail(student.getUser().getEmail());
                }
                if (student.getUser().getPhone() != null) {
                    existingStudent.get().getUser().setPhone(student.getUser().getPhone());
                }
                if (student.getUser().getAddress() != null) {
                    existingStudent.get().getUser().setAddress(student.getUser().getAddress());
                }
            }

            Student updatedStudent = studentService.updateStudent(studentToUpdate);
            response.put("success", true);
            response.put("message", "Student updated successfully");
            response.put("student", updatedStudent);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating student: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Delete student
    @DeleteMapping("/api/{id}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteStudent(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<Student> student = studentService.findById(id);
            if (!student.isPresent()) {
                response.put("success", false);
                response.put("message", "Student not found");
                return ResponseEntity.notFound().build();
            }

            studentService.deleteStudent(id);
            response.put("success", true);
            response.put("message", "Student deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting student: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get student statistics
    @GetMapping("/api/statistics")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getStudentStatistics() {
        try {
            Map<String, Long> stats = studentService.getStudentStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Get all grade levels
    @GetMapping("/api/grade-levels")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<List<String>> getAllGradeLevels() {
        try {
            List<String> gradeLevels = studentService.getAllGradeLevels();
            return ResponseEntity.ok(gradeLevels);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
