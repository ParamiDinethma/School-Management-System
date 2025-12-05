package com.wsims.service;

import com.parami.wsims.entity.ReportTemplate;
import com.parami.wsims.repository.ReportTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReportTemplateServiceImpl implements ReportTemplateService {

    private final ReportTemplateRepository repository;

    @Autowired
    public ReportTemplateServiceImpl(ReportTemplateRepository repository) {
        this.repository = repository;
    }

    @Override
    public ReportTemplate create(ReportTemplate template) {
        return repository.save(template);
    }

    @Override
    public ReportTemplate update(ReportTemplate template) {
        return repository.save(template);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<ReportTemplate> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<ReportTemplate> findAll() {
        return repository.findAll();
    }

    @Override
    public Page<ReportTemplate> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }
}


