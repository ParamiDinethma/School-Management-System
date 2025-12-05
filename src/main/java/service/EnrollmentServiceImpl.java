package com.wsims.service;

import com.parami.wsims.entity.Enrollment;
import com.parami.wsims.entity.User;
import com.parami.wsims.entity.Course;
import com.parami.wsims.repository.EnrollmentRepository;
import com.parami.wsims.repository.UserRepository;
import com.parami.wsims.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepository,
                               UserRepository userRepository,
                               CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public Enrollment saveEnrollment(Enrollment enrollment) {
        return enrollmentRepository.save(enrollment);
    }

    @Override
    public Enrollment updateEnrollment(Enrollment enrollment) {
        return enrollmentRepository.save(enrollment);
    }

    @Override
    public void deleteEnrollment(Long id) {
        enrollmentRepository.deleteById(id);
    }

    @Override
    public Optional<Enrollment> findById(Long id) {
        return enrollmentRepository.findById(id);
    }

    @Override
    public List<Enrollment> findAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    @Override
    public List<Enrollment> findEnrollmentsByStudent(User student) {
        return enrollmentRepository.findByStudent(student);
    }

    @Override
    public Page<Enrollment> findEnrollmentsByStudent(User student, Pageable pageable) {
        return enrollmentRepository.findByStudent(student, pageable);
    }

    @Override
    public List<Course> getAvailableCoursesForStudent(Long studentId) {
        return enrollmentRepository.findAvailableCoursesForStudent(studentId);
    }

    @Override
    public Enrollment enrollStudentInCourse(Long studentId, Long courseId, String remarks) {
        // Check if student exists
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));
        
        // Check if course exists
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + courseId));
        
        // Check if already enrolled
        if (enrollmentRepository.existsByStudentAndCourse(student, course)) {
            throw new IllegalArgumentException("Student is already enrolled in this course");
        }
        
        // Check if course is active
        if (!course.getIsActive()) {
            throw new IllegalArgumentException("Course is not active");
        }
        
        // Create enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus("ACTIVE");
        enrollment.setRemarks(remarks);
        enrollment.setEnrollmentDate(LocalDate.now());
        
        return enrollmentRepository.save(enrollment);
    }

    @Override
    public void unenrollStudentFromCourse(Long studentId, Long courseId) {
        Optional<Enrollment> enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (enrollment.isPresent()) {
            enrollmentRepository.delete(enrollment.get());
        } else {
            throw new IllegalArgumentException("Enrollment not found");
        }
    }

    @Override
    public boolean isStudentEnrolledInCourse(Long studentId, Long courseId) {
        return enrollmentRepository.isStudentEnrolledInCourse(studentId, courseId);
    }

    @Override
    public List<Enrollment> findEnrollmentsByCourse(Course course) {
        return enrollmentRepository.findByCourse(course);
    }

    @Override
    public Page<Enrollment> findEnrollmentsByCourse(Course course, Pageable pageable) {
        return enrollmentRepository.findByCourse(course, pageable);
    }

    @Override
    public List<Enrollment> findActiveEnrollmentsByCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + courseId));
        return enrollmentRepository.findByStatusAndCourse("ACTIVE", course);
    }

    @Override
    public List<User> getStudentsByCourse(Long courseId) {
        return findActiveEnrollmentsByCourse(courseId)
                .stream()
                .map(Enrollment::getStudent)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Enrollment> findEnrollmentsByStatus(String status) {
        return enrollmentRepository.findByStatus(status);
    }

    @Override
    public Page<Enrollment> findEnrollmentsByStatus(String status, Pageable pageable) {
        return enrollmentRepository.findByStatus(status, pageable);
    }

    @Override
    public Enrollment updateEnrollmentStatus(Long enrollmentId, String status, String remarks) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found with id: " + enrollmentId));
        
        if (!isValidEnrollmentStatus(status)) {
            throw new IllegalArgumentException("Invalid enrollment status: " + status);
        }
        
        enrollment.setStatus(status);
        enrollment.setRemarks(remarks);
        
        return enrollmentRepository.save(enrollment);
    }

    @Override
    public Enrollment updateEnrollmentGrade(Long enrollmentId, String grade) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found with id: " + enrollmentId));
        
        if (grade != null && !isValidGrade(grade)) {
            throw new IllegalArgumentException("Invalid grade: " + grade);
        }
        
        enrollment.setGrade(grade);
        
        return enrollmentRepository.save(enrollment);
    }

    @Override
    public Page<Enrollment> findEnrollmentsWithPagination(Pageable pageable) {
        return enrollmentRepository.findAll(pageable);
    }

    @Override
    public Page<Enrollment> searchEnrollments(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return enrollmentRepository.findAll(pageable);
        }
        return enrollmentRepository.findBySearchTerm(searchTerm.trim(), pageable);
    }

    @Override
    public List<Enrollment> findEnrollmentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return enrollmentRepository.findByEnrollmentDateBetween(startDate, endDate);
    }

    @Override
    public long getTotalEnrollmentCount() {
        return enrollmentRepository.count();
    }

    @Override
    public long getActiveEnrollmentCount() {
        return enrollmentRepository.countByStatus("ACTIVE");
    }

    @Override
    public long getEnrollmentCountByStatus(String status) {
        return enrollmentRepository.countByStatus(status);
    }

    @Override
    public long getEnrollmentCountByCourse(Long courseId) {
        return enrollmentRepository.countByCourseIdAndStatus(courseId, "ACTIVE");
    }

    @Override
    public long getEnrollmentCountByCourseAndStatus(Long courseId, String status) {
        return enrollmentRepository.countByCourseIdAndStatus(courseId, status);
    }

    @Override
    public boolean canStudentEnrollInCourse(Long studentId, Long courseId) {
        // Check if student exists and is active
        Optional<User> student = userRepository.findById(studentId);
        if (!student.isPresent() || !student.get().getIsActive()) {
            return false;
        }
        
        // Check if course exists and is active
        Optional<Course> course = courseRepository.findById(courseId);
        if (!course.isPresent() || !course.get().getIsActive()) {
            return false;
        }
        
        // Check if not already enrolled
        return !isStudentEnrolledInCourse(studentId, courseId);
    }

    @Override
    public boolean isValidEnrollmentStatus(String status) {
        return status != null && (status.equals("ACTIVE") || status.equals("COMPLETED") || 
                                 status.equals("DROPPED") || status.equals("SUSPENDED"));
    }

    @Override
    public boolean isValidGrade(String grade) {
        return grade == null || grade.equals("A") || grade.equals("B") || grade.equals("C") || 
               grade.equals("D") || grade.equals("F") || grade.equals("P") || grade.equals("NP");
    }
}
