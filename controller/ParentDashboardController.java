package com.wsims.controller;

import com.parami.wsims.entity.User;
import com.parami.wsims.entity.Attendance;
import com.parami.wsims.service.ParentLinkService;
import com.parami.wsims.service.UserService;
import com.parami.wsims.service.AttendanceService;
import com.parami.wsims.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/parent")
@PreAuthorize("hasAuthority('PARENT')")
public class ParentDashboardController {

    private final ParentLinkService parentLinkService;
    private final UserService userService;
    private final AttendanceService attendanceService;
    private final AnnouncementService announcementService;

    @Autowired
    public ParentDashboardController(ParentLinkService parentLinkService,
                                   UserService userService,
                                   AttendanceService attendanceService,
                                   AnnouncementService announcementService) {
        this.parentLinkService = parentLinkService;
        this.userService = userService;
        this.attendanceService = attendanceService;
        this.announcementService = announcementService;
    }

    // Parent dashboard page
    @GetMapping("/dashboard")
    public String parentDashboard(Model model) {
        try {
            // Get current parent user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get linked children
            List<User> children = parentLinkService.getLinkedStudentsForParent(currentUser.getId());
            
            // Fetch announcements for PARENT role
            var announcements = announcementService.getAnnouncementsForRole("PARENT");
            
            model.addAttribute("parent", currentUser);
            model.addAttribute("children", children);
            model.addAttribute("childrenCount", children.size());
            model.addAttribute("announcements", announcements);

            return "parent-dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            model.addAttribute("announcements", new ArrayList<>());
            return "parent-dashboard";
        }
    }

