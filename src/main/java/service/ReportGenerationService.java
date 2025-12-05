package com.wsims.service;

import com.parami.wsims.entity.User;
import com.parami.wsims.entity.ExamSchedule;

import java.io.ByteArrayOutputStream;

public interface ReportGenerationService {
    
    /**
     * Generate a PDF report card for a student
     * @param studentId The ID of the student
     * @param examScheduleId The ID of the exam schedule (term)
     * @return ByteArrayOutputStream containing the PDF data
     */
    ByteArrayOutputStream generateStudentReportCard(Long studentId, Long examScheduleId);
    
    /**
     * Generate a PDF report card for a student with custom term name
     * @param studentId The ID of the student
     * @param examScheduleId The ID of the exam schedule (term)
     * @param termName Custom term name to display
     * @return ByteArrayOutputStream containing the PDF data
     */
    ByteArrayOutputStream generateStudentReportCard(Long studentId, Long examScheduleId, String termName);

    /**
     * Generate a PDF report card using a specific report template (header/footer).
     * @param studentId student identifier
     * @param examScheduleId exam schedule identifier
     * @param termName optional custom term name
     * @param templateId optional template id to use for header/footer
     */
    ByteArrayOutputStream generateStudentReportCard(Long studentId, Long examScheduleId, String termName, Long templateId);
    
    /**
     * Get student information for report generation
     * @param studentId The ID of the student
     * @return User entity containing student information
     */
    User getStudentForReport(Long studentId);
    
    /**
     * Get exam schedule information for report generation
     * @param examScheduleId The ID of the exam schedule
     * @return ExamSchedule entity containing exam information
     */
    ExamSchedule getExamScheduleForReport(Long examScheduleId);
}
