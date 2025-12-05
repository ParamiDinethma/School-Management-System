package com.wsims.service;

import com.parami.wsims.entity.*;
import com.parami.wsims.repository.GradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class GradeServiceImpl implements GradeService {

    @Autowired
    private GradeRepository gradeRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private SubjectService subjectService;
    
    @Autowired
    private ExamScheduleService examScheduleService;

    @Override
    @Transactional
    public Grade saveGrade(Grade grade) {
        return gradeRepository.save(grade);
    }

    @Override
    public List<Grade> saveBulkGrades(List<Map<String, Object>> gradeEntries, Long courseId, Long examScheduleId) {
        List<Grade> savedGrades = new ArrayList<>();
        
        System.out.println("=== Starting bulk grade save ===");
        System.out.println("Course ID: " + courseId);
        System.out.println("Exam Schedule ID: " + examScheduleId);
        System.out.println("Number of grade entries: " + gradeEntries.size());
        
        if (gradeEntries == null || gradeEntries.isEmpty()) {
            System.err.println("ERROR: No grade entries provided!");
            return savedGrades;
        }
        
        // Log the first entry to see its structure
        System.out.println("First entry structure: " + gradeEntries.get(0));
        
        // Check for duplicate entries that might cause constraint violations
        Set<String> seenEntries = new HashSet<>();
        for (Map<String, Object> entry : gradeEntries) {
            String entryKey = entry.get("studentId") + "-" + entry.get("subjectId");
            if (seenEntries.contains(entryKey)) {
                System.err.println("WARNING: Duplicate entry found for student " + entry.get("studentId") + 
                                 ", subject " + entry.get("subjectId"));
            } else {
                seenEntries.add(entryKey);
            }
        }
        
        for (Map<String, Object> entry : gradeEntries) {
            try {
                Long studentId = Long.valueOf(entry.get("studentId").toString());
                Long subjectId = Long.valueOf(entry.get("subjectId").toString());
                
                System.out.println("Processing entry - Student ID: " + studentId + ", Subject ID: " + subjectId);
                
                // Get entities
                User student = userService.getUserById(studentId);
                Course course = courseService.getCourseById(courseId);
                Subject subject = subjectService.getSubjectById(subjectId);
                ExamSchedule examSchedule = examScheduleService.getExamScheduleById(examScheduleId);
                
                System.out.println("Entities found - Student: " + (student != null ? "ID=" + student.getId() : "NULL") + 
                                 ", Course: " + (course != null ? "ID=" + course.getId() : "NULL") + 
                                 ", Subject: " + (subject != null ? "ID=" + subject.getId() : "NULL") + 
                                 ", ExamSchedule: " + (examSchedule != null ? "ID=" + examSchedule.getId() : "NULL"));
                
                if (student == null || course == null || subject == null || examSchedule == null) {
                    System.err.println("Skipping entry due to null entities");
                    continue; // Skip invalid entries
                }
                
                // Get or create grade
                Grade grade = getOrCreateGrade(studentId, courseId, subjectId, examScheduleId);
                System.out.println("Grade object created/retrieved: " + (grade != null ? "YES" : "NO"));
                
                if (grade == null) {
                    System.err.println("Cannot proceed - grade object is null for student " + studentId + ", subject " + subjectId);
                    continue;
                }
                
                // Update grade with new data
                if (entry.containsKey("marksObtained") && entry.get("marksObtained") != null) {
                    BigDecimal marksObtained = new BigDecimal(entry.get("marksObtained").toString());
                    grade.setMarksObtained(marksObtained);
                    System.out.println("Set marks obtained: " + marksObtained);
                }
                
                // Always ensure totalMarks is set (required field)
                if (entry.containsKey("totalMarks") && entry.get("totalMarks") != null) {
                    BigDecimal totalMarks = new BigDecimal(entry.get("totalMarks").toString());
                    grade.setTotalMarks(totalMarks);
                    System.out.println("Set total marks from entry: " + totalMarks);
                } else {
                    // Set default total marks if not provided
                    grade.setTotalMarks(new BigDecimal("100"));
                    System.out.println("Set default total marks: 100");
                }
                
                if (entry.containsKey("comments")) {
                    String comments = entry.get("comments") != null ? entry.get("comments").toString() : "";
                    grade.setComments(comments);
                    System.out.println("Set comments: " + comments);
                }
                
                // Set created by if not already set
                if (grade.getCreatedBy() == null) {
                    // Get current user from security context
                    User currentUser = getCurrentUser();
                    if (currentUser != null) {
                        grade.setCreatedBy(currentUser);
                        System.out.println("Set created by: " + currentUser.getUsername());
                    } else {
                        System.err.println("Warning: Could not get current user for createdBy field");
                    }
                }
                
                // Validate grade before saving
                if (grade.getMarksObtained() != null && grade.getTotalMarks() != null) {
                    if (grade.isValidMarks()) {
                        System.out.println("Grade validation passed, attempting to save...");
                        Grade savedGrade = saveGradeInSeparateTransaction(grade);
                        if (savedGrade != null) {
                            savedGrades.add(savedGrade);
                            System.out.println("Grade saved successfully with ID: " + savedGrade.getId());
                        } else {
                            System.err.println("Failed to save grade for student " + studentId + ", subject " + subjectId);
                            System.err.println("Grade details - Student: " + (grade.getStudent() != null ? grade.getStudent().getId() : "NULL") +
                                             ", Course: " + (grade.getCourse() != null ? grade.getCourse().getId() : "NULL") +
                                             ", Subject: " + (grade.getSubject() != null ? grade.getSubject().getId() : "NULL") +
                                             ", ExamSchedule: " + (grade.getExamSchedule() != null ? grade.getExamSchedule().getId() : "NULL") +
                                             ", Marks: " + grade.getMarksObtained() + "/" + grade.getTotalMarks());
                        }
                    } else {
                        System.err.println("Invalid marks for student " + studentId + ", subject " + subjectId + ": " + 
                                         grade.getMarksObtained() + "/" + grade.getTotalMarks());
                    }
                } else {
                    System.err.println("Missing required marks data for student " + studentId + ", subject " + subjectId);
                    System.err.println("Marks obtained: " + grade.getMarksObtained());
                    System.err.println("Total marks: " + grade.getTotalMarks());
                }
                
            } catch (Exception e) {
                // Log error but continue processing other entries
                System.err.println("Error processing grade entry: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("=== Bulk grade save completed ===");
        System.out.println("Successfully saved " + savedGrades.size() + " grades out of " + gradeEntries.size() + " entries");
        
        return savedGrades;
    }

    @Override
    public List<Grade> getGradesByStudent(Long studentId) {
        return gradeRepository.findByStudentIdOrderByExamScheduleStartDateDesc(studentId);
    }

    @Override
    public List<Grade> getGradesByCourse(Long courseId) {
        return gradeRepository.findByCourseId(courseId);
    }

    @Override
    public List<Grade> getGradesByCourseAndExam(Long courseId, Long examScheduleId) {
        return gradeRepository.findByCourseIdAndExamScheduleId(courseId, examScheduleId);
    }

    @Override
    public List<Grade> getGradesByStudentAndCourse(Long studentId, Long courseId) {
        return gradeRepository.findByStudentIdAndCourseId(studentId, courseId);
    }

    @Override
    public List<Grade> getGradesByStudentCourseAndExam(Long studentId, Long courseId, Long examScheduleId) {
        return gradeRepository.findByStudentIdAndCourseIdAndExamScheduleId(studentId, courseId, examScheduleId);
    }

    @Override
    public Page<Grade> getGrades(Pageable pageable) {
        return gradeRepository.findAll(pageable);
    }

    @Override
    public Page<Grade> getGradesByStudent(Long studentId, Pageable pageable) {
        return gradeRepository.findByStudentId(studentId, pageable);
    }

    @Override
    public Page<Grade> getGradesByCourse(Long courseId, Pageable pageable) {
        return gradeRepository.findByCourseId(courseId, pageable);
    }

    @Override
    public void deleteGrade(Long gradeId) {
        gradeRepository.deleteById(gradeId);
    }

    @Override
    public Grade getGradeById(Long gradeId) {
        return gradeRepository.findById(gradeId).orElse(null);
    }

    @Override
    public boolean gradeExists(Long studentId, Long courseId, Long subjectId, Long examScheduleId) {
        return gradeRepository.existsByStudentIdAndCourseIdAndSubjectIdAndExamScheduleId(
                studentId, courseId, subjectId, examScheduleId);
    }

    @Override
    public Grade getOrCreateGrade(Long studentId, Long courseId, Long subjectId, Long examScheduleId) {
        Optional<Grade> existingGrade = gradeRepository.findByStudentIdAndCourseIdAndSubjectIdAndExamScheduleId(
                studentId, courseId, subjectId, examScheduleId);
        
        if (existingGrade.isPresent()) {
            System.out.println("Found existing grade for student " + studentId + ", subject " + subjectId + 
                             " with ID: " + existingGrade.get().getId());
            return existingGrade.get();
        }
        
        // Create new grade (don't save yet, just prepare it)
        Grade newGrade = new Grade();
        User student = userService.getUserById(studentId);
        Course course = courseService.getCourseById(courseId);
        Subject subject = subjectService.getSubjectById(subjectId);
        ExamSchedule examSchedule = examScheduleService.getExamScheduleById(examScheduleId);
        
        if (student == null || course == null || subject == null || examSchedule == null) {
            System.err.println("Cannot create grade - missing entities: student=" + (student != null) + 
                             ", course=" + (course != null) + ", subject=" + (subject != null) + 
                             ", examSchedule=" + (examSchedule != null));
            return null;
        }
        
        newGrade.setStudent(student);
        newGrade.setCourse(course);
        newGrade.setSubject(subject);
        newGrade.setExamSchedule(examSchedule);
        newGrade.setTotalMarks(new BigDecimal("100")); // Default total marks
        newGrade.setCreatedBy(getCurrentUser());
        
        System.out.println("Created new grade object for student " + studentId + ", subject " + subjectId);
        return newGrade; // Don't save here, let the caller handle saving
    }

    /**
     * Save a single grade in its own transaction to avoid rollback issues
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Grade saveGradeInSeparateTransaction(Grade grade) {
        try {
            System.out.println("Attempting to save grade for student " + grade.getStudent().getId() + 
                             ", subject " + grade.getSubject().getId() + 
                             ", marks: " + grade.getMarksObtained() + "/" + grade.getTotalMarks());
            Grade savedGrade = gradeRepository.save(grade);
            System.out.println("Successfully saved grade with ID: " + savedGrade.getId());
            return savedGrade;
        } catch (Exception e) {
            System.err.println("Error saving grade in separate transaction: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the currently authenticated user
     */
    private User getCurrentUser() {
        try {
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
                org.springframework.security.core.userdetails.User springUser = 
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
                
                // Get the actual User entity from the database using the username
                return userService.findByUsername(springUser.getUsername()).orElse(null);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
            return null;
        }
    }
}
