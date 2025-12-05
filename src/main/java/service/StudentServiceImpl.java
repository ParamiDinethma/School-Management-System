package com.wsims.service;

import com.parami.wsims.dto.UserCreationDTO;
import com.parami.wsims.entity.Role;
import com.parami.wsims.entity.Student;
import com.parami.wsims.entity.User;
import com.parami.wsims.repository.RoleRepository;
import com.parami.wsims.repository.StudentRepository;
import com.parami.wsims.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public StudentServiceImpl(StudentRepository studentRepository,
                             UserRepository userRepository,
                             RoleRepository roleRepository,
                             PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Student saveStudent(Student student) {
        return studentRepository.save(student);
    }

    @Override
    public Student updateStudent(Student student) {
        return studentRepository.save(student);
    }

    @Override
    public void deleteStudent(Long userId) {
        studentRepository.deleteById(userId);
    }

    @Override
    public Optional<Student> findById(Long userId) {
        return studentRepository.findById(userId);
    }

    @Override
    public Optional<Student> findByUserId(Long userId) {
        return studentRepository.findByUserId(userId);
    }

    @Override
    public List<Student> findAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public Page<Student> findStudentsWithPagination(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    @Override
    public Page<Student> searchStudents(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return studentRepository.findAll(pageable);
        }
        return studentRepository.findBySearchTerm(searchTerm.trim(), pageable);
    }

    @Override
    public Page<Student> findStudentsByGradeLevel(String gradeLevel, Pageable pageable) {
        if (gradeLevel == null || gradeLevel.trim().isEmpty() || "ALL".equals(gradeLevel)) {
            return studentRepository.findAll(pageable);
        }
        return studentRepository.findByGradeLevel(gradeLevel, pageable);
    }

    @Override
    public Page<Student> searchStudentsByGradeLevel(String gradeLevel, String searchTerm, Pageable pageable) {
        if ((gradeLevel == null || gradeLevel.trim().isEmpty() || "ALL".equals(gradeLevel)) && 
            (searchTerm == null || searchTerm.trim().isEmpty())) {
            return studentRepository.findAll(pageable);
        }
        
        if (gradeLevel == null || gradeLevel.trim().isEmpty() || "ALL".equals(gradeLevel)) {
            return searchStudents(searchTerm, pageable);
        }
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findStudentsByGradeLevel(gradeLevel, pageable);
        }
        
        return studentRepository.findByGradeLevelAndSearchTerm(gradeLevel, searchTerm.trim(), pageable);
    }

    @Override
    public Map<String, Long> getStudentStatistics() {
        Map<String, Long> stats = new HashMap<>();
        
        long totalStudents = getTotalStudentCount();
        long activeStudents = getActiveStudentCount();
        long inactiveStudents = getInactiveStudentCount();
        
        stats.put("total", totalStudents);
        stats.put("active", activeStudents);
        stats.put("inactive", inactiveStudents);
        
        return stats;
    }

    @Override
    public long getTotalStudentCount() {
        return studentRepository.countTotalStudents();
    }

    @Override
    public long getActiveStudentCount() {
        LocalDate cutoffDate = LocalDate.now().minusYears(1);
        return studentRepository.countActiveStudents(cutoffDate);
    }

    @Override
    public long getInactiveStudentCount() {
        LocalDate cutoffDate = LocalDate.now().minusYears(1);
        return studentRepository.countInactiveStudents(cutoffDate);
    }

    @Override
    public Student createStudentFromDTO(UserCreationDTO userData) {
        // Validate role exists
        Role role = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalArgumentException("STUDENT role not found"));

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

        User savedUser = userRepository.save(user);

        // Create and save Student entity
        Student student = new Student();
        student.setUser(savedUser);
        student.setGradeLevel(userData.getGradeLevel());
        student.setEmergencyContactPhone(userData.getEmergencyContactPhone());
        student.setEnrollmentDate(LocalDate.now());

        return studentRepository.save(student);
    }

    @Override
    public List<String> getAllGradeLevels() {
        // Common grade levels - you can modify this list as needed
        return Arrays.asList(
            "Pre-K", "Kindergarten", "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5",
            "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"
        );
    }
}
