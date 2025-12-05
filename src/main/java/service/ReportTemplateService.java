package com.wsims.service;

import com.parami.wsims.entity.ReportTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ReportTemplateService {
    ReportTemplate create(ReportTemplate template);
    ReportTemplate update(ReportTemplate template);
    void delete(Long id);
    Optional<ReportTemplate> findById(Long id);
    List<ReportTemplate> findAll();
    Page<ReportTemplate> findAll(Pageable pageable);
}


