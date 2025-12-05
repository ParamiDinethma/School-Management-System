package com.wsims.controller;

import com.parami.wsims.entity.ExamSchedule;
import com.parami.wsims.service.ExamScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/exam-schedules")
public class ExamScheduleController {

    private final ExamScheduleService examScheduleService;

    @Autowired
    public ExamScheduleController(ExamScheduleService examScheduleService) {
        this.examScheduleService = examScheduleService;
    }

    // Display the exam schedule management page
    @GetMapping
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'REGISTRAR', 'IT_ADMIN')")
    public String examScheduleManagementPage(Model model) {
        return "exam-schedule-management";
    }

    // API: Get all exam schedules with pagination and search
    @GetMapping("/api")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'REGISTRAR', 'IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllExamSchedules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "examName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<ExamSchedule> examSchedulePage;
            if (search != null && !search.trim().isEmpty()) {
                examSchedulePage = examScheduleService.getAllExamSchedules(search, pageable);
            } else {
                examSchedulePage = examScheduleService.getAllExamSchedules(pageable);
            }
            
            response.put("content", examSchedulePage.getContent());
            response.put("totalElements", examSchedulePage.getTotalElements());
            response.put("totalPages", examSchedulePage.getTotalPages());
            response.put("currentPage", examSchedulePage.getNumber());
            response.put("size", examSchedulePage.getSize());
            response.put("first", examSchedulePage.isFirst());
            response.put("last", examSchedulePage.isLast());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error retrieving exam schedules: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API: Get exam schedule by ID
    @GetMapping("/api/{id}")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'REGISTRAR', 'IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getExamScheduleById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ExamSchedule examSchedule = examScheduleService.getExamScheduleById(id);
            response.put("examSchedule", examSchedule);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("error", "Error retrieving exam schedule: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API: Create new exam schedule
    @PostMapping("/api")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'REGISTRAR', 'IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createExamSchedule(@RequestBody ExamSchedule examSchedule) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ExamSchedule createdExamSchedule = examScheduleService.createExamSchedule(examSchedule);
            response.put("examSchedule", createdExamSchedule);
            response.put("message", "Exam schedule created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("error", "Error creating exam schedule: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API: Update exam schedule
    @PutMapping("/api/{id}")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'REGISTRAR', 'IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateExamSchedule(@PathVariable Long id, @RequestBody ExamSchedule examSchedule) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ExamSchedule updatedExamSchedule = examScheduleService.updateExamSchedule(id, examSchedule);
            response.put("examSchedule", updatedExamSchedule);
            response.put("message", "Exam schedule updated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("error", "Error updating exam schedule: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API: Delete exam schedule
    @DeleteMapping("/api/{id}")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'REGISTRAR', 'IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteExamSchedule(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            examScheduleService.deleteExamSchedule(id);
            response.put("message", "Exam schedule deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("error", "Error deleting exam schedule: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API: Get exam schedule statistics
    @GetMapping("/api/statistics")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'REGISTRAR', 'IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getExamScheduleStatistics() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Long> statistics = examScheduleService.getExamScheduleStatistics();
            response.put("statistics", statistics);
            
            // Add additional statistics
            response.put("upcomingCount", examScheduleService.findUpcomingExams().size());
            response.put("ongoingCount", examScheduleService.findOngoingExams().size());
            response.put("pastCount", examScheduleService.findPastExams().size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error retrieving statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API: Get upcoming exams
    @GetMapping("/api/upcoming")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'REGISTRAR', 'IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUpcomingExams() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ExamSchedule> upcomingExams = examScheduleService.findUpcomingExams();
            response.put("upcomingExams", upcomingExams);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error retrieving upcoming exams: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API: Get ongoing exams
    @GetMapping("/api/ongoing")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'REGISTRAR', 'IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOngoingExams() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ExamSchedule> ongoingExams = examScheduleService.findOngoingExams();
            response.put("ongoingExams", ongoingExams);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error retrieving ongoing exams: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API: Update exam schedule status
    @PutMapping("/api/{id}/status")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'REGISTRAR', 'IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateExamScheduleStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String status = request.get("status");
            ExamSchedule updatedExamSchedule;
            
            switch (status.toUpperCase()) {
                case "ACTIVE":
                    updatedExamSchedule = examScheduleService.activateExamSchedule(id);
                    break;
                case "INACTIVE":
                    updatedExamSchedule = examScheduleService.deactivateExamSchedule(id);
                    break;
                case "COMPLETED":
                    updatedExamSchedule = examScheduleService.completeExamSchedule(id);
                    break;
                default:
                    response.put("error", "Invalid status: " + status);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            response.put("examSchedule", updatedExamSchedule);
            response.put("message", "Exam schedule status updated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.put("error", "Error updating exam schedule status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API: Bulk operations
    @PostMapping("/api/bulk/status")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'REGISTRAR', 'IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkUpdateStatus(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) request.get("ids");
            String statusStr = (String) request.get("status");
            
            ExamSchedule.ExamStatus status = ExamSchedule.ExamStatus.valueOf(statusStr.toUpperCase());
            examScheduleService.bulkUpdateStatus(ids, status);
            
            response.put("message", "Bulk status update completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error performing bulk update: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/api/bulk/delete")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'REGISTRAR', 'IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkDelete(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) request.get("ids");
            examScheduleService.bulkDeleteExamSchedules(ids);
            
            response.put("message", "Bulk delete completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error performing bulk delete: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API: Check for date conflicts
    @PostMapping("/api/check-conflicts")
    @PreAuthorize("hasAnyAuthority('PRINCIPAL', 'REGISTRAR', 'IT_ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkDateConflicts(@RequestBody ExamSchedule examSchedule) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ExamSchedule> conflicts = examScheduleService.findOverlappingExamSchedules(examSchedule);
            response.put("hasConflicts", !conflicts.isEmpty());
            response.put("conflicts", conflicts);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error checking conflicts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
