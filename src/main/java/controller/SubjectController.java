package com.wsims.controller;

import com.parami.wsims.entity.Subject;
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
@RequestMapping("/admin/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    @Autowired
    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    // Display the subjects management page
    @GetMapping
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    public String subjectsPage() {
        return "subject-management";
    }

    // Get all subjects with pagination and search
    @GetMapping("/api")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSubjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {

        try {
            // Add debugging logs
            System.out.println("SubjectController.getSubjects() called with:");
            System.out.println("  page: " + page);
            System.out.println("  size: " + size);
            System.out.println("  sortBy: " + sortBy);
            System.out.println("  sortDir: " + sortDir);
            System.out.println("  search: " + search);

            // Fix sorting field mapping
            String sortField = sortBy;
            if ("id".equals(sortBy)) {
                sortField = "id";
            } else if ("subjectName".equals(sortBy)) {
                sortField = "subjectName";
            } else if ("subjectCode".equals(sortBy)) {
                sortField = "subjectCode";
            }

            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortField).descending() : Sort.by(sortField).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Subject> subjects;
            if (search != null && !search.trim().isEmpty()) {
                subjects = subjectService.searchSubjects(search, pageable);
            } else {
                subjects = subjectService.findSubjectsWithPagination(pageable);
            }

            // Add debugging logs for results
            System.out.println("Found " + subjects.getTotalElements() + " total subjects");
            System.out.println("Returning " + subjects.getContent().size() + " subjects for page " + page);

            Map<String, Object> response = new HashMap<>();
            response.put("subjects", subjects.getContent());
            response.put("currentPage", subjects.getNumber());
            response.put("totalItems", subjects.getTotalElements());
            response.put("totalPages", subjects.getTotalPages());
            response.put("pageSize", subjects.getSize());
            response.put("hasNext", subjects.hasNext());
            response.put("hasPrevious", subjects.hasPrevious());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in SubjectController.getSubjects(): " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load subjects");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get subject by ID
    @GetMapping("/api/{id}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Subject> getSubjectById(@PathVariable Long id) {
        Optional<Subject> subject = subjectService.findById(id);
        return subject.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    // Get all subjects (for dropdowns)
    @GetMapping("/api/all")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<List<Subject>> getAllSubjects() {
        List<Subject> subjects = subjectService.findAllSubjects();
        return ResponseEntity.ok(subjects);
    }

    // Create new subject
    @PostMapping("/api")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createSubject(@RequestBody Subject subject) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate required fields
            if (subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty() ||
                subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Subject name and code are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Set default values
            if (subject.getIsActive() == null) {
                subject.setIsActive(true);
            }
            // Note: credits, hoursPerWeek, and status fields are not available in current database schema

            Subject savedSubject = subjectService.saveSubject(subject);
            response.put("success", true);
            response.put("message", "Subject created successfully");
            response.put("subject", savedSubject);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating subject: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Update subject
    @PutMapping("/api/{id}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateSubject(@PathVariable Long id, @RequestBody Subject subject) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Subject> existingSubject = subjectService.findById(id);
            if (!existingSubject.isPresent()) {
                response.put("success", false);
                response.put("message", "Subject not found");
                return ResponseEntity.notFound().build();
            }

            // Validate required fields
            if (subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty() ||
                subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Subject name and code are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Update the subject
            Subject subjectToUpdate = existingSubject.get();
            subjectToUpdate.setSubjectName(subject.getSubjectName());
            subjectToUpdate.setSubjectCode(subject.getSubjectCode());
            subjectToUpdate.setDescription(subject.getDescription());
            // Note: credits, hoursPerWeek, and status fields are not available in current database schema
            subjectToUpdate.setIsActive(subject.getIsActive());

            Subject updatedSubject = subjectService.updateSubject(subjectToUpdate);
            response.put("success", true);
            response.put("message", "Subject updated successfully");
            response.put("subject", updatedSubject);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating subject: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Delete subject
    @DeleteMapping("/api/{id}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteSubject(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Check if subject exists first
            Optional<Subject> existingSubject = subjectService.findById(id);
            if (!existingSubject.isPresent()) {
                response.put("success", false);
                response.put("message", "Subject not found with ID: " + id);
                return ResponseEntity.notFound().build();
            }
            
            subjectService.deleteSubject(id);
            response.put("success", true);
            response.put("message", "Subject '" + existingSubject.get().getSubjectName() + "' deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting subject: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get subject statistics
    @GetMapping("/api/statistics")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getSubjectStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", subjectService.getTotalSubjectCount());
        stats.put("active", subjectService.getActiveSubjectCount());
        stats.put("inactive", subjectService.getInactiveSubjectCount());
        return ResponseEntity.ok(stats);
    }

    // Debug endpoint to test database connection
    @GetMapping("/api/debug")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugSubjects() {
        Map<String, Object> debug = new HashMap<>();
        try {
            List<Subject> allSubjects = subjectService.findAllSubjects();
            debug.put("totalSubjects", allSubjects.size());
            debug.put("subjects", allSubjects);
            debug.put("success", true);
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            debug.put("success", false);
            debug.put("error", e.getMessage());
            debug.put("stackTrace", e.getStackTrace());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(debug);
        }
    }
}