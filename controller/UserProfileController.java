package com.wsims.controller;

import com.parami.wsims.entity.User;
import com.parami.wsims.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserService userService;

    @Autowired
    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String username = auth.getName();
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId());
        dto.put("userId", user.getId());
        dto.put("username", user.getUsername());
        dto.put("firstName", user.getFirstName());
        dto.put("lastName", user.getLastName());
        dto.put("email", user.getEmail());
        dto.put("phone", user.getPhone());
        dto.put("address", user.getAddress());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile() {
        return getCurrentUser();
    }

    @org.springframework.web.bind.annotation.PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@org.springframework.web.bind.annotation.RequestBody Map<String, Object> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String username = auth.getName();
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        if (payload.containsKey("firstName")) user.setFirstName(String.valueOf(payload.get("firstName")));
        if (payload.containsKey("lastName")) user.setLastName(String.valueOf(payload.get("lastName")));
        if (payload.containsKey("email")) user.setEmail(String.valueOf(payload.get("email")));
        if (payload.containsKey("phone")) user.setPhone(String.valueOf(payload.get("phone")));
        if (payload.containsKey("address")) user.setAddress(String.valueOf(payload.get("address")));

        userService.updateUser(user);

        Map<String, Object> dto = new HashMap<>();
        dto.put("success", true);
        dto.put("message", "Profile updated");
        return ResponseEntity.ok(dto);
    }
}


