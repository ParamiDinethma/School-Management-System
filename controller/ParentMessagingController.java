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
 * Controller for parent messaging functionality
 */
@Controller
@RequestMapping("/parent/messages")
public class ParentMessagingController {

    private final MessageService messageService;
    private final UserService userService;

    @Autowired
    public ParentMessagingController(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    /**
     * Display parent messaging inbox
     */
    @GetMapping
    public String parentMessagingPage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("currentUser", currentUser);
        return "parent-messaging";
    }

    /**
     * Get recent conversations for parent
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
     * Get conversation between parent and teacher
     */
    @GetMapping("/api/conversation/{teacherId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getConversation(@PathVariable Long teacherId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new IllegalArgumentException("User not authenticated");
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Message> conversation = messageService.getConversation(currentUser.getId(), teacherId, pageable);
            
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
     * Send a reply message to teacher
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
     * Get new messages in a conversation after a specific message ID
     */
    @GetMapping("/api/conversation/{teacherId}/new")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNewMessages(@PathVariable Long teacherId,
                                                              @RequestParam Long afterMessageId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new IllegalArgumentException("User not authenticated");
            }

            List<Message> newMessages = messageService.getNewMessages(currentUser.getId(), teacherId, afterMessageId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", newMessages);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching new messages: " + e.getMessage());
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
