package com.wsims.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.parami.wsims.entity.*;
import com.parami.wsims.repository.GradeRepository;
import com.parami.wsims.repository.UserRepository;
import com.parami.wsims.repository.ExamScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportGenerationServiceImpl implements ReportGenerationService {

    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final com.parami.wsims.repository.ReportTemplateRepository templateRepository;

    @Autowired
    public ReportGenerationServiceImpl(GradeRepository gradeRepository,
                                     UserRepository userRepository,
                                     ExamScheduleRepository examScheduleRepository,
                                     com.parami.wsims.repository.ReportTemplateRepository templateRepository) {
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.templateRepository = templateRepository;
    }

    @Override
    public ByteArrayOutputStream generateStudentReportCard(Long studentId, Long examScheduleId) {
        return generateStudentReportCard(studentId, examScheduleId, null);
    }

    @Override
    public ByteArrayOutputStream generateStudentReportCard(Long studentId, Long examScheduleId, String termName) {
        try {
            // Get student and exam schedule information
            User student = getStudentForReport(studentId);
            ExamSchedule examSchedule = getExamScheduleForReport(examScheduleId);
            
            // Get grades for the student and exam schedule
            List<Grade> grades = gradeRepository.findByStudentIdAndExamScheduleId(studentId, examScheduleId);
            
            // Create PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Add content to PDF
            addReportHeader(document, student, examSchedule, termName);
            addStudentInfo(document, student);
            addGradesTable(document, grades);
            addFooter(document);

            document.close();
            return outputStream;

        } catch (Exception e) {
            throw new RuntimeException("Error generating report card: " + e.getMessage(), e);
        }
    }

    @Override
    public ByteArrayOutputStream generateStudentReportCard(Long studentId, Long examScheduleId, String termName, Long templateId) {
        if (templateId == null) {
            return generateStudentReportCard(studentId, examScheduleId, termName);
        }
        try {
            User student = getStudentForReport(studentId);
            ExamSchedule examSchedule = getExamScheduleForReport(examScheduleId);
            List<Grade> grades = gradeRepository.findByStudentIdAndExamScheduleId(studentId, examScheduleId);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            com.parami.wsims.entity.ReportTemplate template = templateRepository.findById(templateId).orElse(null);
            String headerOverride = template != null ? template.getHeaderText() : null;
            String footerOverride = template != null ? template.getFooterText() : null;

            addReportHeader(document, student, examSchedule, termName);
            if (headerOverride != null && !headerOverride.isBlank()) {
                document.add(new Paragraph(headerOverride).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));
            }
            addStudentInfo(document, student);
            addGradesTable(document, grades);
            if (footerOverride != null && !footerOverride.isBlank()) {
                document.add(new Paragraph(footerOverride).setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
            }
            addFooter(document);

            document.close();
            return outputStream;
        } catch (Exception e) {
            throw new RuntimeException("Error generating report card with template: " + e.getMessage(), e);
        }
    }

    @Override
    public User getStudentForReport(Long studentId) {
        return userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));
    }

    @Override
    public ExamSchedule getExamScheduleForReport(Long examScheduleId) {
        return examScheduleRepository.findById(examScheduleId)
                .orElseThrow(() -> new RuntimeException("Exam schedule not found with ID: " + examScheduleId));
    }

    private void addReportHeader(Document document, User student, ExamSchedule examSchedule, String termName) {
        // School header
        Paragraph schoolHeader = new Paragraph("WEB-BASED SCHOOL MANAGEMENT SYSTEM")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18)
                .setBold()
                .setMarginBottom(10);

        Paragraph reportTitle = new Paragraph("ACADEMIC REPORT CARD")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(16)
                .setBold()
                .setMarginBottom(20);

        document.add(schoolHeader);
        document.add(reportTitle);

        // Academic year and term information
        String academicYear = examSchedule.getAcademicYear();
        String examName = examSchedule.getExamName();
        String displayTermName = termName != null ? termName : examName;
        
        Paragraph academicInfo = new Paragraph()
                .add("Academic Year: ").add(academicYear)
                .add(" | Term: ").add(displayTermName)
                .add(" | Report Generated: ").add(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12)
                .setMarginBottom(20);

        document.add(academicInfo);
    }

    private void addStudentInfo(Document document, User student) {
        // Student information section
        Paragraph studentSection = new Paragraph("STUDENT INFORMATION")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);

        document.add(studentSection);

        // Create a table for student information
        Table studentTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Student details
        addStudentInfoRow(studentTable, "Student ID", student.getUsername());
        addStudentInfoRow(studentTable, "Full Name", student.getFirstName() + " " + student.getLastName());
        addStudentInfoRow(studentTable, "Class/Grade", "Student");
        addStudentInfoRow(studentTable, "Email", student.getEmail() != null ? student.getEmail() : "N/A");

        document.add(studentTable);
    }

    private void addStudentInfoRow(Table table, String label, String value) {
        Cell labelCell = new Cell().add(new Paragraph(label).setBold()).setPadding(8);
        Cell valueCell = new Cell().add(new Paragraph(value)).setPadding(8);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addGradesTable(Document document, List<Grade> grades) {
        if (grades.isEmpty()) {
            Paragraph noGrades = new Paragraph("No grades available for this term.")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12)
                    .setMarginTop(20);
            document.add(noGrades);
            return;
        }

        // Grades section header
        Paragraph gradesSection = new Paragraph("ACADEMIC PERFORMANCE")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);

        document.add(gradesSection);

        // Create grades table
        Table gradesTable = new Table(UnitValue.createPercentArray(new float[]{25, 15, 15, 15, 15, 15}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Table headers
        String[] headers = {"Subject", "Marks Obtained", "Total Marks", "Percentage", "Letter Grade", "Comments"};
        for (String header : headers) {
            Cell headerCell = new Cell().add(new Paragraph(header).setBold()).setPadding(8);
            gradesTable.addHeaderCell(headerCell);
        }

        // Add grade data
        for (Grade grade : grades) {
            gradesTable.addCell(createCell(grade.getSubject().getSubjectName()));
            gradesTable.addCell(createCell(String.valueOf(grade.getMarksObtained())));
            gradesTable.addCell(createCell(String.valueOf(grade.getTotalMarks())));
            gradesTable.addCell(createCell(String.format("%.2f%%", grade.getPercentage())));
            gradesTable.addCell(createCell(grade.getLetterGrade()));
            gradesTable.addCell(createCell(grade.getComments() != null ? grade.getComments() : ""));
        }

        document.add(gradesTable);

        // Add summary statistics
        addGradesSummary(document, grades);
    }

    private Cell createCell(String content) {
        return new Cell().add(new Paragraph(content)).setPadding(8);
    }

    private void addGradesSummary(Document document, List<Grade> grades) {
        // Calculate summary statistics
        double totalPercentage = grades.stream()
                .mapToDouble(grade -> grade.getPercentage().doubleValue())
                .average()
                .orElse(0.0);

        long passingGrades = grades.stream()
                .filter(Grade::isPassing)
                .count();

        long excellentGrades = grades.stream()
                .filter(Grade::isExcellent)
                .count();

        // Summary section
        Paragraph summarySection = new Paragraph("PERFORMANCE SUMMARY")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);

        document.add(summarySection);

        // Create summary table
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        summaryTable.addCell(createCell("Overall Average"));
        summaryTable.addCell(createCell(String.format("%.2f%%", totalPercentage)));

        summaryTable.addCell(createCell("Passing Grades"));
        summaryTable.addCell(createCell(String.valueOf(passingGrades) + "/" + grades.size()));

        summaryTable.addCell(createCell("Excellent Grades (A)"));
        summaryTable.addCell(createCell(String.valueOf(excellentGrades)));

        summaryTable.addCell(createCell("Overall Performance"));
        summaryTable.addCell(createCell(getOverallPerformance(totalPercentage)));

        document.add(summaryTable);
    }

    private String getOverallPerformance(double average) {
        if (average >= 90) return "Outstanding";
        else if (average >= 80) return "Excellent";
        else if (average >= 70) return "Good";
        else if (average >= 60) return "Satisfactory";
        else if (average >= 50) return "Needs Improvement";
        else return "Below Expectations";
    }

    private void addFooter(Document document) {
        // Add footer
        Paragraph footer = new Paragraph()
                .add("This report was generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10)
                .setMarginTop(30);

        document.add(footer);

        // Add signature lines
        Paragraph signatures = new Paragraph()
                .add("Class Teacher Signature: ________________________\n\n")
                .add("Principal Signature: ________________________\n\n")
                .setFontSize(12)
                .setMarginTop(40);

        document.add(signatures);
    }
}
