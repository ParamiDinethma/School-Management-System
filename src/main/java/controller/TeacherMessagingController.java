package com.wsims.controller;

import com.parami.wsims.entity.Message;
import com.parami.wsims.entity.User;
import com.parami.wsims.service.MessageService;
import com.parami.wsims.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for teacher messaging functionality
 */
@Controller
@RequestMapping("/teacher/messages")
public class TeacherMessagingController {

    private final MessageService messageService;
    private final UserService userService;

    @Autowired
    public TeacherMessagingController(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    /**
     * Display teacher messaging interface
     */
    @GetMapping
    public String teacherMessagingPage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Get students for this teacher (through enrollments and course-subject links)
        List<User> students = userService.findStudentsForTeacher(currentUser.getId());
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("students", students);
        return "teacher-messaging";
    }

    /**
     * Get all students for the current teacher
     */
    @GetMapping("/api/students")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStudents() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new IllegalArgumentException("User not authenticated");
            }

            List<User> students = userService.findStudentsForTeacher(currentUser.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("students", students);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching students: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get parents for a specific student
     */
    @GetMapping("/api/students/{studentId}/parents")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getParentsForStudent(@PathVariable Long studentId) {
        try {
            List<User> parents = userService.findParentsByStudentId(studentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("parents", parents);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching parents: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get conversation between teacher and parent
     */
    @GetMapping("/api/conversation/{parentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getConversation(@PathVariable Long parentId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new IllegalArgumentException("User not authenticated");
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Message> conversation = messageService.getConversation(currentUser.getId(), parentId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("conversation", conversation.getContent());
            response.put("totalPages", conversation.getTotalPages());
            response.put("currentPage", conversation.getNumber());
            response.put("totalElements", conversation.getTotalElements());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching conversation: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Send a message to parent
     */
    @PostMapping("/api/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new IllegalArgumentException("User not authenticated");
            }

            Long recipientId = Long.valueOf(request.get("recipientId").toString());
            String content = request.get("content").toString();

            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("Message content cannot be empty");
            }

            Optional<User> recipientOpt = userService.findById(recipientId);
            if (recipientOpt.isEmpty()) {
                throw new IllegalArgumentException("Recipient not found");
            }

            Message message = messageService.sendMessage(currentUser, recipientOpt.get(), content.trim());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Message sent successfully");
            response.put("messageId", message.getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error sending message: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    /**
     * Delete a message
     */
    @DeleteMapping("/api/message/{messageId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteMessage(@PathVariable Long messageId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new IllegalArgumentException("User not authenticated");
            }

            messageService.deleteMessage(messageId, currentUser.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Message deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting message: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    /**
     * Get recent conversations
     */
    @GetMapping("/api/conversations")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRecentConversations(@RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "10") int size) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new IllegalArgumentException("User not authenticated");
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Message> conversations = messageService.getRecentConversations(currentUser.getId(), pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("conversations", conversations.getContent());
            response.put("totalPages", conversations.getTotalPages());
            response.put("currentPage", conversations.getNumber());
            response.put("totalElements", conversations.getTotalElements());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching conversations: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            return userService.findByUsername(username).orElse(null);
        }
        return null;
    }
}
