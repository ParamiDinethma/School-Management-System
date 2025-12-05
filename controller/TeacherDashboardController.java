package com.wsims.controller;

import com.parami.wsims.entity.Course;
import com.parami.wsims.entity.User;
import com.parami.wsims.service.AnnouncementService;
import com.parami.wsims.service.CourseService;
import com.parami.wsims.service.EnrollmentService;
import com.parami.wsims.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/teacher")
@PreAuthorize("hasAuthority('TEACHER')")
public class TeacherDashboardController {
    
    private final UserService userService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final AnnouncementService announcementService;
    
    @Autowired
    public TeacherDashboardController(UserService userService,
                                    CourseService courseService,
                                    EnrollmentService enrollmentService,
                                    AnnouncementService announcementService) {
        this.userService = userService;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.announcementService = announcementService;
    }
    
    @GetMapping("/dashboard")
    public String teacherDashboard(Model model, Authentication authentication) {
        try {
            // Get current teacher
            String username = authentication.getName();
            User teacher = userService.findByUsername(username).orElse(null);
            
            if (teacher != null) {
                // Get teacher's courses
                List<Course> courses = courseService.getCoursesByTeacher(teacher.getId());
                
                // Count total students across all courses
                int totalStudents = 0;
                for (Course course : courses) {
                    try {
                        totalStudents += enrollmentService.getStudentsByCourse(course.getId()).size();
                    } catch (Exception e) {
                        // If there's an error getting students for a course, continue with others
                        continue;
                    }
                }
                
                // Get full announcements (no truncation)
                var announcements = announcementService.getAnnouncementsForRole("TEACHER");
                
                // Add to model
                model.addAttribute("teacher", teacher);
                model.addAttribute("totalCourses", courses.size());
                model.addAttribute("totalStudents", totalStudents);
                model.addAttribute("pendingTasks", 0); // Placeholder for now
                model.addAttribute("announcements", announcements);
            } else {
                // If teacher not found, provide default values
                model.addAttribute("teacher", null);
                model.addAttribute("totalCourses", 0);
                model.addAttribute("totalStudents", 0);
                model.addAttribute("pendingTasks", 0);
                model.addAttribute("announcements", new ArrayList<>());
            }
        } catch (Exception e) {
            // If there's any error, provide default values
            model.addAttribute("teacher", null);
            model.addAttribute("totalCourses", 0);
            model.addAttribute("totalStudents", 0);
            model.addAttribute("pendingTasks", 0);
            model.addAttribute("announcements", new ArrayList<>());
        }
        
        return "teacher-dashboard";
    }
}

