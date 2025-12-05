package com.wsims.controller;

import com.parami.wsims.entity.ExamSchedule;
import com.parami.wsims.entity.User;
import com.parami.wsims.service.ExamScheduleService;
import com.parami.wsims.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class TeacherReportController {

    private final UserService userService;
    private final ExamScheduleService examScheduleService;

    @Autowired
    public TeacherReportController(UserService userService, ExamScheduleService examScheduleService) {
        this.userService = userService;
        this.examScheduleService = examScheduleService;
    }

    @GetMapping("/teacher/reports/generate")
    @PreAuthorize("hasAuthority('TEACHER')")
    public String showTeacherReportGenerationPage(Model model) {
        try {
            List<User> students = userService.getAllStudents();
            List<ExamSchedule> examSchedules = examScheduleService.getAllExamSchedules();
            model.addAttribute("students", students);
            model.addAttribute("examSchedules", examSchedules);
        } catch (Exception e) {
            model.addAttribute("error", "Error loading report generation page: " + e.getMessage());
        }
        return "teacher-report-generation";
    }
}


