package com.wsims.service;


import com.parami.wsims.entity.User;
import com.parami.wsims.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User saveUser(User user) {
        // Encode password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public User updateUser(User user) {
        // If password is being updated, encode it
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Page<User> findUsersWithPagination(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findBySearchTerm(searchTerm.trim(), pageable);
    }

    @Override
    public Page<User> findUsersByRole(String roleName, Pageable pageable) {
        if (roleName == null || roleName.trim().isEmpty() || "ALL".equals(roleName)) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByRoleName(roleName, pageable);
    }

    @Override
    public Page<User> searchUsersByRole(String roleName, String searchTerm, Pageable pageable) {
        if ((roleName == null || roleName.trim().isEmpty() || "ALL".equals(roleName)) && 
            (searchTerm == null || searchTerm.trim().isEmpty())) {
            return userRepository.findAll(pageable);
        }
        
        if (roleName == null || roleName.trim().isEmpty() || "ALL".equals(roleName)) {
            return searchUsers(searchTerm, pageable);
        }
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findUsersByRole(roleName, pageable);
        }
        
        return userRepository.findByRoleAndSearchTerm(roleName, searchTerm.trim(), pageable);
    }

    @Override
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmailAndIdNot(email, -1L);
    }

    @Override
    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsernameAndIdNot(username, -1L);
    }

    @Override
    public boolean isEmailExistsForOtherUser(String email, Long userId) {
        return userRepository.existsByEmailAndIdNot(email, userId);
    }

    @Override
    public boolean isUsernameExistsForOtherUser(String username, Long userId) {
        return userRepository.existsByUsernameAndIdNot(username, userId);
    }
    
    @Override
    public List<User> findStudentsForTeacher(Long teacherId) {
        return userRepository.findStudentsForTeacher(teacherId);
    }
    
    @Override
    public List<User> findParentsByStudentId(Long studentId) {
        return userRepository.findParentsByStudentId(studentId);
    }
    
    @Override
    public List<User> findByRoleName(String roleName) {
        return userRepository.findByRoleName(roleName);
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
    
    @Override
    public List<User> getAllStudents() {
        return userRepository.findByRoleName("STUDENT");
    }
    
    @Override
    public List<User> getStudentsByGradeLevel(String gradeLevel) {
        // Return all students since gradeLevel field doesn't exist in User entity
        return userRepository.findByRoleName("STUDENT");
    }
}