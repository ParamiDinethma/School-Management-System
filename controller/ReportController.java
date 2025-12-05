package com.wsims.controller;

import com.parami.wsims.entity.User;
import com.parami.wsims.entity.ExamSchedule;
import com.parami.wsims.service.ReportGenerationService;
import com.parami.wsims.service.UserService;
import com.parami.wsims.service.ExamScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportGenerationService reportGenerationService;
    private final UserService userService;
    private final ExamScheduleService examScheduleService;
    private final com.parami.wsims.service.ReportTemplateService templateService;

    @Autowired
    public ReportController(ReportGenerationService reportGenerationService,
                          UserService userService,
                          ExamScheduleService examScheduleService,
                          com.parami.wsims.service.ReportTemplateService templateService) {
        this.reportGenerationService = reportGenerationService;
        this.userService = userService;
        this.examScheduleService = examScheduleService;
        this.templateService = templateService;
    }

    /**
     * Display the report generation page
     */
    @GetMapping("/generate")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'TEACHER', 'IT_ADMIN', 'REGISTRAR')")
    public String showReportGenerationPage(Model model) {
        try {
            // Get all students (for dropdown)
            List<User> students = userService.getAllStudents();
            
            // Get all exam schedules (for dropdown)
            List<ExamSchedule> examSchedules = examScheduleService.getAllExamSchedules();
            
            model.addAttribute("students", students);
            model.addAttribute("examSchedules", examSchedules);
            
            return "report-generation";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading report generation page: " + e.getMessage());
            return "report-generation";
        }
    }

    /**
     * API endpoint to get students by grade level
     */
    @GetMapping("/api/students")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'TEACHER', 'IT_ADMIN', 'REGISTRAR')")
    public ResponseEntity<List<User>> getStudentsByGrade(@RequestParam(required = false) String gradeLevel) {
        try {
            List<User> students;
            if (gradeLevel != null && !gradeLevel.trim().isEmpty()) {
                students = userService.getStudentsByGradeLevel(gradeLevel);
            } else {
                students = userService.getAllStudents();
            }
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * API endpoint to get all exam schedules
     */
    @GetMapping("/api/exam-schedules")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'TEACHER', 'IT_ADMIN', 'REGISTRAR')")
    public ResponseEntity<List<ExamSchedule>> getExamSchedules() {
        try {
            List<ExamSchedule> examSchedules = examScheduleService.getAllExamSchedules();
            return ResponseEntity.ok(examSchedules);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * API endpoint to fetch active report templates for dropdown
     */
    @GetMapping("/api/templates")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'TEACHER', 'IT_ADMIN', 'REGISTRAR')")
    public ResponseEntity<List<com.parami.wsims.entity.ReportTemplate>> getActiveTemplates() {
        try {
            // Filter active templates on the client or add a repository method if needed
            List<com.parami.wsims.entity.ReportTemplate> all = templateService.findAll();
            List<com.parami.wsims.entity.ReportTemplate> active = all.stream().filter(com.parami.wsims.entity.ReportTemplate::isActive).toList();
            return ResponseEntity.ok(active);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate and download PDF report card
     */
    @PostMapping("/generate-pdf")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'TEACHER', 'IT_ADMIN', 'REGISTRAR')")
    public ResponseEntity<ByteArrayResource> generateReportCard(
            @RequestParam Long studentId,
            @RequestParam Long examScheduleId,
            @RequestParam(required = false) String termName,
            @RequestParam(required = false) Long templateId) {
        
        try {
            System.out.println("Generating report card for student ID: " + studentId + ", exam schedule ID: " + examScheduleId);
            
            // Get student and exam schedule for filename
            User student = userService.getUserById(studentId);
            ExamSchedule examSchedule = examScheduleService.getExamScheduleById(examScheduleId);
            
            // Generate PDF
            ByteArrayOutputStream pdfOutputStream = reportGenerationService.generateStudentReportCard(
                studentId, examScheduleId, termName, templateId);
            
            // Create filename
            String filename = String.format("ReportCard_%s_%s_%s.pdf",
                student.getUsername(),
                examSchedule.getExamName().replaceAll("\\s+", "_"),
                LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            );
            
            // Create response
            ByteArrayResource resource = new ByteArrayResource(pdfOutputStream.toByteArray());
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfOutputStream.size())
                    .body(resource);
                    
        } catch (Exception e) {
            System.err.println("Error generating report card: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Preview report card data (for validation before PDF generation)
     */
    @GetMapping("/preview/{studentId}/{examScheduleId}")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'TEACHER', 'IT_ADMIN', 'REGISTRAR')")
    public ResponseEntity<Map<String, Object>> previewReportCard(
            @PathVariable Long studentId,
            @PathVariable Long examScheduleId) {
        
        try {
            Map<String, Object> previewData = new HashMap<>();
            
            // Get student information
            User student = userService.getUserById(studentId);
            previewData.put("student", student);
            
            // Get exam schedule information
            ExamSchedule examSchedule = examScheduleService.getExamScheduleById(examScheduleId);
            previewData.put("examSchedule", examSchedule);
            
            // Get grades (this will be used in the actual PDF generation)
            // We don't need to fetch grades here as it's just for preview
            
            return ResponseEntity.ok(previewData);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error previewing report card: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
