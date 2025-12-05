package com.wsims.config;

import com.parami.wsims.entity.Role;
import com.parami.wsims.entity.User;
import com.parami.wsims.repository.RoleRepository;
import com.parami.wsims.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeUsers();
    }

    private void initializeRoles() {
        // Create roles if they don't exist
        createRoleIfNotExists("PRINCIPAL");
        createRoleIfNotExists("TEACHER");
        createRoleIfNotExists("STUDENT");
        createRoleIfNotExists("STAFF");
        createRoleIfNotExists("IT_ADMIN");
    }

    private void createRoleIfNotExists(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
            System.out.println("Created role: " + roleName);
        }
    }

    private void initializeUsers() {
        // Create Principal user if not exists
        createUserIfNotExists("principal", "principal@school.com", "Principal", "User", "PRINCIPAL", "password");
        
        // Create IT Admin user if not exists
        createUserIfNotExists("admin", "admin@school.com", "IT", "Admin", "IT_ADMIN", "password");
        
        // Create sample teacher
        createUserIfNotExists("teacher1", "teacher1@school.com", "John", "Smith", "TEACHER", "password");
        
        // Create sample student
        createUserIfNotExists("student1", "student1@school.com", "Alice", "Johnson", "STUDENT", "password");
        
        // Create sample staff
        createUserIfNotExists("staff1", "staff1@school.com", "Bob", "Wilson", "STAFF", "password");
    }

    private void createUserIfNotExists(String username, String email, String firstName, String lastName, 
                                     String roleName, String password) {
        if (userRepository.findByUsername(username).isEmpty()) {
            Role role = roleRepository.findByName(roleName).orElseThrow();
            
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setPhone("+1234567890");
            user.setAddress("123 School Street, Education City");
            
            userRepository.save(user);
            System.out.println("Created user: " + username + " (" + roleName + ")");
        }
    }
}
