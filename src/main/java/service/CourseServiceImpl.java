package com.wsims.service;

import com.parami.wsims.entity.Course;
import com.parami.wsims.entity.CourseSubjectLink;
import com.parami.wsims.entity.Subject;
import com.parami.wsims.repository.CourseRepository;
import com.parami.wsims.repository.CourseSubjectLinkRepository;
import com.parami.wsims.repository.SubjectRepository;
import com.parami.wsims.strategy.CourseValidationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseSubjectLinkRepository courseSubjectLinkRepository;
    private final SubjectRepository subjectRepository;
    private final CourseValidationContext validationContext;

    @Autowired
    public CourseServiceImpl(CourseRepository courseRepository,
                           CourseSubjectLinkRepository courseSubjectLinkRepository,
                           SubjectRepository subjectRepository,
                           CourseValidationContext validationContext) {
        this.courseRepository = courseRepository;
        this.courseSubjectLinkRepository = courseSubjectLinkRepository;
        this.subjectRepository = subjectRepository;
        this.validationContext = validationContext;
    }

    @Override
    public Course saveCourse(Course course) {
        // Apply strategy-based validation
        Map<String, Object> validationResult = validationContext.validateCourse(course);
        
        if (!(Boolean) validationResult.get("success")) {
            throw new IllegalArgumentException(validationResult.get("message").toString());
        }
        
        return courseRepository.save(course);
    }

    @Override
    public Course updateCourse(Course course) {
        // Apply strategy-based validation
        Map<String, Object> validationResult = validationContext.validateCourse(course);
        
        if (!(Boolean) validationResult.get("success")) {
            throw new IllegalArgumentException(validationResult.get("message").toString());
        }
        
        return courseRepository.save(course);
    }

    @Override
    public void deleteCourse(Long id) {
        // First, check if course exists
        Optional<Course> course = courseRepository.findById(id);
        if (!course.isPresent()) {
            throw new IllegalArgumentException("Course not found with id: " + id);
        }
        
        // Delete all course-subject links first to avoid foreign key constraints
        List<CourseSubjectLink> courseSubjectLinks = courseSubjectLinkRepository.findByCourseId(id);
        if (!courseSubjectLinks.isEmpty()) {
            courseSubjectLinkRepository.deleteAll(courseSubjectLinks);
        }
        
        // Now delete the course
        courseRepository.deleteById(id);
    }

    @Override
    public Optional<Course> findById(Long id) {
        return courseRepository.findById(id);
    }

    @Override
    public List<Course> findAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    public Page<Course> findCoursesWithPagination(Pageable pageable) {
        return courseRepository.findAll(pageable);
    }

    @Override
    public Page<Course> searchCourses(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return courseRepository.findAll(pageable);
        }
        String formattedSearchTerm = "%" + searchTerm.trim() + "%";
        return courseRepository.findBySearchTerm(formattedSearchTerm, pageable);
    }

    @Override
    public Page<Course> findActiveCourses(Pageable pageable) {
        return courseRepository.findByIsActiveTrue(pageable);
    }

    @Override
    public Page<Course> searchActiveCourses(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return courseRepository.findByIsActiveTrue(pageable);
        }
        String formattedSearchTerm = "%" + searchTerm.trim() + "%";
        return courseRepository.findActiveBySearchTerm(formattedSearchTerm, pageable);
    }

    @Override
    public void assignSubjectToCourse(Long courseId, Long subjectId, Boolean isMandatory, String gradeLevel) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + courseId));
        
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found with id: " + subjectId));
        
        // Check if link already exists
        if (courseSubjectLinkRepository.existsByCourseIdAndSubjectId(courseId, subjectId)) {
            throw new IllegalArgumentException("Subject is already assigned to this course");
        }
        
        CourseSubjectLink link = new CourseSubjectLink();
        link.setCourse(course);
        link.setSubject(subject);
        link.setIsMandatory(isMandatory != null ? isMandatory : true);
        link.setGradeLevel(gradeLevel != null ? gradeLevel : "Grade 1");
        
        courseSubjectLinkRepository.save(link);
    }

    @Override
    public void removeSubjectFromCourse(Long courseId, Long subjectId) {
        Optional<CourseSubjectLink> link = courseSubjectLinkRepository.findByCourseIdAndSubjectId(courseId, subjectId);
        if (link.isPresent()) {
            courseSubjectLinkRepository.delete(link.get());
        } else {
            throw new IllegalArgumentException("Subject is not assigned to this course");
        }
    }

    @Override
    public List<Subject> getSubjectsByCourseId(Long courseId) {
        return courseSubjectLinkRepository.findSubjectsByCourseId(courseId);
    }

    @Override
    public List<Subject> getSubjectsByCourse(Long courseId) {
        return getSubjectsByCourseId(courseId);
    }

    @Override
    public List<Course> getCoursesByTeacher(Long teacherId) {
        // For now, return all courses - in a real implementation, this would filter by teacher
        return courseRepository.findAll();
    }

    @Override
    public Course getCourseById(Long courseId) {
        return courseRepository.findById(courseId).orElse(null);
    }

    @Override
    public List<Subject> getMandatorySubjectsByCourseId(Long courseId) {
        return courseSubjectLinkRepository.findMandatoryByCourseId(courseId)
                .stream()
                .map(CourseSubjectLink::getSubject)
                .toList();
    }

    @Override
    public List<Subject> getOptionalSubjectsByCourseId(Long courseId) {
        return courseSubjectLinkRepository.findOptionalByCourseId(courseId)
                .stream()
                .map(CourseSubjectLink::getSubject)
                .toList();
    }

    @Override
    public long getTotalCourseCount() {
        return courseRepository.count();
    }

    @Override
    public long getActiveCourseCount() {
        return courseRepository.findAll().stream()
                .filter(course -> course.getIsActive() != null && course.getIsActive())
                .count();
    }

    @Override
    public long getInactiveCourseCount() {
        return courseRepository.findAll().stream()
                .filter(course -> course.getIsActive() == null || !course.getIsActive())
                .count();
    }

    @Override
    public boolean isCourseCodeExists(String courseCode) {
        return courseRepository.existsByCourseCode(courseCode);
    }

    @Override
    public boolean isCourseCodeExistsForOtherCourse(String courseCode, Long courseId) {
        return courseRepository.existsByCourseCodeAndIdNot(courseCode, courseId);
    }
    
    @Override
    public Map<String, Object> validateCourse(Course course) {
        return validationContext.validateCourse(course);
    }
}
