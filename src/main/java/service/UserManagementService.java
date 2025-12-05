package com.wsims.service;

import com.parami.wsims.dto.UserCreationDTO;
import com.parami.wsims.entity.*;
import com.parami.wsims.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserManagementService(UserRepository userRepository,
                               RoleRepository roleRepository,
                               StudentRepository studentRepository,
                               TeacherRepository teacherRepository,
                               ParentRepository parentRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.parentRepository = parentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(UserCreationDTO userData) {
        // Validate role exists
        Role role = roleRepository.findByName(userData.getRoleName())
                .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + userData.getRoleName()));

        // Create and save User entity
        User user = new User();
        user.setUsername(userData.getUsername());
        user.setPassword(passwordEncoder.encode(userData.getPassword()));
        user.setEmail(userData.getEmail());
        user.setFirstName(userData.getFirstName());
        user.setLastName(userData.getLastName());
        user.setPhone(userData.getPhone());
        user.setAddress(userData.getAddress());
        user.setRole(role);
        user.setIsActive(true); // Set user as active by default

        User savedUser = userRepository.save(user);

        // Create and save the specific role entity based on roleName
        switch (userData.getRoleName().toUpperCase()) {
            case "STUDENT":
                createStudent(savedUser, userData);
                break;
            case "TEACHER":
                createTeacher(savedUser, userData);
                break;
            case "PARENT":
                createParent(savedUser, userData);
                break;
            default:
                // For other roles (PRINCIPAL, STAFF, IT_ADMIN), we only create the User entity
                // No additional role-specific entity needed
                break;
        }

        return savedUser;
    }

    private void createStudent(User user, UserCreationDTO userData) {
        Student student = new Student();
        student.setUser(user);
        student.setGradeLevel(userData.getGradeLevel());
        student.setEmergencyContactPhone(userData.getEmergencyContactPhone());
        student.setEnrollmentDate(LocalDate.now()); // Set current date as enrollment date

        studentRepository.save(student);
    }

    private void createTeacher(User user, UserCreationDTO userData) {
        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setSpecialization(userData.getSpecialization());
        teacher.setDepartment(userData.getDepartment());
        teacher.setHireDate(LocalDate.now()); // Set current date as hire date

        teacherRepository.save(teacher);
    }

    private void createParent(User user, UserCreationDTO userData) {
        Parent parent = new Parent();
        parent.setUser(user);
        parent.setOccupation(userData.getOccupation());

        parentRepository.save(parent);
    }
}
