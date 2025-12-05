package com.wsims.controller;

import com.parami.wsims.entity.StudentParentLink;
import com.parami.wsims.entity.Student;
import com.parami.wsims.entity.Parent;
import com.parami.wsims.entity.User;
import com.parami.wsims.service.ParentLinkService;
import com.parami.wsims.service.UserService;
import com.parami.wsims.repository.StudentRepository;
import com.parami.wsims.repository.ParentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/parent-links")
@PreAuthorize("hasAnyAuthority('PRINCIPAL', 'IT_ADMIN', 'REGISTRAR')")
public class ParentLinkManagementController {

    private final ParentLinkService parentLinkService;
    private final UserService userService;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;

    @Autowired
    public ParentLinkManagementController(ParentLinkService parentLinkService,
                                        UserService userService,
                                        StudentRepository studentRepository,
                                        ParentRepository parentRepository) {
        this.parentLinkService = parentLinkService;
        this.userService = userService;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
    }

    // Display the parent link management page
    @GetMapping
    public String parentLinkManagementPage(Model model) {
        // Add statistics to the model
        Map<String, Long> statsLong = parentLinkService.getLinkStatistics();
        Map<String, Object> stats = new HashMap<>();
        statsLong.forEach((key, value) -> stats.put(key, value));
        model.addAttribute("statistics", stats);
        return "parent-link-management";
    }

    // Get all parent-student links with pagination
    @GetMapping("/api/links")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllLinks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchType) {

        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("=== Getting parent links ===");
            System.out.println("Page: " + page + ", Size: " + size + ", Search: " + search);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<StudentParentLink> linksPage;

            if (search != null && !search.trim().isEmpty()) {
                if ("student".equals(searchType)) {
                    linksPage = parentLinkService.searchLinksByStudentName(search.trim(), pageable);
                } else if ("parent".equals(searchType)) {
                    linksPage = parentLinkService.searchLinksByParentName(search.trim(), pageable);
                } else {
                    linksPage = parentLinkService.getAllLinks(pageable);
                }
            } else {
                linksPage = parentLinkService.getAllLinks(pageable);
            }

            System.out.println("Found " + linksPage.getContent().size() + " links");
            System.out.println("Total elements: " + linksPage.getTotalElements());

            response.put("content", linksPage.getContent());
            response.put("totalElements", linksPage.getTotalElements());
            response.put("totalPages", linksPage.getTotalPages());
            response.put("currentPage", linksPage.getNumber());
            response.put("size", linksPage.getSize());
            response.put("first", linksPage.isFirst());
            response.put("last", linksPage.isLast());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error retrieving parent links: " + e.getMessage());
            e.printStackTrace();
            response.put("error", "Error retrieving parent links: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get all students for dropdown
    @GetMapping("/api/students")
    @ResponseBody
    public ResponseEntity<List<User>> getAllStudents() {
        try {
            System.out.println("=== Getting students for dropdown ===");
            
            // Get all Student entities and map to their User objects
            List<Student> studentEntities = studentRepository.findAll();
            System.out.println("Found " + studentEntities.size() + " Student entities");
            
            List<User> students = studentEntities.stream()
                    .map(Student::getUser)
                    .toList();
            System.out.println("Returning " + students.size() + " students");
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            System.err.println("Error loading students: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get all parents for dropdown
    @GetMapping("/api/parents")
    @ResponseBody
    public ResponseEntity<List<User>> getAllParents() {
        try {
            System.out.println("=== Getting parents for dropdown ===");
            
            // First, try to get Parent entities
            List<Parent> parentEntities = parentRepository.findAll();
            System.out.println("Found " + parentEntities.size() + " Parent entities");
            
            if (!parentEntities.isEmpty()) {
                // If Parent entities exist, map to User objects
                List<User> parents = parentEntities.stream()
                        .map(Parent::getUser)
                        .toList();
                System.out.println("Returning " + parents.size() + " parents from Parent entities");
                return ResponseEntity.ok(parents);
            } else {
                // If no Parent entities exist, get users with PARENT role directly
                System.out.println("No Parent entities found, getting users with PARENT role...");
                Pageable pageable = PageRequest.of(0, 1000);
                Page<User> parentsPage = userService.findUsersByRole("PARENT", pageable);
                System.out.println("Found " + parentsPage.getContent().size() + " users with PARENT role");
                return ResponseEntity.ok(parentsPage.getContent());
            }
        } catch (Exception e) {
            System.err.println("Error loading parents: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get unlinked students
    @GetMapping("/api/students/unlinked")
    @ResponseBody
    public ResponseEntity<List<Student>> getUnlinkedStudents() {
        try {
            List<Student> students = parentLinkService.getUnlinkedStudents();
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get unlinked parents
    @GetMapping("/api/parents/unlinked")
    @ResponseBody
    public ResponseEntity<List<Parent>> getUnlinkedParents() {
        try {
            List<Parent> parents = parentLinkService.getUnlinkedParents();
            return ResponseEntity.ok(parents);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Create a new parent-student link
    @PostMapping("/api/links")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createLink(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long studentUserId = Long.valueOf(request.get("studentUserId").toString());
            Long parentUserId = Long.valueOf(request.get("parentUserId").toString());

            // Get current user as the creator
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            StudentParentLink link = parentLinkService.createLink(studentUserId, parentUserId, currentUser.getId());
            
            response.put("success", true);
            response.put("message", "Parent-student link created successfully");
            response.put("link", link);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating link: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Delete a parent-student link
    @DeleteMapping("/api/links")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteLink(
            @RequestParam Long studentUserId,
            @RequestParam Long parentUserId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            parentLinkService.deleteLink(studentUserId, parentUserId);
            
            response.put("success", true);
            response.put("message", "Parent-student link deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting link: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get statistics
    @GetMapping("/api/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Long> statsLong = parentLinkService.getLinkStatistics();
            Map<String, Object> stats = new HashMap<>();
            statsLong.forEach((key, value) -> stats.put(key, value));
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error retrieving statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Debug endpoint to check data
    @GetMapping("/api/debug")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugData() {
        Map<String, Object> debug = new HashMap<>();
        try {
            List<Parent> parents = parentRepository.findAll();
            List<Student> students = studentRepository.findAll();
            
            debug.put("parentCount", parents.size());
            debug.put("studentCount", students.size());
            
            if (!parents.isEmpty()) {
                Parent firstParent = parents.get(0);
                debug.put("firstParentUserId", firstParent.getUserId());
                debug.put("firstParentUser", firstParent.getUser());
                debug.put("firstParentUserNull", firstParent.getUser() == null);
            }
            
            if (!students.isEmpty()) {
                Student firstStudent = students.get(0);
                debug.put("firstStudentUserId", firstStudent.getUserId());
                debug.put("firstStudentUser", firstStudent.getUser());
                debug.put("firstStudentUserNull", firstStudent.getUser() == null);
            }
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            debug.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(debug);
        }
    }
}