    // Child detail page
    @GetMapping("/child/{childUserId}")
    public String childDetail(@PathVariable Long childUserId, Model model) {
        System.out.println("=== CHILD DETAIL METHOD START ===");
        System.out.println("Loading child detail page for child ID: " + childUserId);
        
        try {
            // Get current parent user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("Authentication: " + (auth != null ? "Present" : "NULL"));
            
            if (auth == null) {
                System.out.println("ERROR: Authentication is null");
                model.addAttribute("error", "Authentication failed");
                return "parent-dashboard";
            }
            
            String username = auth.getName();
            System.out.println("Username from auth: " + username);
            
            User currentUser = userService.findByUsername(username)
                    .orElse(null);
            System.out.println("Current user lookup result: " + (currentUser != null ? "Found" : "NULL"));
            
            if (currentUser == null) {
                System.out.println("ERROR: User not found with username: " + username);
                model.addAttribute("error", "User not found");
                return "parent-dashboard";
            }

            System.out.println("Current parent: " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");

            // Verify parent has access to this child
            List<User> children = parentLinkService.getLinkedStudentsForParent(currentUser.getId());
            System.out.println("Found " + children.size() + " linked children");
            
            // Debug: Print all child IDs
            for (User child : children) {
                System.out.println("Linked child: " + child.getUsername() + " (ID: " + child.getId() + ")");
            }
            
            if (children.isEmpty()) {
                System.out.println("No children linked to parent " + currentUser.getId());
                model.addAttribute("error", "No children are linked to your account. Please contact the school administration to link your children.");
                return "parent-dashboard";
            }
            
            System.out.println("Looking for child ID: " + childUserId);
            User selectedChild = children.stream()
                    .filter(child -> {
                        System.out.println("Checking child ID: " + child.getId() + " against requested: " + childUserId);
                        return child.getId().equals(childUserId);
                    })
                    .findFirst()
                    .orElse(null);

            if (selectedChild == null) {
                System.out.println("ERROR: Child " + childUserId + " not found in linked children list");
                model.addAttribute("error", "Access denied: Child not linked to parent");
                return "parent-dashboard";
            }

            System.out.println("Selected child: " + selectedChild.getUsername() + " (ID: " + selectedChild.getId() + ")");

            model.addAttribute("parent", currentUser);
            model.addAttribute("child", selectedChild);
            model.addAttribute("childUser", selectedChild);
            model.addAttribute("jsChildUserId", selectedChild.getId()); // Simple primitive for JavaScript

            System.out.println("Added childUser to model with ID: " + selectedChild.getId());
            System.out.println("jsChildUserId set to: " + selectedChild.getId());
            System.out.println("About to return parent-child-detail template");
            
            // Debug: Check model attributes
            System.out.println("Model attributes being passed:");
            System.out.println("- parent: " + (model.asMap().containsKey("parent") ? "Present" : "Missing"));
            System.out.println("- child: " + (model.asMap().containsKey("child") ? "Present" : "Missing"));
            System.out.println("- childUser: " + (model.asMap().containsKey("childUser") ? "Present" : "Missing"));
            
            System.out.println("=== CHILD DETAIL METHOD END ===");

            return "parent-child-detail";
        } catch (Exception e) {
            System.err.println("=== EXCEPTION IN CHILD DETAIL METHOD ===");
            System.err.println("Error in childDetail method: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END EXCEPTION ===");
            model.addAttribute("error", "Error loading child details: " + e.getMessage());
            return "parent-dashboard";
        }
    }

    // API: Get children for current parent
    @GetMapping("/api/children")
    @ResponseBody
    public ResponseEntity<List<User>> getMyChildren() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("API: Getting children for parent ID: " + currentUser.getId());
            List<User> children = parentLinkService.getLinkedStudentsForParent(currentUser.getId());
            System.out.println("API: Found " + children.size() + " children");
            return ResponseEntity.ok(children);
        } catch (Exception e) {
            System.err.println("API Error getting children: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Debug endpoint to check parent-child links
    @GetMapping("/api/debug/links")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugLinks() {
        Map<String, Object> response = new HashMap<>();
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<User> children = parentLinkService.getLinkedStudentsForParent(currentUser.getId());
            
            response.put("parentId", currentUser.getId());
            response.put("parentUsername", currentUser.getUsername());
            response.put("childrenCount", children.size());
            response.put("children", children.stream().map(child -> Map.of(
                "id", child.getId(),
                "username", child.getUsername(),
                "firstName", child.getFirstName(),
                "lastName", child.getLastName()
            )).toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Simple test endpoint to verify template processing
    @GetMapping("/test-child/{childUserId}")
    public String testChildDetail(@PathVariable Long childUserId, Model model) {
        try {
            System.out.println("TEST: Loading child detail for ID: " + childUserId);
            
            // Get current parent user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get first available child (for testing)
            List<User> children = parentLinkService.getLinkedStudentsForParent(currentUser.getId());
            if (children.isEmpty()) {
                model.addAttribute("error", "No children found");
                return "parent-dashboard";
            }
            
            User testChild = children.get(0); // Use first child for testing
            System.out.println("TEST: Using child: " + testChild.getUsername() + " (ID: " + testChild.getId() + ")");
            
            model.addAttribute("parent", currentUser);
            model.addAttribute("child", testChild);
            model.addAttribute("childUser", testChild);
            model.addAttribute("jsChildUserId", testChild.getId()); // Simple primitive for JavaScript
            
            System.out.println("TEST: Added childUser to model with ID: " + testChild.getId());
            return "parent-child-detail";
        } catch (Exception e) {
            System.err.println("TEST Error: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Test error: " + e.getMessage());
            return "parent-dashboard";
        }
    }

    // Debug template to test model attributes
    @GetMapping("/debug-child/{childUserId}")
    public String debugChildDetail(@PathVariable Long childUserId, Model model) {
        try {
            System.out.println("DEBUG: Loading child detail for ID: " + childUserId);
            
            // Get current parent user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get first available child (for testing)
            List<User> children = parentLinkService.getLinkedStudentsForParent(currentUser.getId());
            if (children.isEmpty()) {
                model.addAttribute("error", "No children found");
                return "parent-dashboard";
            }
            
            User testChild = children.get(0); // Use first child for testing
            System.out.println("DEBUG: Using child: " + testChild.getUsername() + " (ID: " + testChild.getId() + ")");
            
            model.addAttribute("parent", currentUser);
            model.addAttribute("child", testChild);
            model.addAttribute("childUser", testChild);
            model.addAttribute("jsChildUserId", testChild.getId()); // Simple primitive for JavaScript
            
            System.out.println("DEBUG: Added childUser to model with ID: " + testChild.getId());
            return "debug-template"; // Return a simple debug template
        } catch (Exception e) {
            System.err.println("DEBUG Error: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Debug error: " + e.getMessage());
            return "parent-dashboard";
        }
    }

    // API: Get child's attendance history
    @GetMapping("/api/child/{childUserId}/attendance")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChildAttendance(
            @PathVariable Long childUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "attendanceDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status) {

        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Getting attendance for child ID: " + childUserId);
            
            // Get current parent user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("Current parent user: " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");

            // Verify parent has access to this child
            List<User> children = parentLinkService.getLinkedStudentsForParent(currentUser.getId());
            System.out.println("Found " + children.size() + " linked children for parent");
            
            boolean hasAccess = children.stream()
                    .anyMatch(child -> child.getId().equals(childUserId));
            
            if (!hasAccess) {
                System.out.println("Access denied: Child " + childUserId + " not linked to parent " + currentUser.getId());
                response.put("error", "Access denied: Child not linked to parent");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : null;
            LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : null;

            Page<Attendance> attendancePage = attendanceService.getStudentAttendanceHistory(childUserId, start, end, status, pageable);

            response.put("content", attendancePage.getContent());
            response.put("totalElements", attendancePage.getTotalElements());
            response.put("totalPages", attendancePage.getTotalPages());
            response.put("currentPage", attendancePage.getNumber());
            response.put("size", attendancePage.getSize());
            response.put("first", attendancePage.isFirst());
            response.put("last", attendancePage.isLast());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error retrieving attendance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API: Get child's attendance statistics
    @GetMapping("/api/child/{childUserId}/attendance/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChildAttendanceStatistics(@PathVariable Long childUserId) {
        try {
            System.out.println("Getting statistics for child ID: " + childUserId);
            
            // Get current parent user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("Current parent user for statistics: " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");

            // Verify parent has access to this child
            List<User> children = parentLinkService.getLinkedStudentsForParent(currentUser.getId());
            boolean hasAccess = children.stream()
                    .anyMatch(child -> child.getId().equals(childUserId));
            
            if (!hasAccess) {
                System.out.println("Access denied for statistics: Child " + childUserId + " not linked to parent " + currentUser.getId());
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Access denied: Child not linked to parent");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            Map<String, Long> statistics = attendanceService.getStudentAttendanceStatistics(childUserId);
            System.out.println("Retrieved statistics: " + statistics);
            return ResponseEntity.ok(Map.of("statistics", statistics));
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error retrieving statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Parent announcements page
    @GetMapping("/announcements")
    public String parentAnnouncements(Model model) {
        try {
            // Get current parent user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Fetch announcements for PARENT role
            var announcements = announcementService.getAnnouncementsForRole("PARENT");
            
            model.addAttribute("parent", currentUser);
            model.addAttribute("announcements", announcements);

            return "parent-announcements";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading announcements: " + e.getMessage());
            model.addAttribute("announcements", new ArrayList<>());
            return "parent-announcements";
        }
    }
}

