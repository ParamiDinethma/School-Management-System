package com.wsims.controller;

import com.parami.wsims.entity.Announcement;
import com.parami.wsims.entity.Announcement.TargetAudience;
import com.parami.wsims.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/announcements")
public class AnnouncementController {
    
    @Autowired
    private AnnouncementService announcementService;
    
    /**
     * Display the announcements management page
     */
    @GetMapping
    public String showAnnouncementsPage() {
        return "announcements";
    }
    
    /**
     * Get all announcements (API endpoint)
     */
    @GetMapping("/api/all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllAnnouncements() {
        try {
            List<Announcement> announcements = announcementService.getAllAnnouncements();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("announcements", announcements);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching announcements: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Create a new announcement
     */
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createAnnouncement(@RequestBody Map<String, String> request) {
        try {
            // Validate required fields
            String title = request.get("title");
            String content = request.get("content");
            String targetAudienceStr = request.get("targetAudience");
            
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("Title is required");
            }
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("Content is required");
            }
            if (targetAudienceStr == null || targetAudienceStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Target audience is required");
            }
            
            // Get the currently logged-in user's username
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String createdBy = "Unknown";
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
                org.springframework.security.core.userdetails.User springUser = 
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
                createdBy = springUser.getUsername();
            }
            
            // Parse target audience
            TargetAudience targetAudience = TargetAudience.valueOf(targetAudienceStr);
            
            // Create announcement
            Announcement announcement = new Announcement();
            announcement.setTitle(title.trim());
            announcement.setContent(content.trim());
            announcement.setTargetAudience(targetAudience);
            announcement.setCreatedBy(createdBy);
            
            Announcement savedAnnouncement = announcementService.createAnnouncement(announcement);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Announcement posted successfully!");
            response.put("announcement", savedAnnouncement);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating announcement: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Delete an announcement
     */
    @DeleteMapping("/api/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAnnouncement(@PathVariable Long id) {
        try {
            announcementService.deleteAnnouncement(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Announcement deleted successfully!");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting announcement: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}


