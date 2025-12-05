package com.wsims.controller;

import com.parami.wsims.entity.User;
import com.parami.wsims.service.UserService;
import com.parami.wsims.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;

@Controller
public class DashboardController {

    private final UserService userService;
    private final AnnouncementService announcementService;

    @Autowired
    public DashboardController(UserService userService, AnnouncementService announcementService) {
        this.userService = userService;
        this.announcementService = announcementService;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        try {
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                User currentUser = userService.findByUsername(username).orElse(null);
                
                if (currentUser != null && currentUser.getRole() != null) {
                    // Determine user role and fetch appropriate announcements
                    String roleName = currentUser.getRole().getName();
                    var announcements = announcementService.getAnnouncementsForRole(roleName);
                    model.addAttribute("announcements", announcements);
                    model.addAttribute("currentUser", currentUser);
                } else {
                    model.addAttribute("announcements", new ArrayList<>());
                }
            } else {
                model.addAttribute("announcements", new ArrayList<>());
            }
        } catch (Exception e) {
            // If there's an error fetching announcements, continue without them
            model.addAttribute("announcements", new ArrayList<>());
        }
        
        // This tells Spring to render the "dashboard.html" template
        return "dashboard";
    }
}