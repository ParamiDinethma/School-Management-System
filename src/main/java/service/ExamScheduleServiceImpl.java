package com.wsims.service;

import com.parami.wsims.entity.ExamSchedule;
import com.parami.wsims.entity.User;
import com.parami.wsims.repository.ExamScheduleRepository;
import com.parami.wsims.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ExamScheduleServiceImpl implements ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;
    private final UserRepository userRepository;

    @Autowired
    public ExamScheduleServiceImpl(ExamScheduleRepository examScheduleRepository, UserRepository userRepository) {
        this.examScheduleRepository = examScheduleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ExamSchedule createExamSchedule(ExamSchedule examSchedule) {
        // Validate exam schedule
        if (!isExamScheduleValid(examSchedule)) {
            throw new IllegalArgumentException("Invalid exam schedule data");
        }

        // Check for date conflicts
        if (hasDateConflicts(examSchedule)) {
            throw new IllegalArgumentException("Exam schedule conflicts with existing schedules");
        }

        // Set created by user
        User currentUser = getCurrentUser();
        examSchedule.setCreatedBy(currentUser);
        
        // Set default status if not provided
        if (examSchedule.getStatus() == null) {
            examSchedule.setStatus(ExamSchedule.ExamStatus.ACTIVE);
        }

        return examScheduleRepository.save(examSchedule);
    }

    @Override
    public ExamSchedule updateExamSchedule(Long id, ExamSchedule examSchedule) {
        ExamSchedule existingExam = examScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Exam schedule not found with id: " + id));

        // Validate exam schedule
        if (!isExamScheduleValid(examSchedule)) {
            throw new IllegalArgumentException("Invalid exam schedule data");
        }

        // Check for date conflicts (excluding current exam)
        ExamSchedule tempExam = new ExamSchedule();
        tempExam.setStartDate(examSchedule.getStartDate());
        tempExam.setEndDate(examSchedule.getEndDate());
        
        List<ExamSchedule> conflicts = findOverlappingExamSchedules(tempExam);
        if (!conflicts.isEmpty() && !conflicts.stream().anyMatch(c -> c.getId().equals(id))) {
            throw new IllegalArgumentException("Exam schedule conflicts with existing schedules");
        }

        // Update fields
        existingExam.setExamName(examSchedule.getExamName());
        existingExam.setDescription(examSchedule.getDescription());
        existingExam.setStartDate(examSchedule.getStartDate());
        existingExam.setEndDate(examSchedule.getEndDate());
        existingExam.setAcademicYear(examSchedule.getAcademicYear());
        existingExam.setSemester(examSchedule.getSemester());
        existingExam.setStatus(examSchedule.getStatus());

        return examScheduleRepository.save(existingExam);
    }

    @Override
    public void deleteExamSchedule(Long id) {
        ExamSchedule examSchedule = examScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Exam schedule not found with id: " + id));

        // Check if exam schedule is referenced by grades
        // Note: This would require a grades repository check in a real implementation
        
        examScheduleRepository.delete(examSchedule);
    }

    @Override
    public ExamSchedule getExamScheduleById(Long id) {
        return examScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Exam schedule not found with id: " + id));
    }

    @Override
    public Page<ExamSchedule> getAllExamSchedules(Pageable pageable) {
        return examScheduleRepository.findAll(pageable);
    }

    @Override
    public Page<ExamSchedule> getAllExamSchedules(String searchTerm, Pageable pageable) {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return examScheduleRepository.findBySearchTerm(searchTerm, pageable);
        }
        return examScheduleRepository.findAll(pageable);
    }

    @Override
    public List<ExamSchedule> getAllExamSchedules() {
        return examScheduleRepository.findAll();
    }

    @Override
    public List<ExamSchedule> findByStatus(ExamSchedule.ExamStatus status) {
        return examScheduleRepository.findByStatus(status);
    }

    @Override
    public List<ExamSchedule> getActiveExamSchedules() {
        return examScheduleRepository.findByStatus(ExamSchedule.ExamStatus.ACTIVE);
    }

    @Override
    public List<ExamSchedule> findByAcademicYear(String academicYear) {
        return examScheduleRepository.findByAcademicYear(academicYear);
    }

    @Override
    public List<ExamSchedule> findBySemester(String semester) {
        return examScheduleRepository.findBySemester(semester);
    }

    @Override
    public List<ExamSchedule> findByAcademicYearAndSemester(String academicYear, String semester) {
        return examScheduleRepository.findByAcademicYearAndSemester(academicYear, semester);
    }

    @Override
    public List<ExamSchedule> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return examScheduleRepository.findByDateRange(startDate, endDate);
    }

    @Override
    public List<ExamSchedule> findUpcomingExams() {
        return examScheduleRepository.findUpcomingExams(LocalDate.now());
    }

    @Override
    public List<ExamSchedule> findOngoingExams() {
        return examScheduleRepository.findOngoingExams(LocalDate.now());
    }

    @Override
    public List<ExamSchedule> findPastExams() {
        return examScheduleRepository.findPastExams(LocalDate.now());
    }

    @Override
    public Page<ExamSchedule> searchExamSchedules(String searchTerm, Pageable pageable) {
        return examScheduleRepository.findBySearchTerm(searchTerm, pageable);
    }

    @Override
    public boolean isExamScheduleValid(ExamSchedule examSchedule) {
        if (examSchedule == null) {
            return false;
        }
        
        if (examSchedule.getExamName() == null || examSchedule.getExamName().trim().isEmpty()) {
            return false;
        }
        
        if (examSchedule.getStartDate() == null || examSchedule.getEndDate() == null) {
            return false;
        }
        
        return examSchedule.isValidDateRange();
    }

    @Override
    public boolean hasDateConflicts(ExamSchedule examSchedule) {
        List<ExamSchedule> conflicts = findOverlappingExamSchedules(examSchedule);
        return !conflicts.isEmpty();
    }

    @Override
    public List<ExamSchedule> findOverlappingExamSchedules(ExamSchedule examSchedule) {
        return examScheduleRepository.findOverlappingExamSchedules(
                examSchedule.getStartDate(), 
                examSchedule.getEndDate()
        );
    }

    @Override
    public Map<String, Long> getExamScheduleStatistics() {
        Map<String, Long> statistics = new HashMap<>();
        
        List<Object[]> statusStats = examScheduleRepository.getExamScheduleStatistics();
        for (Object[] stat : statusStats) {
            String status = (String) stat[0];
            Long count = (Long) stat[1];
            statistics.put(status, count);
        }
        
        return statistics;
    }

    @Override
    public long countByStatus(ExamSchedule.ExamStatus status) {
        return examScheduleRepository.countByStatus(status);
    }

    @Override
    public long countByAcademicYear(String academicYear) {
        return examScheduleRepository.countByAcademicYear(academicYear);
    }

    @Override
    public ExamSchedule activateExamSchedule(Long id) {
        ExamSchedule examSchedule = getExamScheduleById(id);
        examSchedule.setStatus(ExamSchedule.ExamStatus.ACTIVE);
        return examScheduleRepository.save(examSchedule);
    }

    @Override
    public ExamSchedule deactivateExamSchedule(Long id) {
        ExamSchedule examSchedule = getExamScheduleById(id);
        examSchedule.setStatus(ExamSchedule.ExamStatus.INACTIVE);
        return examScheduleRepository.save(examSchedule);
    }

    @Override
    public ExamSchedule completeExamSchedule(Long id) {
        ExamSchedule examSchedule = getExamScheduleById(id);
        examSchedule.setStatus(ExamSchedule.ExamStatus.COMPLETED);
        return examScheduleRepository.save(examSchedule);
    }

    @Override
    public void bulkUpdateStatus(List<Long> ids, ExamSchedule.ExamStatus status) {
        for (Long id : ids) {
            try {
                ExamSchedule examSchedule = getExamScheduleById(id);
                examSchedule.setStatus(status);
                examScheduleRepository.save(examSchedule);
            } catch (IllegalArgumentException e) {
                // Log error and continue with other IDs
                System.err.println("Failed to update exam schedule with ID " + id + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void bulkDeleteExamSchedules(List<Long> ids) {
        for (Long id : ids) {
            try {
                deleteExamSchedule(id);
            } catch (IllegalArgumentException e) {
                // Log error and continue with other IDs
                System.err.println("Failed to delete exam schedule with ID " + id + ": " + e.getMessage());
            }
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }
}

