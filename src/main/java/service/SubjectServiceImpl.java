package com.wsims.service;

import com.parami.wsims.entity.Subject;
import com.parami.wsims.repository.SubjectRepository;
import com.parami.wsims.repository.CourseSubjectLinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseSubjectLinkRepository courseSubjectLinkRepository;

    @Autowired
    public SubjectServiceImpl(SubjectRepository subjectRepository,
                            CourseSubjectLinkRepository courseSubjectLinkRepository) {
        this.subjectRepository = subjectRepository;
        this.courseSubjectLinkRepository = courseSubjectLinkRepository;
    }

    @Override
    public Subject saveSubject(Subject subject) {
        return subjectRepository.save(subject);
    }

    @Override
    public Subject updateSubject(Subject subject) {
        return subjectRepository.save(subject);
    }

    @Override
    public void deleteSubject(Long id) {
        // First, check if subject exists
        Optional<Subject> subject = subjectRepository.findById(id);
        if (!subject.isPresent()) {
            throw new IllegalArgumentException("Subject not found with id: " + id);
        }
        
        // Delete all course-subject links first to avoid foreign key constraints
        List<com.parami.wsims.entity.CourseSubjectLink> courseSubjectLinks =
            courseSubjectLinkRepository.findBySubjectId(id);
        if (!courseSubjectLinks.isEmpty()) {
            courseSubjectLinkRepository.deleteAll(courseSubjectLinks);
        }
        
        // Now delete the subject
        subjectRepository.deleteById(id);
    }

    @Override
    public Optional<Subject> findById(Long id) {
        return subjectRepository.findById(id);
    }

    @Override
    public Subject getSubjectById(Long id) {
        return subjectRepository.findById(id).orElse(null);
    }

    @Override
    public List<Subject> findAllSubjects() {
        return subjectRepository.findAll();
    }

    @Override
    public Page<Subject> findSubjectsWithPagination(Pageable pageable) {
        System.out.println("SubjectServiceImpl.findSubjectsWithPagination() called with pageable: " + pageable);
        Page<Subject> result = subjectRepository.findAll(pageable);
        System.out.println("Found " + result.getTotalElements() + " total subjects, returning " + result.getContent().size() + " for page " + result.getNumber());
        return result;
    }

    @Override
    public Page<Subject> searchSubjects(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return subjectRepository.findAll(pageable);
        }
        return subjectRepository.findBySearchTerm(searchTerm.trim(), pageable);
    }

    @Override
    public Page<Subject> findActiveSubjects(Pageable pageable) {
        return subjectRepository.findByIsActiveTrue(pageable);
    }

    @Override
    public Page<Subject> searchActiveSubjects(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return subjectRepository.findByIsActiveTrue(pageable);
        }
        return subjectRepository.findActiveBySearchTerm(searchTerm.trim(), pageable);
    }

    @Override
    public long getTotalSubjectCount() {
        return subjectRepository.count();
    }

    @Override
    public long getActiveSubjectCount() {
        return subjectRepository.findAll().stream()
                .filter(subject -> subject.getIsActive() != null && subject.getIsActive())
                .count();
    }

    @Override
    public long getInactiveSubjectCount() {
        return subjectRepository.findAll().stream()
                .filter(subject -> subject.getIsActive() == null || !subject.getIsActive())
                .count();
    }

    @Override
    public boolean isSubjectCodeExists(String subjectCode) {
        return subjectRepository.existsBySubjectCode(subjectCode);
    }

    @Override
    public boolean isSubjectCodeExistsForOtherSubject(String subjectCode, Long subjectId) {
        return subjectRepository.existsBySubjectCodeAndIdNot(subjectCode, subjectId);
    }

    @Override
    public List<Subject> getAvailableSubjectsForCourse(Long courseId) {
        List<Subject> allSubjects = subjectRepository.findAll();
        List<Subject> assignedSubjects = courseSubjectLinkRepository.findSubjectsByCourseId(courseId);
        
        return allSubjects.stream()
                .filter(subject -> !assignedSubjects.contains(subject))
                .toList();
    }
}
