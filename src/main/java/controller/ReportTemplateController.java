package com.wsims.controller;

import com.parami.wsims.entity.ReportTemplate;
import com.parami.wsims.service.ReportTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/report-templates")
@PreAuthorize("hasAnyAuthority('PRINCIPAL','IT_ADMIN')")
public class ReportTemplateController {

    private final ReportTemplateService service;

    @Autowired
    public ReportTemplateController(ReportTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public String page(Model model) {
        List<ReportTemplate> templates = service.findAll();
        model.addAttribute("templates", templates);
        return "report-templates";
    }

    // Create
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> create(@RequestBody ReportTemplate template) {
        ReportTemplate created = service.create(template);
        return ResponseEntity.ok(Map.of("success", true, "template", created));
    }

    // Read all
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(Map.of("success", true, "templates", service.findAll()));
    }

    // Read one
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> get(@PathVariable Long id) {
        return service.findById(id)
                .map(t -> ResponseEntity.ok(Map.of("success", true, "template", t)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Update
    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ReportTemplate template) {
        return service.findById(id)
                .map(existing -> {
                    // Only update mutable fields; preserve createdAt and other immutable data
                    existing.setTemplateName(template.getTemplateName());
                    existing.setHeaderText(template.getHeaderText());
                    existing.setFooterText(template.getFooterText());
                    existing.setActive(template.isActive());
                    ReportTemplate updated = service.update(existing);
                    return ResponseEntity.ok(Map.of("success", true, "template", updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete
    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}


