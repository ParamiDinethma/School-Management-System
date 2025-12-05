package com.wsims.controller;

import com.parami.wsims.entity.*;
import com.parami.wsims.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/teacher/grades")
public class GradeEntryController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private ExamScheduleService examScheduleService;
    
    @Autowired
    private EnrollmentService enrollmentService;
    
    @Autowired
    private GradeService gradeService;

    /**
     * Display the grade entry interface
     */
    @GetMapping
    public String gradeEntryPage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Get courses taught by this teacher
        List<Course> teacherCourses = courseService.getCoursesByTeacher(currentUser.getId());
        
        // Get active exam schedules
        List<ExamSchedule> activeExamSchedules = examScheduleService.getActiveExamSchedules();
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("teacherCourses", teacherCourses);
        model.addAttribute("activeExamSchedules", activeExamSchedules);
        
        return "grade-entry";
    }

    /**
     * Get enrolled students for a specific course
     */
    @GetMapping("/api/course/{courseId}/students")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStudentsForCourse(@PathVariable Long courseId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new IllegalArgumentException("User not authenticated");
            }

            // Verify teacher teaches this course
            List<Course> teacherCourses = courseService.getCoursesByTeacher(currentUser.getId());
            boolean teachesCourse = teacherCourses.stream()
                    .anyMatch(course -> course.getId().equals(courseId));
            
            if (!teachesCourse) {
                throw new IllegalArgumentException("You don't teach this course");
            }

            // Get enrolled students
            List<User> enrolledStudents = enrollmentService.getStudentsByCourse(courseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("students", enrolledStudents);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching students: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get subjects for a specific course
     */
    @GetMapping("/api/course/{courseId}/subjects")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSubjectsForCourse(@PathVariable Long courseId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new IllegalArgumentException("User not authenticated");
            }

            // Verify teacher teaches this course
            List<Course> teacherCourses = courseService.getCoursesByTeacher(currentUser.getId());
            boolean teachesCourse = teacherCourses.stream()
                    .anyMatch(course -> course.getId().equals(courseId));
            
            if (!teachesCourse) {
                throw new IllegalArgumentException("You don't teach this course");
            }

            // Get subjects for this course
            List<Subject> courseSubjects = courseService.getSubjectsByCourse(courseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subjects", courseSubjects);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching subjects: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Save grades for multiple students
     */
    @PostMapping("/api/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveGrades(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== GradeEntryController: Starting save grades ===");
            System.out.println("Request received: " + request);
            
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                System.err.println("ERROR: User not authenticated");
                throw new IllegalArgumentException("User not authenticated");
            }
            
            System.out.println("Current user: " + currentUser.getUsername());

            Long courseId = Long.valueOf(request.get("courseId").toString());
            Long examScheduleId = Long.valueOf(request.get("examScheduleId").toString());
            
            System.out.println("Course ID: " + courseId + ", Exam Schedule ID: " + examScheduleId);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> gradeEntries = (List<Map<String, Object>>) request.get("gradeEntries");
            
            System.out.println("Grade entries received: " + gradeEntries.size());

            // Verify teacher teaches this course
            List<Course> teacherCourses = courseService.getCoursesByTeacher(currentUser.getId());
            boolean teachesCourse = teacherCourses.stream()
                    .anyMatch(course -> course.getId().equals(courseId));
            
            if (!teachesCourse) {
                throw new IllegalArgumentException("You don't teach this course");
            }

            // Save grades
            System.out.println("Calling gradeService.saveBulkGrades...");
            List<Grade> savedGrades = gradeService.saveBulkGrades(gradeEntries, courseId, examScheduleId);
            System.out.println("GradeService returned " + savedGrades.size() + " saved grades");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Grades saved successfully!");
            response.put("savedCount", savedGrades.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("=== GradeEntryController: Exception caught ===");
            System.err.println("Exception type: " + e.getClass().getSimpleName());
            System.err.println("Exception message: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error saving grades: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get existing grades for a course and exam schedule
     */
    @GetMapping("/api/course/{courseId}/exam/{examScheduleId}/grades")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getExistingGrades(@PathVariable Long courseId, 
                                                                @PathVariable Long examScheduleId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new IllegalArgumentException("User not authenticated");
            }

            // Verify teacher teaches this course
            List<Course> teacherCourses = courseService.getCoursesByTeacher(currentUser.getId());
            boolean teachesCourse = teacherCourses.stream()
                    .anyMatch(course -> course.getId().equals(courseId));
            
            if (!teachesCourse) {
                throw new IllegalArgumentException("You don't teach this course");
            }

            // Get existing grades
            List<Grade> existingGrades = gradeService.getGradesByCourseAndExam(courseId, examScheduleId);

            // Map to lightweight DTO to avoid @JsonIgnore hiding nested IDs
            List<Map<String, Object>> gradeDtos = existingGrades.stream().map(g -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", g.getId());
                try { m.put("studentId", g.getStudent() != null ? g.getStudent().getId() : null); } catch (Exception ignore) {}
                try { m.put("subjectId", g.getSubject() != null ? g.getSubject().getId() : null); } catch (Exception ignore) {}
                m.put("marksObtained", g.getMarksObtained());
                m.put("comments", g.getComments());
                return m;
            }).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("grades", gradeDtos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching existing grades: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Delete a specific grade
     */
    @DeleteMapping("/api/{gradeId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteGrade(@PathVariable Long gradeId) {
        Map<String, Object> response = new HashMap<>();
        try {
            gradeService.deleteGrade(gradeId);
            response.put("success", true);
            response.put("message", "Grade deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting grade: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get the currently authenticated user
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
                org.springframework.security.core.userdetails.User springUser = 
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
                
                // Get the actual User entity from the database using the username
                return userService.findByUsername(springUser.getUsername()).orElse(null);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
