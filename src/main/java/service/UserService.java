package com.wsims.service;


import com.parami.wsims.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);
    
    // CRUD Operations
    User saveUser(User user);
    User updateUser(User user);
    void deleteUser(Long id);
    Optional<User> findById(Long id);
    List<User> findAllUsers();
    
    // Search and Pagination
    Page<User> findUsersWithPagination(Pageable pageable);
    Page<User> searchUsers(String searchTerm, Pageable pageable);
    Page<User> findUsersByRole(String roleName, Pageable pageable);
    Page<User> searchUsersByRole(String roleName, String searchTerm, Pageable pageable);
    
    // Validation
    boolean isEmailExists(String email);
    boolean isUsernameExists(String username);
    boolean isEmailExistsForOtherUser(String email, Long userId);
    boolean isUsernameExistsForOtherUser(String username, Long userId);
    
    // Messaging-related methods
    List<User> findStudentsForTeacher(Long teacherId);
    List<User> findParentsByStudentId(Long studentId);
    List<User> findByRoleName(String roleName);
    User getUserById(Long userId);
    
    // Report generation methods
    List<User> getAllStudents();
    List<User> getStudentsByGradeLevel(String gradeLevel);
}