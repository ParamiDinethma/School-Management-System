package com.wsims.controller;

import com.parami.wsims.dto.UserCreationDTO;
import com.parami.wsims.entity.User;
import com.parami.wsims.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
public class UserCreationController {

    private final UserManagementService userManagementService;

    @Autowired
    public UserCreationController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody UserCreationDTO userData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create the user and associated role entity
            User createdUser = userManagementService.createUser(userData);
            
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("userId", createdUser.getId());
            response.put("username", createdUser.getUsername());
            response.put("email", createdUser.getEmail());
            response.put("role", createdUser.getRole().getName());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
