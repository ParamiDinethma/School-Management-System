package com.wsims.controller;

import com.parami.wsims.entity.Grade;
import com.parami.wsims.entity.User;
import com.parami.wsims.service.GradeService;
import com.parami.wsims.service.ParentLinkService;
import com.parami.wsims.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentGradesController {

    @Autowired
    private GradeService gradeService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ParentLinkService parentLinkService;

    /**
     * Display student grades page
     */
    @GetMapping("/grades")
    public String studentGrades(Model model) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return "redirect:/login";
            }

            List<Grade> grades = gradeService.getGradesByStudent(currentUser.getId());
            
            // Group grades by exam schedule
            Map<String, List<Grade>> gradesByExam = grades.stream()
                    .collect(Collectors.groupingBy(
                            grade -> grade.getExamSchedule().getExamName() + " (" + grade.getExamSchedule().getAcademicYear() + ")",
                            Collectors.toList()
                    ));

            // Calculate summary statistics
            long passingGradesCount = grades.stream()
                    .filter(grade -> grade.isPassing())
                    .count();
            
            long excellentGradesCount = grades.stream()
                    .filter(grade -> grade.isExcellent())
                    .count();

            model.addAttribute("grades", grades);
            model.addAttribute("gradesByExam", gradesByExam);
            model.addAttribute("student", currentUser);
            model.addAttribute("passingGradesCount", passingGradesCount);
            model.addAttribute("excellentGradesCount", excellentGradesCount);
            
            return "student-grades";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading grades: " + e.getMessage());
            return "student-grades";
        }
    }

    /**
     * API endpoint to get grades for a specific student (for parent portal)
     */
    @GetMapping("/api/grades/{studentId}")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('STUDENT', 'PARENT')")
    public ResponseEntity<Map<String, Object>> getStudentGrades(@PathVariable Long studentId) {
        try {
            System.out.println("=== GRADES API CALLED ===");
            System.out.println("Requested student ID: " + studentId);
            
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                System.out.println("ERROR: User not authenticated");
                return ResponseEntity.status(401).body(createErrorResponse("User not authenticated"));
            }

            System.out.println("Current user: " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ", Role: " + currentUser.getRole().getName() + ")");

            // Check if current user is the student or a parent of the student
            if (!currentUser.getId().equals(studentId) && !isParentOfStudent(currentUser.getId(), studentId)) {
                System.out.println("ACCESS DENIED: User not authorized to view student grades");
                return ResponseEntity.status(403).body(createErrorResponse("Access denied"));
            }
            
            System.out.println("ACCESS GRANTED: Loading grades for student " + studentId);

            List<Grade> grades = gradeService.getGradesByStudent(studentId);
            
            // Group grades by exam schedule
            Map<String, List<Grade>> gradesByExam = grades.stream()
                    .collect(Collectors.groupingBy(
                            grade -> grade.getExamSchedule().getExamName() + " (" + grade.getExamSchedule().getAcademicYear() + ")",
                            Collectors.toList()
                    ));

            // Calculate summary statistics
            long passingGradesCount = grades.stream()
                    .filter(grade -> grade.isPassing())
                    .count();
            
            long excellentGradesCount = grades.stream()
                    .filter(grade -> grade.isExcellent())
                    .count();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("grades", grades);
            response.put("gradesByExam", gradesByExam);
            response.put("student", userService.getUserById(studentId));
            response.put("passingGradesCount", passingGradesCount);
            response.put("excellentGradesCount", excellentGradesCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Error loading grades: " + e.getMessage()));
        }
    }

    /**
     * Get the currently authenticated user
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                return userService.findByUsername(authentication.getName()).orElse(null);
            }
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if current user is a parent of the given student
     */
    private boolean isParentOfStudent(Long parentId, Long studentId) {
        try {
            System.out.println("Checking if parent " + parentId + " is linked to student " + studentId);
            boolean isLinked = parentLinkService.isParentLinkedToStudent(parentId, studentId);
            System.out.println("Parent-student link check result: " + isLinked);
            return isLinked;
        } catch (Exception e) {
            System.err.println("Error checking parent relationship: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
