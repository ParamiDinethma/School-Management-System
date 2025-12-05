package com.wsims.controller;

import com.parami.wsims.entity.User;
import com.parami.wsims.service.UserService;
import com.parami.wsims.service.EnrollmentService;
import com.parami.wsims.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;

@Controller
@RequestMapping("/student")
public class StudentDashboardController {

    private final UserService userService;
    private final EnrollmentService enrollmentService;
    private final AnnouncementService announcementService;

    @Autowired
    public StudentDashboardController(UserService userService, EnrollmentService enrollmentService, AnnouncementService announcementService) {
        this.userService = userService;
        this.enrollmentService = enrollmentService;
        this.announcementService = announcementService;
    }

    // Display the student dashboard
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('STUDENT')")
    public String studentDashboard(Authentication authentication, Model model) {
        try {
            // Fetch announcements for STUDENT role
            var announcements = announcementService.getAnnouncementsForRole("STUDENT");
            model.addAttribute("announcements", announcements);
        } catch (Exception e) {
            // If there's an error fetching announcements, continue without them
            model.addAttribute("announcements", new ArrayList<>());
        }
        return "student-dashboard";
    }

    // Display the student profile page
    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('STUDENT')")
    public String studentProfile(Authentication authentication) {
        return "student-profile";
    }
}
