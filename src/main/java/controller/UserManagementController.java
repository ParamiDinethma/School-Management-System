package com.wsims.controller;

import com.parami.wsims.entity.Role;
import com.parami.wsims.entity.User;
import com.parami.wsims.repository.RoleRepository;
import com.parami.wsims.service.UserService;
import com.parami.wsims.service.UserManagementService;
import com.parami.wsims.dto.UserCreationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final UserManagementService userManagementService;

    @Autowired
    public UserManagementController(UserService userService, RoleRepository roleRepository, UserManagementService userManagementService) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.userManagementService = userManagementService;
    }

    // Display the user management page
    @GetMapping
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    public String userManagementPage() {
        return "user-management";
    }

    // Get all users with pagination and search
    @GetMapping("/api")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<User> users;
            if ((search != null && !search.trim().isEmpty()) && 
                (role != null && !role.trim().isEmpty() && !role.equals("ALL"))) {
                users = userService.searchUsersByRole(role, search, pageable);
            } else if (search != null && !search.trim().isEmpty()) {
                users = userService.searchUsers(search, pageable);
            } else if (role != null && !role.trim().isEmpty() && !role.equals("ALL")) {
                users = userService.findUsersByRole(role, pageable);
            } else {
                users = userService.findUsersWithPagination(pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("users", users.getContent());
            response.put("currentPage", users.getNumber());
            response.put("totalItems", users.getTotalElements());
            response.put("totalPages", users.getTotalPages());
            response.put("pageSize", users.getSize());
            response.put("hasNext", users.hasNext());
            response.put("hasPrevious", users.hasPrevious());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Get user by ID
    @GetMapping("/api/{id}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    // Create new user
    @PostMapping("/api")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> userData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create UserCreationDTO from the request data
            UserCreationDTO userCreationDTO = new UserCreationDTO();
            userCreationDTO.setUsername((String) userData.get("username"));
            userCreationDTO.setPassword((String) userData.get("password"));
            userCreationDTO.setEmail((String) userData.get("email"));
            userCreationDTO.setFirstName((String) userData.get("firstName"));
            userCreationDTO.setLastName((String) userData.get("lastName"));
            userCreationDTO.setPhone((String) userData.get("phone"));
            userCreationDTO.setAddress((String) userData.get("address"));
            
            // Get role name
            @SuppressWarnings("unchecked")
            Map<String, Object> roleData = (Map<String, Object>) userData.get("role");
            if (roleData != null) {
                userCreationDTO.setRoleName((String) roleData.get("name"));
            }
            
            // Add role-specific fields
            userCreationDTO.setGradeLevel((String) userData.get("gradeLevel"));
            userCreationDTO.setEmergencyContactPhone((String) userData.get("emergencyContactPhone"));
            userCreationDTO.setSpecialization((String) userData.get("specialization"));
            userCreationDTO.setDepartment((String) userData.get("department"));
            userCreationDTO.setOccupation((String) userData.get("occupation"));

            User savedUser = userManagementService.createUser(userCreationDTO);
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("user", savedUser);
            return ResponseEntity.ok(response);
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

    // Update user
    @PutMapping("/api/{id}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<User> existingUser = userService.findById(id);
            if (!existingUser.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }

            // Validate email uniqueness (excluding current user)
            if (userService.isEmailExistsForOtherUser(user.getEmail(), id)) {
                response.put("success", false);
                response.put("message", "Email already exists");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate username uniqueness (excluding current user)
            if (userService.isUsernameExistsForOtherUser(user.getUsername(), id)) {
                response.put("success", false);
                response.put("message", "Username already exists");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate required fields
            if (user.getFirstName() == null || user.getFirstName().trim().isEmpty() ||
                user.getLastName() == null || user.getLastName().trim().isEmpty() ||
                user.getEmail() == null || user.getEmail().trim().isEmpty() ||
                user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "All required fields must be filled");
                return ResponseEntity.badRequest().body(response);
            }

            // Get the existing user and update its fields
            User userToUpdate = existingUser.get();
            
            // Update the fields
            userToUpdate.setFirstName(user.getFirstName());
            userToUpdate.setLastName(user.getLastName());
            userToUpdate.setEmail(user.getEmail());
            userToUpdate.setUsername(user.getUsername());
            userToUpdate.setPhone(user.getPhone());
            userToUpdate.setAddress(user.getAddress());
            
            // Handle password update
            if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
                userToUpdate.setPassword(user.getPassword()); // Will be encoded in service
            }
            
            // Handle role update - load the role from database
            if (user.getRole() != null && user.getRole().getName() != null) {
                Optional<Role> role = roleRepository.findByName(user.getRole().getName());
                if (role.isPresent()) {
                    userToUpdate.setRole(role.get());
                } else {
                    response.put("success", false);
                    response.put("message", "Invalid role specified");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            User updatedUser = userService.updateUser(userToUpdate);
            response.put("success", true);
            response.put("message", "User updated successfully");
            response.put("user", updatedUser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Delete user
    @DeleteMapping("/api/{id}")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<User> user = userService.findById(id);
            if (!user.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }

            userService.deleteUser(id);
            response.put("success", true);
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get all roles
    @GetMapping("/api/roles")
    @PreAuthorize("hasAuthority('PRINCIPAL') or hasAuthority('IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return ResponseEntity.ok(roles);
    }
}
